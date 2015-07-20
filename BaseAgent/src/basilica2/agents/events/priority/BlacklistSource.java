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
//    	System.out.println("SOURCE: "+p.getSource());
//    	System.out.println("WHITELIST: "+whitelist);
//    	System.out.println("BLACKLIST: "+blacklist);
    	
    	for(String exception : whitelist)
    		if(p.getSource().getName().contains(exception))
				return true;
    		
    	for(String verboten : blacklist)
    		if(p.getSource().getName().contains(verboten))
    				return false;
    			
    	return true;
    }
    
    public double likelyNext(PriorityEvent p)
    {
        double permitted = allows(p)?1.333:0.5;
        double similar = p.getName().equals(this.getName()) ? 1.333:0.75;
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
