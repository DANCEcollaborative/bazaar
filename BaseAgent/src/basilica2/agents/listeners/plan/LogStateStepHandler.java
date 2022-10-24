package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.events.LogEvent;
import basilica2.agents.events.LogStateEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.TimeoutAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class LogStateStepHandler implements StepHandler
{
	String stateTag = null;
	String stateValue = null; 
	Boolean sendLog = true; 
	String logTag = null; 
	
	@Override
	public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source) {
	
//		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Executing logstate step ...");

		if(currentStep.attributes.containsKey("state_tag"))
		{
			stateTag = currentStep.attributes.get("state_tag");
		} else {
			stateTag = null; 
		}
		if(currentStep.attributes.containsKey("state_value"))
		{
			stateValue = currentStep.attributes.get("state_value");
		} else {
			stateValue = null; 
		}
		if(currentStep.attributes.containsKey("send_log"))
		{
			sendLog = Boolean.valueOf(currentStep.attributes.get("send_log"));
		} else {
			sendLog = true; 
		}
		if(currentStep.attributes.containsKey("log_tag"))
		{
			logTag = currentStep.attributes.get("log_tag");
		} else {
			logTag = null; 
		}
		
		String sendLogString = String.valueOf(sendLog); 
		LogStateEvent logStateEvent = new LogStateEvent(source,stateTag,stateValue,sendLog,logTag); 	
//        System.err.println("LogStateStepHandler, execute - LogStateEvent created: stateTag: " + stateTag + "  stateValue: " + stateValue + "  stateValue: " + stateValue + "  sendLog: " + sendLogString + "  logTag: " + logTag);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateStepHandler, execute - LogStateEvent created: stateTag: " + stateTag + "  stateValue: " + stateValue + "  stateValue: " + stateValue + "  sendLog: " + sendLogString + "  logTag: " + logTag);
//		source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "LogStateStep", logStateEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
		source.pushEventProposal("macro",logStateEvent,OutputCoordinator.HIGH_PRIORITY, 4.0);
		overmind.stepDone();
	}
	
}
