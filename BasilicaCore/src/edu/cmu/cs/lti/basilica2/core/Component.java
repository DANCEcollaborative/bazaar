/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package edu.cmu.cs.lti.basilica2.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.cmu.cs.lti.basilica2.observers.ComponentObserver;
import edu.cmu.cs.lti.project911.utils.log.Logger;

/**
 * 
 * @author rohitk
 */
public abstract class Component implements Runnable
{

	public final int THROTTLE_RATE = 100;
	protected Agent myAgent;
	protected List<Event> myEventQueue;
	protected List<ComponentObserver> myObservers;
	protected List<Connection> myInwardConnections;
	protected List<Connection> myOutgoingConnections;
	protected Thread myThread = null;
	protected boolean ready = false, running = false, stopSignalled = false;
	protected String myName;
	protected Properties myProperties;

	/**
	 * 
	 * @param a
	 *            the agent using this component.
	 * @param n
	 *            component name
	 * @param pf
	 *            the properties filename
	 */
	public Component(Agent a, String n, String pf)
	{
		myAgent = a;
		myName = n;
		if ((pf == null) || (pf.trim().isEmpty()))
		{
			pf = getClass().getSimpleName()+".properties";
		}
		
		myProperties = new Properties()
		{

			@Override
			public String getProperty(String key)
			{
				String value = super.getProperty(key);
				log(Logger.LOG_NORMAL, "Property read: " + key + " =\t" + value);
				return value;
			}
		};
			
			File propertiesFile = new File(pf);
			if(!propertiesFile.exists())
			{
				propertiesFile = new File("properties/"+pf);
			}
			if(propertiesFile.exists()) try
			{
				myProperties.load(new FileReader(propertiesFile));
			}
			catch (IOException ex)
			{
				log(Logger.LOG_ERROR, "<error>Unable to read properties from " + pf + "</error>");
				ex.printStackTrace();
				return;
			}
		
		myEventQueue = new ArrayList<Event>();
		myObservers = new ArrayList<ComponentObserver>();
		myInwardConnections = new ArrayList<Connection>();
		myOutgoingConnections = new ArrayList<Connection>();
		informObservers(ComponentObserver.CREATED);
		log(Logger.LOG_NORMAL, "<created>" + myName + "</created>");
		ready = true;
	}

	public Component(Agent a, String n)
	{
		this(a, n, null);
	}

	/**
	 * Can also be used to restart
	 */
	public void initialize()
	{
		if (running)
		{
			log(Logger.LOG_WARNING, "<warning>" + myName + " is already initialized and running</warning>");
			return;
		}

		if (myAgent.inSyncMode())
		{
			log(Logger.LOG_NORMAL, "<initialized mode=\"synchronous\">" + myName + "</initialized>");
		}
		else
		{
			if (!ready)
			{
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
			if (ready)
			{
				myThread = new Thread(this);
				running = true;
				myThread.start();
				informObservers(ComponentObserver.STARTED);
				log(Logger.LOG_NORMAL, "<initialized>" + myName + "</initialized>");
			}
			else
			{
				log(Logger.LOG_ERROR, "<error>Unable to initialize " + myName + ". Not ready.</error>");
				throw new UnsupportedOperationException(myName + " is not ready");
			}
		}
	}

	public void run()
	{
		while (!stopSignalled)
		{
			if (!myEventQueue.isEmpty())
			{
				Event e = myEventQueue.remove(0);
				if (e.isValid())
				{
					log(Logger.LOG_LOW, "<processing name=\"" + e.getName() + "\" from=\"" + e.getSender().getName() + "\">" + e.toString()
							+ "</processing>");
					processEvent(e);
					log(Logger.LOG_LOW, "<processed name=\"" + e.getName() + "\" from=\"" + e.getSender().getName() + "\"></processed>");
				}
				else
				{
					log(Logger.LOG_LOW, "<ignoring name=\"" + e.getName() + "\" from=\"" + e.getSender().getName() + "\"> Invalid Event: " + e.toString()
							+ "</processing>");
				}
			}
			else
			{
				// Do the throttle
				try
				{
					Thread.sleep(THROTTLE_RATE);
					tick();
				}
				catch (Exception e)
				{
					log(Logger.LOG_WARNING, "<warning>Throttling problem</warning>");
					e.printStackTrace();
				}
			}
		}
		log(Logger.LOG_NORMAL, "<stopped>" + myName + "</stopped>");
		informObservers(ComponentObserver.STOPPED);
		running = false;
		stopSignalled = false;
	}

	// For some component to extend!
	protected void tick()
	{
	}

	public void stop()
	{
		if (!myAgent.inSyncMode())
		{
			if (running)
			{
				log(Logger.LOG_NORMAL, "Stop signalled");
				stopSignalled = true;
			}
			else
			{
				log(Logger.LOG_WARNING, "<warning>Cannot stop " + myName + ". Not running currently.</warning>");
			}
		}
	}

	public void dispose()
	{
		if (running)
		{
			// Timeout for a few seconds
			Thread t = new Thread(new Runnable()
			{

				public void run()
				{
					try
					{
						Thread.sleep(2000);
					}
					catch (Exception ex)
					{
						log(Logger.LOG_FATAL, "<fatal>Crash while waiting for disposing " + myName + "</fatal>");
						ex.printStackTrace();
					}
				}
			});
			t.start();
		}
		log(Logger.LOG_LOW, "Removing inward connections");
		for (int i = 0; i < myInwardConnections.size(); i++)
		{
			myInwardConnections.get(i).remove();
		}

		log(Logger.LOG_LOW, "Removing outgoing connections");
		for (int i = 0; i < myOutgoingConnections.size(); i++)
		{
			myOutgoingConnections.get(i).remove();
		}

		log(Logger.LOG_NORMAL, "<disposed>" + myName + "</disposed>");
		informObservers(ComponentObserver.DISPOSED);
		ready = false;
	}

	public void receiveEvent(Event e)
	{
		if (!ready)
		{
			log(Logger.LOG_ERROR, "Unable to receive " + e.getName() + ". Not ready.");
			throw new UnsupportedOperationException(myName + " is not ready");
		}

//		log(Logger.LOG_LOW, "Received name=" + e.getName() + " from=" + e.getSender().getName());
//		log(Logger.LOG_LOW, "Informing " + myObservers.size() + " Observers of Event Reception");
		notifyEventObservers(e);
		if (myAgent.inSyncMode())
		{
			processEvent(e);
		}
		else
		{
			synchronized (this)
			{
				myEventQueue.add(e);
				log(Logger.LOG_LOW, "<queued name=\"" + e.getName() + "\" from=\"" + e.getSender().getName() + "\"></queued>");
			}
		}
	}

	protected void notifyEventObservers(Event e)
	{
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).eventReceived(this, e);
		}
	}

	protected abstract void processEvent(Event e);

	public void addObserver(ComponentObserver o)
	{
		myObservers.add(o);
		log(Logger.LOG_LOW, "Observer Added:" + o.getName() + ". " + myObservers.size() + " Observers now.");
	}

	public void removeObserver(ComponentObserver o)
	{
		myObservers.remove(o);
		log(Logger.LOG_LOW, "Observer Removed:" + o.getName() + "");
	}

	public void addConnection(Connection c)
	{
		if (c.getSenderComponent().equals(this))
		{
			myOutgoingConnections.add(c);
			log(Logger.LOG_LOW, "Outgoing connection added from " + myName + " to " + c.getReceiverComponent().getName() + "");
		}
		else if (c.getReceiverComponent().equals(this))
		{
			myInwardConnections.add(c);
			log(Logger.LOG_LOW, "Inward connection added from " + myName + " to " + c.getSenderComponent().getName() + "");
		}
		else
		{
			log(Logger.LOG_WARNING, "<warning>Connection cannot be added. Neither sender nor receiver match \"this\".</warning>");
		}
	}

	public void removeConnection(Connection c)
	{
		if (c.getSenderComponent().equals(this))
		{
			if (myOutgoingConnections.remove(c))
			{
				log(Logger.LOG_LOW, "Outgoing connection to " + c.getReceiverComponent().getName() + "removed");
			}
		}
		else if (c.getReceiverComponent().equals(this))
		{
			if (myInwardConnections.remove(c))
			{
				log(Logger.LOG_LOW, "Inward connection to " + c.getSenderComponent().getName() + "removed");
			}
		}
		else
		{
			log(Logger.LOG_WARNING, "<warning>Connection cannot be removed. Neither sender nor receiver match \"this\".</warning>");
		}
	}

	protected void broadcast(Event e)
	{
		if (e.getSender() == null || !e.getSender().equals(this)) e.setSender(this);

		log(Logger.LOG_LOW, "<broadcasting>" + e.getName() + " on " + myOutgoingConnections.size() + " connections</broadcasting>");
		
		for (int i = 0; i < myOutgoingConnections.size(); i++)
		{
			myOutgoingConnections.get(i).communicate(e);
		}
		informObserversOfSending(e);
	}

	protected void dispatchEvent(Component receiver, Event e)
	{
		if (!e.getSender().equals(this)) e.setSender(this);
		log(Logger.LOG_NORMAL, "<dispatching to=\"" + receiver.getName() + "\">" + e.getName() + "</dispatching>");
		for (int i = 0; i < myOutgoingConnections.size(); i++)
		{
			if (myOutgoingConnections.get(i).getReceiverComponent().equals(receiver))
			{
				myOutgoingConnections.get(i).communicate(e);
				break;
			}
		}
		informObserversOfSending(e);
	}

	protected void informObserversOfSending(Event e)
	{
		log(Logger.LOG_LOW, "Informing " + myObservers.size() + " Observers of Event Sending");
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).eventSent(this, e);
		}
	}

	protected void informObservers(String s)
	{
		log(Logger.LOG_LOW, "Informing " + myObservers.size() + " Observers of " + s + "");
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).inform(this, s);
		}
	}

	public String getName()
	{
		return myName;
	}

	public abstract String getType();

	public boolean isRunning()
	{
		return running;
	}

	protected synchronized void log(String level, String message)
	{
		myAgent.log(myAgent.getName() + "." + myName, level, message);
	}
	
	public Agent getAgent()
	{
		return myAgent;
	}

	@Override
	public String toString()
	{
		return myName;
	}
}
