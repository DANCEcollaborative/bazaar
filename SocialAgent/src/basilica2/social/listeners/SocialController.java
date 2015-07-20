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
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.social.data.StrategyScores;
import basilica2.social.data.TurnCounts;
//import basilica2.social.events.CheckBlockingEvent;
//import basilica2.social.events.CompleteIntroductionsEvent;
//import basilica2.social.events.DoIntroductionsEvent;
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.StepDoneEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.AbstractPrioritySource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.social.events.DormantStudentEvent;
import basilica2.social.events.SocialTurnPerformedEvent;
//import basilica2.social.events.PromptEvent;
import basilica2.social.events.SocialTriggerEvent;
//import basilica2.social.events.TutoringBlockEvent;
//import basilica2.social.events.UnblockEvent;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.sun.tools.javac.util.Log;

/**
 * 
 * @author rohitk
 */
public class SocialController extends BasilicaAdapter implements TimeoutReceiver
{

	public static String GENERIC_NAME = "SocialController";
	public static String GENERIC_TYPE = "Filter";

	// For cordination between task & social
	private String dormantStudent = null;
	// Other control variables
	private String condition = "RULES";
	public static final String CONDITION_NONE = "NONE";
	public static final String CONDITION_RULES = "RULES";
	public static final String CONDITION_RANDOM = "RANDOM";
	public static final String CONDITION_MODEL = "MODEL";
	private InputCoordinator source;
	private PromptTable socialPrompter = null;
	private String status = "";
	private boolean readyToDoSocial = true;
	private String agentName;
	private double socialPriority;
	private double socialWindow;
	private double socialBlackout;
	private boolean participate;
	private boolean doSocial;

	public SocialController(Agent a)
	{
		super(a);
		String socialString = System.getProperty("social_condition", "social");
		String participationString = System.getProperty("participation_condition", "participation");

		socialPrompter = new PromptTable(properties.getProperty("prompt_file", "social_prompts.xml"));
		agentName = a.getUsername();
		socialPriority = Double.parseDouble(properties.getProperty("priority", "0.1"));
		socialWindow = Double.parseDouble(properties.getProperty("window", "3"));
		socialBlackout= Double.parseDouble(properties.getProperty("blackout", "5"));
		
		participate = System.getProperty("basilica2.agents.condition", "").contains(participationString);
		doSocial = System.getProperty("basilica2.agents.condition", "").contains(socialString);
	}

	private void updateBehaviorLog(String behavior)
	{
		try
		{
			String filename = "behaviors" + File.separator + "tutor" + File.separator + getAgent().getName() + ".tutorbehaviors.txt";
			FileWriter fw = new FileWriter(filename, true);
			fw.write(getAgent().getName() + "\t" + behavior + "\n");
			fw.flush();
			fw.close();
		}
		catch (Exception e)
		{
			log(Logger.LOG_ERROR, "Error while updating Status File (" + e.toString() + ")");
		}
	}

	public void setCondition(String c)
	{
		condition = c;
	}

	private void handleDormantStudentEvent(DormantStudentEvent dse)
	{
		dormantStudent = dse.getStudentName();
	}

	private void handleSocialTriggerEvent(SocialTriggerEvent ste)
	{
		log(Logger.LOG_NORMAL, "received social trigger event " + ste);
		// if (ste.getFrom().equalsIgnoreCase(this.condition))
		{
			if (readyToDoSocial)
			{ // To prevent any social behavior before introductions are
				// completed
				// Just sleep for 2 seconds to ensure that the StrategyScores
				// are computed
				Timer t = new Timer(2.0, "WAIT_BEFORE_PERFORMING", this);
				t.start();
			}
		}
		// else
		// {
		// //informObservers("Ignoring Trigger from Unmatching Condition");
		// }
	}

	private String doRouletteWheelSelection(String[] ids, double[] shares)
	{
		boolean[] hasShare = new boolean[ids.length];
		int nHaveShare = 0;
		double shareSum = 0.0;
		String selected = null;
		for (int i = 0; i < ids.length; i++)
		{
			if (shares[i] == 0.0)
			{
				hasShare[i] = false;
			}
			else
			{
				hasShare[i] = true;
				nHaveShare++;
				shareSum += shares[i];
			}
			log(Logger.LOG_LOW, "Roulette: Id=" + ids[i] + " Share=" + shares[i]);
		}
		if (nHaveShare == 0)
		{
			log(Logger.LOG_LOW, "Roulette: No one has share");
			return null;
		}
		else if (nHaveShare == 1)
		{
			for (int i = 0; i < ids.length; i++)
			{
				if (hasShare[i])
				{
					log(Logger.LOG_LOW, "Roulette: Only " + ids[i] + " has share");
					return ids[i];
				}
			}
		}
		else
		{
			double random = Math.random() * shareSum;
			double sum = 0.0;
			for (int i = 0; i < ids.length; i++)
			{
				if (hasShare[i])
				{
					sum += shares[i];
					log(Logger.LOG_LOW, "Roulette: Random=" + random + " Consider:" + ids[i] + " Share=" + shares[i] + " ShareSum:" + sum);
					if (random <= sum)
					{
						log(Logger.LOG_LOW, "Roulette: " + ids[i] + " is selected");
						return ids[i];
					}
				}
			}
		}
		return selected;
	}

	public String getStatus()
	{
		return status;
	}

	private void proposeSocialPrompt(final String promptKey, Map<String, String> slots)
	{
		log(Logger.LOG_NORMAL, "proposing social prompt: "+promptKey);
		status = promptKey + ", " + status;
		final String message = socialPrompter.lookup(promptKey, slots);
		MessageEvent me = new MessageEvent(source, agentName, message, "SOCIAL", promptKey);
		PriorityEvent pete = PriorityEvent.makeBlackoutEvent("Social", me, socialPriority, socialWindow, socialBlackout);

		pete.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p)
			{
				log(Logger.LOG_NORMAL, "accepted social prompt: "+promptKey);
				incrementSocialCount();
				source.queueNewEvent(new SocialTurnPerformedEvent(source, message));
			}

			@Override
			public void rejected(PriorityEvent p)
			{
				log(Logger.LOG_NORMAL, "rejected social prompt: "+promptKey);
			}
		});
		source.pushProposal(pete);
	}

	private void performBehavior()
	{
		State s = StateMemory.getSharedState(agent);
		StrategyScores scores = (StrategyScores) s.more().get("StrategyScores");
		if (scores == null) scores = new StrategyScores();

		// informObservers("<PerformingBehavior>" + scores.toString() +
		// "</PerformingBehavior>");
		boolean performRandom = true;
		boolean usedStateChanged = false;
		boolean usedDormantStudent = false;
		boolean usedDormantGroup = false;
		boolean usedNewMessage = false;

		if (scores.hasStateChanged())
		{
			// Choose between 3a, 3cd, 2c
			String[] ids = new String[3];
			double[] shares = new double[3];
			ids[0] = "3a";
			shares[0] = scores.get3aScore() * 0.4;
			ids[1] = "3cd";
			shares[1] = scores.get3cdScore() * 0.4;
			ids[2] = "2c";
			shares[2] = scores.get2cScore() * 0.2;
			String selectedStrategy = doRouletteWheelSelection(ids, shares);
			// informObservers("State Change Roulette Selected: " +
			// selectedStrategy);
			if (selectedStrategy == null)
			{
				performRandom = true;
			}
			else if (selectedStrategy.equals("3a"))
			{
				performRandom = false;
				usedStateChanged = true;
				perform3a();
			}
			else if (selectedStrategy.equals("3cd"))
			{
				performRandom = false;
				usedStateChanged = true;
				perform3cd();
			}
			else if (selectedStrategy.equals("2c"))
			{
				performRandom = false;
				usedStateChanged = true;
				perform2c();
			}
		}

		if(participate)
		{
			if (performRandom && scores.isGroupDormant())
			{
				performRandom = false;
				usedDormantGroup = true;
				perform2e();
			}
	
			if (performRandom && scores.isStudentDormant())
			{
				performRandom = false;
				usedDormantStudent = true;
				perform2d();
			}
		}
		
		if (performRandom && scores.isNewMessage())
		{
			// Choose between 2a, 2b, 2f, 2g, 4a, 4b, 6, 7
			String[] ids = new String[8];
			double[] shares = new double[8];
			ids[0] = "2a";
			shares[0] = scores.get2aScore() * 0.1; // Multiplied by relative
													// priority of these moves
			ids[1] = "2b";
			shares[1] = scores.get2bScore() * 0.1;
			ids[2] = "2f";
			shares[2] = scores.get2fScore() * 0.1;
			ids[3] = "2g";
			shares[3] = scores.get2gScore() * 0.1;
			ids[4] = "4a";
			shares[4] = scores.get4aScore() * 0.25;
			ids[5] = "4b";
			shares[5] = scores.get4bScore() * 0.25;
			ids[6] = "6";
			shares[6] = scores.get6Score() * 0.05;
			ids[7] = "7";
			shares[7] = scores.get7Score() * 0.05;
			String selectedStrategy = doRouletteWheelSelection(ids, shares);
			// informObservers("New Message Roulette Selected: " +
			// selectedStrategy);

			if (selectedStrategy == null)
			{
				performRandom = true;
			}
			else if (selectedStrategy.equals("2a"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("BE_PROTECTIVE", "Friendly:Strategy2a", true);
			}
			else if (selectedStrategy.equals("2b"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("REASSURE", "Friendly:Strategy2b", false);
			}
			else if (selectedStrategy.equals("2f"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("SMILE", "Friendly:Strategy2f", true);
			}
			else if (selectedStrategy.equals("2g"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("PARTICIPATE_HAPPY", "Friendly:Strategy2g", true);
			}
			else if (selectedStrategy.equals("4a"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("SHOW_ATTENTION_ENCOURAGEMENT", "Agreeing:Strategy4a", false);
			}
			else if (selectedStrategy.equals("4b"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("SHOW_COMPREHENSION_APPROVAL", "Agreeing:Strategy4b", false);
			}
			else if (selectedStrategy.equals("6"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("BE_ANTAGONIST", "BeAntagonist:Strategy6", true);
			}
			else if (selectedStrategy.equals("7"))
			{
				performRandom = false;
				usedNewMessage = true;
				performSocialPrompt("APOLOGIZE", "Apologize:Strategy7", true);
			}
		}

		if (performRandom)
		{
			// log(Logger.LOG_WARNING, "NO Strategy Selected!");
			// If none are applicable, do a Generic Social move (only in
			// Random/Model conditions)
			updateBehaviorLog("Random:StrategyNone");
			this.proposeSocialPrompt("RANDOM_SOCIAL", null);
			usedNewMessage = true;
			usedDormantGroup = true;
			usedDormantStudent = true;
			usedStateChanged = true;
		}

		scores.reset(usedStateChanged, usedDormantGroup, usedDormantStudent, usedNewMessage);
		StateMemory.commitSharedState(s, agent);
	}

	private void perform3a()
	{
		updateBehaviorLog("TensionRelease:Strategy3a");
		proposeSocialPrompt("CALCULATIONS_DONE_SOCIAL", null);
	}

	private void perform3cd()
	{
		updateBehaviorLog("TensionRelease:Strategy3cd");
		proposeSocialPrompt("REVIEW_DESIGN_SOCIAL", null);
	}

	private void perform2d()
	{
		// Perform 2d
		State s = StateMemory.getSharedState(agent);
		Map<String, String> sf = new HashMap<String, String>();
		sf.put("[STUDENT]", s.getStudentName(dormantStudent == null ? "" : dormantStudent));
		updateBehaviorLog("Friendly:Strategy2d");
		proposeSocialPrompt("ENCOURAGE_INDIVIDUAL", sf);
		dormantStudent = null;

	}

	private void perform2e()
	{
		// Perform 2e
		updateBehaviorLog("Friendly:Strategy2e");
		proposeSocialPrompt("ENCOURAGE_GROUP", null);

	}

	private void perform2c()
	{
		boolean performed = false;
		State s = StateMemory.getSharedState(agent);
		TurnCounts tc = (TurnCounts) StateMemory.getSharedState(agent).more().get("TurnCounts");
		if (tc == null)
		{
			tc = new TurnCounts(0, 0);
			s.more().put("TurnCounts", tc);
		}
		String requester = tc.getLastRequester();
		if (requester != null)
		{
			Map<String, Integer> answerers = tc.getDialogAnswerer();
			Map<String, String> sf = new HashMap<String, String>();
			String[] as = answerers.keySet().toArray(new String[0]);
			if (as.length > 0)
			{
				String mostAnswerer = as[0];
				String leastAnswerer = as[0];
				int mostAnswers = answerers.get(mostAnswerer);
				int leastAnswers = answerers.get(mostAnswerer);
				int totalAnswers = 0;
				for (int i = 1; i < as.length; i++)
				{
					totalAnswers += answerers.get(as[i]);
					if (mostAnswers <= answerers.get(as[i]))
					{
						mostAnswers = answerers.get(as[i]);
						mostAnswerer = as[i];
					}
					if (leastAnswers > answerers.get(as[i]))
					{
						leastAnswers = answerers.get(as[i]);
						leastAnswerer = as[i];
					}
				}
				if (totalAnswers >= 1)
				{
					if (requester.equalsIgnoreCase(mostAnswerer))
					{
						// Praise requester
						updateBehaviorLog("Friendly:Strategy2c");
						sf.put("[STUDENT]", s.getStudentName(requester));
						performed = true;
					}
					else
					{
						// Praise requester and mostAnswerer
						updateBehaviorLog("Friendly:Strategy2c");
						sf.put("[STUDENT]", s.getStudentName(requester) + ", " + s.getStudentName(mostAnswerer));
						performed = true;
					}

				}
			}
			else
			{
				// Praise requester
				updateBehaviorLog("Friendly:Strategy2c");
				sf.put("[STUDENT]", s.getStudentName(requester));
				performed = true;
			}
			proposeSocialPrompt("PRAISE", sf);
		}
		if (!performed)
		{
			updateBehaviorLog("Friendly:Strategy2e");
			proposeSocialPrompt("ENCOURAGE_GROUP_DIALOG", null);
			performed = true;
		}
	}

	private void performSocialPrompt(String promptId, String logId, boolean ignoreBlock)
	{
		updateBehaviorLog(logId);

		proposeSocialPrompt(promptId, null);
	}

	private void incrementSocialCount()
	{
		State s = StateMemory.getSharedState(agent);
		TurnCounts tc = (TurnCounts) s.more().get("TurnCounts");
		if (tc == null)
		{
			tc = new TurnCounts(0, 0);
			s.more().put("TurnCounts", tc);
			log("SocialController", Logger.LOG_WARNING, "Turn Count is undefined. Instantiating...");

			
		}
		tc.incrementSocialCount();
		StateMemory.commitSharedState(s, agent);

		//+1 total because the actual turn hasn't registered yet through EchoEvent...
		log("SocialController", Logger.LOG_NORMAL, tc.getSocialTurnCount() + "/" + (tc.getTotalTurnCount()+1) + " social turns = " + 
				tc.getSocialTurnCount() / (double)(tc.getTotalTurnCount()+1));

	}

	private boolean isTriggerPrompt(String n)
	{
		if (n.equals("BE_ANTAGONIST")) { return true; }
		if (n.equals("BE_ANTAGONIST")) { return true; }
		if (n.equals("APOLOGIZE")) { return true; }
		if (n.equals("SHOW_COMPREHENSION_APPROVAL")) { return true; }
		if (n.equals("SHOW_ATTENTION_ENCOURAGEMENT")) { return true; }
		if (n.equals("PARTICIPATE_HAPPY")) { return true; }
		if (n.equals("SMILE")) { return true; }
		if (n.equals("REASSURE")) { return true; }
		if (n.equals("BE_PROTECTIVE")) { return true; }
		if (n.equals("ENCOURAGE_INDIVIDUAL")) { return true; }
		if (n.equals("ENCOURAGE_GROUP")) { return true; }
		return false;
	}

	public void timedOut(String id)
	{
		if (id.equalsIgnoreCase("WAIT_BEFORE_PERFORMING"))
		{
			performBehavior();
		}
	}

	public void log(String from, String level, String msg)
	{
		agent.log(from, level, msg);
	}

	@Override
	public void processEvent(InputCoordinator source, Event e)
	{
		if(doSocial)
		{
			this.source = source;
			if (e instanceof DormantStudentEvent)
			{
				handleDormantStudentEvent((DormantStudentEvent) e);
			}
			else if (e instanceof SocialTriggerEvent)
			{
				handleSocialTriggerEvent((SocialTriggerEvent) e);
			}
		}

	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		status = "";
		
		State s = StateMemory.getSharedState(agent);
		TurnCounts tc = (TurnCounts) StateMemory.getSharedState(agent).more().get("TurnCounts");
		if (tc == null)
		{
			tc = new TurnCounts(0, 0);
			s.more().put("TurnCounts", tc);
		}
	
		if(event instanceof EchoEvent)
		{
			tc.incrementTotalCount();
		}
		else
		{
		
			MessageEvent me = (MessageEvent) event;
		
	
			if (me.checkAnnotation("QUESTION") != null || me.checkAnnotation("HELP_REQUEST") != null)
			{
				tc.addRequester(me.getFrom());
				status = "req=" + me.getFrom() + "," + status;
			}
			else if (me.checkAnnotation("AFFIRMATIVE") != null || me.checkAnnotation("IDEA_CONTRIBUTION") != null
					|| me.checkAnnotation("EXPLANATION_CONTRIBUTION") != null || me.checkAnnotation("PREDICTION_CONTRIBUTION") != null)
			{
				tc.addAnswerer(me.getFrom());
				status = "ans=" + me.getFrom() + "," + status;
			}
		}

		status = "ratio "+(tc.getSocialTurnCount()  / (double) tc.getTotalTurnCount()) + ", "+status;

		log("SocialController", Logger.LOG_NORMAL, status);
		
		StateMemory.commitSharedState(s, agent);

	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { EchoEvent.class, MessageEvent.class };
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[] { DormantStudentEvent.class, SocialTriggerEvent.class };
	}
}
