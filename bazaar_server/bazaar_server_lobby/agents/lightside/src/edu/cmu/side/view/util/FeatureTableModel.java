package edu.cmu.side.view.util;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

public class FeatureTableModel extends DefaultTableModel{
	private static final long serialVersionUID = -6623645069818166916L;

	//this causes problems when there are blank/broken cells in a column.
	@Override
	public Class<?> getColumnClass(int col){
		if(this.getRowCount()>0 && this.getValueAt(0,col) != null){
			return this.getValueAt(0, col).getClass();
		}
		else return Object.class;
	}
	public FeatureTableModel(){
		this(new Vector(), new Vector());
	}
	
	public FeatureTableModel(Vector data, Vector header){
		super(data, header);
	}
}
