package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.events.StartExternalTimerEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class StartExternalTimerStepHandler implements StepHandler
{
	String time = "0";
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source) {
	
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Executing StartExternalTimerStep step ...");

		if(currentStep.attributes.containsKey("time"))
		{
			time = currentStep.attributes.get("time");
		} else {
			time = "0"; 
		}
		
		StartExternalTimerEvent startExternalTimerEvent = new StartExternalTimerEvent(source,time); 	
        System.err.println("StartExternalTimerStepHandler, execute - StartExternalTimerEvent created -- time: " + time);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"StartExternalTimerStepHandler, execute - StartExternalTimerEvent created -- time: " + time);
		source.pushEventProposal("macro",startExternalTimerEvent,OutputCoordinator.HIGH_PRIORITY, 4.0);
		overmind.stepDone();
	}
	
}
