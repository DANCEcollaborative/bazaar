/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.agents.events.priority;
import java.util.*;

import basilica2.util.PropertiesLoader;

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
    private double samename_true_multiplier = 0.667;
	private double samename_false_multiplier = 1.5;

    public PrioritySource(String name, boolean blocking)
    {
       this(name, blocking, false);
    }

    public PrioritySource(String name, boolean blocking, boolean blockSelf)
    {
        super(name);
        Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
        samename_true_multiplier = Double.parseDouble(properties.getProperty("samename_true_multiplier",""+samename_true_multiplier));
        samename_false_multiplier = Double.parseDouble(properties.getProperty("samename_false_multiplier",""+samename_false_multiplier));

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
        return p.getSource().getName().equals(name) ? samename_true_multiplier : samename_false_multiplier;
        // return p.getSource().getName().equals(name) ? 0.9 : 1.5;
    }
    
}
