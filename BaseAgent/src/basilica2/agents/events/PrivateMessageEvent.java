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
		printPrivateMessageEvent(to,from,message); 
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
	public PrivateMessageEvent cloneMessage(String newText)
	{
		PrivateMessageEvent pm = new PrivateMessageEvent(getSender(), toUser, from, newText);
		pm.annotations = this.annotations;
		return pm;
	}
	
	public void printPrivateMessageEvent(String to, String from, String message) {
		System.out.println("\nNew PrivateMessageEvent -- to:" + to + " -- from:" + from + "  -- message: " + message + "\n"); 
	}
	
	
}
