package edu.cmu.side.view.build;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.view.generic.GenericMatrixPanel;

public class BuildMatrixPanel extends GenericMatrixPanel{

	@Override
	public void refreshPanel() {
		if(BuildModelControl.hasHighlightedTrainedModelRecipe()){
			refreshPanel(BuildModelControl.getHighlightedTrainedModelRecipe().getTrainingResult().getConfusionMatrix());				
		}else{
			refreshPanel(new TreeMap<String, Map<String, List<Integer>>>());
		}
	}

}
