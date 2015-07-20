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

import edu.cmu.cs.lti.basilica2.observers.ConnectionObserver;
import edu.cmu.cs.lti.project911.utils.IdGenerator;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class Connection {

    private Component from,  to;
    private Agent myAgent;
    private Long id;
    private List<ConnectionObserver> myObservers;
    private boolean ready = false;

    public Connection(Agent a, Component f, Component t) {
        id = IdGenerator.get();
        myAgent = a;
        from = f;
        to = t;
        myObservers = new ArrayList<ConnectionObserver>();
        ready = true;
        log(Logger.LOG_LOW, "Connection initialized. Id=" + id.toString() + "");
        informObservers(ConnectionObserver.CREATED);
    }

    public Component getSenderComponent() {
        return from;
    }

    public Component getReceiverComponent() {
        return to;
    }

    public Long getId() {
        return id;
    }

    public String getReadableId() {
        return ("(" + from.getName() + "->" + to.getName() + ")");
    }

    public void addObserver(ConnectionObserver o) {
        log(Logger.LOG_NORMAL, "Observer Added:" + o.getName() + "");
        myObservers.add(o);
    }

    public void removeObserver(ConnectionObserver o) {
        log(Logger.LOG_NORMAL, "Observer Removed:" + o.getName() + "");
        myObservers.remove(o);
    }

    protected void log(String level, String message) {
        myAgent.log(this.getReadableId(), level, message);
    }

    public void communicate(Event e) {
        if (!ready) {
            log(Logger.LOG_ERROR, "<error>Unable to communicate " + e.getName() + ". Not ready.</error>");
            throw new UnsupportedOperationException(this.getReadableId() + " is not ready");
        }

        if (e.getSender().equals(from)) {
            log(Logger.LOG_LOW, "Communicate " + e.getName() + " from " + from.getName() + " to " + to.getName() + "");
            for (int i = 0; i < myObservers.size(); i++) {
                myObservers.get(i).communicating(this, e);
            }
            to.receiveEvent(e);
        } else {
            log(Logger.LOG_WARNING, "<warning>Cannot communicate " + e.getName() + " because sender is not " + from.getName() + "</warning>");
        }
    }

    public void remove() {
        ready = false;
        log(Logger.LOG_LOW, "Removing connection");
        informObservers(ConnectionObserver.REMOVED);
        this.from.removeConnection(this);
        this.to.removeConnection(this);
    }

    protected void informObservers(String s) {
        for (int i = 0; i < myObservers.size(); i++) {
            myObservers.get(i).inform(this, s);
        }
    }

    @Override
    public String toString() {
        return this.getReadableId();
    }
}
