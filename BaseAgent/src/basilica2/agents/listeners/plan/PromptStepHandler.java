package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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

class PromptStepHandler implements StepHandler
{
	private PromptTable prompter;
	private double wordsPerSecond = 200/60.0;
	private double constantDelay = 0.1;
	private boolean rateLimited = true;
	private double defaultPromptPriority = OutputCoordinator.HIGH_PRIORITY;
	
	private HashMap<String, String> slots = null;

	public static String getStepType()
	{
		return "prompt";
	}

	public PromptStepHandler()
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
		
		rateLimited = properties.getProperty("rate_limited", "true").equals("true");
		Logger.commonLog("PromptStepHandler", Logger.LOG_NORMAL, "default priority="+defaultPromptPriority+ ", wait "+constantDelay +" seconds after prompts"
		+(rateLimited?", +"+wordsPerSecond+" wps":""));
	}

	public PromptStepHandler(String promptsPath)
	{
		prompter = new PromptTable(promptsPath);
	}

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
		if(slots == null)
		{
			slots = new HashMap<String, String>();
			slots.put("[AGENT NAME]", source.getAgent().getUsername().split(" ")[0]);
		}

		State news = StateMemory.getSharedState(overmind.getAgent());
		slots.put("[NAMES]", news.getStudentNamesString());
		List<String> studentNames = news.getStudentNames();
		for (int i = 0; i < studentNames.size(); i++)
		{
			String studentName = studentNames.get(i);
			slots.put("[NAME" + (i + 1) + "]", studentName);
		}

		String promptKey = step.name;
		if(step.attributes.containsKey("prompt"))
		{
			promptKey = step.attributes.get("prompt");
		}
		
		String promptText = prompter.lookup(promptKey, slots);
		final double delay = constantDelay + (rateLimited?(promptText.split(" ").length/wordsPerSecond):0);
		
		MessageEvent me = new MessageEvent(source, overmind.getAgent().getUsername(), promptText, promptKey);
		makePromptProposal(source, delay, me, step.attributes);
		Logger.commonLog("PromptStepHandler", Logger.LOG_NORMAL, "starting "+delay+" second prompt delay");
		
		new Timer(delay, new TimeoutReceiver()
		{

			@Override
			public void timedOut(String id)
			{
				Logger.commonLog("PromptStepHandler", Logger.LOG_NORMAL, "ending "+delay+" second prompt delay");
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