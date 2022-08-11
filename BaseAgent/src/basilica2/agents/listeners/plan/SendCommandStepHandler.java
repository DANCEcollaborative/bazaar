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
import basilica2.agents.events.SendCommandEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;

class SendCommandStepHandler implements StepHandler
{
	private PromptTable commander;
	private double defaultCommandPriority = OutputCoordinator.HIGHEST_PRIORITY;
	
	private HashMap<String, String> slots = null;

	public static String getStepType()
	{
		return "send_command";
	}

	public SendCommandStepHandler()
	{
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");

		try
		{
			String commandsPath = properties.getProperty("command_file","plans/commands.xml");
			commander = new PromptTable(commandsPath);
		}
		catch (Exception e){}
	}

	public SendCommandStepHandler(String commandsPath)
	{
		commander = new PromptTable(commandsPath);
	}

	public void execute(Step step, final PlanExecutor overmind, InputCoordinator source)
	{
		State news = StateMemory.getSharedState(overmind.getAgent());
		String commandKey = step.name;
		if(step.attributes.containsKey("command"))
		{
			commandKey = step.attributes.get("command");
		}
		
		String command = commander.lookup(commandKey);
		
		SendCommandEvent sendCommandEvent = new SendCommandEvent(source,command); 	
//      System.err.println("LogStepHandler, execute - SendCommandEvent created: tag: " + tag + "   details: " + details);
//      Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStepHandler, execute - SendCommandEvent created: tag: " + tag + "   details: " + details);
		source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "SendCommand", sendCommandEvent, OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
		overmind.stepDone();
	}

}