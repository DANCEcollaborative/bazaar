const {test}=require('node:test');
const assert=require('node:assert/strict');
const {createPictureRotation}=require('../../bazaar_server/bazaar_server_lobby/lobby/static/representation-picture-rotation.js');
const settle=async()=>{for(let i=0;i<100;i++)await Promise.resolve();};
function controller(request,extra={}) {
  const applied=[],statuses=[],timers=[];let id=0;
  const store=createPictureRotation({request,hash:async text=>'hash-'+text,apply:a=>applied.push(a),
    status:s=>statuses.push(s),uuid:()=>String(++id),schedule:fn=>{timers.push(fn);return timers.length;},...extra});
  return {store,applied,statuses,timers};
}
test('saved angle is restored without generating another write',async()=>{
  const calls=[];const c=controller(async body=>{calls.push(body);return {frame_id:'f1',rotation_degrees:17.5};});
  c.store.image('original');await settle();assert.deepEqual(c.applied,[17.5]);
  assert.deepEqual(calls,[{image_sha256:'hash-original'}]);assert.equal(c.store.pending(),false);
});
test('manual edits during lookup win over its late response and save in order',async()=>{
  let resolve;const wait=new Promise(done=>resolve=done);const calls=[];
  const c=controller(async body=>{calls.push(body);if(body.image_sha256){await wait;return {frame_id:'f1',rotation_degrees:0};}
    return {saved:true,mutation_id:body.mutation_id};});
  c.store.image('one');c.store.change(90);c.store.change(17.5);await settle();resolve();await settle();
  assert.deepEqual(c.applied,[]);assert.deepEqual(calls.slice(1).map(b=>b.rotation_degrees),[90,17.5]);
  assert.ok(calls.slice(1).every(b=>b.frame_id==='f1'));assert.equal(c.store.pending(),false);
});
test('a lost save acknowledgement retries the same mutation before later edits',async()=>{
  const calls=[];let fail=true;
  const c=controller(async body=>{calls.push({...body});if(body.image_sha256)return {frame_id:'f1',rotation_degrees:0};
    if(fail){fail=false;throw Error('lost ack');}return {saved:true,mutation_id:body.mutation_id};});
  c.store.image('one');await settle();c.store.change(90);await settle();c.store.change(-90);await settle();
  assert.equal(c.store.pending(),true);assert.match(c.statuses.at(-1),/Saving/);
  c.timers.shift()();await settle();
  assert.deepEqual(calls[1],calls[2]);assert.equal(calls[3].rotation_degrees,-90);assert.equal(c.store.pending(),false);
});
test('edits remain tied to their original frame when a new image arrives',async()=>{
  let resolve;const wait=new Promise(done=>resolve=done);const calls=[];
  const c=controller(async body=>{calls.push({...body});if(body.image_sha256)return {frame_id:body.image_sha256==='hash-one'?'f1':'f2',rotation_degrees:0};
    if(body.frame_id==='f1')await wait;return {saved:true,mutation_id:body.mutation_id};});
  c.store.image('one');await settle();c.store.change(90);await settle();
  c.store.image('two');c.store.change(17);resolve();await settle();
  assert.deepEqual(calls.filter(b=>b.frame_id).map(b=>[b.frame_id,b.rotation_degrees]),[['f1',90],['f2',17]]);
  assert.deepEqual(c.applied,[0,90]);
});
test('hash identifies the original decoded JPEG bytes, not rendered or reencoded pixels',async()=>{
  const calls=[];const c=controller(async body=>{calls.push(body);return {frame_id:'f1',rotation_degrees:0};},{hash:undefined});
  c.store.image(Buffer.from('original image bytes').toString('base64'));
  // Native WebCrypto completion yields beyond the microtask queue.
  await new Promise(resolve=>setTimeout(resolve,20));
  const expected=require('node:crypto').createHash('sha256').update('original image bytes').digest('hex');
  assert.equal(calls[0].image_sha256,expected);
});

test('a current manual angle persists and is saved on an in-flight frame with an older snapshot',async()=>{
  const calls=[];const c=controller(async body=>{calls.push(body);if(body.image_sha256)return {frame_id:body.image_sha256==='hash-one'?'f1':'f2',rotation_degrees:0};
    return {saved:true,mutation_id:body.mutation_id};});
  c.store.image('one');await settle();c.store.change(90);await settle();
  c.store.image('two');await settle();
  assert.deepEqual(c.applied,[0,90]);
  assert.deepEqual(calls.filter(b=>b.frame_id).map(b=>[b.frame_id,b.rotation_degrees]),[['f1',90],['f2',90]]);
});
