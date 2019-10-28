package edu.cmu.side.plugin;


public abstract class EvaluateTwoModelPlugin extends SIDEPlugin{

	@Override
	public String getType() {
		return "model_comparison";
	}

	
	/**
	 * @return A short prefix string for the plugin name.
	 */
	@Override
	public abstract String getOutputName();

	public abstract void refreshPanel();
}
