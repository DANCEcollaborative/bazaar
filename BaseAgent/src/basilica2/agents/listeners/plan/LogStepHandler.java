package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.data.PromptTable;
import basilica2.agents.events.LogEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class LogStepHandler implements StepHandler
{
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source)
	{
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Executing log step ...");

		String logData = currentStep.attributes.get("log_data");
		LogEvent logEvent = new LogEvent(source,logData); 	
        System.err.println("LogStepHandler, execute - LogEvent created: logData: " + logData);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStepHandler, execute - LogEvent created - logData: " + logData);
		source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "LogStep", logEvent, OutputCoordinator.HIGH_PRIORITY, 30.0, 3));
	}
	
}
