package basilica2.myagent.listeners;

import basilica2.accountable.listeners.AbstractAccountableActor;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class RevoiceActor extends AbstractAccountableActor
{

	public RevoiceActor(Agent a)
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
		System.err.println("ClimateChange RevoiceActor, enter shouldTriggerOnCandidate"); 
		int myTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn");
		int allTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn");
		int myCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn", candidateLabel);
		int allCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn", candidateLabel);

		double ratio = (myCandidates-1)/(double)(myTurns + 1);
		log(Logger.LOG_NORMAL, me.getFrom()+"'s " +candidateLabel+ " ratio is "+ratio);
		
		return ratio/(double)myTurns <= targetRatio;
	}

	@Override //already matches - additional checks go here.
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{
		System.err.println("ClimateChange RevoiceActor, enter shouldAnnotateAsCandidate"); 
		return !me.hasAnnotations("QUESTION") && !me.getText().contains("?");
	}
	
}
