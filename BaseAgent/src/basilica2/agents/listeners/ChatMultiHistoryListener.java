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
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.MultiModalFilter;
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


public class ChatMultiHistoryListener extends BasilicaAdapter
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
    private String privateUsernamePrefix = "Private_";
    private String cameraUsernamePrefix = "Camera_";
    private Boolean includeImages = false;
 

	
	public ChatMultiHistoryListener(Agent a)
	{
		super(a);
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		System.err.println(this.getClass().getSimpleName());
		path = properties.getProperty("path","./chat_history/ChatMultiHistory.json");
		privateUsernamePrefix = properties.getProperty("private-username-prefix",privateUsernamePrefix);
		cameraUsernamePrefix = properties.getProperty("camera-username-prefix",cameraUsernamePrefix);

        // Create the file and its directory structure if they do not exist
        createFileIfNotExists(path);
        readAndSetSessionId();
//        inactivityTimer = new Timer();
        inactivityPeriod = Long.parseLong(properties.getProperty("timeout")) * 1000;
        inactivityTimerFlag = Boolean.parseBoolean(properties.getProperty("timeout_flag"));
        includeImages = Boolean.parseBoolean(properties.getProperty("include_images"));            
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
		} else if (e instanceof PrivateMessageEvent) {

			try {
				handlePrivateMessageEvent(source, (PrivateMessageEvent) e);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}	
		} 
	}
	
	
	public void handleMessageEvent(InputCoordinator source, MessageEvent me) throws JSONException {
		String sender = me.getFrom();
		String receiver; 
		if (sender.startsWith(privateUsernamePrefix)) {
			receiver = agent.getName();
		} else {
			receiver = "group";
		}
		String content = me.getText();
		saveMessageToHistory(sender, receiver, content);
	    System.out.println("ChatHistoryMultiListener handleMessageEvent -- sender=" + sender + "  -- receiver=" + receiver + "  --  message: " + me.getText()); 
	}
	
	
	public void handlePrivateMessageEvent(InputCoordinator source, PrivateMessageEvent pme) throws JSONException {
		String sender = pme.getFrom();
		String receiver = pme.getDestinationUser();
		String content = pme.getText();
//		resetInactivityTimer(source);
//		updateLastSenders(sender);
		saveMessageToHistory(sender, receiver, content);
	    System.out.println("ChatHistoryMultiListener handlePrivateMessageEvent -- sender=" + sender + "  -- receiver=" + receiver + "  --  message: " + pme.getText()); 
	}
	
	
	public void handleBotMessageEvent(InputCoordinator source, BotMessageEvent bme) throws JSONException {
		String sender = bme.getFrom();
		String receiver = "???";
		String content = bme.getText();
//		resetInactivityTimer(source);
//		updateLastSenders(sender);
		saveMessageToHistory(sender, receiver, content);
	    System.out.println("ChatHistoryMultiListener handleBotMessageEvent -- sender=" + sender + "  -- receiver=" + receiver + "  --  message: " + bme.getText()); 
	}

	public synchronized void saveMessageToHistory(String sender, String receiver, String content) {
	    if (!includeImages) {
	    	replaceTagValueInMultimodalContent(content,"image","<redacted>");
	    }
	    JSONObject messageJson = new JSONObject();
	    try {
	    	messageJson.put("session_id", this.sessionID);
			messageJson.put("sender", sender);
			messageJson.put("receiver", receiver);
			
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
	
	private String replaceTagValueInMultimodalContent(String content, String tag, String replacement) {
		StringBuilder sb = new StringBuilder(content);
		
//		*** Responses from LLM have multimodal format but may not have "multimodal:::true".  		
//		int testMM = sb.indexOf("multimodal:::true");
//		if (testMM < 0) {
//			return content; 
//		}
		String searchString = tag + MultiModalFilter.withinModeDelim;  
        int tagStart = sb.indexOf(searchString);
		if (tagStart < 0) {
			return content; 
		}
        tagStart = tagStart + searchString.length(); 
        int tagEnd = sb.indexOf(MultiModalFilter.multiModalDelim,tagStart); 
        if (tagEnd < 1) {
        	tagEnd = sb.length();
        }
        sb = sb.replace(tagStart, tagEnd, replacement); 
        System.out.println("\n\n*** replaceTagValueInMultimodalContent returning***:\n" + sb.toString() + "\n\n");
        return sb.toString(); 
	}

	public JSONArray retrieveChatHistory(int numberOfMessages, String target) {
		System.out.println("ChatMultiHistory, retrieveChatHistory -- target: " + target); 
        List<String> targetLines = new ArrayList<>();
		JSONArray messages = new JSONArray();
		
	    try {
	        // Read all lines from the file into a list
	        List<String> lines = Files.readAllLines(Paths.get(path));

	        lines.forEach(line -> {
				try {
					JSONObject me = new JSONObject(line);
					if (me.has("session_id") && me.getInt("session_id") == this.sessionID) {
						if (target.equals("public")) {
							if (me.has("sender") && me.getString("sender").startsWith(privateUsernamePrefix)) {
								return;
							} else if (me.has("receiver") && me.getString("receiver").startsWith(privateUsernamePrefix)) {
								return;
							} else if (me.has("sender") && me.getString("sender").startsWith(cameraUsernamePrefix)) {
								return;
							} else if (me.has("receiver") && me.getString("receiver").startsWith(cameraUsernamePrefix)) {
								return;
							} else {
								targetLines.add(line);
							}
						} else if (me.has("sender") && target.equals(me.getString("sender"))) {
							targetLines.add(line);
						} else if (me.has("receiver") && target.equals(me.getString("receiver"))) {
							targetLines.add(line);
						} 
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
	        
	        // Get the last N lines from the list
	        int n = Math.max(0, numberOfMessages);
	        int start = Math.max(0, targetLines.size() - n);
	        List<String> lastNLines = targetLines.subList(start, targetLines.size());

	        // Convert each line into a JSON object and add it to the JSONArray
	        lastNLines.forEach(line -> {
				try {
					JSONObject me = new JSONObject(line);
					messages.put(me);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
	    } catch (IOException e) {
	        Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Error reading from chat history file: " + e.getMessage());
	    }
	    System.out.println("ChatMultiHistoryListener retrieved chat history: " + messages.toString());
	    return messages;
	}

	public void sendActiveRequest(InputCoordinator source) {
		LlmChatListener sender = getNextSenderPreprocessor(source);
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
        List<BasilicaPreProcessor> llmSenders = source.getAllPreProcessorsContains("LlmChatListener");
        
        for (BasilicaPreProcessor preProcessor : llmSenders) {
            // Assuming there's a way to get a meaningful preprocessor name or using the class name as fallback
            String preProcessorName = preProcessor.getClass().getSimpleName();

            // Safely cast to LlmChatListener and retrieve the listener name
            // Note: This cast assumes all preProcessors in llmSenders are indeed instances of LlmChatListener
            String listenerName = "";
            if (preProcessor instanceof LlmChatListener) {
                listenerName = ((LlmChatListener) preProcessor).myName;
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
    
    public LlmChatListener getNextSenderPreprocessor(InputCoordinator source) {
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
    	return (LlmChatListener)(source.getPreProcessor(preprocessor));
    }

	@Override
	public void processEvent(InputCoordinator source, Event e) {
		System.err.println("ChatHistoryMultiListener: enter processEvent ");
		if (listenerSenderCount == -1) {
			getLlmListeners(source);
		}
		System.out.println("ChatHistoryMultiListener, processEvent: got LlmListeners " + Integer.toString(listenerSenderCount));
		if (e instanceof BotMessageEvent) {
				
	//			handleMessageEvent(source, (BotMessageEvent) e);
			BotMessageEvent bm = (BotMessageEvent) e;
			try {
				handleBotMessageEvent(source, (BotMessageEvent) e);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Logger.commonLog("ChatHistoryMultiListener", Logger.LOG_NORMAL, "ChatHistoryMultiListener got BotMessageEvent " + bm.getText()); 
			System.err.println("ChatHistoryMultiListener got BotMessageEvent " + bm.getText());			
		}
		
	}	
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class, PrivateMessageEvent.class};
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return new Class[]{BotMessageEvent.class};
	}
}