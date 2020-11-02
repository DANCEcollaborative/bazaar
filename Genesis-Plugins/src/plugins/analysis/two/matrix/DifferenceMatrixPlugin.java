package plugins.analysis.two.matrix;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import plugins.metrics.features.models.ErrorAnalysisFeatureMetrics;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.EvaluateTwoModelPlugin;
import edu.cmu.side.plugin.ModelFeatureMetricPlugin;
import edu.cmu.side.view.util.SIDETable;

public class DifferenceMatrixPlugin extends EvaluateTwoModelPlugin{


	protected static SIDETable baseTable;
	protected static SIDETable compTable;

	protected static String highlightedRow;
	protected static String highlightedColumn;
	protected static Feature highlightedFeature;
	
	protected static Collection<Integer> uniqueBaseline;
	protected static Collection<Integer> uniqueCompete;

	static boolean calcBaseline = false;
	static boolean calcCompete = false;
	
	static ItemListener differenceMatrixListener = new ItemListener(){

		@Override
		public void itemStateChanged(ItemEvent e) {

		}
	};

	static Map<ModelFeatureMetricPlugin, Map<String, Boolean>> metricPlugins = new HashMap<ModelFeatureMetricPlugin, Map<String, Boolean>>();
	static ModelFeatureMetricPlugin evaluator;
	
	public static void setCalculateOption(String s){
		if("baseline".equals(s)){
			calcBaseline = true;
			calcCompete = false;
		}else if("compete".equals(s)){
			calcBaseline = false;
			calcCompete = true;
		}
	}
	
	public static ModelFeatureMetricPlugin getEvaluator(){
		return evaluator;
	}
	
	public static boolean isCalculateBaseline(){
		return calcBaseline;
	}
	
	public static boolean isCalculateCompete(){
		return calcCompete;
	}

	public static void setHighlightedCell(String row, String col){
		highlightedRow = row;
		highlightedColumn = col;
	}
	
	public static String getHighlightedRow(){
		return highlightedRow;
	}
	public static String getHighlightedColumn(){
		return highlightedColumn;
	}
	
	public static Feature getHighlightedFeature(){
		return highlightedFeature;
	}
	
	public static void setHighlightedFeature(Feature f){
		highlightedFeature = f;
	}
	
	public static Map<ModelFeatureMetricPlugin, Map<String, Boolean>> getMetricPlugins(){
		return metricPlugins;
	}
	
	public static ItemListener getCheckboxListener(){
		return differenceMatrixListener;
	}
	public static void setBaseTable(SIDETable table){
		baseTable = table;
	}

	public static void setCompTable(SIDETable table){
		compTable = table;
	}

	public static SIDETable getBaseTable(){
		return baseTable;
	}

	public static SIDETable getCompTable(){
		return compTable;
	}

	public static Collection<Integer> uniqueInstances(TrainingResult a, TrainingResult b, String pred, String act){
		Collection<Integer> uniques = new TreeSet<Integer>();
		if(a.getConfusionMatrix().containsKey(pred) && a.getConfusionMatrix().get(pred).containsKey(act)){
			for(Integer i : a.getConfusionMatrix().get(pred).get(act)){
				if(!b.getConfusionMatrix().containsKey(pred) || !b.getConfusionMatrix().get(pred).containsKey(act) || !b.getConfusionMatrix().get(pred).get(act).contains(i)){
					uniques.add(i);
				}
			}			
		}
		return uniques;
	}

	JSplitPane split = new JSplitPane();
	MatricesDisplayPanel matrices = new MatricesDisplayPanel();
	MatrixExplorePanel explore = new MatrixExplorePanel();
	public DifferenceMatrixPlugin(){
		Map<String, Boolean> freqMetrics = new HashMap<String, Boolean>();

		ErrorAnalysisFeatureMetrics limitedMetrics = new ErrorAnalysisFeatureMetrics(){
			@Override
			public Map<Feature, Double> evaluateModelFeatures(Recipe model, boolean[] mask, String eval, String pred, String act, StatusUpdater update){
				List<Integer> highlight = new ArrayList<Integer>();
				for(int i = 0; i < mask.length; i++){
					if(mask[i]) highlight.add(i);
				}
				initiateMap(model.getTrainingResult(), true);
//				System.out.println("DMP146 " + eval + ", " + pred + ", " + act + ", " + model);
				System.out.println(evaluations.get(model).keySet().size());
				System.out.println(evaluations.get(model).get(pred).keySet().size());
				System.out.println(evaluations.get(model).get(pred).get(act).keySet().size());
				System.out.println(evaluations.get(model).get(pred).get(act).get(eval).keySet().size());
				return evaluations.get(model).get(pred).get(act).get(eval);
			}
			

			@Override
			public String getHighlightedRow() {
				return DifferenceMatrixPlugin.getHighlightedRow();
			}

			@Override
			public String getHighlightedColumn() {
				return DifferenceMatrixPlugin.getHighlightedColumn();
			}

		};
		evaluator = limitedMetrics;
		freqMetrics.put(ErrorAnalysisFeatureMetrics.avg, true);
		freqMetrics.put(ErrorAnalysisFeatureMetrics.freq, true);
		metricPlugins.put(limitedMetrics, freqMetrics);

//		matrices.setPreferredSize(new Dimension(0, 0));
		explore.setPreferredSize(new Dimension(0, 0));
		
		JScrollPane matrixScroller = new JScrollPane(matrices);
		matrixScroller.setPreferredSize(new Dimension(0, 0));
		
		split.setLeftComponent(matrixScroller);
		split.setRightComponent(explore);
		split.setDividerLocation(350);
		
		GenesisControl.addListenerToMap(matrices, explore.getInstancePanel());
		
	};

	@Override
	public String getOutputName() {
		return "diffMatrix";
	}

	@Override
	public String toString(){
		return "Difference Matrix";
	}

	@Override
	public void refreshPanel() {
		Recipe baselineRec = CompareModelsControl.getBaselineTrainedModelRecipe();
		Recipe competeRec = CompareModelsControl.getCompetingTrainedModelRecipe();
		if(baselineRec != null && competeRec != null){
			TrainingResult base = baselineRec.getTrainingResult();
			TrainingResult comp = competeRec.getTrainingResult();
			if(highlightedRow != null && highlightedColumn != null){
				uniqueBaseline = uniqueInstances(base, comp, highlightedColumn, highlightedRow);				
				uniqueCompete = uniqueInstances(comp, base, highlightedColumn, highlightedRow);				
			}
		}
		matrices.refreshPanel();
		explore.refreshPanel();
	}

	@Override
	protected Component getConfigurationUIForSubclass() {
		Recipe baselineRec = CompareModelsControl.getBaselineTrainedModelRecipe();
		Recipe competeRec = CompareModelsControl.getCompetingTrainedModelRecipe();
		if(baselineRec != null && competeRec != null){
			
		}
		return split;
	}

	@Override
	public Map<String, String> generateConfigurationSettings() {
		Map<String, String> settings = new TreeMap<String, String>();
		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) {
		// TODO Auto-generated method stub

	}

}
