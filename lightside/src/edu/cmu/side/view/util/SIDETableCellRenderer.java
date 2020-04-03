package edu.cmu.side.view.util;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

public class SIDETableCellRenderer extends DefaultTableCellRenderer{

	DecimalFormat decimalFormat = new DecimalFormat("#.####");
	private int cutoff = 100;

	public SIDETableCellRenderer(int cutoff)
	{
		super();
		this.cutoff = cutoff;
	}
	
	public SIDETableCellRenderer()
	{
		super();
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
		Component rend = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, vColIndex);
		
		if(!isSelected)
			rend.setBackground(Color.white);

		if(value instanceof RadioButtonListEntry){
			RadioButtonListEntry radioButton = (RadioButtonListEntry) value;
			radioButton.setEnabled(isEnabled());
			radioButton.setFont(getFont());
			radioButton.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			rend = radioButton;

	        if(radioButton.getText().length() > cutoff )
	        {
	        	radioButton.setText(radioButton.getText().substring(0, cutoff)+"...");
	        	
	        }
		}
		else if(value instanceof CheckBoxListEntry){
			CheckBoxListEntry checkButton = (CheckBoxListEntry) value;
			checkButton.setEnabled(isEnabled());
			checkButton.setFont(getFont());
			checkButton.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			rend = checkButton;
			
	        if(checkButton.getText().length() > cutoff )
	        {
	        	checkButton.setText(checkButton.getText().substring(0, cutoff)+"...");
	        	
	        }
			
		}
		else if(value instanceof Double && rend instanceof JLabel)
		{
			JLabel label = (JLabel) rend;
			if(((Double)value).isNaN())
				label.setText("?");
			else 
				label.setText(decimalFormat.format(value));
		}
		
		
        
		return rend;
	}
}
