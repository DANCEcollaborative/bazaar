package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.components.InputCoordinator;
import edu.cmu.cs.lti.basilica2.core.Agent;
import basilica2.util.PropertiesLoader;
import basilica2.util.HttpUtility;

import java.io.IOException;

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

//		myLogger = new Logger(this, agentName);
//		myLogger.setConfiguration(true, true, true, true, false, true);
		
		try
		{
			String host = properties.getProperty("host","bazaar.lti.cs.cmu.edu");
			String port = properties.getProperty("port","1248");
			String path = properties.getProperty("path","/stepUpdate/");
			String delimiter = properties.getProperty("delimiter","|||");
			String charset = properties.getProperty("charset","UTF-8");
		}
		catch (Exception e){}
	}
	

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
		String charset = "UTF-8";		
		Agent agent = overmind.getAgent();
		String roomName = agent.getRoomName();
		String message = step.attributes.get("message");
		String room = "session_id=" + roomName; 		
		
//		String encodedMessage; 
//		String encodedRoom; 		
//		try {
//			encodedMessage = URLEncoder.encode(message,charset).replace("+", "%20");
//			encodedRoom = URLEncoder.encode(room,charset).replace("+", "%20");
//	    } catch (IOException e) {
//	    	e.printStackTrace();
//	    	return; 
//	    }
		
		String externalMessage = "?" + room + "&" + message; 
		System.err.println("ExternalMessageSender, execute -- externalMessage: " + externalMessage); 
//		log(Logger.LOG_NORMAL, "ExternalMessageSender execute -- externalMessage: " + externalMessage);
		Logger.commonLog("ExternalMessageSender", Logger.LOG_NORMAL, "execute -- externalMessage: \" + externalMessage");
		String response = sendExternalMessageGet(externalMessage);	
		System.err.println("ExternalMessageSender, execute -- response: " + response); 
		
		overmind.stepDone();		
	}


	public String sendExternalMessageGet(String message)
	{		
		String host = "http://bazaar.lti.cs.cmu.edu";
		String port = "1248";
		String path = "/stepUpdate/";
		
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