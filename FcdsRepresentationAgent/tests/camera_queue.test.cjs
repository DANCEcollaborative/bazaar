const assert = require('node:assert/strict');
const { test } = require('node:test');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const { webcrypto } = require('node:crypto');

const source = fs.readFileSync(path.join(__dirname, '../ui/camera_fcds-recorded.js'), 'utf8');
const room = 'fcds-p2-26-fall-1a-room260911999';
const credential = '2000000000.' + 'a'.repeat(64);
const settle = async () => { for (let n = 0; n < 80; n++) await Promise.resolve(); };

function fakeDatabase(rows) {
  return { transaction() {
    const tx = { objectStore() { return {
      put(value) { rows.set(value.id, structuredClone(value)); queueMicrotask(() => tx.oncomplete?.()); },
      delete(id) { rows.delete(id); queueMicrotask(() => tx.oncomplete?.()); },
      getAll() {
        const request = {};
        queueMicrotask(() => { request.result = [...rows.values()].map(value => structuredClone(value)); request.onsuccess?.(); });
        return request;
      }
    }; } };
    return tx;
  } };
}

async function camera(rows = new Map(), storageFails = false, options = {}) {
  const elements = {}, intervals = new Map(), calls = [], socketEvents = {}, feed = [];
  const orientationWait=options.orientationWait;
  let disconnects = 0, nowMs = 0;
  let orientationResult=options.orientation || {rotation_degrees:0,confidence:'high',readable:true};
  let orientationFailure=false;
  let cameraError = options.cameraError || null;
  let failure = false, uploadFailure = false, wrongAck = false, stoppedTracks = 0;
  let phase = options.phase === undefined ? 'Paper' : options.phase;
  let framePhase = null, relayState = options.relayState || 'accepted_by_relay', phaseDenied = !!options.phaseDenied;
  const makeCanvas=()=>({width:0,height:0,getContext:()=>({drawImage(){},fillRect(){},translate(){},rotate(){}}),toDataURL:()=> 'data:image/jpeg;base64,c3ludGhldGlj'});
  const get = id => elements[id] ||= {
    style: {}, value: '1', ...(id==='preview'?{videoWidth:960,videoHeight:720,readyState:2}:{}),
    ...(id==='manualPreview'?{naturalWidth:1200,naturalHeight:900}:{}), removeAttribute() {}, click() {},
    play: async () => {}, getContext: () => ({ drawImage() {},fillRect(){},translate(){},rotate(){} }),
    toDataURL: () => {if(id==='canvas')nowMs+=options.renderElapsedMs || 0;return 'data:image/jpeg;base64,c3ludGhldGlj';}, prepend(p) {feed.push(p.textContent);},
    set src(value) { this._src = value; queueMicrotask(() => this.onload?.()); },
    get src() { return this._src; }
  };
  const db = fakeDatabase(rows);
  const windowEvents = {}, documentEvents = {};
  const context = {
    URLSearchParams, Date, JSON, Promise, Error, String, Math, crypto: webcrypto, performance:{now:()=>nowMs},
    FileReader: class { readAsDataURL() { queueMicrotask(() => { this.result = 'data:image/png;base64,c3ludGhldGlj'; this.onload?.(); }); } },
    location: { search: '?room=' + room + '&user=1', hash: '#capture=' + credential },
    document: { getElementById: get, createElement: name => name==='canvas'?makeCanvas():get(name),
      addEventListener: (name, fn) => { documentEvents[name] = fn; } },
    window: { addEventListener: (name, fn) => { windowEvents[name] = fn; } },
    navigator: { userAgent: 'synthetic test', mediaDevices: { getUserMedia: async () => {
      if(cameraError) { const error=Error('camera denied');error.name=cameraError;throw error; }
      return {getTracks: () => [{ stop() { stoppedTracks++; } }]};
    } } },
    indexedDB: { open() { const request = {}; queueMicrotask(() => {
      if (storageFails) request.onerror?.();
      else { request.result = db; request.onsuccess?.(); }
    }); return request; } },
    io: () => ({ on(name, fn) {socketEvents[name]=fn;}, emit() {}, disconnect() {disconnects++;} }),
    setInterval: (fn, delay) => { intervals.set(delay, fn); return delay; },
    clearInterval: delay => intervals.delete(delay),
    AbortSignal: { timeout: () => undefined },
    fetch: async (url, requestOptions) => {
      const body = JSON.parse(requestOptions.body); calls.push({ url, body, headers: requestOptions.headers });
      if(url.endsWith('/orientation')) {
        if(orientationFailure)throw Error('orientation offline');
        if(orientationWait)await orientationWait;
        nowMs+=options.orientationElapsedMs || 0;
        return {ok:true,json:async()=>orientationResult};
      }
      if (failure) throw Error('offline');
      const phaseResult = {phase, camera_allowed: phase === 'Paper' || phase === 'Setup'};
      if (url.endsWith('/phase')) return {ok: !phaseDenied, status:phaseDenied ? 403 : 200, json: async () => phaseResult};
      if (url.endsWith('/frame-status')) return { ok: true, json: async () => ({
        stored: true, frame_id: body.frame_id, ...phaseResult, relay_state: phase === 'Coding' || phase === 'Submit' ? 'phase_closed' : relayState
      }) };
      if (uploadFailure) throw Error('upload unavailable');
      const uploadPhase = framePhase || phase;
      return { ok: true, json: async () => ({ stored: true, phase:uploadPhase, camera_allowed:uploadPhase === 'Paper' || uploadPhase === 'Setup', relay_state:uploadPhase === 'Coding' || uploadPhase === 'Submit' ? 'phase_closed' : 'queued',
        frame_id: wrongAck ? 'wrong-id' : body.frame_id,
        event_ids: wrongAck ? [] : (body.events || []).map(e => e.event_id) }) };
    }
  };
  await vm.runInNewContext(source, context); await settle();
  return { rows, calls, get, intervals, windowEvents, documentEvents, socketEvents, feed,
    get disconnects() {return disconnects;},
    orientation(value) {orientationResult=value;}, orientationFailure(value) {orientationFailure=value;},
    cameraError(value) { cameraError = value; }, offline(value) { failure = value; }, uploadFailure(value) { uploadFailure = value; }, wrongAck(value) { wrongAck = value; },
    phase(value) { phase = value; }, phaseDenied(value) { phaseDenied = value; }, framePhase(value) { framePhase = value; }, relayState(value) { relayState = value; },
    get stoppedTracks() { return stoppedTracks; },
    async flush() { intervals.get(2000)(); await settle(); },
    async relayStatus() { intervals.get(3000)(); await settle(); } };
}

test('camera retains failed/unacknowledged frames, then retries the same ID after reload', async () => {
  const first = await camera();
  assert.equal(first.rows.size, 0, 'initial page event was acknowledged');
  first.offline(true);
  await first.get('start').onclick(); await settle();
  assert.equal(first.get('preview').hidden, false, 'live capture shows the video preview');
  const frame = [...first.rows.values()].find(row => row.kind === 'frame');
  assert.ok(frame, 'frame was durably queued before upload');
  assert.equal(frame.body.participant_id, '1');
  assert.equal(frame.body.room_id, room);
  assert.equal(frame.body.capture_mode, 'continuous');
  first.get('stop').onclick(); await settle();
  assert.equal(first.get('preview').hidden, true, 'stopped capture hides the unused video panel');
  assert.ok(first.stoppedTracks > 0);
  assert.ok(first.rows.has(frame.id), 'stop does not discard a queued frame');
  first.offline(false); first.wrongAck(true);
  await first.flush();
  assert.ok(first.rows.has(frame.id), 'a mismatched archive acknowledgement is not accepted');

  // Another participant's queue must not be sent with this phone's credentials.
  first.rows.set('other-user', { id: 'other-user', room, user: '2', time: '2026-09-01', seq: 1 });
  const reopened = await camera(first.rows);
  await reopened.flush();
  assert.ok(!reopened.rows.has(frame.id), 'reload retries and acknowledges the original frame');
  assert.ok(reopened.rows.has('other-user'), 'other participant records remain isolated');
  const replay = reopened.calls.find(call => call.body.frame_id === frame.id);
  assert.ok(replay);
  assert.equal(replay.body.producer_id, frame.body.producer_id);
  assert.equal(replay.body.producer_sequence, frame.body.producer_sequence);
  assert.ok(reopened.calls.every(call => call.body.participant_id === '1'));
  assert.ok(reopened.calls.every(call => call.headers.Authorization === 'Camera ' + credential));
});

test('a selected photo is reviewed, archived and retried without starting a stream', async () => {
  const page = await camera();
  page.offline(true);
  page.get('chooseInput').files = [{ type: 'image/png' }];
  await page.get('chooseInput').onchange(); await settle();
  assert.equal(page.get('correctedPhoto').hidden, false);
  assert.equal(page.get('sendPhoto').disabled, false);
  await page.get('sendPhoto').onclick(); await settle();
  const frame = [...page.rows.values()].find(row => row.kind === 'frame');
  assert.ok(frame, 'manual image remains queued while offline');
  assert.equal(frame.body.capture_mode, 'manual');
  assert.equal(frame.body.mimeType, 'image/jpeg', 'browser canvas normalizes the chosen image');
  assert.equal(page.get('manualPreview').hidden, true);
  assert.equal(page.stoppedTracks, 0, 'manual submission does not need a continuous camera stream');
  assert.match(page.get('manualStatus').textContent, /not finished uploading/i);
  page.offline(false); await page.flush();
  assert.equal(page.rows.get(frame.id).kind, 'relay', 'durable receipt survives archive acknowledgement');
  assert.match(page.get('manualStatus').textContent, /Photo uploaded. Delivering it to your Paper tutor page/i);
  await page.relayStatus();
  assert.match(page.get('manualStatus').textContent, /sent to the tutor/i);
  assert.equal(page.calls.find(call => call.url.endsWith('/frame-status')).body.frame_id, frame.id);
  page.get('takeInput').files = [{ type: 'image/jpeg' }];
  await page.get('takeInput').onchange(); await settle();
  assert.equal(page.get('sendPhoto').disabled, false, 'another photo can be sent later');
  page.get('discardPhoto').onclick();
  assert.equal(page.get('sendPhoto').disabled, true);
});

test('manual controls are disabled when durable browser storage is unavailable', async () => {
  const page = await camera(new Map(), true);
  assert.equal(page.get('start').disabled, true);
  assert.equal(page.get('takePhoto').disabled, true);
  assert.equal(page.get('choosePhoto').disabled, true);
  assert.match(page.get('manualStatus').textContent, /storage is unavailable/i);
});


test('archive acknowledgement survives reload and resumes relay polling without re-upload', async () => {
  const first = await camera(new Map(), false, {relayState:'queued'});
  first.get('chooseInput').files = [{type:'image/png'}];
  await first.get('chooseInput').onchange(); await settle();
  await first.get('sendPhoto').onclick(); await settle();
  const receipt = [...first.rows.values()].find(row => row.kind === 'relay');
  assert.ok(receipt, 'pending relay receipt is durable after image upload');
  assert.equal(receipt.body.imageBase64, undefined, 'receipt no longer needs the image bytes');
  const reopened = await camera(first.rows);
  await reopened.relayStatus();
  assert.ok(!reopened.rows.has(receipt.id));
  assert.ok(reopened.calls.some(call => call.url.endsWith('/frame-status') && call.body.frame_id === receipt.id));
  assert.ok(!reopened.calls.some(call => call.url.endsWith('/frame') && call.body.frame_id === receipt.id), 'already archived photo is not re-uploaded');
  assert.match(reopened.get('manualStatus').textContent, /Photo sent to the tutor/);
});

test('phase change stops the live camera and blocks new photos', async () => {
  const page = await camera();
  await page.get('start').onclick(); await settle();
  assert.equal(page.get('preview').hidden, false);
  page.phase('Coding'); await page.relayStatus();
  assert.equal(page.get('preview').hidden, true);
  assert.ok(page.stoppedTracks > 0);
  for (const id of ['start','takePhoto','choosePhoto','sendPhoto']) assert.equal(page.get(id).disabled, true);
  assert.match(page.get('status').textContent, /Return to your JupyterLab notebook/);
  assert.match(page.get('manualStatus').textContent, /New photos will not receive tutor feedback/);
});

test('photo crossing the phase boundary is acknowledged without promising tutor feedback', async () => {
  const page = await camera();
  page.get('chooseInput').files = [{type:'image/png'}];
  await page.get('chooseInput').onchange(); await settle();
  page.framePhase('Coding');
  await page.get('sendPhoto').onclick(); await settle();
  assert.ok(![...page.rows.values()].some(row => row.kind === 'frame' || row.kind === 'relay'));
  assert.match(page.get('manualStatus').textContent, /paper phase has ended/);
  assert.doesNotMatch(page.get('manualStatus').textContent, /Check your Paper tutor page for feedback/);
  // An older Paper response must not reopen a completed phase.
  page.phase('Paper'); await page.relayStatus();
  assert.equal(page.get('choosePhoto').disabled, true);
});

test('unknown phase disables capture until the paper phase is confirmed', async () => {
  const page = await camera(new Map(), false, {phase:null});
  assert.equal(page.get('start').disabled, true);
  assert.equal(page.get('choosePhoto').disabled, true);
  page.phase('Setup'); await page.relayStatus();
  assert.equal(page.get('choosePhoto').disabled, false);
  page.phase('Paper'); await page.get('start').onclick(); await settle();
  page.offline(true); await page.relayStatus();
  assert.equal(page.get('start').disabled, true);
  assert.equal(page.get('preview').hidden, true);
});


test('phase-ended guidance preserves pending-photo warning until durable acknowledgement', async () => {
  const page = await camera();
  page.offline(true);
  page.get('chooseInput').files = [{type:'image/png'}];
  await page.get('chooseInput').onchange(); await settle();
  await page.get('sendPhoto').onclick(); await settle();
  const frame = [...page.rows.values()].find(row => row.kind === 'frame');
  assert.ok(frame);
  // Phase requests recover, but uploads remain unavailable.
  page.offline(false);page.uploadFailure(true);page.phase('Coding');
  await page.relayStatus();await page.flush();
  assert.ok(page.rows.has(frame.id));
  assert.match(page.get('status').textContent, /paper phase has ended/);
  assert.match(page.get('manualStatus').textContent, /Keep this page open/);
  assert.match(page.get('manualStatus').textContent, /Upload will retry/);
  assert.doesNotMatch(page.get('status').textContent, /Return to your JupyterLab notebook/);
  page.uploadFailure(false);page.wrongAck(true);await page.flush();
  assert.ok(page.rows.has(frame.id));
  assert.match(page.get('manualStatus').textContent, /Keep this page open/);
  page.wrongAck(false);await page.flush();
  assert.ok(!page.rows.has(frame.id));
  assert.match(page.get('status').textContent, /Return to your JupyterLab notebook/);
  assert.doesNotMatch(page.get('manualStatus').textContent, /Keep this page open/);
});


test('expired camera link explains how to reopen it instead of waiting indefinitely', async () => {
  const page = await camera(new Map(), false, {phaseDenied:true});
  assert.equal(page.get('start').disabled, true);
  assert.equal(page.get('choosePhoto').disabled, true);
  assert.match(page.get('status').textContent, /link has expired or is invalid/);
  assert.match(page.get('status').textContent, /Reopen Open paper tutor/);
  assert.doesNotMatch(page.get('status').textContent, /Checking the activity phase/);
  page.phaseDenied(false);await page.relayStatus();
  assert.equal(page.get('choosePhoto').disabled, false);
  assert.doesNotMatch(page.get('status').textContent, /expired/);
});

test('phase network failure explains automatic retry and recovers controls', async () => {
  const page = await camera();
  page.offline(true);await page.relayStatus();
  assert.equal(page.get('start').disabled, true);
  assert.match(page.get('status').textContent, /Cannot check the activity phase/);
  assert.match(page.get('status').textContent, /retrying automatically/);
  page.offline(false);await page.relayStatus();
  assert.equal(page.get('start').disabled, false);
  assert.doesNotMatch(page.get('status').textContent, /Cannot check/);
});


test('camera permission denial survives successful uploads and explains photo fallback', async () => {
  const page = await camera(new Map(), false, {cameraError:'NotAllowedError'});
  await page.get('start').onclick();await settle();
  await page.flush();await page.relayStatus();
  assert.match(page.get('status').textContent, /Camera access was denied/);
  assert.match(page.get('status').textContent, /Take a photo or Choose an existing photo/);
  assert.equal(page.get('takePhoto').disabled, false);
  assert.equal(page.get('choosePhoto').disabled, false);
  assert.equal(page.rows.size, 0, 'permission-error event was acknowledged without hiding the camera failure');
  await page.flush();
  assert.match(page.get('status').textContent, /Camera access was denied/);
  page.cameraError(null);await page.get('start').onclick();await settle();
  assert.equal(page.get('preview').hidden, false);
  assert.match(page.get('status').textContent, /Camera running/);
  assert.doesNotMatch(page.get('status').textContent, /denied/);
});


test('setup photo acknowledgement directs students to the preview without promising a reply', async () => {
  const page = await camera(new Map(), false, {phase:'Setup'});
  page.get('chooseInput').files = [{type:'image/png'}];
  await page.get('chooseInput').onchange();await settle();
  await page.get('sendPhoto').onclick();await settle();
  assert.match(page.get('manualStatus').textContent, /Delivering it to your Paper tutor page/);
  await page.relayStatus();
  assert.match(page.get('manualStatus').textContent, /Check the preview on your Paper tutor page/);
  assert.doesNotMatch(page.get('manualStatus').textContent, /feedback|reply|response|Waiting for the tutor/i);
});


test('phone feedback decodes speech, ignores image envelopes, and stays connected without capture', async () => {
  const page = await camera();
  const receive = page.socketEvents.update_private_chat;
  assert.equal(typeof receive, 'function', 'single-photo users connect before starting camera');
  receive('Camera_1', 'OPEBot', 'multimodal:::true;%;from:::OPEBot;%;to:::Camera_1;%;speech:::Check s[i].');
  assert.equal(page.feed[0], 'OPEBot: Check s[i].');
  receive('Camera_1', 'OPEBot', 'cameraImageUpdate:::true;%;image:::secret-base64');
  assert.equal(page.feed.length, 1);
  await page.get('start').onclick();await settle();
  page.get('stop').onclick();await settle();
  assert.equal(page.disconnects, 0, 'stopping capture keeps tutor feedback connected');
  receive('Camera_1', 'OPEBot', 'A plain reply');
  assert.equal(page.feed[1], 'OPEBot: A plain reply');
  page.windowEvents.pagehide();
  assert.equal(page.disconnects, 1);
});


test('automatic orientation is applied before the JPEG enters the durable queue', async()=>{
  const page=await camera(new Map(),false,{orientation:{rotation_degrees:90,confidence:'high',readable:true}});
  page.uploadFailure(true);
  await page.get('start').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.width,720);assert.equal(frame.body.height,960);
  assert.deepEqual(frame.body.orientation,{method:'automatic',rotation_degrees:90,confidence:'high',readable:true});
  assert.equal(page.calls.filter(call=>call.url.endsWith('/orientation')).length,1);
});

test('uncertain orientation holds capture; manual angle resumes without another model request', async()=>{
  const page=await camera(new Map(),false,{orientation:{rotation_degrees:180,confidence:'low',readable:true}});
  page.uploadFailure(true);
  await page.get('start').onclick();await settle();
  assert.ok(![...page.rows.values()].some(row=>row.kind==='frame'));
  assert.match(page.get('orientationStatus').textContent,/paused/);
  page.get('cameraAngle').value='-90';page.get('cameraAngle').onchange();
  page.get('useCameraAngle').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.orientation.rotation_degrees,-90);
  assert.equal(frame.body.orientation.method,'manual');
  assert.equal(page.calls.filter(call=>call.url.endsWith('/orientation')).length,1);
  page.intervals.get(10000)();await settle();
  assert.equal([...page.rows.values()].filter(row=>row.kind==='frame').length,2);
});

test('orientation outage does not archive an uncorrected continuous frame', async()=>{
  const page=await camera();page.orientationFailure(true);
  await page.get('start').onclick();await settle();
  assert.ok(!page.calls.some(call=>call.url.endsWith('/frame')));
  assert.equal(page.get('useCameraAngle').disabled,false);
});

test('late orientation response is discarded after stopping capture', async()=>{
  let resolve;
  const wait=new Promise(done=>resolve=done);
  const page=await camera(new Map(),false,{orientationWait:wait});
  const starting=page.get('start').onclick();await settle();
  page.get('stop').onclick();resolve();await starting;await settle();
  assert.ok(!page.calls.some(call=>call.url.endsWith('/frame')));
  assert.equal(page.get('correctedPreview').hidden,true);
  assert.ok(!page.intervals.has(10000),'stopped capture does not restart its timer');
});

test('manual photo can override an automatic angle before queueing', async()=>{
  const page=await camera(new Map(),false,{orientation:{rotation_degrees:180,confidence:'high',readable:true}});
  page.uploadFailure(true);page.get('chooseInput').files=[{type:'image/jpeg'}];
  page.get('chooseInput').onchange();await settle();
  page.get('photoLeft').onclick();
  await page.get('sendPhoto').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.orientation.rotation_degrees,90);
  assert.equal(frame.body.orientation.method,'manual');
  assert.equal(frame.body.width,900);assert.equal(frame.body.height,1200);
});

test('phase ending during orientation prevents that frame from being uploaded',async()=>{
  let resolve;const wait=new Promise(done=>resolve=done);
  const page=await camera(new Map(),false,{orientationWait:wait});
  const starting=page.get('start').onclick();await settle();
  page.phase('Coding');await page.relayStatus();resolve();await starting;await settle();
  assert.ok(!page.calls.some(call=>call.url.endsWith('/frame')));
  assert.equal(page.get('start').disabled,true);
});

test('a manually confirmed photo can queue while automatic detection and uploads are unavailable',async()=>{
  const page=await camera();page.orientationFailure(true);page.offline(true);
  page.get('chooseInput').files=[{type:'image/jpeg'}];page.get('chooseInput').onchange();await settle();
  assert.match(page.get('manualStatus').textContent,/unavailable/);
  page.get('photoRight').onclick();await page.get('sendPhoto').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.orientation.method,'manual');
  assert.equal(frame.body.orientation.rotation_degrees,90);
});

test('rotation bounds contain all four image corners for quarter turns and arbitrary tilt',()=>{
  const helper=source.split('/* Versioned Dev camera')[0];
  const draw=vm.runInNewContext(helper+';drawOrientedImage');
  for(const angle of [0,90,180,-90,17,-32.5]){
    let translate,rotate,rectangle;
    const canvas={getContext:()=>({fillRect(){},translate(x,y){translate=[x,y];},rotate(a){rotate=a;},drawImage(...args){rectangle=args.slice(1);}})};
    draw(canvas,{width:1280,height:720},angle,960);
    const [left,top,width,height]=rectangle;
    for(const [x,y] of [[left,top],[left+width,top],[left,top+height],[left+width,top+height]]){
      const u=x*Math.cos(rotate)-y*Math.sin(rotate)+translate[0];
      const v=x*Math.sin(rotate)+y*Math.cos(rotate)+translate[1];
      assert.ok(u>=-1e-8&&u<=canvas.width+1e-8&&v>=-1e-8&&v<=canvas.height+1e-8,'corner is preserved at '+angle);
    }
    assert.ok(Math.max(canvas.width,canvas.height)<=960);
    if(angle===90){assert.equal(canvas.width,540);assert.equal(canvas.height,960);assert.ok(rotate>0);}
  }
});


test('blank frames stay unrotated and capture rechecks for writing on the next frame',async()=>{
  const blank={rotation_degrees:0,confidence:'high',readable:false,writing_present:false};
  const page=await camera(new Map(),false,{orientation:blank});page.uploadFailure(true);
  await page.get('start').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.ok(frame,'blank frame is queued without manual intervention');
  assert.equal(frame.body.width,960);assert.equal(frame.body.height,720);
  assert.deepEqual(frame.body.orientation,{method:'automatic',...blank});
  assert.match(page.get('orientationStatus').textContent,/No writing detected/);
  page.orientation({rotation_degrees:90,confidence:'high',readable:true,writing_present:true});
  page.intervals.get(10000)();await settle();
  const frames=[...page.rows.values()].filter(row=>row.kind==='frame');
  assert.equal(frames.length,2);assert.equal(frames[1].body.orientation.rotation_degrees,90);
  assert.equal(frames[1].body.orientation.writing_present,true);
  assert.equal(page.calls.filter(call=>call.url.endsWith('/orientation')).length,2);
});

test('obscured writing and uncertain blank classification hold capture without rotation',async()=>{
  for(const result of [
    {rotation_degrees:90,confidence:'high',readable:false,writing_present:true},
    {rotation_degrees:0,confidence:'low',readable:false,writing_present:false},
    {rotation_degrees:90,confidence:'high',readable:false,writing_present:false}
  ]){
    const page=await camera(new Map(),false,{orientation:result});
    await page.get('start').onclick();await settle();
    assert.ok(!page.calls.some(call=>call.url.endsWith('/frame')));
    assert.equal(page.get('cameraAngle').value,'0');
    assert.match(page.get('orientationStatus').textContent,/paused/);
  }
});

test('blank selected photos stay unrotated and preserve presence metadata on Send',async()=>{
  const blank={rotation_degrees:0,confidence:'high',readable:false,writing_present:false};
  const page=await camera(new Map(),false,{orientation:blank});page.uploadFailure(true);
  page.get('chooseInput').files=[{type:'image/jpeg'}];page.get('chooseInput').onchange();await settle();
  assert.match(page.get('manualStatus').textContent,/No writing detected/);
  await page.get('sendPhoto').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.deepEqual(frame.body.orientation,{method:'automatic',...blank});
});

test('late provider results never become automatic corrections or archived frames',async()=>{
  const page=await camera(new Map(),false,{orientationElapsedMs:5000,
    orientation:{rotation_degrees:90,confidence:'high',readable:true,writing_present:true}});
  await page.get('start').onclick();await settle();
  assert.ok(!page.calls.some(call=>call.url.endsWith('/frame')));
  assert.equal(page.get('cameraAngle').value,'0');
  assert.match(page.get('orientationStatus').textContent,/paused/);
});

test('continuous JPEG encoding is included in the correction deadline',async()=>{
  const page=await camera(new Map(),false,{orientationElapsedMs:4500,renderElapsedMs:500,
    orientation:{rotation_degrees:90,confidence:'high',readable:true,writing_present:true}});
  await page.get('start').onclick();await settle();
  assert.ok(!page.calls.some(call=>call.url.endsWith('/frame')));
  assert.equal(page.get('cameraAngle').value,'0');
  assert.match(page.get('orientationStatus').textContent,/exceeded five seconds/);
});

test('late selected-photo detection leaves the photo unrotated for manual review',async()=>{
  const page=await camera(new Map(),false,{orientationElapsedMs:5001,
    orientation:{rotation_degrees:90,confidence:'high',readable:true,writing_present:true}});
  page.get('chooseInput').files=[{type:'image/jpeg'}];page.get('chooseInput').onchange();await settle();
  assert.equal(page.get('photoAngle').value,'0');
  assert.match(page.get('manualStatus').textContent,/exceeded five seconds/);
});


test('automatic corrections taking more than two seconds can succeed before five seconds',async()=>{
  const page=await camera(new Map(),false,{orientationElapsedMs:4200,renderElapsedMs:300,
    orientation:{rotation_degrees:90,confidence:'high',readable:true,writing_present:true}});
  page.uploadFailure(true);await page.get('start').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.orientation.method,'automatic');
  assert.equal(frame.body.orientation.rotation_degrees,90);
});

test('manual camera controls remain available during detection and late results cannot override them',async()=>{
  let resolve;const wait=new Promise(done=>resolve=done);
  const page=await camera(new Map(),false,{orientationWait:wait,
    orientation:{rotation_degrees:180,confidence:'high',readable:true,writing_present:true}});
  page.uploadFailure(true);const starting=page.get('start').onclick();await settle();
  for(const id of ['cameraLeft','cameraRight','cameraAngle','useCameraAngle'])assert.equal(page.get(id).disabled,false,id);
  page.get('cameraRight').onclick();page.get('useCameraAngle').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.orientation.method,'manual');assert.equal(frame.body.orientation.rotation_degrees,90);
  resolve();await starting;await settle();
  assert.equal(page.get('cameraAngle').value,'90');
  assert.equal([...page.rows.values()].filter(row=>row.kind==='frame').length,1);
  assert.ok(page.intervals.has(10000),'manual takeover preserves continuous capture');
  page.intervals.get(10000)();await settle();
  assert.equal([...page.rows.values()].filter(row=>row.kind==='frame').length,2);
});

test('manual photo controls and Send remain available during detection and preserve the manual angle',async()=>{
  let resolve;const wait=new Promise(done=>resolve=done);
  const page=await camera(new Map(),false,{orientationWait:wait,
    orientation:{rotation_degrees:180,confidence:'high',readable:true,writing_present:true}});
  page.uploadFailure(true);page.get('chooseInput').files=[{type:'image/jpeg'}];
  page.get('chooseInput').onchange();await settle();
  for(const id of ['photoLeft','photoRight','photoAngle','sendPhoto'])assert.equal(page.get(id).disabled,false,id);
  page.get('photoLeft').onclick();resolve();await settle();
  assert.equal(page.get('photoAngle').value,'-90');
  await page.get('sendPhoto').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.orientation.method,'manual');assert.equal(frame.body.orientation.rotation_degrees,-90);
});

test('sending a manually reviewed photo before detection finishes invalidates the pending result',async()=>{
  let resolve;const wait=new Promise(done=>resolve=done);
  const page=await camera(new Map(),false,{orientationWait:wait});
  page.uploadFailure(true);page.get('chooseInput').files=[{type:'image/jpeg'}];
  page.get('chooseInput').onchange();await settle();
  await page.get('sendPhoto').onclick();await settle();
  const frame=[...page.rows.values()].find(row=>row.kind==='frame');
  assert.equal(frame.body.orientation.method,'manual');
  resolve();await settle();
  assert.equal([...page.rows.values()].filter(row=>row.kind==='frame').length,1);
  assert.equal(page.get('photoCorrection').hidden,true);
});
