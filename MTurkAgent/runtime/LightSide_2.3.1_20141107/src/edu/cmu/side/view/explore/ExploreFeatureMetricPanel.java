package edu.cmu.side.view.explore;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.FeatureMetricTogglePanel;
import edu.cmu.side.view.util.RadioButtonListEntry;
import edu.cmu.side.view.util.SIDETableCellRenderer;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public class ExploreFeatureMetricPanel extends FeatureMetricTogglePanel{

	ActionBar action;
	
	public ExploreFeatureMetricPanel(ActionBar act){
		super();
		action = act;
		featureTable.addMouseListener(new ToggleMouseAdapter(featureTable, true){

			@Override
			public void setHighlight(Object row, String col) {
				if(row instanceof RadioButtonListEntry){
					ExploreResultsControl.setHighlightedFeature((Feature)((RadioButtonListEntry)row).getValue());
				}
				Workbench.update(ExploreFeatureMetricPanel.this);
			}
		});
		featureTable.setDefaultRenderer(Object.class, new SIDETableCellRenderer());
		featureTable.setDefaultRenderer(Double.class, new SIDETableCellRenderer());
	}

	@Override
	public String getTargetAnnotation() { return null; }

	@Override
	public ActionBar getActionBar(){
		return action;
	}

	@Override
	public void refreshPanel(){
		super.refreshPanel();
		Recipe target = ExploreResultsControl.getHighlightedTrainedModelRecipe();
		if(target != null){
			FeatureTable table = target.getTrainingResult().getEvaluationTable();
			boolean[] mask = new boolean[table.getDocumentList().getSize()];
			for(int i = 0; i < mask.length; i++) mask[i] = true;
			refreshPanel(target, ExploreResultsControl.getFeatureEvaluationPlugins(), mask);
		}else{
			refreshPanel(target, ExploreResultsControl.getFeatureEvaluationPlugins(), new boolean[0]);
		}
	}
}
