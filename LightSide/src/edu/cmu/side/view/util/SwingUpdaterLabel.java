package edu.cmu.side.view.util;

import javax.swing.JLabel;

import edu.cmu.side.model.StatusUpdater;

public class SwingUpdaterLabel extends JLabel implements StatusUpdater{

	@Override
	public void update(String textSlot, int slot1, int slot2) {
		setText(textSlot + " " + slot1 + "/" + slot2);
	}
	
	@Override
	public void update(String text){
		setText(text);
	}

	@Override
	public void reset() {
		setText("");
	}
}
