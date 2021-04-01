package basilica2.myagent.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
public class Register implements BasilicaPreProcessor
{

	public Register() {}

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
		String annotations = ""; 
		if (event instanceof MessageEvent)
		{
			String prompt_message = ""; 
			MessageEvent me = (MessageEvent)event;
			annotations = me.getAnnotationString();
			// System.err.println("In MLAgent, Register.java preProcessEvent, annotations: " + annotations); 
			
			// User user = getUser(me.getFrom());

			Boolean promptFound = false; 
			if (annotations != "") {
				prompt_message = annotations;
				System.err.println("=== prompt_message: " + prompt_message); 
				PromptEvent prompt = new PromptEvent(source,prompt_message,"");
				source.queueNewEvent(prompt);
			}			
	    }
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
