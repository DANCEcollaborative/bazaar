var mysql      = require('mysql');
var mysql_auth = {
      host     : 'localhost',
      user     : 'root',
      password : '',
    };
    
var connection;

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

handleDisconnect();


var express = require('express');
var csv = require('csv');

var app = express()
  , http = require('http')
  , server = http.createServer(app)
  , io = require('socket.io').listen(server);

server.listen(8000);
// routing

io.set('log level', 1);

app.get('/chat/*', function (req, res) 
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
			io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join');
			logMessage(socket, "join", "presence");
		    }
                });
            
                //connection.end()
            }, 100, socket);

        });
    }
    else if(!secret)
    {
	io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join');
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
    query = 'insert into nodechat.message (roomid, username, useraddress, content, type, timestamp)' 
                    +'values ((select id from nodechat.room where name='+connection.escape(socket.room)+'), '
                    +''+connection.escape(socket.username)+', '+connection.escape(endpoint.address+':'+endpoint.port)+', '+connection.escape(content)+', '+connection.escape(type)+', now());';
                    
    
    connection.query(query, function(err, rows, fields) 
    {
         if (err) 
            console.log(err);
    });
    
    //connection.end()
    
}

io.sockets.on('connection', function (socket) {

    // when the client emits 'adduser', this listens and executes
	socket.on('snoop', function(room){
	
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
	    // add the client's username to the global list
	    //if(!usernames[room])
	    //  usernames[room] = {};
	    //usernames[room][username] = username;
	    // send client to room 1
	    socket.join(room);
	    // echo to client they've connected
		
	    loadHistory(socket, true);
	    //io.sockets.in(socket.room).emit('updateusers', usernames[socket.room]);
	});


	// when the client emits 'adduser', this listens and executes
	socket.on('adduser', function(room, username, temporary){
	
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
	    // add the client's username to the global list
	    if(!usernames[room])
		usernames[room] = {};
	    usernames[room][username] = username;
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
//		io.sockets.in(socket.room).emit('updatechat', socket.username, socket.username+' shared an <a href="'+data+'" target="blank">image.</a>');
		io.sockets.in(socket.room).emit('updatehtml', socket.username, data);
	});
	
	// when the client emits 'sendchat', this listens and executes
	socket.on('sendimage', function (data) {
		// we tell the client to execute 'updatechat' with 2 parameters
		//console.log("sending image "+data+"on behalf of "+socket.username)
		
		logMessage(socket, data, "image");
//		io.sockets.in(socket.room).emit('updatechat', socket.username, socket.username+' shared an <a href="'+data+'" target="blank">image.</a>');
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
	   
	    if(socket.room in usernames && socket.username in usernames[socket.room])
	    {
		    // remove the username from global usernames list
            delete usernames[socket.room][socket.username];
            if(usernames[socket.room])
            {
                // update list of users in chat, client-side
                io.sockets.in(socket.room).emit('updateusers', usernames[socket.room]);
                // echo globally that this client has left
                
                io.sockets.in(socket.room).emit('updatepresence', socket.username, 'leave');
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
