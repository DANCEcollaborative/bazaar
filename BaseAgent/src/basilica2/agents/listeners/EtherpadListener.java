package basilica2.agents.listeners;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.FileEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.TypingEvent;
import basilica2.agents.events.WhiteboardEvent;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import de.fhg.ipsi.utils.StringUtilities;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

//TODO: EtherpadListener.properties

public class EtherpadListener extends BasilicaAdapter
{	
	
	//TODO: parameterize these.
	int port = 3306;
	int checkInterval = 2000;
	String host = "128.2.220.51";
	String user = "bazaar";
	String password = "********";
	String dbName = "etherpad-lite";
	// String agentUserName="guest";

	MysqlDataSource dataSource = new MysqlDataSource();
	Connection conn;	
	boolean connected = false;	
	// int chatID;
	// int userID;
	long lastTime = 0;
	
	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#disconnect()
	 */
	/*
	@Override
	public void disconnect()
	{
		System.out.println("Etherpad disconnecting...");
		connected = false;
		try
		{
			exitChat(conn);
			conn.close();
		}
		catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/ 

	//TODO: poll for incoming messages
	//TODO: report presence events
	//TODO: report extant students at login
	

	public EtherpadListener(Agent a)
	{
		super(a);
		if (properties != null)
		{
			try{host = getProperties().getProperty("host", host);}
			catch(Exception e) {e.printStackTrace();}
			try{user = getProperties().getProperty("user", user);}
			catch(Exception e) {e.printStackTrace();}	
			try{password = getProperties().getProperty("password", password);}
			catch(Exception e) {e.printStackTrace();}	
			try{dbName = getProperties().getProperty("dbName", dbName);}
			catch(Exception e) {e.printStackTrace();}						
		}
		roomName = a.getRoomName();
		

		// agentUserName = myProperties.getProperty("moodle_agent_username", agentUserName);
		
		try{port = Integer.parseInt(myProperties.getProperty("db_port", ""+port));}
		catch(NumberFormatException e) {e.printStackTrace();}

		try{checkInterval = Integer.parseInt(myProperties.getProperty("check_interval", ""+port));}
		catch(NumberFormatException e) {e.printStackTrace();}
		
		System.out.println("mysql://"+user+"@"+host+":"+port+"/"+dbName);
		// System.out.println(agentUserName);
	}

	@Override
	protected void processEvent(Event e)
	{
		/**
		if(e instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent) e;
			try
			{
				insertMessage(me.getText(), conn);
			}
			catch (Exception e1)
			{
				System.err.println("couldn't send message: "+me);
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		//TODO: private messages? "beeps"?
		*/ 
	}
	
	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#login(java.lang.String)
	 */
	// @Override
	public void login(String roomName)
	{
		// System.out.println("logging in to "+roomName+"@"+host);
		System.out.println("logging in to mysql @ "+host);
		try
		{
			dataSource.setUser(user);
			dataSource.setPassword(password);
			dataSource.setServerName(host);
			dataSource.setPort(port);
			dataSource.setDatabaseName(dbName);

			conn = dataSource.getConnection();
			
			// chatID = getChatID(roomName, conn);
			// userID = getUserID(agentUserName, conn);
			connected = true;
			
			lastTime = getSystemTime();
			
			// enterChat(	chatID, userID, conn);
			
			/*
			new Thread()
			{
				public void run()
				{
					while(connected)
					{
						try
						{
							justPing(conn);
							Thread.sleep(checkInterval);
//							System.out.println("checking room "+chatID);
							checkMessages(conn);
								
						}
						catch(Exception e)
						{
							e.printStackTrace();
	//						connected = false;
						}
					}
				}
			}.start();
			*/			
		}
		catch (Exception e)
		{
			System.err.println("Couldn't log in to the chat server...");
			e.printStackTrace();
			
			JOptionPane.showMessageDialog(null, "Couldn't access chat database: "+ e.getMessage(), "Login Failure", JOptionPane.ERROR_MESSAGE);
			
			connected = false;
		}

	}


	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		/*
		File file; 
		FileEvent.fileEventType eventType = FileEvent.fileEventType.valueOf("created"); 
		for (int i=0; i < fileCompleted.length; i++) {
			if (!fileCompleted[i]) {
				file = new File(filePath + "room-" + roomName + "-" + fileNames[i] + fileSuffix);
				System.err.println("Checking file: " + file.getPath()); 
				synchronized(source) {
					if (file.exists()) {
						fileCompleted[i] = true;
						System.err.println("File newly exists: " + file.getPath()); 
						FileEvent fEvent = new FileEvent(source,fileNames[i],eventType);
						source.pushEvent(fEvent);
					}				
				}					
			}

		}
		*/
	}	
	
	
	private int getUserID(String userName, Connection conn) throws SQLException
	{
		String userQuery = "select id from mdl_user where username='"+userName+"'";

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(userQuery);

		int userID = 1;
		if (rs.next())
		{
			userID = rs.getInt("id");

		}
		else
		{
			throw new RuntimeException("Etherpad user '"+userName+"' does not exist!");
		}
		rs.close();
		stmt.close();
		
		return userID;
	}
	
	private int getChatID(String roomName, Connection conn) throws SQLException
	{
		String roomQuery = "select id from mdl_chat where name='"+roomName+"'";

		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(roomQuery);

		int roomID = 1;
		if (rs.next())
		{
			roomID = rs.getInt("id");

		}
		else
		{
			throw new RuntimeException("Etherpad chat room '"+roomName+"' does not exist!");
		}
		rs.close();
		stmt.close();
		
		return roomID;
	}

	@Override
	public String getType()
	{
		return "ChatClient";
	}
	//unneccessary.
		protected void exitChat(Connection conn) throws SQLException
		{
			String insertExit = "INSERT INTO mdl_chat_messages_current (chatid, userid, groupid, system, message, timestamp) " +
					"VALUES (" + chatID + ", "
					+ userID + ", '0', '1', 'exit', UNIX_TIMESTAMP(NOW()));";

//			String deleteRow = "DELETE FROM mdl_chat_users WHERE chatid="+chatID+" and userID="+userID+";";
			
			Statement stmt;
			stmt = conn.createStatement();
			stmt.addBatch(insertExit);
//			stmt.addBatch(deleteRow);
			stmt.executeBatch();

			stmt.close();
		}

		protected void enterChat(int chatID, int userID, Connection conn) throws SQLException
		{
			String insertEnter = "INSERT INTO mdl_chat_messages_current (chatid, userid, groupid, system, message, timestamp) " +
					"VALUES (" + chatID + ", "	+ userID + ", '0', '1', 'enter', UNIX_TIMESTAMP(NOW())); ";

			
			String insertPing =  "INSERT INTO mdl_chat_users (chatid, userid, groupid, firstping, lastping, lastmessageping) " +
					"VALUES ("+chatID+", "+userID+", 0, UNIX_TIMESTAMP(NOW()), UNIX_TIMESTAMP(NOW()), UNIX_TIMESTAMP(NOW()) );";
			
			Statement stmt;
			stmt = conn.createStatement();
			stmt.addBatch(insertEnter);
			stmt.addBatch(insertPing);
			stmt.executeBatch();

			stmt.close();
		}

		protected void justPing(Connection conn) throws SQLException
		{
			String updatePing = "UPDATE mdl_chat_users SET lastping=UNIX_TIMESTAMP(NOW()) " +
					"where chatid="+chatID+" and userid="+userID+";";
			
			Statement stmt;
			
			stmt = conn.createStatement();
			stmt.execute(updatePing);

			stmt.close();
		}
		
		protected void insertMessage(String message, Connection conn) throws SQLException
		{
			
			message = message.replaceAll("\"", "\\\\\"");
			
			String insertMessage = "INSERT INTO mdl_chat_messages_current (chatid, userid, groupid, system, message, timestamp) " +
					"VALUES (" + chatID + ", "
					+ userID + ", '0', '0', \"" + message + "\", UNIX_TIMESTAMP(NOW()));";
			
			String pingMessage = "UPDATE mdl_chat_users SET lastping=UNIX_TIMESTAMP(NOW()), lastmessageping=UNIX_TIMESTAMP(NOW()) " +
					"where chatid="+chatID+" and userid="+userID+";";
			
			Statement stmt;
			
			stmt = conn.createStatement();
			stmt.addBatch(insertMessage);
			stmt.addBatch(pingMessage);
			stmt.executeBatch();

			stmt.close();
		}

		protected long getSystemTime() throws SQLException
		{
			String timeQuery = "select UNIX_TIMESTAMP(NOW()) from mdl_chat;";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(timeQuery);
			if(rs.next())
			{
				return rs.getLong(1);
			}
			this.log(Logger.LOG_ERROR, "no time!");
			return 0;
		}
		
		protected void checkMessages(Connection conn) throws SQLException
		{
	
			String messageQuery = "select firstname, lastname, username, message, system, timestamp " +
					"from mdl_chat_messages_current, mdl_user where chatid = " + chatID
					+ " AND timestamp > " + lastTime + " AND mdl_chat_messages_current.userid=mdl_user.id ORDER BY timestamp ASC";

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(messageQuery);

			while (rs.next())
			{
				
				lastTime = rs.getLong("timestamp");
				
				Date when = new Date(lastTime);
				String text = rs.getString("message");
				String userFrom = rs.getString("firstname")+" "+rs.getString("lastname");
//				String userFrom = rs.getString("username");
				String incomingUsername = rs.getString("username");
				boolean systemEvent = rs.getInt("system") != 0;
				
				Event bazaarEvent = null;

				this.log(Logger.LOG_NORMAL, "Incoming Etherpad Event "+when+"\t"+userFrom+"\t'"+text+"'");
				
				if(incomingUsername.equals(agentUserName))
				{
					if(!systemEvent)
					{
						bazaarEvent = new EchoEvent(this, new MessageEvent(this, this.getAgent().getUsername(), text));
					}
				}
				
				else if(systemEvent)
				{
					if(text.equals("enter"))
					{
						bazaarEvent = new PresenceEvent(this, userFrom, PresenceEvent.PRESENT);
					}
					if(text.equals("exit"))
					{
						bazaarEvent = new PresenceEvent(this, userFrom, PresenceEvent.ABSENT);
					}
					else this.log(Logger.LOG_WARNING, "Unknown System event '"+text+"' from "+userFrom);
				}
				
				else
				{
					bazaarEvent = new MessageEvent(this, userFrom, text);
				}
				
				
				if(bazaarEvent != null)
				{
					this.broadcast(bazaarEvent);
				}

			}
			rs.close();
			stmt.close();
			
		}
		
		/**
		 * @return the classes of events that this Preprocessor cares about
		 */
		@Override
		public Class[] getPreprocessorEventClasses()
		{
			return new Class[]{MessageEvent.class, ReadyEvent.class, PresenceEvent.class, WhiteboardEvent.class, TypingEvent.class};
		}


		@Override
		public void processEvent(InputCoordinator source, Event event) {
			// TODO Auto-generated method stub
			
		}


		@Override
		public Class[] getListenerEventClasses() {
			// TODO Auto-generated method stub
			return null;
		}


}
