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
	private Boolean intialized = false; 
	private int keywordNumberGoal = 0; 
	private int keywordMentionsGoal = 0;
	private int[] multipleKeywordMentionsGoal = new int[] {0,0}; 
	private int multipleMentionsNumGoal = 0;		// Goal for number of keywords with at least min number of mentions
	private int multipleMentionsMinGoal = 0;	    // Min number of mentions for a keyword to be counted as meeting 
													//    the multiple mentions goal
	String [] promptPriorities = null; 
	int promptPriorityNumberGoal = Integer.MAX_VALUE;
	int promptPriorityMentionsGoal = Integer.MAX_VALUE;
	int promptPriorityMultipleMentionsGoal = Integer.MAX_VALUE;
	private Boolean freeToComment = true;
	private int numPromptsNumber = 0;
	private int numPromptsMentions = 0;
	private int numPromptsMultipleMentions = 0;
	private int repeatedPromptDelay = 300;
	
	private HashMap<String,Integer> prioritiesAndCounts = new LinkedHashMap<>(); 
	private Map<String, Boolean> promptable = new HashMap<String, Boolean>();
	
	
	
	public KeywordWatcher(Agent a) 
	{
		super(a);
		agent = a; 
	}
	
	public void initialize() {
		if (properties != null)
		{
			String[] keywords = properties.getProperty("keywords", "").split("[\\s,]+");		
			State state = StateMemory.getSharedState(agent);
			state.addKeywords(keywords);
			StateMemory.commitSharedState(state, agent);		
			try{keywordNumberGoal = Integer.valueOf(getProperties().getProperty("number-goal", "0"));}
			catch(Exception e) {e.printStackTrace();}	
			try{keywordMentionsGoal = Integer.valueOf(getProperties().getProperty("mentions-goal", "0"));}
			catch(Exception e) {e.printStackTrace();}	
			try{repeatedPromptDelay = Integer.valueOf(getProperties().getProperty("repeated-prompt-delay", "300"));}
			catch(Exception e) {e.printStackTrace();}	
			
			try{multipleKeywordMentionsGoal = Stream.of(getProperties().getProperty("multiple-mentions-goal", "").split("[\\s,]+")).mapToInt(Integer::parseInt).toArray();}
			catch(Exception e) {e.printStackTrace();}	
			multipleMentionsNumGoal = multipleKeywordMentionsGoal[0]; 
			multipleMentionsMinGoal = multipleKeywordMentionsGoal[1]; 
			
			promptPriorities = properties.getProperty("prompt-priorities", "").split("[\\s,]+");
			for (int i=0; i<promptPriorities.length; i++) {
				prioritiesAndCounts.put(promptPriorities[i],0); 
			}
			
			
//			int priority = 1; 
//			for (int i=0; i<promptPriorities.length; i++) {
//				if (promptPriorities[i].equals("number-goal")) {
//					promptPriorityNumberGoal = priority; 
//					priority += 1; 
//				} else if (promptPriorities[i].equals("mentions-goal")) {
//					promptPriorityMentionsGoal = priority; 
//					priority += 1; 
//				} else if (promptPriorities[i].equals("multiple-mentions-goal")) {
//					promptPriorityMultipleMentionsGoal = priority; 
//					priority += 1; 				
//			}
	
//			System.err.println("*** KeywordWatcher.initialize - keywords: " + Arrays.toString(keywords));	
//			System.err.println("*** KeywordWatcher.initialize - keywords in State:");
//			state.printKeywordCounts();
//			System.err.println("*** KeywordWatcher.initialize, keywordNumberGoal: " + keywordNumberGoal);
//			System.err.println("*** KeywordWatcher.initialize, keywordMentionsGoal: " + keywordMentionsGoal);
//			System.err.println("*** KeywordWatcher.initialize, multipleKeywordMentionsGoal: " + Arrays.toString(multipleKeywordMentionsGoal));
//			System.err.println("*** KeywordWatcher.initialize, repeatedPromptDelay: " + repeatedPromptDelay);
		}
	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
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
//		System.err.println("KeywordWatcher.handleMessageEvent - enter"); 
		State state = StateMemory.getSharedState(agent);
		int numKeywords = state.getNumKeywords(); 
		
		if (numKeywords > 0) {
			Boolean prompted = false; 
			String[] annotations = me.getAllAnnotations(); 
			System.err.println("*** KeywordWatcher.handleMessageEvent, annotations: " + Arrays.toString(annotations));
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
				System.err.println("*** KeywordWatcher.handleMessageEvent -- updated keyword counts:");
				news.printKeywordCounts();		
			} else {
	//			System.out.println("*** KeywordWatcher.handleMessageEvent: No keywords found");
			}
			if (freeToComment) {
				prompted = promptIfAppropriate(); 
			} else {
				System.err.println(">>>>>>> KeywordWatcher.handleMessageEvent: Too soon to prompt <<<<<<<<<<"); 
			}
			if (prompted) {
				setPromptTimer(repeatedPromptDelay); 
			}
		}
    }
	
	private Boolean promptIfAppropriate() {

//		Boolean promptableNonZero = false; 
//		Boolean promptableMentions = false; 
//		Boolean promptableMultipleMentions = false; 
		int numNonZero = nonZeroKeyWordCount(); 
		if (numNonZero < keywordNumberGoal) {
			promptable.put("number-goal", true);
		} else {
			promptable.put("number-goal", false);
		}
		
		int maxMentions = maxKeywordCount(); 
		if (maxMentions < keywordMentionsGoal) {
			promptable.put("mentions-goal", true);
		} else {
			promptable.put("mentions-goal", false);
		}
		int numAtMentionsGoal = numKeywordMinCount(multipleMentionsMinGoal); 
		if (numAtMentionsGoal < multipleMentionsNumGoal) {
			promptable.put("multiple-mentions-goal", true);
		} else {
			promptable.put("multiple-mentions-goal", false);
		}
		
		Iterator<Integer> valueIterator = prioritiesAndCounts.values().iterator();		
		int minCount = Integer.MAX_VALUE; 
		int nextValue; 
		while (valueIterator.hasNext()) {
			nextValue = valueIterator.next(); 
			if (nextValue < minCount) {
				minCount = nextValue; 
			}
		}
		
		String promptType = null; 
		for (Map.Entry<String, Integer> entry : prioritiesAndCounts.entrySet()) {
			if (entry.getValue() <= minCount) {
				if (promptable.get(entry.getKey()) == true) {
					promptType = entry.getKey(); 
					prioritiesAndCounts.put(entry.getKey(),entry.getValue() + 1); 
					break; 
				}
			}				
		}
		
		System.err.println(">>>>>>> KeywordWatcher.promptIfAppropriate - promptType: " + promptType + " <<<<<<<<<<"); 
		
		if (promptType != null) {
			return true;
		} else {
			return false; 
		}
	}

		
//		for (int i=0; i<promptPriorities.length; i++) {
//			if (promptPriorities[i].equals("number-goal")) {
//				
//			}
//		}
		
//		if (promptableNonZero) {
//			if 
//		}
		
		

		
		// ========== TEMPORARY =========== 
//		int numNonZero = nonZeroKeyWordCount(); 
//		int maxMentions = maxKeywordCount(); 
//		int numAtMentionsGoal = numKeywordMinCount(keywordMentionsGoal); 
		
//		return false; 
		// ========== TEMPORARY ===========  
	
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
	
	private int maxKeywordCount() {
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
		System.err.println("=== KeywordWatcher.numKeywordMinCount: " + numReachedMin + " ===");   	
		return numReachedMin; 
	}
	
	
	private void setPromptTimer(int promptDelay) {
	
		System.err.println("KeywordWatcher.setPromptTimer: " + promptDelay); 
		freeToComment = false; 
		
		// Never set freeToComment to 'true' if promptDelay == 0
		if (promptDelay > 0) {
			new Timer(promptDelay, new TimeoutAdapter()
			{
				@Override
				public void timedOut(String id)
				{
					freeToComment = true; 
				}
			}).start();
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
	public void processEvent(InputCoordinator source, Event event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return null;
	}

}
