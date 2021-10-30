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
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.util.PropertiesLoader;
import basilica2.agents.listeners.plan.MatchStepHandler;
import basilica2.agents.data.PromptTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
	private boolean use_catch_up = false; //whether to use the catch_up_message function
	private PromptTable catch_up_prompter;
	private String[] catch_up_stages;
	private String[] catch_up_steps;

	public PresenceWatcher(Agent a)
	{
		super(a);

		String name = a.getName();
		int underscore = name.indexOf("_");
		if (underscore > -1)
			agent_name = name.substring(0, underscore);
		else
			agent_name = name;
		
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");

		if (properties != null)
		{
			use_catch_up = Boolean.parseBoolean(properties.getProperty("use_catch_up", "false"));
			launch_timeout = Integer.parseInt(properties.getProperty("launch_timeout", "60"));
			expected_number_of_students = Integer.parseInt(properties.getProperty("expected_number_of_students", "1"));
			if (use_catch_up) {
				catch_up_stages = properties.getProperty("catch_up_stages", "").split("[\\s,]+");
				catch_up_steps = properties.getProperty("catch_up_steps", "").split("[\\s,]+");
				String promptsPath = properties.getProperty("prompt_file","plans/catch_up_prompts.xml");
				catch_up_prompter = new PromptTable(promptsPath);
			}
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
				if (use_catch_up) {
					// Send catch-up message only when the agent sees this student for the first time 
					// and the agent is at a stage and a step that allow sending catch-up message (predefined in the properties)
					boolean catchup = false;
					if ((!news.getAllStudentNames().contains(pe.getUsername())) && Arrays.asList(catch_up_stages).contains(news.getStageName()) && Arrays.asList(catch_up_steps).contains(news.getStepName())) {
						catchup = true;
					}
					news.addStudent(pe.getUsername());
					if (catchup) {
						sendCatchUpMessage(source, news, pe);
						NewRoleAssignment(source, news, pe);
						
					}
				}else {
					news.addStudent(pe.getUsername());
				}
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
		// Start as soon as agent is present if not waiting for students
		else if ((source.isAgentName(pe.getUsername())) && expected_number_of_students == 0)
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
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"AGENT PRESENT");
				StateMemory.commitSharedState(news, agent);
				initiate(source, news);

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
	
	private void sendCatchUpMessage(InputCoordinator source, State news, PresenceEvent pe) {
		// Send private message to the new student. The private message prompts are defined in catch_up_prompter
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Send catch up message to " + pe.getUsername());
		
		Map<String, String> slots = new HashMap<String, String>();
		slots.put("[NAME]", pe.getUsername());
		String prompt_name = news.getStageName() + "_" + news.getStepName();
		String prompt_text = catch_up_prompter.lookup(prompt_name, slots);
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Catch Up Message: " + prompt_name + " " + prompt_text);
		// This private message event has low priority and large timeout, so it will be sent after public messages
		Event e = new PrivateMessageEvent(source, pe.getUsername(), getAgent().getName(), prompt_text, "CATCHUPMESSAGE");
		double p = 0.25;
		double timeout = 100;
		source.addEventProposal(e, p, timeout);
		
	}
	
	private void NewRoleAssignment(InputCoordinator source, State news, PresenceEvent pe) {
		// Assign a role to the new student following the same logic in MatchStepHandler/RotateStepHandler 
		// and broadcast the message to the whole group.
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"NewRoleAssignment");
		
		// Load necessary properties for role assignment from MatchStepHandler
		// Please refer to MatchStepHandler.properties for the meaning of the variables
		Properties match_properties = PropertiesLoader.loadProperties(MatchStepHandler.class.getSimpleName() + ".properties");
		String[] match_roles = match_properties.getProperty("roles", "").split("[\\s,]+");
		int num_roles = match_roles.length;
		for (int i=0; i< num_roles; i++) {
			match_roles[i] = match_roles[i].replace("_", " "); 
		}
		String default_role = match_properties.getProperty("default_role", "").replace("_", " ");
		Boolean match_multiple_to_default = Boolean.parseBoolean(match_properties.getProperty("match_multiple_to_default", "true"));
		int min_users_to_match = Integer.parseInt(match_properties.getProperty("min_users_to_match", "2"));
		
		// Get present students, including the new student
		String[] student_ids = news.getStudentIds(); 
		int num_students = student_ids.length;
		String prompt_text = new String();
		
		if (num_students<min_users_to_match) {
			return ;
		}
		if (num_students==min_users_to_match) { // start role assignment for the whole group
			String prompt_name = "PROMPT_ROLE_MATCH";
			prompt_text = catch_up_prompter.match(prompt_name, student_ids, match_roles, num_students, news);
			
		}else {
			if (num_students<=num_roles) { // the new student will take a new role
				Map<String, String> slots = new HashMap<String, String>();
				slots.put("[NAME]", pe.getUsername());
				slots.put("[ROLE]", match_roles[num_students-1]);
				String prompt_name = "PROMPT_ROLE_MATCH_SINGLE";
				prompt_text = catch_up_prompter.lookup(prompt_name, slots);
				
				news.setStudentRole(pe.getUsername(), match_roles[num_students-1]);
			}else {
				if (match_multiple_to_default) { // the new student will take the default role
					Map<String, String> slots = new HashMap<String, String>();
					slots.put("[NAME]", pe.getUsername());
					slots.put("[ROLE]", default_role);
					String prompt_name = "PROMPT_ROLE_MATCH_SINGLE";
					prompt_text = catch_up_prompter.lookup(prompt_name, slots);
					news.setStudentRole(pe.getUsername(), default_role);
				}
			}
		}
		if (prompt_text.length()>0) {
			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"prompt_text: " + prompt_text);
			Event e = new MessageEvent(source, getAgent().getName(), prompt_text, "NEWROLEASSIGNMENT");
			double p = 0.5;
			double timeout = 100;
			source.addEventProposal(e, p, timeout);
		}
	}
}
