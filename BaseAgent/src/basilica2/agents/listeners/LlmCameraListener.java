package basilica2.agents.listeners;

import java.io.IOException;

import java.util.Properties;
import javax.net.ssl.SSLContext;

import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.ImageEvent;
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
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.time.Instant;
import java.time.Duration;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class LlmCameraListener extends LlmChatListener
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
    private String cameraUsernamePrefix = "Camera_";
    private String privateUsernamePrefix = "Private_";
    private Boolean privateMessaging = true; 
    private String privateServer = "https://bazaar.lti.cs.cmu.edu"; 
    private String urlPrefix = "/bazaar/chat/";
    private String htmlPage = "private_space";
    // Username under which tab-share-chat.html itself is loaded (its own
    // id/user URL path segments, e.g. ".../group/group/..."), so we know who
    // to send tab-relabeling updates to. See sendTabShareUserUpdate.
    private String tabShareUsername = "group";
    private String cameraUrl = "https://tinyurl.com/bazaarcam1";
    private int shrinkImagePercent = 50; 
    public  List<String> topics;
    private Instant start = Instant.now();
    private Instant finish;
    private volatile double threshold = 0.05;

    // Tracks presence-related bookkeeping per userName, in the order each
    // userName was first seen (LinkedHashMap preserves insertion order).
    // Keyed by userName so lookups by userName (see getUserNum below) are
    // O(1). All reads and writes of this map, of nextUserNum, and of any
    // individual UserPresenceInfo's sendMessage flag MUST go through a
    // synchronized(presenceLock) block -- see presenceLock below -- so do
    // not touch these fields directly from new code; go through
    // consumeSendMessageFlag / getUserNum / setSendMessage instead.
    private final Map<String, UserPresenceInfo> userPresenceMap = new LinkedHashMap<String, UserPresenceInfo>();

    // Counter used to assign sequential userNum values, starting at 1, in
    // the order the first PresenceEvent for each userName is processed.
    // Only ever mutated while holding presenceLock.
    private int nextUserNum = 1;

    // Guards userPresenceMap, nextUserNum, and every UserPresenceInfo's
    // sendMessage flag. PresenceEvents (and any future code that inspects
    // or toggles sendMessage) may run concurrently on different threads;
    // without a shared lock, two threads could both see sendMessage==true
    // for the same brand-new userName and both send a welcome message, or
    // both assign the same nextUserNum to two different users.
    private final Object presenceLock = new Object();

    // Holder for the per-user presence bookkeeping described in
    // requirement 2. Every field is private -- nothing outside this class
    // (not even other methods of the enclosing LlmCameraListener) can read
    // or write userName/userNum/sendMessage directly; they must go through
    // getUserNum() / consumeSendMessage() / setSendMessage(boolean) below.
    // That removes the possibility of some future line of code doing
    // `info.sendMessage = ...` unsynchronized by accident -- it simply
    // won't compile. These instance methods do NOT synchronize themselves:
    // callers (LlmCameraListener.consumeSendMessageFlag/getUserNum/
    // setSendMessage) must call them only from within a
    // synchronized(presenceLock) block.
    private static class UserPresenceInfo {
        private final String userName;
        private final int userNum;
        private boolean sendMessage;

        private UserPresenceInfo(String userName, int userNum, boolean sendMessage) {
            this.userName = userName;
            this.userNum = userNum;
            this.sendMessage = sendMessage;
        }

        private int getUserNum() {
            return userNum;
        }

        /**
         * If sendMessage is currently true, flips it to false and returns
         * true (caller should send the message). Otherwise returns false.
         * Caller must hold presenceLock.
         */
        private boolean consumeSendMessage() {
            if (sendMessage) {
                sendMessage = false;
                return true;
            }
            return false;
        }

        /** Caller must hold presenceLock. */
        private void setSendMessage(boolean sendMessage) {
            this.sendMessage = sendMessage;
        }
    }

	public LlmCameraListener(Agent a)
	{
		super(a);
//		Properties api_key_prop = PropertiesLoader.loadProperties("apiKey.properties");
		
		Properties llm_prop = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		try {
			
			myName = llm_prop.getProperty("name");
			String[] topicList = properties.getProperty("topics", "").split("[\\s,]+");
			int topicIndex = 0;
	        for (String topic : topicList) {
	        	topicList[topicIndex] = topic.toLowerCase();
	        	topicIndex++;
	        }		
			topics = Arrays.asList(topicList);
			model = llm_prop.getProperty("model");
//			System.err.println(myName + " model: "+model);
			requestURL = llm_prop.getProperty(model+".request.url");
			apiKey = llm_prop.getProperty(model+".api.key");
			context = llm_prop.getProperty(model+".prompt.context");
			contextFlag = Boolean.parseBoolean(llm_prop.getProperty(model+".context.flag"));
			temperature = Double.valueOf(llm_prop.getProperty(model+".temperature"));
			cameraUsernamePrefix = llm_prop.getProperty("camera-username-prefix",cameraUsernamePrefix);
			privateUsernamePrefix = llm_prop.getProperty("private-username-prefix",privateUsernamePrefix);
			privateServer = llm_prop.getProperty("private-server",privateServer);
			urlPrefix = llm_prop.getProperty("url-prefix",urlPrefix);
			cameraUrl = llm_prop.getProperty("camera-url",cameraUrl);
			htmlPage = llm_prop.getProperty("html-page",htmlPage);
			tabShareUsername = llm_prop.getProperty("tab-share-username",tabShareUsername);
			shrinkImagePercent = Integer.parseInt(llm_prop.getProperty("shrink-image-percent","50"));
			
			threshold = Double.parseDouble(llm_prop.getProperty("threshold", "0.05"));
			privateMessaging = Boolean.parseBoolean(properties.getProperty("private-messaging", privateMessaging.toString()));
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
		if (e instanceof PrivateMessageEvent) {
//	        System.err.println("LlmCameraListener preProcessEvent for PrivateMessageEvent");
			finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			if (timeElapsed > 1500) {
				boolean proceed = messageFilter((PrivateMessageEvent) e);
				if (proceed) {
//			        System.err.println("LlmCameraListener preProcessEvent: calling handleMessageEvent");
					try {
						handleMessageEvent(source, (PrivateMessageEvent) e);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} 
				start = finish;
			}
		} else if (e instanceof MessageEvent) {
//	        System.err.println("LlmCameraListener preProcessEvent for MessageEvent");
			finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			if (timeElapsed > 1500) {
				boolean proceed = messageFilter((MessageEvent) e);
				if (proceed) {
//			        System.err.println("LlmCameraListener preProcessEvent: calling handleMessageEvent");
					try {
						handleMessageEvent(source, (MessageEvent) e);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				} 
				start = finish;
			}
		}
		else if (e instanceof ImageEvent)
		{
//		    System.err.println("LlmCameraListener preProcessEvent for ImageEvent");
		    ImageEvent ie = (ImageEvent) e;
//	        System.err.println("LlmCameraListener preProcessEvent: calling handleImageEvent");
			try {
				handleImageEvent(source, ie);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if (e instanceof PresenceEvent)
		{
//		    System.err.println("LlmCameraListener preProcessEvent for ImageEvent");
			PresenceEvent pe = (PresenceEvent) e;
//	        System.err.println("LlmCameraListener preProcessEvent: calling handlePresenceEvent");
			try {
				handlePresenceEvent(source, pe);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	public boolean messageFilter(MessageEvent e) {
		String messageText = e.getText();
		String globalActiveListenerName = StateMemory.getSharedState(agent).getGlobalActiveListener();
//        System.err.println("LlmCameraListener messageFilter -- this.myName: " + this.myName);
//        System.err.println("LlmCameraListener messageFilter -- globalActiveListenerName: " + globalActiveListenerName);
		if (globalActiveListenerName.equalsIgnoreCase(this.myName)) {
//	        System.err.println("LlmCameraListener messageFilter -- name match!");
			return true;
		} else if (globalActiveListenerName.equals("") && messageText.contains(this.myName)) {
//	        System.err.println("LlmCameraListener messageFilter -- name match!");
			return true;
		}
		List<String> topicWords = getTopicWords(messageText);
		if (!topicWords.isEmpty()) {
//			System.err.println("LlmCameraListener messageFilter -- topic match!");
			return true;
		} else {
//			System.err.println("LlmCameraListenerr messageFilter -- NO topic match");
			return false;
		}
	}
	
	public List<String> getTopicWords (String messageText) {
        List<String> foundWords = topics.stream()
                .filter(messageText::contains)
                .collect(Collectors.toList());
//        System.err.println("getTopicWords - messageText: " + messageText);
//        System.err.println("getTopicWords - found words: ");
//        for (String word : foundWords) {
//        	System.err.println("   " + word);
//        }
//        foundWords.forEach(System.err::println);
        return foundWords;				
	}
    
    // Private chat messages come from an HTML page indended for private use.
    // Usernames from that page have prefix 'Private_', followed by the userId 
    // from the corresponding non-private page. User names from the corresponding
    // camera HTML page have prefix 'Camera_', followed by the same userId. 
    // The userId value is sent to the LLM for both chat messages and camera images
    // so that the LLM can recognize that both the messages and the images come
    // from the same user.
    // But from collaborative HTML pages, the username will be something like an 
    // actual name. For those pages, we use the actual name so that the LLM doesn't 
    // confuse the private and collaborative contexts. 	
	public void handleMessageEvent(InputCoordinator source, MessageEvent me) throws JSONException {
	    // Prepare the prompt based on the received message
//        System.err.println("LlmCameraListener handleMessageEvent -- received MessageEvent");
	    String prompt = me.getText(); // student chat message
	    String sender = me.getFrom();
//	    String senderToLlm; 
//	    if (sender.startsWith(privateUsernamePrefix)) {
//	    	senderToLlm = sender.substring(privateUsernamePrefix.length()); 
//	    } else {
//	    	senderToLlm = sender;
//	    }
//	    String jsonPayload = constructPayloadMultiParty(source, prompt, senderToLlm);
//	    String jsonPayload = constructPayloadMultiParty(source, prompt, sender);
//	    openAIrequestAndResponse(source,jsonPayload,false,sender,senderToLlm);
	    openAIrequestAndResponse(source,prompt,false,sender);
	}
	
    // Private chat messages come from an HTML page indended for private use.
    // Usernames from that page have prefix 'Private_', followed by the userId 
    // from the corresponding non-private page. User names from the corresponding
    // camera HTML page have prefix 'Camera_', followed by the same userId. 
    // The userId value is sent to the LLM for both chat messages and camera images
    // so that the LLM can recognize that both the messages and the images come
    // from the same user.
    // But from collaborative HTML pages, the username will be something like an 
    // actual name. For those pages, we use the actual name so that the LLM doesn't 
    // confuse the private and collaborative contexts. 	
	public void handlePrivateMessageEvent(InputCoordinator source, PrivateMessageEvent pme) throws JSONException {
	    // Prepare the prompt based on the received message
//        System.out.println("LlmCameraListener handlePrivateMessageEvent -- enter");
	    String prompt = pme.getText(); // student chat message
//        System.out.println("LlmCameraListener handlePrivateMessageEvent -- prompt: " + prompt);
	    String receiver = pme.getDestinationUser(); 
//        System.out.println("LlmCameraListener handlePrivateMessageEvent -- toUser: " + receiver);
	    String sender = pme.getFrom();
//        System.out.println("LlmCameraListener handlePrivateMessageEvent -- sender: " + sender);
//	    String senderToLlm; 
//	    if (sender.startsWith(privateUsernamePrefix)) {
//	    	senderToLlm = sender.substring(privateUsernamePrefix.length()); 
//	    } else {
//	    	senderToLlm = sender;
//	    }
//	    String jsonPayload = constructPayloadMultiParty(source, prompt, senderToLlm);
//	    String jsonPayload = constructPayloadMultiParty(source, prompt, sender);
//	    openAIrequestAndResponse(source,jsonPayload,false,sender,senderToLlm);
	    openAIrequestAndResponse(source,prompt,false,sender);
	}


    // Images come from an HTML page that sends images from a user's private camera.
    // Usernames from that page have prefix 'Camera_', followed by the userId 
    // from the corresponding non-private page. User names from the corresponding
    // private HTML page have prefix 'Private_', followed by the same userId. 
    // The userId value is sent to the LLM for both chat messages and camera images
    // so that the LLM can recognize that both the messages and the images come
    // from the same user.
    // But from collaborative HTML pages, the username will be something like an 
    // actual name. For those pages, we use the actual name so that the LLM doesn't 
    // confuse the private and collaborative contexts. 	
	public void handleImageEvent(InputCoordinator source, ImageEvent ie) throws JSONException {
//        System.err.println("LlmCameraListener handleImageEvent -- received ImageEvent");
	    String prompt = "none";
	    String sender = ie.getSenderUsername();
	    String userId = sender.substring(cameraUsernamePrefix.length());
	    String imageBase64 = ie.getImageBase64();
	    String mimeType = ie.getMimeType();

	    // Track (create or update) the latest image received for this userId.
	    State s = State.copy(StateMemory.getSharedState(agent));
	    String previousImage = s.getCurrentImage(userId); // null if no prior image for this userId
	    s.setCurrentImage(userId, imageBase64);
	    s.setCurrentImageMimeType(userId, mimeType);
	    StateMemory.commitSharedState(s, agent);

	    // Only forward to the LLM if this userId's image changed significantly
	    // from the previous one we saw for that same userId.
	    boolean significantChange = true;
	    if (previousImage != null) {
	        try {
	            significantChange = !almostIdentical(imageBase64, previousImage);
	        } catch (IOException ex) {
	            System.err.println("LlmCameraListener handleImageEvent -- error comparing images for userId=" + userId);
	            ex.printStackTrace();
	        }
	    }

	    if (significantChange) {
//	        System.err.println("LlmCameraListener handleImageEvent -- image for userId=" + userId + " changed significantly; sending to LLM");
	        openAIrequestAndResponse(source,prompt,false,sender);
	        displayImageOnPrivatePage(source, userId, imageBase64, mimeType);
	    } else {
//	        System.err.println("LlmCameraListener handleImageEvent -- image for userId=" + userId + " is similar to previous image; not sending");
	    }
	}

    // Images come from an HTML page that sends images from a user's private camera.
    // Usernames from that page have prefix 'Camera_', followed by the userId 
    // from the corresponding non-private page. User names from the corresponding
    // private HTML page have prefix 'Private_', followed by the same userId. 
    // The userId value is sent to the LLM for both chat messages and camera images
    // so that the LLM can recognize that both the messages and the images come
    // from the same user.
    // But from collaborative HTML pages, the username will be something like an 
    // actual name. For those pages, we use the actual name so that the LLM doesn't 
    // confuse the private and collaborative contexts. 	
	public void handlePresenceEvent(InputCoordinator source, PresenceEvent pe) throws JSONException {
//      System.out.println("LlmCameraListener handlePresenceEvent -- received PresenceEvent");
//	    String prompt = "none";
//	    String sender = pe.getSenderUsername();
//	    String userId = sender.substring(cameraUsernamePrefix.length());
		String agentName = agent.getName();
		String userName = pe.getUsername();
		System.err.println("handlePresenceEvent  -- agent name=" + agentName + "  -- user name=" + userName);

		// Ignore presence events for this agent itself.
		if ((userName.equals(this.myName)) || (userName.startsWith(privateUsernamePrefix)) || (userName.startsWith(cameraUsernamePrefix))) {
			return;
		}
		

		// Automaticically look up/create this user's bookkeeping and, if
		// sendMessage is currently true, claim it (flip to false) so that a
		// concurrent PresenceEvent for the same userName can't also see
		// sendMessage==true and send a second, duplicate message. Also reports
		// whether this call is the one that just assigned userName its
		// userNum, so we can notify tab-share-chat.html exactly once, right
		// when that assignment happens.
		PresenceLookupResult lookup = consumeSendMessageFlagAndCheckNew(userName);

		if (lookup.isNewlyAssigned) {
			System.err.println("handlePresenceEvent, isNewlyAssigned -- lookup.userNum=" + lookup.userNum + "  -- user name=" + userName);
			sendTabShareUserUpdate(source, lookup.userNum, userName);
		}

		if (lookup.shouldSendWelcome) {
			String agentNamePrefix = this.myName + "_";
			String sessionId = agentName.substring(agentNamePrefix.length());
			String sessionIdLast3 = sessionId.substring(Math.max(0, sessionId.length() - 3));
			String userNum = String.valueOf(lookup.userNum);
			String privateName = privateUsernamePrefix + userNum;
			String url = privateServer + urlPrefix + sessionId + "/" + privateName + "/" + privateName + "/?" + "html=" + htmlPage; 
			String privateMessage = "Welcome, " + userName + "!" + "  \n\nOpen the following URL in a separate tab or window: " + url;
//			PrivateMessageEvent newPMe = new PrivateMessageEvent(source,userName,this.myName,privateMessage);
			MessageEvent newPMe1 = new MessageEvent(source, this.myName, privateMessage);
			source.pushEventProposal(newPMe1);
			String cameraMessage = userName + ", with your camera open URL\n" + cameraUrl + "\n\nand enter\nSession ID: " + sessionIdLast3 + "\nUser ID: " + userNum; 
			MessageEvent newPMe2 = new MessageEvent(source, this.myName, cameraMessage);
			source.addEventProposal(newPMe2);
		}
	}

	/**
	 * Thread-safe: looks up (or creates, on first sighting) the
	 * UserPresenceInfo for userName, and if its sendMessage flag is
	 * currently true, flips it to false and returns true (meaning the
	 * caller should send the welcome message). Returns false otherwise.
	 * The lookup/create/check/flip all happen under presenceLock as a
	 * single atomic step, which is what prevents two threads from both
	 * observing sendMessage==true for the same userName and both sending
	 * a message, and from assigning the same nextUserNum to two users.
	 */
	private boolean consumeSendMessageFlag(String userName) {
		synchronized (presenceLock) {
			UserPresenceInfo info = userPresenceMap.get(userName);
			if (info == null) {
				// First PresenceEvent seen for this userName: assign the next
				// sequential userNum and default sendMessage to true.
				info = new UserPresenceInfo(userName, nextUserNum, true);
				nextUserNum++;
				userPresenceMap.put(userName, info);
			}
			return info.consumeSendMessage();
		}
	}

	/**
	 * Combined result of looking up (and, on first sighting, creating) a
	 * userName's presence bookkeeping in one atomic step: its userNum,
	 * whether this particular call is the one that just assigned that
	 * userNum (i.e. userName had never been seen before), and whether the
	 * one-time welcome message should be sent now. See
	 * consumeSendMessageFlagAndCheckNew, which is the only place this is
	 * constructed.
	 */
	private static class PresenceLookupResult {
		private final int userNum;
		private final boolean isNewlyAssigned;
		private final boolean shouldSendWelcome;

		private PresenceLookupResult(int userNum, boolean isNewlyAssigned, boolean shouldSendWelcome) {
			this.userNum = userNum;
			this.isNewlyAssigned = isNewlyAssigned;
			this.shouldSendWelcome = shouldSendWelcome;
		}
	}

	/**
	 * Thread-safe: looks up (or creates, on first sighting) the
	 * UserPresenceInfo for userName, and atomically reports its userNum,
	 * whether this call is the one that just assigned that userNum, and
	 * whether the one-time welcome message should now be sent (same
	 * consume-and-flip semantics as consumeSendMessageFlag). Delegates to
	 * consumeSendMessageFlag and getUserNum -- both of which independently
	 * acquire presenceLock -- from within one more synchronized(presenceLock)
	 * block; Java's intrinsic locks are reentrant, so a single thread
	 * re-entering the same lock here is safe, and it's what keeps the
	 * "is this new" check atomic with the flag flip.
	 */
	private PresenceLookupResult consumeSendMessageFlagAndCheckNew(String userName) {
		synchronized (presenceLock) {
			boolean isNewlyAssigned = !userPresenceMap.containsKey(userName);
			boolean shouldSendWelcome = consumeSendMessageFlag(userName);
			int userNum = getUserNum(userName).intValue();
			return new PresenceLookupResult(userNum, isNewlyAssigned, shouldSendWelcome);
		}
	}

	/**
	 * Notifies the tab-share-chat.html page -- loaded as the
	 * tabShareUsername user (its own id/user URL path segments, e.g.
	 * ".../group/group/..."; see the "tab-share-username" property, default
	 * "group") -- that userNum has just been assigned to userName, so it can
	 * relabel the corresponding "Private_&lt;userNum&gt;" tab in place. Uses
	 * the same tagged, multimodal-delimited message scheme as
	 * displayImageOnPrivatePage / private_space.html's "cameraImageUpdate"
	 * tag: tab-share-chat.html recognizes the "tabUserUpdate:::true" tag on
	 * an incoming private message and updates its tab label instead of
	 * appending the message as a chat line.
	 */
	public void sendTabShareUserUpdate(InputCoordinator source, int userNum, String userName) {
		String taggedMessage =
			"tabUserUpdate" + MultiModalFilter.withinModeDelim + "true"
			+ MultiModalFilter.multiModalDelim
			+ "userNum" + MultiModalFilter.withinModeDelim + userNum
			+ MultiModalFilter.multiModalDelim
			+ "userName" + MultiModalFilter.withinModeDelim + userName;
		System.err.println("sendTabShareUserUpdate - sending message: " + taggedMessage);
		PrivateMessageEvent tabUpdatePme = new PrivateMessageEvent(source, tabShareUsername, this.myName, taggedMessage);
		source.pushEventProposal(tabUpdatePme);
	}

	/**
	 * Thread-safe lookup of the sequential userNum assigned to userName.
	 * Returns null if no PresenceEvent has been recorded yet for that
	 * userName (or if userName is this.myName, which is never tracked).
	 * Always go through this method (rather than caching/reading a
	 * UserPresenceInfo's fields directly) so reads are properly
	 * synchronized with concurrent PresenceEvents.
	 */
	public Integer getUserNum(String userName) {
		synchronized (presenceLock) {
			UserPresenceInfo info = userPresenceMap.get(userName);
			return (info != null) ? Integer.valueOf(info.getUserNum()) : null;
		}
	}

	/**
	 * Thread-safe setter for a tracked user's sendMessage flag, for future
	 * iterations that need to toggle it. Does nothing if userName hasn't
	 * been seen yet.
	 */
	public void setSendMessage(String userName, boolean sendMessage) {
		synchronized (presenceLock) {
			UserPresenceInfo info = userPresenceMap.get(userName);
			if (info != null) {
				info.setSendMessage(sendMessage);
			}
		}
	}

	/**
	 * Pushes the latest camera image for userId to that user's own private_space.html
	 * page (username Private_<userId>) so it can be displayed there. Reuses the
	 * existing private-message delivery channel (PrivateMessageEvent), tagging the
	 * message body with the same multimodal delimiter scheme
	 * (MultiModalFilter.withinModeDelim / multiModalDelim) already used elsewhere in
	 * this codebase for encoding structured, multi-field payloads inside chat messages.
	 * private_space.html recognizes the "cameraImageUpdate:::true" tag and renders the
	 * image instead of appending it as a chat message.
	 */
	public void displayImageOnPrivatePage(InputCoordinator source, String userId, String imageBase64, String mimeType) {
	    String privateTarget = privateUsernamePrefix + userId;

	    // Shrink the image by shrinkImagePercent before displaying it, falling back
	    // to the original (full-size) image if shrinking fails for any reason.
	    String displayImageBase64 = imageBase64;
	    try {
	        displayImageBase64 = shrinkImage(imageBase64, mimeType, shrinkImagePercent);
	    } catch (IOException ex) {
	        System.err.println("LlmCameraListener displayImageOnPrivatePage -- error shrinking image for userId=" + userId + "; displaying original image");
	        ex.printStackTrace();
	    }

	    String taggedMessage =
	        "cameraImageUpdate" + MultiModalFilter.withinModeDelim + "true"
	        + MultiModalFilter.multiModalDelim
	        + "mimeType" + MultiModalFilter.withinModeDelim + mimeType
	        + MultiModalFilter.multiModalDelim
	        + "image" + MultiModalFilter.withinModeDelim + displayImageBase64;
	    PrivateMessageEvent imagePme = new PrivateMessageEvent(source, privateTarget, this.myName, taggedMessage);
	    source.pushEventProposal(imagePme);
	}

	/**
	 * Decodes a base64-encoded image, scales it down so its resulting width/height
	 * are percent% of the original (e.g. percent=25 shrinks it to 1/4 its original
	 * width and height), re-encodes it in a format matching mimeType, and returns
	 * the result as a base64 string. percent is clamped to [0, 100]; a percent of
	 * 100 returns the image unshrunk (aside from a re-encode), and a percent of 0
	 * collapses it to a minimum 1x1 image.
	 */
	private static String shrinkImage(String base64, String mimeType, int percent) throws IOException {
	    byte[] bytes = Base64.getDecoder().decode(base64);
	    BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));
	    if (original == null) {
	        throw new IOException("Unable to decode image for shrinking");
	    }

	    double scale = Math.max(0, Math.min(100, percent)) / 100.0;
	    int newWidth = Math.max(1, (int) Math.round(original.getWidth() * scale));
	    int newHeight = Math.max(1, (int) Math.round(original.getHeight() * scale));

	    Image scaledInstance = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
	    BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
	    Graphics2D g2d = resized.createGraphics();
	    g2d.drawImage(scaledInstance, 0, 0, null);
	    g2d.dispose();

	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    ImageIO.write(resized, formatNameFromMimeType(mimeType), baos);
	    return Base64.getEncoder().encodeToString(baos.toByteArray());
	}

	/** Maps a MIME type like "image/jpeg" or "image/png" to an ImageIO format name. */
	private static String formatNameFromMimeType(String mimeType) {
	    if (mimeType == null) {
	        return "jpg";
	    }
	    int slashIndex = mimeType.indexOf('/');
	    String subtype = (slashIndex >= 0) ? mimeType.substring(slashIndex + 1) : mimeType;
	    if (subtype.equalsIgnoreCase("jpeg")) {
	        return "jpg";
	    }
	    return subtype;
	}

	/**
	 * Compares two base64-encoded JPEG images using a perceptual average-hash and
	 * reports whether they are "almost identical" -- i.e. their dissimilarity is at
	 * or below this listener's configured threshold (properties/LlmCameraListener.properties,
	 * key "threshold", default 0.05).
	 */
	public boolean almostIdentical(String base64Jpeg1, String base64Jpeg2) throws IOException {
    	if (base64Jpeg1 == null || base64Jpeg2 == null || base64Jpeg1.isEmpty() || base64Jpeg2.isEmpty()) {
    		return false;
    	}
        long hash1 = averageHash(decode(base64Jpeg1));
        long hash2 = averageHash(decode(base64Jpeg2));
        int hammingDistance = Long.bitCount(hash1 ^ hash2);
        double dissimilarity = hammingDistance / 64.0;
//        System.err.println("LlmCameraListener - latest image dissimilarity: " + String.valueOf(dissimilarity));
        return dissimilarity <= threshold;
    }

    private static BufferedImage decode(String base64) throws IOException {
        byte[] bytes = Base64.getDecoder().decode(base64);
        return ImageIO.read(new ByteArrayInputStream(bytes));
    }

    private static long averageHash(BufferedImage src) {
        Image scaled = src.getScaledInstance(8, 8, Image.SCALE_SMOOTH);
        BufferedImage small = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        small.getGraphics().drawImage(scaled, 0, 0, null);

        int[] lum = new int[64];
        long sum = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int rgb = small.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                int val = (r + g + b) / 3;
                lum[y * 8 + x] = val;
                sum += val;
            }
        }
        int avg = (int) (sum / 64);

        long hash = 0L;
        for (int i = 0; i < 64; i++) {
            if (lum[i] >= avg) hash |= (1L << i);
        }
        return hash;
    }
	
	public void openAIrequestAndResponse(InputCoordinator source, String prompt, Boolean fromSystem, String sender)  {
		String jsonPayload = constructPayloadMultiParty(source, prompt, sender);
//        System.err.println("LlmCameraListener openAIrequestAndResponse -- sending to LLM");
	    String response = sendToOpenAI(source, jsonPayload, false);
//        System.err.println("LlmCameraListener openAIrequestAndResponse -- OpenAI response: " + response);
        if (!"No response".equals(response)) {
        	
			if ((!sender.startsWith(privateUsernamePrefix)) && (!sender.startsWith(cameraUsernamePrefix))) {
		    	MessageEvent newMe = new MessageEvent(source, this.myName, response);
		    	source.pushEventProposal(newMe);
			} else {
			    String senderSuffix = "";
				if (sender.startsWith(privateUsernamePrefix)) {
			    	senderSuffix = sender.substring(privateUsernamePrefix.length()); 
			    } else if (sender.startsWith(cameraUsernamePrefix)) {
			    	senderSuffix = sender.substring(cameraUsernamePrefix.length()); 
			    }
				String privateStudentName = privateUsernamePrefix + senderSuffix;
	    		PrivateMessageEvent newPMe1 = new PrivateMessageEvent(source,privateStudentName,this.myName,response); 
	    		source.pushEventProposal(newPMe1); 
	    		String privateCameraName = cameraUsernamePrefix + senderSuffix;
	    		PrivateMessageEvent newPMe2 = new PrivateMessageEvent(source,privateCameraName,this.myName,response); 
	    		source.pushEventProposal(newPMe2); 
			}
	    } else {
	    	System.err.println("LlmCameraListener openAIrequestAndResponse: LLM returned 'No response'");
	    }
//	    Logger.commonLog("LlmCameraListener", Logger.LOG_NORMAL, "LlmCameraListener, openAIrequestAndResponse -- response from OpenAI: " + response); 	
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
//	        System.err.println("requestURL: " + requestURL);
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
//		        System.err.println("CONNECTION: " + responseCode);
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
//			        System.err.println("@@@@@@@@@raw response: " + response.toString());
			        // Parse the raw response into a JSONObject
			        JSONObject jsonResponse = new JSONObject(response.toString());
			        
			        JSONArray choices = jsonResponse.getJSONArray("choices");
			        JSONObject firstChoice = choices.getJSONObject(0);
			        JSONObject message = firstChoice.getJSONObject("message");
			        String contentString = message.getString("content");

			        // Step 3: the "content" field is itself a JSON string -> parse again
			        JSONObject content = new JSONObject(contentString);

			        // Step 4: pull out the two fields
			        String responseType = content.getString("response_type");
			        String reply = content.getString("reply");			        
			        
			        if ("No response".equals(responseType)) {
			        	return responseType;
			        } else {
			        	return reply; 
			        }
//			        return reply; 
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
//			                System.out.println("Prediction Result: " + prediction.toString());

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
//					System.err.println("111111@@@@@@@@ llama response: " + responseText);
		            
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
	
	
	public String getAllMessages(InputCoordinator source, String prompt, String promptSender) {
		String allMessages = "Conversation in the chatroom:\n\n";
		String target; 
	    if (promptSender.startsWith(privateUsernamePrefix)) {
	    	target = promptSender; 
	    } else {
	    	target = "public";
	    }
	    try {
 			BasilicaListener historyListener = source.getListenerByName("ChatMultiHistoryListener");
		    JSONArray chatHistory = ((ChatMultiHistoryListener) historyListener).retrieveChatHistory(this.contextLen,target);
		    for (int i = 0; i < chatHistory.length(); i++) {
	            JSONObject originalMessage = chatHistory.getJSONObject(i);
	            String sender = originalMessage.getString("sender");
	            String receiver = originalMessage.getString("receiver");
	            String content = originalMessage.getString("content");
	            String currentMessage = "sender:" + sender + "  receiver:" + receiver + "  content: " + content + "\n";
	            allMessages += currentMessage;
	        }

	    } catch(Exception e) {};
	    
	    if (prompt != null && target != null) {
	    	allMessages += promptSender + ": " + prompt + "\n";
	    }
	    return allMessages;
	}
	
	public String constructPayloadMultiParty(InputCoordinator source, String prompt, String promptSender) {
		JSONObject payload = new JSONObject();
		Boolean sendImage = true;

		// Send image only for Private_ and Camera_ users
		if (promptSender.startsWith(privateUsernamePrefix)) {
			sendImage = true;
		} else if (promptSender.startsWith(cameraUsernamePrefix)) {
			sendImage = true;
		} else {
			sendImage = false;
		}

		String userId = null;
		if (promptSender.startsWith(privateUsernamePrefix)) {
			userId = promptSender.substring(privateUsernamePrefix.length());
		} else if (promptSender.startsWith(cameraUsernamePrefix)) {
			userId = promptSender.substring(cameraUsernamePrefix.length());
		}


		if (model.equals("openai")) {
			
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
		    
		    JSONObject allPromptMessage = new JSONObject();
		    
		    String target;
		    if (promptSender.startsWith(privateUsernamePrefix)) {
		    	target = promptSender; 
		    } else if (promptSender.startsWith(cameraUsernamePrefix)) {
		    	target = privateUsernamePrefix + promptSender.substring(cameraUsernamePrefix.length()); 
		    } else {
		    	target = promptSender; 
		    }
		    
		    String allMessages = getAllMessages(source, prompt, target);
	    
		    try {
				allPromptMessage.put("role", "user");

			    // Vision payload: content is an array of image + text parts
			    JSONArray contentParts = new JSONArray();

				// Look up the latest image (if any) received for this particular userId,
				// rather than a single shared "most recent image across all users".
				String currentImage = null;
				String currentImageMimeType = null;
				if (userId != null) {
				    State s = StateMemory.getSharedState(agent);
				    currentImage = s.getCurrentImage(userId);
				    currentImageMimeType = s.getCurrentImageMimeType(userId);
				}
				if ((currentImage != null) && (sendImage == true)) {
				    JSONObject imagePart = new JSONObject();
				    imagePart.put("type", "image_url");
				    JSONObject imageUrl = new JSONObject();
				    imageUrl.put("url", "data:" + currentImageMimeType + ";base64," + currentImage);
				    imagePart.put("image_url", imageUrl);
				    contentParts.put(imagePart);	    

				    JSONObject textPart = new JSONObject();
				    textPart.put("type", "text");
				    textPart.put("text", allMessages);
				    contentParts.put(textPart);

				    allPromptMessage.put("content", contentParts);
//				    System.out.println("LlmCameraListener: sending message with image frame attached");
				} else {
				    // No image yet – plain text as before
				    allPromptMessage.put("content", allMessages);
				}

				messages.put(allPromptMessage);
				payload.put("messages", messages);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			    
		} else if (model.equals("llama2")) {
			
			JSONObject input = new JSONObject();
			
			try {
				String allMessages = getAllMessages(source, prompt, promptSender);
				input.put("max_new_tokens", 500);
				input.put("top_p", 1);
				input.put("top_k", 0);
				input.put("temperature", 0.7);
				input.put("prompt", allMessages);
				input.put("system_prompt", this.context);
				input.put("prompt_template", "<s>[INST] <<SYS>>\\n{system_prompt}\\n<</SYS>>\\n\\n{prompt} [/INST]");
				input.put("max_new_tokens", 256);
				input.put("min_new_tokens", 1);
				
//				payload.put("stream", true);
				payload.put("input", input);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		    
//		System.out.println(this.getClass().getSimpleName()+"GENERATED PAYLOAD@@@@");
//		System.out.println("LlmCameraListener constructPayloadMultiParty returning payload: " + payload.toString()); 
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
		return new Class[] {MessageEvent.class, PrivateMessageEvent.class, ImageEvent.class, PresenceEvent.class};
//		return new Class[] {MessageEvent.class, PrivateMessageEvent.class, ImageEvent.class};
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return null;
	}
}