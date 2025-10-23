package basilica2.agents.events.priority;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public abstract class AbstractPrioritySource
{
	protected String name;
	protected boolean blocking;
	
	public AbstractPrioritySource(String name)
	{
		this.name = name;
	}
	
	/**
	 * 
	 * @param e
	 * @return true if this source allows this event during its activation window, false otherwise.
	 */
	public abstract boolean allows(PriorityEvent e);
	
	/**
	 * 
	 * @param p
	 * @return a priority weight for a subsequent event, presuming it's allowed.
	 */
    public abstract double likelyNext(PriorityEvent p);
    
    /**
     * 
     * @return true if this already-active source has anything to say about later events, false when this source should be ignored.
     * 
     */
    public boolean isBlocking()
    {
        return blocking;
    }
    
    public void setBlocking(boolean b)
    {
    	blocking = b;
    }

    /**
     * un-block when this timer expires - more appropriate for a shared source.
     */
    public void setTimeout(double seconds)
    {
    	new Timer(seconds, new TimeoutReceiver()
    	{

			@Override
			public void timedOut(String id)
			{
				setBlocking(false);
				log(getClass().getSimpleName(),Logger.LOG_NORMAL,name+" is no longer blocking.");
			}

			@Override
			public void log(String from, String level, String msg)
			{}
		}).start();
    }
	
    public String getName()
    {
        return this.name;
    }
    
    public String toString()
    {
        return "Source: "+getName();
    }
}
