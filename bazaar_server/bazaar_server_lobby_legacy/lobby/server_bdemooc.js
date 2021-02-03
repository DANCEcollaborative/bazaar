var express = require('express');
var app = express();
var bodyParser = require('body-parser');
var lti = require('ims-lti');
var consumer_key = "BazaarLTI";
var consumer_secret = "BLTI";

//variables for grading
var url = require('url');
var request = require('request');
var OAuth   = require('oauth-1.0a');
var Crypto = require("crypto");


//var NonceStore = require('ims-lti');
//var nonceStore = NonceStore();
//var signature_method= "HMAC_SHA1";

var generateXML = function(lis_result_sourcedid , curScore){
    var result = '<?xml version = "1.0" encoding = "UTF-8"?>'
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

var generateRequest = function(Url,lis_result_sourcedid,curScore){
    var postHead = {
        url: Url,
        method: 'POST',
        'content-type': 'application/xml',
        data: generateXML(lis_result_sourcedid,curScore)
    };

    return postHead;
}

app.use(bodyParser.urlencoded());

app.post('/',function(req, res) {
    var provider = new lti.Provider(consumer_key, consumer_secret);
    var isValidG = false;
    provider.valid_request(req,function(err,isValid){
        isValidG = isValid;
    });
    if(isValidG){
        console.log(provider.userId);
        res.writeHead(301,{Location: 'http://erebor.lti.cs.cmu.edu:8007/team?userId='+provider.userId});
        res.end();

        //grading
        var score = 0.5; //dummy grade

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
            var oauth = OAuth({
                consumer: {
                    public: consumer_key,
                    secret: consumer_secret
                },
                signature_method: 'HMAC-SHA1'
            });

            var oauth_body_hash = Crypto.createHash('sha1').update(generateXML(result_sourcedid,score)).digest().toString('base64');
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
PORT = 8084; 

//var CONDITIONS = ["revoice", "press_reasoning", "agree"];
var CONDITIONS = ["none"]; // hyeju changed this value
var TEAM_SIZE = 2; // hyeju changed 3 => 2
//number of messages of history to keep in memory
var MESSAGE_BACKLOG = 200,
    //idle timeout before "booting" a user who's idle - at least five seconds
    SESSION_TIMEOUT =   7*1000,
//how long to wait after the most recent particpant has joined before attempting a team assignment - should be greater than session_timeout
    GROUP_DELAY = 	15*1000, // hyeju changed this value
    LAST_RESORT_TIMEOUT =  2*50*1000, //this is when the system tells students to come back later
//    LOCKDOWN_TIMEOUT = 15*60*1000; //no more students!
    LOCKDOWN_TIMEOUT =  7*24*60*60*1000; //lobby open for 7 days
	
var LOCKDOWN_TIME = 0;
var START_TIME = new Date().getTime();
var chat_url = "http://erebor.lti.cs.cmu.edu:8007/chat/";
var roomname_prefix = "week0-";
var create_script = "../../scripts/create-cc-rooms.sh"

// when the daemon started
var starttime = (new Date()).getTime();
var lastJoin = new Date();
var teamMemberNames = {};

function getLoginInstructionText(nick)
{
    var now = new Date().getTime();
    message = "Welcome to the matchmaker lobby. Hang on for a few minutes, we'll match you up with a team as soon as enough students join. "
    return message;
}

function getUserInstructionText(nick, i, condition)
{    
    instructions = 'After you join your team\'s discussion area, please follow VirtualCarolyn\'s instruction there.';
    return instructions;
}

var conditionOffset = -1;
var numTeams = 0;
var nextID = 0;
var teams = [];
var supplicants = [];
var winston = require('winston');

var sys = require('sys')
var exec = require('child_process').exec;
function puts(error, stdout, stderr) { sys.puts(stdout) }

var fu = require("./fu"),
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
sys.puts("node server.js [room_prefix] [first_team_num] [team_start_time | minutes_until_start] lockdown_time");
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
var teamLogger = new (winston.Logger)({
  transports: [
    new (winston.transports.Console)(),
    new (winston.transports.File)({ filename: 'logs/teams.log' })
  ]
});

winston.add(winston.transports.File, { filename: 'logs/server.log'});

winston.log('info', "The room prefix is "+roomname_prefix+". The first condition will be '"+CONDITIONS[numTeams+conditionOffset+1]+"'");
winston.log('info', "Teams will be assigned starting at "+new Date(START_TIME));
winston.log('info', "The first assigned team will be Team "+(numTeams+1));


var channel = new function () 
{
  var messages = [],
      callbacks = [];

  this.appendMessage = function (nick, type, text, target_nick) 
  {
    var m = { nick: nick
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
        var pending = callbacks[i];
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
    var matching = [];
    
    
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
    var now = new Date();
    while (callbacks.length > 0 && now - callbacks[0].timestamp > 5*1000) 
    {
      callbacks.shift().callback([]);
    }
  }, 2000);
};

var sessions = {};

//when a user joins the lobby
function createSession (nick,consent,reset) 
{
    winston.log('info', nick + " trying to join: consent="+consent + "reset="+reset);
  if (nick.length > 50) return null;
   
  //rejects invalid nicknames
  if (/[^\w_\-^! ]/.exec(nick)) return null;

  for (var i in sessions) 
  {
    var session = sessions[i];
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

  var session = 
  { 
    nick: nick, 
    id: (nextID++).toString(),
    timestamp: new Date(),
    consent: consent,

    poke: function () 
    {
      var now = new Date();
      session.timestamp = now;
    },

    destroy: function () 
    {
      channel.appendMessage(session.nick, "part", "bye", "System");
      var index = supplicants.indexOf(session)
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
    var str = '' + num;
    while (str.length < digits) 
    {
        str = '0' + str;
    }
   
    return str;
}

function shuffle(array) 
{
    var tmp, current, top = array.length;

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
  var now = new Date(); 
 
  for (var id in sessions) 
  {

    if (!sessions.hasOwnProperty(id)) continue;
    var session = sessions[id];

    if (now - session.timestamp > SESSION_TIMEOUT) 
    {
      winston.log('info', "booting "+session.nick+": "+(now - session.timestamp)+" ms since last update");
      
      session.destroy();
    }

    var team = []
    var teamNumber = 0;
    var condition = "?";
          
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


            var teamConsent = true; //change this when team consent matching logic is added back
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
		
                exec("./launch_agent.sh "+roomname_prefix+" "+teamNumber+' "'+condition+'"', puts);
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
	var theTeam = teams[teamNumber-1];
	var condition = CONDITIONS[0];//condition];        
	sys.puts("condition: "+condition);

	for(var i = 0; i < team.length; i++)
        {
         
            var member = team[i];
	    
            var roomname = roomname_prefix+pad(teamNumber, 2);
            var url = chat_url+roomname+"/"+member.nick;
            
	    var instructions = getUserInstructionText(member.nick, i, condition);

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
  var nicks = [];
  for (var id in sessions) {
    if (!sessions.hasOwnProperty(id)) continue;
    var session = sessions[id];
    nicks.push(session.nick);
  }
  res.simpleJSON(200, { nicks: nicks
                      , rss: 0
                      });
});

//this is the incoming message from the client that triggers serssion creation.
fu.get("/join", function (req, res) 
{
  var parsed = qs.parse(url.parse(req.url).query);
  var id = parsed.id;
  var nick = parsed.nick + "#" + id;
  var consent = parsed.consent;
//  var reset = parsed.reset;
  // hyeju changed this part
  var reset = "true";

  winston.log('info',req.url);
  winston.log('info',url.parse(req.url).query);
  winston.log('info',Object.keys(parsed));

  if (nick == null || nick.length == 0) {
    res.simpleJSON(200, {error: "Please use only letters and spaces in your name."});
    return;
  }
  
  var session = createSession(nick, consent==="true",reset==="true");
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
      var now = new Date();
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
          var instruction = getLoginInstructionText(session.nick);
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
  var id = qs.parse(url.parse(req.url).query).id;
  var session;
  if (id && sessions[id]) {
    session = sessions[id];
    session.poke();
  }
  res.simpleJSON(200, { rss: 0 });
});

//this is when the client can tell us that they've left -- doesn't always happen
fu.get("/part", function (req, res) {
  var id = qs.parse(url.parse(req.url).query).id;
  var session;
  if (id && sessions[id]) {
    session = sessions[id];
    session.destroy();
  }
  res.simpleJSON(200, { rss: 0 });
});

//the client is asking for new messages since the given timestamp
fu.get("/recv", function (req, res) {
  var parsed = qs.parse(url.parse(req.url).query);
  if (!parsed.since) 
  {
    res.simpleJSON(400, { error: "Must supply since parameter" });
    return;
  }
  var id = parsed.id;
  var nick = "";
  var session;
  if (id && sessions[id]) 
  {
    session = sessions[id];
    nick = session.nick;
    session.poke();
  }

  var since = parseInt(parsed.since, 10);
  if(session)
  {
      channel.getMessages(since, nick, function (messages) 
      {
      
        var myMessages = [];
        session.poke();
        for(i = 0; i < messages.length; i++)
        {
          var message = messages[i];
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
  var id = qs.parse(url.parse(req.url).query).id;
  var text = qs.parse(url.parse(req.url).query).text;

  var session = sessions[id];
  if (!session || !text) {
    res.simpleJSON(400, { error: "No such session id" });
    return;
  }

  session.poke();

  var target = "System"
  var atMatcher =  /^@(\S+)\s+(.*)$/;
  if(atMatcher.test(text) && session.nick == "System")
  {
    var match = atMatcher.exec(text);
    target=match[1];
    if(target == "all" || target == "everyone")
	text = match[2];
  }

  channel.appendMessage(session.nick, "msg", text, target);
  res.simpleJSON(200, { rss: 0 });
});
