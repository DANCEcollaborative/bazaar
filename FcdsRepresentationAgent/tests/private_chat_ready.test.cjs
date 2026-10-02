const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const html=fs.readFileSync(path.join(__dirname,'../ui/representation-private-space.html'),'utf8');
function part(start,end){const a=html.indexOf(start);const b=html.indexOf(end,a);assert(a>=0&&b>a);return html.slice(a,b);}
function socketHandler(event){const start=html.indexOf("socket.on('"+event+"'");assert(start>=0);return html.slice(start,html.indexOf('\n\t});',start)+6);}
function page(){
 const input={value:'',disabled:true},status={textContent:''},handlers={},emits=[],echo=[];
 const conversation={0:{scrollHeight:10},stop(){return this;},animate(){return this;},text(){return this;}};
 const socket={connected:false,on:(event,fn)=>handlers[event]=fn,emit:(...args)=>emits.push(args)};
 const context={socket,document:{getElementById:id=>id==='data'?input:status},location:{pathname:'/bazaar/chat/fcdsrepresentationfcds-p2-26-fall-1a-room260923999/Private_1/Private_1/'},
  console:{log(){}},prompt:()=>{throw Error('unexpected prompt');},appendMessage:(...args)=>echo.push(args),Date,
  $:selector=>selector==='#data'?{val(value){if(arguments.length)input.value=value;return input.value;}}:conversation};
 vm.createContext(context);
 vm.runInContext(part('    var privateChatReady =','\t// on connection to server')+socketHandler('connect')+socketHandler('updateusers')+part('    function sendMessage()','    // ---- Picture zoom tool'),context);
 return {input,status,handlers,emits,echo,socket,send:()=>context.sendMessage()};
}
test('private composer starts disabled and disconnected send preserves the draft',()=>{
 assert.match(html,/<textarea id="data" disabled/);
 const p=page();p.input.value=' READY! ';p.send();
 assert.equal(p.input.value,' READY! ');assert.equal(p.emits.length,0);assert.equal(p.echo.length,0);
 assert.match(p.status.textContent,/draft is kept/);
});
test('connection alone does not enable chat; room registration enables exact private identity',()=>{
 const p=page();p.socket.connected=true;p.handlers.connect();
 assert.equal(p.input.disabled,true);assert.equal(p.emits[0][0],'adduser');
 p.handlers.updateusers({'Private_2':'Private_2'});assert.equal(p.input.disabled,true);
 p.handlers.updateusers({'Private_1':'different-id'});assert.equal(p.input.disabled,true);
 p.handlers.updateusers({'Private_1':'Private_1'});assert.equal(p.input.disabled,false);
 p.input.value='READY!';p.send();assert.equal(p.input.value,'');
 const sent=p.emits.filter(x=>x[0]==='sendpm');assert.equal(sent.length,1);
 assert.match(sent[0][1],/from:::Private_1;%;speech:::READY!/);assert.equal(p.echo.length,1);
});
test('disconnect keeps an unsent draft and requires registration again before sending',()=>{
 const p=page();p.socket.connected=true;p.handlers.connect();p.handlers.updateusers({'Private_1':'Private_1'});
 p.input.value='My question';p.socket.connected=false;p.handlers.disconnect();p.send();
 assert.equal(p.input.disabled,true);assert.equal(p.input.value,'My question');assert.equal(p.echo.length,0);
 p.socket.connected=true;p.handlers.connect();assert.equal(p.input.disabled,true);p.send();assert.equal(p.input.value,'My question');
 p.handlers.updateusers({'Private_1':'Private_1'});p.send();assert.equal(p.input.value,'');
 assert.equal(p.emits.filter(x=>x[0]==='sendpm').length,1);assert.equal(p.echo.length,1);
});
test('connection errors expose retry status without clearing the draft',()=>{
 const p=page();p.input.value='Please explain';p.handlers.connect_error();
 assert.equal(p.input.value,'Please explain');assert.equal(p.input.disabled,true);assert.match(p.status.textContent,/Cannot connect.*Retrying/);
});
test('only activity wrapper selects the dedicated private template',()=>{
 const wrapper=fs.readFileSync(path.join(__dirname,'../ui/representation-student-recorded.html'),'utf8');
 assert.match(wrapper,/html=representation-private-space/);
});
