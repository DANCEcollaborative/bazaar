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
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class ConditionalListenStepHandler implements StepHandler
{

	private RevoiceActor revoicer = null;
	private SocialController socializer = null;
	private StrategyScoreComputer scoreComputer = null;
	private RuleBasedTriggerComputer triggerComputer = null;
//	private FeedbackActor feedbacker = null;
	private AgreeDisagreeActor agreer = null;
	private MissingTopicReactor reminder = null;
	
	private String condition;
	private boolean doSocial, doFeedback, doRevoice, doAgree, doRemind;

	public ConditionalListenStepHandler()
	{
		condition = System.getProperty("basilica2.agents.condition", "feedback revoice agree remind social");

		doFeedback = condition.contains("feedback");
		doRevoice = condition.contains("revoice");
		doSocial = condition.contains("social");
		doAgree = condition.contains("agree");
		doRemind = condition.contains("remind");
	}
	
	@Override
	public void execute(Step currentStep, final PlanExecutor overmind, InputCoordinator source)
	{
		Logger.commonLog("ConditionalListenStep", Logger.LOG_NORMAL, "starting facilitation step with condition "+condition);

		Agent a = overmind.getAgent();
		if (doSocial && socializer == null)
		{
			socializer = new SocialController(a);
			scoreComputer = new StrategyScoreComputer(a);
			triggerComputer = new RuleBasedTriggerComputer(a);
		}
		if(doRevoice && revoicer == null)
		{
			revoicer = new RevoiceActor(a);
		}
		
		if(doRemind && reminder == null)
		{
			reminder = new MissingTopicReactor(a);
		}
		
		if(doAgree && agreer == null)
		{
			agreer = new AgreeDisagreeActor(a);
		}
//		
//		if(doFeedback && feedbacker == null)
//		{
//			feedbacker = new FeedbackActor(a);
//		}

		// revoicer.setDelegate(new EndStepOnStopListening(overmind,
		// currentStep.name));

//		if (doFeedback) overmind.addHelper(feedbacker);
		if (doRevoice)  overmind.addHelper(revoicer);
		if (doRemind) overmind.addHelper(reminder);
		if (doAgree) overmind.addHelper(agreer);
		if (doSocial)
		{
			overmind.addHelper(scoreComputer);
			overmind.addHelper(triggerComputer);
			overmind.addHelper(socializer);
		}

		if(currentStep.attributes.containsKey("duration"))
			new Timer(Long.parseLong(currentStep.attributes.get("duration")), null, new TimeoutAdapter()
			{
				@Override
				public void timedOut(String id)
				{
					overmind.stepDone();
				}
				
			}).start();
	}

}
