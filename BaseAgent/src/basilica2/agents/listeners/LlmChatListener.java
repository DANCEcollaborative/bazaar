package basilica2.agents.listeners;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.BotMessageEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.util.HttpUtility;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.listeners.*;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

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
	
	// Timer to track inactivity
    private Timer inactivityTimer;
    private long inactivityPeriod = 30 * 1000; // 30 seconds in milliseconds by default
    private String inactivityPrompt;
    private boolean inactivityPromptFlag;
    private boolean contextFlag;
    private int contextLen;
    private String myName;
//    private ChatHistoryListener CHL;
//    private List<String> chatHistory;

	@SuppressWarnings("static-access")
	public LlmChatListener(Agent a)
	{
		super(a);
		Properties api_key_prop = PropertiesLoader.loadProperties("apiKey.properties");
		apiKey = api_key_prop.getProperty("openai.api.key");
		Properties llm_prop = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		try {
			
			requestURL = llm_prop.getProperty("openai.request.url");
			modelName = llm_prop.getProperty("openai.model.name");
			temperature = Double.valueOf(llm_prop.getProperty("openai.temperature"));
			myName = llm_prop.getProperty("name", "OAI");
			context = llm_prop.getProperty("openai.prompt.context").format(myName);
			inactivityPeriod = Long.parseLong(llm_prop.getProperty("openai.timer.timeout")) * 1000;
			inactivityPrompt = llm_prop.getProperty("openai.prompt.timeout");
			// Initialize the inactivity timer
			inactivityPromptFlag = Boolean.parseBoolean(llm_prop.getProperty("openai.flag.timeout"));
			if (inactivityPromptFlag) {
				inactivityTimer = new Timer();
			}
			contextFlag = Boolean.parseBoolean(llm_prop.getProperty("openai.context.flag"));
			if (contextFlag) {
				contextLen = Integer.parseInt(llm_prop.getProperty("openai.context.length"));
			}
	        
	        
		}
		catch (Exception e){}
		
		
		
		
	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent)
		{

			boolean proceed = messageFilter((MessageEvent) e);
			if (proceed) {
				try {
					handleMessageEvent(source, (MessageEvent) e);
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	
			} 
			if (inactivityPromptFlag) {
				resetInactivityTimer(source);
				Logger.commonLog("LLMChatListener", Logger.LOG_NORMAL, "TIME OUT... sending prompt to the room");
			}
			
		}
	}
	
	public boolean messageFilter(MessageEvent e) {
		String message = e.getText();
		String globalActiveListenerName = StateMemory.getSharedState(agent).getGlobalActiveListener();
		if (globalActiveListenerName.equals(this.myName)) {
			return true;
		} else if (globalActiveListenerName.equals("") && message.contains(this.myName)) {
			return true;
		}
		return false;
	}
	
	public void handleMessageEvent(InputCoordinator source, MessageEvent me) throws JSONException {
	    // Prepare the prompt based on the received message
	    String prompt = me.getText(); // student chat message
	    String jsonPayload = constructPayloadWithHistory(source, prompt);
	    
	    
	    // Sending the message to OpenAI and receiving the response
	    String response = sendToOpenAI(source, jsonPayload, false);
	    
	    

//        MessageEvent newMe = new MessageEvent(source, this.getAgent().getUsername(), response);
        MessageEvent newMe = new MessageEvent(source, this.myName, response);
        
//        State s = State.copy(StateMemory.getSharedState(agent));
//        if  (response.contains("?")) {
//	        s.setGlobalActiveListener(this.myName);
//	    } else {
//        	s.setGlobalActiveListener("");
//        }
//        StateMemory.commitSharedState(s, agent);
        source.pushEventProposal(newMe);


	    Logger.commonLog("ExternalChatListener", Logger.LOG_NORMAL, "LlmChatListener, execute -- response from OpenAI: " + response); 
	}


	public String sendToOpenAI(InputCoordinator source, String jsonPayload, Boolean fromAgent) {
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
		            Logger.commonLog("send to openai!!!", Logger.LOG_NORMAL, "LlmChatListener, execute -- response from OpenAI: " + responseText); 
		            
		            State s = State.copy(StateMemory.getSharedState(agent));
		            if  (responseText.contains("?") && !fromAgent) {
		    	        s.setGlobalActiveListener(this.myName);
		    	    } else {
		            	s.setGlobalActiveListener("");
		            }
		            StateMemory.commitSharedState(s, agent);
		            
		            if (this.inactivityPromptFlag) {
		            	resetInactivityTimer(source);
		            }
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
       
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "Failed to send message to OpenAI";
	    }
	    
	    
	}
	
	public void sendActivePromptToOpenAI(InputCoordinator source) {
	    // Prepare the prompt based on the received message
	    String prompt = this.inactivityPrompt;
	    String jsonPayload = constructPayloadWithHistory(source, prompt);
	    
	    // Sending the message to OpenAI and receiving the response
	    String response = sendToOpenAI(source, jsonPayload, true);
	    
        MessageEvent newMe = new MessageEvent(source, this.myName, response);
        source.pushEventProposal(newMe);
	}
	
	public void resetInactivityTimer(InputCoordinator source) {
        // Cancel any existing tasks
        inactivityTimer.cancel();
        inactivityTimer = new Timer(); // Re-instantiate to clear cancelled state
        System.err.println(this.getClass().getSimpleName() + "RESETTIING TIMER 1...");
        // Schedule a new task
        inactivityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendActivePromptToOpenAI(source);
                System.err.println(this.getClass().getSimpleName() + "TIMER 1 TRIGGERED!!!");
            }
        }, inactivityPeriod);
    }
	
	
	private String constructPayloadWithHistory(InputCoordinator source, String prompt) {
	    JSONObject payload = new JSONObject();
	    try {
			payload.put("model", this.modelName);
			payload.put("temperature", this.temperature);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    

	    JSONArray messages = new JSONArray();

	    // Add the fixed context as the first message
	    JSONObject fixedContextMessage = new JSONObject();
	    try {
			fixedContextMessage.put("role", "system");
			fixedContextMessage.put("content", this.context);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    messages.put(fixedContextMessage);
	    
	 // find chathistorylistener
 		try {
 			BasilicaListener CHL = source.getListenerByName("ChatHistoryListener");
		    JSONArray chatHistory = ((ChatHistoryListener) CHL).retrieveChatHistory(this.contextLen);
		    for (int i = 0; i < chatHistory.length(); i++) {
	            JSONObject originalMessage = chatHistory.getJSONObject(i);
	            JSONObject reformattedMessage = new JSONObject();

	            // Determine the role based on the "sender" field
	            String role = "user"; // Default role
	            if (originalMessage.getString("sender").equals(this.myName)) {
	                role = "assistant"; // If the sender is SnowBot, set role to assistant
	            }

	            // Copy the "content" field directly
	            String content = originalMessage.getString("content");

	            // Construct the new message objectß
	            reformattedMessage.put("role", role);
	            reformattedMessage.put("content", content);

	            // Add the reformatted message to the new JSONArray
	            messages.put(reformattedMessage);
	        }

		    System.err.println("Loaded chatHisory: " + chatHistory.toString());
 		} catch(Exception e) {};
	    // Add the current prompt as the last message
	    JSONObject promptMessage = new JSONObject();
	    try {
			promptMessage.put("role", "user");
			promptMessage.put("content", prompt);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	    messages.put(promptMessage);

	    try {
			payload.put("messages", messages);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	    return payload.toString();
	}

	
	
	@Override
	public void processEvent(InputCoordinator source, Event e) {
		// TODO Auto-generated method stub
//		if (e instanceof BotMessageEvent) {
//			BotMessageEvent bme = (BotMessageEvent)e;
//			System.err.println(bme.getSender() + ": " + bme.getText());
//			if (inactivityPromptFlag) {
//				resetInactivityTimer(source);
//				System.err.println("PROCESS_EVENT:::" + this.getClass().getSimpleName() + ":::reset timer for bot message");
//			}
//		}
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
//		return new Class[]{BotMessageEvent.class};
	}
}