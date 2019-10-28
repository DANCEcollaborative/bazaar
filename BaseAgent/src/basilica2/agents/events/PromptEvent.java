package basilica2.agents.events;

import basilica2.agents.components.InputCoordinator;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class PromptEvent extends Event
{

	public String text;
	public String from;
	
	public PromptEvent(Component source, String text, String from)
	{
		super(source);
		this.text = text;
		this.from = from;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}




}
