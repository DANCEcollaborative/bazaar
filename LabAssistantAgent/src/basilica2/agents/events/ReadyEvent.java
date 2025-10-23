package basilica2.agents.events;

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class ReadyEvent extends Event
{
	protected String username;
	protected boolean isReady = true;
	protected boolean globalReset = false;
	
	
	/**
	 * Constructor for user-specific ready event - this user is ready (or unreadied)
	 * @param isReady
	 * @param userNames
	 */
	public ReadyEvent(Component source, boolean isReady, String username)
	{
		super(source);
		this.username = username;
		this.isReady = isReady;
	}

	/**
	 * Constructor for global ready event - everyone is ready (or unreadied)
	 * @param isReady
	 */
	public ReadyEvent(Component source, boolean isReady)
	{
		super();
		this.isReady = isReady;
		this.globalReset = true;
	}

	@Override
	public String getName()
	{
		// TODO Auto-generated method stub
		return "Ready Event";
	}

	@Override
	public String toString()
	{
		String readyString = isReady ? "Ready":"Unready"; 
		String whoString = globalReset ? "Global" : username;
		return readyString + " Event: "+whoString;
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public boolean isReady()
	{
		return isReady;
	}

	public void setReady(boolean isReady)
	{
		this.isReady = isReady;
	}

	public boolean isGlobalReset()
	{
		return globalReset;
	}

	public void setGlobalReset(boolean globalReset)
	{
		this.globalReset = globalReset;
	}

}
