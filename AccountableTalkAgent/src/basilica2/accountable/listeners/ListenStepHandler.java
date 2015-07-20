package basilica2.accountable.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.listeners.plan.PlanExecutor;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.StepHandler;
import basilica2.social.listeners.RuleBasedTriggerComputer;
import basilica2.social.listeners.SocialController;
import basilica2.social.listeners.StrategyScoreComputer;
import basilica2.util.TimeoutAdapter;
import basilica2.util.Timer;
import edu.cmu.cs.lti.basilica2.core.Agent;

public class ListenStepHandler implements StepHandler
{

	private AgreeDisagreeActor agreer = null;
	private RevoiceActor revoicer = null;
	private SayMoreActor saymore = null;
	private AskForExplanationActor explainer = null;
	private AskForRestateActor restater = null;
	private PressForReasoningActor presser = null;
	
	
	private SocialController socializer = null;
	private StrategyScoreComputer scoreComputer = null;
	private RuleBasedTriggerComputer triggerComputer = null;
//	private FeedbackActor feedbacker;
	
	@Override
	public void execute(Step currentStep, final PlanExecutor overmind, InputCoordinator source)
	{
		System.out.println("starting facilitation step...");
		if(revoicer == null)
		{
			Agent a = overmind.getAgent();
			revoicer = new RevoiceActor(a);
			agreer = new AgreeDisagreeActor(a);
			explainer = new AskForExplanationActor(a);
			saymore = new SayMoreActor(a);
			restater = new AskForRestateActor(a);
			presser = new PressForReasoningActor(a);
			
			socializer = new SocialController(a);
			scoreComputer = new StrategyScoreComputer(a);
			triggerComputer = new RuleBasedTriggerComputer(a);
//			feedbacker = new FeedbackActor(a);
		}
		
		//revoicer.setDelegate(new EndStepOnStopListening(overmind, currentStep.name));

//        overmind.addHelper(feedbacker);
        overmind.addHelper(revoicer);
        overmind.addHelper(agreer);
        overmind.addHelper(explainer);
        overmind.addHelper(saymore);
        overmind.addHelper(restater);
        overmind.addHelper(presser);
        
        
//        overmind.addHelper(scoreComputer);
//        overmind.addHelper(triggerComputer);
//		overmind.addHelper(socializer);
		
		if(currentStep.attributes.containsKey("duration"))
			new Timer(Long.parseLong(currentStep.attributes.get("duration")), null, new TimeoutAdapter()
			{
				@Override
				public void timedOut(String id)
				{
					overmind.stepDone();
				}
				
			}).start();
        
        //new Timer(currentStep.delay, PlanExecutor.PROGRESS_TIMER_ID, overmind).start();
	}
	
}
