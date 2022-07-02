package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.events.EndEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class EndStepHandler implements StepHandler
{
	String endData = null;
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source)
	{
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Executing send_end step ...");

		if(currentStep.attributes.containsKey("end_data"))
		{
			endData = currentStep.attributes.get("endData");
		} else {
			endData = null; 
		}
		EndEvent endEvent = new EndEvent(source,endData); 	
        System.err.println("EndStepHandler, execute - EndEvent created: endData: " + endData);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"EndStepHandler, execute - EndEvent created: endData: " + endData);
		source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "EndStep", endEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
		overmind.stepDone();
	}
	
}
