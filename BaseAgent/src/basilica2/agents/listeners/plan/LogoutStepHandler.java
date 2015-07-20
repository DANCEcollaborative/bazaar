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
 * end the agent's operation when this step is executed.
 * @author dadamson
 *
 */
class LogoutStepHandler implements StepHandler
{
	private PromptTable prompter;

	public static String getStepType()
	{
		return "prompt";
	}

	public LogoutStepHandler()
	{
		
	}

	public void execute(Step step, PlanExecutor overmind, InputCoordinator source)
	{
		Logger.commonLog("LogoutStepHandler", Logger.LOG_NORMAL, "step "+step+": stopping agent");
		overmind.getAgent().dispose();
		System.exit(0);
	}
}