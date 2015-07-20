/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.social.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
//import basilica2.social.components.TriggerInfoMemory;
import basilica2.social.data.PerformanceInfo;
import basilica2.social.data.RulesInfo;
import basilica2.social.data.StrategyScores;
import basilica2.social.data.TriggerInfo;
import basilica2.social.data.TurnCounts;
//import basilica2.social.events.CheckBlockingEvent;
import basilica2.social.events.DormantGroupEvent;
import basilica2.social.events.DormantStudentEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.social.events.SocialTriggerEvent;
import java.util.Map;

/**
 * 
 * @author rohitk
 */
public class RuleBasedTriggerComputer extends BasilicaAdapter
{

	public static String GENERIC_NAME = "RuleBasedTriggerComputer";
	public static String GENERIC_TYPE = "Computer";
	private double social_ratio_threshold = 0.15;
	private String previousDiscourseState = "__NONE__";
	private InputCoordinator source;

	private String status = "?";

	// Strategy Scores
	StrategyScores currentStrategyScores = new StrategyScores();

	public RuleBasedTriggerComputer(Agent a)
	{
		super(a);

		try
		{
			this.social_ratio_threshold = Double.parseDouble(getProperties().getProperty("socialthreshold"));
		}
		catch (Exception e)
		{
			System.err.println("using default value of " + social_ratio_threshold + " for Social Ratio Threshold.");
		}
	}

	private void handleMessageEvent(MessageEvent me)
	{
		String[] givingOrientation = me.checkAnnotation("GIVING_ORIENTATION");
		String[] givingOpinion = me.checkAnnotation("GIVING_OPINION");
		String[] ideaContribution = me.checkAnnotation("IDEA_CONTRIBUTION");
		String[] groupBonding = me.checkAnnotation("GROUP_BONDING");
		String[] abuse = me.checkAnnotation("ABUSE");
		String[] sillyness = me.checkAnnotation("SILLYNESS");
		String[] smiles = me.checkAnnotation("SMILES");
		String[] positivity = me.checkAnnotation("POSITIVITY");
		String[] helpRequest = me.checkAnnotation("HELP_REQUEST");
		String[] discontent = me.checkAnnotation("DISCONTENT");
		String[] teasing = me.checkAnnotation("TEASING");
		String[] tutorError = me.checkAnnotation("TUTOR_ERROR");

		if ((teasing != null) || (abuse != null) || (sillyness != null) || (discontent != null) || (helpRequest != null) || (smiles != null)
				|| (positivity != null) || (groupBonding != null) || (ideaContribution != null) || (givingOrientation != null) || (givingOpinion != null)
				|| (tutorError != null))
		{
			trigger("MB");
		}
	}

	// private void handleCheckBlockingEvent(CheckBlockingEvent cbe) {
	// previousDiscourseState =
	// ModelBasedTriggerComputer.mapStateF09ToModel(cbe.getDoneStep());
	// if
	// (previousDiscourseState.equalsIgnoreCase("DO_CALCULATIONS1_CONFIRM_TORQUE"))
	// {
	// trigger("3a");
	// } else if
	// (previousDiscourseState.equalsIgnoreCase("DO_CALCULATIONS1_CONFIRM_COST_PRICE"))
	// {
	// trigger("3a");
	// } else if
	// (previousDiscourseState.equalsIgnoreCase("DO_CALCULATIONS2_CONFIRM_COST"))
	// {
	// trigger("3a");
	// } else if (previousDiscourseState.equalsIgnoreCase("REVIEW_DESIGN2")) {
	// trigger("3cd");
	// } else if (previousDiscourseState.startsWith("CONCEPT")) {
	// trigger("2c");
	// }
	// }

	private void handleDormantGroupEvent(DormantGroupEvent dge)
	{
		if(System.getProperty("basilica2.agents.condition", "").contains("participation"))
			trigger("2e");
	}

	private void handleDormantStudentEvent(DormantStudentEvent dse)
	{
		if(System.getProperty("basilica2.agents.condition", "").contains("participation"))
			trigger("2d");
	}

	private void trigger(String behavior)
	{
		double sr = getCountRatio();
		if (sr < social_ratio_threshold)
		{
			status = behavior;
			RulesInfo ri = new RulesInfo(behavior);
			TriggerInfo ti1 = new TriggerInfo(ri);
			// getTriggerInfoMemory().commit(ti1);

			PerformanceInfo pi = new PerformanceInfo(behavior, TriggerInfo.TRIGGER_RULES, 1.0);
			TriggerInfo ti2 = new TriggerInfo(pi);
			// getTriggerInfoMemory().commit(ti2);

			source.queueNewEvent(new SocialTriggerEvent(source, TriggerInfo.TRIGGER_RULES, 1.0));
		}
		else
		{
			// this.informObservers("Trigger: " + behavior +
			// " denied because of SocialRatio=" + sr);
		}
	}

	public String getStatus()
	{
		return status;
	}

	private double getCountRatio()
	{
		TurnCounts tc = (TurnCounts) StateMemory.getSharedState(agent).more().get("TurnCounts");
		if (null == tc)
			return 0;
		else
			return (double) tc.getSocialTurnCount() / (double) tc.getTotalTurnCount();
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		this.source = source;
		if (e instanceof MessageEvent)
		{
			handleMessageEvent((MessageEvent) e);
		}
		else if (e instanceof DormantStudentEvent)
		{
			handleDormantStudentEvent((DormantStudentEvent) e);
		}
		else if (e instanceof DormantGroupEvent)
		{
			handleDormantGroupEvent((DormantGroupEvent) e);
		}
		// else if (e instanceof CheckBlockingEvent) {
		// handleCheckBlockingEvent((CheckBlockingEvent) e);
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { MessageEvent.class, DormantStudentEvent.class, DormantGroupEvent.class };
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		// TODO Auto-generated method stub
		return new Class[] {};
	}
}
