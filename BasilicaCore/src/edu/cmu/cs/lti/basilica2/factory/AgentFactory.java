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
import edu.cmu.cs.lti.project911.utils.log.Logger;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author rohitk
 */
public class AgentFactory {

    public Agent makeAgentFromXML(String filename, String id) {
        Agent a = null;
        String agentname = null;
        NodeList components = null;
        NodeList connections = null;

        log("Making Agent from " + filename);

        try {
            DOMParser parser = new DOMParser();
            parser.parse(filename);
            Document dom = parser.getDocument();
            log("File Read");
            NodeList ns1 = dom.getElementsByTagName("agent");
            if ((ns1 != null) && (ns1.getLength() != 0)) {
                log("Agent Tag Read");
                Element agentElement = (Element) ns1.item(0);
                agentname = agentElement.getAttribute("name");
                if (agentname == null) {
                    System.err.println("agentname is null in " + filename + ". Aborting.");
                    return null;
                }

                NodeList ns2 = agentElement.getElementsByTagName("components");
                if ((ns2 != null) && (ns2.getLength() != 0)) {
                    log("Components Tag Read");
                    Element componentList = (Element) ns2.item(0);
                    components = componentList.getElementsByTagName("component");
                    log("Components read. N=" + components.getLength());
                } else {
                    System.err.println("No component list in " + filename + ".");
                }

                NodeList ns3 = agentElement.getElementsByTagName("connections");
                if ((ns3 != null) && (ns3.getLength() != 0)) {
                    Element connectionList = (Element) ns3.item(0);
                    connections = connectionList.getElementsByTagName("connection");
                } else {
                    System.err.println("No connection list in " + filename + ".");
                }

                a = new FactoryBuiltAgent(agentname + "_" + id, components, connections);
                //a.initialize();
            } else {
                System.err.println("No agent descriptions in " + filename + ". Aborting.");
                return null;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        log("Agent build. " + a.getName());
        return a;
    }

    private void log(String m) 
    {
    	Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,m);
    }
}
