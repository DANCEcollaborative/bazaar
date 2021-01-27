package plugins.analysis.two.matrix;

import java.util.Collection;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.FeatureMetricTogglePanel;
import edu.cmu.side.view.util.RadioButtonListEntry;
import edu.cmu.side.view.util.SIDETableCellRenderer;
import edu.cmu.side.view.util.ToggleMouseAdapter;

public class DifferenceFeatureMetricPanel extends FeatureMetricTogglePanel {

	ActionBar action;
	public DifferenceFeatureMetricPanel(ActionBar act){
		super();
		action = act;
		featureTable.addMouseListener(new ToggleMouseAdapter(featureTable, false){

			@Override
			public void setHighlight(Object row, String col) {
				if(row instanceof RadioButtonListEntry){
					DifferenceMatrixPlugin.setHighlightedFeature((Feature)((RadioButtonListEntry)row).getValue());
					Workbench.update(DifferenceFeatureMetricPanel.this);
				}
			}
		});
		featureTable.setDefaultRenderer(Object.class, new SIDETableCellRenderer());
	}
	
	@Override
	public String getTargetAnnotation() { return null; }
	
	@Override
	public ActionBar getActionBar() { return action; }
	
	@Override
	public void refreshPanel(){
		super.refreshPanel();
		Recipe target = CompareModelsControl.getBaselineTrainedModelRecipe();
		if(target != null){
			FeatureTable table = target.getTrainingResult().getEvaluationTable();
			boolean highlightCell = DifferenceMatrixPlugin.getHighlightedColumn() != null && DifferenceMatrixPlugin.getHighlightedRow() != null;
			boolean[] mask = new boolean[table.getDocumentList().getSize()];
			for(int i = 0; i < mask.length; i++){
				mask[i] = !highlightCell;
			}
			if(highlightCell){
				Collection<Integer> uniques = (DifferenceMatrixPlugin.isCalculateBaseline()?DifferenceMatrixPlugin.uniqueBaseline:(DifferenceMatrixPlugin.isCalculateCompete()?DifferenceMatrixPlugin.uniqueCompete:null));
				if(uniques != null){
					for(Integer i : uniques){
						mask[i] = true;
					}
				}else{
					for(int i = 0; i < mask.length; i++){
						mask[i] = true;
					}
				}
			}
			
			refreshPanel(target, DifferenceMatrixPlugin.getMetricPlugins(), mask);
		}else{
			refreshPanel(target, DifferenceMatrixPlugin.getMetricPlugins(), new boolean[0]);
		}
	}
}