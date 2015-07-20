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

import edu.cmu.cs.lti.basilica2.core.Connection;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.observers.ConnectionObserver;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import java.awt.Color;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.GraphConstants;

/**
 *
 * @author rohitk
 */
public class ConnectionEdge extends DefaultEdge implements ConnectionObserver, TimeoutReceiver {

    Connection con = null;
    JGraph myGraph = null;

    public ConnectionEdge(Connection c, JGraph g, ComponentCell fromCell, ComponentCell toCell) {
        super("..");
        con = c;
        myGraph = g;
        c.addObserver(this);

        // Fetch the ports from the new vertices, and connect them with the edge
        this.setSource(fromCell.getChildAt(0));
        this.setTarget(toCell.getChildAt(0));

        // Set Arrow Style for edge
        int arrow = GraphConstants.ARROW_CLASSIC;
        GraphConstants.setLineEnd(this.getAttributes(), arrow);
        GraphConstants.setEndFill(this.getAttributes(), true);
        GraphConstants.setLineColor(this.getAttributes(), Color.BLACK);
    }

    public Connection getConnection() {
        return con;
    }

    public String getName() {
        return getConnection().getReadableId() + ".ui.edge";
    }

    public void communicating(Connection c, Event e) {
        Timer t = new Timer(0.5, "TTTT", this);
        Map attrMap1 = new Hashtable();
        int red = 32 * t.getTimerId().length();
        GraphConstants.setLineColor(attrMap1, new Color(red, 0, 0));
        Map nested = new Hashtable();
        nested.put(this, attrMap1);
        try {
            doEdit(nested);
            t.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void inform(Connection c, String information) {
    }

    public void timedOut(String id) {
        id = id.substring(1);
        Map attrMap1 = new Hashtable();
        int red = 32 * id.length();
        GraphConstants.setLineColor(attrMap1, new Color(red, 0, 0));
        Map nested = new Hashtable();
        nested.put(this, attrMap1);
        doEdit(nested);
        if (id.length() > 0) {
            Timer t = new Timer(0.5, id, this);
            t.start();
        }
    }

    private void doEdit(final Map m) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                myGraph.getGraphLayoutCache().edit(m);
            }
        });
    }

    public void log(String from, String level, String msg) {
    }
}
