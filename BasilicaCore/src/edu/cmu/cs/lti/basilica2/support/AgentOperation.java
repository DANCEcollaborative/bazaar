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
package edu.cmu.cs.lti.basilica2.support;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.observers.AgentObserver;
import edu.cmu.cs.lti.project911.utils.log.LogUser;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author rohitk
 */
public abstract class AgentOperation implements Runnable, LogUser, AgentObserver {

    public static final int THROTTLE_RATE = 500;
    protected String myName;
    protected Map<String, Agent> myAgents;
    protected Logger myLogger;
    protected boolean ready = false,  running = false,  stopSignalled = false;
    private Thread myThread;

    public AgentOperation(String n) 
    {
    	if(n == null)
    		n = getClass().getSimpleName();
        myName = n;
        myLogger = new Logger(this, myName);
        myLogger.setConfiguration(true, true, true, false, false, false);
        log(Logger.LOG_LOW, "Logger initialized");
        myAgents = new HashMap<String, Agent>();
        ready = true;
        log(Logger.LOG_NORMAL, "AgentOperator " + myName + " created");
    }

    public void startOperation() {
        if (!ready) {
            log(Logger.LOG_ERROR, "Cannot start when not ready");
            throw new UnsupportedOperationException("Attempt to start operation when not ready");
        }
        log(Logger.LOG_NORMAL, "Starting operation");
        myThread = new Thread(this);
        myThread.start();
        log(Logger.LOG_NORMAL, "Operation started");
    }

    public void stopOperation() {
        log(Logger.LOG_NORMAL, "Stop Signalled");
        stopSignalled = true;
        
    }

    public void addAgent(Agent a) {
        if (!ready) {
            log(Logger.LOG_ERROR, "Cannot add agent when not ready");
            throw new UnsupportedOperationException("Attempt to add agent when not ready");
        }
        synchronized (this) {
            log(Logger.LOG_NORMAL, "Adding agent " + a.getName() + "");
            myAgents.put(a.getName(), a);
            agentAdded(a);
            log(Logger.LOG_NORMAL, "Agent added " + a.getName() + "");
        }
    }

    public abstract void agentAdded(Agent a);

    public void removeAgent(Agent a) {
        synchronized (this) {
            myAgents.remove(a.getName());
            a.dispose();
            this.agentRemoved(a);
            log(Logger.LOG_NORMAL, "Agent removed " + a.getName() + "");
        }
    }

    public void removeAgent(String name) {
        synchronized (this) {

            log(Logger.LOG_NORMAL, "Removing agent"+ name + "...");
            Agent a = myAgents.remove(name);
            if(a != null)
	         {
	            a.dispose();
	            this.agentRemoved(a);
	            log(Logger.LOG_NORMAL, "Agent removed " + a.getName() + "");
            }
        }
    }

    public abstract void agentRemoved(Agent a);

    public void shutdown() {
        if (!ready) {
            log(Logger.LOG_ERROR, "Cannot shutdown when not ready");
            throw new UnsupportedOperationException("Attempt to shutdown operation when not ready");
        }
        log(Logger.LOG_NORMAL, "Shutting down");
        if (running) {
            this.stopOperation();
        }
        Set<String> anames = myAgents.keySet();
        for (Iterator<String> i = anames.iterator(); i.hasNext();) {
            String aname = i.next();
            Agent a = myAgents.get(aname);
            this.removeAgent(a);
            a.dispose();
        }
        ready = false;
        log(Logger.LOG_NORMAL, "Shutdown completed");
    }

    public void run() {
        running = true;
        while (!stopSignalled) {
            try {
                Thread.sleep(THROTTLE_RATE);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            tick();
        }
        stopSignalled = false;
        running = false;
        log(Logger.LOG_LOW, "Stopped");
        operationOver();
    }

    public abstract void tick();

    public abstract void operationOver();

    public boolean isRunning() {
        return running;
    }

    public String getName() {
        return myName;
    }

    public void log(String level, String message) {
        myLogger.log(myName, level, message);
    }

    public void log(String from, String level, String message) {
        myLogger.log(from, level, message);
    }

    public void agentInitialized(Agent a) {
    }

    public void agentStarted(Agent a) {
    }

    public void agentStopped(Agent a) {
    }

    public void agentDisposed(Agent a) {
        agentRemoved(a);
    }
}
