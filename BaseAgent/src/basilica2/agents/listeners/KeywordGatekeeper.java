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
import basilica2.agents.listeners.KeywordWatcher;
import basilica2.agents.listeners.plan.Step; 
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class KeywordGatekeeper extends BasilicaAdapter
{
	private Step step = null; 
	private String stepName = "step";
	private PromptTable prompter;
	private KeywordWatcher keywordWatcher = null;

	public void setStepName(String stepName)
	{
		this.stepName = stepName; 
	}

	public KeywordGatekeeper(Agent a)
	{
		super(a);
		prompter = new PromptTable("plans/gatekeeper_prompts.xml");
	}
	

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		if (event instanceof MessageEvent)
		{
			keywordWatcher = (KeywordWatcher)source.getPreProcessor("KeywordWatcher");
			if (keywordWatcher.getPromptableNumEntries() == 0) {
				MessageEvent stepCompleteMessageEvent = new MessageEvent(source, getAgent().getUsername(), prompter.lookup("KEYWORD_STEP_COMPLETE"), "KEYWORD_STEP_COMPLETE"); 
				source.pushNamedEventProposal("macro",stepCompleteMessageEvent, "Keyword step complete message", 1.0, 10);
				source.pushEvent(new StepDoneEvent(source, stepName));
			}
		}
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[] { MessageEvent.class };
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
