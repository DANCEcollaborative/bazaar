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

const { EventHubConsumerClient } = require("@azure/event-hubs");
const { ContainerClient } = require("@azure/storage-blob");    
const { BlobCheckpointStore } = require("@azure/eventhubs-checkpointstore-blob");

const connectionString = "";    
const eventHubName = "useractions";
const consumerGroup = "$Default"; // name of the default consumer group
const storageConnectionString = "";
const containerName = "eventhubcheckpoints";

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
//console.log("sleep start");
  const start = new Date().getTime();
  while (1) {
    if ((new Date().getTime() - start) > milliseconds){
      break;
    }
  }
//console.log("sleep over");
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
    res.setHeader("Content-Security-Policy", "default-src https://docs.google.com/ https://erebor.lti.cs.cmu.edu:* https://bazaar.lti.cs.cmu.edu:* https://forum.lti.cs.cmu.edu:* https://collab.lti.cs.cmu.edu:* https://cdn.jsdelivr.net/gh/DANCECollaborative/; connect-src 'self' ws://bazaar.lti.cs.cmu.edu/bazsocket/ http://bazaar.lti.cs.cmu.edu/bazsocket/ wss://bazaar.lti.cs.cmu.edu/bazsocket/ https://bazaar.lti.cs.cmu.edu/bazsocket/ ws://bazaar.lti.cs.cmu.edu/bazsocket/ http://bazaar.lti.cs.cmu.edu/local/bazsocket/ wss://bazaar.lti.cs.cmu.edu/local/bazsocket/ https://bazaar.lti.cs.cmu.edu/ https://cdn.jsdelivr.net/gh/DANCECollaborative/; style-src 'self' https://fonts.googleapis.com/css https://cdn.jsdelivr.net/gh/DANCECollaborative/ https://rawgit.com/gtomar/ 'unsafe-inline'; script-src 'self' https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js https://rawgit.com/gtomar/ https://rawgit.com/marinawang/ https://cdnjs.cloudflare.com/ajax/libs/socket.io/ 'unsafe-inline'; font-src 'self' https://fonts.gstatic.com/; img-src * data"); 
    return next();
});

app.get('/room_status_all', async (req, res) => {
  //console.log("app.get('/room_status_all')");
  const query = 'SELECT name from nodechat.room where name like "normaldist%"';
  // console.log(query);
  try {
    const [rows, fields] = await promisePool.query(query);
    let num_list = "";
    for (var i = 0; i < rows.length; i++) num_list += "<p>" + rows[i].name + "</p>";
    res.send("<body>" + num_list + "</body>");
  } catch (err) {
  //console.log(err);
    res.send(500, "<body><h2>Error</h2><p>Couldn't fetch data</p></body>");
  }

});

app.get('/room_status*', async (req, res) => {
  //console.log("app.get('/room_status*')");
  const query = 'SELECT name from nodechat.room where name like "normaldist%"';
  // console.log(query);
  try {
    const [rows, fields] = await promisePool.query(query);
    let num_list = "";
    for (var i = 0; i < rows.length; i++) num_list += "<p>" + rows[i].name + "</p>";
    res.send("<body>" + num_list + "</body>");
  } catch (err) {
  //console.log(err);
    res.send(500, "<body><h2>Error</h2><p>Couldn't fetch data</p></body>");
  }

});

app.get('/welcome*', async (req, res) => {
  //console.log("Welcome");
    res.sendFile(__dirname + ath.join(__dirname, './html_pages/' + '/welcome.html'));
});


// May call setTeam_ to launch an agent 
app.get('/login*', async (req, res) => {
//console.log("Enter app.get /login");
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
  //console.log("error from db room insert " + err);
  }

  if (1) {        								// if(1) is always true? 
    teamNumber = req.query.roomId;
    setTeam_(teamNumber, req, logger, res);
  }
//console.log("Exit app.get /login");
});


// May launch an agent directly -- IS THIS USED? 
app.post('/login*', async (req, res) => {
//console.log("Enter app.post /login");
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
      //console.log("exec ../bazaar/launch_agent_docker.sh " + req.query.roomName + " " + teamNumber + ' "none"');
        exec("../bazaar/launch_agent_docker.sh " + req.query.roomName + " " + teamNumber + ' "none"', puts);
        sleep(5000);
        setTeam(teamNumber, req, logger, res);
      } else {
        setTeam(teamNumber, req, logger, res);
      }
      // console.log("successful");
    }
  } catch (err) {
  //console.log(err);
    res.send(500, header_stuff + "<body><h2>Error</h2><p>" + err.message + "':</p><pre>" + err + "</pre></body>");
  }
//console.log("Exit app.post /login");

});


function createWorker() {
  return new Worker('./agentup.js');
}

const worker = createWorker();

const agentLaunch = async (roomName, teamNumber) => {
//console.log("agentLaunch: Launching agent -- roomName = " + roomName + "  -- teamNumber = " + teamNumber);
  worker.postMessage({
    roomName: roomName,
    teamNumber: teamNumber
  });
}


function setTeam_(teamNumber,req,logger,res) {
  //console.log("Enter setTeam_");
	if( (!(req.query.roomName + teamNumber in numUsers)) )
	{
			numUsers[req.query.roomName + teamNumber] = 0;
		//console.log("setTeam_: agentLaunch(" + roomname_prefix + "," + teamNumber + ")");
			agentLaunch(req.query.roomName, teamNumber);
	}
	//teamNumber = req.query.roomId;
	let html_page = 'index';
	if(req.query.html != undefined) html_page = req.query.html;

	const roomname = req.query.roomName + teamNumber;
	const url = localURL + '/chat/' + roomname  + '/' + req.query.id + '/' +
														 req.query.username + '/' + req.query.perspective + '/' + '?html=' + html_page + '&forum=' + req.query.forum;

//console.log("setTeam_, url: " + url);
	res.writeHead(301,{Location: url});
	res.end();

	logger.log("info","Number of users : " + numUsers[roomname]);
	logger.log("info","Team number : " + teamNumber);

	//Replace accepts a value between 0 and 1.
   /* provider.outcome_service.send_replace_result(1, function(err, result){
	//console.log("Grade submitted: " + result) // True or false
	});*/

  //console.log("Exit setTeam_");
}


function setTeam(teamNumber,req,provider,logger,res)
{
  //console.log("Enter setTeam");
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
//console.log("setTeam, url: " + url);

	res.writeHead(301,{Location: url});
	res.end();

	logger.log("info","Number of users : " + numUsers[roomname]);
	logger.log("info","Team number : " + teamNumber);
	logger.log("info","provider : " + provider.username);
//console.log(provider);
	//console.log(provider);
	//Replace accepts a value between 0 and 1.
	provider.outcome_service.send_replace_result(1, function(err, result){
	//console.log("Grade submitted: " + result) // True or false
	});
  //console.log("Exit setTeam");
        
}


function setTeam_fromSocket(roomName,teamNumber,userID,username,logger) {
  //console.log("Enter setTeam_fromSocket");
    const roomNameAndNumber = roomName + teamNumber;
    let perspective = null;							// hardcoded for now
    let forum = "undefined";						// hardcoded for now
	if ( (!(roomNameAndNumber in numUsers)) )
	{
			numUsers[roomNameAndNumber] = 0;
			// console.log("setTeam_fromSocket: agentLaunch(" + roomName + "," + paddedTeamNumber + ")");
		//console.log("setTeam_fromSocket: agentLaunch(" + roomName + "," + teamNumber + ")");
			agentLaunch(roomName, teamNumber);
	}
	logger.log("info","Number of users : " + numUsers[roomNameAndNumber]);
	logger.log("info","Team number : " + teamNumber);
  //console.log("Exit setTeam_fromSocket");
}


function addUser(socket, room, username, temporary, id, perspective) {
    if (username != "VirtualErland" || username != "BazaarAgent") {		// This seems intended to exclude Bazaar agents from user count but is incomplete
        if (room in numUsers) {		
           	numUsers[room] = numUsers[room] + 1;
		} else {
			numUsers[room] = 1;
		}	
	}
	
    if (isBlank(username)) {
    //console.log("isBlank(username) is true; username = " + username);
	    origin = socket.handshake.address
	    username = "Guest "+(origin.address+origin.port).substring(6).replace(/\./g, '');
	}
	   
   	if(isBlank(room))
		room = "Limbo"
	
	socket.temporary = temporary;   // don't log anything to the db if this flag is set	
	socket.username = username;		// store the username in the socket session for this client
	socket.room = room;				// store the room name in the socket session for this client	
	socket.Id = id;					// ??? I think socket.id is set automatically; why socket.Id (title case)? 	
	
	// add the client's username to the global list
	if(!usernames[room])
		usernames[room] = {};
	usernames[room][username] = id;

	// set user perspective 
	if(!user_perspectives[room])
	  	user_perspectives[room] = {};
	user_perspectives[room][username] = perspective;

	// Join room 
//console.log("function addUser: Joining room " + room);
	socket.join(room);
	
	// Add to user_sockets list  
	if(!user_sockets[room])
		user_sockets[room] = {};
	user_sockets[room][username] = socket;
						
	loadHistory(socket, false);			// ??? Why is history loaded? 
	io.sockets.in(socket.room).emit('updateusers', usernames[socket.room], user_perspectives[socket.room], "update");
	//socket.emit('updaterooms', [room,], room);
}



function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}


app.get('/chat*', async (req, res) => {

//console.log("Enter app.get /chat");
  let html_page = 'index';
  if (req.query.html !== undefined) html_page = req.query.html;

  res.sendFile(path.join(__dirname, './html_pages/' + html_page + '.html'));
//console.log("Exit app.get /chat");
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


app.get('*', async (req, res) => {
 //console.log("app.get(*): ");
});





app.post('/',function(req, res) {
    const provider = new lti.Provider(consumer_key, consumer_secret);
    const isValidG = false;
    provider.valid_request(req,function(err,isValid){
        isValidG = isValid;
    });
    if(isValidG){
      //console.log(provider.userId);
        // res.writeHead(301,{Location: 'http://erebor.lti.cs.cmu.edu:8007/team?userId='+provider.userId});
        res.end();

        //grading
        const score = 0.5; //dummy grade

        if(score >=0 && score <=1){
          //console.log(provider);
            outcome_url = provider.lis_outcome_service_url;
            //outcome_url = outcome_url.replace("https","http");
            outcome_url = outcome_url.replace("localhost",req.ip);
            result_sourcedid = provider.lis_result_sourcedid;
            //result_sourcedid = result_sourcedid.replace("https","http");
            result_sourcedid = result_sourcedid.replace("localhost",req.ip);
          //console.log("outcome_url is: "+outcome_url);
          //console.log("result source id is: "+result_sourcedid);

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
              //console.log("res.body: "+res.body);
            });
        }
    }else{
        res.end("Invalid consumer secret and consumer key!");
    }
    
    
}).listen(8006);

console.log("server is running on 8006");



// === LOBBY CODE === // 

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
    GROUP_DELAY = 	0, // hyeju changed this value -- then Chas changed it from 15*1000
    // LAST_RESORT_TIMEOUT =  2*50*1000, //this is when the system tells students to come back later
    LAST_RESORT_TIMEOUT =  600*1000, //this is when the system tells students to come back later
    // LOCKDOWN_TIMEOUT = 15*60*1000; //no more students!
    // LOCKDOWN_TIMEOUT =  7*24*60*60*1000; //lobby open for 7 days
    LOCKDOWN_TIMEOUT =  365*24*60*60*1000; //lobby open for 1 year
	
let LOCKDOWN_TIME = 0;
// let LOCKDOWN_TIME = 365*24*60*60*1000; //lobby open for 1 year
let START_TIME = new Date().getTime();
const chat_url = "https://bazaar.lti.cs.cmu.edu/bazaar/chat/";
const roomname_prefix = "jeopardybigwgu";
// const create_script = "../../scripts/create-cc-rooms.sh"

// when the daemon started
let starttime = (new Date()).getTime();
let lastJoin = new Date();
let teamMemberNames = {};

function getLoginInstructionText(nick)
{
    const now = new Date().getTime();
    message = "<p>Welcome to the matchmaker lobby. Hang on for a few minutes. We'll match you up with a partner as soon as enough students join.</p><p>Please confirm that you are in the lobby during your assigned activity time (Friday at 6pm <b>Mountain Time</b>). Any participation outside the designated activity time will not count as participation in this study.</p>"
    return message;
}

function getUserInstructionText(nick, i, condition)
{    
    instructions = 'After you join your team\'s discussion area, please follow the virtual agent\'s instructions there.';
    return instructions;
}

let conditionOffset = -1;
let numTeams = 1500;
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
{   // This message was showing when it shouldn't. Most expeditious fix was commenting out the message. 
    // winston.log('info', new Date(LOCKDOWN_TIME) + " <-- lockdown set");
	// setTimeout(function (){ channel.appendMessage("System", "msg", "This session is now closed. No new students can join. If you don't have a partner yet, please come back later!" ,"all"); }, LOCKDOWN_TIME - new Date().getTime())
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
        if(channel.isVisible(m, pending.nickUnique))
        {
            pending.callback([m]);
            callbacks.splice(i, 1);
        }
    }

    while (messages.length > MESSAGE_BACKLOG)
      messages.shift();
  };

  this.getMessages = function (since, nickUnique, callback) 
  {
    const matching = [];
    
    
    for (var i = 0; i < messages.length; i++) 
    {
        var message = messages[i];
        if(message.timestamp > since && channel.isVisible(message, nickUnique))
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
/*  
  this.isVisible = function(message, nick)
	{     return nick && (message.target_nick == nickUnique 
			      || nick == "System" || nick == message.nick 
			      || (message.nick == "System" 
				  && (message.target_nick == "all" 
				      || message.target_nick == "everyone")));   
	}
*/  
  this.isVisible = function(message, nickUnique)
	{     return message.target_nick == nickUnique || message.target_nick == "all" || message.target_nick == "everyone"; 
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
  // winston.log('info',"server_lobby_https.js, createSession -- " + nick + " trying to join, consent="+consent + " reset="+reset);
  if (nick.length > 50) return null;
   
  //rejects invalid nicknames
  if (/[^\w_\-^! ]/.exec(nick)) 
  {
	winston.log('info',"server_lobby_https.js, createSession -- invalid nickname: " + nick);
	  return null;
  }

/*
  for (const i in sessions) 
  {
    const session = sessions[i];
    if (session && session.nick === nick)     // I think this handles repeat user nicknames by destroying any previous with same nickname
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
*/

  const session = 
  { 
    nick: nick, 
    id: (nextID++).toString(),
    timestamp: new Date(),
    // timeString: new Date().getTime().toString(),
    nickUnique: nick + new Date().getTime().toString(),
    // nickUnique: nick + timeString,
    consent: consent,

	// console.log("In const session, nickUnique = " + nickUnique); 

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
  // console.log("Enter setInterval");
  const now = new Date(); 
  // console.log("setInterval, now = " + now.toString()); 
 
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

			//console.log("setInterval: about to 'exec' ../bazaar/launch_agent_docker.sh "+roomname_prefix+" "+paddedTeamNumber+' "'+condition+'"')
                exec("../bazaar/launch_agent_docker.sh "+roomname_prefix+" "+paddedTeamNumber+' "'+condition+'"', puts);
                //break;
            }
            else team = [];
    } else if(team.length == 0 
	    && supplicants.length > 0
	    && supplicants.indexOf(session) >= 0
	    && now - lastJoin > LAST_RESORT_TIMEOUT) { 
	    
		// console.log("About to send timeout message");
		member = session;
		winston.log('info', "advising student "+member.nickUnique+" to come back later");
		// channel.appendMessage("System", "msg", member.nick+", there's nobody else to match you with right now. Below you can see the best times to come back to meet discussion partners.<br/><img src=\"http://erebor.lti.cs.cmu.edu/dal/dist.png\" width=\"600\" height=\"250\">", member.nick);    
		channel.appendMessage("System", "msg", member.nick+", it looks like there is not a match for you because no one else is present in the lobby right now for this activity. Don't worry! You are still eligible to complete the 3rd part of this study to receive your payment. *IMPORTANT*: In order to remain eligible for the rest of this study, <b><a href=\"https://forms.gle/HNJUWYzaLYBwL53t6\">please follow this link and enter your email address</a></b> to mark your attendance in the lobby today.", member.nickUnique);              
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
//console.log("condition: "+condition);

	for(var i = 0; i < team.length; i++)
        {
         
            const member = team[i];
            
            const memberID = i + 1; 
            const html = "jeopardy"
	
            const roomname = roomname_prefix+pad(teamNumber,2);
            const url = chat_url + roomname + "/" + memberID + "/" + member.nick + "/undefined/?html=" + html + "&forum=undefined";
            
	    const instructions = getUserInstructionText(member.nick, i, condition);

            supplicants.splice(supplicants.indexOf(member),1);
            teamMemberNames[member.nickUnique] = {number:teamNumber, url:url, condition:condition};
            
            winston.log('info', "adding "+member.nickUnique+" to Team "+teamNumber+", condition="+condition);
            teamLogger.log('info', "User:"+member.nickUnique+", Team:"+teamNumber+", condition:"+condition);
            

	    channel.appendMessage("System", "msg", member.nick+", you're on Team "+teamNumber+".", member.nickUnique)            
            setTimeout(function(member, teamNum, url, instructions)
            {
                return function()
                {
                    instructions = '<p>Your team\'s discussion area is ready, '+member.nick+'. <a href="'+url +'" target="_blank"> Follow this link to join the discussion.</a></p><p>'+instructions+'</p>';
        	    channel.appendMessage("System", "msg", instructions, member.nickUnique);
                }
            }(member, teamNumber, url, instructions), 4000); 
        }
	team = [];
    }
  }
  // console.log("Exit setInterval");
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

//this is the incoming message from the client that triggers session creation.
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
      channel.appendMessage("System", "msg", "Welcome.", session.nickUnique);
  }
  else if(reset==="true" || !(nickUnique in teamMemberNames)) //don't re-apply existing names to the team pool - they can re-join their old team!
  {
      const now = new Date();
      if(LOCKDOWN_TIMEOUT > 0 && now > LOCKDOWN_TIME)
      {
          channel.appendMessage("System", "msg", "Sorry, this lobby session has closed already.", session.nickUnique);
      }
      else
      {
	// hyeju changed the mssage here to "hi"
	  if(reset==="true")
	      channel.appendMessage("System", "msg", "Hi, "+nick+".", session.nickUnique);

          lastJoin = new Date();
          supplicants.push(session);
          const instruction = getLoginInstructionText(session.nick);
          if(instruction)
	      channel.appendMessage("System", "msg",instruction, session.nickUnique); 
      }
  }
  else
  {
      channel.appendMessage("System", "msg", "Welcome back, "+nick+"! Your username is already active on Team "+teamMemberNames[nickUnique].number
        +'. <a href="'+teamMemberNames[nickUnique].url+'" target="_blank">Follow this link to return to your team\'s discussion area.</a>', nick);  
      //channel.appendMessage("System", "msg", "Remember, "+teamMemberNames[nickUnique].instructions, nickUnique);
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
    nickUnique = session.nickUnique; 
    session.poke();
  }

  const since = parseInt(parsed.since, 10);
  if(session)
  {
      channel.getMessages(since, nickUnique, function (messages) 
      {
      
        let myMessages = [];
        session.poke();
        for(var i = 0; i < messages.length; i++)
        {
          const message = messages[i];
          //if (nick && (message.target_nick == nickUnique || nick == "System" /* || nick == message.nick */))
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

async function eventHubReceive() {
  // Create a blob container client and a blob checkpoint store using the client.
  const containerClient = new ContainerClient(storageConnectionString, containerName);
  const checkpointStore = new BlobCheckpointStore(containerClient);

  // Create a consumer client for the event hub by specifying the checkpoint store.
  const consumerClient = new EventHubConsumerClient(consumerGroup, connectionString, eventHubName, checkpointStore);

  // Subscribe to the events, and specify handlers for processing the events and errors.
  const subscription = consumerClient.subscribe({
      processEvents: async (events, context) => {
	if (events.length === 0) {
          // console.log(`No events received within wait time. Waiting for next interval`);
          return;
        }
        for (const event of events) {
        //console.log(`=== eventHubReceive event: '${event.body}' from partition: '${context.partitionId}' and consumer group: '${context.consumerGroup}'`);
          eventHubPostTest(); 
        }
        // Update the checkpoint.
        await context.updateCheckpoint(events[events.length - 1]);
      },

      processError: async (err, context) => {
      //console.log(`Error : ${err}`);
      }
    }
  );

  // After 30 seconds, stop processing.
  //   await new Promise((resolve) => {
  //     setTimeout(async () => {
  //       await subscription.close();
  //       await consumerClient.close();
  //       resolve();
  //     }, 30000);
  //   });
}


eventHubReceive().catch((err) => {
//console.log("Error occurred: ", err);
}); 


function callback(error, response, body) {
    if (!error && response.statusCode == 200) {
      //console.log("=== callback response 200, body = " + body);
    }
}


function eventHubPostTest () {

	var headers = {
		'content-type': 'application/json'
	};

	var dataString = '{"admin_secret": "ymuabuippdimkgskicwuxaknlkzkxqeo"}';

	var options = {
		url: 'https://opemanager.azurewebsites.net/auth/admin',
		method: 'POST',
		headers: headers,
		body: dataString
	};
	
	request(options, callback);
}
 

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


function isClientServerConnection(auth) {
  // If the auth object is present, and has a "configuration" property,
  // which is an object that itself has a "clientID" property, whose value
  // is equal to 'DCSS', then this is a DCSS client connection.
  return auth && auth.agent && auth.agent.configuration && 
  		 (auth.agent.configuration.clientID === 'ClientServer' || auth.agent.configuration.clientID === 'DCSS');
}

function translateClientServerAuthToBazaar(auth) {
  /*
    auth format follows. *** indicates property is currently used
    {
      agent: {
        id: int,      			***
        name: string,			***
        configuration: {
          clientID: string		***
        }
      }
      chat: {
        id: int					***
      }
      run: {
        id: int
      },
      user: {
        id: int,
        name: string
        role: {
        	id, name, description
        },
      }
    }    
  */  

  return {
    clientID: auth.agent.configuration.clientID,
    agent: auth.agent.name,
    roomName: auth.chat.id,
    userID: auth.user.id,
    username: auth.user.name
  };
}





function exportCSV(room, res) {
	// const connection = mysql.createConnection(mysql_auth);
	
	 
	pool.query("SELECT DATE_FORMAT(m.timestamp, '%Y-%m-%d'), DATE_FORMAT(m.timestamp, '%H:%i:%s'),  m.type, m.content, m.username from nodechat.message "
				+ 'as m join nodechat.room as r on m.roomid=r.id '
				+ 'where r.name=' + pool.escape(room) + ' order by timestamp', function (err, rows, fields) { 

	if(err) {
	//console.log(err);
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
//console.log("Enter loadHistory");
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
					//console.log(err);						
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
//console.log("Exit loadHistory");
}

function logMessage(socket, content, type) {   
//console.log("Enter logMessage");	
//console.log("logMessage, socket.room = " + socket.room);
//console.log("logMessage, socket.roomid = " + socket.roomid);
//console.log("logMessage, socket.username = " + socket.username);

    if(socket.temporary) return;

    //const connection = mysql.createConnection(mysql_auth);
       
  	pool.query('update nodechat.room set modified=now() where room.name=' + pool.escape(socket.room) + ';', function (err, rows, fields) {
         if (err) {
         //console.log("Error on update nodechat.room set modified=now() where room.name=' + pool.escape(socket.room) + ';', function (err, rows, fields)")
          //console.log(err);
        	}
    });
    
    endpoint = "unknown"
    if(socket.handshake)
		endpoint = socket.handshake.address;
		
//console.log("logMessage, pool.escape(socket.room) = " + pool.escape(socket.room));
//console.log("logMessage, pool.escape(socket.username) = " + pool.escape(socket.username));
//console.log("logMessage, pool.escape(endpoint.address) = " + pool.escape(endpoint.address));
//console.log("logMessage, pool.escape(endpoint.port) = " + pool.escape(endpoint.port));
//console.log("logMessage, pool.escape(socket.Id) = " + pool.escape(socket.Id));
//console.log("logMessage, pool.escape(socket.id) = " + pool.escape(socket.id));
//console.log("logMessage, pool.escape(content) = " + pool.escape(content));
//console.log("logMessage, pool.escape(type) = " + pool.escape(type));
	
	
	
    query = 'insert into nodechat.message (roomid, username, useraddress, userid, content, type, timestamp)' 
    		+ 'values ((select id from nodechat.room where name=' + pool.escape(socket.room) + '), '
    		+ '' + pool.escape(socket.username) + ', ' + pool.escape(endpoint.address + ':' + endpoint.port) + ', ' + pool.escape(socket.Id) + ', ' + pool.escape(content) + ', ' 
    		+ pool.escape(type) + ', now());';                   

//console.log("logMessage: starting pool.query to mysql2");  
 	 pool.query(query, function (err, rows, fields) {
         if (err) {
         //console.log("Error on pool.query(query, function (err, rows, fields)")
          //console.log(err);
        }
            
    });   
//   connection.end()  
  //console.log("logMessage: completed pool.query to mysql2 {by giving to worker thread}");  
//console.log("Exit logMessage");  
}

// io.set('log level', 1);
// DEBUG=io*

io.sockets.on('connection', async (socket) => {

	// console.log("socket.handshake.auth.token = " + socket.handshake.auth.token);
	// console.log("socket.handshake.auth.clientID = " + socket.handshake.auth.clientID);

 	if (isClientServerConnection(socket.handshake.auth)) {

		const {
		  token,
		  clientID, 
		  agent,
		  roomName,
		  userID,
		  username
		} = translateClientServerAuthToBazaar(socket.handshake.auth);
		  
	console.log("socket ID: " + socket.id);
	console.log("token = " + token);
	console.log("clientID = " + clientID);
	console.log("agent = " + agent);
	console.log("roomName = " + roomName);
	console.log("userID = " + userID);
	console.log("username = " + username); 
				
	socket.clientID = clientID;    		
	socket.agent = agent;  				// agent ==> roomName elsewhere in this file
	socket.roomName = roomName;         // roomName ==> teamNumber elsewhere in this file 
	socket.userID = userID;  
	room = agent + roomName; 
	//console.log("room: " + room); 
		
		logger = winston.createLogger({
    		transports: [
      			new (winston.transports.Console)()]
  		});	
		setTeam_fromSocket(agent,roomName,userID,username,logger);
		
		let temporary = false; 
		let perspective = null; 
		addUser(socket, room, username, temporary, userID, perspective)
	}
	

    // when the client emits 'snoop', this listens and executes
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
	  //console.log("info", "Enter socket.on_adduser: -- room: " + room + "  -- username: " + username + "  -- id: " + id);
	    addUser(socket, room, username, temporary, id, perspective);
	  //console.log("info", "Exit socket.on_adduser");
	});

	// when the client emits 'sendchat', this listens and executes
	socket.on('sendchat', async (data)  => {
	//console.log("Enter socket.on('sendchat')'"); 
	//console.log("socket.on('sendchat'): socket.clientID = " + socket.clientID);
	//console.log("socket.on('sendchat'): socket.room = " + socket.room);
		// we tell the client to execute 'updatechat' with 2 parameters
		// console.log("info","socket.on_sendchat: -- room: " + socket.room + "  -- username: " + socket.uusername + "  -- text: " + data);
		logMessage(socket, data, "text");
                console.log("socket.on('sendchat'): socket.clientID = " + socket.clientID + " socket.username = " + socket.username);
  
		if (socket.username == "MLAgent") {
		// if (socket.username == "DCSSLightSideAgent") {
                // if (socket.clientID == "DCSS") {		
		// if (socket.username == "DCSSLightSideAgent") {
		//console.log("socket.on('sendchat'): socket.username == DCSSLightSideAgent; about to emit 'interjection'");
			io.sockets.in(socket.room).emit('interjection', { message: data }); 
		} else {	
		//console.log("socket.on('sendchat'): socket.username *** NOT *** == DCSSLightSideAgent; about to emit 'updatechat'");	
			io.sockets.in(socket.room).emit('updatechat', socket.username, data);
		}
		
		
		// if (typeof socket.clientID !== 'undefined' ) {
		// //console.log("socket.on('sendchat'): socket.clientID NOT undefined");
		// 	if (socket.clientID == "DCSS") {
		// 	// if (socket.clientID == "DO_NOT_GO_HERE") {
		// 	//console.log("socket.on('sendchat'): socket.clientID = DCSS; about to emit 'interjection'");
		// 		io.sockets.in(socket.room).emit('interjection', socket.username, data); 
		// 	} else {	
		// 	//console.log("socket.on('sendchat'): socket.clientID NOT = DCSS");	
		// 		io.sockets.in(socket.room).emit('updatechat', socket.username, data);
	// 		}
	// 	} else {	
	// 	//console.log("socket.on('sendchat'): socket.clientID is UNDEFINED");         // This is the current path 
	// 		io.sockets.in(socket.room).emit('updatechat', socket.username, data);
	// 	}
	//console.log("Exit socket.on('sendchat')"); 
			
	});


	// when the client emits 'request', this listens and executes
	socket.on('request', async (data)  => {	
	//console.log("Enter socket.on_request"); 	
	//console.log("socket.username: " + socket.username);
		// io.sockets.in(socket.room).emit('updatechat', socket.username, data);
		socket.in(socket.room).broadcast.emit('updatechat', socket.username, data.value);
	//console.log("Exit socket.on_request"); 
	})
	
	

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
    //console.log("info", "socket.on_disconnect: -- room: " + socket.room + "  -- username: " + socket.username + "  -- id: " + usernames[socket.room][socket.username]);
    } catch (e) {
    }


	// TEMPORARILY DISTINGUISHING BY EXISTENCE OF AUTH TOKEN
 	if ( typeof socket.handshake.auth.token !== 'undefined' && socket.handshake.auth.token ) {
	//console.log("token is NOT 'undefined'; issuing -leave- with token");
		socket.leave(socket.handshake.auth.token);
	}
		
	else {


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
}

  });
});
