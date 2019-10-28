package edu.cmu.side.view.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.table.AbstractTableModel;

public class MapOfListsTableModel extends AbstractTableModel
{

	private Map<String, List> map;
	private String[] columnLabels = new String[0];

	public MapOfListsTableModel(Map<String, List> map)
	{
		super();
		this.map = map;
		if (map != null)
		{
			columnLabels = map.keySet().toArray(columnLabels);
		}
	}

	@Override
	public Class<?> getColumnClass(int c)
	{
		if(map != null && map.get(columnLabels[c]) != null && !map.get(columnLabels[c]).isEmpty())
		{
			return map.get(columnLabels[c]).get(0).getClass();
		}
		else return Object.class;
	}

	@Override
	public int getColumnCount()
	{
		if (map == null) return 0;

		return columnLabels.length;
	}

	@Override
	public String getColumnName(int c)
	{
		if (c < columnLabels.length)
			return columnLabels[c];
		else
			return "?";
	}

	@Override
	public int getRowCount()
	{
		if (map == null) return 0;

		return map.get(columnLabels[0]).size();
	}

	@Override
	public Object getValueAt(int r, int c)
	{
		if (c < columnLabels.length &&
				map.containsKey(columnLabels[c]) &&
				r < map.get(columnLabels[c]).size() &&
				map.get(columnLabels[c]).get(r) != null) 
		{	
			return map.get(columnLabels[c]).get(r);
		}
		return "?";
	}

	@Override
	public boolean isCellEditable(int arg0, int arg1)
	{
		return false;
	}

	@Override
	public void setValueAt(Object arg0, int arg1, int arg2)
	{
		// TODO Auto-generated method stub
	}

	public void setMap(Map<String, List> map)
	{
		if (map != null)
		{
			columnLabels = new TreeSet<String>(map.keySet()).toArray(columnLabels);
		}
		else
		{
			columnLabels = new String[0];
		}
		this.map = map;

		this.fireTableStructureChanged();
		this.fireTableDataChanged();
	}

	public void setColumnLabels(List<String> labels)
	{
		if(map != null)
		if(labels.size() == map.size())
		{
			columnLabels= labels.toArray(columnLabels);
		}
		else System.err.println("Incorrect number of labels: given "+labels.size()+", expected "+map.size());
	}

	public void setMap(HashMap<String, List> map, List<String> labels)
	{
		this.map = map;
		setColumnLabels(labels);

		this.fireTableStructureChanged();
		this.fireTableDataChanged();
	}
}
