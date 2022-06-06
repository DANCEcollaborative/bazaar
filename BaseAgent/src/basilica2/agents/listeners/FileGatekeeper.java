package basilica2.agents.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.FileEvent;
import basilica2.agents.events.StepDoneEvent;
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
			System.err.println("FileGateKeeper: FileEvent received"); 
			FileEvent fileEvent = (FileEvent)event; 
			String eventFileName = fileEvent.getFileName();
			System.err.println("FileGateKeeper: FileEvent fileName: " + fileName); 
			if (eventFileName.equals(fileName)) {
				source.pushEventProposal(new MessageEvent(source, getAgent().getUsername(), prompter.lookup("FILE_STEP_COMPLETE"), "FILE_STEP_COMPLETE"), 1.0, 2);
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
