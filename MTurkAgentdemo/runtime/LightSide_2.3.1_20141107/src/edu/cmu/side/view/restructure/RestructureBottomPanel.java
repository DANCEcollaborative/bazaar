package edu.cmu.side.view.restructure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.control.RestructureTablesControl;
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

public class RestructureBottomPanel extends AbstractListPanel{

	private GenericLoadPanel control = new GenericLoadPanel("Restructured Tables:") {
		
		@Override
		public void setHighlight(Recipe r) {
			RestructureTablesControl.setHighlightedFilterTableRecipe(r);
			Workbench.update(this);
		}
		
		@Override
		public void refreshPanel() {
			refreshPanel(GenesisControl.getFilterTables());
		}
		
		@Override
		public Recipe getHighlight() {
			return RestructureTablesControl.getHighlightedFilterTableRecipe();
		}

		@Override
		public Stage getLoadableStage()
		{
			return Stage.MODIFIED_TABLE;
		}

		@Override
		public void deleteHighlight()
		{
			RestructureTablesControl.setHighlightedFilterTableRecipe(null);
		}
	};

	ActionBar action;

	public RestructureBottomPanel(ActionBar act){
		action = act;
		GenericMetricChecklistPanel checklist = new GenericMetricChecklistPanel<TableFeatureMetricPlugin>(){
			@Override
			public Map<TableFeatureMetricPlugin, Map<String, Boolean>> getEvaluationPlugins() {
				return RestructureTablesControl.getTableEvaluationPlugins();
			}

			@Override
			public ItemListener getCheckboxListener() {
				return RestructureTablesControl.getEvalCheckboxListener(this);
			}

			@Override
			public void setTargetAnnotation(String s) {
				RestructureTablesControl.setTargetAnnotation(s);
			}
			
			@Override
			public void refreshPanel(){
				if(RestructureTablesControl.hasHighlightedFilterTable()){
					
					refreshPanel(RestructureTablesControl.getHighlightedFilterTableRecipe());
				}else{
					refreshPanel(null);
				}
			}
		};
		GenericFeatureMetricPanel display = new GenericFeatureMetricPanel(){

			@Override
			public String getTargetAnnotation() {
				return RestructureTablesControl.getTargetAnnotation();
			}

			@Override
			public ActionBar getActionBar(){
				return action;
			}
			
			@Override
			public void refreshPanel(){
				if(RestructureTablesControl.hasHighlightedFilterTable()){
					FeatureTable table = RestructureTablesControl.getHighlightedFilterTableRecipe().getFilteredTable();
					boolean[] mask = new boolean[table.getDocumentList().getSize()];
					for(int i = 0; i < mask.length; i++) mask[i] = true;
					refreshPanel(RestructureTablesControl.getHighlightedFilterTableRecipe(), RestructureTablesControl.getTableEvaluationPlugins(), mask);	
				}else{
					refreshPanel(null, RestructureTablesControl.getTableEvaluationPlugins(), new boolean[0]);
				}
			}
		};
		JSplitPane pane = new JSplitPane();
		pane.setLeftComponent(control);

		pane.setBorder(BorderFactory.createEmptyBorder());
		JSplitPane right = new JSplitPane();
		right.setLeftComponent(checklist);
		right.setRightComponent(display);
		right.setBorder(BorderFactory.createEmptyBorder());
		right.setPreferredSize(new Dimension(650,200));
		pane.setRightComponent(right);
		control.setPreferredSize(new Dimension(275,200));		
		checklist.setPreferredSize(new Dimension(275,200));
		display.setPreferredSize(new Dimension(350, 200));

		Dimension minimumSize = new Dimension(50, 200);
		control.setMinimumSize(minimumSize);
		checklist.setMinimumSize(minimumSize);
		display.setMinimumSize(minimumSize);

		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, pane);

		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, control);
		GenesisControl.addListenerToMap(control, checklist);
		GenesisControl.addListenerToMap(control, display);
		GenesisControl.addListenerToMap(checklist, display);
	}
	
	@Override
	public void refreshPanel(){
		control.refreshPanel();
	}
}
