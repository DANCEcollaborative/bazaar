package basilica2.accountable.listeners;

import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class AskForRestateActor extends AbstractAccountableActor
{
	public AskForRestateActor(Agent a)
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
	 */
	@Override
	public boolean shouldTriggerOnCandidate(MessageEvent me)
	{

//		int allExplanations = RollingWindow.sharedWindow().countEvents(windowSize, "EXPLANATION_CONTRIBUTION");
//		int allQuestions = RollingWindow.sharedWindow().countEvents(windowSize, "QUESTION");
		
		int myTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn");
		int allTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn");
		int myCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn", candidateLabel);
		int allCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn", "REVOICABLE");
		
		//double questionToExp = allExplanations / ((double) allQuestions + 1);
		double groupRevoicables = (allCandidates - myCandidates)/(double)Math.max(1, allTurns - myTurns);
		
		log(Logger.LOG_NORMAL, me.getFrom()+" "+candidateLabel+" ratio is "+groupRevoicables);
		
		return groupRevoicables < targetRatio;
	}

	@Override
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{

		return !me.hasAnnotations("QUESTION") && !me.getText().contains("?");
	}
	
}
