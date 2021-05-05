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
	
	private HashMap<String, String> slots1 = null;	
	private HashMap<String, String> slots2 = new HashMap<String, String>();

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
		
		try
		{
			roles = properties.getProperty("roles", "").split("[\\s,]+");
		}
		catch (Exception e){}
		numRoles = roles.length; 
		System.err.println("MatchStepHandler, numRoles: " + Integer.toString(numRoles)); 
		for (int i=0; i< numRoles; i++) {
			roles[i] = roles[i].replace("_", " "); 
			System.err.println("role " + Integer.toString(i) + ": " + roles[i]); 
		}
		roleList = Arrays.asList(roles);
		Collections.shuffle(roleList);
		
		for (int i = 0; i < numRoles; i++)
		{
			String role = roleList.get(i);
			// System.err.println("In loop - role: " + role);
			slots2.put("[ROLE" + (i + 1) + "]", role);
		}		
		// roleList.toArray(roles);
		
		try
		{
			// maxUsersToMatch = Integer.parseInt(properties.getProperty("max_users_to_match"));
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
		System.err.println("MatchStepHandler: entering 'execute'");
		State news = StateMemory.getSharedState(overmind.getAgent());
		if (news.getNumRoles() == 0) 
		{
			news.setRoles(roles);
		}
		if(slots1 == null)
		{
			slots1 = new HashMap<String, String>();
			slots1.put("[AGENT NAME]", source.getAgent().getUsername().split(" ")[0]);
		}
		slots1.put("[NAMES]", news.getStudentNamesString());
		List<String> studentNames = news.getStudentNames();
		System.err.println("MatchStepHandler, execute: num students = " + Integer.toString(studentNames.size()));
		for (int i = 0; i < studentNames.size() && i < maxUsersToMatch; i++)
		{
			String studentName = studentNames.get(i);
			slots1.put("[NAME" + (i + 1) + "]", studentName);
		}
		String rolesString = news.getRolesString(); 
		slots2.put("[ROLES]",rolesString);
		for (int i = 0; i < studentNames.size() && i < maxUsersToMatch; i++)
		{
			String role = roleList.get(i);
			slots2.put("[ROLE" + (i + 1) + "]", role);
		}

		String promptKey = step.name;
		if(step.attributes.containsKey("prompt"))
		{
			promptKey = step.attributes.get("prompt");
		}
	
        // Variable prompt message, if available, depending upon number of students
		String promptText; 
        int numStudents = StateMemory.getSharedState(overmind.getAgent()).getStudentCount(); 
        String adjustedPromptKey = promptKey;
        String promptSuffix = "_" + Integer.toString(numStudents); 
        if (numStudents > 0) {
        	adjustedPromptKey = promptKey + promptSuffix; 
        	String adjustedPromptText = prompter.match(adjustedPromptKey, slots1, slots2);
        	if (adjustedPromptText == adjustedPromptKey) {
        		promptText = prompter.match(promptKey, slots1, slots2);
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