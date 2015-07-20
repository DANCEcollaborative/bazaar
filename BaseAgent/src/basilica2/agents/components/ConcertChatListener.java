/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.agents.components;

import basilica2.agents.components.ConcertChatActor;
import de.fhg.ipsi.chatblocks2.awareness.AbstractAwarenessInfo;
import de.fhg.ipsi.chatblocks2.awareness.AwarenessEvent;
import de.fhg.ipsi.chatblocks2.awareness.DefaultAddOperation;
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
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.TypingEvent;
import de.fhg.ipsi.concertchat.applications.whiteboard.*;
import de.fhg.ipsi.concertchat.*;
import de.fhg.ipsi.utils.ImageUtilities;
import de.fhg.ipsi.whiteboard.Command;
import de.fhg.ipsi.whiteboard.Graphic;
import de.fhg.ipsi.whiteboard.OutlineProperties;
import de.fhg.ipsi.whiteboard.operation.CreateCommand;
import de.fhg.ipsi.whiteboard.piece.image.ImageCache;
import de.fhg.ipsi.whiteboard.piece.image.ImageStuff;
import de.fhg.ipsi.whiteboard.piece.text.TextStuff;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;

/**
 * 
 * @author rohitk
 */
public class ConcertChatListener extends Component implements ChannelListener, IAwarenessListener
{

	public static String GENERIC_NAME = "ConcertChatListener";
	public static String GENERIC_TYPE = "Filter";
	private String agentName = "Tutor";
	private String application_class = "de.fhg.ipsi.concertchat.applications.whiteboard.WhiteboardChat";
	private String chat_room_name = null;
	private String session_key = null;
	IChatApplication application;
	IPersistentSession session;
	LoginProcessModel loginModel;
	// Map loginParams;
	ConcertChatActor ccActor;
	IClientConnection myConnection;
	
	public ConcertChatListener(Agent a, String agentName, String n, String pf)
	{
		super(a, n, pf);
		
		int underscore = agentName.indexOf("_");
		if (underscore > -1)
			this.agentName = agentName.substring(0, underscore);
		else
			this.agentName = agentName;
	}
	
	public ConcertChatListener(Agent a, String n, String pf)
	{
		this(a, a.getName(), n, pf);
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
	
	public void connectToChatRoom(String r, ConcertChatActor a)
	{
		log(Logger.LOG_NORMAL, "connecting to "+r);
		chat_room_name = r;
		ccActor = a;
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

	private void setSession(IPersistentSession joinedSession)
	{
		this.session = joinedSession;
		ccActor.setSession(session);
	}

	public ConcertChatActor getCCActor()
	{
		return ccActor;
	}

	public void setCCActor(ConcertChatActor ccActor)
	{
		this.ccActor = ccActor;
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
}
