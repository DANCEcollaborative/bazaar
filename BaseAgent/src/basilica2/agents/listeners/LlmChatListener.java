package basilica2.agents.listeners;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Properties;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.util.HttpUtility;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;

public class LlmChatListener extends BasilicaAdapter
{
	public String host;
	public String port; 
	public String path;
	public String charset;
	public String delimiter;
	public String start_flag;
	private String apiKey;
	private String requestURL;
	private String modelName;
	private String context;
	private double temperature;
	private static String messageSpec = "message=";
	private static String multiModalDelim = ";%;";
	private static String withinModeDelim = ":::";	
	private static String multimodal_spec = "multimodal";
	private static String true_spec = "true"; 
	private static String identity_spec = "identity";
	private static String speech_spec = "speech"; 

	public LlmChatListener(Agent a)
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
		
		Properties llm_prop = PropertiesLoader.loadProperties("LlmChatListener.properties");
		try {
			apiKey = llm_prop.getProperty("openai.api.key");
			requestURL = llm_prop.getProperty("openai.request.url");
			modelName = llm_prop.getProperty("openai.model.name");
			temperature = Double.valueOf(llm_prop.getProperty("openai.temperature"));
			context = llm_prop.getProperty("openai.prompt.context");
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
	
	public void handleMessageEvent(InputCoordinator source, MessageEvent me) {
	    // Prepare the prompt based on the received message
	    String prompt = this.context.length() > 0 ? this.context + " " + me.getText() : me.getText();
	    String modelName = this.modelName;
	    Double temperature = this.temperature;
	    // Construct the payload for OpenAI
	    String jsonPayload = "{" +
	    	    "\"model\": \"" + modelName + "\"," +
	    	    "\"temperature\": " + temperature + "," +
	    	    "\"messages\": [" +
	    	        "{" +
	    	            "\"role\": \"user\"," +
	    	            "\"content\": \"" + prompt.replace("\"", "\\\"") + "\"" +
	    	        "}" +
	    	    "]" +
	    	"}";
	    // Sending the message to OpenAI and receiving the response
	    String response = sendToOpenAI(jsonPayload);


        MessageEvent newMe = new MessageEvent(source, this.getAgent().getUsername(), response);
		PriorityEvent blackout = PriorityEvent.makeBlackoutEvent("LLM", newMe, 1.0, 5, 5);
		blackout.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p) {}
			@Override
			public void rejected(PriorityEvent p) {} // ignore our rejected proposals
		});
		source.pushProposal(blackout);
	    Logger.commonLog("ExternalChatListener", Logger.LOG_NORMAL, "LlmChatListener, execute -- response from OpenAI: " + response); 
	}
//	public void handleMessageEvent(InputCoordinator source, MessageEvent me)
//	{
//		String roomName = agent.getRoomName();
//		String room = "session_id=" + roomName; 
//		String encodedIdentity = ""; 
//		String encodedMessageText = ""; 
//		
//		try {
//			encodedMessageText = URLEncoder.encode(me.getText(),charset).replace("+", "%20");
//			encodedIdentity = URLEncoder.encode(me.getFrom(),charset).replace("+", "%20");
//	    } catch (IOException e) {
//	    	e.printStackTrace();
//	    	return; 
//	    }
//		
////		String message = "message=multimodal:::true;%;identity:::" + identity + ";%;speech:::" + me.getText();
//		String message = messageSpec+ multimodal_spec + withinModeDelim + true_spec + multiModalDelim + identity_spec  + withinModeDelim +
//				encodedIdentity +  multiModalDelim + speech_spec + withinModeDelim + encodedMessageText;
//
//		
//		String externalMessage = start_flag + room + delimiter + message; 
//		System.err.println("ExternalChatListener, execute -- externalMessage: " + externalMessage); 
//		log(Logger.LOG_NORMAL, "ExternalChatListener execute -- externalMessage: " + externalMessage);
//		Logger.commonLog("ExternalChatListener", Logger.LOG_NORMAL, " execute -- externalMessage:  + externalMessage");
//		String response = sendExternalMessageGet(externalMessage);	
//		System.err.println("ExternalChatListener, execute -- response: " + response); 
//	}

	public String sendToOpenAI(String jsonPayload) {
	    String apiKey = this.apiKey;
	    String requestURL = this.requestURL;
	    
	    try {
	        URL url = new URL(requestURL);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("POST");
	        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
	        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
	        conn.setDoOutput(true);
	        
	        try(OutputStream os = conn.getOutputStream()) {
	            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
	            os.write(input, 0, input.length);           
	        }
	        
	        int responseCode = conn.getResponseCode();
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	            // Read input stream
	        	StringBuilder response = new StringBuilder();
		        try (BufferedReader reader = new BufferedReader(
		                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
		            String line;
		            while ((line = reader.readLine()) != null) {
		                response.append(line.trim());
		            }
		        }
//		        return response.toString();
		        // Parse the raw response into a JSONObject
		        JSONObject jsonResponse = new JSONObject(response.toString());

		        //Extract the choices array from the response
		        JSONArray choices = jsonResponse.getJSONArray("choices");
		        System.out.println(response.toString());
		        // Check if there are choices available
		        if (choices.length() > 0) {
		            // Extract the text from the first choice
		            JSONObject responseMessage = choices.getJSONObject(0).getJSONObject("message");
		            String responseText = responseMessage.getString("content");
		            System.out.println("Extracted Response Text: " + responseText);
		            return responseText;
		        } else {
		            System.err.println("No choices found in the response.");
		            return "Error: no text found";
		        }
		        
	        } else {
	            // Read error stream
	            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
	            String line;
	            StringBuilder response = new StringBuilder();
	            while ((line = errorReader.readLine()) != null) {
	                response.append(line);
	            }
	            errorReader.close();
	            // Log or print the error response
	            System.err.println("Error response: " + response.toString());
	            return "Error response: " + response.toString();
	        }
//	        // Read the response...
//	        StringBuilder response = new StringBuilder();
//	        try (BufferedReader reader = new BufferedReader(
//	                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
//	            String line;
//	            while ((line = reader.readLine()) != null) {
//	                response.append(line.trim());
//	            }
//	        }
//	        
//	        return response.toString();
//	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "Failed to send message to OpenAI";
	    }
	    
	}

//	public String sendExternalMessageGet(String message)
//	{
//		String requestURL = host + ":" + port + path + message; 
//		System.err.println("ExternalChatListener, sendExternalMessageGet -- requestURL: " + requestURL); 
//		log(Logger.LOG_NORMAL, "ExternalChatListener sendExternalMessageGet -- requestURL: " + requestURL);
//		Logger.commonLog("ExternalChatListener", Logger.LOG_NORMAL, " sendExternalMessageGet -- requestURL: " + requestURL);	
//		
//		try {
//			HttpUtility.sendGetRequest(requestURL); 
//			String response = HttpUtility.readSingleLineResponse(); 
//			System.err.println("ExternalChatListener, sendExternalMessage -- response: " + response); 
//			return response; 
//			
//	    } catch (IOException e) {
//	    	e.printStackTrace();
//	    	return "sendExternalMessage returned an IOException"; 
//	    }	
//	}
	


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