package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.events.LogEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class LogStepHandler implements StepHandler
{
	String tag = null;
	String details = null; 
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source)
	{
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Executing sendlog step ...");

		if(currentStep.attributes.containsKey("tag"))
		{
			tag = currentStep.attributes.get("tag");
		} else {
			tag = null; 
		}
		if(currentStep.attributes.containsKey("details"))
		{
			details = currentStep.attributes.get("details");
		} else {
			details = null; 
		}
		LogEvent logEvent = new LogEvent(source,tag,details); 	
        System.err.println("LogStepHandler, execute - LogEvent created: tag: " + tag + "   details: " + details);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStepHandler, execute - LogEvent created: tag: " + tag + "   details: " + details);
		source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "LogStep", logEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
		overmind.stepDone();
	}
	
}
