package basilica2.agents.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.FileEvent;
import basilica2.agents.events.StepDoneEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.listeners.plan.Step; 
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class FileGatekeeper extends BasilicaAdapter
{
	private Step step = null; 
	private String stepName = "step";
	private PromptTable prompter;
	private String fileName; 

	public void setStepName(String stepName)
	{
		this.stepName = stepName; 
	}
	
	public void setFileName(String fileName) {
		this.fileName = fileName; 
	}

	public FileGatekeeper(Agent a)
	{
		super(a);
		prompter = new PromptTable("plans/gatekeeper_prompts.xml");

//		if(step.attributes.containsKey("file"))
//		{
//			fileName = step.attributes.get("file");
//		}
	}
	

	// TODO: Files created early and/or out of order
	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		if (event instanceof FileEvent)
		{
			System.err.println("FileGateKeeper.processEvent: FileEvent received"); 
			log(Logger.LOG_NORMAL, "FileGateKeeper.processEvent: FileEvent received");
			FileEvent fileEvent = (FileEvent)event; 
			String eventFileName = fileEvent.getFileName();
			log(Logger.LOG_NORMAL, "FileGateKeeper.processEvent: eventFileName = " + eventFileName);
			log(Logger.LOG_NORMAL, "FileGateKeeper.processEvent: fileName = " + fileName);
			System.err.println("FileGateKeeper.processEvent: eventFileName = " + eventFileName);
			System.err.println("FileGateKeeper.processEvent: FileEvent fileName: " + fileName); 
			if (eventFileName.equals(fileName)) {
				log(Logger.LOG_NORMAL, "FileGateKeeper.processEvent: pushing FILE_STEP_COMPLETE prompt and StepDoneEvent");
				System.err.println("FileGateKeeper.processEvent: pushing FILE_STEP_COMPLETE prompt and StepDoneEvent");
				MessageEvent stepCompleteMessageEvent = new MessageEvent(source, getAgent().getUsername(), prompter.lookup("FILE_STEP_COMPLETE"), "FILE_STEP_COMPLETE"); 
//				source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "PromptStep", stepCompleteMessageEvent, OutputCoordinator.HIGHEST_PRIORITY, 10, 7)); 
				source.pushNamedEventProposal("macro",stepCompleteMessageEvent, "File step complete message", 1.0, 10);
				log(Logger.LOG_NORMAL, "FileGateKeeper.processEvent, pushing StepDoneEvent  -  source:" + source.toString() + "  stepName:" + stepName);
				System.err.println("FileGateKeeper.processEvent, pushing StepDoneEvent  -  source:" + source.toString() + "  stepName:" + stepName); 
				source.pushEvent(new StepDoneEvent(source, stepName));
			}
		}
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[] { FileEvent.class };
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
	}

	
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return null;
	}
}
