package basilica2.agents.listeners;

import java.util.Properties;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.SendCommandEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.util.PropertiesLoader;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class CurrentPartBroadcastListener extends BasilicaAdapter
{
    private final PromptTable commander;
    private final String stateKey;

    public CurrentPartBroadcastListener(Agent a)
    {
        super(a, "CurrentPartBroadcastListener");
        Properties props = PropertiesLoader.loadProperties("CurrentPartBroadcastListener.properties");
        String commandsPath = "plans/commands.xml";
        if (props != null)
        {
            commandsPath = props.getProperty("command_file", commandsPath);
        }
        commander = new PromptTable(commandsPath);
        stateKey = (props != null) ? props.getProperty("state_key", "CURRENT_PART_COMMAND") : "CURRENT_PART_COMMAND";
    }

    @Override
    public void processEvent(InputCoordinator source, Event event)
    {
        if (!(event instanceof PresenceEvent))
        {
            return;
        }
        PresenceEvent pe = (PresenceEvent) event;
        if (!PresenceEvent.PRESENT.equals(pe.getType()))
        {
            return;
        }
        State state = StateMemory.getSharedState(getAgent());
        if (state == null)
        {
            return;
        }
        Object commandId = state.more().get(stateKey);
        if (commandId == null)
        {
            return;
        }
        String commandName = commandId.toString();
        if (commandName.length() == 0)
        {
            return;
        }
        String commandText = commander.lookup(commandName);
        if (commandText == null || commandText.length() == 0)
        {
            Logger.commonLog(getClass().getSimpleName(), Logger.LOG_WARNING,
                    "Unknown command id for reconnect: " + commandName);
            return;
        }
        SendCommandEvent sendCommandEvent = new SendCommandEvent(source, commandText);
        source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "CurrentPartReplay", sendCommandEvent,
                OutputCoordinator.HIGH_PRIORITY, 5.0, 2));
    }

    @Override
    public Class[] getListenerEventClasses()
    {
        return new Class[] { PresenceEvent.class };
    }

    @Override
    public Class[] getPreprocessorEventClasses()
    {
        return new Class[] {};
    }
}
