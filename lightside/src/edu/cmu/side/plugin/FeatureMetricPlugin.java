package edu.cmu.side.plugin;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.feature.Feature;

public abstract class FeatureMetricPlugin<E extends Comparable<E>> extends SIDEPlugin{

	ArrayList<String> evaluationNames = new ArrayList<String>();
	
	public FeatureMetricPlugin() {
		super();
	}

	@Override
	protected Component getConfigurationUIForSubclass() {
		return null;
	}
	
	public abstract Map<String, Collection<Feature.Type>> getAvailableEvaluations();
		
	/**
	 * @return A short prefix string for the plugin name.
	 */
	@Override
	public abstract String getOutputName();

	public abstract Map<Feature, E> evaluateFeatures(Recipe recipe, boolean[] mask, String eval, String target, StatusUpdater update);
	
	public abstract boolean canEvaluateRecipe(Recipe r, String evaluationKey);
}
