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
	private int port = 3306;
	// int checkInterval = 2000;
	private String host = "128.2.220.51";	
	private String user = "bazaar";
	private String password = "********";
	private String databaseName = "etherpad-lite";
	private String tableName = "store";
	private String roomNamePrefix = "j";
	// String agentUserName="guest";
	MysqlDataSource dataSource = new MysqlDataSource();
	Connection conn;	
	private boolean connected = false;	
	// int chatID;
	// int userID;
	// long lastTime = 0;
	private InputCoordinator source;
	private String agentID; 
	// private String status = "";
	private String roomName; 	
	private int rowCount; 	
	
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
			try{port = Integer.parseInt(getProperties().getProperty("port", ""+port));}
			catch(Exception e) {e.printStackTrace();}
			try{user = getProperties().getProperty("user", user);}
			catch(Exception e) {e.printStackTrace();}	
			try{password = getProperties().getProperty("password", password);}
			catch(Exception e) {e.printStackTrace();}	
			try{databaseName = getProperties().getProperty("databaseName", databaseName);}
			catch(Exception e) {e.printStackTrace();}	
			try{tableName = getProperties().getProperty("tableName", tableName);}
			catch(Exception e) {e.printStackTrace();}	
			try{roomNamePrefix = getProperties().getProperty("roomNamePrefix", roomNamePrefix);}
			catch(Exception e) {e.printStackTrace();}		
			try{agentID = getProperties().getProperty("agentID", agentID);}
			catch(Exception e) {e.printStackTrace();}					
		}
		roomName = a.getRoomName();
		System.err.println("roomName: " + roomName); 
		System.err.println("mysql://"+user+"@"+host+":"+port+"/"+databaseName+"/"+tableName+"/"+roomNamePrefix+agentID+roomName);
		
		try
		{
			dataSource.setUser(user);
			dataSource.setPassword(password);
			dataSource.setServerName(host);
			dataSource.setPort(port);
			dataSource.setDatabaseName(databaseName);
			conn = dataSource.getConnection();
			connected = true;		
			// lastTime = getSystemTime();		
		}
		catch (Exception e)
		{
			System.err.println("Couldn't login to mysql");
			e.printStackTrace();
			connected = false;
		}
		
	}

	// @Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		try
		{
			checkMessages(conn);
		}
		catch (Exception e1)
		{
			System.err.println("EtherpadListener: couldn't checkMessages");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

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
			
			// TODO: Improve this query 
			String messageQuery = "select * from " + tableName + " where value like '%" + roomNamePrefix + agentID + roomName +"%'";
			System.err.println("mysql query: " + messageQuery); 
				
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(messageQuery);
			
			rowCount = 0;
		      while (rs.next()) {
		           rowCount++;
		      }
		    System.err.println("EtherpadListener, row count for " + roomNamePrefix + " is " + Integer.toString(rowCount));
		    if (rowCount > 1) {
		    	System.err.println("=== Activity detected in Etherpad *** " + roomNamePrefix + " ***");
		    }
		    /**
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
			*/ 
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
