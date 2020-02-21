package basilica2.agents.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;


public class MultiModalFilter implements BasilicaPreProcessor
{ 

	public static String GENERIC_NAME = "MultiModalFilter";
	public static String GENERIC_TYPE = "Filter";
	
	private String locationTag = "location:";

	public void MultiModalFilter()
	{
		// super();
	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent)
		{
			handleMessageEvent(source, (MessageEvent) e);
		}
	}

	private void handleMessageEvent(InputCoordinator source, MessageEvent me)
	{
		String text = me.getText();
		// MessageEvent newme = me;// new MessageEvent(source, me.getFrom(),    // MAY NOT NEED THIS 
								// me.getText());
		// String normalizedText = me.getText().toLowerCase();
		
		if(text.contains(locationTag)) // detect the location tag
		{
			System.out.println(">>>>>> Location flag detected <<<<<<<");
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
