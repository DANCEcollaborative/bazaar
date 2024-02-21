package basilica2.agents.listeners;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Properties;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.URI;
import java.net.HttpURLConnection;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.util.HttpUtility;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class ExternalJSONListener extends BasilicaAdapter
{
	public String host;
	public String port; 
	public String path;
	public String charset;
	public String delimiter;
	public String start_flag;
	private static String messageSpec = "message=";
	private static String multiModalDelim = ";%;";
	private static String withinModeDelim = ":::";	
	private static String multimodal_spec = "multimodal";
	private static String true_spec = "true"; 
	private static String identity_spec = "identity";
	private static String speech_spec = "speech"; 

	public ExternalJSONListener(Agent a)
	{
		super(a);
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		
		try
		{
			host = properties.getProperty("host","bazaar.lti.cs.cmu.edu");
			port = properties.getProperty("port","1248");
			path = properties.getProperty("path","/messageUpdate/");
			charset = properties.getProperty("charset","UTF-8");
			start_flag = properties.getProperty("start_flag","?");
			delimiter = properties.getProperty("delimiter","&");
		}
		catch (Exception e){}
	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent)
		{
			handleMessageEvent(source, (MessageEvent) e);
		}
	}
	

	public void handleMessageEvent(InputCoordinator source, MessageEvent me)
	{
		String roomName = agent.getRoomName();
		String room = "session_id=" + roomName; 
		String encodedIdentity = ""; 
		String encodedMessageText = ""; 
		
		try {
			encodedMessageText = URLEncoder.encode(me.getText(),charset).replace("+", "%20");
			encodedIdentity = URLEncoder.encode(me.getFrom(),charset).replace("+", "%20");
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return; 
	    }
		
//		String message = "message=multimodal:::true;%;identity:::" + identity + ";%;speech:::" + me.getText();
		String message = messageSpec+ multimodal_spec + withinModeDelim + true_spec + multiModalDelim + identity_spec  + withinModeDelim +
				encodedIdentity +  multiModalDelim + speech_spec + withinModeDelim + encodedMessageText;

		
		String externalMessage = start_flag + room + delimiter + message; 
		System.err.println("ExternalJSONListener, execute -- externalMessage: " + externalMessage); 
		log(Logger.LOG_NORMAL, "ExternalJSONListener execute -- externalMessage: " + externalMessage);
		Logger.commonLog("ExternalJSONListener", Logger.LOG_NORMAL, " execute -- externalMessage:  + externalMessage");
		String response = sendExternalMessageGet(externalMessage);	
		System.err.println("ExternalJSONListener, execute -- response: " + response); 
	}


	public String sendExternalMessageGet(String message)
	{
		String requestURL = host + ":" + port + path + message; 
		System.err.println("ExternalJSONListener, sendExternalMessageGet -- requestURL: " + requestURL); 
		log(Logger.LOG_NORMAL, "ExternalJSONListener sendExternalMessageGet -- requestURL: " + requestURL);
		Logger.commonLog("ExternalJSONListener", Logger.LOG_NORMAL, " sendExternalMessageGet -- requestURL: " + requestURL);	
		
		try {
			HttpUtility.sendPostRequest(requestURL, null)
//			HttpUtility.sendGetRequest(requestURL); 
			String response = HttpUtility.readSingleLineResponse(); 
			System.err.println("ExternalJSONListener, sendExternalMessage -- response: " + response); 
			return response; 
			
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return "sendExternalMessage returned an IOException"; 
	    }	
	}
	


	@Override
	public void processEvent(InputCoordinator source, Event event) {
		// TODO Auto-generated method stub
		
	}	
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return null;
	}
}