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
package basilica2.agents.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.data.State;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.PresenceEvent;
import java.util.Hashtable;
import java.util.Map;

/**
 * updates student presence information in StateMemory
 * 
 * @author rohitk
 */
public class PresenceWatcher extends BasilicaAdapter
{
	private int expected_number_of_students = 1;
	private int launch_timeout = 60;
	private boolean initiated = false;
	private String agent_name = "Tutor";

	public PresenceWatcher(Agent a)
	{
		super(a);

		String name = a.getName();
		int underscore = name.indexOf("_");
		if (underscore > -1)
			agent_name = name.substring(0, underscore);
		else
			agent_name = name;

		if (properties != null)
		{
			launch_timeout = Integer.parseInt(properties.getProperty("launch_timeout", "60"));
			expected_number_of_students = Integer.parseInt(properties.getProperty("expected_number_of_students", "1"));
		}
	}

	private void handlePresenceEvent(final InputCoordinator source, PresenceEvent pe)
	{
		if (!pe.getUsername().contains(agent_name) && !source.isAgentName(pe.getUsername()))
		{
			State olds = StateMemory.getSharedState(agent);
			State news;
			if (pe.getType().equals(PresenceEvent.PRESENT))
			{
				if (olds != null)
				{
					news = State.copy(olds);
				}
				else
				{
					news = new State();
				}
				news.addStudent(pe.getUsername());
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"STUDENTS COUNT: " + news.getStudentCount());
				StateMemory.commitSharedState(news, agent);
				initiate(source, news);

			}
			else if (pe.getType().equals(PresenceEvent.ABSENT))
			{
				State updateState = State.copy(olds);
				updateState.removeStudent(pe.getUsername());
				StateMemory.commitSharedState(updateState, agent);
			}
		}
	}

	public void initiate(final InputCoordinator source, State news)
	{
		if (!initiated)
		{
			if ((news.getStudentCount() == 1) && (expected_number_of_students > 1))
			{
				log(Logger.LOG_NORMAL, "waiting "+launch_timeout+" seconds for more students to join...");
				Timer t = new Timer(launch_timeout, "LAUNCH", new TimeoutReceiver()
				{
					public void timedOut(String id)
					{
						// informObservers("<timedout id=\"" + id +
						// "\" />");
						if (!initiated)
						{
							initiateStudentState();

							LaunchEvent le = new LaunchEvent(source);
							source.queueNewEvent(le);
						}
					}

					@Override
					public void log(String from, String level, String msg)
					{
						source.writeLog(level, msg);
					}

				});
				t.start();
			}

			if (news.getStudentCount() >= expected_number_of_students)
			{
				initiateStudentState();
				LaunchEvent le = new LaunchEvent(source);
				source.pushEvent(le);
			}
		}
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		handlePresenceEvent(source, (PresenceEvent) event);
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { PresenceEvent.class };
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
		return new Class[0];
	}

	private void initiateStudentState()
	{
		State olds = StateMemory.getSharedState(getAgent());
		State news = State.copy(olds);
		news.initiate();
		initiated = true;
		StateMemory.commitSharedState(news, getAgent());
	}
}
