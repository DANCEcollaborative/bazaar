package edu.cmu.side.view.util;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import se.datadosen.component.RiverLayout;

public class AbstractListPanel extends JPanel implements Refreshable{
	private static final long serialVersionUID = -1090634417229954402L;

	protected FastListModel listModel;
	protected JList list;
	protected JScrollPane listScroll;

	protected JTextArea description;
	protected JScrollPane describeScroll;
	
	protected JComboBox combo;
	
	protected JButton add;
	protected JButton delete;
	protected JButton clear;
	protected JButton save;
	protected JButton load;
	
	private void init(){
		this.setLayout(new RiverLayout());
		listModel = new FastListModel();
		list = new JList();
		list.setModel(listModel);	
		combo = new JComboBox();
		//combo.setRenderer(new AbbreviatedComboBoxCellRenderer(30));

		listScroll = new JScrollPane(list);
		description = new JTextArea();
		description.setEditable(false);
		describeScroll = new JScrollPane(description);
		add = new JButton("Add");
		delete = new JButton("Delete");
		clear = new JButton("Clear");
		save = new JButton("Save");
		load = new JButton("Load");
		
	}
	
	public AbstractListPanel(){
		init();
	}

	@Override
	public void refreshPanel(){
		if(listModel.getSize()>0 && list.getSelectedIndex()==-1){
			list.setSelectedIndex(0);
		}
	}

	private List<ActionListener> actionListenerList = new ArrayList<ActionListener>();

	public void addActionListener(ActionListener al){
		this.actionListenerList.add(al);
	}
	public void removeActionListener(ActionListener al){
		this.actionListenerList.remove(al);
	}
	
}
