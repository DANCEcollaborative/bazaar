console.log("IN")
var winston    = require('winston');
var mysql      = require('mysql');
var mysql_auth = {
      host     : 'localhost',
      user     : 'local',
      password : 'local',
      socketPath: '/var/run/mysqld/mysqld.sock',
    };
var connection;
var bodyParser = require('body-parser');
var lti = require('ims-lti');
var consumer_key = "BazaarLTI";
var consumer_secret = "BLTI";

function sleep(milliseconds) {
  console.log("sleep start");
  var start = new Date().getTime();
  while (1) {
    if ((new Date().getTime() - start) > milliseconds){
      break;
    }
  }
  console.log("sleep over");
}

function handleDisconnect() {
  connection = mysql.createConnection(mysql_auth); // Recreate the connection, since
                                                  // the old one cannot be reused.

  connection.connect(function(err) {              // The server is either down
    if(err) {                                     // or restarting (takes a while sometimes).
      console.log('error when connecting to db:', err);
      setTimeout(handleDisconnect, 2000); // We introduce a delay before attempting to reconnect,
    }                                     // to avoid a hot loop, and to allow our node script to
  });                                     // process asynchronous requests in the meantime.
                                          // If you're also serving http, display a 503 error.
  connection.on('error', function(err) {
    console.log('db error', err);
    if(err.code === 'PROTOCOL_CONNECTION_LOST') { // Connection to the MySQL server is usually
      handleDisconnect();                         // lost due to either server restart, or a
    } else {
	console.log('unknown db connection error', err)                      // connnection idle timeout (the wait_timeout
      //throw err;                                  // server variable configures this)
	handleDisconnect();
    }
  });
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

handleDisconnect();

var numUsers = {};
var teamNumber = {};
var express = require('express');
var csv = require('csv');
var sys = require('sys')
var exec = require('child_process').exec;

var app = express()
  , http = require('http')
  , server = http.createServer(app)
  , io = require('socket.io').listen(server);

var logger;

server.listen(80);
// routing


function puts(error, stdout, stderr) { sys.puts(stdout) }

io.set('log level', 1);

app.use(bodyParser.urlencoded());

app.post('/login*', function (req, res)
{
    var connection = mysql.createConnection(mysql_auth);
    var provider = new lti.Provider(consumer_key, consumer_secret);
    var isValidRequest = false;
    provider.valid_request(req,function(err,isValid){
        isValidRequest = isValid;
    });
    if(isValidRequest){
        connection.query("SELECT count from nodechat.room_prefix"
            +'where name='+connection.escape(req.query.roomName), function(err, rows, fields)
            {

                if(err)
                {
                     console.log(err);
                     res.send(500, header_stuff+"<body><h2>Error</h2><p>Couldn't fetch data for room '"+req.query.roomName+"':</p><pre>"+err+"</pre></body>");
                }
                else if(rows.length == 0)
                {
                    var connection = mysql.createConnection(mysql_auth);
		    connection.query('insert into nodechat.room_prefix set name='+connection.escape(req.query.roomName)+', created=NOW(), comment="auto-created", count=1;', function(err, rows, fields)
        	    {
                        if(err)
                	{
                     		console.log(err);
                     		res.send(500, header_stuff+"<body><h2>Error</h2><p>Couldn't create room '"+req.query.roomName+"':</p><pre>"+err+"</pre></body>");
                	}
			else
			{
				teamNumber[req.query.roomName] = 1;
			}
                    }
                    
                }
		else
		{
			teamNumber[req.query.roomName] = rows[0].count + 1;
			var connection = mysql.createConnection(mysql_auth);
			connection.query('update nodechat.room_prefix set count='+teamNumber[req.query.roomName]+' where name='+connection.escape(req.query.roomName), function(err, rows, fields)
		            {

                		if(err)
                		{
                     			console.log(err);
                     			res.send(500, header_stuff+"<body><h2>Error</h2><p>Couldn't update count for room '"+req.query.roomName+"':</p><pre>"+err+"</pre></body>");
                		}
			    }

		}
            });    

  
    	if( (!(req.query.roomName + pad(teamNumber[req.query.roomName], 2) in numUsers)) || numUsers[req.query.roomName + pad(teamNumber[req.query.roomName], 2)] <= 0)
    	{
        	/*if(req.query.roomName in teamNumber)
		{
			teamNumber[req.query.roomName] = teamNumber[req.query.roomName] + 1;
		}
		else
		{
			teamNumber[req.query.roomName] = 100;
		}*/
                numUsers[req.query.roomName + pad(teamNumber[req.query.roomName], 2)] = 0;
        	exec("../lobby/launch_agent.sh " + req.query.roomName + " " + teamNumber[req.query.roomName] + ' "none"', puts);
       		sleep(5000);
    	}

        var roomname = req.query.roomName + pad(teamNumber[req.query.roomName], 2);
	var url = 'http://bazaar.lti.cs.cmu.edu:80/chat/'+ roomname  + '/' + provider.userId + '/';
    	logger      = new (winston.Logger)({   transports: [
    	new (winston.transports.Console)(),
    	new (winston.transports.File)({ filename: roomname + ".log" })]});

    	res.writeHead(301,{Location: url});
    	res.end();

    	logger.log("info","Number of users : " + numUsers[roomname]);
    	logger.log("info","Team number : " + teamNumber[req.query.roomName]);

        //Replace accepts a value between 0 and 1.
        provider.outcome_service.send_replace_result(1, function(err, result){
            console.log("Grade submitted: " + result) // True or false
        });
        
   }
});

app.get('/chat*', function (req, res) 
{
    res.sendfile('index.html');
});

app.get('/observe/*', function (req, res) 
{
    res.sendfile('index.html');
});


app.get('/data/*', function (req, res) 
{
    groups = /\/data\/([^\/]+)/.exec(req.url)	  
    room = groups[1];

    exportCSV(room, res);

});

// sockets by username
var user_sockets = {};

// usernames which are currently connected to each chat room
var usernames = {};

// rooms which are currently available in chat
var rooms = [];

function isBlank(str)
{
    return !str || /^\s*$/.test(str)
}
var header_stuff = "<head>\n"+
"\t<link href='http://fonts.googleapis.com/css?family=Oxygen' rel='stylesheet' type='text/css'>\n"+
"\t<link href='http://ankara.lti.cs.cmu.edu/include/discussion.css' rel='stylesheet' type='text/css'>\n"+
"</head>";

function exportCSV(room, res)
{
            var connection = mysql.createConnection(mysql_auth);
            
             
            connection.query("SELECT DATE_FORMAT(m.timestamp, '%Y-%m-%d'), DATE_FORMAT(m.timestamp, '%H:%i:%s'),  m.type, m.content, m.username from nodechat.message "
            +'as m join nodechat.room as r on m.roomid=r.id '
            +'where r.name='+connection.escape(room)+' order by timestamp', function(err, rows, fields) 
            {

                if(err)
		{
		     console.log(err);
		     res.send(500, header_stuff+"<body><h2>Export Error</h2><p>Couldn't fetch data for room '"+room+"':</p><pre>"+err+"</pre></body>");
                }
		else if(rows.length == 0)
		{
		    res.send(404, header_stuff+"<body><h2>Empty Room</h2><p>Couldn't fetch data for empty room '"+room+"'.</p></body>");
		}
                else
		{
                     rows.unshift(['DATE', 'TIME', 'TYPE', 'TEXT', 'AUTHOR']);
                 
		     res.set('Content-Type', 'text/csv');
		     res.header("Content-Disposition", "attachment;filename="+room+".csv");
                     csv().from(rows).to(res);
                }
            });
      

}

function loadHistory(socket, secret)
{
    if(!socket.temporary)
    {
        //var connection = mysql.createConnection(mysql_auth);
        var id = null;
        if(socket.room in usernames && socket.username in usernames[socket.room])
        {
	    id = usernames[socket.room][socket.username];
        }
        connection.query('insert ignore into nodechat.room set name='+connection.escape(socket.room)+', created=NOW(), modified=NOW(), comment="auto-created";', function(err, rows, fields)
        {    
            setTimeout( function(socket)
            {
        
                var connection = mysql.createConnection(mysql_auth);
                 
                connection.query('SELECT m.timestamp, m.type, m.content, m.username from nodechat.message '
                +'as m join nodechat.room as r on m.roomid=r.id '
                +'where r.name='+connection.escape(socket.room)+' and not(m.type like "private") order by timestamp', function(err, rows, fields) 
                {
                     if (err) 
                        console.log(err);
                        
                    socket.emit('dump_history', rows);
		    
		    if(!secret)
		    {
			io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join', id);
			logMessage(socket, "join", "presence");
		    }
                });
            
                connection.end()
            }, 100, socket);

        });
    }
    else if(!secret)
    {
	io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join', id);
    }
}

function logMessage(socket, content, type)
{    
    if(socket.temporary) return;

    //var connection = mysql.createConnection(mysql_auth);
    
    
    connection.query('update nodechat.room set modified=now() where room.name='+connection.escape(socket.room)+';', function(err, rows, fields)
    {
         if (err) 
            console.log(err);
    });
    
    endpoint = "unknown"
    if(socket.handshake)
	endpoint = socket.handshake.address;
    query = 'insert into nodechat.message (roomid, username, useraddress, userid, content, type, timestamp)' 
                    +'values ((select id from nodechat.room where name='+connection.escape(socket.room)+'), '
                    +''+connection.escape(socket.username)+', '+connection.escape(endpoint.address+':'+endpoint.port)+', '+connection.escape(socket.id) +', '+connection.escape(content)+', '+connection.escape(type)+', now());';
                    
    
    connection.query(query, function(err, rows, fields) 
    {
         if (err) 
            console.log(err);
    });
    
//   connection.end()
    
}

io.sockets.on('connection', function (socket) {

        // when the client emits 'adduser', this listens and executes
	socket.on('snoop', function(room, id){
	
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
            socket.id = id;
	    // add the client's username to the global list
	    if(!usernames[room])
	      usernames[room] = {};
	    usernames[room][username] = id;
	    // send client to room 1
	    socket.join(room);
	    // echo to client they've connected
		
	    loadHistory(socket, true);
	});


	// when the client emits 'adduser', this listens and executes
	socket.on('adduser', function(room, username, temporary, id){
           
           if(username != "VirtualErland")
	   {
                if(room in numUsers)
		{		
           		numUsers[room] = numUsers[room] + 1;
		}
		else
		{
			numUser[room] = 1;
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
	    socket.temporary = temporary
	
	    // store the username in the socket session for this client
	    socket.username = username;
	    // store the room name in the socket session for this client
	    socket.room = room;
            //socket.id = id;
	    // add the client's username to the global list
	    if(!usernames[room])
		usernames[room] = {};
	    usernames[room][username] = id;

	    // send client to room 1
	    socket.join(room);
	    // echo to client they've connected
	    
	    if(!user_sockets[room])
		user_sockets[room] = {}
	    user_sockets[room][username] = socket;
	    
            
	        	
	    loadHistory(socket, false);
	    io.sockets.in(socket.room).emit('updateusers', usernames[socket.room]);
	    //socket.emit('updaterooms', [room,], room);
	});

	// when the client emits 'sendchat', this listens and executes
	socket.on('sendchat', function (data) 
	{
		// we tell the client to execute 'updatechat' with 2 parameters
		logMessage(socket, data, "text");
		io.sockets.in(socket.room).emit('updatechat', socket.username, data);
	});
	

	// when the client emits 'sendchat', this listens and executes
	socket.on('sendpm', function (data, to_user) 
	{
		// we tell the client to execute 'updatechat' with 2 parameters
		logMessage(socket, data, "private");
		if(socket.room in user_sockets && to_user in user_sockets[socket.room])
    		user_sockets[socket.room][to_user].emit('update_private_chat', socket.username, data);
	});
	
	
    // when the client emits 'sendchat', this listens and executes
	socket.on('sendhtml', function (data) 
	{
		// we tell the client to execute 'updatechat' with 2 parameters
		//console.log("sending html "+data+"on behalf of "+socket.username)
		
		logMessage(socket, data, "html");
		io.sockets.in(socket.room).emit('updatehtml', socket.username, data);
	});
	
	// when the client emits 'sendchat', this listens and executes
	socket.on('sendimage', function (data) {
		
		logMessage(socket, data, "image");
		io.sockets.in(socket.room).emit('updateimage', socket.username, data);
	});

 
	// when the client emits 'sendchat', this listens and executes
	socket.on('ready', function (data) 
	{
		logMessage(socket, data, "ready");
		io.sockets.in(socket.room).emit('updateready', socket.username, data);
	});
	
        // when the client emits 'sendchat', this listens and executes
	socket.on('global_ready', function (data) 
	{
		logMessage(socket, "global "+data, "ready");
		io.sockets.in(socket.room).emit('update_global_ready', data);
	});

	socket.on('switchRoom', function(newroom)
	{
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
	socket.on('disconnect', function()
	{
            logger.log("info",socket.username + " disconnects");
            if(socket.username != "VirtualErland" && socket.room in numUsers)
            {
            	numUsers[socket.room] = numUsers[socket.room] - 1; 
            }
	    if(socket.room in usernames && socket.username in usernames[socket.room])
	    {
            // remove the username from global usernames list
            var id = usernames[socket.room][socket.username];
            delete usernames[socket.room][socket.username];
            if(usernames[socket.room])
            {
                // update list of users in chat, client-side
                io.sockets.in(socket.room).emit('updateusers', usernames[socket.room]);
                // echo globally that this client has left
                
                io.sockets.in(socket.room).emit('updatepresence', socket.username, 'leave', id);
                logMessage(socket, "leave", "presence");
            }
	    }
	    
		if(socket.room in user_sockets && socket.username in user_sockets[socket.room])
	    {
	       delete user_sockets[socket.room][socket.username];
	    }
	    
	    if(socket.room)
		  socket.leave(socket.room);
	    
	});
});
