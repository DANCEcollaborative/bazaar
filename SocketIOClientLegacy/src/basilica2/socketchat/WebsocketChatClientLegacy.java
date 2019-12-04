package basilica2.socketchat;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.net.MalformedURLException;
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

public class WebsocketChatClientLegacy extends Component implements ChatClient
{	
	
	String socketURL = "http://localhost:8000";
	String agentUserName = "ROBOT";
	String agentRoomName = "ROOM";
	
	
	boolean connected = false;
	SocketIO socket;
	
	
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


	public WebsocketChatClientLegacy(Agent a, String n, String pf)
	{
		super(a, n, pf);
		
		socketURL = myProperties.getProperty("socket_url", socketURL);
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
		try
		{
			socket = new SocketIO(socketURL);
			socket.connect(new ChatSocketCallback());
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
	
	class ChatSocketCallback implements IOCallback
	{
		@Override
		public void onMessage(JSONObject json, IOAcknowledge ack)
		{
			try
			{
				System.out.println("Server said:" + json.toString(2));
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}

		@Override
		public void onMessage(String data, IOAcknowledge ack)
		{
			System.out.println("Server said: " + data);
		}

		@Override
		public void onError(SocketIOException socketIOException)
		{
			System.out.println("an Error occurred...");
			socketIOException.printStackTrace();

			System.out.println("attempting to reconnect...");
			try
			{
				socket = new SocketIO(socketURL);
				socket.connect(new ChatSocketCallback());
				
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
			catch (MalformedURLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		@Override
		public void onDisconnect()
		{
			System.out.println("Connection terminated.");
		}

		@Override
		public void onConnect()
		{
			System.out.println("Connection established");
		}

		@Override
		public void on(String event, IOAcknowledge ack, Object... args)
		{

			if(event.equals("updateusers"))
			{
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
						
						PresenceEvent pe = new PresenceEvent(WebsocketChatClientLegacy.this, key, PresenceEvent.PRESENT, value, perspective, (String)args[2]);
						WebsocketChatClientLegacy.this.broadcast(pe);
					}
				
			}
			else if(event.equals("updatechat"))
			{
				String message = (String)args[1];
				message = StringEscapeUtils.unescapeHtml4(message);
				MessageEvent me = new MessageEvent(WebsocketChatClientLegacy.this, (String)args[0], message);
				WebsocketChatClientLegacy.this.broadcast(me);
			}
			else if(event.equals("updateimage"))
			{
				String message = (String)args[1];
				WhiteboardEvent me = new WhiteboardEvent(WebsocketChatClientLegacy.this, message, (String)args[0], message);
				WebsocketChatClientLegacy.this.broadcast(me);
			}
			else if(event.equals("updatepresence"))
			{
				String message = (String)args[1];
				System.out.println("Perspective : " + (String)args[3]);
				PresenceEvent pe = new PresenceEvent(WebsocketChatClientLegacy.this, (String)args[0], message.equals("join")?PresenceEvent.PRESENT:PresenceEvent.ABSENT, (String)args[2], (String)args[3]);
				WebsocketChatClientLegacy.this.broadcast(pe);
			}
			else if(event.equals("updateready"))
			{
				String state = (String)args[1];
				ReadyEvent re = new ReadyEvent(WebsocketChatClientLegacy.this, state.equals("ready"), (String)args[0]);
				WebsocketChatClientLegacy.this.broadcast(re);
			}
			else if(event.equals("dumphistory"))
			{
				System.out.println("Ignoring historical messages.");
			}
			
			else
			{
				System.out.println("Server triggered unhandled event '" + event + "'");
			}
			
		}
	}	


}
