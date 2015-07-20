package basilica2.agents.events;

import edu.cmu.cs.lti.basilica2.core.Component;


public class PrivateMessageEvent extends MessageEvent
{

	private String toUser;
	
	public PrivateMessageEvent(Component source, String to, String from, String message, String... annotations)
	{
		super(source, from, message, annotations);
		toUser = to;
		this.addAnnotations("PRIVATE_MESSAGE", "to:"+toUser);
	}

	public String getDestinationUser()
	{
		return toUser;
	}

	public void setDestinationUser(String toUser)
	{
		this.toUser = toUser;
	}
	
	@Override 
	public MessageEvent cloneMessage(String newText)
	{
		PrivateMessageEvent pm = new PrivateMessageEvent(getSender(), toUser, from, newText);
		pm.annotations = this.annotations;
		return pm;
	}
	
	
}
