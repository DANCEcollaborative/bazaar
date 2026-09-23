// Browser contract tests: no Google authentication or production mutation.
// Run with a Playwright-capable Node installation from any working directory.
const { chromium } = require('playwright');
const { readFileSync } = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const source = readFileSync(path.join(__dirname, '../ui/student-entry.html'), 'utf8');
const origin = 'https://bree.lti.cs.cmu.edu';
const ACT = 'fcds-p2-26-fall-1a';
const user = { name: 'Entry Test', email: 'entry-test@example.invalid' };
(async () => {
 const browser = await chromium.launch({ headless: true });
 let passed = 0;
 async function scenario(name, enroll, test, identity=user) {
  const context = await browser.newContext({viewport:{width:1000,height:1100}});
  try {
   if(identity) await context.addInitScript(x=>sessionStorage.setItem('currentUser',JSON.stringify(x)),identity);
   await context.route('https://accounts.google.com/**', route=>route.abort());
   await context.route(origin+'/**', async route => {
    const url=new URL(route.request().url());
    if(url.pathname==='/k8sjchat.html') return route.fulfill({contentType:'text/html',body:source});
    if(url.pathname.startsWith('/api/activities/by-email/')) return enroll(route);
    throw new Error('Unexpected request: '+url.pathname);
   });
   const page=await context.newPage(); await page.goto(origin+'/k8sjchat.html');
   await test(page,context); passed++; console.log('PASS '+name);
  } finally {await context.close();}
 }
 try {
  const ok=rows=>route=>route.fulfill({json:rows});
  await scenario('logged out and mobile layout',ok([]),async p=>{
   await p.setViewportSize({width:390,height:844});
   assert(await p.getByRole('heading',{name:'JupyterLab Bot Chat',exact:true}).isVisible());
   assert.equal(await p.locator('select').count(),0);
   assert.equal(await p.locator('#loggedOut').isVisible(),true);
   assert.equal(await p.locator('#startBtn').isVisible(),false);
   assert(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  },null);
  await scenario('unknown account cannot launch',ok([]),async p=>{
   await p.getByText('This account is not enrolled',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isDisabled());
   assert(await p.locator('#retryEnrollmentBtn').isVisible());
   await p.locator('#logoutBtn').click();
   assert(await p.locator('#loggedOut').isVisible());
  });
  await scenario('other activity enrollment cannot launch this activity',ok([{activity_id:'another-activity',activity_name:'Other'}]),async p=>{
   await p.getByText('This account is not enrolled',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isDisabled());
   assert(!await p.getByText('Other',{exact:true}).count());
  });
  let attempts=0;
  await scenario('service error can be retried after recovery',route=>++attempts===1?route.fulfill({status:503,body:'Unavailable'}):ok([{activity_id:ACT}])(route),async p=>{
   await p.getByText('We could not check enrollment.',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isDisabled());
   await p.locator('#retryEnrollmentBtn').click();
   await p.getByText('You are enrolled.',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isEnabled());
  });
  await scenario('single fixed launch, notebook path and URL identity preserved, reopen after reload',ok([{activity_id:'other'},{activity_id:ACT}]),async (p,c)=>{
   await p.getByText('You are enrolled.',{exact:false}).waitFor();
   let body;
   await c.route(origin+'/getJupyterlabUrl',async route=>{body=route.request().postDataJSON();return route.fulfill({contentType:'text/plain',body:'https://collab.lti.cs.cmu.edu/test/lab?token=synthetic&room_name=test-room&email=entry-test%40example.invalid&activity_id='+ACT});});
   await c.route('https://collab.lti.cs.cmu.edu/**',route=>route.fulfill({body:'Notebook stand-in'}));
   const wait=c.waitForEvent('page');await p.locator('#startBtn').click();const lab=await wait;
   await lab.waitForURL(/RTC:workspace.ipynb/);
   const url=new URL(lab.url()); assert.equal(url.pathname,'/test/lab/tree/RTC:workspace.ipynb');
   assert.equal(url.searchParams.get('token'),'synthetic'); assert.equal(url.searchParams.get('room_name'),'test-room');
   assert.deepEqual(body,{name:user.name,email:user.email,password:user.email,entityId:ACT});
   await p.getByRole('button',{name:'Reopen activity'}).waitFor();
   await p.reload(); await p.getByText('You are enrolled.',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isEnabled());
  });
  await scenario('popup denial gives an actionable error without starting allocation',ok([{activity_id:ACT}]),async p=>{
   await p.getByText('You are enrolled.',{exact:false}).waitFor();
   await p.evaluate(()=>window.open=()=>null);await p.locator('#startBtn').click();
   await p.getByText('Popup was blocked.',{exact:false}).waitFor();assert(await p.locator('#startBtn').isEnabled());
  });
  await scenario('allocation failure closes the waiting tab and allows retry',ok([{activity_id:ACT}]),async(p,c)=>{
   await p.getByText('You are enrolled.',{exact:false}).waitFor();
   await c.route(origin+'/getJupyterlabUrl',route=>route.fulfill({status:503,body:'Unavailable'}));
   await p.locator('#startBtn').click();await p.getByText('We could not open the activity.',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isEnabled());
   assert.equal(c.pages().length,1);
  });
  await scenario('stale enrollment response cannot enable launch after sign out',async route=>{
   await new Promise(r=>setTimeout(r,500));await ok([{activity_id:ACT}])(route);
  },async p=>{
   await p.locator('#logoutBtn').click();await p.waitForTimeout(650);
   assert(await p.locator('#startBtn').isDisabled());assert(await p.locator('#loggedOut').isVisible());
  });
  async function googleSignIn(page,email) {
   const token='test.'+Buffer.from(JSON.stringify({email,name:'CMU Browser Test'})).toString('base64url')+'.signature';
   await page.evaluate(credential=>handleCredentialResponse({credential}),token);
   return token;
  }
  for (const email of ['student@cmu.edu','student@andrew.cmu.edu']) {
   let joined=false;
   await scenario('first Google sign-in auto-enrolls '+email,route=>ok(joined?[{activity_id:ACT}]:[])(route),async(p,c)=>{
    let sentToken;
    await c.route(origin+'/api/activity/'+ACT+'/self-enroll',async route=>{
     sentToken=route.request().headers().authorization;
     assert.equal(route.request().method(),'POST');joined=true;
     return route.fulfill({json:{status:'enrolled',activity_id:ACT,email}});
    });
    const token=await googleSignIn(p,email);
    await p.getByText('You are enrolled.',{exact:false}).waitFor();
    assert.equal(sentToken,'Bearer '+token);assert(await p.locator('#startBtn').isEnabled());
    assert(!await p.evaluate(x=>JSON.stringify(sessionStorage).includes(x),token));
    await p.reload();await p.getByText('You are enrolled.',{exact:false}).waitFor();
    assert(await p.locator('#startBtn').isEnabled());
   },null);
  }
  await scenario('saved CMU identity requires a fresh Google sign-in before first enrollment',ok([]),async p=>{
   await p.getByRole('button',{name:'Sign in again'}).waitFor();assert(await p.locator('#startBtn').isDisabled());
   await p.getByRole('button',{name:'Sign in again'}).click();assert(await p.locator('#loggedOut').isVisible());
  },{name:'CMU Test',email:'new@andrew.cmu.edu'});
  await scenario('expired Google credential requests sign-in again',ok([]),async(p,c)=>{
   await c.route(origin+'/api/activity/'+ACT+'/self-enroll',route=>route.fulfill({status:401,json:{detail:'Expired'}}));
   await googleSignIn(p,'new@andrew.cmu.edu');await p.getByRole('button',{name:'Sign in again'}).waitFor();
   assert(await p.locator('#startBtn').isDisabled());
  },null);
  await scenario('disabled activity or rejected verification does not enable launch',ok([]),async(p,c)=>{
   await c.route(origin+'/api/activity/'+ACT+'/self-enroll',route=>route.fulfill({status:403,json:{detail:'Unavailable'}}));
   await googleSignIn(p,'new@andrew.cmu.edu');await p.getByText('CMU access could not be enabled.',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isDisabled());
  },null);
  await scenario('lookalike domain cannot trigger CMU self-enrollment',ok([]),async p=>{
   await googleSignIn(p,'new@cmu.edu.evil.example');await p.getByText('This account is not enrolled.',{exact:false}).waitFor();
   assert(await p.locator('#startBtn').isDisabled());
  },null);
  console.log(`${passed} browser scenarios passed`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
