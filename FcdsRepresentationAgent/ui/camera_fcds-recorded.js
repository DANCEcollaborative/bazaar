/* Versioned Dev camera: queue locally, acknowledge only after archival on Bree. */
(async () => {
  const $ = id => document.getElementById(id);
  const params = new URLSearchParams(location.search);
  const room = params.get('room'), user = params.get('user');
  const credential = new URLSearchParams(location.hash.slice(1)).get('capture');
  const producer = 'camera:' + crypto.randomUUID();
  let sequence = 0, db, stream, timer, socket, busy = false, taking = false;
  let issue = '', captureIssue = '', queued = 0, pendingPhotos = 0, lastSaved = '', stopped = true;
  let selectedPhotoFile = null, manualBusy = false;
  let latestManualFrameId = null, checkingRelay = false;
  let phase = null, phaseIssue = '', paperEnded = false, checkingPhase = false;
  const valid = /^fcds-p2-26-fall-1a-room\d{9}$/.test(room || '') && /^[1-4]$/.test(user || '') && /^\d+\.[a-f0-9]{64}$/.test(credential || '');
  $('start').disabled = $('takePhoto').disabled = $('choosePhoto').disabled = true;
  if (!valid) { $('status').textContent = $('manualStatus').textContent = 'Open the personal camera link or scan the QR code from your Paper tutor page.'; return; }
  $('identity').textContent = 'Session ' + room.slice(-3) + ' · User ' + user;
  function cameraAllowed() { return !paperEnded && (phase === 'Setup' || phase === 'Paper'); }
  function draw() {
    const allowed = cameraAllowed();
    $('start').disabled = !allowed || !stopped;
    $('takePhoto').disabled = $('choosePhoto').disabled = !allowed;
    $('sendPhoto').disabled = !allowed || manualBusy || !selectedPhotoFile;
    if (paperEnded) {
      if (pendingPhotos > 0) {
        $('status').textContent = 'The paper phase has ended. Capture has stopped.';
        $('manualStatus').textContent = 'Your photo upload is not finished. Keep this page open until the pending photos have uploaded.' + (issue ? ' Upload will retry: ' + issue + '.' : ' Uploading…') + ' These photos will not receive tutor feedback.';
      } else {
        $('status').textContent = $('manualStatus').textContent = 'The paper phase has ended. Return to your JupyterLab notebook for the next step. New photos will not receive tutor feedback.';
      }
    } else if (!allowed) {
      $('status').textContent = phaseIssue || 'Checking the activity phase… Photo controls will be available during the paper phase.';
      if(pendingPhotos>0)$('manualStatus').textContent='Your photo upload is not finished. Keep this page open until the pending photos have uploaded.' + (issue ? ' Upload will retry: ' + issue + '.' : '');
    } else {
      const uploadStatus = issue ? issue + '. Keep this page open to retry.' :
        (stopped ? 'Camera stopped. ' : 'Camera running. ') + (lastSaved ? 'Last upload: ' + lastSaved + '.' : 'Ready.');
      $('status').textContent = captureIssue ? captureIssue + (issue ? ' ' + uploadStatus : '') : uploadStatus;
    }
    $('status').style.background = issue || captureIssue || phaseIssue ? '#ffe3dd' : queued ? '#fff2ce' : '#e5f3e9';
  }
  function applyPhase(result) {
    if (result.phase === 'Coding' || result.phase === 'Submit') paperEnded = true;
    phase = ['Setup','Paper','Coding','Submit'].includes(result.phase) ? result.phase : null;
    if(phase)phaseIssue='';
    if (!cameraAllowed() && stream) stop(false);
    draw();
  }
  async function refreshPhase() {
    if (checkingPhase) return;
    checkingPhase = true;
    try {
      const response = await fetch('/fcds-recorder/v1/camera/phase', {
        method:'POST', headers:{'Content-Type':'application/json','Authorization':'Camera '+credential},
        body:JSON.stringify({room_id:room,participant_id:user}), signal:AbortSignal.timeout(15000)
      });
      if (!response.ok) {
        if(response.status===403) {
          phaseIssue='This camera link has expired or is invalid. Reopen Open paper tutor in JupyterLab and scan its QR code again.';
          applyPhase({phase:null});return;
        }
        throw Error('Phase unavailable');
      }
      const result=await response.json();phaseIssue='';applyPhase(result);
    } catch (_) {
      phaseIssue='Cannot check the activity phase. Check your connection; retrying automatically.';
      applyPhase({phase:null});
    }
    finally { checkingPhase = false; }
  }
  db = await new Promise((resolve,reject) => {
    const r = indexedDB.open('fcds-camera-records',1);
    r.onupgradeneeded = () => r.result.createObjectStore('queue',{keyPath:'id'});
    r.onsuccess = () => resolve(r.result); r.onerror = () => reject(r.error);
  }).catch(() => null);
  if (!db) { $('status').textContent = $('manualStatus').textContent = 'Browser storage is unavailable. Use another browser before sending images.'; $('start').disabled=$('takePhoto').disabled=$('choosePhoto').disabled=true; return; }
  function change(action) { return new Promise((resolve,reject) => {
    const tx=db.transaction('queue','readwrite');action(tx.objectStore('queue'));
    tx.oncomplete=resolve;tx.onerror=()=>reject(tx.error);tx.onabort=()=>reject(tx.error);
  }); }
  function records() { return new Promise((resolve,reject) => {
    const r=db.transaction('queue').objectStore('queue').getAll();
    r.onsuccess=()=>resolve(r.result.filter(e=>e.room===room && e.user===user).sort((a,b)=>a.time.localeCompare(b.time)||a.seq-b.seq));r.onerror=()=>reject(r.error);
  }); }
  async function put(kind,payload) {
    const id=crypto.randomUUID(), time=new Date().toISOString(), seq=++sequence;
    const body=kind==='frame' ? {room_id:room,participant_id:user,frame_id:id,producer_id:producer,producer_sequence:seq,captured_at:time,...payload} :
      {room_id:room,participant_id:user,events:[{event_id:id,producer_id:producer,producer_sequence:seq,occurred_at:time,event_type:'camera.'+kind,payload}]};
    await change(s=>s.put({id,time,seq,room,user,kind,body}));queued++;if(kind==='frame')pendingPhotos++;draw();return id;
  }
  async function event(kind,payload={}) { try {await put(kind,payload);} catch {issue='Local storage failed. Capture has stopped';stop(false);draw();} }
  function relayMessage(result) {
    if (paperEnded || result.relay_state === 'phase_closed') return;
    if (!cameraAllowed()) { $('manualStatus').textContent = 'Photo uploaded. Checking delivery to your Paper tutor page…'; return; }
    $('manualStatus').textContent = result.relay_state === 'accepted_by_relay'
      ? 'Photo sent to the tutor. Check the preview on your Paper tutor page.'
      : 'Photo uploaded. Delivering it to your Paper tutor page…';
  }
  async function flush() {
    if(busy)return;busy=true;
    let pendingManual=0;
    try {
      const all=(await records()).filter(e=>e.kind!=='relay');queued=all.length;pendingPhotos=all.filter(e=>e.kind==='frame').length;
      pendingManual=all.filter(e=>e.kind==='frame' && e.body.capture_mode==='manual').length;
      for(const e of all.slice(0,12)) {
        const response=await fetch('/fcds-recorder/v1/camera/'+(e.kind==='frame'?'frame':'events'),{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Camera '+credential},body:JSON.stringify(e.body),signal:AbortSignal.timeout(15000)});
        if(!response.ok)throw Error(response.status===403?'Camera link expired or invalid; reopen your personal link':'Photo upload unavailable; retrying');
        const result=await response.json();
        if(!result.stored || (e.kind==='frame'?result.frame_id!==e.id:!result.event_ids.includes(e.id)))throw Error('Upload was not confirmed');
        if(e.kind==='frame') applyPhase(result);
        const manual=e.kind==='frame' && e.body.capture_mode==='manual';
        const pendingRelay=manual && result.relay_state!=='accepted_by_relay' && result.relay_state!=='phase_closed';
        // Replace the image with a small durable receipt in one transaction.
        // A reload after archival must continue checking delivery, not lose it.
        await change(s=>pendingRelay ? s.put({id:e.id,time:e.time,seq:e.seq,room,user,kind:'relay',body:{frame_id:e.id}}) : s.delete(e.id));
        queued--;if(e.kind==='frame')pendingPhotos--;lastSaved=new Date().toLocaleTimeString();
        if(manual) {
          pendingManual--;
          if(!latestManualFrameId || latestManualFrameId===e.id) {
            latestManualFrameId=e.id;relayMessage(result);
          }
        }
      }
      issue='';
    }catch(error){
      issue=error.message || 'Connection unavailable; retrying';
      if(pendingManual>0)$('manualStatus').textContent='Your photo has not finished uploading. Upload will retry: '+issue+'. Keep this page open.';
    }
    finally{busy=false;draw();}
  }
  async function checkManualRelay() {
    if(checkingRelay)return;
    checkingRelay=true;
    try {
      for(const receipt of (await records()).filter(e=>e.kind==='relay')) {
        const frameId=receipt.id;
        const response=await fetch('/fcds-recorder/v1/camera/frame-status',{
          method:'POST',headers:{'Content-Type':'application/json','Authorization':'Camera '+credential},
          body:JSON.stringify({room_id:room,participant_id:user,frame_id:frameId}),signal:AbortSignal.timeout(15000)
        });
        if(!response.ok)continue;
        const result=await response.json();
        if(!result.stored || result.frame_id!==frameId)continue;
        applyPhase(result);
        if(result.relay_state==='accepted_by_relay' || result.relay_state==='phase_closed') await change(s=>s.delete(frameId));
        if(latestManualFrameId===frameId)relayMessage(result);
      }
    }catch(_) { /* Keep durable receipts for the next check or page reload. */ }
    finally{checkingRelay=false;draw();}
  }
  async function capture() {
    if(taking || !stream || !cameraAllowed())return;taking=true;
    try {
      const pending=await records();
      if(pending.length>=240 || pending.reduce((n,e)=>n+JSON.stringify(e).length,0)>64*1024*1024) {
        await event('capture.paused',{reason:'queue capacity reached'});stop(false);issue='Capture paused because the upload queue is full. Wait for uploads, then restart';draw();return;
      }
      if(!stream || !cameraAllowed())return;
      const video=$('preview');
      if(!video.videoWidth || video.readyState<2){await event('capture.skipped',{reason:'video not ready'});return;}
      const canvas=$('canvas'),scale=Math.min(1,960/video.videoWidth);
      canvas.width=Math.round(video.videoWidth*scale);canvas.height=Math.round(video.videoHeight*scale);
      canvas.getContext('2d').drawImage(video,0,0,canvas.width,canvas.height);
      await put('frame',{imageBase64:canvas.toDataURL('image/jpeg',0.65).split(',')[1],mimeType:'image/jpeg',width:canvas.width,height:canvas.height,paper_problem:$('problem').value,capture_mode:'continuous'});void flush();
    }catch(error){issue='Frame could not be queued; capture stopped';await event('capture.error',{error_type:error.name});stop(false);draw();}
    finally{taking=false;}
  }
  function stop(record=true) {
    clearInterval(timer);stream?.getTracks().forEach(t=>t.stop());stream=null;$('preview').srcObject=null;$('preview').hidden=true;socket?.disconnect();socket=null;stopped=true;
    $('start').disabled=false;$('stop').disabled=true;if(record)void event('stopped');draw();void flush();
  }
  $('start').onclick=async()=>{
    if(!cameraAllowed())return;
    captureIssue='';$('start').disabled=true;
    try {
      stream=await navigator.mediaDevices.getUserMedia({audio:false,video:{facingMode:{ideal:'environment'},width:{ideal:1280},height:{ideal:720}}});
      if(!cameraAllowed()){stop(false);return;}
      $('preview').srcObject=stream;$('preview').hidden=false;await $('preview').play();stopped=false;$('stop').disabled=false;captureIssue='';
      // Relay connectivity affects preview/tutoring, not whether images are archived.
      socket=io('/',{path:'/bazsocket'});
      socket.on('connect',()=>{socket.emit('adduser','fcdsrepresentation'+room,'Camera_'+user,true,'Camera_'+user,null);void event('relay.connected');});
      socket.on('disconnect',reason=>void event('relay.disconnected',{reason}));
      socket.on('connect_error',()=>void event('relay.error'));
      socket.on('update_private_chat',(_to,from,text)=>{if(from==='Camera_'+user)return;const p=document.createElement('p');p.textContent=String(from)+': '+String(text);$('feed').prepend(p);});
      await event('started',{interval_ms:10000,audio:false});await capture();timer=setInterval(()=>void capture(),10000);draw();
    }catch(error){
      stop(false);
      captureIssue=error.name==='NotAllowedError'
        ? 'Camera access was denied. Allow camera access in your browser, or use Take a photo or Choose an existing photo below.'
        : 'The camera could not start. Try Start camera again, or use Take a photo or Choose an existing photo below.';
      void event('permission_or_start_error',{error_type:error.name});draw();
    }
  };
  $('stop').onclick=()=>stop();$('problem').onchange=()=>void event('problem.changed',{paper_problem:$('problem').value});
  function discardSelection() {
    selectedPhotoFile=null;
    $('manualPreview').removeAttribute('src');$('manualPreview').hidden=true;
    $('takeInput').value=$('chooseInput').value='';
    $('sendPhoto').disabled=$('discardPhoto').disabled=true;
  }
  $('takePhoto').onclick=()=>{if(!cameraAllowed())return;if(stream)stop();$('takeInput').click();};
  $('choosePhoto').onclick=()=>{if(!cameraAllowed())return;if(stream)stop();$('chooseInput').click();};
  $('discardPhoto').onclick=()=>{discardSelection();$('manualStatus').textContent='Photo discarded. You can take or choose another.';draw();};
  async function selectPhoto(input) {
    const file=input.files?.[0];if(!file || !cameraAllowed())return;
    discardSelection();
    if(!file.type.startsWith('image/')) {$('manualStatus').textContent='Choose an image file, then try again.';return;}
    if(file.size>25*1024*1024) {$('manualStatus').textContent='This file is too large to open. Choose a smaller photo.';return;}
    const preview=$('manualPreview');
    try {
      // Bree allows data: images in its CSP, but blocks blob: object URLs.
      const dataUrl=await new Promise((resolve,reject)=>{
        const reader=new FileReader();reader.onload=()=>resolve(reader.result);
        reader.onerror=()=>reject(reader.error || Error('Could not read the file'));
        reader.readAsDataURL(file);
      });
      await new Promise((resolve,reject)=>{preview.onload=resolve;preview.onerror=()=>reject(Error('Could not display the photo'));preview.src=dataUrl;});
      if(!preview.naturalWidth || !preview.naturalHeight)throw Error('The selected image could not be read');
      if(!cameraAllowed()){discardSelection();draw();return;}
      selectedPhotoFile=file;preview.hidden=false;
      $('sendPhoto').disabled=$('discardPhoto').disabled=false;
      $('manualStatus').textContent='Review the photo. Make sure your writing is readable and no private information is visible, then press Send this photo.';
      await event('manual.selected',{paper_problem:$('problem').value});
    }catch(error){preview.removeAttribute('src');$('manualStatus').textContent='The photo could not be opened. Choose another image.';await event('manual.selection_error',{error_type:error.name || 'ImageLoadError'});}
  }
  $('takeInput').onchange=()=>void selectPhoto($('takeInput'));
  $('chooseInput').onchange=()=>void selectPhoto($('chooseInput'));
  $('sendPhoto').onclick=async()=>{
    if(manualBusy || !selectedPhotoFile || !cameraAllowed())return;
    manualBusy=true;$('sendPhoto').disabled=true;
    try {
      const pending=await records();
      if(pending.length>=240 || pending.reduce((n,e)=>n+JSON.stringify(e).length,0)>64*1024*1024) {
        const full=Error('The upload queue is full. Keep this page open until earlier records are saved, then try again.');full.name='QueueFullError';throw full;
      }
      const preview=$('manualPreview'),canvas=document.createElement('canvas');
      let maxSide=1600,quality=0.75,imageBase64;
      for(let attempt=0;attempt<3;attempt++) {
        const scale=Math.min(1,maxSide/Math.max(preview.naturalWidth,preview.naturalHeight));
        canvas.width=Math.round(preview.naturalWidth*scale);canvas.height=Math.round(preview.naturalHeight*scale);
        canvas.getContext('2d').drawImage(preview,0,0,canvas.width,canvas.height);
        imageBase64=canvas.toDataURL('image/jpeg',quality).split(',')[1];
        if(imageBase64 && imageBase64.length*3/4<=2*1024*1024)break;
        maxSide=Math.round(maxSide*0.75);quality=0.65;
      }
      if(!imageBase64 || imageBase64.length*3/4>2*1024*1024)throw Error('This photo is too large to save. Choose a smaller photo.');
      latestManualFrameId=await put('frame',{imageBase64,mimeType:'image/jpeg',width:canvas.width,height:canvas.height,paper_problem:$('problem').value,capture_mode:'manual'});
      $('manualStatus').textContent='Photo uploading… Keep this page open.';
      discardSelection();await event('manual.submitted',{paper_problem:$('problem').value});void flush();
    }catch(error){$('manualStatus').textContent=error.message || 'Could not queue the photo. Try again.';if(error.name!=='QueueFullError')await event('manual.send_error',{error_type:error.name});$('sendPhoto').disabled=!selectedPhotoFile;}
    finally{manualBusy=false;draw();}
  };
  window.addEventListener('online',()=>{void event('online');void flush();});window.addEventListener('offline',()=>{issue='Offline; records are queued on this phone';void event('offline');draw();});
  window.addEventListener('pagehide',()=>stop());document.addEventListener('visibilitychange',()=>void event('visibility',{state:document.visibilityState}));
  const queuedRecords=await records();
  pendingPhotos=queuedRecords.filter(e=>e.kind==='frame').length;
  const existing=queuedRecords.filter(e=>e.kind==='relay' || (e.kind==='frame' && e.body.capture_mode==='manual'));
  if(existing.length) {
    const latest=existing[existing.length-1];latestManualFrameId=latest.id;
    $('manualStatus').textContent=latest.kind==='relay' ? 'Photo uploaded. Checking delivery to your Paper tutor page…' : 'Your previous photo is waiting to upload. Keep this page open.';
  }
  await refreshPhase();
  await event('page.opened',{version:'0.1.0',user_agent:navigator.userAgent});setInterval(()=>void flush(),2000);
  setInterval(()=>void refreshPhase().then(checkManualRelay),3000);void flush();void checkManualRelay();
})();
