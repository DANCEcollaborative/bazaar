package basilica2.socketchat;

import edu.cmu.cs.lti.basilica2.core.Component;
import basilica2.agents.events.MessageEvent;

public class DisplayHTMLEvent extends MessageEvent
{

	public DisplayHTMLEvent(Component source, String from, String body)
	{
		super(source, from, body);
	}
	
	@Override
	public String[] getParts()
	{
		return new String[]{this.getText()};
	}

}
