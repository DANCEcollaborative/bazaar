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
package edu.cmu.cs.lti.basilica2.factory;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Connection;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.lang.reflect.Constructor;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author rohitk
 */
public class FactoryBuiltAgent extends Agent {

    NodeList xmlComponents = null;
    NodeList xmlConnections = null;

    public FactoryBuiltAgent(String n, NodeList xcom, NodeList xcon) {
        super(n);
        xmlComponents = xcom;
        xmlConnections = xcon;
        log(Logger.LOG_LOW, "Agent is FactoryBuilt");
    }

    @Override
    protected void createComponents() {

        if ((xmlComponents != null) && (xmlComponents.getLength() != 0)) {
            log(Logger.LOG_LOW, "" + xmlComponents.getLength() + " possible components");
            for (int i = 0; i < xmlComponents.getLength(); i++) {
                Element comxml = (Element) xmlComponents.item(i);
                String name = comxml.getAttribute("name");
                String type = comxml.getAttribute("type");
                String classname = comxml.getAttribute("class");
                String properties = comxml.getAttribute("properties");
                try {
                    log(Logger.LOG_LOW, "Creating component " + i + ": name=" + name + " class=" + classname + " properties=" + properties + "");
                    Class c = Class.forName(classname);
                    log(Logger.LOG_LOW, "Step1: Class creation passed");

                    Class[] paramTypes = new Class[3];
                    paramTypes[0] = Agent.class;
                    paramTypes[1] = String.class;
                    paramTypes[2] = String.class;
                    Constructor ct = c.getConstructor(paramTypes);
                    log(Logger.LOG_LOW, "Step2: Constructor creation passed");

                    Object[] paramValues = new Object[3];
                    paramValues[0] = this;
                    paramValues[1] = name;
                    paramValues[2] = properties;
                    Component com = (Component) ct.newInstance(paramValues);
                    log(Logger.LOG_LOW, "Step3: Component creation passed");

                    this.addComponent(com);
                    log(Logger.LOG_LOW, "Step4: Component " + name + " created, added");
                } catch (Exception ex) {
                    log(Logger.LOG_FATAL, "Unable to create component " + name + ".");
                    ex.printStackTrace();
                    return;
                }
            }
        } else {
            log(Logger.LOG_ERROR, "No components for " + this.getName() + "");
            return;
        }
    }

    @Override
    protected void connectComponents() {
        if ((xmlConnections != null) && (xmlConnections.getLength() != 0)) {
            log(Logger.LOG_LOW, "" + xmlConnections.getLength() + " possible connection");
            for (int i = 0; i < xmlConnections.getLength(); i++) {
                Element conxml = (Element) xmlConnections.item(i);
                String fromname = conxml.getAttribute("from");
                String toname = conxml.getAttribute("to");
                log(Logger.LOG_LOW, "Creating connection " + i + ": from=" + fromname + " to=" + toname + "");
                Component fromcom = getComponent(fromname);
                Component tocom = getComponent(toname);
                if (fromcom == null || tocom == null) {
                    log(Logger.LOG_WARNING, "One of the connectees does not exist. Ignoring this connection.");
                    continue;
                } else {
                    Connection c = makeConnection(fromcom, tocom);
                    if (c == null) {
                        log(Logger.LOG_ERROR, "Failure while making connection. Ignoring");
                        continue;
                    } else {
                        log(Logger.LOG_LOW, "Connection " + c.getReadableId() + " created, added");
                    }
                }
            }
        } else {
            log(Logger.LOG_ERROR, "No connections for " + this.getName() + "");
            return;
        }
    }

    public void receiveLoggerMessage(String msg) {
    }
}