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

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import basilica2.agents.components.ChatClient;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.ImageEvent;
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
import basilica2.agents.events.StartExternalTimerEvent;
import basilica2.agents.events.PoseEvent.poseEventType;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.MultiModalFilter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

//TODO: MoodleChatClient.properties

public class WebsocketChatClient extends Component implements ChatClient
{	

	String socketURL = "http://localhost:8000";
	String socketSubURL = null;
	Agent agent; 
	String agentUserName = "ROBOT";
	String agentRoomName = "ROOM";
//	private String multiModalDelim = ";%;";
//	private String withinModeDelim = ":::";	
	private String sendFilePrefix = "sendfile-";
	private static final String CAMERA_FRAME_TAG = "cameraframe";  


	boolean connected = false;

	Socket socket;

	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#disconnect()
	 */
	@Override
	public void disconnect()
	{
		log(Logger.LOG_NORMAL, "WebsocketChatClient, disconnect");
		socket.disconnect();
	}

	//TODO: poll for incoming messages
	//TODO: report presence events
	//TODO: report extant students at login


	public WebsocketChatClient(Agent a, String n, String pf)
	{
		super(a, n, pf);
		agent = a;

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
	        System.err.println("WebsocketChatClient, processEvent, SendCommandEvent - command: " + sce.getCommand());
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
		else if(e instanceof StartExternalTimerEvent)
		{
			StartExternalTimerEvent sete = (StartExternalTimerEvent) e;
	        System.err.println("WebsocketChatClient, processEvent, StartExternalTimerEvent - time: " + sete.getTime());
	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent, StartExternalTimerEvent - time: " + sete.getTime());
			try
			{
				insertStartExternalTimerEvent(sete.getTime());
			}
			catch (Exception e1)
			{
				System.err.println("WebsocketChatClient, processEvent - couldn't send StartExternalTimerEvent: " + sete);
		        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, processEvent - couldn't send StartExternalTimerEvent: " +sete);
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
//		socket.emit("sendchat", message);
		socket.emit("sendchatwithroom", agentRoomName, message);
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
        socket.emit("sendcommandeventwithroom", agentRoomName, command);
//		socket.emit("sendcommandevent", command);
	}

	protected void insertStartExternalTimerEvent(String time)
	{
        System.err.println("WebsocketChatClient, insertStartExternalTimerEvent - time: " + time);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, insertStartExternalTimerEvent - time: " + time);
		socket.emit("starttimer", time);
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
	

    public static boolean almostIdentical(String base64Jpeg1, String base64Jpeg2, double threshold) throws IOException {
//    	if (base64Jpeg1 == "") {
//    		System.err.println("New image is null");
//    	}
//    	if (base64Jpeg2 == "") {
//    		System.err.println("Previous image is null");
//    	}
    	if (base64Jpeg1 == "" || base64Jpeg2 == "") {
    		return false; 
    	}
        long hash1 = averageHash(decode(base64Jpeg1));
        long hash2 = averageHash(decode(base64Jpeg2));
        int hammingDistance = Long.bitCount(hash1 ^ hash2);
        double dissimilarity = hammingDistance / 64.0;
        System.err.println("WebsocketChatClient, dissiimilarity: " + String.valueOf(dissimilarity));
        return dissimilarity <= threshold; // e.g. threshold = 0.1
    }

    private static BufferedImage decode(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static long averageHash(BufferedImage src) {
        Image scaled = src.getScaledInstance(8, 8, Image.SCALE_SMOOTH);
        BufferedImage small = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        small.getGraphics().drawImage(scaled, 0, 0, null);

        int[] lum = new int[64];
        long sum = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int rgb = small.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                int val = (r + g + b) / 3;
                lum[y * 8 + x] = val;
                sum += val;
            }
        }
        int avg = (int) (sum / 64);

        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            if (lum[i] >= avg) hash |= (1L << i);
        }
        return hash;
    }
	

	public void setCallbacks() {
		socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {

			@Override
			public void call(Object... args)
			{
				System.err.println("an Error occurred...");

				log(Logger.LOG_NORMAL, "WebsocketChatClient, setCallbacks - enter");
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
					log(Logger.LOG_NORMAL, "WebsocketChatClient, EVENT_DISCONNECT");
				}
				
			}).on(Socket.EVENT_CONNECT, new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					System.err.println("WebsocketChatClient: Connection established");
					log(Logger.LOG_NORMAL, "WebsocketChatClient, EVENT_CONNECT");
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
					String senderUsername = (String)args[0];
					String message = (String)args[1];
					message = StringEscapeUtils.unescapeHtml4(message);
//					if (message.contains(CAMERA_FRAME_TAG + MultiModalFilter.withinModeDelim)) {
					if (message.contains(CAMERA_FRAME_TAG)) {
						System.out.println("WebsocketChatClient, updatechat received camera pic in message");
					}
					else {
						System.out.println("WebsocketChatClient, updatechat received message: " + message);
				        log(Logger.LOG_NORMAL,"WebsocketChatClient, updatechat received message: " + message);
					}	

			        // -------------------------------------------------------
			        // Detect camera frame messages before other checks.
			        // A camera frame message is multimodal and contains the
			        // cameraframe::: tag.  We parse all fields and broadcast
			        // an ImageEvent so vision/OCR listeners can handle it.
			        // -------------------------------------------------------
				    if (message.contains(CAMERA_FRAME_TAG)) {

			        	System.err.println("\n*** WebsocketChatClient, updatechat: cameraframe received in multimodal message ***\n");
			        	String[] segments = message.split(
			        		java.util.regex.Pattern.quote(MultiModalFilter.multiModalDelim));

			        	String imageBase64 = "";
			        	String mimeType    = "image/jpeg";
			        	String problemId   = "";
			        	String fromUser    = senderUsername;
			        	int    width       = 0;
			        	int    height      = 0;
			        	int    frameCount  = 0;

			        	for (String segment : segments) {
			        		// Split on ::: but limit to 2 parts so base64 content
			        		// (which may contain colons) is never split further.
			        		String[] kv = segment.split(
			        			java.util.regex.Pattern.quote(MultiModalFilter.withinModeDelim), 2);
			        		if (kv.length < 2) continue;
			        		String key   = kv[0].trim();
			        		String value = kv[1];          // preserve base64 exactly

			        		switch (key) {
			        			case "from":        fromUser    = value;                   break;
			        			case "mimeType":    mimeType    = value;                   break;
			        			case "problemId":   problemId   = value;                   break;
			        			case "cameraframe": imageBase64 = value;                   break;
			        			case "width":
			        				try { width  = Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) {}
			        				break;
			        			case "height":
			        				try { height = Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) {}
			        				break;
			        			case "frameCount":
			        				try { frameCount = Integer.parseInt(value.trim()); } catch (NumberFormatException ignored) {}
			        				break;
			        			default: break;
			        		}
			        	}
			        				    		
			    		State s = State.copy(StateMemory.getSharedState(agent));
			    		String previousImage = s.getCurrentImage(); 
			    		s.setCurrentImage(imageBase64);
			    		StateMemory.commitSharedState(s, agent);
			    		try {
							boolean similar = almostIdentical(imageBase64,previousImage,0.1);
							if (similar) {
								System.err.println("*** WebsocketChatClient, updatechat: Image received is similar to previous image; not sending ***");
							}
							else {
								System.err.println("*** WebsocketChatClient, updatechat: Updated image received");
					        	System.err.println("*** WebsocketChatClient, updatechat: ImageEvent frame=" + frameCount
					        		+ " size=" + width + "x" + height + " from=" + fromUser);
					        	log(Logger.LOG_NORMAL, "WebsocketChatClient, updatechat: ImageEvent frame=" + frameCount
					        		+ " size=" + width + "x" + height + " from=" + fromUser);
					        	ImageEvent ie = new ImageEvent(WebsocketChatClient.this,
					        		fromUser, imageBase64, mimeType, width, height, problemId, frameCount);
					        	WebsocketChatClient.this.broadcast(ie);
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
			    			

			        // -------------------------------------------------------
			        // Existing path: sendfile prefix check, then normal chat.
			        // -------------------------------------------------------
			        } else if (message.startsWith(sendFilePrefix)) {
			        	String filename = message.replace(sendFilePrefix,"");
						System.out.println("WebsocketChatClient, updatechat with sendfile received: " + filename); 
						log(Logger.LOG_NORMAL, "WebsocketChatClient, updatechat with sendfile received - filename = " + filename);					
						FileEvent.fileEventType eventType = FileEvent.fileEventType.valueOf("created"); 
						FileEvent fe = new FileEvent(WebsocketChatClient.this,filename,eventType);
						WebsocketChatClient.this.broadcast(fe);

			        } else {			        
						MessageEvent me = new MessageEvent(WebsocketChatClient.this, (String)args[0], message);
						WebsocketChatClient.this.broadcast(me);
			        }
				}
				
			// I think Bazaar does not receive 'sendpm' from NodeJS. Instead it receives 'update_private_chat'.
			}).on("sendpm", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					String user = (String)args[0];
					String message = (String)args[1];
					user = StringEscapeUtils.unescapeHtml4(user);
					message = StringEscapeUtils.unescapeHtml4(message);
					System.err.println("WebsocketChatClient, sendpm received from user " + user + ": " + message);
			        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, sendpm received from user " + user + ": " + message);
//					MessageEvent me = new MessageEvent(WebsocketChatClient.this, user, message);
					String test_message = StringEscapeUtils.unescapeHtml4("Shhhh. Bazaar received a private message.");
			        MessageEvent me = new MessageEvent(WebsocketChatClient.this, user, test_message);
					WebsocketChatClient.this.broadcast(me);
				}	        
			}).on("update_private_chat", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					String user = (String)args[0];
					String message = (String)args[1];
					user = StringEscapeUtils.unescapeHtml4(user);
					message = StringEscapeUtils.unescapeHtml4(message);
					System.err.println("WebsocketChatClient, update_private_chat received from user " + user + ": " + message);
			        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"WebsocketChatClient, update_private_chat received from user " + user + ": " + message);
					String test_message = "Shhhh. Bazaar received a private message from " + user + "."; 
//					String test_message = StringEscapeUtils.unescapeHtml4("Shhhh. Bazaar received a private message."); 
//					MessageEvent me = new MessageEvent(WebsocketChatClient.this, user, message);
//			        MessageEvent me = new MessageEvent(WebsocketChatClient.this, user, test_message);
//					WebsocketChatClient.this.broadcast(me);
					insertMessage(test_message); 
					
				}	        
			}).on("sendfile", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					String filename = (String)args[1];
					System.out.println("WebsocketChatClient, sendfile received: " + filename); 
					log(Logger.LOG_NORMAL, "WebsocketChatClient, sendfile received - filename = " + filename);
					
					
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
