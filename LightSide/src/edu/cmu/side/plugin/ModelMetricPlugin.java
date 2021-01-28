package edu.cmu.side.plugin;

import java.util.Map;

import edu.cmu.side.model.data.TrainingResult;

public abstract class ModelMetricPlugin extends SIDEPlugin{

	public ModelMetricPlugin() {
		super();
	}
	
	public static String type = "model_evaluation";
	
	@Override
	public String getType() {
		return type;
	}
		
	/**
	 * @return A short prefix string for the plugin name.
	 */
	@Override
	public abstract String getOutputName();

	/**
	 * 
	 */
	public abstract Map<String, String> evaluateModel(TrainingResult model, Map<String, String> settings);
}
