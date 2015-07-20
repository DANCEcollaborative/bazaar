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
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.StepDoneEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class Gatekeeper extends BasilicaAdapter
{
	private Collection<String> keymasters = new ArrayList<String>();
	private Collection<String> remainingKeys = new ArrayList<String>();
	private Collection<String> receivedKeys = new ArrayList<String>();
	private String keyPhrase = "^(ok|okay)?\\s*(ready)|(next)|(done)(\\p{Punct}+|\\s*$)";
	private Pattern keyPattern = Pattern.compile(keyPhrase, Pattern.CASE_INSENSITIVE);
	private String stepName = "step";
	private PromptTable prompter;
	private Map<String, String> slots;
	private InputCoordinator source;

	public void setStepName(String stepDoneName)
	{
		this.stepName = stepDoneName;
	}

	public Gatekeeper(Agent a)
	{
		super(a);
		keymasters = StateMemory.getSharedState(a).getStudentNames();
		prompter = new PromptTable("plans/gatekeeper_prompts.xml");
		slots = new HashMap<String, String>();
	}

	public void setKeymasters(Collection<String> keymasters)
	{
		this.keymasters = keymasters;
		resetGate();
	}

	public void setKeyPhrase(String phrase)
	{
		keyPhrase = phrase;
		keyPattern = Pattern.compile(keyPhrase, Pattern.CASE_INSENSITIVE);
	}

	public void resetGateForAllStudents()
	{
		keymasters = StateMemory.getSharedState(getAgent()).getStudentNames();

		log(Logger.LOG_NORMAL, "waiting for responses from " + keymasters);
		resetGate();
	}

	public void resetGate()
	{
		receivedKeys.clear();
		remainingKeys.clear();
		remainingKeys.addAll(keymasters);

		if (source != null) // unready the clients
		{
			source.pushEventProposal(new ReadyEvent(source, false), 1.0, 60);
		}
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		this.source = source;
		if (event instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent) event;
			log(Logger.LOG_NORMAL, me.getText());

			String username = me.getFrom();

			if (keyPattern.matcher(me.getText()).matches())// me.getText().toLowerCase().matches(keyPhrase))
			{
				readyUser(source, username);
			}
		}
		else if (event instanceof ReadyEvent)
		{
			ReadyEvent re = (ReadyEvent) event;

			if (re.isGlobalReset())
			{
				// don't actually want to handle global events for now.
				// if(re.isReady())
				// {
				// this.allReady(source);
				// }
				// else
				// {
				// this.resetGateForAllStudents();
				// }
			}
			else
			{
				String username = re.getUsername();
				if (re.isReady())
				{
					readyUser(source, username);
				}
				else
				{
					unreadyUser(username);
				}
			}
		}
	}

	public void unreadyUser(String username)
	{
		log(Logger.LOG_NORMAL, username + " isn't ready anymore.");
		if (receivedKeys.contains(username))
		{
			
			receivedKeys.remove(username);
		}
		remainingKeys.add(username);
	}

	public void readyUser(InputCoordinator source, String username)
	{
		List<String> students = StateMemory.getSharedState(getAgent()).getStudentNames();
		log(Logger.LOG_NORMAL, username + " is ready!");
		receivedKeys.add(username);

		if (remainingKeys.contains(username))
		{
			remainingKeys.remove(username);
		}
		log(Logger.LOG_NORMAL, "waiting on ready from " + remainingKeys);

		if (remainingKeys.isEmpty() || receivedKeys.containsAll(students)) 
		{
			allReady(source);
		}
		else
		{
			slots.put("[STUDENT]", username);
			slots.put("[NUM_READY]", receivedKeys.size() + "");
			slots.put("[NUM_REMAINING]", (keymasters.size() - receivedKeys.size()) + "");
			source.addEventProposal(new PrivateMessageEvent(source, username, getAgent().getName(), prompter.lookup("ACKNOWLEDGE", slots), "ACKNOWLEDGE"));
		}
	}

	public void allReady(InputCoordinator source)
	{
		slots.put("[NUM_READY]", receivedKeys.size() + "");
		slots.put("[NUM_REMAINING]", (keymasters.size() - receivedKeys.size()) + "");

		remainingKeys.clear();
		source.pushEventProposal(new MessageEvent(source, getAgent().getUsername(), prompter.lookup("ALL_READY", slots), "ALL_READY"), 1.0, 2);
		source.pushEvent(new StepDoneEvent(source, stepName));
		source.addEventProposal(new ReadyEvent(source, false), 1.0, 60);
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[] { MessageEvent.class, ReadyEvent.class };
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
