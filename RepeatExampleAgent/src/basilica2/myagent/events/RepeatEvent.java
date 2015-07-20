package basilica2.myagent.events;

import basilica2.agents.components.InputCoordinator;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class RepeatEvent extends Event
{

	public String text;
	public String from;
	
	public RepeatEvent(Component source, String text, String from)
	{
		super(source);
		this.text = text;
		this.from = from;
	}

	@Override
	public String getName()
	{
		return this.toString();
	}

	@Override
	public String toString()
	{
		return "repeat of "+text+" by "+from;
	}

}
