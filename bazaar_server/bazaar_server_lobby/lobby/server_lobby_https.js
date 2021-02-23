// const express = require('express');
// const app = express();

// const mysql      = require('mysql');
// const mysql_auth = {
//       host     : 'nodechat',
//       user     : 'root',
//       password : 'smoot',
//       port     : 3306
//     };
// const connection;
const winston    = require('winston');
const bodyParser = require('body-parser');
const {pool, promisePool} = require('./database')
const {Worker, isMainThread} = require('worker_threads');

const lti = require('ims-lti');
const consumer_key = "BazaarLTI";
const consumer_secret = "BLTI";
const localPort = 443;
const localHost = '0.0.0.0';
const localURL = "/bazaar";

//variables for grading
// const url = require('url');
const request = require('request');
const OAuth   = require('oauth-1.0a');
const Crypto = require("crypto");

const app = require('express')();
const server = require('http').createServer(app);
const io = require('socket.io')(server, {path: '/bazsocket', allowEIO3: true});
const path = require('path'); 

server.listen(localPort);

const generateXML = function(lis_result_sourcedid , curScore){
    const result = '<?xml version = "1.0" encoding = "UTF-8"?>'
               + '<imsx_POXEnvelopeRequest xmlns = "http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">'
               + '<imsx_POXHeader>'
               + '<imsx_POXRequestHeaderInfo>'
               + '<imsx_version>V1.0</imsx_version>'
               + '<imsx_messageIdentifier>99999999123</imsx_messageIdentifier>'
               + '</imsx_POXRequestHeaderInfo>'
               + '</imsx_POXHeader>'
               + '<imsx_POXBody>'
               + '<replaceResultRequest>'
               + '<resultRecord>'
               + '<sourcedGUID>'
               + '<sourcedId>' + lis_result_sourcedid + '</sourcedId>'
               + '</sourcedGUID>'
               + '<result>'
               + '<resultScore>'
               + '<language>en</language>'
               + '<textString>' + curScore + '</textString>'
               + '</resultScore>'
               + '</result>'
               + '</resultRecord>'
               + '</replaceResultRequest>'
               + '</imsx_POXBody>'
               + '</imsx_POXEnvelopeRequest>';
    return result;
}

function sleep(milliseconds) {
  console.log("sleep start");
  const start = new Date().getTime();
  while (1) {
    if ((new Date().getTime() - start) > milliseconds){
      break;
    }
  }
  console.log("sleep over");
}

const generateRequest = function(Url,lis_result_sourcedid,curScore){
    const postHead = {
        url: Url,
        method: 'POST',
        'content-type': 'application/xml',
        data: generateXML(lis_result_sourcedid,curScore)
    };

    return postHead;
}

// app.use(bodyParser.urlencoded());
app.use(bodyParser.urlencoded({ extended: true }));

// Adding content security policy
app.use(function(req, res, next) {
    res.setHeader("Content-Security-Policy", "default-src 'self' https://docs.google.com/spreadsheets/d/1OoLD9dZaVfLJz-0X-OQ3WnrI97G9syUKGEJWDR2Mxdg/edit?ts=57c83646#gid=298683489 https://erebor.lti.cs.cmu.edu:9001/p/ https://misty.lti.cs.cmu.edu/lobby/bazaar/vertical.pdf https://misty.lti.cs.cmu.edu/lobby/bazaar/horizontal.pdf; connect-src 'self' ws://misty.lti.cs.cmu.edu/bazsocket/ http://misty.lti.cs.cmu.edu/bazsocket/ wss://misty.lti.cs.cmu.edu/bazsocket/ https://misty.lti.cs.cmu.edu/bazsocket/ ws://misty.lti.cs.cmu.edu/bazsocket/ http://misty.lti.cs.cmu.edu/local/bazsocket/ wss://misty.lti.cs.cmu.edu/local/bazsocket/ https://misty.lti.cs.cmu.edu/local/bazsocket/; style-src 'self' https://fonts.googleapis.com/css https://cdn.jsdelivr.net/gh/DANCECollaborative/bazaar@latest/bazaar_server/bazaar_server_https/bazaar/discussionnew2.css https://rawgit.com/gtomar/help-button-javascript/master/discussion.css 'unsafe-inline'; script-src 'self' https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js https://rawgit.com/gtomar/stylesheets/master/colors.js https://rawgit.com/gtomar/stylesheets/master/jquery.sortable.js https://rawgit.com/marinawang/bazaar/master/client.js https://cdnjs.cloudflare.com/ajax/libs/socket.io/2.3.0/socket.io.js 'unsafe-inline'; font-src 'self' https://fonts.gstatic.com/s/oxygen/v10/2sDfZG1Wl4LcnbuKjk0mRUe0Aw.woff2 https://fonts.gstatic.com/s/oxygen/v10/2sDfZG1Wl4LcnbuKgE0mRUe0A4Uc.woff2; img-src http://www.dnr.sc.gov/climate/sco/Education/wxmap/wxmap.gif http://misty.lti.cs.cmu.edu/favicon.ico"); 
    return next();
});

app.get('/room_status_all', async (req, res) => {
    console.log("app.get('/room_status_all')");
  const query = 'SELECT name from nodechat.room where name like "normaldist%"';
  // console.log(query);
  try {
    const [rows, fields] = await promisePool.query(query);
    let num_list = "";
    for (var i = 0; i < rows.length; i++) num_list += "<p>" + rows[i].name + "</p>";
    res.send("<body>" + num_list + "</body>");
  } catch (err) {
    console.log(err);
    res.send(500, "<body><h2>Error</h2><p>Couldn't fetch data</p></body>");
  }

});

app.get('/room_status*', async (req, res) => {
    console.log("app.get('/room_status*')");
  const query = 'SELECT name from nodechat.room where name like "normaldist%"';
  // console.log(query);
  try {
    const [rows, fields] = await promisePool.query(query);
    let num_list = "";
    for (var i = 0; i < rows.length; i++) num_list += "<p>" + rows[i].name + "</p>";
    res.send("<body>" + num_list + "</body>");
  } catch (err) {
    console.log(err);
    res.send(500, "<body><h2>Error</h2><p>Couldn't fetch data</p></body>");
  }

});

app.get('/welcome*', async (req, res) => {
    console.log("Welcome");
    res.sendFile(__dirname + '/welcome.html');
});



app.get('/login*', async (req, res) => {
    console.log("Hi");
  teamNumber = 0;
  logger = winston.createLogger({
    transports: [
      new (winston.transports.Console)()]
  });

  const query = 'INSERT INTO nodechat.consent (roomname, userid, consent) VALUES (' + promisePool.escape(req.query.roomName + req.query.roomId) + ',' + promisePool.escape(req.query.id) + ',' + (req.query.consent !== "undefined" && req.query.consent == "agree" ? 1 : 0) + ')';
  // console.log(query);
  try {
    await promisePool.query(query);
  } catch (err) {
    console.log("error from db room insert " + err);
  }

  if (1) {
    teamNumber = req.query.roomId;
    setTeam_(teamNumber, req, logger, res);
  }
});

app.post('/login*', async (req, res) => {
    console.log("Hi");
  teamNumber = 0;
  logger = winston.createLogger({
    transports: [
      new (winston.transports.Console)()]
  });

  // console.log("SELECT count from nodechat.room_prefix" + ' where name=' + req.query.roomName);
  try {
    let [rows, fields] = null;
    try {
      [rows, fields] = await promisePool.query("SELECT count from nodechat.room_prefix"
        + ' where name=' + promisePool.escape(req.query.roomName));
    } catch (err) {
      throw new Error("Couldn't fetch data for room " + req.query.roomName);
    }
    if (rows.length === 0) {
      // console.log('insert into nodechat.room_prefix set name=' + req.query.roomName + ', created=NOW(), comment="auto-created", count=1;');
      try {
        await promisePool.query('insert into nodechat.room_prefix set name=' + promisePool.escape(req.query.roomName) + ', created=NOW(), comment="auto-created", count=1;');
      } catch (err) {
        throw new Error("Couldn't create room " + req.query.roomName);
      }
    } else {
      teamNumber = rows[0].count;
      // console.log('count : ' + teamNumber);
      // console.log('users : ' + numUsers);
      if ((!(req.query.roomName + pad(teamNumber, 2) in numUsers)) || numUsers[req.query.roomName + pad(teamNumber, 2)] <= 0) {
        teamNumber = teamNumber + 1;
        // console.log('increased count : ' + teamNumber);
        // console.log('update nodechat.room_prefix set count=' + teamNumber + ' where name=' + req.query.roomName);
        try {
          await promisePool.query('insert into nodechat.room_prefix set name=' + promisePool.escape(req.query.roomName) + ', created=NOW(), comment="auto-created", count=1;');
        } catch (err) {
          throw new Error("Couldn't update count for room " + req.query.roomName);
        }
        numUsers[req.query.roomName + pad(teamNumber, 2)] = 0;
        exec("../bazaar/launch_agent_docker.sh " + req.query.roomName + " " + teamNumber + ' "none"', puts);
        sleep(5000);
        setTeam(teamNumber, req, logger, res);
      } else {
        setTeam(teamNumber, req, logger, res);
      }
      // console.log("successful");
    }
  } catch (err) {
    console.log(err);
    res.send(500, header_stuff + "<body><h2>Error</h2><p>" + err.message + "':</p><pre>" + err + "</pre></body>");
  }

});


function createWorker() {
  return new Worker('./agentup.js');
}

const worker = createWorker();

const agentLaunch = async (roomName, teamNumber) => {
  worker.postMessage({
    roomName: roomName,
    teamNumber: teamNumber
  });
}


function setTeam_(teamNumber,req,logger,res)
{
        if( (!(req.query.roomName + teamNumber in numUsers)) )
        {
                numUsers[req.query.roomName + teamNumber] = 0;
                console.log("../bazaar/launch_agent_docker.sh " + req.query.roomName + " " + teamNumber + ' "none"', puts);
    			agentLaunch(req.query.roomName, teamNumber);
        }
        //teamNumber = req.query.roomId;
        let html_page = 'index';
        if(req.query.html != undefined) html_page = req.query.html;

        const roomname = req.query.roomName + teamNumber;
        const url = localURL + '/chat/' + roomname  + '/' + req.query.id + '/' +
                                                             req.query.username + '/' + req.query.perspective + '/' + '?html=' + html_page + '&forum=' + req.query.forum;
        console.log("setTeam_, url: " + url);
        res.writeHead(301,{Location: url});
        res.end();

        logger.log("info","Number of users : " + numUsers[roomname]);
        logger.log("info","Team number : " + teamNumber);

        //Replace accepts a value between 0 and 1.
       /* provider.outcome_service.send_replace_result(1, function(err, result){
            console.log("Grade submitted: " + result) // True or false
        });*/

}
function setTeam(teamNumber,req,provider,logger,res)
{
    	/*if( (!(req.query.roomName + pad(teamNumber, 2) in numUsers)) || numUsers[req.query.roomName + pad(teamNumber, 2)] <= 0)
    	{
                numUsers[req.query.roomName + pad(teamNumber, 2)] = 0;
        	exec("../bazaar/launch_agent_docker.sh " + req.query.roomName + " " + teamNumber + ' "none"', puts);
       		sleep(5000);
    	}*/
        let html_page = 'index';
        if(req.query.html != undefined) html_page = req.query.html;
        const roomname = req.query.roomName + pad(teamNumber, 2);
        const url = localURL + '/chat/' + roomname + '/' + provider.userId + '/?html=' + html_page;

    	res.writeHead(301,{Location: url});
    	res.end();

    	logger.log("info","Number of users : " + numUsers[roomname]);
    	logger.log("info","Team number : " + teamNumber);
        logger.log("info","provider : " + provider.username);
        console.log(provider);
        //console.log(provider);
        //Replace accepts a value between 0 and 1.
        provider.outcome_service.send_replace_result(1, function(err, result){
            console.log("Grade submitted: " + result) // True or false
        });
        
}




function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}


app.get('/chat*', async (req, res) => {

  let html_page = 'index';
  if (req.query.html !== undefined) html_page = req.query.html;

  res.sendFile(path.join(__dirname, './html_pages/' + html_page + '.html'));
});

app.get('/discussionnew.css', async (req,res) => {
    res.sendFile(__dirname + '/discussionnew.css')
});

app.get('/bazaar/static/*', async (req, res) => {
  const file = req.path.substring(req.path.lastIndexOf('/') + 1);
  res.sendFile(path.join(__dirname, './static/' + file))
});

app.get('/observe/*', async (req, res) => {
    res.sendFile(__dirname + '/index_ccc.html');
});


app.get('/data/*', async (req, res) => {
    groups = /\/data\/([^\/]+)/.exec(req.url)	  
    room = groups[1];

    exportCSV(room, res);

});


app.post('/',function(req, res) {
    const provider = new lti.Provider(consumer_key, consumer_secret);
    const isValidG = false;
    provider.valid_request(req,function(err,isValid){
        isValidG = isValid;
    });
    if(isValidG){
        console.log(provider.userId);
        // res.writeHead(301,{Location: 'http://erebor.lti.cs.cmu.edu:8007/team?userId='+provider.userId});
        res.end();

        //grading
        const score = 0.5; //dummy grade

        if(score >=0 && score <=1){
            console.log(provider);
            outcome_url = provider.lis_outcome_service_url;
            //outcome_url = outcome_url.replace("https","http");
            outcome_url = outcome_url.replace("localhost",req.ip);
            result_sourcedid = provider.lis_result_sourcedid;
            //result_sourcedid = result_sourcedid.replace("https","http");
            result_sourcedid = result_sourcedid.replace("localhost",req.ip);
            console.log("outcome_url is: "+outcome_url);
            console.log("result source id is: "+result_sourcedid);

            //building OAuth
            const oauth = OAuth({
                consumer: {
                    public: consumer_key,
                    secret: consumer_secret
                },
                signature_method: 'HMAC-SHA1'
            });

            const oauth_body_hash = Crypto.createHash('sha1').update(generateXML(result_sourcedid,score)).digest().toString('base64');
            //console.log(oauth.authorize( generateRequest(outcome_url, result_sourcedid, score),oauth_body_hash));
            request({
                url: outcome_url,
                method: 'POST',
                //followAllRedirects:"true",
                //https: "false",
                'content-type': 'application/xml',
                form: generateXML(result_sourcedid,score),
                headers: oauth.toHeader( oauth.authorize( generateRequest(outcome_url, result_sourcedid, score),oauth_body_hash))
            },function(err,res){
                console.log("res.body: "+res.body);
            });
        }
    }else{
        res.end("Invalid consumer secret and consumer key!");
    }
    
    
}).listen(8006);

console.log("server is running on 8006");



//David's Code

HOST = null; // localhost
PORT = 444; 

//const CONDITIONS = ["revoice", "press_reasoning", "agree"];
const CONDITIONS = ["none"]; // hyeju changed this value
const TEAM_SIZE = 2; // hyeju changed 3 => 2
//number of messages of history to keep in memory
const MESSAGE_BACKLOG = 200,
    //idle timeout before "booting" a user who's idle - at least five seconds
    SESSION_TIMEOUT =   7*1000,
//how long to wait after the most recent particpant has joined before attempting a team assignment - should be greater than session_timeout
    GROUP_DELAY = 	15*1000, // hyeju changed this value
    LAST_RESORT_TIMEOUT =  2*50*1000, //this is when the system tells students to come back later
//    LOCKDOWN_TIMEOUT = 15*60*1000; //no more students!
    LOCKDOWN_TIMEOUT =  7*24*60*60*1000; //lobby open for 7 days
	
let LOCKDOWN_TIME = 0;
let START_TIME = new Date().getTime();
const chat_url = "https://misty.lti.cs.cmu.edu/bazaar/chat/";
const roomname_prefix = "weather";
// const create_script = "../../scripts/create-cc-rooms.sh"

// when the daemon started
let starttime = (new Date()).getTime();
let lastJoin = new Date();
let teamMemberNames = {};

function getLoginInstructionText(nick)
{
    const now = new Date().getTime();
    message = "Welcome to the matchmaker lobby. Hang on for a few minutes, we'll match you up with a team as soon as enough students join. "
    return message;
}

function getUserInstructionText(nick, i, condition)
{    
    instructions = 'After you join your team\'s discussion area, please follow VirtualCarolyn\'s instruction there.';
    return instructions;
}

let conditionOffset = -1;
let numTeams = 0;
let nextID = 0;
let teams = [];
let supplicants = [];

let numUsers = {};
let teamNumber = 0;

const csv = require('csv');
const util = require('util')
const exec = require('child_process').exec;

let logger;


function puts(error, stdout, stderr) { console.log(stdout) }

const fu = require("./fu"),
    sys = require("util"),
    url = require("url"),
    qs = require("querystring");

function parseTime(start_minutes, base_time)
{
    if(base_time == null)
        tim = new Date();
    else
        tim = new Date(base_time)
        
    if(start_minutes.indexOf(":") >=0)
        {
            start_oclock = start_minutes.split(":");
            if(tim.getHours() > parseInt(start_oclock[0]))
                start_oclock[0] = parseInt(start_oclock[0])+12;
            
            tim.setHours(start_oclock[0]);
            tim.setMinutes(parseInt(start_oclock[1]));
            tim.setSeconds(0);
            tim = tim.getTime(); //time in ms
        }
    else
        tim = tim.valueOf() + (start_minutes * 60 * 1000);
        
    return tim; //time in ms
}

//when lockdown_time == 0, server stays open forever
console.log("node server.js [room_prefix] [first_team_num] [team_start_time | minutes_until_start] lockdown_time");
if(process.argv.length > 2)
{
    winston.log('info', process.argv);
    roomname_prefix = process.argv[2];
    if(process.argv.length > 3)
    {
        numTeams = process.argv[3] - 1;
        if(process.argv.length > 4)
        {
            START_TIME = parseTime(process.argv[4]);	
            winston.log('info', new Date(START_TIME) + " <-- start time parsed");    
            
            if(process.argv.length > 5)
            {
                LOCKDOWN_TIME = parseTime(process.argv[5], START_TIME);
                LOCKDOWN_TIMEOUT = LOCKDOWN_TIME - START_TIME;
                winston.log('info', new Date(LOCKDOWN_TIME) + " <-- lockdown parsed");
                winston.log('info', (LOCKDOWN_TIMEOUT/60000) + " min <-- lockdown timeout");
            }
        }	
    }
}


if(LOCKDOWN_TIME == 0)
{
    LOCKDOWN_TIME = START_TIME+LOCKDOWN_TIMEOUT
}
    
if(LOCKDOWN_TIMEOUT > 0)
{
    winston.log('info', new Date(LOCKDOWN_TIME) + " <-- lockdown set");
	setTimeout(function (){ channel.appendMessage("System", "msg", "This session is now closed. No new students can join. If you don't have a partner yet, please come back later!" ,"all"); }, LOCKDOWN_TIME - new Date().getTime())
}

//logs team information -- who's on what team
const teamLogger = winston.createLogger({
  transports: [
    new (winston.transports.Console)(),
    new (winston.transports.File)({ filename: 'logs/teams.log' })
  ]
});

// winston.add(winston.transports.File, { filename: 'logs/server.log'});
winston.add(new winston.transports.File({ filename: 'logs/server.log' }));

winston.log('info', "The room prefix is "+roomname_prefix+". The first condition will be '"+CONDITIONS[numTeams+conditionOffset+1]+"'");
winston.log('info', "Teams will be assigned starting at "+new Date(START_TIME));
winston.log('info', "The first assigned team will be Team "+(numTeams+1));


const channel = new function () 
{
  const messages = [],
      callbacks = [];

  this.appendMessage = function (nick, type, text, target_nick) 
  {
    const m = { nick: nick
            , type: type // "msg", "join", "part", "alive"
            , text: text
            , target_nick: target_nick
            , timestamp: (new Date()).getTime()
            };

    switch (type) {
      case "msg":
        winston.log('info', new Date()+" "+nick+" @ "+target_nick +": "+ text);
        break;
      case "join":
        winston.log('info', nick + " join");
        break;
      case "part":
        winston.log('info', nick + " part");
        break;
    }

    messages.push( m );

    //while (callbacks.length > 0) 
    for (var i = 0; i < callbacks.length; i++) 
    {
        const pending = callbacks[i];
        if(channel.isVisible(m, pending.nick))
        {
            pending.callback([m]);
            callbacks.splice(i, 1);
        }
    }

    while (messages.length > MESSAGE_BACKLOG)
      messages.shift();
  };

  this.getMessages = function (since, nick, callback) 
  {
    const matching = [];
    
    
    for (var i = 0; i < messages.length; i++) 
    {
        var message = messages[i];
        if(message.timestamp > since && channel.isVisible(message, nick))
        {
            matching.push(message);
        }
    }

    if (matching.length != 0) 
    {
      callback(matching);
    } 
    else 
    {
        callback([]);
      //callbacks.push({ timestamp: new Date(), callback: callback, nick:nick});
    }
  };
  
  this.isVisible = function(message, nick)
	{     return nick && (message.target_nick == nick 
			      || nick == "System" || nick == message.nick 
			      || (message.nick == "System" 
				  && (message.target_nick == "all" 
				      || message.target_nick == "everyone")));   
	}

  // clear old callbacks
  // they can hang around for at most 30 seconds.
  setInterval(function () 
  {
    const now = new Date();
    while (callbacks.length > 0 && now - callbacks[0].timestamp > 5*1000) 
    {
      callbacks.shift().callback([]);
    }
  }, 2000);
};

const sessions = {};

//when a user joins the lobby
function createSession (nick,consent,reset) 
{
  winston.log('info',"server_lobby_https.js, createSession -- " + nick + " trying to join, consent="+consent + " reset="+reset);
  if (nick.length > 50) return null;
   
  //rejects invalid nicknames
  if (/[^\w_\-^! ]/.exec(nick)) 
  {
	winston.log('info',"server_lobby_https.js, createSession -- invalid nickname: " + nick);
	  return null;
  }

  for (const i in sessions) 
  {
    const session = sessions[i];
    if (session && session.nick === nick) 
    {
	//"reset" should always be true. It comes from the UI,
	//but hyeju has hard-coded it to true in the server. 
	if(!reset)
	    return null;
	else
	{
	
	    sessions[i].destroy()
	    winston.log('info', nick + " has been reset.");
	    break;
	}
    }
  }

  const session = 
  { 
    nick: nick, 
    id: (nextID++).toString(),
    timestamp: new Date(),
    consent: consent,

    poke: function () 
    {
      const now = new Date();
      session.timestamp = now;
    },

    destroy: function () 
    {
      channel.appendMessage(session.nick, "part", "bye", "System");
      const index = supplicants.indexOf(session)
      if(index > -1)
      {  
        supplicants.splice(index, 1);
      }
      delete sessions[session.id];
    }
  };

  sessions[session.id] = session;
  return session;
}

function pad(num, digits)
{
    let str = '' + num;
    while (str.length < digits) 
    {
        str = '0' + str;
    }
   
    return str;
}

function shuffle(array) 
{
    let tmp, current, top = array.length;

    if(top) while(--top) {
	    current = Math.floor(Math.random() * (top + 1));
	    tmp = array[current];
	    array[current] = array[top];
	    array[top] = tmp;
	}

    return array;
}

// interval where team formation and idle user removal happens
setInterval(function () 
{
  const now = new Date(); 
 
  for (const id in sessions) 
  {

    if (!sessions.hasOwnProperty(id)) continue;
    const session = sessions[id];

    if (now - session.timestamp > SESSION_TIMEOUT) 
    {
      winston.log('info', "booting "+session.nick+": "+(now - session.timestamp)+" ms since last update");
      
      session.destroy();
    }

    let team = []
    let teamNumber = 0;
    let condition = "?";
          
    if(now - START_TIME < 0)
    {
	shuffle(supplicants);
	return;
    }
    if(lastJoin < START_TIME)
	lastJoin = START_TIME;

    teamsize = TEAM_SIZE
    
    if(supplicants.length >= teamsize && now - lastJoin > GROUP_DELAY) //assemble a team
    {
        winston.log('info', "check for regular-sized homogenous-consent group");


            const teamConsent = true; //change this when team consent matching logic is added back
            for(var i = 0; i < supplicants.length && team.length < TEAM_SIZE; i++)
            {
                var member = supplicants[i];
                
                team.push(member);
            }
            
            if(team.length >= teamsize)
            {
                numTeams++;
                teamNumber = numTeams;
		conditionNum = (teamNumber + conditionOffset)%CONDITIONS.length
                condition = CONDITIONS[conditionNum];
                
                teams.push({number:teamNumber, members:team, consent:teamConsent, conditionNum:conditionNum});
		
		//create room if it's not there
		//exec(create_script+" "+roomname_prefix+" "+teamNumber, puts);

                 //launch agent!
                paddedTeamNumber = pad(teamNumber,2);

                exec("../bazaar/launch_agent_docker.sh "+roomname_prefix+" "+paddedTeamNumber+' "'+condition+'"', puts);
                //break;
            }
            else team = [];
    }
    else if(team.length == 0 
	    && supplicants.length > 0
	    && supplicants.indexOf(session) >= 0
	    && now - lastJoin > LAST_RESORT_TIMEOUT)
    {
	member = session;
	winston.log('info', "advising student "+member.nick+" to come back later");
	channel.appendMessage("System", "msg", member.nick+", there's nobody else to match you with right now. Below you can see the best times to come back to meet discussion partners.<br/><img src=\"http://erebor.lti.cs.cmu.edu/dal/dist.png\" width=\"600\" height=\"250\">", member.nick);           
        supplicants.splice(supplicants.indexOf(member),1);
        setTimeout(function(member)
        {
            return function()
            {
		member.destroy() 
            }
        }(member), 10000); 

    }
   
    //deploy and inform!
    if(team.length > 0)
    {
	winston.log('info', "putting "+team.length+" new users on team "+teamNumber);
	const theTeam = teams[teamNumber-1];
	const condition = CONDITIONS[0];//condition];        
	console.log("condition: "+condition);

	for(var i = 0; i < team.length; i++)
        {
         
            const member = team[i];
            
            const memberID = i + 1; 
            const html = "index_ccc"
	
            const roomname = roomname_prefix+pad(teamNumber,2);
            const url = chat_url + roomname + "/" + memberID + "/" + member.nick + "/undefined/?html=" + html + "&forum=undefined";
            
	    const instructions = getUserInstructionText(member.nick, i, condition);

            supplicants.splice(supplicants.indexOf(member),1);
            teamMemberNames[member.nick] = {number:teamNumber, url:url, condition:condition};
            
            winston.log('info', "adding "+member.nick+" to Team "+teamNumber+", condition="+condition);
            teamLogger.log('info', "User:"+member.nick+", Team:"+teamNumber+", condition:"+condition);
            

	    channel.appendMessage("System", "msg", member.nick+", you're on Team "+teamNumber+".", member.nick)            
            setTimeout(function(member, teamNum, url, instructions)
            {
                return function()
                {
                    instructions = '<p>Your team\'s discussion area is ready, '+member.nick+'. <a href="'+url +'" target="_blank"> Follow this link to join the discussion.</a></p><p>'+instructions+'</p>';
        	    channel.appendMessage("System", "msg", instructions, member.nick);
                }
            }(member, teamNumber, url, instructions), 4000); 
        }
	team = [];
    }
  }
}, 5*1000);



fu.listen(Number(process.env.PORT || PORT), HOST);

fu.get("/", fu.staticHandler("index.html"));
fu.get("/style.css", fu.staticHandler("style.css"));
fu.get("/client.js", fu.staticHandler("client.js"));
fu.get("/jquery-1.2.6.min.js", fu.staticHandler("jquery-1.2.6.min.js"));

fu.get("/who", function (req, res) {
  const nicks = [];
  for (const id in sessions) {
    if (!sessions.hasOwnProperty(id)) continue;
    const session = sessions[id];
    nicks.push(session.nick);
  }
  res.simpleJSON(200, { nicks: nicks
                      , rss: 0
                      });
});

//this is the incoming message from the client that triggers serssion creation.
fu.get("/join", function (req, res) 
{
  let parsed = qs.parse(url.parse(req.url).query);
  let id = parsed.id; 
  // const nick = parsed.nick + "#" + id;
  let nick = parsed.nick;
  let consent = parsed.consent;
//  const reset = parsed.reset;
  // hyeju changed this part
  let reset = "true";

  winston.log('info',req.url);
  winston.log('info',url.parse(req.url).query);
  winston.log('info',Object.keys(parsed));

  if (nick == null || nick.length == 0) {
    res.simpleJSON(200, {error: "Please use only letters and spaces in your name."});
    return;
  }
  
  const session = createSession(nick, consent==="true",reset==="true");
  if (session == null) 
  {
    res.simpleJSON(200, {error: "Username already active. If you're re-joining with the same name, try again in 15 seconds."});
    return;
  }

  winston.log('info', "connection: " + nick + "@" + res.connection.remoteAddress + ": consent="+(consent==="true") + " reset="+parsed.reset);

  channel.appendMessage(session.nick, "join", "joined!", "System");
  
  if(nick == "System")
  {
      channel.appendMessage("System", "msg", "Welcome.", session.nick);
  }
  else if(reset==="true" || !(nick in teamMemberNames)) //don't re-apply existing names to the team pool - they can re-join their old team!
  {
      const now = new Date();
      if(LOCKDOWN_TIMEOUT > 0 && now > LOCKDOWN_TIME)
      {
          channel.appendMessage("System", "msg", "Sorry, this lobby session has closed already.", session.nick);
      }
      else
      {
	// hyeju changed the mssage here to "hi"
	  if(reset==="true")
	      channel.appendMessage("System", "msg", "Hi, "+nick+".", session.nick);

          lastJoin = new Date();
          supplicants.push(session);
          const instruction = getLoginInstructionText(session.nick);
          if(instruction)
	      channel.appendMessage("System", "msg",instruction, session.nick); 
      }
  }
  else
  {
      channel.appendMessage("System", "msg", "Welcome back, "+nick+"! Your username is already active on Team "+teamMemberNames[nick].number
        +'. <a href="'+teamMemberNames[nick].url+'" target="_blank">Follow this link to return to your team\'s discussion area.</a>', nick);  
      //channel.appendMessage("System", "msg", "Remember, "+teamMemberNames[nick].instructions, nick);
  }

res.simpleJSON(200, { id: session.id
                      , nick: session.nick
                      , rss: 0
                      , starttime: starttime
                      });
});

//this is where we receive the keepalive message from the client
fu.get("/alive", function (req, res) {
  const id = qs.parse(url.parse(req.url).query).id;
  let session;
  if (id && sessions[id]) {
    session = sessions[id];
    session.poke();
  }
  res.simpleJSON(200, { rss: 0 });
});

//this is when the client can tell us that they've left -- doesn't always happen
fu.get("/part", function (req, res) {
  const id = qs.parse(url.parse(req.url).query).id;
  let session;
  if (id && sessions[id]) {
    session = sessions[id];
    session.destroy();
  }
  res.simpleJSON(200, { rss: 0 });
});

//the client is asking for new messages since the given timestamp
fu.get("/recv", function (req, res) {
  const parsed = qs.parse(url.parse(req.url).query);
  if (!parsed.since) 
  {
    res.simpleJSON(400, { error: "Must supply since parameter" });
    return;
  }
  let id = parsed.id;
  let nick = "";
  let session;
  if (id && sessions[id]) 
  {
    session = sessions[id];
    nick = session.nick;
    session.poke();
  }

  const since = parseInt(parsed.since, 10);
  if(session)
  {
      channel.getMessages(since, nick, function (messages) 
      {
      
        let myMessages = [];
        session.poke();
        for(var i = 0; i < messages.length; i++)
        {
          const message = messages[i];
          //if (nick && (message.target_nick == nick || nick == "System" || nick == message.nick))
          {
            myMessages.push(message);
          }
         
        }
        res.simpleJSON(200, { messages: myMessages, rss: 0 });
      });
    }
    else
    {
        res.simpleJSON(200, { messages: [], rss: 0 });
    }
});

//the client is sending a chat message to the matchmaker system
fu.get("/send", function (req, res) {
  const id = qs.parse(url.parse(req.url).query).id;
  let text = qs.parse(url.parse(req.url).query).text;

  const session = sessions[id];
  if (!session || !text) {
    res.simpleJSON(400, { error: "No such session id" });
    return;
  }

  session.poke();

  let target = "System"
  const atMatcher =  /^@(\S+)\s+(.*)$/;
  if(atMatcher.test(text) && session.nick == "System")
  {
    const match = atMatcher.exec(text);
    target=match[1];
    if(target == "all" || target == "everyone")
	text = match[2];
  }

  channel.appendMessage(session.nick, "msg", text, target);
  res.simpleJSON(200, { rss: 0 });
});



// sockets by username
let user_sockets = {};

// usernames which are currently connected to each chat room
let usernames = {};

// user_perspectives
let user_perspectives = {};
// rooms which are currently available in chat
let rooms = [];

function isBlank(str)
{
    return !str || /^\s*$/.test(str)
}
const header_stuff = "<head>\n"+
"\t<link href='http://fonts.googleapis.com/css?family=Oxygen' rel='stylesheet' type='text/css'>\n"+
"\t<link href='http://ankara.lti.cs.cmu.edu/include/discussion.css' rel='stylesheet' type='text/css'>\n"+
"</head>";

function exportCSV(room, res) {
	// const connection = mysql.createConnection(mysql_auth);
	
	 
	pool.query("SELECT DATE_FORMAT(m.timestamp, '%Y-%m-%d'), DATE_FORMAT(m.timestamp, '%H:%i:%s'),  m.type, m.content, m.username from nodechat.message "
				+ 'as m join nodechat.room as r on m.roomid=r.id '
				+ 'where r.name=' + pool.escape(room) + ' order by timestamp', function (err, rows, fields) { 

	if(err) {
		 console.log(err);
		 res.send(500, header_stuff+"<body><h2>Export Error</h2><p>Couldn't fetch data for room '"+room+"':</p><pre>"+err+"</pre></body>");
	} else if(rows.length == 0) {
			res.send(404, header_stuff+"<body><h2>Empty Room</h2><p>Couldn't fetch data for empty room '"+room+"'.</p></body>");
	} else {
		 rows.unshift(['DATE', 'TIME', 'TYPE', 'TEXT', 'AUTHOR']);                
		 res.set('Content-Type', 'text/csv');
		 res.header("Content-Disposition", "attachment;filename="+room+".csv");
				 csv().from(rows).to(res);
	}
});
}
      

function loadHistory(socket, secret)
{
    if(!socket.temporary) {
        //const connection = mysql.createConnection(mysql_auth);
        let id = null;
        if(socket.room in usernames && socket.username in usernames[socket.room]) {
	    	id = usernames[socket.room][socket.username];
        }

        let perspective = null;
        if(socket.room in user_perspectives && socket.username in user_perspectives[socket.room]) {
            perspective = user_perspectives[socket.room][socket.username];
        }
 
        
    	pool.query('insert ignore into nodechat.room set name=' + pool.escape(socket.room) + ', created=NOW(), modified=NOW(), comment="auto-created";', function (err, rows, fields) {
            setTimeout( function(socket) {
                // const connection = mysql.createConnection(mysql_auth);
                 
				pool.query('SELECT m.timestamp, m.type, m.content, m.username from nodechat.message '
					+ 'as m join nodechat.room as r on m.roomid=r.id '
					+ 'where r.name=' + pool.escape(socket.room) + ' and not(m.type like "private") order by timestamp', function (err, rows, fields) {
					if (err) 
						console.log(err);						
					socket.emit('dump_history', rows);
			
					if(!secret) {
						io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join', id, perspective);
						logMessage(socket, "join", "presence");
					}
				});
             
            // connection.end()
            }, 100, socket);

        });
    }
    else if(!secret)
    {
	io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join', id, perspective);
    }
}

function logMessage(socket, content, type) {   
    if(socket.temporary) return;

    //const connection = mysql.createConnection(mysql_auth);
    
    
  	pool.query('update nodechat.room set modified=now() where room.name=' + pool.escape(socket.room) + ';', function (err, rows, fields) {
         if (err) 
            console.log(err);
    });
    
    endpoint = "unknown"
    if(socket.handshake)
	endpoint = socket.handshake.address;
    query = 'insert into nodechat.message (roomid, username, useraddress, userid, content, type, timestamp)' 
    		+ 'values ((select id from nodechat.room where name=' + pool.escape(socket.room) + '), '
    		+ '' + pool.escape(socket.username) + ', ' + pool.escape(endpoint.address + ':' + endpoint.port) + ', ' + pool.escape(socket.Id) + ', ' + pool.escape(content) + ', ' 
    		+ pool.escape(type) + ', now());';                   

 	 pool.query(query, function (err, rows, fields) {
         if (err) 
            console.log(err);
    });   
//   connection.end()    
}

// io.set('log level', 1);
DEBUG=io*

io.sockets.on('connection', async (socket) => {
        // when the client emits 'adduser', this listens and executes
	socket.on('snoop', async (room, id, perspective) => {
	
	   origin = socket.handshake.address
	   username = "Data Collector @ "+origin.address;
	   if(isBlank(room))
            room = "Limbo"
	
	    //don't log anything to the db if this flag is set
	    socket.temporary = false;
	
	    // store the username in the socket session for this client
	    socket.username = username;
	    // store the room name in the socket session for this client
	    socket.room = room;
            //socket.Id = id;
	    // add the client's username to the global list
	    if(!usernames[room])
	      usernames[room] = {};
	    usernames[room][username] = id;

            if(!user_perspectives[room])
              user_perspectives[room] = {};
            user_perspectives[room][username] = perspective;

	    // send client to room 1
	    socket.join(room);
	    // echo to client they've connected
		
	    loadHistory(socket, true);
	});


	// when the client emits 'adduser', this listens and executes
	socket.on('adduser', async (room, username, temporary, id, perspective) => {
	
	    console.log("info", "socket.on_adduser: -- room: " + room + "  -- username: " + username + "  -- id: " + id);
           
           if(username != "VirtualErland" || username != "BazaarAgent")
	   {
                if(room in numUsers)
		{		
           		numUsers[room] = numUsers[room] + 1;
		}
		else
		{
			numUsers[room] = 1;
		}	
	   }
           //logger.log("info",username +" connects");
           if(isBlank(username))
	   {
	       origin = socket.handshake.address
	       username = "Guest "+(origin.address+origin.port).substring(6).replace(/\./g, '');
	   }
	   
	   if(isBlank(room))
            room = "Limbo"
	
	    //don't log anything to the db if this flag is set
	    socket.temporary = temporary;
	
	    // store the username in the socket session for this client
	    socket.username = username;
	    // store the room name in the socket session for this client
	    socket.room = room;
            //console.log(id);
            socket.Id = id;
	    // add the client's username to the global list
	    if(!usernames[room])
		usernames[room] = {};
	    usernames[room][username] = id;

            if(!user_perspectives[room])
              user_perspectives[room] = {};
            user_perspectives[room][username] = perspective;

	    // send client to room 1
	    socket.join(room);
	    // echo to client they've connected
	    
	    if(!user_sockets[room])
		user_sockets[room] = {};
	    user_sockets[room][username] = socket;
	    
            
	        	
	    loadHistory(socket, false);
	    io.sockets.in(socket.room).emit('updateusers', usernames[socket.room], user_perspectives[socket.room], "update");
	    //socket.emit('updaterooms', [room,], room);
	});

	// when the client emits 'sendchat', this listens and executes
	socket.on('sendchat', async (data)  => {
		// we tell the client to execute 'updatechat' with 2 parameters
		// console.log("info","socket.on_sendchat: -- room: " + socket.room + "  -- username: " + socket.uusername + "  -- text: " + data);
		logMessage(socket, data, "text");
		io.sockets.in(socket.room).emit('updatechat', socket.username, data);
	});
	

	// when the client emits 'sendpm', this listens and executes
	socket.on('sendpm', async (data, to_user)  => {
		// we tell the client to execute 'updatechat' with 2 parameters
		logMessage(socket, data, "private");
		if(socket.room in user_sockets && to_user in user_sockets[socket.room])
    		user_sockets[socket.room][to_user].emit('update_private_chat', socket.username, data);
	});
	
	
    // when the client emits 'sendhtml', this listens and executes
	socket.on('sendhtml', async (data) => {
		// we tell the client to execute 'updatechat' with 2 parameters
		//console.log("sending html "+data+"on behalf of "+socket.username)
		
		logMessage(socket, data, "html");
		io.sockets.in(socket.room).emit('updatehtml', socket.username, data);
	});
	
	// when the client emits 'sendimage', this listens and executes
	socket.on('sendimage', async (data)  => {
		logMessage(socket, data, "image");
		io.sockets.in(socket.room).emit('updateimage', socket.username, data);
	});

 
	// when the client emits 'ready', this listens and executes
	socket.on('ready', async (data)  => {
		logMessage(socket, data, "ready");
		io.sockets.in(socket.room).emit('updateready', socket.username, data);
	});
	
        // when the client emits 'global_unready', this listens and executes
	socket.on('global_ready', async (data) => {
		logMessage(socket, "global "+data, "ready");
		io.sockets.in(socket.room).emit('update_global_ready', data);
	});

	socket.on('switchRoom', async (newroom) => {
		// leave the current room (stored in session)
	    if(socket.room in usernames && socket.username in usernames[socket.room])
	        delete usernames[socket.room][socket.username];
	    io.sockets.in(socket.room).emit('updateusers', usernames[socket.room]);
	    io.sockets.in(socket.room).emit('updatepresence', username,  'leave');
		
	    logMessage(socket, "leave", "presence");
	    socket.leave(socket.room);
	    // join new room, received as function parameter
	    socket.join(newroom);
	    // sent message to OLD room
	    // update socket session room title
	    socket.room = newroom;
	    
	    usernames[socket.room][socket.username] = username;
	    io.sockets.in(socket.room).emit('updateusers', usernames[socket.room]);
	    io.sockets.in(socket.room).emit('updatepresence', username, 'join');
	    
	    socket.emit('updaterooms', [room,], newroom);
	    logMessage(socket, "join", "presence");
	});

	// when the user disconnects... perform this
	socket.on('disconnect', async () => {
    try {
      console.log("info", "socket.on_disconnect: -- room: " + socket.room + "  -- username: " + socket.username + "  -- id: " + usernames[socket.room][socket.username]);
    } catch (e) {
    }

    if ((socket.username != "VirtualErland" || socket.username != "BazaarAgent") && socket.room in numUsers) {
      numUsers[socket.room] = numUsers[socket.room] - 1;
    }
    if (socket.room in usernames && socket.username in usernames[socket.room]) {
      // remove the username from global usernames list
      const id = usernames[socket.room][socket.username];
      const perspective = user_perspectives[socket.room][socket.username];
      delete usernames[socket.room][socket.username];
      if (usernames[socket.room]) {
        // update list of users in chat, client-side
        io.sockets.in(socket.room).emit('updateusers', usernames[socket.room], user_perspectives[socket.room], "update");
        // echo globally that this client has left

        io.sockets.in(socket.room).emit('updatepresence', socket.username, 'leave', id, perspective);
        logMessage(socket, "leave", "presence");
      }
    }

    if (socket.room in user_sockets && socket.username in user_sockets[socket.room]) {
      delete user_sockets[socket.room][socket.username];
    }

    if (socket.room)
      socket.leave(socket.room);

  });
});
