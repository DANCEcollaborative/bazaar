package basilica2.myagent.listeners;

import java.util.Arrays;
import java.util.List;

import basilica2.accountable.listeners.AbstractAccountableActor;
//import basilica2.myagent.listeners.AbstractAccountableActor;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.TypingEvent;
import basilica2.agents.events.WhiteboardEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.util.TimeoutAdapter;
import dadamson.words.ASentenceMatcher.SentenceMatch;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class QuestionActor extends AbstractAccountableActor
{
	
	private String altCandidateLabel = candidateLabel; 
	
	public QuestionActor(Agent a)
	{
		super(a); 
		altCandidateLabel = properties.getProperty("alternative_candidate_label", altCandidateLabel);
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
		System.err.println("AgreeDisgreeActor, enter shouldTriggerOnCandidate");
		int myTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn");
		int allTurns = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn");
		int myCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, me.getFrom()+"_turn", candidateLabel);
		int allCandidates = RollingWindow.sharedWindow().countEvents(ratioWindowTime, "student_turn", candidateLabel);
		
		double ratio = (allCandidates - myCandidates) /(double)Math.max(1, allTurns - myTurns);
		log(Logger.LOG_NORMAL, me.getFrom()+"'s " +candidateLabel+ " ratio is "+ratio);
		
		// DO NOT TRIGGER IF WORD COUNT IS TOO LOW OR TOO HIGH
		Integer wordCount = getWordCount(me.getText()); 
		if (wordCount < wordCountMin) {
			System.err.println("QuestionActor, shouldTriggerOnCandidate = false: wordCount < wordCountMin");
			return false; 
		}	
		if ((wordCountMax != -1) && (wordCount > wordCountMax)) {
			System.err.println("QuestionActor, shouldTriggerOnCandidate = false: wordCount > wordCountMax");
			return false; 
		}		
		
		String match = topicWordMatch(me.getText());
		if (match == null) {
			return false; 
		}

		System.err.println("QuestionActor, shouldTriggerOnCandidate = true");
		// return ratio < targetRatio;
		return true; 
	}

	@Override
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{
		System.err.println("QuestionActor, enter shouldAnnotateAsCandidate"); 
		String text = me.getText(); 
		Integer wordCount = getWordCount(text);
		if (wordCount < wordCountMin) {
			System.err.println("QuestionActor, shouldAnnotateAsCandidate = false"); 
			return false; 
		}
		if ((me.hasAnnotations("QUESTION")) || (me.getText().contains("?"))) {
			System.err.println("QuestionActor, shouldAnnotateAsCandidate = true"); 
			return true; 
		}
		
		String match = topicWordMatch(text);
		if (match == null) {
			return false; 
		}

		//System.out.println("ADA: "+shouldAnnotate + " <-- "+me);
		System.err.println("QuestionActor, shouldAnnotateAsCandidate = true"); 
		// System.err.println("AgreeDisagreeActor, exit shouldAnnotateAsCandidate"); 
		return true;
	}
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}
	
}
