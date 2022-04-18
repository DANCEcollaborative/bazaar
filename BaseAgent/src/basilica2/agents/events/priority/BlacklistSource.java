package basilica2.agents.events.priority;

import java.util.*;

import de.fhg.ipsi.utils.Log;

public class BlacklistSource extends AbstractPrioritySource
{
	private List<String> blacklist = new ArrayList<String>();
	private List<String> whitelist = new ArrayList<String>(); //exceptions
	
	//***blockThese is a list of *substrings* of source names that can be blocked...
	public BlacklistSource(String name, String... blockThese)
	{
		super(name);
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
    		if(exception.length()>0 && p.getSource().getName().contains(exception))
				return true;
    	}
    	for(String verboten : blacklist) {
    		if(verboten.length()>0 && p.getSource().getName().contains(verboten))
    				return false;
    	}
    	return true;
    }
    
    public double likelyNext(PriorityEvent p)
    {
    	double permitted = allows(p)?1.0:0.5;
        double similar = p.getSource().getName().equals(this.getName()) ? 0.667:1.0;
        System.out.println("BlacklistSource permitted * similar: "+permitted+" * "+similar);
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