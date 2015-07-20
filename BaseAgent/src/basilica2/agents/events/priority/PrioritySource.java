/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.agents.events.priority;
import java.util.*;

/**
 * the arbiter of a single source of prioritizable proposals
 * @author dadamson
 */
public class PrioritySource extends AbstractPrioritySource
{
    /**
     * name should be the key for a thread of related actions 
     * - for example, a tutorial dialogue might be keyed on the dialogue's name
     * exclusive action sources should have unique names.
     */
    
    private Collection<String> allowableInterruptions = new ArrayList<String>();

    public PrioritySource(String name, boolean blocking)
    {
       this(name, blocking, false);
    }

    public PrioritySource(String name, boolean blocking, boolean blockSelf)
    {
        super(name);
        this.blocking = blocking;
        if(!blockSelf)
        	allowableInterruptions.add(name);
    }

    public Collection<String> getAllowableInterruptions()
    {
        return allowableInterruptions;
    }
    
    public boolean allows(PriorityEvent p)
    {
        return !blocking || allowableInterruptions.contains(p.getSource().getName());
    }
    
    public double likelyNext(PriorityEvent p)
    {
        return p.getSource().getName().equals(name) ? 0.667 : 1.5;
    }
    
}
