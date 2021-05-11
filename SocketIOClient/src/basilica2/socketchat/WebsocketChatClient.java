package basilica2.socketchat;

/*import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
 */
import io.socket.client.*;
import io.socket.emitter.Emitter;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

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

		Logger sioLogger = java.util.logging.Logger.getLogger("io.socket");
		sioLogger.setLevel(Level.SEVERE);

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
		//TODO: private messages? "beeps"?

	}

	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#login(java.lang.String)
	 */
	@Override
	public void login(String roomName)
	{
		agentRoomName = roomName;
		System.out.println("logging in to "+roomName+" at "+socketURL);
		if (socketSubURL != null) {
			System.out.println("    Using specialized socket.io address " + socketSubURL);			
		}
		try
		{
			if (socketSubURL != null) {
				IO.Options o = new IO.Options();
				o.path = socketSubURL;
				socket = IO.socket(socketURL, o);
			} else {
				socket = IO.socket(socketURL);
			}
			setCallbacks();
			socket.connect();
			socket.emit("adduser", agentRoomName, agentUserName, new Boolean(false));
		}
		catch (Exception e)
		{
			System.err.println("Couldn't log in to the chat server...");
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
		socket.on(Socket.EVENT_ERROR, new Emitter.Listener() {

			@Override
			public void call(Object... args)
			{
				System.out.println("an Error occurred...");
				//socketIOException.printStackTrace();

				System.out.println("attempting to reconnect...");
				try
				{
					if (socketSubURL != null) {
						IO.Options o = new IO.Options();
						o.path = socketSubURL;
						socket = IO.socket(socketURL, o);
					} else {
						socket = IO.socket(socketURL);
					}
					setCallbacks();
					socket.connect();

					new Timer().schedule(new TimerTask()
					{
						public void run()
						{
							System.out.println("Logging back in to chat room.");
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
					System.out.println("Connection terminated.");
				}
			}).on(Socket.EVENT_CONNECT, new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					System.out.println("Connection established");
				}
			}).on("updateusers", new Emitter.Listener() { 

				@Override
				public void call(Object... args)
				{
					// System.out.println("WebsocketChatClient, enter .on('updateusers'), call"); 
					JSONArray names_list = ((JSONObject) args[0]).names();
					JSONArray perspective_list = ((JSONObject) args[1]).names();
					//System.out.println("Users: "+((JSONObject) args[0]).names() + " " + Integer.toString(names_list.length()));
					System.out.println("Users: "+((JSONObject) args[0]).names());
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
					System.err.println("WebsocketChatClient, exit .on('updateusers'), call"); 

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
					String message = (String)args[1];
					System.out.println("Perspective : " + (String)args[3]);
					PresenceEvent pe = new PresenceEvent(WebsocketChatClient.this, (String)args[0], message.equals("join")?PresenceEvent.PRESENT:PresenceEvent.ABSENT, (String)args[2], (String)args[3]);
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
