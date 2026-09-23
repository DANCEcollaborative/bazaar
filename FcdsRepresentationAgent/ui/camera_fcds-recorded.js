/* Versioned Dev camera: queue locally, acknowledge only after archival on Bree. */
(async () => {
  const $ = id => document.getElementById(id);
  const params = new URLSearchParams(location.search);
  const room = params.get('room'), user = params.get('user');
  const credential = new URLSearchParams(location.hash.slice(1)).get('capture');
  const producer = 'camera:' + crypto.randomUUID();
  let sequence = 0, db, stream, timer, socket, busy = false, taking = false;
  let issue = '', queued = 0, lastSaved = '', stopped = true;
  let selectedPhotoFile = null, manualBusy = false;
  let latestManualFrameId = null, pendingRelayFrameId = null, checkingRelay = false;
  const valid = /^fcds-p2-26-fall-1a-room\d{9}$/.test(room || '') && /^[1-4]$/.test(user || '') && /^\d+\.[a-f0-9]{64}$/.test(credential || '');
  $('start').disabled = !valid;
  $('takePhoto').disabled = $('choosePhoto').disabled = !valid;
  if (!valid) { $('status').textContent = $('manualStatus').textContent = 'Open the personal camera link or scan the QR code from your Paper tutor page.'; return; }
  $('identity').textContent = 'Session ' + room.slice(-3) + ' · User ' + user;
  function draw() {
    $('status').textContent = issue ? issue + '. Keep this page open to retry.' :
      (stopped ? 'Camera stopped. ' : 'Camera running. ') + (lastSaved ? 'Last upload: ' + lastSaved + '.' : 'Ready.');
    $('status').style.background = issue ? '#ffe3dd' : queued ? '#fff2ce' : '#e5f3e9';
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
    await change(s=>s.put({id,time,seq,room,user,kind,body}));queued++;draw();return id;
  }
  async function event(kind,payload={}) { try {await put(kind,payload);} catch {issue='Local storage failed. Capture has stopped';stop(false);draw();} }
  async function flush() {
    if(busy)return;busy=true;
    let pendingManual=0;
    try {
      const all=await records();queued=all.length;
      pendingManual=all.filter(e=>e.kind==='frame' && e.body.capture_mode==='manual').length;
      for(const e of all.slice(0,12)) {
        const response=await fetch('/fcds-recorder/v1/camera/'+(e.kind==='frame'?'frame':'events'),{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Camera '+credential},body:JSON.stringify(e.body),signal:AbortSignal.timeout(15000)});
        if(!response.ok)throw Error(response.status===403?'Camera link expired or invalid; reopen your personal link':'Photo upload unavailable; retrying');
        const result=await response.json();
        if(!result.stored || (e.kind==='frame'?result.frame_id!==e.id:!result.event_ids.includes(e.id)))throw Error('Upload was not confirmed');
        await change(s=>s.delete(e.id));queued--;lastSaved=new Date().toLocaleTimeString();
        if(e.kind==='frame' && e.body.capture_mode==='manual') {
          pendingManual--;
          if(!latestManualFrameId || latestManualFrameId===e.id) {
            latestManualFrameId=e.id;
            pendingRelayFrameId=result.relay_state==='accepted_by_relay' ? null : e.id;
            $('manualStatus').textContent = pendingRelayFrameId
              ? 'Photo uploaded. Waiting for the tutor; you can keep working.'
              : 'Photo sent to the tutor. Check your Paper tutor page for feedback.';
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
    if(!pendingRelayFrameId || checkingRelay)return;
    const frameId=pendingRelayFrameId;checkingRelay=true;
    try {
      const response=await fetch('/fcds-recorder/v1/camera/frame-status',{
        method:'POST',headers:{'Content-Type':'application/json','Authorization':'Camera '+credential},
        body:JSON.stringify({room_id:room,participant_id:user,frame_id:frameId}),signal:AbortSignal.timeout(15000)
      });
      if(!response.ok)return;
      const result=await response.json();
      if(result.stored && result.frame_id===frameId && result.relay_state==='accepted_by_relay' && latestManualFrameId===frameId) {
        pendingRelayFrameId=null;
        $('manualStatus').textContent='Photo sent to the tutor. Check your Paper tutor page for feedback.';
      }
    }catch(_) { /* Archive is durable; retry the status check without re-uploading. */ }
    finally{checkingRelay=false;}
  }
  async function capture() {
    if(taking || !stream)return;taking=true;
    try {
      const pending=await records();
      if(pending.length>=240 || pending.reduce((n,e)=>n+JSON.stringify(e).length,0)>64*1024*1024) {
        await event('capture.paused',{reason:'queue capacity reached'});stop(false);issue='Capture paused because the upload queue is full. Wait for uploads, then restart';draw();return;
      }
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
    $('start').disabled=true;
    try {
      stream=await navigator.mediaDevices.getUserMedia({audio:false,video:{facingMode:{ideal:'environment'},width:{ideal:1280},height:{ideal:720}}});
      $('preview').srcObject=stream;$('preview').hidden=false;await $('preview').play();stopped=false;$('stop').disabled=false;issue='';
      // Relay connectivity affects preview/tutoring, not whether images are archived.
      socket=io('/',{path:'/bazsocket'});
      socket.on('connect',()=>{socket.emit('adduser','fcdsrepresentation'+room,'Camera_'+user,true,'Camera_'+user,null);void event('relay.connected');});
      socket.on('disconnect',reason=>void event('relay.disconnected',{reason}));
      socket.on('connect_error',()=>void event('relay.error'));
      socket.on('update_private_chat',(_to,from,text)=>{if(from==='Camera_'+user)return;const p=document.createElement('p');p.textContent=String(from)+': '+String(text);$('feed').prepend(p);});
      await event('started',{interval_ms:10000,audio:false});await capture();timer=setInterval(()=>void capture(),10000);draw();
    }catch(error){stop(false);issue='Camera access failed: '+error.name;void event('permission_or_start_error',{error_type:error.name});draw();}
  };
  $('stop').onclick=()=>stop();$('problem').onchange=()=>void event('problem.changed',{paper_problem:$('problem').value});
  function discardSelection() {
    selectedPhotoFile=null;
    $('manualPreview').removeAttribute('src');$('manualPreview').hidden=true;
    $('takeInput').value=$('chooseInput').value='';
    $('sendPhoto').disabled=$('discardPhoto').disabled=true;
  }
  $('takePhoto').onclick=()=>{if(stream)stop();$('takeInput').click();};
  $('choosePhoto').onclick=()=>{if(stream)stop();$('chooseInput').click();};
  $('discardPhoto').onclick=()=>{discardSelection();$('manualStatus').textContent='Photo discarded. You can take or choose another.';};
  async function selectPhoto(input) {
    const file=input.files?.[0];if(!file)return;
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
      selectedPhotoFile=file;preview.hidden=false;
      $('sendPhoto').disabled=$('discardPhoto').disabled=false;
      $('manualStatus').textContent='Review the photo. Make sure your writing is readable and no private information is visible, then press Send this photo.';
      await event('manual.selected',{paper_problem:$('problem').value});
    }catch(error){preview.removeAttribute('src');$('manualStatus').textContent='The photo could not be opened. Choose another image.';await event('manual.selection_error',{error_type:error.name || 'ImageLoadError'});}
  }
  $('takeInput').onchange=()=>void selectPhoto($('takeInput'));
  $('chooseInput').onchange=()=>void selectPhoto($('chooseInput'));
  $('sendPhoto').onclick=async()=>{
    if(manualBusy || !selectedPhotoFile)return;
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
      pendingRelayFrameId=null;
      $('manualStatus').textContent='Photo queued on this device. Saving to Bree… Keep this page open.';
      discardSelection();await event('manual.submitted',{paper_problem:$('problem').value});void flush();
    }catch(error){$('manualStatus').textContent=error.message || 'Could not queue the photo. Try again.';if(error.name!=='QueueFullError')await event('manual.send_error',{error_type:error.name});$('sendPhoto').disabled=!selectedPhotoFile;}
    finally{manualBusy=false;}
  };
  window.addEventListener('online',()=>{void event('online');void flush();});window.addEventListener('offline',()=>{issue='Offline; records are queued on this phone';void event('offline');draw();});
  window.addEventListener('pagehide',()=>stop());document.addEventListener('visibilitychange',()=>void event('visibility',{state:document.visibilityState}));
  await event('page.opened',{version:'0.1.0',user_agent:navigator.userAgent});setInterval(()=>void flush(),2000);setInterval(()=>void checkManualRelay(),3000);void flush();
})();
