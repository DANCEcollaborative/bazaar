package basilica2.agents.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;

import java.util.Map;

import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.events.TypingEvent;
import basilica2.agents.events.WhiteboardEvent;
import basilica2.agents.events.PoseEvent.poseEventType;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.events.FileEvent;
import basilica2.agents.listeners.PresenceWatcher;
import basilica2.agents.listeners.MultiModalFilter.multiModalTag;
import basilica2.agents.listeners.plan.StepHandler;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.data.State;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.*; 
import java.lang.Math;
import java.time.LocalDateTime; 
import java.io.*;


public class KeywordWatcher extends BasilicaAdapter
{ 
	private InputCoordinator source;
	Agent agent; 
	private String agentName; 
	private PromptTable prompter = null;
	private double keywordPriority;
	private double keywordWindow;
	private double keywordBlackout;
	
	private Boolean intialized = false; 
	private int keywordNumberGoal = 0; 
	private int keywordMentionsGoal = 0;
	private int[] multipleKeywordMentionsGoal = new int[] {0,0}; 
	private int multipleMentionsNumGoal = 0;		// Goal for number of keywords with at least min number of mentions
	private int multipleMentionsMinGoal = 0;	    // Min number of mentions for a keyword to be counted as meeting 
													//    the multiple mentions goal
	private String [] promptPriorities = null; 
	private Boolean freeToComment = true;
	private int promptInterval = 300;	
	private HashMap<String,Integer> prioritiesAndCounts = new LinkedHashMap<>(); 
	private Map<String, Boolean> promptable = new HashMap<String, Boolean>();
	private Boolean shouldPrompt = true; 
	
	
	
	public KeywordWatcher(Agent a) 
	{
		super(a);
		agent = a; 
		agentName = a.getUsername();
	}
	
	public void initialize() {
		if (properties != null)
		{

			prompter = new PromptTable(properties.getProperty("prompt_file", "plan_prompts.xml"));
			keywordPriority = Double.parseDouble(properties.getProperty("priority", "0.8"));
			keywordWindow = Double.parseDouble(properties.getProperty("window", "15"));
			keywordBlackout= Double.parseDouble(properties.getProperty("blackout", "5"));
			String[] keywords = properties.getProperty("keywords", "").split("[\\s,]+");
			addKeywords(keywords);	
			try{keywordNumberGoal = Integer.valueOf(getProperties().getProperty("number-goal", "0"));}
			catch(Exception e) {e.printStackTrace();}	
			try{keywordMentionsGoal = Integer.valueOf(getProperties().getProperty("mentions-goal", "0"));}
			catch(Exception e) {e.printStackTrace();}	
			try{promptInterval = Integer.valueOf(getProperties().getProperty("prompt-interval", "300"));}
			catch(Exception e) {e.printStackTrace();}	
			
			try{multipleKeywordMentionsGoal = Stream.of(getProperties().getProperty("multiple-mentions-goal", "").split("[\\s,]+")).mapToInt(Integer::parseInt).toArray();}
			catch(Exception e) {e.printStackTrace();}	
			multipleMentionsNumGoal = multipleKeywordMentionsGoal[0]; 
			multipleMentionsMinGoal = multipleKeywordMentionsGoal[1]; 
			
			promptPriorities = properties.getProperty("prompt-priorities", "").split("[\\s,]+");
			for (int i=0; i<promptPriorities.length; i++) {
				prioritiesAndCounts.put(promptPriorities[i],0); 
			}
		}
	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		this.source = source;
		if (!intialized) {
//			System.err.println("*** KeywordWatcher.preProcessEvent - calling initialize");
			initialize();
			intialized = true; 
		}
		if (e instanceof MessageEvent)
		{
			handleMessageEvent(source, (MessageEvent) e);
		}
	}

	// Checks messages for keywords. If found, adds to keyword count(s) 
	private void handleMessageEvent(InputCoordinator source, MessageEvent me)
	{
		
		State state = StateMemory.getSharedState(agent);
		int numKeywords = state.getNumKeywords(); 
		
		if (numKeywords > 0) {
			Boolean prompted = false; 
			String[] annotations = me.getAllAnnotations(); 
//			System.err.println("*** KeywordWatcher.handleMessageEvent, annotations: " + Arrays.toString(annotations));
			Set<String> keywords = state.getKeywords();
			boolean keywordFound = false;
			for (int i=0; i < annotations.length; i++) {
				if (keywords.contains(annotations[i])) {
					keywordFound = true; 
					state.bumpKeywordCount(annotations[i]); 					
				}
			}
			if (keywordFound) {
				State news = State.copy(state);
				StateMemory.commitSharedState(news, agent);	

			} else {
	//			System.out.println("*** KeywordWatcher.handleMessageEvent: No keywords found");
			}
			if (freeToComment) {
				promptIfAppropriate(); 
			}
		}
    }
	
	private void promptIfAppropriate() {
		
		if (!shouldPrompt) {
			System.err.println("KeywordWatcher.promptIfAppropriate.: shouldPrompt == false");
			return; 
		}

		// Check if number of keywords mentioned is promptable
		int numNonZero = nonZeroKeyWordCount(); 
		if (numNonZero < keywordNumberGoal) {
			promptable.put("number-goal", true);
//			System.err.println("KeywordWatcher.promptIfAppropriate - number-goal is promptable"); 
		} else {
			promptable.remove("number-goal");
		}
		
		// Check if number of mentions for a single keyword is promptable
		int maxMentions = maxKeywordCount(); 
		if (maxMentions < keywordMentionsGoal) {
			promptable.put("mentions-goal", true);
//			System.err.println("KeywordWatcher.promptIfAppropriate - mentions-goal is promptable"); 
		} else {
			promptable.remove("mentions-goal");
		}
		
		// Check if number of keywords with a minimum number of mentions is promptable 
		int numAtMentionsGoal = numKeywordMinCount(multipleMentionsMinGoal); 
		if (numAtMentionsGoal < multipleMentionsNumGoal) {
			promptable.put("multiple-mentions-goal", true);
//			System.err.println("KeywordWatcher.promptIfAppropriate - multiple-mentions-goal is promptable"); 
		} else {
			promptable.remove("multiple-mentions-goal");
		}
		
		// Get the the least number of prompts already provided from among the priorities	
		int minCount = Integer.MAX_VALUE; 
		String key;
		int promptCount; 
		for (Map.Entry<String, Integer> entry : prioritiesAndCounts.entrySet()) {
			key = entry.getKey();
			promptCount = entry.getValue(); 
			if (promptable.containsKey(key)) {
				if (promptCount < minCount) {
					minCount = promptCount; 
				}
			}
		}
//		System.err.println("KeywordWatcher.promptIfAppropriate - minimum prompts count among remaining priorities is " + minCount);
		
		// Check for highest priority prompt that is promptable and at least tied for fewest previous prompts
		String promptType = null; 
		key = null; 
		for (Map.Entry<String, Integer> entry : prioritiesAndCounts.entrySet()) {    // 
			if (entry.getValue() <= minCount) {
				key = entry.getKey(); 
				if (promptable.containsKey(key)) {
					if (promptable.get(key) == true) {
						promptType = key; 
						proposeKeywordPrompt(key,null); 
						break; 
					}
				}
			}				
		}
	}

	
	private int nonZeroKeyWordCount() {	
		int nonZeroCount = 0;
		State state = StateMemory.getSharedState(agent);
		Iterator<Integer> keywordvalueIterator = state.getKeywordCountsValues().iterator();	
		while (keywordvalueIterator.hasNext()) {
			if (keywordvalueIterator.next() != 0) {
				nonZeroCount += 1; 
			}
		}
//		System.err.println("KeywordWatcher.nonZeroKeyWordCount: " + nonZeroCount); 
		return nonZeroCount; 
	}
	
	public int maxKeywordCount() {
		int maxValue = 0;
		State state = StateMemory.getSharedState(agent);
		Iterator<Integer> keywordvalueIterator = state.getKeywordCountsValues().iterator();	
		int nextValue; 
		while (keywordvalueIterator.hasNext()) {
			nextValue = keywordvalueIterator.next(); 
			if (nextValue > maxValue) {
				maxValue = nextValue; 
			}
		}	
//		System.err.println("KeywordWatcher.maxKeywordCount: " + maxValue);     
		return maxValue; 
	}
	
	private int numKeywordMinCount(int minCount) {
		int numReachedMin = 0;
		if (minCount > 0) {
			State state = StateMemory.getSharedState(agent);
			Iterator<Integer> keywordvalueIterator = state.getKeywordCountsValues().iterator();	
			int nextValue; 
			while (keywordvalueIterator.hasNext()) {
				nextValue = keywordvalueIterator.next(); 
				if (nextValue >= minCount) {
					numReachedMin +=  1; 
				}
			}	 			
		}
//		System.err.println("=== KeywordWatcher.numKeywordMinCount: " + numReachedMin + " ===");   	
		return numReachedMin; 
	}
	
	
	public void setPromptTimer(int promptInterval) {
	
//		System.err.println(">>>> KeywordWatcher.setPromptTimer: " + promptInterval); 
		freeToComment = false; 
		
		// Never set freeToComment to 'true' if promptInterval == 0
		if (promptInterval > 0) {
			new Timer(promptInterval, new TimeoutAdapter()
			{
				@Override
				public void timedOut(String id)
				{
					freeToComment = true; 
				}
			}).start();
		}
	}
	


	private void proposeKeywordPrompt(final String promptKey, Map<String, String> slots)
	{
		log(Logger.LOG_NORMAL, "proposing keyword prompt: "+promptKey);
		System.err.println(">>>>>>> KeywordWatcher.proposeKeywordPrompt - proposing promptKey: " + promptKey + " <<<<<<<<<<"); 
		final String message = prompter.lookup(promptKey, slots);
		MessageEvent me = new MessageEvent(source, agentName, message, "KEYWORD", promptKey);
		PriorityEvent pete = PriorityEvent.makeBlackoutEvent("KEYWORD", me, keywordPriority, keywordWindow, keywordBlackout);

		pete.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p)
			{
				log(Logger.LOG_NORMAL, "accepted keyword prompt: "+promptKey);
				bumpPromptCount(promptKey);
				setPromptTimer(promptInterval); 
			}

			@Override
			public void rejected(PriorityEvent p)
			{
				log(Logger.LOG_NORMAL, "rejected keyword prompt: "+promptKey);
			}
		});
		source.pushProposal(pete);
	}
	
	private void bumpPromptCount(String key) {
		int oldCount = prioritiesAndCounts.get(key);
		int updatedCount = oldCount + 1; 
		prioritiesAndCounts.put(key, updatedCount);		
	}
	
	public int getPromptableNumEntries() {
		return promptable.size(); 
	}	
	
	public void addKeywords(String[] keywords) {	
		State state = StateMemory.getSharedState(agent);
		state.addKeywords(keywords);
		StateMemory.commitSharedState(state, agent);		
	}
	
	public void setKeywordNumberGoal (int goal) {
		keywordNumberGoal = goal; 
	}	
	
	public void setKeywordMentionsGoal (int goal) {
		keywordMentionsGoal = goal; 
	}
		
	public void setMultipleKeywordMentionsGoal (int[] goals) {
		multipleMentionsNumGoal = goals[0]; 
		multipleMentionsMinGoal = goals[1]; 
	}
	
	public void setPromptPriorities (String[] priorities) {
		for (int i=0; i<priorities.length; i++) {
			prioritiesAndCounts.put(priorities[i],0); 
		}
	}
	
	public void setPromptInterval (int intervalSeconds) { 
		promptInterval = intervalSeconds; 
	}
	
	public void removeAllKeywords () {	
		System.err.println("!!!!!! KeywordWatcher.removeAllKeywords: enter !!!!!!"); 
		State state = StateMemory.getSharedState(agent);
		state.removeAllKeywords();
		StateMemory.commitSharedState(state, agent);
		state.printKeywordCounts();
	}
	
	public void resetAllKeywordCounts () {	
		State state = StateMemory.getSharedState(agent);
		state.resetAllKeywordCounts();
		StateMemory.commitSharedState(state, agent);			
	}
	
	public void setShouldPrompt (Boolean shouldPromptSetting) {	
		shouldPrompt = shouldPromptSetting; 		
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
	public void processEvent(InputCoordinator source, Event event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return null;
	}

}
