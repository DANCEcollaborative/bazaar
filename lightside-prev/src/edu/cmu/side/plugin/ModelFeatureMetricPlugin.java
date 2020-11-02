package edu.cmu.side.plugin;

import java.util.Map;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.feature.Feature;

public abstract class ModelFeatureMetricPlugin<E extends Comparable<E>> extends FeatureMetricPlugin{

	
	public static String type = "model_feature_evaluation";
	
	@Override
	public String getType() {
		return type;	
	}

	public Map<Feature, E> evaluateModelFeatures(Recipe traningResultsRecipe, boolean[] mask, String eval, StatusUpdater update) {
		String act = getHighlightedRow();
		String pred = getHighlightedColumn();
		return evaluateModelFeatures(traningResultsRecipe, mask, eval, pred, act, update);
	}
	
	public abstract String getHighlightedRow();
	
	public abstract String getHighlightedColumn();
	
	@Override
	public Map<Feature, E> evaluateFeatures(Recipe recipe, boolean[] mask, String eval, String target, StatusUpdater update){
		return evaluateModelFeatures(recipe, mask, eval, update);
	}

	public abstract Map<Feature, E> evaluateModelFeatures(Recipe recipe, boolean[] mask, String eval, String pred, String act, StatusUpdater update);

	public abstract E targetedFeatureEvaluation(Recipe recipe, boolean[] mask, String eval, String pred, String act, Feature f, StatusUpdater update);
}
