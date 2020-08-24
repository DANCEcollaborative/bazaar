package edu.cmu.side.plugin;

import java.util.Map;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;

public abstract class TableFeatureMetricPlugin<E extends Comparable<E>> extends FeatureMetricPlugin{


	
	public static String type = "table_feature_evaluation";
	
	@Override
	public String getType() {
		return type;	
	}
	
	protected abstract Map<Feature, E> evaluateTableFeatures(FeatureTable model, boolean[] mask, String eval, String target, StatusUpdater update);

	@Override
	public Map<Feature, E> evaluateFeatures(Recipe recipe, boolean[] mask, String eval, String target, StatusUpdater update){
		return evaluateTableFeatures(recipe.getTrainingTable(), mask, eval, target, update);
	}

}
