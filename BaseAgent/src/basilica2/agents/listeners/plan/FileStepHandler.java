package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.data.PromptTable;
import basilica2.agents.events.MessageEvent;
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
		
		String checkinPrompt = currentStep.attributes.get("checkin_prompt");
		if(checkinPrompt == null)
		{
			checkinPrompt = "WAIT_FOR_CHECKIN";
		}
		
		if(!checkinPrompt.equals("NONE"))
		{
			source.pushEventProposal(new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(checkinPrompt), "WAIT_FOR_CHECKIN"), OutputCoordinator.LOW_PRIORITY, 10);
		}
	
//		String type = "prompt";
//		currentStep.executeStepHandlers(source, type);
		
		new Timer(Math.max(currentStep.timeout - 180, currentStep.timeout*0.75), currentStep.name, new TimeoutAdapter()
		{
			@Override
			public void timedOut(String id)
			{
				if(currentStep.equals(overmind.currentPlan.currentStage.currentStep)) //the plan has not progressed on its own yet
				{
					String warningPrompt = currentStep.attributes.get("warning_prompt");
					MessageEvent softWarning = new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(warningPrompt), "FILE_STEP_TIMEOUT_WARNING");
					source.pushEventProposal(softWarning, OutputCoordinator.MEDIUM_PRIORITY, 5);
				}
			}
		}).start();
		
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
