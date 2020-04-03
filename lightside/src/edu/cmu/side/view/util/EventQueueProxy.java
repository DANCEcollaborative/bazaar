package edu.cmu.side.view.util;

import java.awt.AWTEvent;
import java.awt.EventQueue;

import javax.swing.JOptionPane;

public class EventQueueProxy extends EventQueue {
	 
    protected void dispatchEvent(AWTEvent newEvent) {
        try {
            super.dispatchEvent(newEvent);
        } catch (Throwable t) {
            t.printStackTrace();
            String message = t.getClass().getSimpleName() +":\n"+ t.getMessage();
 
            if (message == null || message.length() == 0) {
                message = t.getClass().getSimpleName();
            }
 
            JOptionPane.showMessageDialog(null, "Hi there! Something has gone wrong in the user interface -\n"
            									+ "probably nothing to worry about, \n"
            									+ "but it's possible you'll need to quit LightSide\n"
            									+ "and open it again.\n"+message, "Workbench Error", JOptionPane.ERROR_MESSAGE);
            
            t.printStackTrace();
        }
    }
}