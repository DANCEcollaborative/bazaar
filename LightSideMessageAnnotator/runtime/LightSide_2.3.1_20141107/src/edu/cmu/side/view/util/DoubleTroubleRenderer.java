package edu.cmu.side.view.util;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

public class DoubleTroubleRenderer extends DefaultTableCellRenderer
{

	private final static DecimalFormat decimalFormat = new DecimalFormat("#.####");
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) 
	{
		Component rend = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, vColIndex);
		
		JLabel label = (JLabel) rend;
		
		if(value instanceof Double)
		{
			if(((Double)value).isNaN())
				label.setText("?");
			else 
				label.setText(decimalFormat.format(value));
		}
		else
		{
//			System.out.println("DoubleTroubleRenderer: "+value+" is not a Double!");
			label.setText(value.toString());
		}
        
		return rend;
	}

}
