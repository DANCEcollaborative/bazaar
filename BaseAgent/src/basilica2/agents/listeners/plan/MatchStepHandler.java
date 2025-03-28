package basilica2.agents.listeners.plan;



import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.LogStateEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;
import basilica2.agents.listeners.plan.StepHandler;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.PlanExecutor;

public class MatchStepHandler implements StepHandler
{
	// Please refer to MatchStepHandler.properties for the meaning of the following variables
	private PromptTable prompter;
	private double wordsPerSecond = 200/60.0;
	private double constantDelay = 0.1;
	private boolean rateLimited = true;
	private double defaultPromptPriority = OutputCoordinator.HIGH_PRIORITY;
	private int minUsersToMatch = 2;
	private String defaultRole = new String(); 
	private String[] roles; 
	private boolean matchMultipleToDefault = true;
	private int numRoles; 
	private boolean sendMatchRemoteLog = false; 

	public static String getStepType()
	{
		return "match";
	}

	public MatchStepHandler()
	{
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		String promptsPath = properties.getProperty("prompt_file","plans/plan_prompts.xml");
		prompter = new PromptTable(promptsPath);

		try
		{
			defaultPromptPriority = Double.parseDouble(properties.getProperty("prompt_file",""+defaultPromptPriority));
		}
		catch (Exception e){}
		
		try
		{
			wordsPerSecond = Double.parseDouble(properties.getProperty("words_per_minute"))/60.0;
		}
		catch (Exception e){}
		
		try
		{
			constantDelay = Double.parseDouble(properties.getProperty("delay_after_prompt"));
		}
		catch (Exception e){}
		
		try{sendMatchRemoteLog = Boolean.parseBoolean(properties.getProperty("send_match_remote_log", "false"));}
		catch(Exception e) {e.printStackTrace();}
		
		// Get the set of roles to match with students, then shuffle the roles
		try
		{
			roles = properties.getProperty("roles", "").split("[\\s,]+");
			defaultRole = properties.getProperty("default_role", "");
			matchMultipleToDefault = Boolean.parseBoolean(properties.getProperty("match_multiple_to_default", "true"));
		}
		catch (Exception e){}
		numRoles = roles.length; 
		for (int i=0; i< numRoles; i++) {
			roles[i] = roles[i].replace("_", " "); 
		}
		try
		{
			minUsersToMatch = Integer.parseInt(properties.getProperty("min_users_to_match",""+minUsersToMatch));
		}
		catch (Exception e){}
		
		rateLimited = properties.getProperty("rate_limited", "true").equals("true");
//		Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "default priority="+defaultPromptPriority+ ", wait "+constantDelay +" seconds after prompts"
//		+(rateLimited?", +"+wordsPerSecond+" wps":""));
	}

	public MatchStepHandler(String promptsPath)
	{
		prompter = new PromptTable(promptsPath);
	}
	
	
	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
//		Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "Executing MatchStepHandler");
		State olds = StateMemory.getSharedState(overmind.getAgent());		
		State news = State.copy(olds);
		
		// Initialize the agent's state's roles if they haven't been initialized already
		if (news.getNumRoles() == 0) 
		{
			news.setRoles(roles);
		}
 
		// Get the IDs of the students currently present
		// String[] studentIds = state.getStudentIds(); 
		// Get the IDs of the students ever present		
//		news.setRandomizedStudentList();
		String[] studentIds = news.getRandomizedStudentIds(); 
		

//		System.err.println("MatchStepHandler, execute, studentIds after randomization:");
//		Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "MatchStepHandler, execute, studentIds after randomization:");
//		for (int i=0; i < studentIds.length; i++) {			 
//			System.err.println("MatchStepHandler, NewRoleAssignment, studentIds[" + String.valueOf(i) + "] = " + studentIds[i]);
//			Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "MatchStepHandler, NewRoleAssignment, studentIds[" + String.valueOf(i) + "] = " + studentIds[i]);
//		} 	
		
		int numStudents = studentIds.length; 
		
		// Get the root promptKey. There should be prompts with suffixes like _1, _2, _3, ...,
		// for various numbers of students & roles to match
		String promptKey = step.name;
		if(step.attributes.containsKey("prompt"))
		{
			promptKey = step.attributes.get("prompt");
		}
	
        // Variable prompt message, if available, depending upon the number of students & roles to match
		String promptText; 
        String adjustedPromptKey = promptKey;
        String promptSuffix = new String();
		promptSuffix = "_" + Integer.toString(numStudents); 
        int maxMatch = 0;
    	if (numStudents >= minUsersToMatch && numStudents <= numRoles) {
    		// Each student will take a different role
    		// promptSuffix = "_" + Integer.toString(numStudents)
    		maxMatch = numStudents;
    	}
    	if (numStudents > numRoles) {
    		if (matchMultipleToDefault) {
    			// Every student need to take a role. There are more students than roles, so after assigning the roles to different students, the rest of them will take the default role.
    			// promptSuffix = "_MAX_ALL"
    			List<String> roleList = new ArrayList<String>(Arrays.asList(roles));
    			roleList.remove(roleList.indexOf(defaultRole));
        		for (int i=numRoles-1; i<numStudents; i++) {
        			roleList.add(new String(defaultRole));
        		}
        		roles = roleList.toArray(new String[numStudents]);
        		promptSuffix = "_MAX_ALL";
        		maxMatch = numStudents;
    		}else {
    			// After assigning the roles to different students, the rest of the students will not take roles.
    			// promptSuffix = "_MAX_NONE"
    			promptSuffix = "_MAX_NONE";
    			maxMatch = numRoles;
    		}
    	}
//		System.out.println("MatchStepHandler execute - numStudents: " + Integer.toString(numStudents)); 
//		System.out.println("MatchStepHandler execute - numRoles: " + Integer.toString(numRoles)); 
        if (numStudents > 0) {
        	adjustedPromptKey = promptKey + promptSuffix; 
        	String adjustedPromptText = prompter.match(adjustedPromptKey, studentIds, roles, defaultRole, maxMatch, news);
//    		System.out.println("MatchStepHandler execute - adjustedPromptKey: " + adjustedPromptKey); 
//    		System.out.println("MatchStepHandler execute - adjustedPromptText: " + adjustedPromptText); 
        	if (adjustedPromptText == adjustedPromptKey) {
        		System.err.println("MatchStepHandler, execute: first match attempt failed"); 
        		promptText = prompter.match(promptKey, studentIds, roles, defaultRole, 0, news);
        	}
        	else {
        		promptText = adjustedPromptText; 
        		if (sendMatchRemoteLog) {
        			
        			// Send role assignments to remote logging
        			LogStateEvent logStateEvent = new LogStateEvent(source,"role_assignments",prompter.getIdsRoles(),false,null); 	
//        	        System.err.println("MatchStepHandler, execute - LogStateEvent created: " + logStateEvent.toString());
//        	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"MatchStepHandler, execute - LogStateEvent created: " + logStateEvent.toString());
        			source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "LogStateEvent", logStateEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
        		}
        	}
        } else {
        	promptText = prompter.lookup(promptKey);
        }
        	
		final double delay = constantDelay + (rateLimited?(promptText.split(" ").length/wordsPerSecond):0);
		
		MessageEvent me = new MessageEvent(source, overmind.getAgent().getUsername(), promptText, promptKey);
		makePromptProposal(source, delay, me, step.attributes);
//		Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "starting "+delay+" second prompt delay");
		
		new Timer(delay, new TimeoutReceiver()
		{

			@Override
			public void timedOut(String id)
			{
				Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "ending "+delay+" second prompt delay");
//				overmind.stepDone();
				overmind.stepDone(step.name);
			}

			@Override
			public void log(String from, String level, String msg)
			{}
			
		}){}.start();
		
		StateMemory.commitSharedState(news, overmind.getAgent());
		//overmind.stepDone();// other types have different "done" conditions -
							// this one is easy.
		// the Step sets the after-step-is-done delay on its own, from steps.xml
	}


	/**
	 * @param source
	 * @param me
	 * @param delay
	 */
	protected void makePromptProposal(InputCoordinator source, final double delay, final MessageEvent me, Map<String, String> attributes)
	{
		double priority = defaultPromptPriority;

		if(attributes.containsKey("priority"))
		{
			priority = Double.parseDouble(attributes.get("priority"));
		}
		
		if(!attributes.containsKey("lag"))
		{
			source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "PromptStep", me, priority, 30.0, delay));
		}
		else
		{
			double lagTime = Double.parseDouble(attributes.get("lag"));
			double timeout = Double.parseDouble(attributes.get("expires"));
			source.pushProposal(PriorityEvent.makeOpportunisticEvent("macro", "PromptStep", me, priority, lagTime, timeout, delay, ""));
		}
	}
	
	public void NewRoleAssignment(InputCoordinator source, State news, PresenceEvent pe, String from) {
		// Assign a role to the new student following the same logic in MatchStepHandler/RotateStepHandler 
		// and broadcast the message to the whole group.
		// Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"MatchStepHandler NewRoleAssignment");

		// Get present students, including the new student
		String[] student_ids = news.getStudentIds(); 
//		System.err.println("MatchStepHandler, NewRoleAssignment, initial student_ids:");
//		for (int i=0; i < student_ids.length; i++) {			 
//			System.err.println("MatchStepHandler, NewRoleAssignment, student_id[" + String.valueOf(i) + "] = " + student_ids[i]);
//		}
		int num_students = student_ids.length;
		String prompt_text = new String();
		
		if (num_students<minUsersToMatch) {
			return ;
		}
		if (num_students==minUsersToMatch) { // start role assignment for the whole group
			State state = StateMemory.getSharedState(source.getAgent());
//			state.setRandomizedStudentList();
			String[] studentIds = state.getRandomizedStudentIds(); 
//			System.err.println("MatchStepHandler, NewRoleAssignment, studentIds after randomization:");
//			for (int i=0; i < studentIds.length; i++) {			 
//				System.err.println("MatchStepHandler, NewRoleAssignment, studentIds[" + String.valueOf(i) + "] = " + studentIds[i]);
//			}
			String prompt_name = "PROMPT_MID_ROLE_MATCH";
			prompt_text = prompter.match(prompt_name, studentIds, roles, num_students, news);
			
		}else {
			if (num_students<=numRoles) { // the new student will take a new role
				Map<String, String> slots = new HashMap<String, String>();
				slots.put("[NAME]", pe.getUsername());
				slots.put("[ROLE]", roles[num_students-1]);
				String prompt_name = "PROMPT_SINGLE_ROLE_MATCH";
				prompt_text = prompter.lookup(prompt_name, slots);
				
				news.setStudentRole(pe.getUsername(), roles[num_students-1]);
			}else {
				if (matchMultipleToDefault) { // the new student will take the default role
					Map<String, String> slots = new HashMap<String, String>();
					slots.put("[NAME]", pe.getUsername());
					slots.put("[ROLE]", defaultRole);
					String prompt_name = "PROMPT_SINGLE_ROLE_MATCH";
					prompt_text = prompter.lookup(prompt_name, slots);
					news.setStudentRole(pe.getUsername(), defaultRole);
				}
			}
		}
		if (prompt_text.length()>0) {
//			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"prompt_text: " + prompt_text);
			Event e = new MessageEvent(source, from, prompt_text, "NEWROLEASSIGNMENT");
			double p = OutputCoordinator.LOW15_PRIORITY;
			double timeout = 60;
			source.addEventProposal(e, p, timeout);

    		if (sendMatchRemoteLog) {
    			// Send role assignments to remote logging
    			LogStateEvent logStateEvent = new LogStateEvent(source,"role_assignments",prompter.getIdsRoles(),false,null); 		
//    	        System.err.println("MatchStepHandler, execute - LogStateEvent created: " + logStateEvent.toString());
//    	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"MatchStepHandler, execute - LogStateEvent created: " + logStateEvent.toString());
    			source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "LogStateEvent", logStateEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
    		}
		}
	}
	
}