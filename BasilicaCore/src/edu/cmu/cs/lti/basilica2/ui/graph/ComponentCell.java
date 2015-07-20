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

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.observers.ComponentObserver;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Map;
import javax.swing.SwingUtilities;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;

/**
 *
 * @author rohitk
 */
public class ComponentCell extends DefaultGraphCell implements ComponentObserver, TimeoutReceiver {

    Component com = null;
    JGraph myGraph = null;
    Timer currentTimer = null;

    public ComponentCell(Component c, JGraph g) {
        this(c, g, 20, 20, 120, 20);
    }

    public ComponentCell(Component c, JGraph g, double x, double y, double w, double h) {
        super(c);
        com = c;
        myGraph = g;
        c.addObserver(this);

        // Set bounds
        GraphConstants.setBounds(this.getAttributes(), new Rectangle2D.Double(x, y, w, h));

        //Set Background
        //GraphConstants.setGradientColor(this.getAttributes(), bg);
        //GraphConstants.setOpaque(this.getAttributes(), true);

        // Set raised border
        //GraphConstants.setBorder(this.getAttributes(), BorderFactory.createRaisedBevelBorder());
        GraphConstants.setBorderColor(this.getAttributes(), Color.BLACK);

        // Add a Floating Port
        this.addPort();
    }

    public Component getComponent() {
        return com;
    }

    public String getName() {
        return this.getComponent().getName() + ".ui.cell";
    }

    public void eventReceived(Component c, Event e) {
        if (currentTimer != null) {
            currentTimer.stopAndQuit();
        }
        currentTimer = new Timer(2.0, this);
        Map attrMap1 = new Hashtable();
        GraphConstants.setBorderColor(attrMap1, Color.GREEN);
        Map nested = new Hashtable();
        nested.put(this, attrMap1);
        try {
            doEdit(nested);
            currentTimer.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void eventSent(Component c, Event e) {
        if (currentTimer != null) {
            currentTimer.stopAndQuit();
        }
        currentTimer = new Timer(2.0, this);
        Map attrMap1 = new Hashtable();
        GraphConstants.setBorderColor(attrMap1, Color.BLUE);
        Map nested = new Hashtable();
        nested.put(this, attrMap1);
        try {
            doEdit(nested);
            currentTimer.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void inform(Component c, String information) {
    }

    public void timedOut(String id) {
        currentTimer = null;
        Map attrMap1 = new Hashtable();
        GraphConstants.setBorderColor(attrMap1, Color.BLACK);
        Map nested = new Hashtable();
        nested.put(this, attrMap1);
        try {
            doEdit(nested);
        } catch (Exception e) {
            e.printStackTrace();
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
