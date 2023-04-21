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
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

import java.util.Arrays;
import java.util.Hashtable;
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
	private Boolean keywordsInitialized = false; 
	private int keywordNumberGoal = 0; 
	private int keywordMentionsGoal = 0;
	private int[] multipleKeywordMentionsGoal = new int[] {0,0}; 
	private int repeatedPromptDelay = 300;
	
	
	
	public KeywordWatcher(Agent a) 
	{
		super(a);
		agent = a; 
	}
	
	public void initializeKeywords() {
		if (properties != null)
		{
			String[] keywords = properties.getProperty("keywords", "").split("[\\s,]+");
//			System.err.println("*** KeywordWatcher.initializeKeywords - keywords: " + Arrays.toString(keywords));			
			State state = StateMemory.getSharedState(agent);
			state.addKeywords(keywords);
			StateMemory.commitSharedState(state, agent);	
//			System.err.println("*** KeywordWatcher.initializeKeywords - keywords in State:");
//			state.printKeywordCounts();
	
			try{keywordNumberGoal = Integer.valueOf(getProperties().getProperty("keyword-number-goal", "0"));}
			catch(Exception e) {e.printStackTrace();}	
			try{keywordMentionsGoal = Integer.valueOf(getProperties().getProperty("keyword-mentions-goal", "0"));}
			catch(Exception e) {e.printStackTrace();}

//			try{multipleKeywordMentionsGoal = Integer.valueOf(getProperties().getProperty("multiple-keyword-mentions-goal", "{0,0}").split("[\\s,]+"));}
//			catch(Exception e) {e.printStackTrace();}
			
			multipleKeywordMentionsGoal = Stream.of(getProperties().getProperty("multiple-keyword-mentions-goal", "").split("[\\s,]+")).mapToInt(Integer::parseInt).toArray();
			
			try{repeatedPromptDelay = Integer.valueOf(getProperties().getProperty("repeated-prompt-delay", "300"));}
			catch(Exception e) {e.printStackTrace();}	
			
//			static int[] parseIntArray(String[] arr) {
//			    return Stream.of(arr).mapToInt(Integer::parseInt).toArray();
//			}

			System.err.println("*** KeywordWatcher.constructor, keywordNumberGoal: " + keywordNumberGoal);
			System.err.println("*** KeywordWatcher.constructor, keywordMentionsGoal: " + keywordMentionsGoal);
			System.err.println("*** KeywordWatcher.constructor, multipleKeywordMentionsGoal: " + Arrays.toString(multipleKeywordMentionsGoal));
			System.err.println("*** KeywordWatcher.constructor, repeatedPromptDelay: " + repeatedPromptDelay);
		}
	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (!keywordsInitialized) {
//			System.err.println("*** KeywordWatcher.preProcessEvent - calling initializeKeywords");
			initializeKeywords();
			keywordsInitialized = true; 
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
		String[] annotations = me.getAllAnnotations(); 
		System.err.println("*** KeywordWatcher.handleMessageEvent, annotations: " + Arrays.toString(annotations));
		State olds = StateMemory.getSharedState(agent);
		State news = State.copy(olds);
		Set keywords = news.getKeywords();
		boolean keywordFound = false;
		
		for (int i=0; i < annotations.length; i++) {
			if (keywords.contains(annotations[i])) {
				keywordFound = true; 
				news.bumpKeywordCount(annotations[i]); 					
			}
		}
		if (keywordFound) {
			StateMemory.commitSharedState(news, agent);	
			System.err.println("*** KeywordWatcher.handleMessageEvent -- updated keyword counts:");
			news.printKeywordCounts();		
		} else {
			System.err.println("*** KeywordWatcher.handleMessageEvent: No keywords found");
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
