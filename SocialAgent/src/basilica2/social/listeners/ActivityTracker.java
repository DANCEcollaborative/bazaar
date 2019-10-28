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

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.social.events.DormantGroupEvent;
import basilica2.social.events.DormantStudentEvent;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;

import java.util.Hashtable;
import java.util.Map;

/**
 * 
 * @author rohitk + dadamson
 */
public class ActivityTracker extends BasilicaAdapter implements TimeoutReceiver
{

	public static String GENERIC_NAME = "ActivityTracker";
	public static String GENERIC_TYPE = "Filter";
	private double activity_prompt_pulse = 3;
	//private double chat_time=10; //no bazaar after 10 minutes;
	
	private int group_activity_min_threshold = 0; // Less than 2 turns in 3
													// minutes is bad!
	private Map<String, Integer> messageCounts;
	private int totalMessages = 0;
	private boolean shouldTrack = true;
	private InputCoordinator source;
	private String status = "";
	private boolean isTracking;

	public ActivityTracker(Agent a)
	{
		super(a);
		messageCounts = new Hashtable<String, Integer>();
	}

	public void setTrackMode(boolean m)
	{
		shouldTrack = m;
	}

	public String getStatus()
	{
		return status;
	}

	private void handleLaunchEvent(LaunchEvent le)
	{
		startTracking();
	}

	private void startTracking()
	{
		if (shouldTrack)
		{

			State s = StateMemory.getSharedState(agent);
			String[] sids = s.getStudentIds();
			for (String sid : sids)
			{
				messageCounts.put(sid, 0);
			}
			isTracking = true;

			Timer t = new Timer(activity_prompt_pulse * 60, this);
			t.start();
		}
	}

//	private void startTrackingofWholeChat(){
//		Timer t= new Timer(chat_time, this);
//		t.start();
//	}
	private void handleMessageEvent(MessageEvent me)
	{
//		startTrackingofWholeChat();// track the time of the whole chat
		
		if (!isTracking && shouldTrack) startTracking();
		String from = me.getFrom();
		Integer count = messageCounts.get(from);
		if (count == null)
		{
			Map<String, Integer> newMCs = new Hashtable<String, Integer>();
			State s = StateMemory.getSharedState(agent);
			String[] sids = s.getStudentIds();
			s.addStudent(from);
			StateMemory.commitSharedState(s, getAgent());
			for (int i = 0; i < sids.length; i++)
			{
				Integer mc = messageCounts.get(sids[i]);
				if (mc == null)
				{
					mc = 0;
				}
				newMCs.put(sids[i], mc);
			}
			messageCounts = newMCs;
		}
		count = messageCounts.get(from);
		if (count == null)
		{
			messageCounts.put(from, 1);
		}
		else
		{
			count++;
			messageCounts.put(from, count);
		}
		totalMessages++;
	}

	public void timedOut(String id)
	{
		// informObservers("<timedout id=\"" + id + "\" />");
		// Find the person with the lowest number of messages since last pulse
        System.out.println("Dormant");
		String[] participants = messageCounts.keySet().toArray(new String[0]);
		if(participants == null)
		{
			log(Logger.LOG_WARNING, "Participants list is empty!");
			System.out.println("empty Dormant");
			participants = new String[0];
		}
		if (participants.length > 0)
		{
			int maxCount = 0, minCount = 100;
			String minCountParticipant = participants[(int) (Math.random()*participants.length)];
			
			for (int i = 0; i < participants.length; i++)
			{
				int count = messageCounts.get(participants[i]);
				if (maxCount < count)
				{
					maxCount = count;
				}
				if (minCount > count)
				{
					minCount = count;
					minCountParticipant = participants[i];
				}
			}
			System.out.println("Dormant");
			if (totalMessages <= group_activity_min_threshold)
			{
				System.out.println("Dormant Group");
				DormantGroupEvent dge = new DormantGroupEvent(source);
				source.queueNewEvent(dge);
				status = "dormant=GROUP," + status;
			}
			else if ((minCount * 2) < maxCount)
			{
					System.out.println("Dormant Student");	
					DormantStudentEvent dse = new DormantStudentEvent(source, minCountParticipant);
					source.queueNewEvent(dse);
					status = "dormant=" + source + "," + status;

					log(Logger.LOG_NORMAL, minCountParticipant+" is dormant ("+minCount+" turns vs student max "+maxCount+")");
				
			}

			totalMessages = 0;
			messageCounts.clear();
			startTracking();
		}
	}

	public void log(String from, String level, String msg)
	{
		agent.log(level, from, msg);
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Class[] getListenerEventClasses()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		this.source = source;
		if (e instanceof LaunchEvent)
		{
			handleLaunchEvent((LaunchEvent) e);
		}
		else if (e instanceof MessageEvent)
		{
			handleMessageEvent((MessageEvent) e);
		}

	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { MessageEvent.class, LaunchEvent.class };
	}
}
