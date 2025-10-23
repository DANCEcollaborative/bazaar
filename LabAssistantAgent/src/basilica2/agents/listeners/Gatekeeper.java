package basilica2.agents.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.StepDoneEvent;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class Gatekeeper extends BasilicaAdapter
{
    private Set<String> keymasters = new HashSet<String>();
    private Set<String> remainingKeys = new HashSet<String>();
    private Set<String> receivedKeys = new HashSet<String>();
	// private String keyPhrase = "^(ok|okay)?\\s*(ready)|(next)|(done)(\\p{Punct}+|\\s*$)";
	private String keyPhrase = ".*(ready|next|done).*";
	private Pattern keyPattern;
	private String stepName = "step";
    private PromptTable prompter;
    private Map<String, String> slots;
    private InputCoordinator source;
    private int requiredParticipants = 0;

	public void setStepName(String stepDoneName)
	{
		this.stepName = stepDoneName;
	}

	public Gatekeeper(Agent a)
	{
		super(a);
		
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");	
		if (properties != null)
		{
			try{keyPhrase = getProperties().getProperty("keyPhrase", keyPhrase);}
			catch(Exception e) {e.printStackTrace();}
		}
		
        keyPattern = Pattern.compile(keyPhrase, Pattern.CASE_INSENSITIVE);
        System.err.println("\n\n*** GATEKEEPER *** keyPhrase: " + keyPhrase + "\n\n");
        
        State state = StateMemory.getSharedState(a);
        if(state != null)
        {
            keymasters = new HashSet<String>(state.getStudentIdList());
        }
        else
        {
            keymasters = new HashSet<String>();
        }
		prompter = new PromptTable("plans/gatekeeper_prompts.xml");
		slots = new HashMap<String, String>();
	}

    public void setKeymasters(Collection<String> keymasters)
    {
        this.keymasters = new HashSet<String>(keymasters);
        resetGate();
    }

    public void setKeyPhrase(String phrase)
    {
        keyPhrase = phrase;
        keyPattern = Pattern.compile(keyPhrase, Pattern.CASE_INSENSITIVE);
    }

    public void setRequiredParticipants(int count)
    {
        requiredParticipants = Math.max(0, count);
    }

    public void resetGateForAllStudents()
    {
        // keymasters = StateMemory.getSharedState(getAgent()).getStudentNames();
        State state = StateMemory.getSharedState(getAgent());
        if (state != null)
        {
            keymasters = new HashSet<String>(state.getStudentIdList());
        }
        else
        {
            keymasters = new HashSet<String>();
        }

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

			if (keyPattern.matcher(me.getText()).matches())   // me.getText().toLowerCase().matches(keyPhrase))
			{
//				System.err.println("===== Matched ready phrase ====="); 
				readyUser(source, username);
			}
			else {
//				System.err.println("===== Did not match ready phrase ====="); 
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
//					System.err.println("===== Received 'isReady' ====="); 
					readyUser(source, username);
				}
				else
				{
//					System.err.println("===== Received NOT 'isReady'; calling readyUser() ====="); 
					readyUser(source, username);
					// unreadyUser(username);         // TEMP: quit toggling ready off
				}
			}
		}
	}

	public void unreadyUser(String username)
	{
//		log(Logger.LOG_NORMAL, username + " isn't ready anymore.");
		if (receivedKeys.contains(username))
		{
			
			receivedKeys.remove(username);
		}
		remainingKeys.add(username);
	}

    public void readyUser(InputCoordinator source, String username)
    {
        State sharedState = StateMemory.getSharedState(getAgent());
        List<String> students = (sharedState != null) ? sharedState.getStudentNames() : new ArrayList<String>();
//		log(Logger.LOG_NORMAL, username + " is ready!");
		receivedKeys.add(username);

        if (!keymasters.contains(username))
        {
            keymasters.add(username);
        }
        if (remainingKeys.contains(username))
        {
            remainingKeys.remove(username);
        }
		log(Logger.LOG_NORMAL, "waiting on ready from " + remainingKeys);

        boolean countSatisfied = (requiredParticipants > 0)
                ? receivedKeys.size() >= requiredParticipants
                : remainingKeys.isEmpty();

        boolean studentsSatisfied = (requiredParticipants > 0)
                ? false
                : receivedKeys.containsAll(students);

        if (countSatisfied || studentsSatisfied) 
        {
            allReady(source);
        }
        else
        {
            State currentState = StateMemory.getSharedState(getAgent());
            String studentName = (currentState != null) ? currentState.getStudentName(username) : username;
            slots.put("[STUDENT]", studentName);
            int numReady = receivedKeys.size();
            int numRemaining;
            if (requiredParticipants > 0)
            {
                numRemaining = Math.max(requiredParticipants - numReady, 0);
                numReady = Math.min(numReady, requiredParticipants);
            }
            else
            {
                numRemaining = Math.max(keymasters.size() - receivedKeys.size(), 0);
            }
            slots.put("[NUM_READY]", Integer.toString(numReady));
            slots.put("[NUM_REMAINING]", Integer.toString(numRemaining));
            source.addEventProposal(new PrivateMessageEvent(source, username, getAgent().getName(), prompter.lookup("ACKNOWLEDGE", slots), "ACKNOWLEDGE"));
        }
	}

	public void allReady(InputCoordinator source)
	{
        int numReady = receivedKeys.size();
        int numRemaining;
        if (requiredParticipants > 0)
        {
            numRemaining = Math.max(requiredParticipants - numReady, 0);
            numReady = Math.min(numReady, requiredParticipants);
        }
        else
        {
            numRemaining = Math.max(keymasters.size() - receivedKeys.size(), 0);
        }
        slots.put("[NUM_READY]", Integer.toString(numReady));
        slots.put("[NUM_REMAINING]", Integer.toString(numRemaining));

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
