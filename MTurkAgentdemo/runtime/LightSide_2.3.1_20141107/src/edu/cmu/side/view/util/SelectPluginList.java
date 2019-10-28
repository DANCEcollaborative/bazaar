package edu.cmu.side.view.util;

/*
 * Copyright (C) 2005 - 2007 JasperSoft Corporation.  All rights reserved. 
 * http://www.jaspersoft.com.
 *
 * Unless you have purchased a commercial license agreement from JasperSoft,
 * the following license terms apply:
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published by
 * the Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; and without the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see http://www.gnu.org/licenses/gpl.txt
 * or write to:
 *
 * Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330,
 * Boston, MA  USA  02111-1307
 *
 *
 *
 *
 * CheckBoxList.java
 * 
 * Created on October 5, 2006, 9:53 AM
 *
 */

/**
 *
 * @author gtoffoli
 */
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import edu.cmu.side.plugin.SIDEPlugin;


public class SelectPluginList extends JList {

	public SelectPluginList() {
		super();

		setModel(new FastListModel());
		setCellRenderer(new SelectPluginCellRenderer());

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int index = locationToIndex(e.getPoint());

				if (index != -1) {
					Object obj = ((FastListModel)getModel()).get(index);
					if (obj instanceof JCheckBox) {
						JCheckBox checkbox = (JCheckBox) obj;
						if(checkbox.isEnabled())
							checkbox.setSelected(!checkbox.isSelected());
						repaint();
					}
					if(obj instanceof JRadioButton){
						JRadioButton radio = (JRadioButton) obj;
						if(radio.isEnabled())
							radio.setSelected(!radio.isSelected());
						repaint();
					}
				}
			}
		}

				);

		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}

	public int[] getCheckedIndices() {
		List<Integer> list = new ArrayList<Integer>();
		FastListModel dlm = (FastListModel) getModel();
		for (int i = 0; i < dlm.size(); ++i) {
			Object obj = dlm.get(i);
			if (obj instanceof JCheckBox) {
				JCheckBox checkbox = (JCheckBox) obj;
				if (checkbox.isSelected()) {
					list.add(new Integer(i));
				}
			}
		}

		int[] indices = new int[list.size()];

		for (int i = 0; i < list.size(); ++i) {
			indices[i] = list.get(i).intValue();
		}

		return indices;
	}

	public List<JCheckBox> getCheckedItems() {
		List<JCheckBox> list = new ArrayList<JCheckBox>();
		FastListModel dlm = (FastListModel) getModel();
		for (int i = 0; i < dlm.size(); ++i) {
			Object obj = dlm.get(i);
			if (obj instanceof JCheckBox) {
				JCheckBox checkbox = (JCheckBox) obj;
				if (checkbox.isSelected()) {
					list.add(checkbox);
				}
			}
		}
		return list;
	}
}

/*
 * Copyright (C) 2005 - 2007 JasperSoft Corporation. All rights reserved.
 * http://www.jaspersoft.com.
 * 
 * Unless you have purchased a commercial license agreement from JasperSoft, the
 * following license terms apply:
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 * 
 * This program is distributed WITHOUT ANY WARRANTY; and without the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see http://www.gnu.org/licenses/gpl.txt or write to:
 * 
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA USA
 * 02111-1307
 * 
 * 
 * 
 * 
 * CheckboxCellRenderer.java
 * 
 * Created on October 5, 2006, 10:03 AM
 * 
 */

/**
 * 
 * @author gtoffoli
 */
class SelectPluginCellRenderer extends DefaultListCellRenderer {
	protected static Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index,
			boolean isSelected, boolean cellHasFocus) {
		if(value instanceof SIDEPlugin){
			JLabel label = new JLabel(value.toString());
			return label;
		}else if (value instanceof CheckBoxListEntry) {
			CheckBoxListEntry checkbox = (CheckBoxListEntry) value;
			checkbox.setFont(getFont());
			checkbox.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			checkbox.setEnabled(checkbox.isEnabled());
			checkbox.setSelected(checkbox.isSelected());
			
			return checkbox;
		} else if(value instanceof RadioButtonListEntry){
			RadioButtonListEntry radioButton = (RadioButtonListEntry) value;
			radioButton.setFont(getFont());
			radioButton.setBorder(isSelected ? UIManager.getBorder("List.focusCellHighlightBorder") : noFocusBorder);
			return radioButton;
		} else if(value != null){
			return super.getListCellRendererComponent(list, value.getClass().getName(), index,
					isSelected, cellHasFocus);
		}else{
			return super.getListCellRendererComponent(list, "", index,
					isSelected, cellHasFocus);    	
		}
	}

}

/*
 * Copyright (C) 2005 - 2007 JasperSoft Corporation. All rights reserved.
 * http://www.jaspersoft.com.
 * 
 * Unless you have purchased a commercial license agreement from JasperSoft, the
 * following license terms apply:
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 * 
 * This program is distributed WITHOUT ANY WARRANTY; and without the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see http://www.gnu.org/licenses/gpl.txt or write to:
 * 
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA USA
 * 02111-1307
 * 
 * 
 * 
 * 
 * CheckBoxListEntry.java
 * 
 * Created on October 5, 2006, 10:19 AM
 * 
 */
