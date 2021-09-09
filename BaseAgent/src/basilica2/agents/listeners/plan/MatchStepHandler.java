package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;

class MatchStepHandler implements StepHandler
{
	private PromptTable prompter;
	private double wordsPerSecond = 200/60.0;
	private double constantDelay = 0.1;
	private boolean rateLimited = true;
	private double defaultPromptPriority = OutputCoordinator.HIGH_PRIORITY;
	private int maxUsersToMatch = 2;
	private String[] roles; 
	private List<String> roleList = new ArrayList<String>(); 
	private int numRoles; 

	public static String getStepType()
	{
		return "match";
	}

	public MatchStepHandler()
	{
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		String promptsPath = properties.getProperty("prompt_file","plan/plan_prompts.xml");
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
		
		// Get the set of roles to match with students, then shuffle the roles
		try
		{
			roles = properties.getProperty("roles", "").split("[\\s,]+");
		}
		catch (Exception e){}
		numRoles = roles.length; 
		for (int i=0; i< numRoles; i++) {
			roles[i] = roles[i].replace("_", " "); 
		}
		roleList = Arrays.asList(roles);
		Collections.shuffle(roleList);
		roleList.toArray(roles);
		try
		{
			maxUsersToMatch = Integer.parseInt(properties.getProperty("max_users_to_match",""+maxUsersToMatch));
		}
		catch (Exception e){}	
		if (maxUsersToMatch > numRoles) {
			maxUsersToMatch = numRoles; 
		}
		
		rateLimited = properties.getProperty("rate_limited", "true").equals("true");
		Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "default priority="+defaultPromptPriority+ ", wait "+constantDelay +" seconds after prompts"
		+(rateLimited?", +"+wordsPerSecond+" wps":""));
	}

	public MatchStepHandler(String promptsPath)
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
		String[] studentIds = state.getStudentIds(); 
		int numStudents = studentIds.length; 
		if (maxUsersToMatch > numStudents) {
			maxUsersToMatch = numStudents; 
		}

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
        String promptSuffix = "_" + Integer.toString(maxUsersToMatch); 
        if (maxUsersToMatch > 0) {
        	adjustedPromptKey = promptKey + promptSuffix; 
        	String adjustedPromptText = prompter.match(adjustedPromptKey, studentIds, roles, maxUsersToMatch, state);
        	if (adjustedPromptText == adjustedPromptKey) {
        		System.err.println("MatchStepHandler, execute: first match attempt failed"); 
        		promptText = prompter.match(promptKey, studentIds, roles, maxUsersToMatch, state);
        	}
        	else {
        		promptText = adjustedPromptText; 
        	}
        } else {
        	promptText = prompter.lookup(promptKey);
        }
        	
		final double delay = constantDelay + (rateLimited?(promptText.split(" ").length/wordsPerSecond):0);
		
		MessageEvent me = new MessageEvent(source, overmind.getAgent().getUsername(), promptText, promptKey);
		makePromptProposal(source, delay, me, step.attributes);
		Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "starting "+delay+" second prompt delay");
		
		new Timer(delay, new TimeoutReceiver()
		{

			@Override
			public void timedOut(String id)
			{
				Logger.commonLog("MatchStepHandler", Logger.LOG_NORMAL, "ending "+delay+" second prompt delay");
				overmind.stepDone();
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
			source.pushProposal(PriorityEvent.makeBlackoutEvent("PromptStep", me, priority, 30.0, delay));
		}
		else
		{
			double lagTime = Double.parseDouble(attributes.get("lag"));
			double timeout = Double.parseDouble(attributes.get("expires"));
			source.pushProposal(PriorityEvent.makeOpportunisticEvent("PromptStep", me, priority, lagTime, timeout, delay, ""));
		}
	}
}