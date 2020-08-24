package edu.cmu.side.view.generic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.FeatureMetricPlugin;
import edu.cmu.side.view.util.CheckBoxListEntry;

public abstract class FeatureMetricCheckboxPanel extends GenericFeatureMetricPanel
{

	protected Set<Feature> selectedFeatures = new TreeSet<Feature>();
	protected JButton sortSelectedButton = new JButton("Sort Selected");

	final static String selectKey = "FeatureMetricCheckBoxPanelSelectActionKey";
	final static String deselectKey = "FeatureMetricCheckBoxPanelDeselectActionKey";

	public FeatureMetricCheckboxPanel()
	{
		super();
		
//		filterSearchField.setColumns(10);
		filterPanel.add("left", sortSelectedButton);
		
		final Comparator<CheckBoxListEntry> selectedComparator = new Comparator<CheckBoxListEntry>()
		{
			@Override
			public int compare(CheckBoxListEntry o1, CheckBoxListEntry o2)
			{
				if(o1.isSelected() ^ o2.isSelected())
					return o1.isSelected()?-1:1;
				
				return 0;
			}

		};
		
		sortSelectedButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				filterTable(model, "");
				TableRowSorter<? extends TableModel> rowSorter = (TableRowSorter<? extends TableModel>) featureTable.getRowSorter();
				List<? extends SortKey> originalKeys = rowSorter.getSortKeys();
				List<SortKey> keys = new ArrayList<SortKey>(originalKeys);
				
				Comparator oldComparator = rowSorter.getComparator(0);
				rowSorter.setComparator(0, selectedComparator);
				
				Iterator<SortKey> sortIt = keys.iterator();
				while(sortIt.hasNext())
				{
					SortKey key = sortIt.next();
					if(key.getColumn() == 0)
						sortIt.remove();
				}
				
				keys.add(0, new SortKey(0, SortOrder.ASCENDING));
				rowSorter.allRowsChanged();
				rowSorter.setSortKeys(keys);
				featureTable.repaint();
				
				rowSorter.setComparator(0, oldComparator);
				
			}
		});
		
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		KeyStroke space = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
		KeyStroke backspace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		featureTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, selectKey);
		featureTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(space, selectKey);
		featureTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(backspace, deselectKey);
		featureTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(delete, deselectKey);
		featureTable.getActionMap().put(selectKey, new AbstractAction() {

		    @Override
		    public void actionPerformed(ActionEvent e) 
		    {
		    	int[] rows = featureTable.getSelectedRows();
				
				boolean willSelect = false;
				
				for(int row : rows)
				{
					CheckBoxListEntry entry = (CheckBoxListEntry) display.getValueAt(featureTable.convertRowIndexToModel(row), 0);
					willSelect = willSelect || !entry.isSelected();
				}
				
				for(int row : rows)
				{
					CheckBoxListEntry entry = (CheckBoxListEntry) display.getValueAt(featureTable.convertRowIndexToModel(row), 0);
					entry.setSelected(willSelect);
				}	
				
				featureTable.repaint();
		    }
		});
		
		featureTable.getActionMap().put(deselectKey, new AbstractAction() {

		    @Override
		    public void actionPerformed(ActionEvent e) 
		    {
		    	int[] rows = featureTable.getSelectedRows();
				
				for(int row : rows)
				{
					CheckBoxListEntry entry = (CheckBoxListEntry) display.getValueAt(featureTable.convertRowIndexToModel(row), 0);
					entry.setSelected(false);
				}	
				
				featureTable.repaint();
		    }
		});
		
		
	}
	
	@Override
	public Object getCellObject(Object o)
	{
		CheckBoxListEntry tb = new CheckBoxListEntry(o, (o != null && selectedFeatures.contains(o)));
		tb.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent arg0)
			{
				CheckBoxListEntry entry = ((CheckBoxListEntry) arg0.getSource());
				Object value = entry.getValue();
				if (entry.isSelected())
				{
					if(value instanceof Feature)
						selectedFeatures.add((Feature) value);
					else
						System.out.println(value+" in list is not a Feature!");
				}
				else
				{
					selectedFeatures.remove(value);
				}
				
				selectedFeaturesChanged();
			}

		});
		return tb;
	}

	public abstract void selectedFeaturesChanged();
	
	@Override
	public Collection<Feature> getSelectedFeatures()
	{
		return selectedFeatures;
	}
	
	@Override
	public void refreshPanel(Recipe recipe, Map<? extends FeatureMetricPlugin, Map<String, Boolean>> tableEvaluationPlugins, boolean[] mask)
	{
		FeatureTable newTable = (recipe == null ? null : recipe.getTrainingTable());
		if(localTable != newTable)
		{
			selectedFeatures.clear();
		}
		super.refreshPanel(recipe, tableEvaluationPlugins, mask);
	}
	
	public void clearSelection()
	{
		selectedFeatures.clear();
		for(int row = 0; row < featureTable.getRowCount(); row++)
		{
			CheckBoxListEntry entry = (CheckBoxListEntry)featureTable.getValueAt(row, 0);
			entry.setSelected(false);
		}
	}
}
