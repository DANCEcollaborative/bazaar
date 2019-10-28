package edu.cmu.side.view.util;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JRadioButton;

public abstract class ToggleMouseAdapter extends MouseAdapter{
	SIDETable panel;
	Boolean flip;
	public ToggleMouseAdapter(SIDETable p, boolean f){
		super();
		panel = p;
		flip = f;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		int col = panel.columnAtPoint(e.getPoint());
		int row = panel.rowAtPoint(e.getPoint());
		if (col != -1 && row != -1) {
			Object rowObj = panel.getValueAt(row, 0);
			String colName = panel.getColumnName(col);
			setHighlight(rowObj, colName);
			Object obj = panel.getValueAt(row, col);
			if (obj instanceof JCheckBox && flip) {
				JCheckBox checkbox = (JCheckBox) obj;
				checkbox.setSelected(!checkbox.isSelected());
				panel.repaint();
			}
			if(obj instanceof JRadioButton && flip){
				JRadioButton radio = (JRadioButton) obj;
				radio.setSelected(!radio.isSelected());
				panel.repaint();
			}
		}
	}
	
	public abstract void setHighlight(Object rowObj, String col);
}