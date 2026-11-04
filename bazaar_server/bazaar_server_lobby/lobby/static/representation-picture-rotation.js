/* Persist display angles separately from immutable camera images. */
(function(root) {
  function createPictureRotation(options) {
    const tasks=[];
    let current=null, selectedAngle=null, running=false, retry=null;
    const status=options.status || (()=>{});
    const schedule=options.schedule || (fn=>setTimeout(fn,3000));
    const hash=options.hash || (async base64=>{
      const bytes=Uint8Array.from(atob(base64),ch=>ch.charCodeAt(0));
      const digest=await crypto.subtle.digest('SHA-256',bytes);
      return Array.from(new Uint8Array(digest),b=>b.toString(16).padStart(2,'0')).join('');
    });
    const request=options.request || (async body=>{
      const response=await fetch('/fcds-recorder/v1/camera/image-rotation',{
        method:'POST',headers:{'Content-Type':'application/json','Authorization':'Camera '+options.credential},
        body:JSON.stringify({room_id:options.room,participant_id:options.user,...body}),
        signal:AbortSignal.timeout(8000)
      });
      if(!response.ok) {
        const error=Error('Rotation could not be saved');
        error.permanent=response.status>=400 && response.status<500;
        throw error;
      }
      return response.json();
    });
    async function drain() {
      if(running || retry!==null)return;
      running=true;
      try {
        while(tasks.length) {
          const task=tasks[0], image=task.image;
          try {
            if(task.kind==='read') {
              image.digest=image.digest || await hash(image.base64);
              const saved=await request({image_sha256:image.digest});
              if(typeof saved.frame_id!=='string' || !Number.isFinite(saved.rotation_degrees))throw Error('Invalid rotation response');
              image.frame=saved.frame_id;image.savedAngle=saved.rotation_degrees;
              if(current===image && !image.edited) {
                options.apply(saved.rotation_degrees);
                status('Saved angle loaded.');
              }
            } else {
              if(task.inherited && (image.editVersion!==task.version || image.savedAngle===task.angle)) {
                tasks.shift();continue;
              }
              const saved=await request({frame_id:image.frame,rotation_degrees:task.angle,mutation_id:task.id});
              if(!saved.saved || saved.mutation_id!==task.id)throw Error('Rotation was not acknowledged');
              if(current===image && image.editVersion===task.version)status('Angle saved for this image and new captures.');
            }
            tasks.shift();
          } catch(error) {
            status(error.permanent
              ? 'Angle changes are not saved. Reopen your personal Paper tutor link and try again.'
              : 'Angle changes are not saved yet. Keep this page open; retrying automatically.');
            // Preserve order and mutation IDs even if a save succeeded but its ACK was lost.
            if(!error.permanent)retry=schedule(()=>{retry=null;void drain();});
            return;
          }
        }
      } finally {running=false;}
    }
    return {
      image(base64) {
        current={base64,frame:null,digest:null,edited:false,editVersion:0};
        tasks.push({kind:'read',image:current});
        // Keep a student's choice visible across frames that were already in flight
        // when the angle changed. Backfill their metadata after identification.
        if(selectedAngle!==null) {
          current.edited=true;current.editVersion=1;options.apply(selectedAngle);
          tasks.push({kind:'write',image:current,angle:selectedAngle,version:1,inherited:true,
            id:options.uuid?options.uuid():crypto.randomUUID()});
        }
        status('Loading saved angle…');void drain();
      },
      change(angle) {
        selectedAngle=angle;
        if(!current){status('The angle can be saved once a paper image arrives.');return;}
        current.edited=true;current.editVersion++;
        tasks.push({kind:'write',image:current,angle,version:current.editVersion,
          id:options.uuid?options.uuid():crypto.randomUUID()});
        status('Saving angle…');void drain();
      },
      pending() {return tasks.some(task=>task.kind==='write');}
    };
  }
  if(typeof module==='object' && module.exports)module.exports={createPictureRotation};
  else root.createPictureRotation=createPictureRotation;
})(typeof globalThis==='object'?globalThis:this);
