package basilica2.agents.listeners;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FileReader;

import javax.net.ssl.SSLContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.BotMessageEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
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
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.time.Instant;
import java.time.Duration;
import java.util.Scanner;

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
	private String model;
	private String modelName;
	private String context;
	private double temperature;
    private boolean contextFlag;
    private int contextLen;
    public String myName;
    private Instant start = Instant.now();
    private Instant finish;
    private Boolean waitingForFirstEntry = true;
    private String instructionContent;
    private List<String> userNames = new ArrayList<>();
    private String roomName;
    private Integer roomNumber = 0;
    private List<String> apiKeys;
	public LlmChatListener(Agent a)
	{
		super(a);
//		Properties api_key_prop = PropertiesLoader.loadProperties("apiKey.properties");
		apiKeys = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("properties"+File.separator+"apiKeys.properties"))) {
            String line;
            while ((line = reader.readLine()) != null) {
            	apiKeys.add(line);
            }
            roomName = a.getRoomName().replaceAll("[^0-9]", "");
            System.err.println("roomName@@@@" + roomName);
            
            if (!roomName.isEmpty()) {
            	roomNumber = Integer.parseInt(roomName);
            }
            apiKey = apiKeys.get(roomNumber % apiKeys.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
//        apiKeys.forEach(System.out::println); 
		
		Properties llm_prop = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		try {
			myName = llm_prop.getProperty("name");
			model = llm_prop.getProperty("model");
//			System.err.println(myName + " model: "+model);
			requestURL = llm_prop.getProperty(model+".request.url");
//			apiKey = api_key_prop.getProperty(model+".api.key");
			context = llm_prop.getProperty(model+".prompt.context");
			contextFlag = Boolean.parseBoolean(llm_prop.getProperty(model+".context.flag"));
			temperature = Double.valueOf(llm_prop.getProperty(model+".temperature"));
			instructionContent = llm_prop.getProperty("room.instruction");// + "\n\nThe room number is: "+ roomNumber+"; the api key index is: " + roomNumber % apiKeys.size();
			if (contextFlag) {
				contextLen = Integer.parseInt(llm_prop.getProperty(model+".context.length"));
			}
			if (model.equals("openai")) {
				
				modelName = llm_prop.getProperty(model+".model.name");
				
				
				
			} else if (model.equals("llama2")) {
//				requestURL = requestURL + "/v1/";
//				System.err.println("URLLLLL: "+requestURL);
			}
			
		}
		catch (Exception e){}
		
		
		
		
	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent)
		{
			finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			if (timeElapsed > 1500) {
				boolean proceed = messageFilter((MessageEvent) e);
				boolean sendInstruction = instructionFilter((MessageEvent) e);
				if (sendInstruction) {
					sendInstruction(source);
				}else if (proceed) {
					try {
						handleMessageEvent(source, (MessageEvent) e);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} 
				start = finish;
			}
		} else if (e instanceof PresenceEvent) {
			handlePresenceEvent(source, (PresenceEvent) e);
		}
	}
	
	public void sendInstruction(InputCoordinator source) {
		MessageEvent newMe = new MessageEvent(source, "PromptBot", instructionContent);
		source.pushEventProposal(newMe);
	}
	
	public boolean instructionFilter(MessageEvent e) {
		String message = e.getText();
		return message.replaceAll("[^a-zA-Z]", "").trim().toLowerCase().equals("instruction");
	}
	public boolean messageFilter(MessageEvent e) {
		String message = e.getText();
		return message.startsWith("Prompty!");
	}
	
	public void handlePresenceEvent(InputCoordinator source, PresenceEvent pe) {
		String userName = pe.getUsername(); 
		Boolean isAgentName = source.isAgentName(userName);
		
		if (!isAgentName) {
			if (!userNames.contains(userName)) {
				userNames.add(userName);
				if (waitingForFirstEntry) {
					String welcomeMe = "Welcome " + userName + "! Here's the instruction for using the PromptBot.\n\n" + instructionContent +  "\n\nTo access this instruction again, type \"instruction\"."; 
					MessageEvent newMe = new MessageEvent(source, "PromptBot", welcomeMe);
					source.pushEventProposal(newMe);
	//					sendInstruction(source);
					waitingForFirstEntry = false;
				} else {
					String welcomeMe = "Welcome " + userName + "! Type \"instruction\" for details."; 
					MessageEvent newMe = new MessageEvent(source, "PromptBot", welcomeMe);
					source.pushEventProposal(newMe);
				}
			}
			
		}
		
		System.err.println("presence event!! userName: " + userName + ", isAgentName: " + isAgentName.toString());
	}
	
	public void handleMessageEvent(InputCoordinator source, MessageEvent me) throws JSONException {
        String prompt = me.getText().substring("Prompty!".length()).trim(); // Get the prompt after "Prompty!"
        String sender = me.getFrom();
        String jsonPayload = constructPayloadMultiParty(source, prompt, sender);

        String response = sendToOpenAI(source, jsonPayload, false);
        if (!response.isEmpty()) {
            MessageEvent newMe = new MessageEvent(source, this.myName, response);
            source.pushEventProposal(newMe);
        }

        Logger.commonLog("LlmChatListener", Logger.LOG_NORMAL, "LlmChatListener, execute -- response from OpenAI: " + response);
    }


	public String sendToOpenAI(InputCoordinator source, String jsonPayload, Boolean fromSystem) {
	    String apiKey = this.apiKey;
	    String requestURL = this.requestURL;
	    try {
	        
	    	// update vvv // 
	        String sslType = "TLSv1.2"; 
	        SSLContext sslContext = SSLContext.getInstance(sslType); 
	        sslContext.init(null, null, null);
	        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
	    	// update ^^^ // 
	        System.err.println("requestURL: " + requestURL);
	        URL url = new URL(requestURL);
//	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
	        try {
		        conn.setRequestMethod("POST");
		        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
		        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
		        conn.setDoOutput(true);
		        conn.setDoInput(true);
		        	        
		        try(OutputStream os = conn.getOutputStream()) {
		            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
		            os.write(input, 0, input.length);           
		        }
		        
		        int responseCode = conn.getResponseCode();
		        System.err.println("CONNECTION: " + responseCode);
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
			        System.err.println("@@@@@@@@@raw response: " + response.toString());
			        // Parse the raw response into a JSONObject
			        JSONObject jsonResponse = new JSONObject(response.toString());
			        String responseText;
			        if (this.model.equals("openai")) {
			        	//Extract the choices array from the response
				        JSONArray choices = jsonResponse.getJSONArray("choices");
				        System.out.println(response.toString());
				        // Check if there are choices available
				        if (choices.length() > 0) {
				            // Extract the text from the first choice
				            JSONObject responseMessage = choices.getJSONObject(0).getJSONObject("message");
				            responseText = responseMessage.getString("content");
				            System.out.println("Extracted Response Text: " + responseText);
				            
//				            State s = State.copy(StateMemory.getSharedState(agent));
//		                    s.setGlobalActiveListener("");
//		                    StateMemory.commitSharedState(s, agent);
				            return responseText;
				        }
//			        } else if (this.model.equals("llama2")) {
//			        	// Extract output array
//			            JSONArray outputArray = jsonResponse.getJSONArray("output");
//			            // Format output as a string
//			            StringBuilder formattedOutput = new StringBuilder();
//			            for (int i = 0; i < outputArray.length(); i++) {
//			                formattedOutput.append(outputArray.getString(i));
//			                if (i < outputArray.length() - 1) {
//			                    formattedOutput.append(" ");
//			                }
//			            }
//
//			            String[] parseResponse = formattedOutput.toString().split(": ");
//			            responseText = parseResponse[parseResponse.length - 1];
//			            System.out.println("Extracted Response Text: " + responseText);
//			            
//			            State s = State.copy(StateMemory.getSharedState(agent));
//		            	if  (responseText.contains("?")) {
//			    	        s.setGlobalActiveListener(this.myName);
//			    	    } else {
//			            	s.setGlobalActiveListener("");
//			            }
//			            StateMemory.commitSharedState(s, agent);
//			            return responseText;
			        }
			        return "";
		        } 
		        else if (responseCode == HttpURLConnection.HTTP_CREATED) {
		            // Read input stream
		            StringBuilder response = new StringBuilder();
		            try (BufferedReader reader = new BufferedReader(
		                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
		                String line;
		                while ((line = reader.readLine()) != null) {
		                    response.append(line.trim());
		                }
		            }
		            JSONObject jsonResponse = new JSONObject(response.toString());
		          
		            String predictionUrlString = jsonResponse.getJSONObject("urls").getString("get");
		            String responseText = "";
		            try {
		            	String predsslType = "TLSv1.2"; 
		    	        SSLContext predsslContext = SSLContext.getInstance(predsslType); 
		    	        predsslContext.init(null, null, null);
		    	        HttpsURLConnection.setDefaultSSLSocketFactory(predsslContext.getSocketFactory());
		                // Create a URL object from the prediction URL
		                URL predictionUrl = new URL(predictionUrlString);
		                
		                
		                // Polling interval in milliseconds
		                long pollingInterval = 1000; // 1 second

		                // Poll until prediction is complete
		                boolean predictionComplete = false;
		                while (!predictionComplete) {
			                // Open a connection to the URL
			                HttpURLConnection predictionConn = (HttpURLConnection) predictionUrl.openConnection();
	
			                // Set request method
			                predictionConn.setRequestMethod("GET");
			                predictionConn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			                predictionConn.setRequestProperty("Authorization", "Bearer " + apiKey);
			                // Read the response
			                Scanner scanner = new Scanner(predictionConn.getInputStream());
			                StringBuilder prediction = new StringBuilder();
			                while (scanner.hasNextLine()) {
			                	prediction.append(scanner.nextLine());
			                }
			                scanner.close();
			                JSONObject responseJson = new JSONObject(prediction.toString());
			                // Check if prediction is complete
			                String status = responseJson.getString("status");
			                if (status.equals("succeeded")) {
			                    predictionComplete = true;
			                    JSONArray outputArray = responseJson.getJSONArray("output");
					            // Format output as a string
					            StringBuilder formattedOutput = new StringBuilder();
					            for (int i = 0; i < outputArray.length(); i++) {
					                formattedOutput.append(outputArray.getString(i));
//					                if (i < outputArray.length() - 1) {
//					                    formattedOutput.append(" ");
//					                }
					            }
		
					            String[] parseResponse = formattedOutput.toString().split(": ");
					            responseText = parseResponse[parseResponse.length - 1];
					            System.out.println("Extracted Response Text: " + responseText);
					            
					            State s = State.copy(StateMemory.getSharedState(agent));
				            	if  (responseText.contains("?")) {
					    	        s.setGlobalActiveListener(this.myName);
					    	    } else {
					            	s.setGlobalActiveListener("");
					            }
					            StateMemory.commitSharedState(s, agent);
					            return responseText;
			                } 
	
			                // Print the prediction result
			                System.out.println("Prediction Result: " + prediction.toString());

			                // Close the connection
			                predictionConn.disconnect();
			                responseText = prediction.toString();
			             // Close the connection
			                conn.disconnect();
	
			                // Sleep for polling interval
			                Thread.sleep(pollingInterval);
		                }
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
					System.err.println("111111@@@@@@@@ llama response: " + responseText);
		            
		            // Parse the response, if needed
		            // Depending on the API, you might need to extract data from the response body
		            
		            // Handle the response data accordingly
		            // Example: Parse JSON response and extract necessary information
		            
		            // Return appropriate result or data
		            return responseText;
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
		            return "";
		        }
	        } finally {
                conn.disconnect(); // Ensure the connection is closed
            }
       
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "";
	    }
	    
	    
	}
	
	
	public JSONArray getAllMessages(InputCoordinator source, String prompt, String promptSender) {
		JSONArray messages = new JSONArray();
//	    try {
// 			BasilicaListener CHL = source.getListenerByName("ChatHistoryListener");
//		    JSONArray chatHistory = ((ChatHistoryListener) CHL).retrieveChatHistory(this.contextLen);
//		    for (int i = 0; i < chatHistory.length(); i++) {
//	            JSONObject originalMessage = chatHistory.getJSONObject(i);
////		            JSONObject reformattedMessage = new JSONObject();
//
//	            // Determine the role based on the "sender" field
//	             // Default role
//	            String content = originalMessage.getString("content");
//	            String sender = originalMessage.getString("sender");
//	            
//	            if ((content.startsWith("Prompty!") && !sender.equals(this.myName)) || sender.equals(this.myName)) {
//	            	JSONObject message = new JSONObject();
//	                message.put("role", sender.equals(this.myName) ? "assistant" : "user");
//	                message.put("content", content);
//	                messages.put(message);
//	            }
//	            
//	        }
//
//	    } catch (Exception e) {
//            e.printStackTrace();
//        }
//	    
	    if (prompt != null && promptSender != null) {
            JSONObject promptMessage = new JSONObject();
            try {
				promptMessage.put("role", "user");
				promptMessage.put("content", prompt);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            messages.put(promptMessage);
        }

        return messages;
	}
	
	public String constructPayloadMultiParty(InputCoordinator source, String prompt, String promptSender) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("model", this.modelName);
            payload.put("temperature", this.temperature);

            JSONArray messages = getAllMessages(source, prompt, promptSender);

            payload.put("messages", messages);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.err.println("INPUT PAYLOAD: " + payload.toString());
        return payload.toString();
	}


	
	
	@Override
	public void processEvent(InputCoordinator source, Event e) {
		// TODO Auto-generated method stub

	}	
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class, PresenceEvent.class};
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return null;
	}
}