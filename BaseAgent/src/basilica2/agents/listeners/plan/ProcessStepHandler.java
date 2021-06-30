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

class ProcessStepHandler implements StepHandler
{
	private String processPath; 

	public static String getStepType()
	{
		return "process";
	}

	public ProcessStepHandler()
	{
		System.err.println("ProcessStepHandler, entering constructor");
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");		
		try
		{
			processPath = properties.getProperty("process_path","processes/");
		}
		catch (Exception e){}	
		System.err.println("ProcessStepHandler, exiting constructor");	
	}

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
		System.err.println("ProcessStepHandler, entering execute");
		String processFile = ""; 
		
		try
		{
			processFile = step.attributes.get("process");
		}
		catch (Exception e){}
	
		ProcessBuilder pb = new ProcessBuilder(processPath + processFile);
		try
		{
			Process p = pb.start();
		}
		catch (Exception e){}
		System.err.println("ProcessStepHandler, exiting execute");
	}
	
}