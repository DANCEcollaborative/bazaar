const assert = require('node:assert/strict');
const { test } = require('node:test');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const { webcrypto } = require('node:crypto');

const source = fs.readFileSync(path.join(__dirname, '../ui/camera_fcds-recorded.js'), 'utf8');
const room = 'fcds-p2-26-fall-1a-room260911999';
const credential = '2000000000.' + 'a'.repeat(64);
const settle = async () => { for (let n = 0; n < 30; n++) await Promise.resolve(); };

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

async function camera(rows = new Map(), storageFails = false) {
  const elements = {}, intervals = new Map(), calls = [];
  let failure = false, wrongAck = false, stoppedTracks = 0;
  const get = id => elements[id] ||= {
    style: {}, value: '1', videoWidth: 960, videoHeight: 720, readyState: 2,
    naturalWidth: 1200, naturalHeight: 900, removeAttribute() {}, click() {},
    play: async () => {}, getContext: () => ({ drawImage() {} }),
    toDataURL: () => 'data:image/jpeg;base64,c3ludGhldGlj', prepend() {},
    set src(value) { this._src = value; queueMicrotask(() => this.onload?.()); },
    get src() { return this._src; }
  };
  const db = fakeDatabase(rows);
  const windowEvents = {}, documentEvents = {};
  const context = {
    URLSearchParams, Date, JSON, Promise, Error, String, Math, crypto: webcrypto,
    FileReader: class { readAsDataURL() { queueMicrotask(() => { this.result = 'data:image/png;base64,c3ludGhldGlj'; this.onload?.(); }); } },
    location: { search: '?room=' + room + '&user=1', hash: '#capture=' + credential },
    document: { getElementById: get, createElement: name => get(name),
      addEventListener: (name, fn) => { documentEvents[name] = fn; } },
    window: { addEventListener: (name, fn) => { windowEvents[name] = fn; } },
    navigator: { userAgent: 'synthetic test', mediaDevices: { getUserMedia: async () => ({
      getTracks: () => [{ stop() { stoppedTracks++; } }]
    }) } },
    indexedDB: { open() { const request = {}; queueMicrotask(() => {
      if (storageFails) request.onerror?.();
      else { request.result = db; request.onsuccess?.(); }
    }); return request; } },
    io: () => ({ on() {}, disconnect() {} }),
    setInterval: (fn, delay) => { intervals.set(delay, fn); return delay; },
    clearInterval: delay => intervals.delete(delay),
    AbortSignal: { timeout: () => undefined },
    fetch: async (url, options) => {
      const body = JSON.parse(options.body); calls.push({ url, body, headers: options.headers });
      if (failure) throw Error('offline');
      if (url.endsWith('/frame-status')) return { ok: true, json: async () => ({
        stored: true, frame_id: body.frame_id, relay_state: 'accepted_by_relay'
      }) };
      return { ok: true, json: async () => ({ stored: true,
        frame_id: wrongAck ? 'wrong-id' : body.frame_id,
        event_ids: wrongAck ? [] : (body.events || []).map(e => e.event_id) }) };
    }
  };
  await vm.runInNewContext(source, context); await settle();
  return { rows, calls, get, intervals, windowEvents, documentEvents,
    offline(value) { failure = value; }, wrongAck(value) { wrongAck = value; },
    get stoppedTracks() { return stoppedTracks; },
    async flush() { intervals.get(2000)(); await settle(); },
    async relayStatus() { intervals.get(3000)(); await settle(); } };
}

test('camera retains failed/unacknowledged frames, then retries the same ID after reload', async () => {
  const first = await camera();
  assert.equal(first.rows.size, 0, 'initial page event was acknowledged');
  first.offline(true);
  await first.get('start').onclick(); await settle();
  const frame = [...first.rows.values()].find(row => row.kind === 'frame');
  assert.ok(frame, 'frame was durably queued before upload');
  assert.equal(frame.body.participant_id, '1');
  assert.equal(frame.body.room_id, room);
  assert.equal(frame.body.capture_mode, 'continuous');
  first.get('stop').onclick(); await settle();
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
  assert.equal(page.get('manualPreview').hidden, false);
  assert.equal(page.get('sendPhoto').disabled, false);
  await page.get('sendPhoto').onclick(); await settle();
  const frame = [...page.rows.values()].find(row => row.kind === 'frame');
  assert.ok(frame, 'manual image remains queued while offline');
  assert.equal(frame.body.capture_mode, 'manual');
  assert.equal(frame.body.mimeType, 'image/jpeg', 'browser canvas normalizes the chosen image');
  assert.equal(page.get('manualPreview').hidden, true);
  assert.equal(page.stoppedTracks, 0, 'manual submission does not need a continuous camera stream');
  assert.match(page.get('manualStatus').textContent, /queued on this device/i);
  page.offline(false); await page.flush();
  assert.ok(!page.rows.has(frame.id));
  assert.match(page.get('manualStatus').textContent, /saved on Bree and queued for the tutor service/i);
  await page.relayStatus();
  assert.match(page.get('manualStatus').textContent, /sent to the tutor service/i);
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
