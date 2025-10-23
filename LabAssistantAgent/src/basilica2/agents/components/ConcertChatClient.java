package basilica2.agents.components;

import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.TypingEvent;
import de.fhg.ipsi.chatblocks2.awareness.AwarenessEvent;
import de.fhg.ipsi.chatblocks2.awareness.IAwarenessInfo;
import de.fhg.ipsi.chatblocks2.awareness.IAwarenessListener;
import de.fhg.ipsi.chatblocks2.model.IMessageType;
import de.fhg.ipsi.chatblocks2.model.messagebased.ChatMessage;
import de.fhg.ipsi.chatblocks2.model.messagebased.PresenceMessage;
import de.fhg.ipsi.concertchat.client.IChatApplication;
import de.fhg.ipsi.concertchat.client.LoginProcessModel;
import de.fhg.ipsi.concertchat.framework.AgiloClientConnection;
import de.fhg.ipsi.concertchat.framework.ChannelListener;
import de.fhg.ipsi.concertchat.framework.ClientConnection;
import de.fhg.ipsi.concertchat.framework.IClientConnection;
import de.fhg.ipsi.concertchat.framework.ILoginObserver;
import de.fhg.ipsi.concertchat.framework.IPersistentSession;
import de.fhg.ipsi.concertchat.framework.ISessionJoinObserver;
import de.fhg.ipsi.concertchat.framework.LoginEvent;
import de.fhg.ipsi.concertchat.framework.agilo.login.ILoginConstants;
import de.fhg.ipsi.framework.ImplementationsFactory;
import de.fhg.ipsi.states.StateEvent;
import de.fhg.ipsi.states.StateListener;
import de.fhg.ipsi.user.IUser;
import de.fhg.ipsi.user.UserManager;
import de.fhg.ipsi.utils.StringUtilities;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class ConcertChatClient extends WhiteboardActor  implements ChannelListener, IAwarenessListener, ChatClient
{
	public static String GENERIC_NAME = "ConcertChatListener";
	public static String GENERIC_TYPE = "Filter";
	private String agentName = "Tutor";
	private String application_class = "de.fhg.ipsi.concertchat.applications.whiteboard.WhiteboardChat";
	private String chat_room_name = null;
	private String session_key = null;
	IChatApplication application;
	LoginProcessModel loginModel;
	// Map loginParams;
	IClientConnection myConnection;
	
	public ConcertChatClient(Agent a, String n, String pf)
	{
		this(a, a.getName(), n, pf);
	}

	public ConcertChatClient(Agent a, String agentName, String componentName, String pf)
	{

		super(a, agentName, componentName, pf);
		
		int underscore = agentName.indexOf("_");
		if (underscore > -1)
			this.agentName = agentName.substring(0, underscore);
		else
			this.agentName = agentName;
	}

	public ConcertChatClient(Agent a, String n)
	{
		this(a, a.getName(), n, n+".properties");
	}
	
	@Override
	public void dispose()
	{
		super.dispose();
		this.logout();
	}

	@Override
	protected void processEvent(Event e)
	{
	}

	@Override
	public String getType()
	{
		return GENERIC_TYPE;
	}

	public void messageReceived(de.fhg.ipsi.concertchat.framework.MessageEvent event)
	{

		log(Logger.LOG_NORMAL, event.getType()+" ("+event.getClass().getSimpleName()+"): "+event.getMessageContainer().getMessage());
		
		if (event.getType().equalsIgnoreCase("Chatroom1"))
		{
			ChatMessage msg = (ChatMessage) event.getMessageContainer().getMessage();
			if (msg.getMessageText() != null)
			{
				String text = msg.getMessageText();
				String from = msg.getAuthorName();
				MessageEvent e = new MessageEvent(this, from, text);
				e.setOriginal(msg);
				this.broadcast(e);
				

			}
		}
		else if (event.getType().equalsIgnoreCase("presenceType"))
		{
			PresenceMessage msg = (PresenceMessage) event.getMessageContainer().getMessage();

			String agent = msg.getAuthorName();
			if (msg.getType().getLogicalName().equals(IMessageType.JOIN))
			{
				PresenceEvent e = new PresenceEvent(this, agent, PresenceEvent.PRESENT);
				e.setOriginal(msg);
				this.broadcast(e);
			}
			else if (msg.getType().getLogicalName().equals(IMessageType.LEAVE))
			{
				PresenceEvent e = new PresenceEvent(this, agent, PresenceEvent.ABSENT);
				e.setOriginal(msg);
				this.broadcast(e);
			}
		}

	}
	
	public void connectToChatRoom(String r)
	{
		log(Logger.LOG_NORMAL, "connecting to "+r);
		chat_room_name = r;
		session_key = "Session-" + getName() + "-" + Logger.getTimeStamp(true);
		doConcertChatLogin();
	}

	private void doConcertChatLogin()
	{
		loginModel = new LoginProcessModel();
		myConnection = createClientConnection();
		ClientConnection.setClientConnection(myConnection);
		setupLoginModel(loginModel);
		setupLoginHandling(loginModel);
		loginModel.setState(LoginProcessModel.AUTHENTICATION);
	}

	private IClientConnection createClientConnection()
	{
		return new AgiloClientConnection()
		{

			@Override
			protected Map createModules()
			{
				Map modules = super.createModules();
				return modules;
			}
		};
	}

	private void setupLoginModel(LoginProcessModel loginModel)
	{
		loginModel.setRoomID(this.chat_room_name);
		loginModel.setFixedRoom(true);
		loginModel.setLoginHandle(this.agentName);
		loginModel.setFixedHandle(true);
		loginModel.setSessionKey(session_key);
		loginModel.setMessage("Logging In ...");
		loginModel.setPassword("");
		loginModel.setPswNeeded(false);
	}

	private void setupLoginHandling(final LoginProcessModel loginModel)
	{
		loginModel.addStateListener(new StateListener(LoginProcessModel.LOGIN_FAILED)
		{

			public void stateReached(StateEvent event)
			{
				log(Logger.LOG_ERROR, "<error>Login Failure!!</error>");
				handleLoginFailure();
			}
		});

		loginModel.addStateListener(new StateListener(LoginProcessModel.AUTHENTICATION)
		{
			public void stateReached(StateEvent event)
			{
				doLogin();
			}
		});

		loginModel.addStateListener(new StateListener(LoginProcessModel.LOGGED_IN)
		{

			ISessionJoinObserver observer = new ISessionJoinObserver()
			{

				public void sessionJoined(IPersistentSession joinedSession, String roomName)
				{
					log(Logger.LOG_NORMAL, "joined session for "+agentName+": "+joinedSession);
					setSession(joinedSession);
					initApplication();
				}

				public void sessionJoinDenied(String roomName, String errMsg)
				{
					log(Logger.LOG_ERROR, "Session Join Denied: "+errMsg);
					myAgent.dispose();
				}
			};

			public void stateReached(StateEvent event)
			{
				ClientConnection.getClientConnection().joinSession(loginModel.getRoomID(), application_class, observer);
			}
		});
	}

	public void setSession(IPersistentSession joinedSession)
	{
		this.session = joinedSession;
		super.setSession(joinedSession);
	}
	
	public IPersistentSession getSession()
	{
		return session;
	}

	void handleLoginFailure()
	{
		SwingUtilities.invokeLater(new Runnable()
		{

			public void run()
			{
				String loginHandle = loginModel.getLoginHandle();
				String message;

				if (loginModel.isFixedProcess() && !loginModel.isPreAuthProcess())
				{
					message = StringUtilities.replacePlaceholder("Logging Issue .... ", 'n', loginHandle);
					loginModel.setMessage(message);
					loginModel.setState(LoginProcessModel.DISABLED_STATE);

				}
				else
				{
					loginModel.setMessage("Logging In ...");
					if (loginModel.isPreAuthProcess())
					{
						loginModel.setSessionKey(null);
					}
					loginModel.setState(LoginProcessModel.ENTER_LOGIN_DATA);
				}
			}
		});
	}

	void doLogin()
	{
		boolean anonymous = !loginModel.isPswNeeded();
		String psw = anonymous ? null : loginModel.getPassword();
		final String loginHandle = loginModel.getLoginHandle();
		ClientConnection.getClientConnection().login(loginHandle, psw, anonymous, createLoginParams(), new ILoginObserver()
		{

			public void loggedIn(LoginEvent event)
			{
				// loginParams = event.getParams();
				if (event.isSuccessfull())
				{
					loginModel.setState(LoginProcessModel.LOGGED_IN);
				}
				else
				{
					loginModel.setFailMessage(event.getFailReason());
					loginModel.setState(LoginProcessModel.LOGIN_FAILED);
				}
			}
		});
	}

	Map createLoginParams()
	{
		HashMap paramMap = new HashMap();
		paramMap.put(ILoginConstants.CHANNELID_KEY, loginModel.getRoomID());
		paramMap.put(ILoginConstants.SESSKIONKEY_KEY, loginModel.getSessionKey());
		return paramMap;
	}

	void initApplication()
	{
		Logger.commonLog("CCListener", Logger.LOG_NORMAL, "initializing CC application...");
		this.application = (IChatApplication) ImplementationsFactory.createObject(application_class);
		application.setSession(this.session);
		this.session.getChannel().addChannelListener(this);
		this.session.getAwarenessModel().addAwarenessListener(this);

		// Send out presence messages for already logged in users
		TableModel users = this.application.getAwarenessPanel().getUserTable().getModel();
		for (int i = 0; i < users.getRowCount(); i++)
		{
			IUser u = UserManager.getUserForUID(users.getValueAt(i, 0));
			PresenceEvent e = new PresenceEvent(this, u.getHandleName(), PresenceEvent.PRESENT);

			Logger.commonLog("CCListener", Logger.LOG_NORMAL, "sending presence event for extant user "+u.getHandleName());
			this.broadcast(e);
		}
	}

	public void logout()
	{
		if (session != null)
		{
			session.leave();
		}
		ClientConnection.getClientConnection().logout();
	}

	@Override
	public void modelChanged(AwarenessEvent event)
	{
		 IAwarenessInfo aware = event.getAwarenessInfo();
		 String type = aware.getType();
		 IUser user = aware.getUser();
		 Object param = aware.getParameters();
		 
		 if(type.equalsIgnoreCase("isTyping"))
		 {
			 TypingEvent typing = new TypingEvent(user.getHandleName());
			 this.broadcast(typing);
		 }

		 Logger.commonLog("CCListener", Logger.LOG_NORMAL, "AWARENESS: "+user.getHandleName()+" "+type+": "+param);
		
	}

	@Override
	public void disconnect()
	{
		logout();
	}

	@Override
	public void login(String roomName)
	{
		connectToChatRoom(roomName);
	}
	
}
