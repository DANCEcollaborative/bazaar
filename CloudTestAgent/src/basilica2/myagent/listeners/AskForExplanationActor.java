package basilica2.myagent.listeners;

import basilica2.accountable.listeners.AbstractAccountableActor;
//import basilica2.myagent.listeners.AbstractAccountableActor;

import basilica2.accountable.listeners.AbstractAccountableActor;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class AskForExplanationActor extends AbstractAccountableActor
{
	public AskForExplanationActor(Agent a)
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
		System.err.println("AskForExplanationActor, enter shouldTriggerOnCandidate"); 
		// if(me.hasAnnotations("EXPLANATION_CONTRIBUTION"))
		//	return false;
					
		int windowSize = 20*60;
		int myTurns = RollingWindow.sharedWindow().countEvents(windowSize, me.getFrom()+"_turn");
		int allTurns = RollingWindow.sharedWindow().countEvents(windowSize, "student_turn");
		int myCandidates = RollingWindow.sharedWindow().countEvents(windowSize, me.getFrom()+"_turn", "REVOICABLE");
		int allCandidates = RollingWindow.sharedWindow().countEvents(windowSize, "student_turn", "REVOICABLE");
		
		// DO NOT TRIGGER IF WORD COUNT IS TOO LOW OR TOO HIGH
		Integer wordCount = getWordCount(me.getText()); 
		if (wordCount < wordCountMin) {
			System.err.println("AskForExplanationActor, shouldTriggerOnCandidate = false: wordCount < wordCountMin");
			return false; 
		}	
		if ((wordCountMax != -1) && (wordCount > wordCountMax)) {
			System.err.println("AskForExplanationActor, shouldTriggerOnCandidate = false: wordCount > wordCountMax");
			return false; 
		}	
		
		double ratio = (allCandidates - myCandidates) /(double)Math.max(1, allTurns - myTurns);
		log(Logger.LOG_NORMAL, "group RB ratio is "+ratio);
		
		System.err.println("AskForExplanationActor, shouldTriggerOnCandidate, ratio: " + Double.toString(ratio));

		System.err.println("AskForExplanationActor, shouldTriggerOnCandidate = true");
		// return ratio < targetRatio;
		return true; 
	}

	@Override
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{
		System.err.println("AskForExplanationActor, enter shouldAnnotateAsCandidate"); 

		// DO NOT ANNOTATE IF WORD COUNT IS TOO LOW OR TOO HIGH
		Integer wordCount = getWordCount(me.getText()); 
		if (wordCount < wordCountMin) {
			System.err.println("AskForExplanationActor, shouldAnnotateAsCandidate = false: wordCount < wordCountMin");
			return false; 
		}	
		if ((wordCountMax != -1) && (wordCount > wordCountMax)) {
			System.err.println("AskForExplanationActor, shouldAnnotateAsCandidate = false: wordCount > wordCountMax");
			return false; 
		}	
		
		// DO NOT ANNOTATE IF QUESTION
		if ((me.hasAnnotations("QUESTION")) || (me.getText().contains("?"))) {
			System.err.println("AskForExplanationActor, shouldAnnotateAsCandidate = false: this is a question"); 
			return false; 
		}

		System.err.println("AskForExplanationActor, shouldAnnotateAsCandidate = true");		
		return true; 
	}
	
}
