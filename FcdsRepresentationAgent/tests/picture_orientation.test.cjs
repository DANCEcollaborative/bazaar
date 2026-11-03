const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const root=path.join(__dirname,'../../bazaar_server/bazaar_server_lobby/lobby');
const html=fs.readFileSync(path.join(root,'html_pages/representation-private-space.html'),'utf8');
function section(start,end){const a=html.indexOf(start),b=html.indexOf(end,a);assert(a>=0&&b>a);return html.slice(a,b);}
function viewer(){
  const elements={camera_image:{naturalWidth:1200,naturalHeight:800,style:{}},
    camera_image_div:{clientWidth:600,clientHeight:400,style:{display:'flex'}},
    camera_image_stage:{style:{}},picture_angle:{value:0},zoom_level:{textContent:''},image_column:{style:{setProperty(){}}}};
  const context={document:{getElementById:id=>elements[id]},Math,Number,isFinite};vm.createContext(context);
  vm.runInContext(section('    function sizeCameraImage()','    function getMultimodalValue(')+
    section('    var pictureZoomPct =','\t// on load of page'),context);
  return {context,elements};
}
test('quarter turns fit rotated bounds and retain zoom on angle reset',()=>{
  const {context:c,elements:e}=viewer();c.setPictureRotation(90);
  assert.equal(e.camera_image_stage.style.width,'267px');assert.equal(e.camera_image_stage.style.height,'400px');
  assert.match(e.camera_image.style.transform,/rotate\(90deg\)/);
  c.stepPictureZoom(1);assert.equal(e.camera_image_stage.style.height,'500px');
  c.setPictureRotation(0);assert.equal(c.pictureZoomPct,125);assert.equal(e.camera_image_stage.style.width,'750px');
});
test('arbitrary tilt preserves every corner within the scrollable stage',()=>{
  const {context:c,elements:e}=viewer();c.pictureZoomPct=300;
  for(const angle of [0,90,180,-90,17,-32.5]){
    c.setPictureRotation(angle);
    const w=parseFloat(e.camera_image.style.width),h=parseFloat(e.camera_image.style.height);
    const sw=parseFloat(e.camera_image_stage.style.width),sh=parseFloat(e.camera_image_stage.style.height);
    const r=c.pictureRotation*Math.PI/180;
    for(const [x,y] of [[-w/2,-h/2],[w/2,-h/2],[-w/2,h/2],[w/2,h/2]]){
      assert.ok(Math.abs(x*Math.cos(r)-y*Math.sin(r))<=sw/2+1e-8);
      assert.ok(Math.abs(x*Math.sin(r)+y*Math.cos(r))<=sh/2+1e-8);
    }
  }
});
test('rotation persists through new images and resize; zoom reset keeps the angle',()=>{
  const {context:c,elements:e}=viewer();c.setPictureRotation(-90);c.stepPictureZoom(1);
  e.camera_image.naturalWidth=800;e.camera_image.naturalHeight=1200;c.sizeCameraImage();
  assert.equal(c.pictureRotation,-90);assert.match(e.camera_image.style.transform,/rotate\(-90deg\)/);
  e.camera_image_div.clientWidth=300;c.sizeCameraImage();assert.equal(c.pictureRotation,-90);
  c.resetPictureZoom();assert.equal(c.pictureZoomPct,100);assert.equal(c.pictureRotation,-90);
  c.setPictureRotation(270);assert.equal(c.pictureRotation,-90);
  c.setPictureRotation(NaN);assert.equal(c.pictureRotation,-90);
});
test('served camera exposes no rotation controls, inference requests, or rotation metadata',()=>{
  const cameraHtml=fs.readFileSync(path.join(root,'static/camera_fcds-recorded.html'),'utf8');
  const cameraJs=fs.readFileSync(path.join(root,'static/camera_fcds-recorded.js'),'utf8');
  assert.doesNotMatch(cameraHtml,/cameraAngle|photoAngle|cameraCorrection|photoCorrection/);
  assert.doesNotMatch(cameraJs,/\/camera\/orientation|detectOrientation|rotation_degrees|\.rotate\(/);
  for(const id of ['rotate_left','rotate_right','picture_angle','rotation_reset'])assert.ok(html.includes('id="'+id+'"'));
});
