package edu.cmu.side.view.explore;

import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.JLabel;

import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.plugin.ModelFeatureMetricPlugin;
import edu.cmu.side.view.generic.GenericMetricChecklistPanel;

public class ExploreMetricChecklistPanel extends GenericMetricChecklistPanel<ModelFeatureMetricPlugin>{

		public ExploreMetricChecklistPanel(){
			super();
			this.removeAll();
			add("left", new JLabel("Evaluations to Display:"));
			add("br hfill vfill", describeScroll);
		}
		
		@Override
		public ItemListener getCheckboxListener() {
			return ExploreResultsControl.getCheckboxListener(this);
		}

		@Override
		public Map<ModelFeatureMetricPlugin, Map<String, Boolean>> getEvaluationPlugins() {
			return ExploreResultsControl.getFeatureEvaluationPlugins();
		}

		@Override
		public void setTargetAnnotation(String s) {}
		
		@Override
	public void refreshPanel()
	{
		if (ExploreResultsControl.hasHighlightedTrainedModelRecipe())
		{
			Recipe highlight = ExploreResultsControl.getHighlightedTrainedModelRecipe();
			refreshPanel(highlight);
		}
		else
		{
//			refreshPanel(null);
		}
	}
	
	
}
