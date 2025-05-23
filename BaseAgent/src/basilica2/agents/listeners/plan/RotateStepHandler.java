package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.LogStateEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;
import basilica2.agents.listeners.plan.StepHandler;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.PlanExecutor;

public class RotateStepHandler implements StepHandler
{
	// Please refer to RoleStepHandler.properties for the meaning of the following variables
	private PromptTable prompter;
	private double wordsPerSecond = 200/60.0;
	private double constantDelay = 0.1;
	private boolean rateLimited = true;
	private double defaultPromptPriority = OutputCoordinator.HIGH_PRIORITY;
	private int minUsersToMatch = 2;
	private String defaultRole = new String();
	private boolean matchMultipleToDefault = true;
	private String[] roles; 
	private List<String> roleList = new ArrayList<String>(); 
	private int numRoles; 
	private boolean sendMatchRemoteLog = false; 

	public static String getStepType()
	{
		return "rotate";
	}

	public RotateStepHandler()
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
//		Logger.commonLog("RotateStepHandler", Logger.LOG_NORMAL, "default priority="+defaultPromptPriority+ ", wait "+constantDelay +" seconds after prompts"
//		+(rateLimited?", +"+wordsPerSecond+" wps":""));
	}

	public RotateStepHandler(String promptsPath)
	{
		prompter = new PromptTable(promptsPath);
	}

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
		State state = StateMemory.getSharedState(overmind.getAgent());
		
		// Initialize the agent's state's roles if they haven't been initialized already
		if (state.getNumRoles() == 0) 
		{
			state.setRoles(roles);
		}
 
		// Get the IDs of the students currently present
		// String[] studentIds = state.getStudentIds();  d
		// Get the IDs of the students ever present
//		String[] studentIds = state.getStudentIdsPresentOrNot(); 
//		String[] studentIds = state.getRandomizedStudentIds(); 
		String[] studentIds = state.getStudentIds(); 
		

//		System.err.println("RotateStepHandler, execute, studentIds, not randomized:");
//		Logger.commonLog("RotateStepHandler", Logger.LOG_NORMAL, "RotateStepHandler, execute, studentIds after randomization:");
//		for (int i=0; i < studentIds.length; i++) {			 
//			System.err.println("RotateStepHandler, NewRoleAssignment, studentIds[" + String.valueOf(i) + "] = " + studentIds[i]);
//			Logger.commonLog("RotateStepHandler", Logger.LOG_NORMAL, "RotateStepHandler, NewRoleAssignment, studentIds[" + String.valueOf(i) + "] = " + studentIds[i]);
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
        int maxMatch = 0;
        List<String> newRoles = new ArrayList<String>();
        if (numStudents > 0) {
        	if (numStudents < minUsersToMatch) {
        		// No role assignment if there are not enough students to start role assignment; No prompt suffix
        		newRoles = new ArrayList<String>(Arrays.asList(roles));
            }else {
            	Map<String, String> roleAssignment = new LinkedHashMap<String, String>();
            	List<String> remainingStudents = new ArrayList<String>(Arrays.asList(studentIds));
            	List<String> remainingRoles = new ArrayList<String>();
            	Collections.shuffle(remainingStudents);
            	if (numStudents <= numRoles) {
            		// When there are less students than roles, every student will take a different role
            		// prompt suffix = "_" + numStudents
            		// For every role, check which student hasn't taken it so far and assign it to that student
            		// If everyone has taken this role, save the role in remainingRoles
            		for (int i=0;i<numStudents;i++) {
            			boolean roleAssigned = false;
            			for (String Sid: remainingStudents) {
            				if ((!state.getStudentPreviousRoles(Sid).contains(roles[i])) && (!roleAssignment.containsKey(Sid))) {
            					roleAssignment.put(Sid,roles[i]);
            					roleAssigned=true;
            					remainingStudents.remove(remainingStudents.indexOf(Sid));
            					break;
            				}
            			}
            			if (!roleAssigned) {
            				remainingRoles.add(new String(roles[i]));
            			}
            		}
            		try {
            			// For the roles that everyone has taken before, choose a student who doesn't take this role in the last round
            			
            			if (remainingRoles.size()>0) {
                			for (int i=0; i<remainingRoles.size(); i++) {
                				boolean roleAssigned = false;
                				for (String Sid: remainingStudents) {
                					if (!state.getStudentRole(Sid).equalsIgnoreCase(remainingRoles.get(i))) {
                						roleAssignment.put(Sid,remainingRoles.get(i));
                						roleAssigned=true;
                						Logger.commonLog("RotateStepHandler", Logger.LOG_FATAL, "roleAssigned");
                						remainingStudents.remove(remainingStudents.indexOf(Sid));
                    					break;
                					}
                				}
                				if (!roleAssigned) {
                					roleAssignment.put(remainingStudents.get(0),remainingRoles.get(i));
                					remainingStudents.remove(0);
                				}
                			}
                		}
            		}
            		catch(Exception e){
            			Logger.commonLog("RotateStepHandler", Logger.LOG_FATAL, "Error at remainingRoles assignment.");
            		}
            		int i = 0;
            		for (String key: roleAssignment.keySet()) {
            			studentIds[i] = key;
            			newRoles.add(roleAssignment.get(key));
            			i++;
            		}
            		
            		promptSuffix = "_" + Integer.toString(numStudents); 
            		maxMatch = numStudents;
            	}else {
            		// When there are more students than roles, some students will take roles while the others may or may not take roles
            		// Same as the above, guarantee that one role is taken by different students in different rounds
        			
        			for (int i=0; i<numRoles; i++) {
            			if (matchMultipleToDefault && roles[i].equalsIgnoreCase(defaultRole)) {continue;}
            			boolean roleAssigned = false;
            			for (String Sid: remainingStudents) {
            				if ((!state.getStudentPreviousRoles(Sid).contains(roles[i])) && (!roleAssignment.containsKey(Sid))) {
            					roleAssignment.put(Sid,roles[i]);
            					roleAssigned=true;
            					remainingStudents.remove(remainingStudents.indexOf(Sid));
            					break;
            				}
            			}
            			if (!roleAssigned) {
            				remainingRoles.add(new String(roles[i]));
            			}
            		}
            		try {
            			if (remainingRoles.size()>0) {
                			for (int i=0; i<remainingRoles.size(); i++) {
                				boolean roleAssigned = false;
                				for (String Sid: remainingStudents) {
                					if (!state.getStudentRole(Sid).equalsIgnoreCase(remainingRoles.get(i))) {
                						roleAssignment.put(Sid,remainingRoles.get(i));
                						roleAssigned=true;
                						remainingStudents.remove(remainingStudents.indexOf(Sid));
                    					break;
                					}
                				}
                				if (!roleAssigned) {
                					roleAssignment.put(remainingStudents.get(0),remainingRoles.get(i));
                					remainingStudents.remove(0);
                				}
                			}
                		}
            		}
            		catch(Exception e){
            			Logger.commonLog("RotateStepHandler", Logger.LOG_FATAL, "Error at remainingRoles assignment.");
            			}
            		int i = 0;
            		for (String key: roleAssignment.keySet()) {
            			studentIds[i] = key;
            			newRoles.add(roleAssignment.get(key));
            			i++;
            		}
            		if (matchMultipleToDefault) {
            			// If every student needs to have a role, the remaining students who haven't got a role will take the default role
            			// prompt suffix = "_MAX_ALL"
                		for (int j=0;j<remainingStudents.size();j++) {
                			newRoles.add(new String(defaultRole));
                			studentIds[i]=remainingStudents.get(j);
                			i++;
                		}
                		promptSuffix = "_MAX_ALL";
                		maxMatch = numStudents;
            		}else {
            			// One role is assigned to exactly one student and the others will not take roles
            			// prompt suffix = "_MAX_NONE"
            			promptSuffix = "_MAX_NONE";
            			maxMatch = numRoles;
            		}
            		
            	}
            }
        	
    		adjustedPromptKey = promptKey + promptSuffix;
    		String[] newRolesArray = newRoles.toArray(new String[numStudents]);
    		
        	String adjustedPromptText = prompter.match(adjustedPromptKey, studentIds, newRolesArray, defaultRole, maxMatch, state);
//        	Logger.commonLog("RotateStepHandler", Logger.LOG_FATAL, "maxMatch: "+Integer.toString(maxMatch));
//        	Logger.commonLog("RotateStepHandler", Logger.LOG_NORMAL, "adjustedPromptKey: "+adjustedPromptKey);
        	
        	if (adjustedPromptText == adjustedPromptKey) {
//        		System.err.println("RotateStepHandler, execute: first match attempt failed"); 
        		promptText = prompter.match(promptKey, studentIds, roles, defaultRole, 0, state);
        	}
        	else {
        		promptText = adjustedPromptText; 
        		if (sendMatchRemoteLog) {
        			// Send role assignments to remote logging
        			LogStateEvent LogStateEvent = new LogStateEvent(source,"role_assignments",prompter.getIdsRoles(),false,null); 		
        	        System.err.println("RotateStepHandler, execute - LogStateEvent created: " + LogStateEvent.toString());
        	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"RotateStepHandler, execute - LogStateEvent created: " + LogStateEvent.toString());
        			source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "LogStateEvent", LogStateEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
        		}
        	}
        } else {
        	promptText = prompter.lookup(promptKey);
        }
        	
		final double delay = constantDelay + (rateLimited?(promptText.split(" ").length/wordsPerSecond):0);
		
		MessageEvent me = new MessageEvent(source, overmind.getAgent().getUsername(), promptText, promptKey);
		makePromptProposal(source, delay, me, step.attributes);
//		Logger.commonLog("RotateStepHandler", Logger.LOG_NORMAL, "starting "+delay+" second prompt delay");
		
		new Timer(delay, new TimeoutReceiver()
		{

			@Override
			public void timedOut(String id)
			{
				Logger.commonLog("RotateStepHandler", Logger.LOG_NORMAL, "ending "+delay+" second prompt delay");
//				overmind.stepDone();
				overmind.stepDone(step.name);
			}

			@Override
			public void log(String from, String level, String msg)
			{}
			
		}){}.start();
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
}