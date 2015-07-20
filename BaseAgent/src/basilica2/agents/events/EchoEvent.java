package basilica2.agents.events;

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class EchoEvent extends Event
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	MessageEvent echo;
	public EchoEvent(Component s, MessageEvent me)
	{
		super(s);
		echo = me;
	}

	@Override
	public String getName()
	{
		return "Echo";
	}

	@Override
	public String toString()
	{
		return "Echo: "+echo;
	}

	public MessageEvent getEvent()
	{
		// TODO Auto-generated method stub
		return echo;
	}

}
