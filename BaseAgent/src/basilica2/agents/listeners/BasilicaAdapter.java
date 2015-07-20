
    /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.agents.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.AbstractPrioritySource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PrioritySource;
import basilica2.agents.listeners.BasilicaListener;
import basilica2.agents.listeners.BasilicaPreProcessor;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author dadamson
 */
/**
 *
 * @author dadamson
 */
public abstract class BasilicaAdapter implements BasilicaListener, BasilicaPreProcessor
{
    public interface ListenerListener
    {
        public void startedListening(BasilicaAdapter bask, InputCoordinator source);
        public void stoppedListening(BasilicaAdapter bask, InputCoordinator source);
    }
    
    protected Agent agent;
    
    protected AbstractPrioritySource prioritySource;
    protected ListenerListener delegate;
    protected Properties properties;
    
    private boolean listening = false;
            
    private BasilicaAdapter(Agent a, String sourceName, String propertiesFile)
    {
        agent = a;
        prioritySource = new PrioritySource(sourceName, false);
        initProperties(propertiesFile);
    }

    public BasilicaAdapter(Agent a)
    {
        this(a, "Generic", null);
        this.prioritySource = new PrioritySource(this.getClass().getSimpleName(), false);

        initProperties(this.getClass().getSimpleName()+".properties");
    }
    
    public BasilicaAdapter(Agent a, String sourceName)
    {
        this(a, sourceName, null);
        initProperties(this.getClass().getSimpleName()+".properties");
    }
    
    public Agent getAgent()
    {
        return agent;
    }
    
    
    public void stopListening(InputCoordinator source)
    {
    	if(listening)
    	{
	    	listening = false;
	        source.removePreProcessor(this);
	        source.removeListener(this);
	        if(delegate != null)
	            delegate.stoppedListening(this, source);
	        
	        delegate = null;
    	}
    }
    
    public void startListening(final InputCoordinator source)
    {
    	if(!listening)
    	{
	    	listening = true;
	        Class[] ppc = getPreprocessorEventClasses();
	        Class[] pc = getListenerEventClasses();
	        
	        if(ppc != null)
				for(Class c : ppc)
		        	source.addPreProcessor(c, this);
			if(pc != null)
				for(Class c : pc)
		        	source.addListener(c, this);
	        		
	        if(delegate != null)
	            delegate.startedListening(this, source);
    	}
    }
    
    public void setDelegate(ListenerListener delegate)
    {
        this.delegate = delegate;
    }
    
    public void initProperties(String pf)
    {
        properties = new Properties();
        if ((pf != null) && (pf.trim().length() != 0)) 
        {
        	File propertiesFile = new File(pf);
			if(!propertiesFile.exists())
			{
				propertiesFile = new File("properties"+File.separator+pf);
				if(!propertiesFile.exists())
					return;
			}
            try 
            {
                properties.load(new FileReader(propertiesFile));
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
                return;
            }
        }
    }
    
    protected void log(String level, String news)
	{
    	if(agent != null)
    	{
    		agent.log(this.getClass().getSimpleName(), level, news);
    	}
    	else System.out.println(this.getClass().getSimpleName()+": "+news);
		
	}
    
    public void informObservers(String news)
    {
    	agent.log(this.getClass().getSimpleName(), Logger.LOG_NORMAL, news);
    }

	public Properties getProperties()
	{
		return properties;
	}

}

