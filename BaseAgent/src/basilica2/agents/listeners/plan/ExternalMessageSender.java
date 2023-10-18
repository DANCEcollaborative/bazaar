package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import edu.cmu.cs.lti.basilica2.core.Agent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;
import basilica2.util.HttpUtility;

import java.io.IOException;
import java.net.URLEncoder;

class ExternalMessageSender implements StepHandler
{
	private String host;
	private String port; 
	private String path;
	private String delimiter;
	private String charset;
	
	
	public static String getStepType()
	{
		return "send_external_message";
	}

	public ExternalMessageSender()
	{
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");

//		Agent agent = overmind.getAgent();
//		myLogger = new Logger(this, agentName);
//		myLogger.setConfiguration(true, true, true, true, false, true);
		
		try
		{
			String host = properties.getProperty("host","bazaar.lti.cs.cmu.edu");
			String port = properties.getProperty("port","1248");
			String path = properties.getProperty("path","/stepUpdate/");
			String delimiter = properties.getProperty("delimiter","|||");
			String charset = properties.getProperty("charset","UTF-8");
//			commander = new PromptTable(commandsPath);
		}
		catch (Exception e){}
	}
	

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
		Agent agent = overmind.getAgent();
//		State news = StateMemory.getSharedState(overmind.getAgent());
		State news = StateMemory.getSharedState(agent);

		String roomName = agent.getRoomName();

		String delimiter = "|||";
		String charset = "UTF-8";
		
		String message = step.attributes.get("message");
//		String room = "?session_id=" + roomName; 
		String room = "session_id=" + roomName; 
		String roomPlusMessage = room + "&" + message; 
		String encodedMessage; 
		String encodedRoom; 
		


		
		try {
			encodedMessage = URLEncoder.encode(message,charset).replace("+", "%20");
			encodedRoom = URLEncoder.encode(room,charset).replace("+", "%20");
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return; 
	    }
		
		 

//		String externalMessage = "?" + encodedRoom + "&" + encodedMessage; 
		String externalMessage = "?" + room + "&" + message; 
//		String externalMessage = roomName + delimiter + encodedMessage; 
//		System.err.println("ExternalMessageSender, execute -- encodedMessage: " + encodedMessage); 
		System.err.println("ExternalMessageSender, execute -- externalMessage: " + externalMessage); 
//		log(Logger.LOG_NORMAL, "ExternalMessageSender execute -- externalMessage: " + externalMessage);
		Logger.commonLog("ExternalMessageSender", Logger.LOG_NORMAL, "execute -- externalMessage: \" + externalMessage");
//		log(Logger.LOG_NORMAL, "ExternalMessageSender, execute -- externalMessage: " + externalMessage);
		String response = sendExternalMessageGet(externalMessage); 
//		String response = sendExternalMessage(encodedMessage);
		

//		String response = sendExternalMessageGet(encodedMessage);
		
		System.err.println("ExternalMessageSender, execute -- response: " + response); 
		overmind.stepDone();		
	}


	public String sendExternalMessageGet(String message)
	{		
		String host = "http://bazaar.lti.cs.cmu.edu";
		String port = "1248";
		String path = "/stepUpdate/";
//		String path = "/stepUpdate";
		
		// TEMP FOR TESTING
//		String port = "5000";
//		String path = "/lobbyDeleteRoom/";
////		String path = "/lobbyDeleteRoom";
		
		try {
			String requestURL = host + ":" + port + path + message; 
			HttpUtility.sendGetRequest(requestURL); 
			String response = HttpUtility.readSingleLineResponse(); 
			System.err.println("ExternalMessageSender, sendExternalMessage -- response: " + response); 
			return response; 
			
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return "sendExternalMessage returned an IOException"; 
	    }	
	}

}