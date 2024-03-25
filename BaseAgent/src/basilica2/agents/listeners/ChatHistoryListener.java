package basilica2.agents.listeners;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

import basilica2.agents.components.InputCoordinator;
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


import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.util.stream.Collectors;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;
import java.text.SimpleDateFormat;


public class ChatHistoryListener extends BasilicaAdapter
{
	public String host;
	public String port; 
	public String path;
	public String charset;
	public String delimiter;
	public String start_flag;
	private int sessionID;
	


	
	public ChatHistoryListener(Agent a)
	{
		super(a);
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		System.err.println(this.getClass().getSimpleName());
		path = properties.getProperty("path","./chat_hisotry/ChatHistory.json");

        // Create the file and its directory structure if they do not exist
        createFileIfNotExists(path);
        readAndSetSessionId();
    }
	
	private void readAndSetSessionId() {
	        try {
	            Path filePath = Paths.get(path);
	            if (Files.exists(filePath)) {
	                List<String> allLines = Files.readAllLines(filePath);
	                if (!allLines.isEmpty()) {
	                    // Check if the last line contains "session_id"
	                    String lastLine = allLines.get(allLines.size() - 1);
	                    if (lastLine.contains("session_id")) {
	                        JSONObject lastLineJson = new JSONObject(lastLine);
	                        // Assuming "session_id" is an integer
	                        this.sessionID = lastLineJson.getInt("session_id") + 1;
	                    } else {
	                        this.sessionID = 0; // Reset session_id to 0 if not found
	                    }
	                }
	            }
	        } catch (IOException e) {
	            System.err.println("An error occurred while reading the chat history file: " + e.getMessage());
	        } catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }


    private void createFileIfNotExists(String filePathStr) {
        try {
            Path filePath = Paths.get(filePathStr);
            // Ensure directory exists
            if (Files.notExists(filePath.getParent())) {
                Files.createDirectories(filePath.getParent());
            }
            // Create the file if it doesn't exist
            if (Files.notExists(filePath)) {
                Files.createFile(filePath);
                System.out.println("Created chat history file at: " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("An error occurred while creating the chat history file: " + e.getMessage());
        }
    }
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent) {

				try {
					handleMessageEvent(source, (MessageEvent) e);
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}	
		}
	}
	
	
	public void handleMessageEvent(InputCoordinator source, MessageEvent me) throws JSONException {
		String sender = me.getFrom();
		String content = me.getText();
		saveMessageToHistory(sender, content);
	    Logger.commonLog("chatHistoryListener", Logger.LOG_NORMAL, "chatHistoryListener saved for " + sender + ": " + me.getText()); 
	}
	
	public void handleBotMessageEvent(InputCoordinator source, BotMessageEvent e) throws JSONException {
		String sender = e.getFrom();
		String content = e.getText();
		saveMessageToHistory(sender, content);
		BasilicaPreProcessor lis1 = source.getPreProcessor("LlmChatListener");
		LlmChatListener LCL = (LlmChatListener) lis1;
		if (LCL != null) {
			LCL.resetInactivityTimer(source);
			System.err.println("CHAT HISOTRY LISTENER1::: reset timer 1");
		} else {
			System.err.println("CHAT HISOTRY LISTENER1::: NULL PTR");
		}
		
		BasilicaPreProcessor lis2 = source.getPreProcessor("LlmChatListener2");
		LlmChatListener2 LCL2 = (LlmChatListener2) lis2;
		if (LCL2 != null) {
			LCL2.resetInactivityTimer(source);
			System.err.println("CHAT HISOTRY LISTENER2::: reset timer 2");
		} else {
			System.err.println("CHAT HISOTRY LISTENER2::: NULL PTR");
		}
		
	    Logger.commonLog("chatHistoryListener", Logger.LOG_NORMAL, "chatHistoryListener heard from : " + content); 
	}

	public synchronized void saveMessageToHistory(String sender, String content) {
	    JSONObject messageJson = new JSONObject();
	    try {
	    	messageJson.put("session_id", this.sessionID);
			messageJson.put("sender", sender);
			messageJson.put("content", content);
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String formattedDate = dateFormat.format(System.currentTimeMillis());
		    messageJson.put("timestamp", formattedDate);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    

	    try {
	        // Save the JSON object to a file, each message on a new line
	        Files.write(Paths.get(path), (messageJson.toString() + "\n").getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	    } catch (IOException e) {
	        Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Error writing to chat history file: " + e.getMessage());
	    }
	}


	public JSONArray retrieveChatHistory(int numberOfMessages) {
		JSONArray messages = new JSONArray();
	    try {
	        // Read all lines from the file into a list
	        List<String> lines = Files.readAllLines(Paths.get(path));

	        // Get the last N lines from the list
	        int start = Math.max(0, lines.size() - numberOfMessages);
	        List<String> lastNLines = lines.subList(start, lines.size());

	        // Convert each line into a JSON object and add it to the JSONArray
	        lastNLines.forEach(line -> {
				try {
					JSONObject me = new JSONObject(line);
					if (me.has("session_id") && me.getInt("session_id") == this.sessionID) {
						messages.put(new JSONObject(line));
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
	    } catch (IOException e) {
	        Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Error reading from chat history file: " + e.getMessage());
	    }
//	    System.err.println("ChatHisoryListener retrieved chat history: " + messages.toString());
	    return messages;
	}


	
	@Override
	public void processEvent(InputCoordinator source, Event e) {
		if (e instanceof BotMessageEvent) {
				
	//			handleMessageEvent(source, (BotMessageEvent) e);
			BotMessageEvent bm = (BotMessageEvent) e;
			try {
				handleBotMessageEvent(source, (BotMessageEvent) e);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Logger.commonLog("chatHistoryListener", Logger.LOG_NORMAL, "chatHistoryListener got BotMessageEvent " + bm.getText()); 
			System.err.print("chatHistoryListener got BotMessageEvent " + bm.getText());
				
			
		}
		
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
		return new Class[]{BotMessageEvent.class};
	}
}