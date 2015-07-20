package basilica2.myagent.listeners;

import java.util.ArrayList;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.myagent.events.RepeatEvent;

public class RepeatWatcher implements BasilicaPreProcessor
{
	
	//a toy-sized model of message history - see "RollingWindow" for a richer event log.
	private ArrayList<String> messages = new ArrayList<String>();

	/**
	 * @param source the InputCoordinator - to push new events to. (Modified events don't need to be re-pushed).
	 * @param event an incoming event which matches one of this preprocessor's advertised classes (see getPreprocessorEventClasses)
	 * 
	 * Preprocess an incoming event, by modifying this event or creating a new event in response. 
	 * All original and new events will be passed by the InputCoordinator to the second-stage Reactors ("BasilicaListener" instances).
	 */
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		MessageEvent me = (MessageEvent)event;
		String normalizedText = me.getText().toLowerCase();
		
		if(messages.contains(normalizedText)) // detect a repeated message
		{
			//this new event will be added to the queue for second-stage processing.
			RepeatEvent repeat = new RepeatEvent(source, me.getText(), me.getFrom());
			
			source.addPreprocessedEvent(repeat);
		}
		
		messages.add(normalizedText);
	}

	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		//only MessageEvents will be delivered to this watcher.
		return new Class[]{MessageEvent.class};
	}

}
