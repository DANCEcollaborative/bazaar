package basilica2.tutor.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.listeners.plan.PlanExecutor;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.StepHandler;
import basilica2.tutor.events.DoTutoringEvent;

/**
 * launch a DoTutoring event to trigger a tutor dialog for this step.
 * 
 * @author dadamson
 * 
 */
public class DialogStepHandler implements StepHandler
{
	@Override
	public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
	{
		String dialogueName = currentStep.attributes.get("dialogue");
		if (dialogueName == null) 
			dialogueName = currentStep.name;
		source.queueNewEvent(new DoTutoringEvent(source, dialogueName));
	}

}
