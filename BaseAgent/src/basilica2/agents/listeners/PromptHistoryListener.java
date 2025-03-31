package basilica2.agents.listeners;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;


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


public class PromptHistoryListener extends BasilicaAdapter
{
	public String host;
	public String port; 
	public String path;
	public String charset;
	public String delimiter;
	public String start_flag;
	private int sessionID;
	private Timer inactivityTimer;
	private long inactivityPeriod;
	private Boolean inactivityTimerFlag = false;
	
	// listener here refers to myName, Preprocessor is the class name
	private List<String> listenerOrder = new ArrayList<>(); // Maintains the turn order of listeners by name
    private Map<String, String> listenerToPreprocessorMap = new LinkedHashMap<>(); // Maps listener names to preprocessor names
    private String lastListenerSender = null;
    private String lastSender = null;
    private int listenerSenderCount = -1;

	
	public PromptHistoryListener(Agent a)
	{
		super(a);
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		System.err.println(this.getClass().getSimpleName());
		path = properties.getProperty("path","./chat_history/ChatHistory.json");
		

        // Create the file and its directory structure if they do not exist
        createFileIfNotExists(path);
        readAndSetSessionId();
        inactivityTimer = new Timer();
        inactivityPeriod = Long.parseLong(properties.getProperty("timeout")) * 1000;
        inactivityTimerFlag = Boolean.parseBoolean(properties.getProperty("timeout_flag"));
        
        
        
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
		if (listenerSenderCount == -1) {
			getLlmListeners(source);
		}
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
//		if (content.equals("END TIMER")) {
//			inactivityTimerFlag = false;
//		} else if (content.equals("START TIMER")) {
//			inactivityTimerFlag = true;
//		}
		resetInactivityTimer(source);
		updateLastSenders(sender);
		saveMessageToHistory(sender, content);
	    Logger.commonLog("PromptHistoryListener", Logger.LOG_NORMAL, "PromptHistoryListener saved for " + sender + ": " + me.getText()); 
	}
	
	public void handleBotMessageEvent(InputCoordinator source, BotMessageEvent e) throws JSONException {
		String sender = e.getFrom();
		String content = e.getText();
		resetInactivityTimer(source);
		updateLastSenders(sender);
		saveMessageToHistory(sender, content);
		
	    Logger.commonLog("PromptHistoryListener", Logger.LOG_NORMAL, "PromptHistoryListener heard from : " + content); 
	}

	public synchronized void saveMessageToHistory(String sender, String content) {
	    JSONObject messageJson = new JSONObject();
	    try {
	    	messageJson.put("session_id", this.sessionID);
	    	
			messageJson.put("sender", sender);
			
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String formattedDate = dateFormat.format(System.currentTimeMillis());
		    messageJson.put("timestamp", formattedDate);
		    
		    messageJson.put("content", content);
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
		
//		System.err.println("\nPromptHistoryListener, numberOfMessages: " +  String.valueOf(numberOfMessages));
		
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
//	    System.err.println("PromptHisoryListener retrieved chat history: " + messages.toString());
	    return messages;
	}

	public void sendActiveRequest(InputCoordinator source) {
		LlmPromptListener sender = getNextSenderPreprocessor(source);
		if (sender != null) {
			sender.sendActivePromptToOpenAI(source);
		}
	}
	
	public void resetInactivityTimer(InputCoordinator source) {
    // Cancel any existing tasks
    inactivityTimer.cancel();
    inactivityTimer = new Timer(); // Re-instantiate to clear cancelled state
    if (!inactivityTimerFlag) {
		return;
	}
    System.err.println(this.getClass().getSimpleName() + " RESETTIING TIMER...");
    // Schedule a new task
    inactivityTimer.schedule(new TimerTask() {
        @Override
        public void run() {
            sendActiveRequest(source);
            System.err.println(this.getClass().getSimpleName() + " TIMER TRIGGERED!!!");
        }
    }, inactivityPeriod);
}


    public void getLlmListeners(InputCoordinator source) {
    	listenerSenderCount = 0;
        List<BasilicaPreProcessor> llmSenders = source.getAllPreProcessorsContains("LlmPromptListener");
        
        for (BasilicaPreProcessor preProcessor : llmSenders) {
            // Assuming there's a way to get a meaningful preprocessor name or using the class name as fallback
            String preProcessorName = preProcessor.getClass().getSimpleName();

            // Safely cast to LlmPromptListener and retrieve the listener name
            // Note: This cast assumes all preProcessors in llmSenders are indeed instances of LlmPromptListener
            String listenerName = "";
            if (preProcessor instanceof LlmPromptListener) {
                listenerName = ((LlmPromptListener) preProcessor).myName;
                // Store both names in the map
                listenerToPreprocessorMap.put(listenerName, preProcessorName);
                listenerOrder.add(listenerName);
                listenerSenderCount++;
            }
        }
        
        for (Map.Entry<String, String> entry : listenerToPreprocessorMap.entrySet()) {
            System.err.println("ListenerName:: " + entry.getKey() + "PreProcessor::  " + entry.getValue());
        }
    }
    
    public synchronized void updateLastSenders(String from) {
    	lastSender = from;
    	if (listenerToPreprocessorMap.containsKey(from)) {
    		lastListenerSender = from;
    	}
    }
    	
//        lastListenerSender = listenerOrder.indexOf(from);
    
    public LlmPromptListener getNextSenderPreprocessor(InputCoordinator source) {
    	if (listenerSenderCount == -1) {
    		getLlmListeners(source);
    	}
    	if (listenerOrder.size() == 0) {
    		return null;
    	}
    	if (!listenerOrder.contains(lastListenerSender)) {
    		return null;
    	}
    	String listener = lastListenerSender;
    	if (lastSender == lastListenerSender) { // if the llmListener just talked, let the next one talk
	    	int lastListenerSenderIndex = listenerOrder.indexOf(lastListenerSender);
	    	int newIdx = (lastListenerSenderIndex + 1) % listenerOrder.size();
	    	listener = listenerOrder.get(newIdx);
    	}
    	String preprocessor = listenerToPreprocessorMap.get(listener);
    	return (LlmPromptListener)(source.getPreProcessor(preprocessor));
    }

	@Override
	public void processEvent(InputCoordinator source, Event e) {
//		System.err.println("PromptHistoryListener: enter processEvent ");
		if (listenerSenderCount == -1) {
			getLlmListeners(source);
		}
//		System.err.println("PromptHistoryListener: got LlmListeners " + Integer.toString(listenerSenderCount));
		if (e instanceof BotMessageEvent) {
				
	//			handleMessageEvent(source, (BotMessageEvent) e);
			BotMessageEvent bm = (BotMessageEvent) e;
			try {
				handleBotMessageEvent(source, (BotMessageEvent) e);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Logger.commonLog("PromptHistoryListener", Logger.LOG_NORMAL, "PromptHistoryListener got BotMessageEvent " + bm.getText()); 
			System.err.println("PromptHistoryListener got BotMessageEvent " + bm.getText());
				
			
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