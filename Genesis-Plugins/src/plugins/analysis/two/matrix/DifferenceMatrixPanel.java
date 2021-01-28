package plugins.analysis.two.matrix;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.ModelFeatureMetricPlugin;
import edu.cmu.side.view.generic.GenericMatrixPanel;

public class DifferenceMatrixPanel extends GenericMatrixPanel{

	
	public DifferenceMatrixPanel(ModelFeatureMetricPlugin p, String s, String l){
		super(p,s);
		label.setText(l);
	}
	private double minVal = Double.MAX_VALUE;
	private double maxVal = Double.MIN_VALUE;
	
	@Override
	public Double[] getSum(){
		return new Double[]{minVal, maxVal};
	}
	
	@Override
	protected Vector<Vector<Object>> generateRows(Map<String, Map<String, List<Integer>>> confusion, Collection<String> labels){
		Vector<Vector<Object>> rows = new Vector<Vector<Object>>();
		TrainingResult baseline = CompareModelsControl.getBaselineTrainedModelRecipe().getTrainingResult();
		TrainingResult competing = CompareModelsControl.getCompetingTrainedModelRecipe().getTrainingResult();
		if(DifferenceMatrixPlugin.getHighlightedFeature() != null){
			for(String act : labels){
				Vector<Object> row = new Vector<Object>();
				row.add(act);
				int index = 1;
				for(String pred : labels){
					for(ModelFeatureMetricPlugin key : DifferenceMatrixPlugin.getMetricPlugins().keySet()){
						TrainingResult a = DifferenceMatrixPlugin.isCalculateBaseline()?baseline:competing;
						TrainingResult b = DifferenceMatrixPlugin.isCalculateCompete()?baseline:competing;
						
						Collection<Integer> uniques = DifferenceMatrixPlugin.uniqueInstances(baseline, b, pred, act);
						boolean[] mask = new boolean[a.getEvaluationTable().getDocumentList().getSize()];
						for(int i = 0; i < mask.length; i++){ mask[i] = false; }
						if(uniques != null){
							for(Integer i : uniques){
								mask[i] = true;
							}
						}else{
							for(int i = 0; i < mask.length; i++){
								mask[i] = true;
							}
						}
						Map<Feature, Double> values = key.evaluateFeatures(CompareModelsControl.getBaselineTrainedModelRecipe(), mask, setting, "", CompareModelsControl.getUpdater());
						if(values != null && values.containsKey(DifferenceMatrixPlugin.getHighlightedFeature())){
							Double val = values.get(DifferenceMatrixPlugin.getHighlightedFeature());
							row.add(val);
							maxVal = Math.max(val, maxVal);							
						}else{
							row.add("");
						}
					}
					index++;
				}
				rows.add(row);
			}
		}
		return rows;
	}
	
	@Override
	public void refreshPanel() {
		if(CompareModelsControl.hasBaselineTrainedModelRecipe() && CompareModelsControl.hasCompetingTrainedModelRecipe()){
			refreshPanel(CompareModelsControl.getBaselineTrainedModelRecipe().getTrainingResult().getConfusionMatrix());
		}
		
	}

}
