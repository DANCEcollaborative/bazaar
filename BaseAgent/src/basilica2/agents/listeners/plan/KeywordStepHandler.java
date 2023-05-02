package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.listeners.KeywordWatcher;
import basilica2.agents.listeners.KeywordGatekeeper;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class KeywordStepHandler implements StepHandler
{

	private KeywordGatekeeper keywordGatekeeper = null;
	private PromptTable prompter = null;
	private KeywordWatcher keywordWatcher = null;
	private Boolean shouldContinuePrompting = false;
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source)
	{
		keywordWatcher = (KeywordWatcher)source.getPreProcessor("KeywordWatcher"); 	
		
		// Keywords	
		String keywords = currentStep.attributes.get("keywords");
//		System.err.println("KeywordStepHandler: keywords: " + keywords);		
		if (keywords != null) {
			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"KeywordStepHandler: keywords before processing: " + keywords);			
			List<String> keywordList = null; 
			if (keywords.contains(",")) {
				keywordList = Stream.of(keywords.split(",")).collect(Collectors.toList());
			} else {
				keywordList = Stream.of(keywords).collect(Collectors.toList());
			}
			keywordWatcher.removeAllKeywords();
			keywordWatcher.addKeywords(keywordList.toArray(new String[0])); 
//			keywordWatcher.printKeywordCounts();
		}

		// Reset keyword counts	
		Boolean resetCounts = true; 
		if(currentStep.attributes.containsKey("reset-counts"))
		{
			resetCounts = Boolean.valueOf(currentStep.attributes.get("reset-counts"));
		}
//		System.err.println("KeywordStepHandler: resetCounts: " + Boolean.valueOf(resetCounts));
		if (resetCounts == true) {
			keywordWatcher.resetAllKeywordCounts();
		}

		// Goal for number of keywords
		Integer keywordNumberGoal = Integer.valueOf(currentStep.attributes.get("number-goal"));
		if (keywordNumberGoal != null) {
//			System.err.println("KeywordStepHandler: keywordNumberGoal: " + keywordNumberGoal);
			keywordWatcher.setKeywordNumberGoal(keywordNumberGoal);
		}
		
		// Goal for number of mentions for a single keyword
		Integer keywordMentionsGoal = Integer.valueOf(currentStep.attributes.get("mentions-goal"));
		if (keywordMentionsGoal != null) {
//			System.err.println("KeywordStepHandler: keywordMentionsGoal: " + keywordMentionsGoal);
			keywordWatcher.setKeywordMentionsGoal(keywordMentionsGoal);
		}

		int[] multipleKeywordMentionsGoal = {0,0};
		try{multipleKeywordMentionsGoal = Stream.of(currentStep.attributes.get("multiple-mentions-goal").split("[\\s,]+")).mapToInt(Integer::parseInt).toArray();}
		catch(Exception e) {e.printStackTrace();}	
		keywordWatcher.setMultipleKeywordMentionsGoal(multipleKeywordMentionsGoal);
		
		// promptInterval	
		Integer promptInterval = Integer.valueOf(currentStep.attributes.get("prompt-interval"));
		if (promptInterval != null) {
//			System.err.println("KeywordStepHandler: promptInterval: " + promptInterval);
			keywordWatcher.setPromptInterval(promptInterval);
		}

		// Continue keyword prompting after step ends? Default to false. 
		if(currentStep.attributes.containsKey("continue-prompting-after"))
		{
			shouldContinuePrompting = Boolean.valueOf(currentStep.attributes.get("continue-prompting-after"));
		}
//		System.err.println("KeywordStepHandler: shouldContinuePrompting: " + Boolean.valueOf(shouldContinuePrompting));

		
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"starting keyword gated step...");
		if(keywordGatekeeper == null)
		{
			Agent a = overmind.getAgent();
			keywordGatekeeper = new KeywordGatekeeper(a);
			prompter = new PromptTable("plans/gatekeeper_prompts.xml");
		}

		keywordGatekeeper.setStepName(currentStep.name);	
		
		overmind.addHelper(keywordGatekeeper);
		
		// checkinPrompt
		String checkinPrompt = currentStep.attributes.get("checkin_prompt");
		if(!checkinPrompt.equals("NONE"))
		{
			source.pushEventProposal(new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(checkinPrompt), "WAIT_FOR_CHECKIN"), OutputCoordinator.LOW_PRIORITY, 10);
		}

		// warningPrompt	
		String warningPrompt = currentStep.attributes.get("warning_prompt");
		if(warningPrompt == null)
		{
			warningPrompt = "NONE";
		}	
		if ((!warningPrompt.equals("NONE")) && (currentStep.timeout != 0))
		{
//			System.err.println("Setting warning prompt"); 
			new Timer(Math.max(currentStep.timeout - 180, currentStep.timeout*0.75), currentStep.name, new TimeoutAdapter()
			{
				@Override
				public void timedOut(String id)
				{
					if(currentStep.equals(overmind.currentPlan.currentStage.currentStep)) //the plan has not progressed on its own yet
					{
						String warningPrompt = currentStep.attributes.get("warning_prompt");
						MessageEvent warning = new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(warningPrompt), "KEYWORD_STEP_TIMEOUT_WARNING");
						source.pushEventProposal(warning, OutputCoordinator.HIGHEST_PRIORITY, 15);
					}
				}
			}).start();
		}
		
		if (currentStep.timeout != 0) { 
//			System.err.println("Setting step timeout"); 
			new Timer(currentStep.timeout, currentStep.name, new TimeoutAdapter()
			{
				@Override
				public void timedOut(String id)
				{
					if(currentStep.equals(overmind.currentPlan.currentStage.currentStep)) //the plan has not progressed on its own yet
					{
						MessageEvent timeoutMsgEvent = new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup("KEYWORD_STEP_TIMED_OUT"), "KEYWORD_STEP_TIMED_OUT");
						source.pushEventProposal(timeoutMsgEvent, OutputCoordinator.HIGHEST_PRIORITY, 10);
						keywordWatcher.setShouldPrompt(shouldContinuePrompting);
					}
				}
			}).start();
		}
		
	}
	
}
