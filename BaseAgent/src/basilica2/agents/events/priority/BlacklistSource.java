package basilica2.agents.events.priority;

import java.util.*;

import basilica2.util.PropertiesLoader;
import de.fhg.ipsi.utils.Log;

public class BlacklistSource extends AbstractPrioritySource
{
	private List<String> blacklist = new ArrayList<String>();
	private List<String> whitelist = new ArrayList<String>(); //exceptions
	
	private double permitted_true_multiplier = 1.333;
	private double permitted_false_multiplier = 0.5;
	private double similar_true_multiplier = 1.333;
	private double similar_false_multiplier = 0.75;
	
	
	//***blockThese is a list of *substrings* of source names that can be blocked...
	public BlacklistSource(String name, String... blockThese)
	{
		super(name);
		
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		permitted_true_multiplier = Double.parseDouble(properties.getProperty("permitted_true_multiplier",""+permitted_true_multiplier));
		permitted_false_multiplier = Double.parseDouble(properties.getProperty("permitted_false_multiplier",""+permitted_false_multiplier));
		similar_true_multiplier = Double.parseDouble(properties.getProperty("similar_true_multiplier",""+similar_true_multiplier));
		similar_false_multiplier = Double.parseDouble(properties.getProperty("similar_false_multiplier",""+similar_false_multiplier));
		
		this.blocking = true;
		Collections.addAll(blacklist, blockThese);
	}
	
	public void addExceptions(String...exceptions)
	{
		Collections.addAll(whitelist, exceptions);
	}

    public boolean allows(PriorityEvent p)
    {
    	
    	//System.out.println("BlacklistSource WHITELIST: "+whitelist);
    	//System.out.println("BlacklistSource BLACKLIST: "+blacklist);
    	for(String exception : whitelist) {
    		// whitelist may be empty string
    		if(exception.length()>0 && p.getSource().getName().contains(exception))
				return true;
    	}
    	for(String verboten : blacklist) {
    		// blacklist may be empty string
    		if(verboten.length()>0 && p.getSource().getName().contains(verboten))
    				return false;
    	}
    	return true;
    }
    
    public double likelyNext(PriorityEvent p)
    {
    	double permitted = allows(p)?permitted_true_multiplier:permitted_false_multiplier;
        double similar = p.getSource().getName().equals(this.getName()) ? similar_true_multiplier:similar_false_multiplier;
        //System.out.println("BlacklistSource permitted * similar: "+permitted+" * "+similar);
        return permitted * similar;
    }

	public List<String> getBlacklist()
	{
		return blacklist;
	}

	public void setBlacklist(List<String> blacklist)
	{
		this.blacklist = blacklist;
	}

}
