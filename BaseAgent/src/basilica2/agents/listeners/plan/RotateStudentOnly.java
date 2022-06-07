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
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;
import basilica2.agents.listeners.plan.StepHandler;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.PlanExecutor;

public class RotateStudentOnly implements StepHandler
{
	private PromptTable prompter;
	private double wordsPerSecond = 200/60.0;
	private double constantDelay = 0.1;
	private boolean rateLimited = true;
	private double defaultPromptPriority = OutputCoordinator.HIGH_PRIORITY;

	public static String getStepType()
	{
		return "rotate";
	}

	public RotateStudentOnly()
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
		
		rateLimited = properties.getProperty("rate_limited", "true").equals("true");
		Logger.commonLog("RotateStudentOnly", Logger.LOG_NORMAL, "default priority="+defaultPromptPriority+ ", wait "+constantDelay +" seconds after prompts"
		+(rateLimited?", +"+wordsPerSecond+" wps":""));
	}

	public RotateStudentOnly(String promptsPath)
	{
		prompter = new PromptTable(promptsPath);
	}

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
//		State state = StateMemory.getSharedState(overmind.getAgent());
//		State state = StateMemory.getSharedState(source.getAgent()); 
		State olds = StateMemory.getSharedState(overmind.getAgent());	
		State news = State.copy(olds);
  
		// Get the IDs of the students ever present
		String[] studentIds = news.getRandomizedStudentIds(); 
		System.err.println("RotateStudentOnly, studentIDs: " + Arrays.toString(studentIds));
		int numStudents = studentIds.length; 
		int studentIndex = news.advanceStudentIndex(); 
		news.setNextStudentIndex(studentIndex);      // should be unnecessary
		String studentID = studentIds[studentIndex]; 
		String[] promptIds = {studentID}; 
		System.err.println("RotateStudentOnly, promptIds: " + Arrays.toString(promptIds));
		System.err.println("RotateStudentOnly, prompt name: " + news.getStudentName(studentID));
		
		// Get the root promptKey. There should be prompts with suffixes like _1, _2, _3, ...,
		// for various numbers of students
		String promptKey = step.name;
		if(step.attributes.containsKey("prompt"))
		{
			promptKey = step.attributes.get("prompt");
		}
	
		String promptText; 
        String promptSuffix = "_1"; 
		String adjustedPromptKey = promptKey + promptSuffix;

        if (numStudents > 0) {
    		
        	String adjustedPromptText = prompter.match(adjustedPromptKey, promptIds, null, null, 1, news);
        	System.err.println("RotateStudentOnly - adjustedPromptText: " + adjustedPromptText);
        	Logger.commonLog("RotateStudentOnly", Logger.LOG_NORMAL, "adjustedPromptKey: "+adjustedPromptKey);
        	
        	if (adjustedPromptText == adjustedPromptKey) {
        		System.err.println("RotateStudentOnly, execute: first match attempt failed"); 
        		promptText = prompter.match(promptKey, promptIds, null, null, 0, news);
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
		Logger.commonLog("RotateStudentOnly", Logger.LOG_NORMAL, "starting "+delay+" second prompt delay");
		
		new Timer(delay, new TimeoutReceiver()
		{

			@Override
			public void timedOut(String id)
			{
				Logger.commonLog("RotateStudentOnly", Logger.LOG_NORMAL, "ending "+delay+" second prompt delay");
				overmind.stepDone();
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
}