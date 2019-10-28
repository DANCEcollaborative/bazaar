package edu.cmu.side.view.util;

import javax.swing.*; 
import java.awt.*; 
import java.util.Vector; 

// got this workaround from the following bug: 
//      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607 
public class WideComboBox extends JComboBox{ 

    public WideComboBox() { 
    } 

    public WideComboBox(final Object items[]){ 
        super(items); 
    } 

    public WideComboBox(Vector items) { 
        super(items); 
    } 

        public WideComboBox(ComboBoxModel aModel) { 
        super(aModel); 
    } 

    private boolean layingOut = false; 

    @Override
	public void doLayout(){ 
        try{ 
            layingOut = true; 
                super.doLayout(); 
        }finally{ 
            layingOut = false; 
        } 
    } 

    @Override
	public Dimension getSize(){ 
        Dimension dim = super.getSize(); 
        if(!layingOut) 
            dim.width = Math.max(dim.width, getPreferredSize().width); 
        return dim; 
    } 
}