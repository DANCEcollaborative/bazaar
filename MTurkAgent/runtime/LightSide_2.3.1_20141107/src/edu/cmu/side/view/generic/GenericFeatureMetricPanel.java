package edu.cmu.side.view.generic;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.FeatureMetricPlugin;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CSVExporter;
import edu.cmu.side.view.util.FeatureTableModel;
import edu.cmu.side.view.util.SIDETable;

public abstract class GenericFeatureMetricPanel extends AbstractListPanel {

	protected SIDETable featureTable = new SIDETable();
	protected FeatureTableModel model = new FeatureTableModel();
	protected FeatureTableModel display = new FeatureTableModel();
	protected JTextField filterSearchField = new JTextField();
	protected JButton exportButton = new JButton("");
	protected JLabel nameLabel = new JLabel("Features in Table:");
	protected JPanel filterPanel = new JPanel(new RiverLayout(0,0));

	
	//FIXME: check for race conditions with evaluations in different panels - if it's an issue, make sure every panel gets updated that needs to.
	protected static boolean evaluating = false;


	public static void setEvaluating(boolean e){
		evaluating = e;
	}

	public static boolean isEvaluating(){
		return evaluating;
	}

	protected FeatureTable localTable;

	public GenericFeatureMetricPanel(){
		setLayout(new RiverLayout(10, 5));
		setBorder(BorderFactory.createEmptyBorder());
		featureTable.setModel(model);
		featureTable.setBorder(BorderFactory.createLineBorder(Color.gray));
		featureTable.setAutoCreateColumnsFromModel(false);
		
		exportButton.setIcon(new ImageIcon("toolkits/icons/note_go.png"));
		exportButton.setToolTipText("Export to CSV...");
		exportButton.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				CSVExporter.exportToCSV(model);
			}});
		exportButton.setEnabled(false);

		JScrollPane tableScroll = new JScrollPane(featureTable);
		filterSearchField.addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent arg0) {}

			@Override
			public void keyReleased(KeyEvent arg0) {
				String filterText = filterSearchField.getText();
				display = filterTable(model, filterText);
				featureTable.setModel(display);
				TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(display);
				featureTable.setRowSorter(sorter);
				featureTable.revalidate();
			}

			@Override
			public void keyTyped(KeyEvent arg0) {}

		});
		
		//nameLabel.setBorder(BorderFactory.createEmptyBorder(10,0,5,0));
		filterPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		
		add("hfill", nameLabel);
		add("right", exportButton);
		add("br hfill", filterPanel);
		filterPanel.add("left", new JLabel("Search:"));
		filterPanel.add("hfill", filterSearchField);
		//add("br left", new JLabel("Search:"));
		//add("hfill", filterSearchField);
		add("br hfill vfill", tableScroll);
	}

	public void refreshPanel(Recipe recipe, Map<? extends FeatureMetricPlugin, Map<String, Boolean>> tableEvaluationPlugins, boolean[] mask)
	{
//		System.out.println("GFM 108: refresh recipe "+recipe+", already evaluating? "+isEvaluating());
//		int countTrues = 0;
//		for(FeatureMetricPlugin plug : tableEvaluationPlugins.keySet()){
//			for(String s : tableEvaluationPlugins.get(plug).keySet()){
//				if(tableEvaluationPlugins.get(plug).get(s)){
//					countTrues++;
//				}
//			}
//		}
		FeatureTable newTable = (recipe == null ? null : recipe.getTrainingTable());
		if(!isEvaluating() )//&& localTable != newTable)
		{
			localTable = newTable;
			EvaluateFeaturesTask task = new EvaluateFeaturesTask(getActionBar(), recipe, tableEvaluationPlugins, mask, getTargetAnnotation());
			task.executeActionBarTask();
		}
		
		exportButton.setEnabled(recipe != null);
	}

	public FeatureTableModel filterTableForSelected(FeatureTableModel ftm, boolean selected)
	{
		Vector<Object> header = new Vector<Object>();
		Vector<Vector<Object>> rows = new Vector<Vector<Object>>();

		for (int i = 0; i < ftm.getColumnCount(); i++)
		{
			header.add(ftm.getColumnName(i));
		}
		if (header.size() > 0)
		{
			for (int i = 0; i < ftm.getRowCount(); i++)
			{
				try
				{
					if (ftm.getValueAt(i, 0) != null && ((AbstractButton) ftm.getValueAt(i, 0)).isSelected() == selected)
					{
						Vector<Object> row = new Vector<Object>();
						for (int j = 0; j < ftm.getColumnCount(); j++)
						{
							row.add(ftm.getValueAt(i, j));
						}
						rows.add(row);
					}
				}
				catch (Exception e)
				{
				}
			}
		}
		FeatureTableModel disp = new FeatureTableModel(rows, header);

		return disp;
	}
	
	public FeatureTableModel filterTable(FeatureTableModel ftm, String t){
		Vector<Object> header = new Vector<Object>();
		Vector<Vector<Object>> rows = new Vector<Vector<Object>>();

		for(int i = 0; i < ftm.getColumnCount(); i++){
			header.add(ftm.getColumnName(i));
		}
		if(header.size() > 0){
			for(int i = 0; i < ftm.getRowCount(); i++){
				try{
					if(ftm.getValueAt(i, 0) != null && ftm.getValueAt(i, 0).toString().contains(t)){
						Vector<Object> row = new Vector<Object>();
						for(int j = 0; j < ftm.getColumnCount(); j++){
							row.add(ftm.getValueAt(i,j));
						}
						rows.add(row);
					}					
				}catch(Exception e){}
			}			
		}
		FeatureTableModel disp = new FeatureTableModel(rows, header);

		return disp;
	}


	public class EvaluateFeaturesTask extends ActionBarTask
	{
		Recipe recipe;
		Map<? extends FeatureMetricPlugin, Map<String, Boolean>> tableEvaluationPlugins;
		boolean[] mask;
		String target;
		FeatureMetricPlugin activePlugin;

		Vector<Object> header = new Vector<Object>();
		Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
		List<SortKey> sortKeysToPass = new ArrayList<SortKey>();

		public EvaluateFeaturesTask(ActionBar action, Recipe r, Map<? extends FeatureMetricPlugin, Map<String, Boolean>> plugins, boolean[] m, String t)
		{
			super(action);
			recipe = r;
			target = t;
			tableEvaluationPlugins = plugins;
			mask = m;
		}
		
		@Override
		protected void beginTask()
		{
//				System.out.println("GFMC 181: begin eval task");
				GenericFeatureMetricPanel.setEvaluating(true);
				combo.setEnabled(false);
				super.beginTask();
		}
		
		@Override
		protected void finishTask()
		{
//			System.out.println("GFMC 190: finish eval task");
			super.finishTask();
			//Filter the table based on searching and re-sort the display.
			if(!halt)
			{
				model = new FeatureTableModel(rows, header);
				
//				for(Vector<Object> row : rows)
//					System.out.println("GFMC 197: rows="+row);
				
				List<SortKey> newSortKeys = new ArrayList<SortKey>();

				display = filterTable(model, filterSearchField.getText());
				TableColumnModel columns = new DefaultTableColumnModel();
				for(int i = 0; i < display.getColumnCount(); i++)
				{
					TableColumn col = new TableColumn(i);
					String columnName = display.getColumnName(i);
					col.setHeaderValue(columnName);
					columns.addColumn(col);
				}
				
				for(SortKey key: sortKeysToPass)
				{
					if(display.getColumnCount() > key.getColumn())
					{
						newSortKeys.add(key);
					}
				}
				
				
				
				featureTable.setColumnModel(columns);
				featureTable.setModel(display);
				TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(display);
				sorter.setSortKeys(newSortKeys);
				sorter.setSortsOnUpdates(true);
				featureTable.setRowSorter(sorter);
				featureTable.sorterChanged(new RowSorterEvent(sorter));
			}

			GenericFeatureMetricPanel.setEvaluating(false);
			combo.setEnabled(true);
			
			actionBar.update.reset();
		}

		@Override
		protected void doTask()
		{
			try
			{
				// Store the way that the table was sorted prior to refreshing
				if(featureTable.getRowSorter() != null){
					List<? extends SortKey> sortKeys = featureTable.getRowSorter().getSortKeys();
					for(SortKey key : sortKeys){
						String colName = featureTable.getColumnName(key.getColumn());
						SortOrder order = key.getSortOrder();
						for(int i = 0; i < model.getColumnCount(); i++){
							if(model.getColumnName(i).equals(colName)){
								sortKeysToPass.add(new SortKey(i, order));
							}
						}
					}
				}
				
				header.add("Feature");
				int rowCount = 1;

				Map<FeatureMetricPlugin, Map<String, Map<Feature, Comparable>>> evals = new HashMap<FeatureMetricPlugin, Map<String, Map<Feature, Comparable>>>();

				if (localTable != null)
				{
					// Generate evaluations for each selected option within a
					// plugin
					for (FeatureMetricPlugin plug : tableEvaluationPlugins.keySet())
					{
						activePlugin = plug;
						evals.put(plug, new TreeMap<String, Map<Feature, Comparable>>());
						
						synchronized(plug)
						{
							for (String s : tableEvaluationPlugins.get(plug).keySet())
							{
								if (tableEvaluationPlugins.get(plug).get(s) && !halt)
								{
									header.add(s);
									rowCount++;
									Map<Feature, Comparable> values = plug.evaluateFeatures(recipe, mask, s, target, actionBar.update);
									evals.get(plug).put(s, values);
								}
							}
						}
					}

					// Fill the table's row with the evaluations we just
					// generated
					for (Feature f : localTable.getFeatureSet())
					{
						Vector<Object> row = new Vector<Object>();
						row.add(getCellObject(f));
						for (FeatureMetricPlugin plug : tableEvaluationPlugins.keySet())
						{
							for (String s : tableEvaluationPlugins.get(plug).keySet())
							{
								if (tableEvaluationPlugins.get(plug).get(s))
								{
									Object value = Double.NaN;
									if (evals.get(plug).containsKey(s) && evals.get(plug).get(s) != null)
									{
										Object tryVal = evals.get(plug).get(s).get(f);
										if (tryVal != null)
										{
											value = tryVal;
										}
									}
									row.add(value);
								}
							}
						}
						rows.add(row);
					}
				}

			}
			catch (Exception e)
			{
				System.err.println("Generic Feature Metric Panel: Error while updating metrics.");
				e.printStackTrace();
			}
		}

		@Override
		public void requestCancel()
		{
			System.out.println("GFMC cancelling...");
			activePlugin.stopWhenPossible();
		}
	}

	public Object getCellObject(Object o){
		return o;
	}

	public abstract String getTargetAnnotation();

	public abstract ActionBar getActionBar();

	public Collection<Feature> getSelectedFeatures()
	{
		int[] rows = featureTable.getSelectedRows();
		List<Feature> features = new ArrayList<Feature>(rows.length);
		
		for(int row : rows)
		{
			features.add((Feature) display.getValueAt(featureTable.convertRowIndexToModel(row), 0));
		}
		
		return features;
	}
}
