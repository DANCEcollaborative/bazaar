package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.data.PromptTable;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.Gatekeeper;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class GatedStepHandler implements StepHandler
{

	private Gatekeeper gatekeeper = null;
	private PromptTable prompter = null;
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source)
	{
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"starting gated step...");
		if(gatekeeper == null)
		{
			Agent a = overmind.getAgent();
			gatekeeper = new Gatekeeper(a);
			prompter = new PromptTable("plans/gatekeeper_prompts.xml");
		}
		
		gatekeeper.setStepName(currentStep.name);
		gatekeeper.resetGateForAllStudents();
		
		overmind.addHelper(gatekeeper);
		
		String type = "prompt";
		if(currentStep.attributes.containsKey("gated_type"))
		{
			type = currentStep.attributes.get("gated_type");
		}
		
		String checkinPrompt = currentStep.attributes.get("checkin_prompt");
		if(checkinPrompt == null)
		{
			checkinPrompt = "WAIT_FOR_CHECKIN";
		}
		
		if(!checkinPrompt.equals("NONE"))
		{
			source.pushEventProposal(new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup(checkinPrompt), "WAIT_FOR_CHECKIN"), OutputCoordinator.LOW_PRIORITY, 10);
		}
		currentStep.executeStepHandlers(source, type);
		
		new Timer(Math.max(currentStep.timeout - 180, currentStep.timeout*0.75), currentStep.name, new TimeoutAdapter()
		{
			@Override
			public void timedOut(String id)
			{
				if(currentStep.equals(overmind.currentPlan.currentStage.currentStep)) //the plan has not progressed on its own yet
				{
					MessageEvent softWarning = new MessageEvent(source, overmind.getAgent().getUsername(), prompter.lookup("TIMEOUT_WARNING"), "TIMEOUT_WARNING");
					source.pushEventProposal(softWarning, OutputCoordinator.LOW_PRIORITY, 20);
				}
			}
		}).start();
		
	}
	
}
