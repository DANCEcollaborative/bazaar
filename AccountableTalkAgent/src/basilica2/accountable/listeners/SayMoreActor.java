package basilica2.accountable.listeners;

import basilica2.agents.components.StateMemory;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class SayMoreActor extends AbstractAccountableActor
{
	public SayMoreActor(Agent a)
	{
		super(a); //all the variance is in the properties file
	}

	//second-tier response - the core AT move isn't needed because the discussion is productive enough - check for secondary opportunity
	@Override
	public void performFollowupCheck(final MessageEvent event)
	{
		//maybe check for revoicing mismatch?
	}

	@Override 
	/**
	 * Goal 1: help students clarify their own reasoning - self RB is low
     *	Revoice - match is high
     *	Say More - match is low
	 */
	public boolean shouldTriggerOnCandidate(MessageEvent me)
	{
		int myTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn");
//		int allTurns = RollingWindow.sharedWindow().countEvents(windowSize, "student_turn");
		int myCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn", candidateLabel);
//		int allCandidates = RollingWindow.sharedWindow().countEvents(windowSize, "student_turn", "REVOICABLE");
		
		double ratio = myCandidates/(double)myTurns;
		log(Logger.LOG_NORMAL, me.getFrom()+" "+candidateLabel+" ratio is "+ratio);
		
		return ratio < targetRatio;
	}

	@Override
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{
		return !me.hasAnnotations("QUESTION") && !me.getText().contains("?");
	}
	
}
