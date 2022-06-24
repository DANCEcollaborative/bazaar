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
import basilica2.agents.events.LogStateEvent;
import basilica2.agents.events.PresenceEvent;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import basilica2.agents.data.PromptTable;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.util.PropertiesLoader;
import basilica2.agents.listeners.plan.MatchStepHandler;
import basilica2.agents.listeners.plan.PlanExecutor;
import basilica2.agents.listeners.plan.StepHandler;
import basilica2.agents.listeners.BasilicaListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
	private String non_user_client_name = ""; 
	private Boolean includeUnderscoreInAgentName = false; 
	private Boolean sendRemoteUserList = false; 
	
	
	private boolean use_catch_up = false; //whether to use the catch_up_message function
	private PromptTable catch_up_prompter;
	private String[] catch_up_stages;
	private String[] catch_up_steps;

	public PresenceWatcher(Agent a)
	{
		super(a);
		
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		
		if (properties != null)
		{
			launch_timeout = Integer.parseInt(properties.getProperty("launch_timeout", "60"));
			expected_number_of_students = Integer.parseInt(properties.getProperty("expected_number_of_students", "1"));
			non_user_client_name = properties.getProperty("non_user_client_name", non_user_client_name);
			includeUnderscoreInAgentName = Boolean.parseBoolean(properties.getProperty("include_underscore_in_agent_name", "false"));
			sendRemoteUserList = Boolean.parseBoolean(properties.getProperty("send_remote_user_list", "false"));
			
			use_catch_up = Boolean.parseBoolean(properties.getProperty("use_catch_up", "false"));
			if (use_catch_up) {
				catch_up_stages = properties.getProperty("catch_up_stages", "").split("[\\s,]+");
				catch_up_steps = properties.getProperty("catch_up_steps", "").split("[\\s,]+");
				String promptsPath = properties.getProperty("prompt_file","plans/plan_prompts.xml");
				catch_up_prompter = new PromptTable(promptsPath);
			}
		}

		String name = a.getName();
		agent_name = name;
		if (!includeUnderscoreInAgentName) {
			int underscore = name.indexOf("_");
			if (underscore > -1)
				agent_name = name.substring(0, underscore);			
		}
		System.err.println("PresenceWatcher, agent_name: " + agent_name);
	}

	private void handlePresenceEvent(final InputCoordinator source, PresenceEvent pe)
	{
		String userName = pe.getUsername(); 
		System.err.println("PresenceEvent.java, handlePresenceEvent - username: " + userName); 
		if (!userName.contains(agent_name) && !source.isAgentName(userName) && !userName.equals(non_user_client_name)) 
		{	
			System.err.println("PresenceEvent.java, handlePresenceEvent - student present: " + userName); 
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
				boolean catchup = false;
				if (use_catch_up) {
					if ((!news.getAllStudentNames().contains(userName)) && Arrays.asList(catch_up_stages).contains(news.getStageName()) && Arrays.asList(catch_up_steps).contains(news.getStepName())) {
						catchup = true;
					}
				}
				news.addStudent(userName);
				if (catchup) {
					sendCatchUpMessage(source, news, pe);
					NewRoleAssignment(source, news, pe);
					
				}
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"STUDENTS COUNT: " + news.getStudentCount());
				if (sendRemoteUserList) {
					sendUserListToRemote(source,news); 
				}
				StateMemory.commitSharedState(news, agent);
				initiate(source, news);

			}
			else if (pe.getType().equals(PresenceEvent.ABSENT))
			{
				State updateState = State.copy(olds);
				updateState.removeStudent(userName);
				if (sendRemoteUserList) {
					sendUserListToRemote(source,updateState); 
				}
				StateMemory.commitSharedState(updateState, agent);
			}
			
		}
		// Start as soon as agent is present if not waiting for students
		else if (((source.isAgentName(userName)) || userName.equals(non_user_client_name)) && expected_number_of_students == 0)
		{
			System.err.println("PresenceEvent.java, handlePresenceEvent - NOT a student, expected_num_students=0"); 
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
				// news.addStudent(userName);
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"AGENT PRESENT");
				StateMemory.commitSharedState(news, agent);
				initiate(source, news);

			}
		}
	}
	
	private void sendUserListToRemote(final InputCoordinator source, State state) {
		String[] usersList = state.getStudentIdsPresentOrNot();
		LogStateEvent logStateEvent = new LogStateEvent(source,"users",usersList,true,"user_strobe"); 	
        System.err.println("MatchStepHandler, execute - LogStateEvent created: " + logStateEvent.toString());
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"MatchStepHandler, execute - LogStateEvent created: " + logStateEvent.toString());
		source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "LogStateEvent", logStateEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
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
		double p = OutputCoordinator.LOW10_PRIORITY;
		double timeout = 60;
		source.addEventProposal(e, p, timeout);
		
	}
	
	private void NewRoleAssignment(InputCoordinator source, State news, PresenceEvent pe) {
		// Assign a role to the new student following the same logic in MatchStepHandler/RotateStepHandler 
		// and broadcast the message to the whole group.
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"InputCoordinator NewRoleAssignment");
		
		// Find the PlanExecutor in InputCoordinator's listeners, and find the MatchStepHandler in PlanExecutor's handlers.
		for(Object keyClass : source.getListeners().keySet())
		{
			Object val = source.getListeners().get(keyClass);
			for (Object o: (List<?>) val) {
				BasilicaListener ca = BasilicaListener.class.cast(o);
				if (ca.getClass()==PlanExecutor.class) {
					Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"casted BasilicaListener "+ca+" is PlanExecutor = "+(ca.getClass()==PlanExecutor.class)+" "+ca.getListenerEventClasses());
					PlanExecutor plan_executor = PlanExecutor.class.cast(ca);
					String typestring = "match";
					Collection<StepHandler> stephandlers = plan_executor.getHandlers(typestring);
					
					for (Object obj: stephandlers) {
						if (obj.getClass()==MatchStepHandler.class) {
							Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL, "get MatchStepHandler");
							MatchStepHandler match_step_handler = MatchStepHandler.class.cast(obj);
							match_step_handler.NewRoleAssignment(source, news, pe, getAgent().getName());
							return ;
						}
					}
					
				}
			}
		}
		
	}
}
