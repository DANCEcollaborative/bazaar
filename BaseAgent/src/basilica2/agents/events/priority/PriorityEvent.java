/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.agents.events.priority;

import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.util.Timer;

/**
 * represents an event proposal. convenience methods offer common ways to 
 * @author dadamson
 */
public class PriorityEvent extends Event
{

	private static final long serialVersionUID = 1L;

	public PriorityEvent(Component sender, PriorityEvent e)
    {
        super(sender);
        this.decoree = e.decoree;
        this.timeout = e.timeout;
        this.callback = e.callback;
        this.priority = e.priority;
        this.source = e.source;
    }

    @Override
    public String getName()
    {
        return "Priority_"+decoree.getName();
    }

    @Override
    public String toString()
    {
        return decoree.toString();
    }
    
    public static abstract class Callback
    {
        abstract public void accepted(PriorityEvent p);
        
        abstract public void rejected(PriorityEvent p);
    }
    private double priority;
    private Callback callback;
    private long timeout;
    private Event decoree;
    private AbstractPrioritySource source;
    private double lifetime; //seconds
    
    public PriorityEvent(Component c, Event decoree, double priority, AbstractPrioritySource source)
    {
        this(c, decoree, priority, source, 10);
    }
    
    /**
     * 
     * @param c the originating component
     * @param decoree the event to be queued.
     * @param priority between 0 and 1 - the maximum priority of this event
     * @param source the arbiter of competing priorities
     * @param lifetime in seconds, until the event expires and is discarded.
     */
    public PriorityEvent(Component c, Event decoree, double priority, AbstractPrioritySource source, double lifetime)
    {
        super(c);
        this.priority = priority;
        this.lifetime = lifetime;
        timeout = Timer.currentTimeMillis()+(long)(lifetime*1000);
        this.callback  = new Callback()
        {
            public void accepted(PriorityEvent p)
            {
            	Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"ACCEPTED! "+p);
            }
            
            public void rejected(PriorityEvent p)
            {
            	Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"REJECTED!" + p);
            }
        };
        this.source = source;
        this.decoree = decoree;
    }

    public AbstractPrioritySource getSource()
    {
        return source;
    }

    public void setSource(String PrioritySource)
    {
        this.source = source;
    }
    
    public void setPriority(double c)
    {
        this.priority = c;
    }
    
    public double getPriority()
    {
        double timeFactor = (0.5*1000*lifetime)/(1+timeout - Timer.currentTimeMillis());
        return this.priority * Math.min(1.0, 1.0/timeFactor);
    }

    public Callback getCallback()
    {
        return callback;
    }

    public void setCallback(Callback callback)
    {
        this.callback = callback;
    }

    public long getInvalidTime()
    {
        return timeout;
    }

    public void setTimeout(double seconds)
    {
        this.timeout = Timer.currentTimeMillis() + (long)(1000*seconds);
    }

    public boolean isAcknowledgementExpected()
    {
        return false;
    }
    
    public Event getEvent()
    {
        return decoree;
    }

	public static PriorityEvent makeSelfBlockingSourceEvent(String sourceName, Event e, double priority, double timeout, double blockout)
	{
		return makeBlockingEvent(sourceName, e, priority, timeout, blockout, sourceName);
	}

	public static PriorityEvent makeBlackoutEvent(String sourceName, Event e, double priority, double timeout, double blockout)
	{
		return makeBlockingEvent(sourceName, e, priority, timeout, blockout, "");
	}
	
	public static PriorityEvent makeBlockingEvent(String sourceName, Event e, double priority, double timeout, final double blockout, String... blacklist)
	{
		final AbstractPrioritySource ps = new BlacklistSource(sourceName, blacklist);
		PriorityEvent pe = new PriorityEvent(e.getSender(), e, priority, ps, timeout);
		pe.setCallback(new Callback()
		{
			public void rejected(PriorityEvent p)
			{
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"rejected! "+p);
			}
			
			public void accepted(PriorityEvent p)
			{
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"accepted. "+p+".\nStarting blockout countdown... ");
				ps.setTimeout(blockout);
			}
		});
		return pe;
	}
	
	public static PriorityEvent makeOpportunisticEvent(final String sourceName, final Event e, final double priority, final double lagTime, final double timeout, final double blockout, final String... blacklist)
	{
		final AbstractPrioritySource ps = new BlacklistSource(sourceName, blacklist);
		PriorityEvent pe = new PriorityEvent(e.getSender(), e, priority, ps, timeout)
		{
			@Override
			public double getPriority()
			{
				if(RollingWindow.sharedWindow().countAnyEvents(lagTime, "student_turn") > 0)
				{
					return 0;
				}
				else return super.getPriority();
			}
		};
		
		pe.setCallback(new Callback()
		{
			public void rejected(PriorityEvent p)
			{
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"rejected! "+p);
			}
			
			public void accepted(PriorityEvent p)
			{
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"accepted. "+p+".\nStarting blockout countdown... ");
				ps.setTimeout(blockout);
			}
		});
		return pe;
	}


	public void addCallback(final Callback callback2)
	{
		final Callback callback1 = getCallback();
		this.setCallback(new Callback()
		{

			@Override
			public void accepted(PriorityEvent p)
			{
				callback1.accepted(p);
				callback2.accepted(p);
			}

			@Override
			public void rejected(PriorityEvent p)
			{
				callback1.rejected(p);
				callback2.rejected(p);
			}
			
		});
	}
}
