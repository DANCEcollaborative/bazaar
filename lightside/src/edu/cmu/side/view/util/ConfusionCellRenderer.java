package edu.cmu.side.view.util;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import edu.cmu.side.view.generic.GenericMatrixPanel;

public class ConfusionCellRenderer  extends DefaultTableCellRenderer{
	GenericMatrixPanel parent;

	DecimalFormat print = new DecimalFormat("#.###");
	public ConfusionCellRenderer(GenericMatrixPanel p){
		parent = p;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
		Double[] sum = parent.getSum();
		Component rend = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, vColIndex);
		
		rend.setFocusable(false);
		rend.setBackground(Color.white);
		rend.setForeground(Color.black);

		if(value instanceof RadioButtonListEntry){
			RadioButtonListEntry radioButton = (RadioButtonListEntry) value;
			radioButton.setEnabled(isEnabled());
			radioButton.setFont(getFont());
			radioButton.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			rend = radioButton;
		}

		if(vColIndex > 0){
			Integer intensity = 0;
			try{
				Object deep = ((SIDETable)table).getDeepValue(rowIndex, vColIndex);
		        double numberValue = 0.0;
		        boolean success = true;
				if(deep != null){
					String contents = deep.toString();					
					try{
						numberValue = (Double.parseDouble(contents));
					}catch(Exception e){
						success = false;
					}
				}

				if(success)
				{
					int r=255, g=255, b=255;
					if(rend instanceof DefaultTableCellRenderer){
				        ((DefaultTableCellRenderer)rend).setText(print.format(numberValue));						
					}
					if(numberValue > 0){
						intensity = (int)((255*numberValue) / sum[1]);
						r = 255-intensity;
						g = 255-intensity;
						b = 255;
					}
					else if(numberValue < 0)
					{
						intensity = (int)((255*-numberValue) / sum[1]);
						
						r = 255;
						g = 255-(intensity/2);
						b = 255-intensity;
						
					}
					r = restrict(r, 0, 255);
					g = restrict(g, 0, 255);
					b = restrict(b, 0, 255);
					
					rend.setBackground(new Color(r, g, b));
					rend.setForeground(intensity<128?Color.black:Color.white);
					
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		return rend;
	}
	
	private int restrict(int x, int min, int max)
	{
		if(x < min)
			return min;
		else if(x > max)
			return max;
		else return x;
	}
}
