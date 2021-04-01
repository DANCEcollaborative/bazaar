package basilica2.myagent.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.agents.listeners.BasilicaPreProcessor;
public class ClassificationResponse implements BasilicaPreProcessor
{

	public ClassificationResponse() {}

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
			MessageEvent me = (MessageEvent)event;
			annotations = me.getAnnotationString();
			// System.err.println("In MLAgent, Register.java preProcessEvent, annotations: " + annotations); 		
			// User user = getUser(me.getFrom());

			if (annotations != "") {
				String prompt_message = annotations;
				
				// Create high priority response since this is all this agent does 
				MessageEvent newMe = new MessageEvent(source, source.getAgent().getName(), prompt_message);
				PriorityEvent blackout = PriorityEvent.makeBlackoutEvent(source.getAgent().getName(), newMe, 1.0, 5, 5);
				blackout.addCallback(new Callback()
				{
					@Override
					public void accepted(PriorityEvent p) {}
					@Override
					public void rejected(PriorityEvent p) {}  // ignore our rejected proposals
				});
				source.pushProposal(blackout);
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
