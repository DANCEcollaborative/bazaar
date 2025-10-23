package basilica2.agents.listeners.plan;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.listeners.*;
/**
 * passes off control for this step to the IntroductionsHandler module.
 * continues to the next step when the IntroductionsHandler stops listening.
 * 
 * @author dadamson
 *
 */
public class GreetStepHandler implements StepHandler
{
	private static IntroductionsHandler introducer;
	public static String getStepType()
	{
		return "greet";
	}
	

	@Override
	public void execute(Step currentStep, final PlanExecutor overmind, InputCoordinator source)
	{
		if(introducer == null)
		{
			introducer = new IntroductionsHandler(overmind.getAgent());
		}
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"starting greet step...");
		 
		//when the introducer de-registers itself as a listener, end this step.
		//alternatively, the introducer could queue a new StepDoneEvent to progress the plan.
        introducer.setDelegate(new EndStepOnStopListeningDelegate(overmind, currentStep.name));
        
        //register the introducer as a listener just for this step.
        overmind.addHelper(introducer);
        
     }
	
}
