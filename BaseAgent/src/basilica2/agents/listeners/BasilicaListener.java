/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.agents.listeners;

import basilica2.agents.components.InputCoordinator;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

/**
 *
 * @author dadamson
 */
public interface BasilicaListener
{
    public void processEvent(InputCoordinator source, Event event);

	public Class[] getListenerEventClasses();
}
