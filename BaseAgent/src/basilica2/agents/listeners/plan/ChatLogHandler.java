package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.io.BufferedInputStream;

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

class ChatLogHandler implements StepHandler
{
	private String processPath; 

	public static String getStepType()
	{
		return "chatlog";
	}

	public ChatLogHandler()
	{
		// System.err.println("ChatLogHandler, entering constructor");
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");		
		try
		{
			processPath = properties.getProperty("process_path","processes/chat_log.sh");
		}
		catch (Exception e){}	
		// System.err.println("ChatLogHandler, exiting constructor");	
	}

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
		// System.err.println("ChatLogHandler, entering execute");
		String processWithArgs = ""; 
		
		String processWithArgsWithPath = processPath + " " + source.getAgent().getRoomName(); 
		System.err.println("ChatLogHandler, execute - processWithArgsWithPath: " + processWithArgsWithPath);
		List<String> processWithArgsList = new ArrayList<>(Arrays.asList(processWithArgsWithPath.split(" ")));
		// System.err.println("ChatLogHandler, execute - processWithArgsList: " + Arrays.deepToString(processWithArgsList.toArray()));
	
		// ProcessBuilder pb = new ProcessBuilder(processPath + processFile);
		ProcessBuilder pb = new ProcessBuilder(processWithArgsList);
		Process p; 
	    int exitValue = -1; 
		try
		{
			p = pb.start();
			System.err.println("ChatLogHandler, process started; waiting for return value");
			exitValue = p.waitFor();

		    if (exitValue != 0) {
		        // check for errors
		    	System.err.println("ChatLogHandler, process exitValue is nonzero");
		        new BufferedInputStream(p.getErrorStream());
		        // throw new RuntimeException("exitValue is nonzero");
		    }
		}
		catch (Exception e){
	    	System.err.println("ChatLogHandler, execution of script failed!");
	        // throw new RuntimeException("execution of script failed!");
		}

		
		
	    if (exitValue != 0) {
	        // check for errors
	        // new BufferedInputStream(p.getErrorStream());
	        // throw new RuntimeException("execution of script failed!");
	    }
		
	    overmind.stepDone();
	    
		System.err.println("ChatLogHandler, exiting execute");
	}
	
}