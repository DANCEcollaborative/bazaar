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
package edu.cmu.cs.lti.basilica2.ui.graph;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Connection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;

/**
 *
 * @author rohitk
 */
public class AgentGraph extends JGraph {

    private Agent myAgent = null;
    private JGraph myGraph = null;
    private GraphModel myModel = null;
    private AgentGraphUser myUser = null;
    Map<Component, ComponentCell> componentMap = new HashMap<Component, ComponentCell>();
    Map<Connection, ConnectionEdge> connectionMap = new HashMap<Connection, ConnectionEdge>();
    List<DefaultGraphCell> cells = new ArrayList<DefaultGraphCell>();

    public AgentGraph(Agent a, AgentGraphUser u) {
        super(new DefaultGraphModel());
        myAgent = a;
        myGraph = this;
        myModel = this.getModel();
        myUser = u;

        // Control-drag should clone selection
        this.setCloneable(true);

        // Enable edit without final RETURN keystroke
        this.setInvokesStopCellEditing(true);

        // When over a cell, jump to its default port (we only have one, anyway)
        this.setJumpToDefaultPort(true);

        Properties p = new Properties();
        String path = "agentview.properties";
        try {
        	if(!new File(path).exists())
        		path = "properties"+File.separator+path;
        	
            p.load(new FileReader(path));
        	
        } catch (IOException ex) {
        }

        String[] comList = myAgent.getAllComponents().keySet().toArray(new String[0]);
        for (int i = 0; i < comList.length; i++) {
            Component com = myAgent.getComponent(comList[i]);
            ComponentCell c = null;
            String n = com.getName();
            if (p.getProperty(n + ".x") != null) {
                double x = Double.parseDouble(p.getProperty(n + ".x"));
                double y = Double.parseDouble(p.getProperty(n + ".y"));
                double w = Double.parseDouble(p.getProperty(n + ".w"));
                double h = Double.parseDouble(p.getProperty(n + ".h"));
                c = new ComponentCell(com, myGraph, x, y, w, h);
            } else {
                c = new ComponentCell(com, myGraph);
            }
            componentMap.put(com, c);
            cells.add(c);
        }

        List<Connection> conList = myAgent.getAllConnections();
        for (int i = 0; i < conList.size(); i++) {
            Connection con = conList.get(i);
            ConnectionEdge e = new ConnectionEdge(con, myGraph, componentMap.get(con.getSenderComponent()), componentMap.get(con.getReceiverComponent()));
            connectionMap.put(con, e);
            cells.add(e);
        }

        this.getGraphLayoutCache().insert(cells.toArray());

        this.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Get Cell under Mousepointer
                    int x = e.getX(), y = e.getY();
                    Object cell = myGraph.getFirstCellForLocation(x, y);
                    // Print Cell Label
                    if (cell != null) {
                        if (cell instanceof ComponentCell) {
                            ComponentCell c = (ComponentCell) cell;
                            Component com = c.getComponent();
                            myUser.ComponentSelected(com);
                        } else if (cell instanceof ConnectionEdge) {
                            ConnectionEdge c = (ConnectionEdge) cell;
                            Connection con = c.getConnection();
                            myUser.ConnectionSelected(con);
                        }
                    }
                }
            }
        });
    }

    public void writeAgentView() {
        Properties p = new Properties();

        try {
            p.load(new FileReader("agentview.properties"));
        } catch (IOException ex) {
        }

        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i) instanceof ComponentCell) {
                ComponentCell c = (ComponentCell) cells.get(i);
                String n = c.getComponent().getName();

                Rectangle2D cr = GraphConstants.getBounds(c.getAttributes());
                double x = cr.getX();
                p.setProperty(n + ".x", "" + x);

                double y = cr.getY();
                p.setProperty(n + ".y", "" + y);

                double w = cr.getWidth();
                p.setProperty(n + ".w", "" + w);

                double h = cr.getHeight();
                p.setProperty(n + ".h", "" + h);
            }
        }

        try {
            p.store(new FileWriter("agentview.properties"), "");
        } catch (IOException ex) {
        }
    }
}
