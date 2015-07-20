package basilica2.myagent.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaListener;
import basilica2.myagent.events.RepeatEvent;

public class RepeatReactor implements BasilicaListener
{

	private static final String REPEAT_COMMENT = "You're repeating something that's already been said, ";
	private static final String FRUSTRATED_COMMENT = "I'm getting sick of reminding you to be original.";
	private int repeats = 0;

	/**
	 * the experimental conditions set for this agent are accessed through the system properties.
	 * @return
	 */
	private boolean isFrustrated() 
	{
		return repeats > 2 && System.getProperty("basilica2.agents.condition").contains("rude");
	}
	
	/**
	 * @param source the InputCoordinator - action proposals are sent back through the source, which will queue them with the output coordinator.
	 * @param event an incoming event which matches one of this reactor's advertised classes (see getListenerEventClasses)
	 * 
	 * React to an incoming event, possibly proposing an 
	 */
	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		if(event instanceof RepeatEvent)
		{
			RepeatEvent re = (RepeatEvent)event;
			String responseMessage;
			
			if(isFrustrated())
			{
				responseMessage = FRUSTRATED_COMMENT;
			}
			else
			{
				responseMessage = REPEAT_COMMENT+re.from;
			}
				
			MessageEvent me = new MessageEvent(source, source.getAgent().getUsername(), responseMessage, "NOTICE_REPEAT");
			
			/*BlackoutEvent parameters: source-name, event, priority (between 0.0 and 1.0), 
			 *   relevance window (timeout, in seconds, before this proposal will be rejected), 
			 *   blackout window (after this proposal is accepted, new proposals will be blocked for this long)
			 *   There are other ways to configure action proposals ("PriorityEvent" instances) -- see ProrityEvent and PrioritySource.
			 */
			PriorityEvent blackout = PriorityEvent.makeBlackoutEvent("REPEAT", me, 1.0, 5, 15);
			
			/**
			 * components can be notified of accepted/rejected proposals by registering a callback.
			 */
			blackout.addCallback(new Callback()
			{
				@Override
				public void accepted(PriorityEvent p) 
				{
					if(isFrustrated())	
						repeats = 0;
					else
						repeats++;
					
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
		else if(event instanceof MessageEvent)
		{
			//respond to regular MessageEvents, if you like.
		}
	}


	/**
	 * @return the classes of events this reactor will respond to
	 */
	@Override
	public Class[] getListenerEventClasses()
	{
		//both RepeatEvents and MessageEvents will be forwarded to this reactor. 
		return new Class[]{RepeatEvent.class, MessageEvent.class};
	}

}
