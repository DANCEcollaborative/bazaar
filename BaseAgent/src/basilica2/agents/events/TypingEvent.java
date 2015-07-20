package basilica2.agents.events;

import java.util.Date;

import edu.cmu.cs.lti.basilica2.core.Event;

public class TypingEvent extends Event
{

	private String user;
	
	public TypingEvent(String username)
	{
		user = username;
	}
	
	public String getUser()
	{
		return user;
	}

	public void setUser(String user)
	{
		this.user = user;
	}

	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString()
	{
		// TODO Auto-generated method stub
		return "Typing: "+user;
	}

}
