/* Versioned Dev camera: queue locally, acknowledge only after archival on Bree. */
(async () => {
  const $ = id => document.getElementById(id);
  const params = new URLSearchParams(location.search);
  const room = params.get('room'), user = params.get('user');
  const credential = new URLSearchParams(location.hash.slice(1)).get('capture');
  const producer = 'camera:' + crypto.randomUUID();
  let sequence = 0, db, stream, timer, socket, busy = false, taking = false;
  let issue = '', queued = 0, lastSaved = '', stopped = true;
  const valid = /^fcds-p2-26-fall-1a-room\d{9}$/.test(room || '') && /^[1-4]$/.test(user || '') && /^\d+\.[a-f0-9]{64}$/.test(credential || '');
  $('start').disabled = !valid;
  if (!valid) { $('status').textContent = 'Open the personal camera link or scan the QR code from your Paper tutor page.'; return; }
  $('identity').textContent = 'Session ' + room.slice(-3) + ' · User ' + user;
  function draw() {
    $('status').textContent = issue ? issue + ' · ' + queued + ' records waiting to be saved. Keep this page open.' :
      queued ? (stopped ? 'Camera stopped. ' : 'Capturing. ') + queued + ' records waiting for Bree.' :
      (stopped ? 'Camera stopped. ' : 'Capturing a still image every 10 seconds. ') + (lastSaved ? 'Saved on Bree at ' + lastSaved + '.' : 'Ready to start.');
    $('status').style.background = issue ? '#ffe3dd' : queued ? '#fff2ce' : '#e5f3e9';
  }
  db = await new Promise((resolve,reject) => {
    const r = indexedDB.open('fcds-camera-records',1);
    r.onupgradeneeded = () => r.result.createObjectStore('queue',{keyPath:'id'});
    r.onsuccess = () => resolve(r.result); r.onerror = () => reject(r.error);
  }).catch(() => null);
  if (!db) { $('status').textContent = 'Browser storage is unavailable. Use another browser before starting capture.'; $('start').disabled=true; return; }
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
    await change(s=>s.put({id,time,seq,room,user,kind,body}));queued++;draw();
  }
  async function event(kind,payload={}) { try {await put(kind,payload);} catch {issue='Local storage failed. Capture has stopped';stop(false);draw();} }
  async function flush() {
    if(busy)return;busy=true;
    try {
      const all=await records();queued=all.length;
      for(const e of all.slice(0,12)) {
        const response=await fetch('/fcds-recorder/v1/camera/'+(e.kind==='frame'?'frame':'events'),{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Camera '+credential},body:JSON.stringify(e.body),signal:AbortSignal.timeout(15000)});
        if(!response.ok)throw Error(response.status===403?'Camera link expired or invalid; reopen your personal link':'Archive unavailable; retrying');
        const result=await response.json();
        if(!result.stored || (e.kind==='frame'?result.frame_id!==e.id:!result.event_ids.includes(e.id)))throw Error('Archive did not confirm this record');
        await change(s=>s.delete(e.id));queued--;lastSaved=new Date().toLocaleTimeString();
      }
      issue='';
    }catch(error){issue=error.message || 'Connection unavailable; retrying';}
    finally{busy=false;draw();}
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
      await put('frame',{imageBase64:canvas.toDataURL('image/jpeg',0.65).split(',')[1],mimeType:'image/jpeg',width:canvas.width,height:canvas.height,paper_problem:$('problem').value});void flush();
    }catch(error){issue='Frame could not be queued; capture stopped';await event('capture.error',{error_type:error.name});stop(false);draw();}
    finally{taking=false;}
  }
  function stop(record=true) {
    clearInterval(timer);stream?.getTracks().forEach(t=>t.stop());stream=null;$('preview').srcObject=null;socket?.disconnect();socket=null;stopped=true;
    $('start').disabled=false;$('stop').disabled=true;if(record)void event('stopped');draw();void flush();
  }
  $('start').onclick=async()=>{
    $('start').disabled=true;
    try {
      stream=await navigator.mediaDevices.getUserMedia({audio:false,video:{facingMode:{ideal:'environment'},width:{ideal:1280},height:{ideal:720}}});
      $('preview').srcObject=stream;await $('preview').play();stopped=false;$('stop').disabled=false;issue='';
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
  window.addEventListener('online',()=>{void event('online');void flush();});window.addEventListener('offline',()=>{issue='Offline; records are queued on this phone';void event('offline');draw();});
  window.addEventListener('pagehide',()=>stop());document.addEventListener('visibilitychange',()=>void event('visibility',{state:document.visibilityState}));
  await event('page.opened',{version:'0.1.0',user_agent:navigator.userAgent});setInterval(()=>void flush(),2000);void flush();
})();
