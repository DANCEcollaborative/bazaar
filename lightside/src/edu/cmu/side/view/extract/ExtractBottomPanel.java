package edu.cmu.side.view.extract;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.plugin.TableFeatureMetricPlugin;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.GenericFeatureMetricPanel;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.generic.GenericMetricChecklistPanel;
import edu.cmu.side.view.util.AbstractListPanel;

public class ExtractBottomPanel extends AbstractListPanel{

	ActionBar action;

	GenericLoadPanel control = new GenericLoadPanel("Feature Table:") {	
		
		@Override
		public void setHighlight(Recipe r) {
			ExtractFeaturesControl.setHighlightedFeatureTableRecipe(r);
			Workbench.update(this);
		}

		@Override
		public void refreshPanel() {
			refreshPanel(GenesisControl.getFeatureTables());
		}

		@Override
		public Recipe getHighlight() {
			return ExtractFeaturesControl.getHighlightedFeatureTableRecipe();
		}

		@Override
		public Stage getLoadableStage()
		{
			return Stage.FEATURE_TABLE;
		}

		@Override
		public void deleteHighlight()
		{
			ExtractFeaturesControl.setHighlightedFeatureTableRecipe(null);
			
		}
	};
	
	public ExtractBottomPanel(ActionBar act){
		action = act;
		GenericMetricChecklistPanel checklist = new GenericMetricChecklistPanel<TableFeatureMetricPlugin>(){
			@Override
			public Map<TableFeatureMetricPlugin, Map<String, Boolean>> getEvaluationPlugins() {
				return ExtractFeaturesControl.getTableEvaluationPlugins();
			}

			@Override
			public ItemListener getCheckboxListener() {
				return ExtractFeaturesControl.getEvalCheckboxListener(this);
			}

			@Override
			public void setTargetAnnotation(String s) {
				ExtractFeaturesControl.setTargetAnnotation(s);
			}
			
			@Override
			public void refreshPanel()
			{
				if (ExtractFeaturesControl.hasHighlightedFeatureTable())
				{
					refreshPanel(ExtractFeaturesControl.getHighlightedFeatureTableRecipe());
				}
				else
				{
					refreshPanel(null);
				}
			}
		};
		GenericFeatureMetricPanel display = new GenericFeatureMetricPanel(){

			@Override
			public String getTargetAnnotation() {
				return ExtractFeaturesControl.getTargetAnnotation();
			}

			@Override
			public ActionBar getActionBar(){
				return action;
			}
			
			@Override
			public void refreshPanel()
			{
				if (ExtractFeaturesControl.hasHighlightedFeatureTable())
				{
					FeatureTable table = ExtractFeaturesControl.getHighlightedFeatureTableRecipe().getFeatureTable();

					//System.out.println("extract bottom panel 95: GFM refresh table "+table);
					boolean[] mask = new boolean[table.getSize()];
					for (int i = 0; i < mask.length; i++)
						mask[i] = true;
					refreshPanel(ExtractFeaturesControl.getHighlightedFeatureTableRecipe(), ExtractFeaturesControl.getTableEvaluationPlugins(), mask);
				}
				else
				{
					refreshPanel(null, ExtractFeaturesControl.getTableEvaluationPlugins(), new boolean[0]);
				}
			}
		};
		
		JSplitPane bigSplit = new JSplitPane();
		bigSplit.setLeftComponent(control);
		
		JSplitPane displaySplit = new JSplitPane();
		displaySplit.setLeftComponent(checklist);
		displaySplit.setRightComponent(display);
		
		bigSplit.setRightComponent(displaySplit);
		
		bigSplit.setBorder(BorderFactory.createEmptyBorder());
		displaySplit.setBorder(BorderFactory.createEmptyBorder());
		display.setBorder(BorderFactory.createEmptyBorder());

		control.setPreferredSize(new Dimension(275,200));
		displaySplit.setPreferredSize(new Dimension(650,200));
		checklist.setPreferredSize(new Dimension(300,200));
		display.setPreferredSize(new Dimension(325, 200));

		Dimension minimumSize = new Dimension(50, 200);
		control.setMinimumSize(minimumSize);
		checklist.setMinimumSize(minimumSize);
		display.setMinimumSize(minimumSize);
		
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, bigSplit);

		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, control);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, checklist);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, display);
		GenesisControl.addListenerToMap(control, checklist);
		GenesisControl.addListenerToMap(control, display);
		GenesisControl.addListenerToMap(checklist, display);
	}

	@Override
	public void refreshPanel()
	{
		control.refreshPanel();
	}
}