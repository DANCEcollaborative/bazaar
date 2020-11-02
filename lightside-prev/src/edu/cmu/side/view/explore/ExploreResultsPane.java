package edu.cmu.side.view.explore;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.plugin.EvaluateOneModelPlugin;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.generic.GenericPluginConfigPanel;
import edu.cmu.side.view.generic.GenericTripleFrame;
import edu.cmu.side.view.util.AbstractListPanel;

public class ExploreResultsPane extends JPanel{

	GenericLoadPanel load = new GenericLoadPanel("Highlight:"){

		@Override
		public void setHighlight(Recipe r) {
			ExploreResultsControl.setHighlightedTrainedModelRecipe(r);
			Workbench.update(this);
		}

		@Override
		public Recipe getHighlight() {
			return ExploreResultsControl.getHighlightedTrainedModelRecipe();
		}

		@Override
		public void refreshPanel() {
			if(!ExploreResultsControl.hasHighlightedTrainedModelRecipe())
			{
				ExploreResultsControl.setHighlightedCell(null, null);
			}
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
			ExploreResultsControl.setHighlightedTrainedModelRecipe(null);
		}
		
	};
	
	ExploreMatrixPanel matrix = new ExploreMatrixPanel();
	ExploreMetricChecklistPanel checklist = new ExploreMetricChecklistPanel();
	ExploreActionBar middle = new ExploreActionBar(ExploreResultsControl.getUpdater());
	ExploreFeatureMetricPanel features = new ExploreFeatureMetricPanel(middle);
	GenericTripleFrame triple;

	GenericPluginConfigPanel<EvaluateOneModelPlugin> analysis = new GenericPluginConfigPanel<EvaluateOneModelPlugin>(false){
		@Override
		public void refreshPanel(){
			refreshPanel(ExploreResultsControl.getModelAnalysisPlugins());
		}
	};
	
	public ExploreResultsPane(){
		setLayout(new BorderLayout());
		JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		matrix.setPreferredSize(new Dimension(325, 150));
		checklist.setPreferredSize(new Dimension(325, 150));
		features.setPreferredSize(new Dimension(325, 250));

		AbstractListPanel panel = new AbstractListPanel();
		panel.setPreferredSize(new Dimension(325,325));
		panel.removeAll();
		panel.setLayout(new BorderLayout());
		panel.add(BorderLayout.CENTER, matrix);
		
		
		panel.add(BorderLayout.SOUTH, checklist);
		triple = new GenericTripleFrame(load, panel, features);
		
		JScrollPane scroll = new JScrollPane(analysis);
		JPanel top = new JPanel(new BorderLayout());
		

//		triple.setPreferredSize(new Dimension(950,350));
		//top.setPreferredSize(new Dimension(950,400));
		

		top.add(BorderLayout.CENTER, triple);
		top.add(BorderLayout.SOUTH, middle);

		left.setTopComponent(top);
		left.setBottomComponent(scroll);
		
		left.setDividerLocation(400);
		
		add(BorderLayout.CENTER, left);


		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, load);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, matrix);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, checklist);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, features);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, analysis);

		GenesisControl.addListenerToMap(load, matrix);
		GenesisControl.addListenerToMap(load, checklist);
		GenesisControl.addListenerToMap(load, features);
		GenesisControl.addListenerToMap(load, analysis);
		GenesisControl.addListenerToMap(checklist, features);
		GenesisControl.addListenerToMap(matrix, features);
		
		GenesisControl.addListenerToMap(checklist, analysis);
		GenesisControl.addListenerToMap(matrix, analysis);
		GenesisControl.addListenerToMap(features, analysis);
		GenesisControl.addListenerToMap(middle, analysis);
		
		ExploreResultsControl.setActionBar(middle);

	}
	
}
