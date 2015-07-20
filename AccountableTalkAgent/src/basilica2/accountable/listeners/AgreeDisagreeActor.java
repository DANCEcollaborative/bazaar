package basilica2.accountable.listeners;

import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class AgreeDisagreeActor extends AbstractAccountableActor
{
	public AgreeDisagreeActor(Agent a)
	{
		super(a); //all the variance is in the properties file
	}

	//second-tier response - the core AT move isn't needed because the discussion is productive enough - check for secondary opportunity
	@Override
	public void performFollowupCheck(final MessageEvent event)
	{
		if (RollingWindow.sharedWindow().countAnyEvents(candidateWindow - WINDOW_BUFFER, "DISAGREEMENT", "CHALLENGE_CONTRIBUTION") > 0)
		{

			MessageEvent responseEvent = getResponseEvent(event.getFrom(), "DISAGREEMENT", "CHALLENGE_CONTRIBUTION");

			if (responseEvent != null) 
			{
				String challenger = responseEvent.getFrom();
				makeFeedbackProposal(challenger, "", responseEvent, accountablePrompts, "REQUEST_DISAGREE_EXPLANATION", 0.2);
			}
		}

		else
		{
			MessageEvent responseEvent = getResponseEvent(event.getFrom(), "AGREEMENT");
			if (responseEvent != null) 
			{
				String challenger = responseEvent.getFrom();
				makeFeedbackProposal(challenger, "", responseEvent, accountablePrompts, "REQUEST_EXPLANATION", 0.2);
			}
		}
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
		int myTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn");
		int allTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn");
		int myCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn", candidateLabel);
		int allCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn", candidateLabel);
		
		double ratio = (allCandidates - myCandidates) /(double)Math.max(1, allTurns - myTurns);
		log(Logger.LOG_NORMAL, me.getFrom()+"'s" +candidateLabel+ " ratio is "+ratio);
		
		return ratio < targetRatio;
	}

	@Override
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{
		boolean shouldAnnotate = !me.hasAnnotations("QUESTION") && !me.getText().contains("?");
		//System.out.println("ADA: "+shouldAnnotate + " <-- "+me);
		return shouldAnnotate;
	}
	
}
