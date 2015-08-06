/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.side.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaListener;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

/**
 * Displays all annotations added to the message.
 * @author dadamson
 */
public class AnnotationReporter  implements BasilicaListener
{
    @Override
    public void processEvent(InputCoordinator source, Event event)
    {
        MessageEvent me = (MessageEvent)event;
       
        Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL, "repsonding to "+me.getFrom());
        String agentName = source.getAgent().getUsername();
        
        MessageEvent report;
        report = new MessageEvent(me.getSender(), agentName, me.getFrom()+"'s message is: "+me.getAnnotationString());
        System.out.println(report);
        
        source.addEventProposal(report);
        
    }

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}
}
