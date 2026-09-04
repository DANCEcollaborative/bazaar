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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

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
    private String botServer = "https://bazaar.lti.cs.cmu.edu"; 
    private String urlPrefix = "/bazaar/chat/";
    private String htmlPagePrivate = "private_space";
    private String htmlPageGroup = "tab-share-chat";
    // Username under which tab-share-chat.html itself is loaded (its own
    // id/user URL path segments, e.g. ".../group/group/..."), so we know who
    // to send tab-relabeling updates to. See sendTabShareUserUpdate.
    private String tabShareUsername = "tab_group";
    private String cameraUrl = "https://tinyurl.com/bazaarcam1";
    private int shrinkImagePercent = 50; 
    public  List<String> topics;
    private Instant start = Instant.now();
    private Instant finish;
    private volatile double imageDiffThreshold = 0.05;
    private int userPollRate = 60;
    // How many seconds after startup the periodic resyncTabShareUserUpdates()
    // polling (see tabShareUserNumWatcher below) is allowed to keep running
    // before it stops itself. Measured against userPollWatcherStart.
    private int userPollTimeout = 7800;
    // Timestamp the resyncTabShareUserUpdates() polling is timed against;
    // captured here (at field-initialization time, i.e. object construction)
    // rather than inside the constructor body so it reflects when this
    // listener actually came into existence.
    private final Instant userPollWatcherStart = Instant.now();

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


    // Volatile because it's written from
    // whatever event-processing thread called preProcessEvent and read from
    // the watcher thread.
    private volatile InputCoordinator source;

    // Every 10 seconds, unconditionally (re-)sends every known
    // userNum -> userName mapping to tab-share-chat.html via
    // sendTabShareUserUpdate -- a periodic full resync alongside the
    // synchronous notification already sent the moment a new userNum is
    // assigned (see the isNewlyAssigned branch in handlePresenceEvent), in
    // case that earlier notification never reached tab-share-chat.html.
    // That can happen more easily than it sounds: sendTabShareUserUpdate's
    // targeted PrivateMessageEvent is delivered by the chat server only to
    // whichever socket(s) are registered as tab_group in that room AT THAT
    // INSTANT (see server_lobby_https.js's 'sendpm' handler) -- if
    // tab-share-chat.html's tab hasn't connected yet, is mid-reconnect, or
    // the message is otherwise lost, the server just logs "did not emit"
    // and drops it silently; there is no ack back to this listener, so a
    // one-shot, only-resend-if-changed check could never tell the
    // difference between "already delivered" and "silently dropped".
    // Resending the full map every tick sidesteps that distinction
    // entirely: setPrivateTabUserName/renderTabLabel on the client side are
    // idempotent, so re-applying a mapping that already landed is harmless,
    // and it guarantees every mapping reaches tab-share-chat.html within 10
    // seconds of its tab actually being connected, regardless of why any
    // earlier attempt failed. A single-thread scheduled executor is used
    // rather than e.g. a raw Thread + sleep loop so the 10-second cadence is
    // handled by the JDK (drift-corrected, and safe to schedule/cancel), and
    // rather than a plain java.util.Timer so an uncaught exception from one
    // run (resyncTabShareUserUpdates already catches its own exceptions,
    // but this is extra insurance) can't silently kill future runs the way it
    // can with Timer. The thread is created as a daemon so it never keeps
    // the JVM alive on its own.
    private final ScheduledExecutorService tabShareUserNumWatcher =
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "LlmCameraListener-tabShareUserNumWatcher");
                t.setDaemon(true);
                return t;
            }
        });

    // Handle on the periodic resyncTabShareUserUpdates() task scheduled on
    // tabShareUserNumWatcher (see the constructor), so that task can cancel
    // its own future once userPollTimeout seconds have elapsed. Volatile
    // because it's written from the constructor's thread and read from
    // tabShareUserNumWatcher's background thread.
    private volatile ScheduledFuture<?> tabShareUserNumWatcherFuture;

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

        private String getUserName() {
            return userName;
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
			botServer = llm_prop.getProperty("bot-server",botServer);
			urlPrefix = llm_prop.getProperty("url-prefix",urlPrefix);
			cameraUrl = llm_prop.getProperty("camera-url",cameraUrl);
			htmlPagePrivate = llm_prop.getProperty("html-page-private",htmlPagePrivate);
			htmlPageGroup = llm_prop.getProperty("html-page-group",htmlPageGroup);
			tabShareUsername = llm_prop.getProperty("tab-share-username",tabShareUsername);
			shrinkImagePercent = Integer.parseInt(llm_prop.getProperty("shrink-image-percent","50"));
			userPollRate = Integer.parseInt(llm_prop.getProperty("user-poll-rate","60"));
			userPollTimeout = Integer.parseInt(llm_prop.getProperty("user-poll-timeout","7800"));
			
			imageDiffThreshold = Double.parseDouble(llm_prop.getProperty("image-diff-threshold", "0.05"));
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

		// Start the periodic (every userPollRate seconds) full resync of
		// every assigned userNum -> userName mapping to tab-share-chat.html.
		// Runs until userPollTimeout seconds have elapsed since
		// userPollWatcherStart, at which point the task cancels its own
		// future so resyncTabShareUserUpdates() stops being called. See
		// tabShareUserNumWatcher and resyncTabShareUserUpdates.
		tabShareUserNumWatcherFuture = tabShareUserNumWatcher.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				long secondsSinceStart = Duration.between(userPollWatcherStart, Instant.now()).getSeconds();
				if (secondsSinceStart >= userPollTimeout) {
					log(Logger.LOG_NORMAL, "LlmCameraListener - userPollTimeout (" + userPollTimeout +
							"s) reached; stopping resyncTabShareUserUpdates polling");
					ScheduledFuture<?> future = tabShareUserNumWatcherFuture;
					if (future != null) {
						future.cancel(false);
					}
					return;
				}
				resyncTabShareUserUpdates();
			}
		}, userPollRate, userPollRate, TimeUnit.SECONDS);
	}


	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		this.source = source;

		if (e instanceof PrivateMessageEvent) {
	        System.err.println("LlmCameraListener preProcessEvent for PrivateMessageEvent");
			finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			if (timeElapsed > 1500) {
				boolean proceed = messageFilter((PrivateMessageEvent) e);
				if (proceed) {
			        System.err.println("LlmCameraListener preProcessEvent: calling handleMessageEvent");
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
 	        System.err.println("LlmCameraListener preProcessEvent for MessageEvent");
			finish = Instant.now();
			long timeElapsed = Duration.between(start, finish).toMillis();
			if (timeElapsed > 1500) {
				boolean proceed = messageFilter((MessageEvent) e);
				if (proceed) {
			        System.err.println("LlmCameraListener preProcessEvent: calling handleMessageEvent");
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
		    System.err.println("LlmCameraListener preProcessEvent for ImageEvent");
		    ImageEvent ie = (ImageEvent) e;
	        System.err.println("LlmCameraListener preProcessEvent: calling handleImageEvent");
			try {
				handleImageEvent(source, ie);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else if (e instanceof PresenceEvent)
		{
		    System.err.println("LlmCameraListener preProcessEvent for PresenceEvent");
			PresenceEvent pe = (PresenceEvent) e;
	        System.err.println("LlmCameraListener preProcessEvent: calling handlePresenceEvent");
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
        System.err.println("LlmCameraListener messageFilter -- this.myName: " + this.myName);
        System.err.println("LlmCameraListener messageFilter -- globalActiveListenerName: " + globalActiveListenerName);
		if (globalActiveListenerName.equalsIgnoreCase(this.myName)) {
	        System.err.println("LlmCameraListener messageFilter -- name match!");
			return true;
		} else if (globalActiveListenerName.equals("") && messageText.contains(this.myName)) {
	        System.err.println("LlmCameraListener messageFilter -- name match!");
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
        System.err.println("LlmCameraListener handleMessageEvent -- received MessageEvent");
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
        System.out.println("LlmCameraListener handlePrivateMessageEvent -- enter");
	    String prompt = pme.getText(); // student chat message
        System.out.println("LlmCameraListener handlePrivateMessageEvent -- prompt: " + prompt);
	    String receiver = pme.getDestinationUser(); 
        System.out.println("LlmCameraListener handlePrivateMessageEvent -- toUser: " + receiver);
	    String sender = pme.getFrom();
        System.out.println("LlmCameraListener handlePrivateMessageEvent -- sender: " + sender);
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
        System.err.println("LlmCameraListener handleImageEvent -- received ImageEvent");
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
			System.err.println("LlmCameraListener handleImageEvent -- image for userId=" + userId + " changed significantly; sending to LLM");
			openAIrequestAndResponse(source,prompt,false,sender);
			displayImageOnPrivatePage(source, userId, imageBase64, mimeType);
	    } else {
	        System.err.println("LlmCameraListener handleImageEvent -- image for userId=" + userId + " is similar to previous image; not sending");
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
		System.out.println("LlmCameraListener handlePresenceEvent -- received PresenceEvent");
//	    String prompt = "none";
//	    String sender = pe.getSenderUsername();
//	    String userId = sender.substring(cameraUsernamePrefix.length());
		String agentName = agent.getName();
		String userName = pe.getUsername();
		System.err.println("handlePresenceEvent  -- agent name=" + agentName + "  -- user name=" + userName);

		// tab-share-chat.html itself connects/reconnects under
		// tabShareUsername. Rather than running it through the normal
		// per-student onboarding flow below (userNum assignment, welcome
		// message -- it isn't a student), treat a PRESENT event for it as a
		// cue to (re-)send every userNum -> userName mapping assigned so
		// far, so its tabs end up correctly labeled even if it just
		// loaded/reloaded and missed the individual tabUserUpdate messages
		// sent while it was away.
		if (userName.equals(tabShareUsername)) {
			System.err.println("handlePresenceEvent  -- userName =" + tabShareUsername);
//			if (PresenceEvent.PRESENT.equals(pe.getType())) {
//				Map<String, Integer> assignedUserNums = snapshotUserNums();
//				for (Map.Entry<String, Integer> entry : assignedUserNums.entrySet()) {
//					System.err.println("handlePresenceEvent - calling sendTabShareUserUpdate: user#=" + entry.getValue().intValue() + " -- name=" + entry.getKey());
//					sendTabShareUserUpdate(source, entry.getValue().intValue(), entry.getKey());
//				}
//			}
			Map<String, Integer> assignedUserNums = snapshotUserNums();
			for (Map.Entry<String, Integer> entry : assignedUserNums.entrySet()) {
				System.err.println("handlePresenceEvent - calling sendTabShareUserUpdate: user#=" + entry.getValue().intValue() + " -- name=" + entry.getKey());
				sendTabShareUserUpdate(source, entry.getValue().intValue(), entry.getKey(),0.9,60);
			}
			return;
		}

		// Ignore presence events for users Private_#, Camera_#, tab_group, and the bot agent
		if (isIgnoredUserName(userName)) {
			System.err.println("handlePresenceEvent - ignoring PE for - " + this.myName + " or " + userName);
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
			// Fires immediately, rather than waiting for the next 10-second
			// resyncTabShareUserUpdates() tick, so the tab is (usually)
			// already labeled by the time the user gets to
			// tab-share-chat.html. resyncTabShareUserUpdates() now
			// unconditionally resends every known mapping on its own
			// schedule regardless of this call, so there's no bookkeeping to
			// record here any more -- resending lookup.userNum again a few
			// seconds from now is harmless.
			System.err.println("handlePresenceEvent, isNewlyAssigned - callingsendTabShareUserUpdate for lookup.userNum=" + lookup.userNum + "  -- user name=" + userName);
			sendTabShareUserUpdate(source, lookup.userNum, userName,0.9,60);
		}

		if (lookup.shouldSendWelcome) {
//		if (pe.getType().equals(PresenceEvent.PRESENT)) {
			String agentNamePrefix = this.myName + "_";
			String sessionId = agentName.substring(agentNamePrefix.length());
			String sessionIdLast3 = sessionId.substring(Math.max(0, sessionId.length() - 3));
			String userNum = String.valueOf(lookup.userNum);
			
//			String privateName = privateUsernamePrefix + userNum;
//			String url = botServer + urlPrefix + sessionId + "/" + privateName + "/" + privateName + "/?" + "html=" + htmlPagePrivate;	
//			System.err.println("handlePresenceEvent, shouldSendWelcome - " + this.myName + " or " + userName); 
//			String redirectMessage = "Welcome, " + userName + "!" + "  \n\nOpen the following URL in a separate tab or window: " + url;

			String url = botServer + urlPrefix + sessionId + "/" + tabShareUsername + "/" + tabShareUsername + "/?" + "html=" + htmlPageGroup;	
			System.err.println("handlePresenceEvent, shouldSendWelcome - userName=" + userName); 
//			String redirectMessage = "Welcome, " + userName + "! " + "Everyone should open the following URL in a separate tab or window:\n\n " + url + "\n\n";
//			if (!lookup.shouldSendWelcome) {
//				redirectMessage = "As a reminder, everybody should open the following URL in a separate tab or window:\n\n " + url + "\n\n";
//			}
			String redirectMessage = "Welcome, " + userName + "! " + "Everyone should open the following URL in a separate tab or window:\n\n " + url + "\n\n";

			System.err.println("handlePresenceEvent, shouldSendWelcome - sending message: " + redirectMessage); 
//			PrivateMessageEvent newPMe = new PrivateMessageEvent(source,userName,this.myName,redirectMessage);
			MessageEvent newPMe1 = new MessageEvent(source, this.myName, redirectMessage);
			source.pushEventProposal(newPMe1,1.0,120);
			
			String cameraMessage = "With your smartphone, open URL \n" + cameraUrl + "\nIn the smartphone web page, enter:\nSession ID: " + sessionIdLast3;
			cameraMessage = cameraMessage + "\n\nImportant: Each person must enter their unique User ID: "; 	
			Map<String, Integer> assignedUserNums = snapshotUserNums();
			for (Map.Entry<String, Integer> entry : assignedUserNums.entrySet()) {
				cameraMessage = cameraMessage + "\n" + entry.getKey() + ": " + entry.getValue().intValue();
			}
//			String cameraMessage = userName + ", with your camera open URL\n" + cameraUrl + "\n\nand enter\nSession ID: " + sessionIdLast3 + "\nUser ID: " + userNum; 
			System.err.println("handlePresenceEvent, PresenceEvent.PRESENT - sending cameraMessage: " + cameraMessage); 
			MessageEvent newPMe2 = new MessageEvent(source, this.myName, cameraMessage);
			source.pushEventProposal(newPMe2,1.0,120);
		}
	}

	/**
	 * True for the userNames that are never tracked in userPresenceMap or
	 * counted toward userNum assignment: this listener's own agent name, the
	 * Private_/Camera_-prefixed per-user identities, and tabShareUsername
	 * itself. Shared by handlePresenceEvent's normal per-PresenceEvent path
	 * and addMissingUsersFromState's State-backfill path so the two can't
	 * drift apart on which userNames count as "real" tracked users.
	 */
	private boolean isIgnoredUserName(String userName) {
		return (userName.equals(this.myName)) || (userName.startsWith(privateUsernamePrefix)) || (userName.startsWith(cameraUsernamePrefix)) || (userName.equals(tabShareUsername));
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
	 * Thread-safe snapshot of every userName -> userNum assignment made so
	 * far (i.e. every userName that has had a UserPresenceInfo instantiated,
	 * in the order each was first seen). Returned as a fresh LinkedHashMap so
	 * callers -- see the tabShareUsername branch in handlePresenceEvent --
	 * can iterate and send events outside presenceLock rather than holding
	 * it for the duration of a loop of event dispatches.
	 */
	private Map<String, Integer> snapshotUserNums() {
		synchronized (presenceLock) {
			Map<String, Integer> snapshot = new LinkedHashMap<String, Integer>();
			for (Map.Entry<String, UserPresenceInfo> entry : userPresenceMap.entrySet()) {
				snapshot.put(entry.getKey(), Integer.valueOf(entry.getValue().getUserNum()));
			}
			return snapshot;
		}
	}

	/**
	 * Thread-safe: cross-checks userPresenceMap against
	 * State.getStudentIdsPresentOrNot() (every student chatId State knows
	 * about, whether currently present or not) and creates a UserPresenceInfo
	 * -- i.e. assigns a userNum -- for any of those chatIds that don't
	 * already have one. Missing users are added in whatever order
	 * getStudentIdsPresentOrNot() returns them in; nothing here depends on
	 * that order.
	 * <p>
	 * This exists because userPresenceMap is otherwise only ever populated
	 * one userName at a time, as this listener happens to observe that
	 * userName's own PresenceEvent (see consumeSendMessageFlag). A student
	 * State already knows about -- added by e.g. PresenceWatcher, or from a
	 * State this agent inherited/restored -- but whose PresenceEvent this
	 * particular listener instance never separately saw would otherwise never
	 * get a userNum and so would never be reflected in tab-share-chat.html's
	 * labels. Called from resyncTabShareUserUpdates, right before it resends
	 * every known mapping, so that resync always sees an up-to-date picture
	 * of every known user.
	 * <p>
	 * Entries created here get sendMessage=false: unlike the normal
	 * PresenceEvent-driven path (consumeSendMessageFlag /
	 * consumeSendMessageFlagAndCheckNew), this is a background reconciliation
	 * pass, not a live per-user onboarding event, so it must never trigger
	 * the one-time welcome-message flow that a genuine PresenceEvent would.
	 * A userNum assigned here reaches tab-share-chat.html on the very same
	 * resyncTabShareUserUpdates pass that just created it, since that pass
	 * resends every mapping unconditionally.
	 */
	private void addMissingUsersFromState() {
		log(Logger.LOG_NORMAL, "LlmCameraListener.addMissingUsersFromState - Enter");
		State state = StateMemory.getSharedState(agent);
		if (state == null) {
			return;
		}
	    String[] studentIds = state.getStudentIdsPresentOrNot();
// 		String[] studentIds = state.getRandomizedStudentIdsPresentOrNot();
		if (studentIds == null) {
			return;
		}

		synchronized (presenceLock) {
			for (String studentId : studentIds) {
				if (studentId == null || isIgnoredUserName(studentId)) {
					continue;
				}
				log(Logger.LOG_NORMAL, "LlmCameraListener.addMissingUsersFromState - found studentId: " + studentId);
				if (!userPresenceMap.containsKey(studentId)) {
					System.err.println("LlmCameraListener addMissingUsersFromState -- adding userName missing from userPresenceMap: " + studentId + " -- assigning userNum=" + nextUserNum);
					log(Logger.LOG_NORMAL, "LlmCameraListener.addMissingUsersFromState - adding studentId: " + studentId + 
							" as userNum: " + nextUserNum);
					UserPresenceInfo info = new UserPresenceInfo(studentId, nextUserNum, false);
					nextUserNum++;
					userPresenceMap.put(studentId, info);
				}
			}
		}
	}

	/**
	 * Runs every userPollRate seconds on tabShareUserNumWatcher's background
	 * thread (started from the constructor), until userPollTimeout seconds
	 * have elapsed since this listener was constructed, at which point the
	 * scheduling task in the constructor stops calling this method.
	 * Unconditionally (re-)sends every userNum -> userName mapping assigned
	 * so far to tab-share-chat.html via sendTabShareUserUpdate, so its tab
	 * labels stay current.
	 * <p>
	 * This is a backstop, not the primary notification path: the primary
	 * path is the synchronous sendTabShareUserUpdate call in
	 * handlePresenceEvent's isNewlyAssigned branch, fired the instant a new
	 * userNum is assigned. This periodic pass exists in case that
	 * synchronous notification didn't reach tab-share-chat.html -- its
	 * tab hadn't connected/reconnected yet at that moment, or the message
	 * was silently dropped by the chat server (see sendTabShareUserUpdate's
	 * own comment) -- so every label eventually catches up within 10
	 * seconds regardless of what happened to any earlier attempt.
	 * <p>
	 * Deliberately unconditional (no "did the max userNum change" check):
	 * there is no delivery acknowledgement from tab-share-chat.html, so this
	 * listener can never actually tell whether a given mapping already got
	 * through -- only whether it was already attempted. Resending everything
	 * every tick sidesteps that distinction. setPrivateTabUserName /
	 * renderTabLabel on the client side are idempotent, so re-applying a
	 * mapping that already landed is harmless, and a handful of short
	 * private messages every 10 seconds is cheap (userPresenceMap is capped
	 * by MAX_NUM_OTHER_FRAMES, currently 10 entries).
	 */
	private void resyncTabShareUserUpdates() {
		try {
			InputCoordinator currentSource = this.source;
			if (currentSource == null) {
				// No event has reached preProcessEvent yet, so there's no
				// InputCoordinator to send a notification through (and, in
				// practice, userPresenceMap will also still be empty).
				return;
			}

			// Backfill userPresenceMap with any student State already knows
			// about but that this listener never separately saw a
			// PresenceEvent for, before resending -- otherwise such a
			// user's userNum would never exist and so could never be sent.
			addMissingUsersFromState();

			Map<String, Integer> assignedUserNums = snapshotUserNums();
			for (Map.Entry<String, Integer> entry : assignedUserNums.entrySet()) {
				log(Logger.LOG_NORMAL, "LlmCameraListener.resyncTabShareUserUpdates - calling sendTabShareUserUpdate for userNum: " +
						entry.getValue().intValue() + " and name: " + entry.getKey());
				sendTabShareUserUpdate(currentSource, entry.getValue().intValue(), entry.getKey(),0.25,60);
			}
		} catch (Exception ex) {
			// Never let an uncaught exception here kill future scheduled
			// runs of this check.
			System.err.println("LlmCameraListener resyncTabShareUserUpdates -- error resending userNum mappings");
			ex.printStackTrace();
		}
	}

	/**
	 * Notifies the tab-share-chat.html page -- loaded as the
	 * tabShareUsername user (default "tab_group")
	 *  -- that userNum has just been assigned to userName, so it can
	 * relabel the corresponding "Private_&lt;userNum&gt;" tab in place.
	 * <p>
	 * This sends the mapping two ways. First, a PrivateMessageEvent
	 * addressed to tabShareUsername: the chat server (see
	 * server_lobby_https.js's 'sendpm' handler) delivers this to every
	 * socket currently connected in that room under the tabShareUsername
	 * identity -- there can be more than one at once (e.g. a stale tab left
	 * open alongside a reload, or more than one person with the group view
	 * open) -- but only to sockets connected AT THAT INSTANT; if
	 * tab-share-chat.html's tab hasn't connected/reconnected yet, or the
	 * message is otherwise lost, the server just logs it and drops it
	 * silently, with no error/ack back to this listener. Second, as a
	 * backstop, a MessageEvent with the same tagged text, which the server
	 * broadcasts to every connected socket in the room regardless of
	 * username -- reaching tab-share-chat.html even in the scenarios where
	 * the targeted send above was dropped. tab-share-chat.html's own
	 * 'updatechat' listener specifically recognizes the "tabUserUpdate" tag
	 * and relabels the tab instead of displaying this payload as a chat
	 * line; any other listener that only recognizes the older "multimodal"
	 * tag will NOT match this tag and will fall through to displaying the
	 * raw tagged text, so this broadcast should only be relied on for
	 * recipients that have been specifically taught to recognize it.
	 * <p>
	 * Because delivery here is never confirmed, callers should not assume a
	 * single call is enough -- see resyncTabShareUserUpdates, which calls
	 * this for every known mapping on an unconditional periodic schedule so
	 * a dropped update is retried without needing to know it was dropped.
	 */
	public void sendTabShareUserUpdate(InputCoordinator source, int userNum, String userName, double priority, double timeout) {
		String taggedMessage =
			MultiModalFilter.multiModalDelim
			+ "tabUserUpdate" + MultiModalFilter.withinModeDelim + "true"
			+ MultiModalFilter.multiModalDelim
			+ "userNum" + MultiModalFilter.withinModeDelim + userNum
			+ MultiModalFilter.multiModalDelim
			+ "userName" + MultiModalFilter.withinModeDelim + userName;
		System.err.println("sendTabShareUserUpdate - sending message: " + taggedMessage);

		PrivateMessageEvent tabUpdatePme = new PrivateMessageEvent(source, tabShareUsername, this.myName, taggedMessage);
		source.pushEventProposal(tabUpdatePme,priority,timeout);

		// Broadcast too, so every socket sharing the tabShareUsername
		// identity gets this update, not just whichever one the server
		// currently has on file as "the" tabShareUsername socket.
		MessageEvent tabUpdateMe = new MessageEvent(source, this.myName, taggedMessage);
		source.pushEventProposal(tabUpdateMe,priority,timeout);
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
	 * or below this listener's configured imageDiffThreshold (properties/LlmCameraListener.properties,
	 * key "imageDiffThreshold", default 0.05).
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
        return dissimilarity <= imageDiffThreshold;
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
        System.err.println("LlmCameraListener openAIrequestAndResponse - sending to LLM -- jsonPayload: " + jsonPayload);
	    String response = sendToOpenAI(source, jsonPayload, false);
        System.err.println("\\n\\n\\n*** LlmCameraListener openAIrequestAndResponse -- OpenAI response: " + response + " ***\n\n\n");
        Logger.commonLog("LlmCameraListener", Logger.LOG_NORMAL, "\n\n\n*** LlmCameraListener, openAIrequestAndResponse -- OpenAI response: " + response +
        		" ***\n\n\n"); 	
        if (!"No response".equals(response)) {
        	
			if ((!sender.startsWith(privateUsernamePrefix)) && (!sender.startsWith(cameraUsernamePrefix))) {
				System.err.println("LlmCameraListener openAIrequestAndResponse -- message to real user: " + response);
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
				System.err.println("LlmCameraListener openAIrequestAndResponse -- sending message to private user: " + response);
	    		source.pushEventProposal(newPMe1); 
	    		String privateCameraName = cameraUsernamePrefix + senderSuffix;
	    		PrivateMessageEvent newPMe2 = new PrivateMessageEvent(source,privateCameraName,this.myName,response); 
				System.err.println("LlmCameraListener openAIrequestAndResponse -- sending message to camera user: " + response);
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
		            // Use the same "No response" sentinel openAIrequestAndResponse()
		            // checks for, not "" -- "" != "No response", so returning ""
		            // here used to slip past that method's
		            // `if (!"No response".equals(response))` guard and get sent
		            // out as a genuinely empty chat message.
		            return "No response";
		        }
	        } finally {
                conn.disconnect(); // Ensure the connection is closed
            }
       
	    } catch (Exception e) {
	        e.printStackTrace();
	        // Same reasoning as the error-response branch above: signal
	        // failure with the sentinel openAIrequestAndResponse() actually
	        // checks for, not "".
	        return "No response";
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