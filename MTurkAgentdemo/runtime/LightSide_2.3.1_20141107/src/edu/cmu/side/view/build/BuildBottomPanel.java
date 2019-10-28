package edu.cmu.side.view.build;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.generic.GenericMatrixPanel;
import edu.cmu.side.view.generic.GenericModelMetricPanel;
import edu.cmu.side.view.util.AbstractListPanel;

public class BuildBottomPanel extends AbstractListPanel {

	private GenericLoadPanel control = new GenericLoadPanel("Trained Models:"){

		@Override
		public void setHighlight(Recipe r) {
			BuildModelControl.setHighlightedTrainedModelRecipe(r);
			Workbench.update(this);
		}

		@Override
		public Recipe getHighlight() {
			return BuildModelControl.getHighlightedTrainedModelRecipe();
		}

		@Override
		public void refreshPanel() {
			refreshPanel(GenesisControl.getTrainedModels());
		}

		@Override
		public Stage getLoadableStage()
		{
			return Stage.TRAINED_MODEL;
		}

		@Override
		public void deleteHighlight()
		{
			BuildModelControl.setHighlightedTrainedModelRecipe(null);
		}
	};

	private GenericMatrixPanel confusion = new GenericMatrixPanel(){

		@Override
		public void refreshPanel() {
			if(BuildModelControl.hasHighlightedTrainedModelRecipe() && 
					BuildModelControl.getHighlightedTrainedModelRecipe().getTrainingResult() != null)
			{
				refreshPanel(BuildModelControl.getHighlightedTrainedModelRecipe().getTrainingResult().getConfusionMatrix());				
			}else
			{
				refreshPanel(new TreeMap<String, Map<String, List<Integer>>>());
			}
		}

	};

	private GenericModelMetricPanel result = new GenericModelMetricPanel(){
		@Override
		public void refreshPanel(){
			refreshPanel(BuildModelControl.getHighlightedTrainedModelRecipe());
		}
	};

	public BuildBottomPanel(){
		setLayout(new BorderLayout());
		JSplitPane pane = new JSplitPane();
		pane.setLeftComponent(control);

		pane.setBorder(BorderFactory.createEmptyBorder());
		JSplitPane right = new JSplitPane();
		right.setLeftComponent(result);
		right.setRightComponent(confusion);
		right.setPreferredSize(new Dimension(650,200));
		right.setBorder(BorderFactory.createEmptyBorder());
		pane.setRightComponent(right);
		control.setPreferredSize(new Dimension(275,200));		
		confusion.setPreferredSize(new Dimension(275,200));
		result.setPreferredSize(new Dimension(350, 200));
		
		Dimension minimumSize = new Dimension(50, 50);
		control.setMinimumSize(minimumSize);
		confusion.setMinimumSize(minimumSize);
		result.setMinimumSize(minimumSize);
		
		add(BorderLayout.CENTER, pane);
		
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, control);
		GenesisControl.addListenerToMap(control, confusion);
		GenesisControl.addListenerToMap(control, result);
		
	}

	@Override
	public void refreshPanel(){
		control.refreshPanel();
	}
}
