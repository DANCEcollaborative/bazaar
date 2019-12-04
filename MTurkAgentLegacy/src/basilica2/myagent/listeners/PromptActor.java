package basilica2.myagent.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaListener;

public class PromptActor implements BasilicaListener
{


	/**
	 * @param source the InputCoordinator - action proposals are sent back through the source, which will queue them with the output coordinator.
	 * @param event an incoming event which matches one of this reactor's advertised classes (see getListenerEventClasses)
	 * 
	 * React to an incoming event, possibly proposing an 
	 */
	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		if(event instanceof PromptEvent)
		{
			PromptEvent re = (PromptEvent)event;
			
			MessageEvent me = new MessageEvent(source, source.getAgent().getUsername(), re.text, re.from);
			
			/*BlackoutEvent parameters: source-name, event, priority (between 0.0 and 1.0), 
			 *   relevance window (timeout, in seconds, before this proposal will be rejected), 
			 *   blackout window (after this proposal is accepted, new proposals will be blocked for this long)
			 *   There are other ways to configure action proposals ("PriorityEvent" instances) -- see ProrityEvent and PrioritySource.
			 */
			PriorityEvent blackout = PriorityEvent.makeBlackoutEvent(re.from, me, 1000000.0, 5, 15);
			
			/**
			 * components can be notified of accepted/rejected proposals by registering a callback.
			 */
			blackout.addCallback(new Callback()
			{
				@Override
				public void accepted(PriorityEvent p) 
				{
	
					
				}

				@Override
				public void rejected(PriorityEvent p) 
				{ 
					// ignore our rejected proposals
				}
			});
			
			/*
			 * There are other was to add a proposal besides addProposal -- see addEventProposal, pushProposal, etc in InputCoordinator.
			 */
			source.addProposal(blackout);
		}

	}


	/**
	 * @return the classes of events this reactor will respond to
	 */
	@Override
	public Class[] getListenerEventClasses()
	{
		//both RepeatEvents and MessageEvents will be forwarded to this reactor. 
		return new Class[]{PromptEvent.class};
	}

}
