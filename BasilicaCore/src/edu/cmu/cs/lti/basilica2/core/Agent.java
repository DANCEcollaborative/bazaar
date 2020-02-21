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

import edu.cmu.cs.lti.basilica2.observers.AgentObserver;
import edu.cmu.cs.lti.project911.utils.log.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author rohitk
 */
public abstract class Agent implements LogUser
{

	protected String agentName;
	protected Logger myLogger;
	protected Map<String, Component> myComponents;
	protected List<Connection> myConnections;
	protected boolean syncMode = false;
	protected List<AgentObserver> myObservers;
	private String userName;
	private String roomName;

	public Agent(String n)
	{
		this(n, false);
	}

	public Agent(String n, boolean mode)
	{
		setName(n);
		syncMode = mode;
		myLogger = new Logger(this, agentName);
		myLogger.setConfiguration(true, true, true, true, false, true);

		log(getClass().getSimpleName(),Logger.LOG_NORMAL,"CONFIGURING AGENT "+agentName);

		log(Logger.LOG_LOW, "Logger initialized");
		myComponents = new HashMap<String, Component>();
		log(Logger.LOG_LOW, "Component Map initialized");
		myConnections = new ArrayList<Connection>();
		log(Logger.LOG_LOW, "Connection List initialized");
		myObservers = new ArrayList<AgentObserver>();
		log(Logger.LOG_LOW, "Observer List initialized");
		log(Logger.LOG_NORMAL, "<created>" + agentName + "</created>");
	}

	// Core functions
	public void initialize()
	{
		log(Logger.LOG_LOW, "Creating components");
		createComponents();
		log(Logger.LOG_LOW, "Components created");
		log(Logger.LOG_LOW, "Connecting components");
		connectComponents();
		log(Logger.LOG_LOW, "Components connected");
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).agentInitialized(this);
		}
		log(Logger.LOG_LOW, "Observers informed of Initialization");
		log(Logger.LOG_NORMAL, "<initialized>" + agentName + "</initialized>");
	}

	public void start()
	{
		log(Logger.LOG_NORMAL, "<starting>Initializing each component</starting>");
		Set<String> cnames = myComponents.keySet();
		for (Iterator<String> i = cnames.iterator(); i.hasNext();)
		{
			String cname = i.next();
			Component c = myComponents.get(cname);
			log(Logger.LOG_LOW, "Initializing " + cname + "");
			c.initialize();
		}
		log(Logger.LOG_NORMAL, "<starting>All components initialized</starting>");
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).agentStarted(this);
		}
		log(Logger.LOG_LOW, "Observers informed of Starting");
	}

	public void stop()
	{
		log(Logger.LOG_NORMAL, "<stopping>Signally stop to each component</starting>");
		Set<String> cnames = myComponents.keySet();
		for (Iterator<String> i = cnames.iterator(); i.hasNext();)
		{
			String cname = i.next();
			Component c = myComponents.get(cname);
			log(Logger.LOG_LOW, "Signallying stop to " + cname + "");
			c.stop();
		}
		log(Logger.LOG_NORMAL, "<stopping>All components signalled to stop</stopping>");
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).agentStopped(this);
		}
		log(Logger.LOG_LOW, "Observers informed of Stopping");
	}

	public void dispose()
	{
		log(Logger.LOG_LOW, "Disposing components");
		String[] cnames = myComponents.keySet().toArray(new String[0]);
		for (int i = 0; i < cnames.length; i++)
		{
			String cname = cnames[i];
			Component c = myComponents.get(cname);
			c.stop();
			c.dispose();
		}
		for (int i = 0; i < cnames.length; i++)
		{
			this.removeComponent(cnames[i]);
		}
		log(Logger.LOG_LOW, "Disposing connections");
		myConnections.clear();
		log(Logger.LOG_LOW, "Agent disposed");
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).agentDisposed(this);
		}
		log(Logger.LOG_LOW, "Observers informed of Disposal");
		myObservers.clear();
	}

	protected abstract void createComponents();

	protected abstract void connectComponents();

	public void addComponent(Component c)
	{
		log(Logger.LOG_LOW, "Adding components: " + c.getName() + "");
		myComponents.put(c.getName(), c);
	}

	protected void removeComponent(Component c)
	{
		log(Logger.LOG_LOW, "Removing components: " + c.getName() + "");
		myComponents.remove(c.getName());
	}

	protected void removeComponent(String n)
	{
		log(Logger.LOG_LOW, "Removing components: " + n + "");
		myComponents.remove(n);
	}

	/**
	 * Something Else can override it if need be
	 * 
	 * @param from
	 * @param to
	 */
	public Connection makeConnection(Component from, Component to)
	{
		log(Logger.LOG_LOW, "Connecting " + from.getName() + " to " + to.getName() + "");
		if (getComponent(from.getName()) == null)
		{
			log(Logger.LOG_ERROR, "<error>Cannot add connection. " + from.getName() + "is not one of my components.</errors>");
			return null;
		}
		else if (getComponent(to.getName()) == null)
		{
			log(Logger.LOG_ERROR, "<error>Cannot add connection. " + to.getName() + "is not one of my components.</errors>");
			return null;
		}
		Connection c = new Connection(this, from, to);
		from.addConnection(c);
		to.addConnection(c);
		myConnections.add(c);
		return c;
	}

	public Connection getConnection(String sender, String receiver)
	{
		for (int i = 0; i < myConnections.size(); i++)
		{
			Connection c = myConnections.get(i);
			if (c.getSenderComponent().getName().equals(sender))
			{
				if (c.getReceiverComponent().getName().equals(receiver)) { return c; }
			}
		}
		return null;
	}

	public void addObserver(AgentObserver o)
	{
		log(Logger.LOG_LOW, "Adding observer: " + o.getClass().getCanonicalName() + "");
		myObservers.add(o);
	}

	public void removeObserver(AgentObserver o)
	{
		log(Logger.LOG_LOW, "Removing observer: " + o.getClass().getCanonicalName() + "");
		myObservers.remove(o);
	}

	// Utilities
	public String getName()
	{
		return agentName;
	}

	public boolean inSyncMode()
	{
		return syncMode;
	}

	public Component getComponent(String n)
	{
		return myComponents.get(n);
	}

	public Map<String, Component> getAllComponents()
	{
		return myComponents;
	}

	public List<Connection> getAllConnections()
	{
		return myConnections;
	}

	// Function components can use to send some triggers to the Agent
	// Agents that need this functionality should override this
	public void informAgent(Component sender, String information)
	{
	}

	// log(Logger.LOG_LOW, "");
	protected void log(String level, String message)
	{
		if (!(level.equals(Logger.LOG_LOW))) // ||level.equals(Logger.LOG_NORMAL)))
			myLogger.log(agentName + ".AGENT", level, message);
	}

	// For use by the components and connections
	public void log(String from, String level, String message)
	{
		//if (!(level.equals(Logger.LOG_LOW))) // ||level.equals(Logger.LOG_NORMAL)))
		myLogger.log(from, level, message);
	}

	public void setLogging(boolean logging)
	{
		myLogger.setConfiguration(logging, logging, logging, logging, false, false);
	}

	public void setName(String name)
	{
		String[] split = name.split("_");
		agentName = name;
		userName = split[0];
		if(split.length > 1)
			roomName = split[1];
		else roomName = "";
	}

	public String getUsername()
	{
		return userName;
	}

	public String getRoomName()
	{
		return roomName;
	}
}
