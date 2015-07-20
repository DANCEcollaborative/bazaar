package basilica2.accountable.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;

public class RevoicableWatcher extends AbstractAccountableActor
{
	public RevoicableWatcher(Agent a)
	{
		super(a); //all the variance is in the properties file
	}

	//second-tier response - the core AT move isn't needed because the discussion is productive enough - check for secondary opportunity
	@Override
	public void performFollowupCheck(final MessageEvent event)
	{
		//maybe check for revoicing mismatch?
	}

	@Override 
	/**
	 * just note it.
	 */
	public boolean shouldTriggerOnCandidate(MessageEvent me)
	{
		return true;
	}
	
	@Override
	public void processEvent(InputCoordinator source, Event e)
	{
		//DO NOTHING
	}

	@Override
	public boolean shouldAnnotateAsCandidate(MessageEvent me)
	{
		return !me.hasAnnotations("QUESTION") && !me.getText().contains("?");
	}
	
}
