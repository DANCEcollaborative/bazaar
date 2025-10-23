package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.List;

import edu.cmu.cs.lti.project911.utils.log.Logger;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;
/**
 * do nothing when this step is executed - PlanExecutor waits for an external StepDoneEvent or a timeout.
 * @author dadamson
 *
 */
class DoNothingStepHandler implements StepHandler
{
	private PromptTable prompter;

	public static String getStepType()
	{
		return "no-op";
	}

	public DoNothingStepHandler()
	{
		
	}

	public void execute(Step step, PlanExecutor overmind, InputCoordinator source)
	{
		Logger.commonLog("DoNothingStepHandler", Logger.LOG_NORMAL, "DoNothingStepHandler does nothing.");
		overmind.stepDone();
	}
}