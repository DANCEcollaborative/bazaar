package basilica2.agents.listeners;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
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


public class EtherpadListener extends BasilicaAdapter
{	
	private int port = 3306;
	// int checkInterval = 2000;
	private String host = "128.2.220.51";	
	private String user = "bazaar";
	private String password = "********";
	private String databaseName = "etherpad-lite";
	private String tableName = "store";
	private String roomNamePrefix = "j";
	MysqlDataSource dataSource = new MysqlDataSource();
	Connection conn;	
	private boolean connected = false;	
	private Agent agent; 
	private InputCoordinator source;
	private String roomName; 	
	private String role;   // The role for this Etherpad activity
	private int rowCount; 	
	

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
			try {
				role = getProperties().getProperty("role", role);
				role = role.replace("_", " "); 
			}
			catch(Exception e) {e.printStackTrace();}					
		}
		agent = a; 
		roomName = a.getRoomName();
		// System.out.println("roomName: " + roomName); 
		// System.out.println("mysql://"+user+"@"+host+":"+port+"/"+databaseName+"/"+tableName+"/"+roomNamePrefix+roomName);
		
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
		String messageQuery = "select * from " + tableName + " where `key` like '%" + roomNamePrefix + roomName +"%'";
		System.out.println("mysql query: " + messageQuery); 
			
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(messageQuery);

		String nextKey; 
		String[] keySplit;
		int maxRevisionNumber = 0;
		int revisionNumber; 
		rowCount = 0;
	    while (rs.next()) {
	        rowCount++;
	        nextKey = rs.getString("key"); 
	        keySplit = nextKey.split(":");
	        if (keySplit.length > 2) {
	            if (keySplit[2].equals("revs")) { 
	        		if (keySplit.length > 3) {
	        			revisionNumber = Integer.parseInt(keySplit[3]); 
	        			if (revisionNumber > maxRevisionNumber) {
	        				maxRevisionNumber = revisionNumber;
	        			}	   
	        		}
	        	}
	        }	           
	    }
	    System.err.println("EtherpadListener " + roomNamePrefix + ", maxRevisionNumber: " + Integer.toString(maxRevisionNumber)); 
	    updateActivityMetric(role, maxRevisionNumber); 
		rs.close();
		stmt.close();		
	}
	
	private void updateActivityMetric (String role, int currentRevisionNumber) {
		// Check if role is for an individual student 
		//    -- Current support is only for roles that are for individuals or for the group as a whole.
		//       (E.g, roles for subgroups of students are not currently supported.)
		State state = StateMemory.getSharedState(agent);
		Boolean roleFound = false; 
		String[] studentIds = state.getStudentIds(); 
		String studentForRole = null; 
		for (int i=0; i < studentIds.length && !roleFound; i++) {
			String studentRole = state.getStudentRole(studentIds[i]);
			if (studentRole.equals(role)) {
				roleFound = true;
				studentForRole = studentIds[i]; 
			}
		}
		
		// Update either student.activityMetric or jointActivityMetric
		if (studentForRole != null) {
			state.setStudentActivityMetric(studentForRole, currentRevisionNumber); 
		} else {
			state.setJointActivityMetric(currentRevisionNumber); 
		}
		
		// Temp for testing
		// System.err.println("jointActivityMetric: " + state.getJointActivityMetric()); 
		
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
