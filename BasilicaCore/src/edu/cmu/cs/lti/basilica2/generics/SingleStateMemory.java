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
package edu.cmu.cs.lti.basilica2.generics;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.observers.GenericObserverEvent;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author rohitk
 */
public class SingleStateMemory<T> extends Component {

    private String filename;
    private BufferedWriter outStream;
    private T currentState;

    public SingleStateMemory(Agent a, String n, String pf) {
        super(a, n, pf);
    }

    public void setFilename(String f) {
        filename = f;
        try {
            outStream = new BufferedWriter(new FileWriter(filename));
            outStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            outStream.write("<memorydump>\n");
        } catch (IOException ex) {
            log(Logger.LOG_ERROR, "Error while using OutStream (" + ex.toString() + ")");
        }
    }

    private void storeCurrentState() {
        if (currentState != null) {
            try {
                if (outStream != null) {
                    outStream.write(currentState.toString() + "\n");
                    outStream.flush();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                log(Logger.LOG_ERROR, "Error while using OutStream (" + ex.toString() + ")");
            }
        }
    }

    public void commit(T s) {
        storeCurrentState();
        currentState = s;
        GenericObserverEvent<T> e = new GenericObserverEvent<T>(this, s);
        this.informObserversOfSending(e);
    }

    public T retrieve() {
        if (currentState == null) {
            this.informObservers("<retrieving>null</retrieving>");
        }
        this.informObservers("<retrieving>" + currentState.toString() + "</retrieving>");
        return currentState;
    }

    @Override
    public void dispose() {
        try {
            if (outStream != null) {
                outStream.write("</memorydump>\n");
                outStream.flush();
                outStream.close();
            }
        } catch (IOException ex) {
            log(Logger.LOG_ERROR, "Error while using OutStream (" + ex.toString() + ")");
        }
        super.dispose();
    }

    @Override
    public String getType() {
        return "SingleStateMemory";
    }

    @Override
    protected void processEvent(Event e) {
    }
}
