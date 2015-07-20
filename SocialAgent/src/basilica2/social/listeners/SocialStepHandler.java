package basilica2.social.listeners;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.listeners.IntroductionsHandler;
import basilica2.agents.listeners.plan.EndStepOnStopListeningDelegate;
import basilica2.agents.listeners.plan.PlanExecutor;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.StepHandler;

public class SocialStepHandler implements StepHandler
{

	private SocialController socializer = null;
	private StrategyScoreComputer scoreComputer = null;
	private RuleBasedTriggerComputer triggerComputer = null;
	
	@Override
	public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
	{
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"starting social step...");
		if(socializer == null)
		{
			Agent a = overmind.getAgent();
			socializer = new SocialController(a);
			scoreComputer = new StrategyScoreComputer(a);
			triggerComputer = new RuleBasedTriggerComputer(a);
		}
		
		//revoicer.setDelegate(new EndStepOnStopListening(overmind, currentStep.name));
        
		overmind.addHelper(socializer);
        overmind.addHelper(scoreComputer);
        overmind.addHelper(triggerComputer);
        
        //new Timer(currentStep.delay, PlanExecutor.PROGRESS_TIMER_ID, overmind).start();
	}
	
}
