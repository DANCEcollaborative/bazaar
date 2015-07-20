package basilica2.accountable.listeners;

import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class PressForReasoningActor extends AbstractAccountableActor
{
	public PressForReasoningActor(Agent a)
	{
		super(a); //all the variance is in the properties file
	}

	//second-tier response - the core AT move isn't needed because the discussion is productive enough - check for secondary opportunity
	@Override
	public void performFollowupCheck(final MessageEvent event)
	{

	}

	/**
	 * additional check for move candidate
	 * Goal 4: help students think with others - group RB is low/imbalanced (how low?)
		    Agree/Disagree - match is low
		    Add on - match is low
		    Explain Other - match is high
	 */
	@Override
	public boolean shouldTriggerOnCandidate(MessageEvent me)
	{

		if(me.hasAnnotations("EXPLANATION_CONTRIBUTION"))
			return false;
		
		int allExplanations = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "EXPLANATION_CONTRIBUTION");
		
//		int myTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn");
		int allTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn");
//		int myCandidates = RollingWindow.sharedWindow().countEvents(windowSize, me.getFrom()+"_turn", candidateLabel);
//		int allCandidates = RollingWindow.sharedWindow().countEvents(windowSize, "student_turn", "REVOICABLE");
		
		
		double ratio = allExplanations/(double)(1.0 + allTurns);
		log(Logger.LOG_NORMAL, me.getFrom()+" "+candidateLabel+" ratio is "+ratio);
		
		return ratio < targetRatio;
	}

	@Override
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{
		return !me.hasAnnotations("QUESTION") && !me.getText().contains("?");
	}
	
}
