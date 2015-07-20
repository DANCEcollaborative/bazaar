/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.myagent.listeners;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.data.RollingWindow.Entry;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

/**
 * Mocks and mimics user input.
 * A subclass of BasilicaAdapter, which provides convenience methods and variables for both Preprocessor and Listener (Reactor) components.
 * @author dadamson
 */

public class ParrotTalker extends BasilicaAdapter
{
	String negative;
	String question;
	String prefix;
	
	/**
	 * connect this component to the given agent, and set up a default proposal source.
	 * component properties will be loaded from properties/ClassName.properties by default
	 * @param a
	 */
    public ParrotTalker(Agent a) 
    {
		super(a);
		negative = this.properties.getProperty("negative");
		question = this.properties.getProperty("question");
		prefix = this.properties.getProperty("prefix");
	}

	@Override
	/**
	 * @param source send new event proposals back through the coordinator, they'll be queued for output
	 * @param event a preprocessed event that matches this reactor's advertised "listener" event types.
	 */
    public void processEvent(InputCoordinator source, Event event)
    {
        MessageEvent message = (MessageEvent)event;
       
        log(Logger.LOG_NORMAL, "repsonding to "+message.getFrom());
        String agentName = getAgent().getUsername();
        
        MessageEvent response;
        
        /*the existing annotations on the incoming message come from earlier preprocessors like MessageAnnotator - 
         *for this reason, the order of components in the operations.properties file does matter. */
        if(message.hasAnnotations("NEGATIVE"))
        {
            /* all parameters after the message text in a MessageEvent are optional String "annotations" 
             * which get logged in a CSV (in the /logs directory) along with the message text, author, and timestamp.
             * this happens for incoming user message events, too.*/
			response = new MessageEvent(message.getSender(), agentName, negative, "NEGATIVE_RESPONSE"); 
        }
        else if(message.hasAnnotations("QUESTION"))
        {
            response = new MessageEvent(message.getSender(), agentName, prefix+" "+question, "QUESTION_RESPONSE", "PREFIXED"); 
            
            
            
            Timer tickTock = new Timer();
            tickTock.schedule(new TimerTask()
            {
				@Override
				public void run()
				{
					List<Entry> questions = RollingWindow.sharedWindow().getEvents(30, "QUESTION"); //gets all the events that match 
				    MessageEvent me = (MessageEvent) questions.get(0).event;
				    String questionText = me.getText();
				}
			}, 30000);
        }
        else
        {
            response = new MessageEvent(message.getSender(), agentName, prefix+" "+message.getText(), "PREFIXED", "MIMICRY");
        }
        
        //provide a link back to the message the agent is referencing.
        response.setReference(message);

        /*
         * directly propose an event without specifying a Proposal Source - a generic one is used. 
         * This proposal has a priority of 0.5 (out of 1.0), and will be rejected after 3 seconds if other sources delay it.
         */
        source.addEventProposal(response, 0.5, 3);
        
    }

    /**only events matching the listed types will be forwarded to this component. */
	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event) 
	{
		//not doing any preprocessing.
	}

	@Override
	public Class[] getPreprocessorEventClasses() 
	{
		// not doing any preprocessing.
		return null;
	}
}
