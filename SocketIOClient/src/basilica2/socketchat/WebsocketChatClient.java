package basilica2.socketchat;

/*import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
 */
import io.socket.client.*;
import io.socket.emitter.Emitter;

import java.net.URI; 
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
//import java.util.logging.Logger;
import edu.cmu.cs.lti.project911.utils.log.Logger;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import basilica2.agents.components.ChatClient;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.WhiteboardEvent;
import basilica2.agents.events.FileEvent;
import basilica2.agents.events.LogEvent;
import basilica2.agents.events.LogStateEvent;
import basilica2.agents.events.EndEvent;
import basilica2.agents.events.SendCommandEvent;
import basilica2.agents.events.PoseEvent.poseEventType;
import basilica2.agents.listeners.MultiModalFilter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

//TODO: MoodleChatClient.properties

public class WebsocketChatClient extends Component implements ChatClient
{	

	String socketURL = "http://localhost:8000";
	String socketSubURL = null;
	String agentUserName = "ROBOT";
	String agentRoomName = "ROOM";
//	private String multiModalDelim = ";%;";
//	private String withinModeDelim = ":::";	


	boolean connected = false;

	Socket socket;

	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#disconnect()
	 */
	@Override
	public void disconnect()
	{
		socket.disconnect();
	}

	//TODO: poll for incoming messages
	//TODO: report presence events
	//TODO: report extant students at login


	public WebsocketChatClient(Agent a, String n, String pf)
	{
		super(a, n, pf);

		socketURL = myProperties.getProperty("socket_url", socketURL);
		socketSubURL = myProperties.getProperty("socket_suburl", socketSubURL);
		agentUserName = a.getUsername();//myProperties.getProperty("agent_username", agentUserName);

		// The following works with a different logger: 'import java.util.logging.Logger;' 
//		Logger sioLogger = java.util.logging.Logger.getLogger("io.socket");
//		sioLogger.setLevel(Level.SEVERE);

	}

	@Override
	protected void processEvent(Event e)
	{
		if(e instanceof ReadyEvent)
		{
			ReadyEvent re = (ReadyEvent) e;
			try //almost always unready, global
			{
				shareReady(re.isReady(), re.isGlobalReset());
			}
			catch (Exception e1)
			{
				System.err.println("couldn't share image: "+re);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if(e instanceof WhiteboardEvent)
		{
			WhiteboardEvent me = (WhiteboardEvent) e;
			try
			{
				shareImage(me.filename);
			}
			catch (Exception e1)
			{
				System.err.println("couldn't share image: "+me);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e instanceof DisplayHTMLEvent)
		{
			DisplayHTMLEvent me = (DisplayHTMLEvent) e;
			try
			{
				insertHTML(me.getText());
			}
			catch (Exception e1)
			{
				System.err.println("couldn't share html: "+me);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e instanceof PrivateMessageEvent)
		{
			PrivateMessageEvent me = (PrivateMessageEvent) e;
			try
			{
				insertPrivateMessage(me.getText(), me.getDestinationUser());
			}
			catch (Exception e1)
			{
				System.err.println("couldn't send message: "+me);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent) e;
			try
			{
				insertMessage(me.getText());
			}
			catch (Exception e1)
			{
				System.err.println("couldn't send message: "+me);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e instanceof LogEvent)
		{
			LogEvent le = (LogEvent) e;
//	        System.err.println("WebsocketChatClient, processEvent, LogEvent - tag: " + le.getLogTag() + "  details: " + le.getLogDetails());
//	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent, LogEvent - tag: " + le.getLogTag() + "  details: " + le.getLogDetails());
			try
			{
				insertLogEvent(le.getLogTag(),le.getLogDetails());
			}
			catch (Exception e1)
			{
				System.err.println("WebsocketChatClient, processEvent - couldn't send LogEvent: " + le);
		        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent - couldn't send LogEvent: " +le);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e instanceof LogStateEvent)
		{
			LogStateEvent lse = (LogStateEvent) e;
			String stateTag = lse.getLogStateTag();
			String stateValue = lse.getLogStateValue(); 
			String sendLog = lse.getLogStateSendLog(); 
			String logTag = lse.getLogEventTag(); 
//	        System.err.println("WebsocketChatClient, processEvent - LogStateEvent: stateTag=" + stateTag + "  stateValue = " + stateValue + "  sendLog = " + sendLog+ "  logTag = " + logTag);
//	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent - LogStateEvent: stateTag=" + stateTag + "  stateValue = " + stateValue + "  sendLog = " + sendLog+ "  logTag = " + logTag);
			try
			{
				insertLogState(stateTag,stateValue,sendLog,logTag);
			}
			catch (Exception e1)
			{
				System.err.println("WebsocketChatClient, processEvent - couldn't send LogStateEvent: "+ lse);
		        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent - couldn't send LogStateEvent: " + lse);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e instanceof EndEvent)
		{
			EndEvent ee = (EndEvent) e;
//	        System.err.println("WebsocketChatClient, processEvent, EndEvent - tag: " + ee.getEndData() + "  details: " + ee.getLogDetails());
//	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent, EndEvent - tag: " + ee.getEndData() + "  details: " + ee.getLogDetails());
			try
			{
				insertEndEvent(ee.getEndData());
			}
			catch (Exception e1)
			{
				System.err.println("WebsocketChatClient, processEvent - couldn't send EndEvent: " + ee);
		        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent - couldn't send EndEvent: " +ee);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if(e instanceof SendCommandEvent)
		{
			SendCommandEvent sce = (SendCommandEvent) e;
	        System.err.println("WebsocketChatClient, processEvent, SendCommandEvent - commannd: " + sce.getCommand());
	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent, SendCommandEvent - commannd: " + sce.getCommand());
			try
			{
				insertSendCommandEvent(sce.getCommand());
			}
			catch (Exception e1)
			{
				System.err.println("WebsocketChatClient, processEvent - couldn't send SendCommandEvent: " + sce);
		        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent - couldn't send SendCommandEvent: " +sce);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		//TODO: private messages? "beeps"?

	}

	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#login(java.lang.String)
	 */
	@Override
	public void login(String roomName)
	{
		agentRoomName = roomName;
//		System.out.println("logging in to "+roomName+" at "+socketURL);
		if (socketSubURL != null) {
//			System.out.println("    Using specialized socket.io address " + socketSubURL);			
		}
		try
		{
			if (socketSubURL != null) {
//				IO.Options o = new IO.Options();
//				o.path = socketSubURL;
//				socket = IO.socket(socketURL, o);
				IO.Options options = new IO.Options().builder()
					// ...
					.build(); 
				options.path = socketSubURL;
				URI uri = URI.create(socketURL); 
				socket = IO.socket(uri, options);
			} else {
				socket = IO.socket(socketURL);
			}
			setCallbacks();
			socket.connect();
			socket.emit("adduser", agentRoomName, agentUserName, new Boolean(false));
		}
		catch (Exception e)
		{
			System.err.println("Couldn't login to the chat server...");
			e.printStackTrace();

			JOptionPane.showMessageDialog(null, "Couldn't access chat server: "+ e.getMessage(), "Login Failure", JOptionPane.ERROR_MESSAGE);

			connected = false;
		}
	}

	@Override
	public String getType()
	{
		return "ChatClient";
	}


	protected void insertHTML(String message)
	{
		socket.emit("sendhtml", message);
	}

	protected void insertMessage(String message)
	{
		socket.emit("sendchat", message);
	}

	protected void insertPrivateMessage(String message, String toUser)
	{
		socket.emit("sendpm", message, toUser);
	}

	protected void insertLogEvent(String logTag, String logDetails)
	{
//        System.err.println("WebsocketChatClient, insertLogEvent - logTag: " + logTag + "   details: " + logDetails);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, insertLogEvent - logTag: " + logTag + "   details: " + logDetails);
		socket.emit("logevent", logTag, logDetails);
	}

	protected void insertLogState(String stateTag, String stateValue, String sendLog, String logTag)
	{
//        System.err.println("WebsocketChatClient, insertLogState - stateTag=" + stateTag + "  stateValue = " + stateValue + "  sendLog = " + sendLog + "  logTag = " + logTag);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, insertLogState - stateTag=" + stateTag + "  stateValue = " + stateValue + "  sendLog = " + sendLog + "  logTag = " + logTag);
		socket.emit("logstate", stateTag, stateValue, sendLog, logTag);
	}

	protected void insertEndEvent(String endData)
	{
//        System.err.println("WebsocketChatClient, insertEndEvent - endData: " + endData);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, insertEndEvent - endData: " + endData);
		socket.emit("endevent", endData);
	}

	protected void insertSendCommandEvent(String command)
	{
        System.err.println("WebsocketChatClient, insertSendCommandEvent - command: " + command);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, insertSendCommandEvent - command: " + command);
		socket.emit("sendcommandevent", command);
	}

	protected void shareImage(String imageURL)
	{
		socket.emit("sendimage", imageURL);
	}

	protected void shareReady(boolean ready, boolean global)
	{
		if(global)
			socket.emit("global_ready", ready?"ready":"unready");
		else
			socket.emit("ready", ready?"ready":"unready");
	}

	public void setCallbacks() {
		socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {

			@Override
			public void call(Object... args)
			{
				System.err.println("an Error occurred...");
				//socketIOException.printStackTrace();

				System.err.println("attempting to reconnect...");
				try
				{
					if (socketSubURL != null) {
//						IO.Options o = new IO.Options();
//						o.path = socketSubURL;
//						socket = IO.socket(socketURL, o);
						IO.Options options = new IO.Options().builder()
							// ...
							.build(); 
						options.path = socketSubURL;
						URI uri = URI.create(socketURL); 
						socket = IO.socket(uri, options);
					} else {
						socket = IO.socket(socketURL);
					}
					setCallbacks();
					socket.connect();

					new Timer().schedule(new TimerTask()
					{
						public void run()
						{
//							System.out.println("Logging back in to chat room.");
							socket.emit("adduser", agentRoomName, agentUserName, new Boolean(false));
							//socket.emit("sendchat" ,"...and I'm back!");
						}
					}, 1000L);

				}
				catch (URISyntaxException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}}).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {

				@Override
				public void call(Object... args)
				{
					System.err.println("Connection terminated.");
				}
			}).on(Socket.EVENT_CONNECT, new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					System.err.println("Connection established");
				}
			}).on("updateusers", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					// System.out.println("WebsocketChatClient, enter .on('updateusers'), call"); 
					JSONArray names_list = ((JSONObject) args[0]).names();
					JSONArray perspective_list = ((JSONObject) args[1]).names();
					//System.out.println("Users: "+((JSONObject) args[0]).names() + " " + Integer.toString(names_list.length()));
//					System.out.println("Users: "+((JSONObject) args[0]).names());
					//System.out.println("Users: "+ names_list.length());
					JSONObject jObject = (JSONObject) args[0];
					JSONObject jObject_ = (JSONObject) args[1];
					Iterator<String> keys = jObject.keys();
					while (keys.hasNext())
					{
						String key = (String)keys.next();
						String value = null;
						String perspective = null;
						try {
							value = jObject.getString(key);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						try {
							perspective = jObject_.getString(key);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						PresenceEvent pe = new PresenceEvent(WebsocketChatClient.this, key, PresenceEvent.PRESENT, value, perspective, (String)args[2]);
						WebsocketChatClient.this.broadcast(pe);
					}
//					System.err.println("WebsocketChatClient, exit .on('updateusers'), call"); 

				}
			}).on("updatechat", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					String message = (String)args[1];
					message = StringEscapeUtils.unescapeHtml4(message);
					MessageEvent me = new MessageEvent(WebsocketChatClient.this, (String)args[0], message);
					WebsocketChatClient.this.broadcast(me);
				}
			}).on("sendfile", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					String filename = (String)args[1];
//					System.err.println("WebsocketChatClient, sendfile received: " + filename); 
//					log(Logger.LOG_NORMAL, "WebsocketChatClient, sendfile received - filename = " + filename);
					FileEvent.fileEventType eventType = FileEvent.fileEventType.valueOf("created"); 
					FileEvent fe = new FileEvent(WebsocketChatClient.this,filename,eventType);
					WebsocketChatClient.this.broadcast(fe);
				}
			}).on("updateimage", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					String message = (String)args[1];
					WhiteboardEvent me = new WhiteboardEvent(WebsocketChatClient.this, message, (String)args[0], message);
					WebsocketChatClient.this.broadcast(me);
				}
			}).on("updatepresence", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
//					System.err.println("WebsocketChatClient updatepresence - enter - message: " + (String)args[1]); 
//					Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient updatepresence - enter - message: " + (String)args[1]);
					String user = (String)args[0]; 
					String message = (String)args[1]; 
					String presence = "join";  
					String userID = "0"; 
					String perspective = "0"; 
					String[] multiModalMessage = message.split(MultiModalFilter.multiModalDelim);
					
					// If this is NOT a multimodal message
					if (multiModalMessage.length <= 1) {
//						System.err.println("WebsocketChatClient updatepresence - NOT a multimodal message"); 
//						Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient updatepresence - NOT a multimodal message");
						presence = message; 
						userID = (String)args[2]; 
						perspective = (String)args[3]; 
	
					} else {
//						System.err.println("WebsocketChatClient updatepresence - multimodal message"); 
//						Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient updatepresence - multimodal message");
						presence = message; 
						perspective = "0";						// hard-coded since perspective won't be supplied
						MultiModalFilter.multiModalTag tag; 
						String [] messagePart; 
						for (int i = 0; i < multiModalMessage.length; i++) {
//							System.out.println("=====" + " Multimodal message entry -- " + multiModalMessage[i] + "======");
							messagePart = multiModalMessage[i].split(MultiModalFilter.withinModeDelim,2);
							
							tag = MultiModalFilter.multiModalTag.valueOf(messagePart[0]);
							
							switch (tag) {
							case multimodal:
//								System.out.println("=========== multimodal message ===========");	
								break;
							case from:  
								user = messagePart[1]; 
//								System.out.println("from: " + user);	
								break;	
							case userID:  
								userID = messagePart[1]; 
//								System.out.println("userID: " + userID);	
								break;		
							case presence:  
								presence = messagePart[1]; 
//								System.out.println("presence: " + presence);	
								break;								
							default:
//								System.out.println(">>>>>>>>> Unused multimodal tag: " + messagePart[0] + "<<<<<<<<<<");
							}
						}					
					}

//					System.err.println("WebsocketChatClient updatepresence - creating PresenceEvent - user:" + user + "  userID: " + userID + "  presence:" + presence); 
//					Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient updatepresence - creating PresenceEvent - user:" + user + "  userID: " + userID + "  presence:" + presence);

					PresenceEvent pe = new PresenceEvent(WebsocketChatClient.this, user, presence.equals("join")?PresenceEvent.PRESENT:PresenceEvent.ABSENT, userID, perspective);
					WebsocketChatClient.this.broadcast(pe);
				}
			}).on("updateready", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					String state = (String)args[1];
					ReadyEvent re = new ReadyEvent(WebsocketChatClient.this, state.equals("ready"), (String)args[0]);
					WebsocketChatClient.this.broadcast(re);
				}
			}).on("dumphistory", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					System.out.println("Ignoring historical messages.");


				}
			});	
	}

}
