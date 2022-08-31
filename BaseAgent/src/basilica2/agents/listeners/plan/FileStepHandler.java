package basilica2.agents.listeners.plan;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.data.PromptTable;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.listeners.FileGatekeeper;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class FileStepHandler implements StepHandler
{

	private FileGatekeeper filegatekeeper = null;
	private PromptTable prompter = null;
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source)
	{
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"starting file gated step...");
		if(filegatekeeper == null)
		{
			Agent a = overmind.getAgent();
			filegatekeeper = new FileGatekeeper(a);
			prompter = new PromptTable("plans/gatekeeper_prompts.xml");
		}

		filegatekeeper.setStepName(currentStep.name);	
		String fileName = currentStep.attributes.get("file");
		filegatekeeper.setFileName(fileName); 
		
		overmind.addHelper(filegatekeeper);

		
		// checkinPrompt
		String checkinPrompt = currentStep.attributes.get("checkin_prompt");
		if(checkinPrompt == null)
		{
			checkinPrompt = "WAIT_FOR_CHECKIN";
		}		
		if(!checkinPrompt.equals("NONE"))
		{
			source.pushEventProposal(new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(checkinPrompt), "WAIT_FOR_CHECKIN"), OutputCoordinator.LOW_PRIORITY, 10);
		}

		
		// delayedPrompt	
		String delayedPrompt = currentStep.attributes.get("delayed_prompt");
		System.err.println("FileStepHandler: delayedPrompt before processing: " + delayedPrompt);
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"FileStepHandler: delayedPrompt before processing: " + delayedPrompt);
		
		List<String> delayedPromptList = null; 
		if (delayedPrompt.contains(",")) {
			delayedPromptList = Stream.of(delayedPrompt.split(",")).collect(Collectors.toList());
		} else {
//			delayedPromptList.add(delayedPrompt); 
			delayedPromptList = Stream.of(delayedPrompt).collect(Collectors.toList());
		}
		
		String delayedPromptTimeString = currentStep.attributes.get("delayed_prompt_time");
		System.err.println("FileStepHandler: delayedPromptTimeString before processing: " + delayedPromptTimeString);
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"FileStepHandler: delayedPromptTimeString before processing: " + delayedPromptTimeString);
		List<String> delayedPromptTimeList = null; 
		if (delayedPromptTimeString.contains(",")) {
			delayedPromptTimeList = Stream.of(delayedPromptTimeString.split(",")).collect(Collectors.toList());
		} else {
//			delayedPromptTimeList.add(delayedPromptTimeString); 
			delayedPromptTimeList = Stream.of(delayedPromptTimeString).collect(Collectors.toList());
		}	
		
		System.err.println("delayedPromptList: " + delayedPromptList); 
		
		System.err.println("delayedPromptTimeList: " + delayedPromptTimeList); 
		
		
//		System.err.println("FileStepHandler, execute - delayedPromptTime = " + String.valueOf(delayedPromptTime) + "   delayedPrompt = " + delayedPrompt);
		if (!delayedPromptList.isEmpty()) 
		{	
			System.err.println("FileStepHandler: Setting delayed prompt(s)"); 
			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"FileStepHandler: Setting delayed prompt(s)");
			
			for (int i=0; i < delayedPromptList.size(); i++) {
				Integer promptTime = Integer.valueOf(delayedPromptTimeList.get(i)); 
				String promptName = delayedPromptList.get(i); 
				new Timer(promptTime, currentStep.name, new TimeoutAdapter() 
				{
					@Override
					public void timedOut(String id)
					{
						if(currentStep.equals(overmind.currentPlan.currentStage.currentStep)) //the plan has not progressed on its own yet
						{
							MessageEvent delayedMessage = new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(promptName));

							System.err.println("FileStepHandler: pushing message: " + prompter.lookup(promptName));
							Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"FileStepHandler: pushing message: " + prompter.lookup(promptName));
							// source.pushEventProposal(delayedMessage, OutputCoordinator.HIGHEST_PRIORITY, 15);
							source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "MessageEvent", delayedMessage, OutputCoordinator.HIGHEST_PRIORITY, 8.0, 4));
						}
					}
				}).start();
			}
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
						MessageEvent warning = new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(warningPrompt), "FILE_STEP_TIMEOUT_WARNING");
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
						MessageEvent timeoutMsgEvent = new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup("FILE_STEP_TIMED_OUT"), "FILE_STEP_TIMED_OUT");
						source.pushEventProposal(timeoutMsgEvent, OutputCoordinator.HIGHEST_PRIORITY, 10);
					}
				}
			}).start();
		}
		
	}
	
}
