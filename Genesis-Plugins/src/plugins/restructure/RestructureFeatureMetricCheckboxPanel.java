package plugins.restructure;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import plugins.metrics.features.tables.BasicFeatureEvaluations;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.plugin.TableFeatureMetricPlugin;
import edu.cmu.side.plugin.control.PluginManager;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.FeatureMetricCheckboxPanel;
import edu.cmu.side.view.util.SIDETableCellRenderer;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public abstract class RestructureFeatureMetricCheckboxPanel extends FeatureMetricCheckboxPanel
{

	protected JComboBox targetLabelBox = new JComboBox();
	protected Set<String> possibleTargets;
	protected JLabel targetLabelLabel = new JLabel("Target: ");
	Map<TableFeatureMetricPlugin, Map<String, Boolean>> evaluators = new HashMap<TableFeatureMetricPlugin, Map<String, Boolean>>();
	
	public RestructureFeatureMetricCheckboxPanel()
	{
		this.remove(nameLabel);
		this.remove(exportButton);

		filterPanel.removeAll();
		filterPanel.add("left", targetLabelLabel);
//		filterPanel.add("hfill", new JPanel());
		filterPanel.add("left", targetLabelBox);
		filterPanel.add("br left", new JLabel("Search:"));
		filterPanel.add("hfill", filterSearchField);
		filterPanel.add("right", sortSelectedButton);
		
		featureTable.addMouseListener(new ToggleMouseAdapter(featureTable, true)
		{

			@Override
			public void setHighlight(Object row, String col)
			{
				
				Workbench.update(this); // TODO: there will be other
										// things to update in the UI
										// soon enough.
			}
		});
		featureTable.setDefaultRenderer(Object.class, new SIDETableCellRenderer());

		targetLabelBox.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				RestructureFeatureMetricCheckboxPanel.this.refreshPanel();
			}
		});
	}
	

	@Override
	public String getTargetAnnotation()
	{
		Object selectedItem = targetLabelBox.getSelectedItem();
		if (selectedItem != null)
			return targetLabelBox.getSelectedItem().toString();
		else
			return "";
	}


	@Override
	public ActionBar getActionBar()
	{
		return RestructureTablesControl.getActionBar();
	}

	@Override
	public void refreshPanel()
	{

		super.refreshPanel();
		Recipe selectedRecipe = getFeatureTableRecipe();
		
		if (selectedRecipe != null)
		{
			FeatureTable table = selectedRecipe.getTrainingTable();
			
			Set<String> possibles = new TreeSet<String>(table.getNominalClassValues());
			if (!possibles.equals(possibleTargets)) targetLabelBox.setModel(new DefaultComboBoxModel(possibles.toArray()));
			possibleTargets = possibles;

			boolean[] mask = new boolean[table.getDocumentList().getSize()];

			for (int i = 0; i < mask.length; i++)
				mask[i] = true;

			updateEvaluators();

			refreshPanel(selectedRecipe, evaluators, mask);
		}
		else
		{
			targetLabelBox.setModel(new DefaultComboBoxModel());
			refreshPanel(null, evaluators, new boolean[0]);
		}

	}


	protected Recipe getFeatureTableRecipe()
	{
		Recipe selectedRecipe = RestructureTablesControl.getHighlightedFeatureTableRecipe();
		return selectedRecipe;
	}

	protected void updateEvaluators()
	{
		
		if (evaluators.isEmpty())
		{
			if (RestructureTablesControl.hasHighlightedFeatureTable())
			{
				Recipe selectedRecipe = getFeatureTableRecipe();
				TableFeatureMetricPlugin basicEvaluator = (TableFeatureMetricPlugin) PluginManager.getPluginByClassname(BasicFeatureEvaluations.class
						.getName());
				evaluators.put(basicEvaluator, new HashMap<String, Boolean>());
				evaluators.get(basicEvaluator).put("Total Hits", true);
				evaluators.get(basicEvaluator).put("Target Hits", true);
				evaluators.get(basicEvaluator).put("Correlation", true);
				evaluators.get(basicEvaluator).put("Kappa", true);
				
			}
		}
	}



}