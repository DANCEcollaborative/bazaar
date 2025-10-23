package basilica2.agents.components;

import java.util.Collection;

import de.fhg.ipsi.chatblocks2.model.IMessageType;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.muc.MultiUserChat;

import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;

//TODO: JabberClient.properties
//TODO: implement group chat, 
//TODO:figure out how to get clients to join automatically



public class JabberClient extends Component implements MessageListener, RosterListener, PacketListener, ChatClient
{	
	private XMPPConnection connection;
	private MultiUserChat room;

	String server = "doctorhrothgar.net";
	String agentUserName = "admin";
	String agentPassword = "password";
	
	public void login(String server, String roomName, String userName, String password) throws XMPPException
	{
		// turn on the enhanced debugger
		XMPPConnection.DEBUG_ENABLED = true;
		
		ConnectionConfiguration config = new ConnectionConfiguration(server, 5222);
		connection = new XMPPConnection(config);
	
		connection.connect();
		connection.login(userName, password);
		
		connection.getRoster().addRosterListener(this);
		connection.getRoster().setSubscriptionMode(Roster.SubscriptionMode.accept_all);
		
		room = new MultiUserChat(connection, roomName+"@conference."+server);
		//room.create(roomName);
        // Send an empty room configuration form which indicates that we want
        // an instant room
        //room.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
		
		room.join(this.getAgent().getUsername());
		room.addMessageListener(this);

		
		//chat = connection.getChatManager().createChat("mr.adamson@gmail.com", this);
		//sendMessage("ROBOT", "mr.adamson@gmail.com");
	}
	
	public void sendGroupMessage(String message) throws XMPPException
	{
		//Chat chat = connection.getChatManager().createChat(to, this);
		room.sendMessage(message);
	}
	
	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#disconnect()
	 */
	@Override
	public void disconnect()
	{
		connection.disconnect();
	}

	public void processMessage(Chat chat, Message message)
	{
		if((message.getType() == Message.Type.chat || message.getType() == Message.Type.groupchat) && message.getBody() != null)
		{
			String name = room.getOccupant(message.getFrom()).getNick();
			System.out.println(name + " says: " + message.getBody());
			MessageEvent e = new MessageEvent(this, name, message.getBody());
			this.broadcast(e);
		}
	}

	@Override
	public void entriesAdded(Collection<String> arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entriesDeleted(Collection<String> arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void entriesUpdated(Collection<String> arg0)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void presenceChanged(Presence p)
	{
		if(p.getType() == Presence.Type.available)
		{
			PresenceEvent e = new PresenceEvent(this, p.getFrom(), PresenceEvent.PRESENT);
			this.broadcast(e);
		}
		if(p.getType() == Presence.Type.unavailable)
		{
			PresenceEvent e = new PresenceEvent(this, p.getFrom(), PresenceEvent.ABSENT);
			this.broadcast(e);
		}
	}

	public JabberClient(Agent a, String n, String pf)
	{
		super(a, n, pf);

	}

	@Override
	protected void processEvent(Event e)
	{
		if(e instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent) e;
			try
			{
				sendGroupMessage(me.getText());
			}
			catch (XMPPException e1)
			{
				System.err.println("couldn't send message: "+me);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}
	
	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#login(java.lang.String)
	 */
	@Override
	public void login(String roomName)
	{
		try
		{
			log(Logger.LOG_NORMAL, "logging in as client...");
			login( server, roomName, agentUserName, agentPassword);
		}
		catch (XMPPException e)
		{
			System.err.println("couldn't log in to the chat server...");
			
			e.printStackTrace();
		}
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void processPacket(Packet packet)
	{
		if(packet instanceof Message)
			processMessage(null, (Message) packet);
		
		else
			System.out.println(packet);
	}

}
