package edu.cmu.side.view.util;

import java.util.Comparator;

import javax.swing.DefaultRowSorter;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import weka.classifiers.lazy.kstar.KStarCache.TableEntry;

/**
 * This class exists to make small changes to the default behavior of JTables to
 * fit the UI better.
 * 
 * @author emayfiel
 * 
 */
public class SIDETable extends JTable
{
	static final private SIDETableCellRenderer renderer = new SIDETableCellRenderer();
	static final private DoubleTroubleRenderer doubleRenderer = new DoubleTroubleRenderer();

	private final Comparator<Comparable> comparator = new Comparator<Comparable>()
	{
		
		public int compare(Comparable o1, Comparable o2)
		{
			int sign = getRowSorter().getSortKeys().get(0).getSortOrder() == SortOrder.ASCENDING?-1:1;
			if (o1 instanceof Double && o2 instanceof Double)
			{
				if (o1.equals(Double.NaN)) return -1*sign;
				if (o2.equals(Double.NaN)) return 1*sign;
			}
			return o1.compareTo(o2);
		}
	};
	
	public SIDETable()
	{
		setDefaultRenderer(Object.class, renderer);
		// setDefaultRenderer(String.class, renderer);
		// setDefaultRenderer(Integer.class, renderer);
		setDefaultRenderer(Double.class, doubleRenderer);
		// setDefaultRenderer(RadioButtonListEntry.class, renderer);
		// setDefaultRenderer(CheckBoxListEntry.class, renderer);

	}
	
	@Override
	public void setRowSorter(RowSorter<? extends TableModel> sorter)
	{

		super.setRowSorter(sorter);
		
		if(sorter instanceof DefaultRowSorter)
			for(int c = 0; c < getColumnCount(); c++ )
				((DefaultRowSorter) sorter).setComparator(c, comparator);
	}

	@Override
	public boolean isCellEditable(int row, int col)
	{
		return false;
	}

	public Object getDeepValue(int row, int col)
	{
		Object o = this.getValueAt(row, col);
		if (o instanceof RadioButtonListEntry)
		{
			return ((RadioButtonListEntry) o).getValue();
		}
		else if (o instanceof ToggleButtonTableEntry)
		{
			return ((ToggleButtonTableEntry) o).getValue();
		}
		else
			return o;

	}

	/**
	 * Corrects the getValueAt method for when the rows in the table have been
	 * sorted.
	 */
	public Object getSortedValue(int row, int col)
	{
		try
		{
			return getModel().getValueAt(getRowSorter().convertRowIndexToModel(row), col);
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
