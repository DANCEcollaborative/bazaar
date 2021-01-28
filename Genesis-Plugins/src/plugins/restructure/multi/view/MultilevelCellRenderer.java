package plugins.restructure.multi.view;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.RadioButtonListEntry;

public class MultilevelCellRenderer  extends DefaultTableCellRenderer{
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int rowIndex, int vColIndex) {
		Component rend = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, vColIndex);
		if(value instanceof RadioButtonListEntry){
			RadioButtonListEntry radioButton = (RadioButtonListEntry) value;
			radioButton.setEnabled(isEnabled());
			radioButton.setFont(getFont());
			radioButton.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			rend = radioButton;
		}
		if(value instanceof CheckBoxListEntry){
			CheckBoxListEntry checkBox = (CheckBoxListEntry) value;
//			System.out.println(checkBox.isSelected() + " MCR26");
			checkBox.setEnabled(isEnabled());
			checkBox.setFont(getFont());
			checkBox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			rend = checkBox;
		}
		return rend;
	}
}
