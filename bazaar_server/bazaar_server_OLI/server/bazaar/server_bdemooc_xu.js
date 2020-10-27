require('dotenv').config()
const winston = require('winston');
const mustache = require('mustache-express');
const fs = require('fs');
const util = require('util')

const path = require('path')
const mysql = require('mysql');
const { check, validationResult } = require('express-validator');
const { default: ShortUniqueId } = require('short-unique-id');
const uid = new ShortUniqueId();
const mysql_auth = {
  host: process.env.MYSQL_HOST,
  user: process.env.MYSQL_ROOT_USER,
  password: process.env.MYSQL_ROOT_PASSWORD,
  port: 3306
};
let connection;
const bodyParser = require('body-parser');
const localPort = 80;
const localURL = "/bazaar";

function sleep(milliseconds) {
  console.log("sleep start");
  const start = new Date().getTime();
  while (1) {
    if ((new Date().getTime() - start) > milliseconds) {
      break;
    }
  }
  console.log("sleep over");
}

function handleDisconnect() {
  connection = mysql.createConnection(mysql_auth); // Recreate the connection, since
  // the old one cannot be reused.

  connection.connect(function (err) {              // The server is either down
    if (err) {                                     // or restarting (takes a while sometimes).
      console.log('error when connecting to db:', err);
      setTimeout(handleDisconnect, 2000); // We introduce a delay before attempting to reconnect,
    }                                     // to avoid a hot loop, and to allow our node script to
  });                                     // process asynchronous requests in the meantime.
                                          // If you're also serving http, display a 503 error.
  connection.on('error', function (err) {
    console.log('db error', err);
    if (err.code === 'PROTOCOL_CONNECTION_LOST') { // Connection to the MySQL server is usually
      handleDisconnect();                         // lost due to either server restart, or a
    } else {
      console.log('unknown db connection error', err)                      // connnection idle timeout (the wait_timeout
      //throw err;                                  // server variable configures this)
      handleDisconnect();
    }
  });
}

function pad(num, digits) {
  let str = '' + num;
  while (str.length < digits) {
    str = '0' + str;
  }

  return str;
}

handleDisconnect();

const numUsers = {};
let teamNumber = 0;

const csv = require('csv');
const exec = require('child_process').exec;

let logger;

const routes = require('express').Router()

function puts(error, stdout, stderr) {
  console.log(stdout)
}

routes.get('/bazaar/room_status_all', async (req, res) => {
  console.log("routes.get('/bazaar/room_status_all')");
  const db = connectDb();
  const query = 'SELECT name from nodechat.room where name like "normaldist%"';
  console.log(query);
  try {
    const rows = await db.query(query);
    let num_list = "";
    for (let i = 0; i < rows.length; i++) num_list += "<p>" + rows[i].name + "</p>";
    res.send("<body>" + num_list + "</body>");
  }catch (err){
    console.log(err);
    res.send(500, "<body><h2>Error</h2><p>Couldn't fetch data</p></body>");
  } finally {
    await db.close();
  }
});

routes.get('/bazaar/room_status*', async (req, res) => {
  console.log("routes.get('/bazaar/room_status*')");
  const db = connectDb();
  const query = 'SELECT name from nodechat.room where name=' + mysql.escape("normaldist" + req.query.roomId);
  console.log(query);
  try {
    const rows = db.query(query);
    if (rows.length === 0) {
      res.send("Has not been used");
    } else {
      res.send("Has already been used");
    }
  }catch (err){
    console.log(err);
    res.send(500, "<body><h2>Error</h2><p>Couldn't fetch data</p></body>");
  }finally {
    await db.close();
  }
});

routes.get('/bazaar/welcome*', async (req, res) => {
  console.log("Welcome");
  res.sendFile(path.join(__dirname, "./welcome.html"));
});


routes.get('/bazaar/login*', async (req, res) => {
  console.log("Hi");
  const db = connectDb();
  teamNumber = 0;
  logger = winston.createLogger({
    transports: [
      new (winston.transports.Console)()]
  });

  const query = 'INSERT INTO nodechat.consent (roomname, userid, consent) VALUES (' + db.escape(req.query.roomName + req.query.roomId) + ',' + db.escape(req.query.mturkid) + ',' + (req.query.consent !== "undefined" && req.query.consent == "agree" ? 1 : 0) + ')';
  console.log(query);
  try {
    await db.query(query);
  }catch (err){
    console.log(err);
  } finally {
    await db.close();
  }

  if (1) {
    teamNumber = req.query.roomId;
    setTeam_(teamNumber, req, logger, res);
  }
});

routes.post('/bazaar/login*', async (req, res) => {
  console.log("Hi");
  teamNumber = 0;
  logger = winston.createLogger({
    transports: [
      new (winston.transports.Console)()]
  });

  console.log("SELECT count from nodechat.room_prefix" + ' where name=' + req.query.roomName);
  const db = connectDb();
  try {
    let [rows, err] =  handle(db.query("SELECT count from nodechat.room_prefix"
      + ' where name=' + db.escape(req.query.roomName)));
    if(err)throw new Error("Couldn't fetch data for room " +req.query.roomName);
    if (rows.length === 0) {
      console.log('insert into nodechat.room_prefix set name=' + req.query.roomName + ', created=NOW(), comment="auto-created", count=1;');
      [rows, err] = handle(db.query('insert into nodechat.room_prefix set name=' + db.escape(req.query.roomName) + ', created=NOW(), comment="auto-created", count=1;'));
      if(err)throw new Error("Couldn't create room " +req.query.roomName);
    }else {
      teamNumber = rows[0].count;
      console.log('count : ' + teamNumber);
      console.log('users : ' + numUsers);
      if ((!(req.query.roomName + pad(teamNumber, 2) in numUsers)) || numUsers[req.query.roomName + pad(teamNumber, 2)] <= 0) {
        teamNumber = teamNumber + 1;
        console.log('increased count : ' + teamNumber);
        console.log('update nodechat.room_prefix set count=' + teamNumber + ' where name=' + req.query.roomName);
        [rows, err] = handle(db.query('insert into nodechat.room_prefix set name=' + db.escape(req.query.roomName) + ', created=NOW(), comment="auto-created", count=1;'));
        if(err)throw new Error("Couldn't update count for room " +req.query.roomName);
        numUsers[req.query.roomName + pad(teamNumber, 2)] = 0;
        exec("../launch_agent.sh " + req.query.roomName + " " + teamNumber + ' "none"', puts);
        sleep(5000);
        setTeam(teamNumber, req, logger, res);
      }else {
        setTeam(teamNumber, req, logger, res);
      }
      console.log("successful");
    }
  }catch (err){
    console.log(err);
    res.send(500, header_stuff + "<body><h2>Error</h2><p>" +err.message + "':</p><pre>" + err + "</pre></body>");
  }finally {
    await db.close();
  }
});

routes.get('/bazaar/chat*', async (req, res) => {

  let html_page = 'index';
  if (req.query.html !== undefined) html_page = req.query.html;

  res.sendFile(path.join(__dirname, './html_pages/'+html_page + '.html'));
});

routes.get('/bazaar/discussionnew.css', async (req, res) => {
  res.sendFile(path.join(__dirname, './discussionnew.css'))
});

routes.get('/bazaar/static/*', async (req, res) => {
  const file = req.path.substring(req.path.lastIndexOf('/')+1);
  res.sendFile(path.join(__dirname, './static/'+file))
});

routes.get('/bazaar/observe/*', async (req, res) => {
  res.sendFile(path.join(__dirname, './index.html'));
});

routes.get('/bazaar/data/*', async (req, res) => {
  groups = /\/data\/([^\/]+)/.exec(req.url)
  room = groups[1];
  exportCSV(room, res);
});

routes.get('/bazaar/config/edit*', async (req, res) => {
  res.render('config_selection', {action: '/bazaar/config/select?ltik=' + req.query.ltik, data: selectOptions()});
});

routes.post('/bazaar/config/select*',[
  check('agent', 'agent is required').exists(),
  check('html', 'index html file is required').exists()
], async (req, res) => {
  const errors = validationResult(req);
  if(!errors.isEmpty()){
    // :TODO: redirect to error page
    return res.status(422).jsonp(errors.array())
  }
  await check('agent', ).escape().trim().run(req);
  await check('html', ).escape().trim().run(req);
  const db = connectDb();
  const token = res.locals.token;
  try {
    const rows = await db.query("SELECT id from nodechat.lti_config"
      + ' where platformId=' + db.escape(token.platformId) + ' and contextId='
      + db.escape(token.platformContext.context.id) + ' and resourceId='
      + db.escape(token.platformContext.resource.id));
    const config = {
      agent: req.body.agent,
      html: req.body.html
    }
    if (rows.length === 0) {
      const query = 'INSERT INTO nodechat.lti_config (platformId, contextId, resourceId, config) VALUES ('
        + db.escape(token.platformId) + ',' + db.escape(token.platformContext.context.id) + ','
        + db.escape(token.platformContext.resource.id) + ',' + db.escape(JSON.stringify(config)) + ')';
      console.log(query);
      await db.query(query);
    }else{
      const query = 'update nodechat.lti_config set config=' + db.escape(JSON.stringify(config))
        + ' where platformId=' + db.escape(token.platformId) + ' and contextId='
        + db.escape(token.platformContext.context.id) + ' and resourceId='
        + db.escape(token.platformContext.resource.id);
      await db.query(query);
    }
  }catch (err){
    console.log(err);
  } finally {
    await db.close();
  }
  let group = token.platformContext.custom.group;
  if(!group || group.length ===0){
    group = uid();
  }
  group = group.slice(0, 10);
  let name = token.userInfo.email;
  if(!name){
    name = token.user;
  }else{
    name = name.split("@")[0];
  }
  lti.redirect(res, '/bazaar/login?roomName='+req.body.agent+'&roomId='+group+'&mturkid='+token.user+'&username='+name+'&perspective=1&html='+req.body.html);
});

let lti = require('ltijs').Provider

lti = lti.setup(process.env.LTI_KEY,
  {
    url: 'mongodb://' + process.env.MONGO_HOST + '/' + process.env.MONGO_INITDB_DATABASE + '?authSource=admin',
    connection: { user: process.env.MONGO_INITDB_ROOT_USERNAME, pass: process.env.MONGO_INITDB_ROOT_PASSWORD }
  }, {
    appRoute: '/bazaar', loginRoute: '/bazaar/ltilogin', keysetRoute: '/bazaar/keys',
    sessionTimeoutRoute: '/bazaar/sessiontimeout', invalidTokenRoute: '/bazaar/invalidtoken',
    cookies: {
      secure: true, // Set secure to true if the testing platform is in a different domain and https is being used
      sameSite: 'None' // Set sameSite to 'None' if the testing platform is in a different domain and https is being used
    },
    devMode: false // Set DevMode to true if the testing platform is in a different domain and https is not being used
  })

lti.whitelist(lti.appRoute(), {route: new RegExp(/^\/bazaar\/welcome$/), method: 'get'}, { route: new RegExp(/^\/bazaar\/static\//), method: 'get' }) // Example Regex usage

// When receiving successful LTI launch redirects to app, otherwise redirects to landing page
lti.onConnect(async (token, req, res) => {
  // console.log("On connect token=" + JSON.stringify(token));
  // console.log('On connect context_token='+JSON.stringify(res.locals.context));
 if (token)  {
    const token = res.locals.token;
    const db = connectDb();
    try {
      // Cache LMS course roster in local db
      // let namesAndRoles = await createOrUpdateRoster(token.platformId, token.platformContext.context.id, null, false);
      // let user = namesAndRoles === null? null: namesAndRoles.find(e => e.user_id === token.user);
      // if(!namesAndRoles || !user) {
      //   try {
      //     namesAndRoles = await lti.NamesAndRoles.getMembers(res.locals.token);
      //     namesAndRoles = namesAndRoles.members;
      //     await createOrUpdateRoster(token.platformId, token.platformContext.context.id, namesAndRoles, true);
      //     user = namesAndRoles.find(e => e.user_id === token.user)
      //   } catch (err) {
      //     console.log(err);
      //     return res.render('error_messages', {message: 'Unable to retrieve roles from LMS'});
      //   }
      // }

      let user = null;
      try {
        let namesAndRoles = await lti.NamesAndRoles.getMembers(res.locals.token);
          namesAndRoles = namesAndRoles.members;
          user = namesAndRoles.find(e => e.user_id === token.user)
        } catch (err) {
          console.log(err);
          return res.render('error_messages', {message: 'Unable to retrieve roles from LMS'});
        }

      if(!user){
        return res.render('error_messages', {message: 'Unable to locate user ' + token.userInfo.email});
      }

      console.log("User accessed " + JSON.stringify(user));
      let isInstructor = false;
      for (let i = 0; i < user.roles.length; i++) {
        if(user.roles[i].toLowerCase().endsWith('instructor')){
          isInstructor = true;
        }
      }

      const rows = await db.query("SELECT config from nodechat.lti_config"
        + ' where platformId=' + db.escape(token.platformId) + ' and contextId='
        + db.escape(token.platformContext.context.id) + ' and resourceId='
        + db.escape(token.platformContext.resource.id));

      let group = token.platformContext.custom.group;
      console.log("group id " + group);
      if(rows.length > 0){
        if(!group || group.trim().length ===0){
          if(isInstructor) {
            group = uid();
          }else {
            return res.render('force_pick_group', {});
          }
        }
        group = group.slice(0, 10);
        console.log("config " + JSON.stringify(rows[0].config).replace(/\\/g, '').substr(1).slice(0, -1))
        const config = JSON.parse(JSON.stringify(rows[0].config).replace(/\\/g, '').substr(1).slice(0, -1));

        let name = token.userInfo.email;
        if(!name){
          name = token.user;
        }else{
          name = name.split("@")[0];
        }
        const continueAction = '/bazaar/login?roomName='+config.agent+'&roomId='+group+'&mturkid='+token.user+'&username='+name+'&perspective=1&html='+config.html;
        const editAction = '/bazaar/config/edit?ltik=' + req.query.ltik;
        if(isInstructor) {
          return res.render('view_configs', {continueAction: continueAction+'&ltik=' + req.query.ltik, editAction: editAction, agent: config.agent, html: config.html});
        }
        lti.redirect(res, continueAction);
      }else{
        if(isInstructor) {
          res.render('config_selection', {action: '/bazaar/config/select?ltik=' + req.query.ltik, data: selectOptions()});
        }else {
          res.render('error_messages', {message: 'This activity is not yet fully configured. Please contact your instructor for help.'});
        }
      }
    }catch (err){
      console.error(err);
      res.render('error_messages', {message: err.message});
    }finally {
      await db.close();
    }

  } else {
    lti.redirect(res, '/bazaar/welcome')
  }
})

const createOrUpdateRoster = async (platformId, contextId, members, forceUpdate) => {
  const db = connectDb();
  try {
    const rows = await db.query("SELECT id, members from nodechat.lti_members_and_roles"
      + ' where platformId=' + db.escape(platformId) + ' and contextId='
      + db.escape(contextId));

    if (rows.length === 0) {
      if(!members){
        return null;
      }
      const query = 'INSERT INTO nodechat.lti_members_and_roles (platformId, contextId, members) VALUES ('
        + db.escape(platformId) + ',' + db.escape(contextId) + ','
        + db.escape(JSON.stringify(members)) + ')';
      console.log(query);
      await db.query(query);
    }else{
      if(!forceUpdate){
        return JSON.parse(JSON.stringify(rows[0].members).replace(/\\/g, '').substr(1).slice(0, -1));
      }
      const query = 'update nodechat.lti_members_and_roles set members=' + db.escape(JSON.stringify(members))
        + ' where platformId=' + db.escape(platformId) + ' and contextId='
        + db.escape(contextId);
      await db.query(query);
    }
    return members;
  }catch (err){
    console.log(err);
  } finally {
    await db.close();
  }
}

const selectOptions = () =>{
  let directoryPath = path.join(__dirname, '../../agents');
  const agentList = [];
  let files = fs.readdirSync(directoryPath);
  files.forEach(function (file) {
    const name = file.replace("agent", "").replace("Agent", "");
    agentList.push({value: name, text: name});
  });
  directoryPath = path.join(__dirname, './html_pages');
  const htmlList = [];
  files = fs.readdirSync(directoryPath);
  files.forEach(function (file) {
    const name = file.replace(".html", "");
    htmlList.push({value: name, text: name});
  });

  return {agentList: agentList, htmlList: htmlList};
}
// When receiving deep linking request redirects to deep link screen
lti.onDeepLinking(async (token, req, res) => {
  return lti.redirect(res, '/bazaar/deeplink', {newResource: true})
})

lti.app.engine('html', mustache());
lti.app.set('view engine', 'html');
lti.app.set('views', path.join(__dirname, "./views"));
// Setting up routes
lti.app.use(routes);
lti.app.use(bodyParser.urlencoded());

// Setup function
const setup = async () => {
  await lti.Database.setup();
}

const server = require('http').createServer(lti.app);
const io = require('socket.io')(server, {path: '/bazsocket'});
server.listen(localPort);
setup();

const connectDb = () => {
  const connection = mysql.createConnection(mysql_auth);
  return {
    query( sql, args ) {
      return util.promisify( connection.query ).call( connection, sql, args );
    },
    escape(val){
      return connection.escape(val);
    },
    close() {
      return util.promisify( connection.end ).call( connection );
    }
  };
}

const gradePassBack = async (req, res) =>{
    const lineItem = {
      scoreMaximum: 1,
      label: 'Grade',
      tag: 'grade'
    }

    const grade = {
      scoreGiven: 1,
      activityProgress: 'Completed',
      gradingProgress: 'FullyGraded'
    }
    return await lti.Grade.scorePublish(res.locals.token, grade, { resourceLinkId: true, autoCreate: lineItem });
}

function setTeam_(teamNumber, req, logger, res) {
  if ((!(req.query.roomName + teamNumber in numUsers))) {
    numUsers[req.query.roomName + teamNumber] = 0;
    console.log("../launch_agent.sh " + req.query.roomName + " " + teamNumber + ' "none"', puts);
    exec("../launch_agent.sh " + req.query.roomName + " " + teamNumber + ' "none"', puts);
    sleep(5000);
  }

  let html_page = 'index';
  if (req.query.html !== undefined) html_page = req.query.html;

  const roomname = req.query.roomName + teamNumber;
  const url = localURL + '/chat/' + roomname + '/' + req.query.mturkid + '/' +
    req.query.username + '/' + req.query.perspective + '/' + '?html=' + html_page + '&forum=' + req.query.forum
    + '&ltik=' + req.query.ltik;

  res.writeHead(301, {Location: url});
  res.end();

  logger.log("info", "Number of users : " + numUsers[roomname]);
  logger.log("info", "Team number : " + teamNumber);

  gradePassBack(req, res).then((response)=>{
    console.log("grade passback response " + JSON.stringify(response));
  }).catch((error)=>{
    console.log("grade passback error " + JSON.stringify(error));
  });
}

function setTeam(teamNumber, req, logger, res) {
  let html_page = 'index';
  if (req.query.html !== undefined) html_page = req.query.html;
  const roomname = req.query.roomName + pad(teamNumber, 2);
  let name = res.locals.token.userInfo.email ;
  if(!name){
    name = res.locals.token.user;
  }else{
    name = name.split("@")[0];
  }
  const url = localURL + '/chat/' + roomname + '/' + name+ '/?html=' + html_page + '&ltik=' + req.query.ltik;

  res.writeHead(301, {Location: url});
  res.end();

  logger.log("info", "Number of users : " + numUsers[roomname]);
  logger.log("info", "Team number : " + teamNumber);
  gradePassBack(req, res).then((response)=>{
    console.log("grade passback response " + JSON.stringify(response));
  }).catch((error)=>{
    console.log("grade passback error " + JSON.stringify(error));
  });
}

// sockets by username
let user_sockets = {};

// usernames which are currently connected to each chat room
let usernames = {};

// user_perspectives
let user_perspectives = {};
// rooms which are currently available in chat
let rooms = [];

function isBlank(str) {
  return !str || /^\s*$/.test(str)
}

const header_stuff = "<head>\n" +
  "\t<link href='http://fonts.googleapis.com/css?family=Oxygen' rel='stylesheet' type='text/css'>\n" +
  "\t<link href='http://ankara.lti.cs.cmu.edu/include/discussion.css' rel='stylesheet' type='text/css'>\n" +
  "</head>";

function exportCSV(room, res) {
  const connection = mysql.createConnection(mysql_auth);

  connection.query("SELECT DATE_FORMAT(m.timestamp, '%Y-%m-%d'), DATE_FORMAT(m.timestamp, '%H:%i:%s'),  m.type, m.content, m.username from nodechat.message "
    + 'as m join nodechat.room as r on m.roomid=r.id '
    + 'where r.name=' + connection.escape(room) + ' order by timestamp', function (err, rows, fields) {

    if (err) {
      console.log(err);
      res.send(500, header_stuff + "<body><h2>Export Error</h2><p>Couldn't fetch data for room '" + room + "':</p><pre>" + err + "</pre></body>");
    } else if (rows.length === 0) {
      res.send(404, header_stuff + "<body><h2>Empty Room</h2><p>Couldn't fetch data for empty room '" + room + "'.</p></body>");
    } else {
      rows.unshift(['DATE', 'TIME', 'TYPE', 'TEXT', 'AUTHOR']);

      res.set('Content-Type', 'text/csv');
      res.header("Content-Disposition", "attachment;filename=" + room + ".csv");
      csv().from(rows).to(res);
    }
  });
}

function loadHistory(socket, secret) {
  if (!socket.temporary) {
    let id = null;
    if (socket.room in usernames && socket.username in usernames[socket.room]) {
      id = usernames[socket.room][socket.username];
    }

    let perspective = null;
    if (socket.room in user_perspectives && socket.username in user_perspectives[socket.room]) {
      perspective = user_perspectives[socket.room][socket.username];
    }

    connection.query('insert ignore into nodechat.room set name=' + connection.escape(socket.room) + ', created=NOW(), modified=NOW(), comment="auto-created";', function (err, rows, fields) {
      setTimeout(function (socket) {

        const connection = mysql.createConnection(mysql_auth);

        connection.query('SELECT m.timestamp, m.type, m.content, m.username from nodechat.message '
          + 'as m join nodechat.room as r on m.roomid=r.id '
          + 'where r.name=' + connection.escape(socket.room) + ' and not(m.type like "private") order by timestamp', function (err, rows, fields) {
          if (err)
            console.log(err);

          socket.emit('dump_history', rows);

          if (!secret) {
            io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join', id, perspective);
            logMessage(socket, "join", "presence");
          }
        });

        connection.end()
      }, 100, socket);

    });
  } else if (!secret) {
    io.sockets.in(socket.room).emit('updatepresence', socket.username, 'join', id, perspective);
  }
}

function logMessage(socket, content, type) {
  if (socket.temporary) return;

  connection.query('update nodechat.room set modified=now() where room.name=' + connection.escape(socket.room) + ';', function (err, rows, fields) {
    if (err)
      console.log(err);
  });

  endpoint = "unknown"
  if (socket.handshake)
    endpoint = socket.handshake.address;
  query = 'insert into nodechat.message (roomid, username, useraddress, userid, content, type, timestamp)'
    + 'values ((select id from nodechat.room where name=' + connection.escape(socket.room) + '), '
    + '' + connection.escape(socket.username) + ', ' + connection.escape(endpoint.address + ':' + endpoint.port) + ', ' + connection.escape(socket.Id) + ', ' + connection.escape(content) + ', ' + connection.escape(type) + ', now());';


  connection.query(query, function (err, rows, fields) {
    if (err)
      console.log(err);
  });

}

const handle = (promise) => {
  return promise
    .then(data => ([data, undefined]))
    .catch(error => Promise.resolve([undefined, error]));
}

io.set('log level', 1);

io.sockets.on('connection', function (socket) {

  console.log("On websocket connect");
  // when the client emits 'adduser', this listens and executes
  socket.on('snoop', function (room, id, perspective) {

    origin = socket.handshake.address
    username = "Data Collector @ " + origin.address;
    if (isBlank(room))
      room = "Limbo"

    //don't log anything to the db if this flag is set
    socket.temporary = false;

    // store the username in the socket session for this client
    socket.username = username;
    // store the room name in the socket session for this client
    socket.room = room;
    //socket.Id = id;
    // add the client's username to the global list
    if (!usernames[room])
      usernames[room] = {};
    usernames[room][username] = id;

    if (!user_perspectives[room])
      user_perspectives[room] = {};
    user_perspectives[room][username] = perspective;

    // send client to room 1
    socket.join(room);
    // echo to client they've connected

    loadHistory(socket, true);
  });


  // when the client emits 'adduser', this listens and executes
  socket.on('adduser', function (room, username, temporary, id, perspective) {

    if (username !== "VirtualErland" || username !== "BazaarAgent") {
      if (room in numUsers) {
        numUsers[room] = numUsers[room] + 1;
      } else {
        numUsers[room] = 1;
      }
    }
    //logger.log("info",username +" connects");
    if (isBlank(username)) {
      origin = socket.handshake.address
      username = "Guest " + (origin.address + origin.port).substring(6).replace(/\./g, '');
    }

    if (isBlank(room))
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
    if (!usernames[room])
      usernames[room] = {};
    usernames[room][username] = id;

    if (!user_perspectives[room])
      user_perspectives[room] = {};
    user_perspectives[room][username] = perspective;

    // send client to room 1
    socket.join(room);
    // echo to client they've connected

    if (!user_sockets[room])
      user_sockets[room] = {};
    user_sockets[room][username] = socket;


    loadHistory(socket, false);
    io.sockets.in(socket.room).emit('updateusers', usernames[socket.room], user_perspectives[socket.room], "update");
    //socket.emit('updaterooms', [room,], room);
  });

  // when the client emits 'sendchat', this listens and executes
  socket.on('sendchat', function (data) {
    // we tell the client to execute 'updatechat' with 2 parameters
    logMessage(socket, data, "text");
    io.sockets.in(socket.room).emit('updatechat', socket.username, data);
  });


  // when the client emits 'sendchat', this listens and executes
  socket.on('sendpm', function (data, to_user) {
    // we tell the client to execute 'updatechat' with 2 parameters
    logMessage(socket, data, "private");
    if (socket.room in user_sockets && to_user in user_sockets[socket.room])
      user_sockets[socket.room][to_user].emit('update_private_chat', socket.username, data);
  });


  // when the client emits 'sendchat', this listens and executes
  socket.on('sendhtml', function (data) {
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
  socket.on('ready', function (data) {
    logMessage(socket, data, "ready");
    io.sockets.in(socket.room).emit('updateready', socket.username, data);
  });

  // when the client emits 'sendchat', this listens and executes
  socket.on('global_ready', function (data) {
    logMessage(socket, "global " + data, "ready");
    io.sockets.in(socket.room).emit('update_global_ready', data);
  });

  socket.on('switchRoom', function (newroom) {
    // leave the current room (stored in session)
    if (socket.room in usernames && socket.username in usernames[socket.room])
      delete usernames[socket.room][socket.username];
    io.sockets.in(socket.room).emit('updateusers', usernames[socket.room]);
    io.sockets.in(socket.room).emit('updatepresence', username, 'leave');

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
  socket.on('disconnect', function () {
    //logger.log("info",socket.username + " disconnects");
    if ((socket.username !== "VirtualErland" || socket.username !== "BazaarAgent") && socket.room in numUsers) {
      numUsers[socket.room] = numUsers[socket.room] - 1;
    }
    if (socket.room in usernames && socket.username in usernames[socket.room]) {
      // remove the username from global usernames list
      let id = usernames[socket.room][socket.username];
      let perspective = user_perspectives[socket.room][socket.username];
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
