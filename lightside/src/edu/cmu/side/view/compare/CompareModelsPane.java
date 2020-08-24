package edu.cmu.side.view.compare;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.util.AbstractListPanel;

public class CompareModelsPane extends AbstractListPanel{

	GenericLoadPanel loadBaseline = new GenericLoadPanel("Baseline Model:"){

		@Override
		public void setHighlight(Recipe r) {
			CompareModelsControl.setBaselineTrainedModelRecipe(r);
			Workbench.update(this);
			verifyModels();
		}

		@Override
		public Recipe getHighlight() {
			return CompareModelsControl.getBaselineTrainedModelRecipe();
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
			CompareModelsControl.setBaselineTrainedModelRecipe(null);
		}

	};
	
	GenericLoadPanel loadCompetitor = new GenericLoadPanel("Competing Model:"){

		@Override
		public void setHighlight(Recipe r) {
			CompareModelsControl.setCompetingTrainedModelRecipe(r);
			Workbench.update(this);
			verifyModels();
		}

		@Override
		public Recipe getHighlight() {
			return CompareModelsControl.getCompetingTrainedModelRecipe();
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
			CompareModelsControl.setCompetingTrainedModelRecipe(null);
		}
	};
	
	JPanel middle = new JPanel(new BorderLayout());
	CompareActionBar dropdown = new CompareActionBar(CompareModelsControl.getUpdater());

	public CompareModelsPane(){
		setLayout(new BorderLayout());
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		JPanel grid = new JPanel(new GridLayout(1,2));
		JPanel top = new JPanel(new BorderLayout());
		grid.add(loadBaseline);
		grid.add(loadCompetitor);
		top.add(BorderLayout.CENTER, grid);
		top.add(BorderLayout.SOUTH, dropdown);
		Workbench.reloadComboBoxContent(combo, CompareModelsControl.getModelComparisonPlugins().keySet(), null);
		if(combo.getItemCount() > 0)
		{
			combo.setSelectedIndex(0);
		}
		JScrollPane scroll = new JScrollPane(middle);
		grid.setPreferredSize(new Dimension(950,200));
		//top.setPreferredSize(new Dimension(950,250));
		//scroll.setPreferredSize(new Dimension(950,400));
		split.setTopComponent(top);
		split.setBottomComponent(scroll);
		split.setDividerLocation(250);
		add(BorderLayout.CENTER, split);

		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, loadBaseline);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, loadCompetitor);
		GenesisControl.addListenerToMap(loadBaseline, this);
		GenesisControl.addListenerToMap(loadCompetitor, this);
		GenesisControl.addListenerToMap(dropdown, this);

	}
	
	@Override
	public void refreshPanel(){
		dropdown.refreshPanel();
		if(CompareModelsControl.getHighlightedModelComparisonPlugin() != null){
			middle.removeAll();
			middle.add(BorderLayout.CENTER, CompareModelsControl.getHighlightedModelComparisonPlugin().getConfigurationUI());
			CompareModelsControl.getHighlightedModelComparisonPlugin().refreshPanel();
			middle.revalidate();
			middle.repaint();
		}
		verifyModels();
	}
	
	protected void verifyModels()
	{
		Recipe baseline = CompareModelsControl.getBaselineTrainedModelRecipe();
		Recipe competitor = CompareModelsControl.getCompetingTrainedModelRecipe();
		
		if(baseline != null && competitor != null)
		{
			DocumentList baseList = baseline.getDocumentList();
			DocumentList competeList = competitor.getDocumentList();
			if(baseline.equals(competitor))
			{
				loadCompetitor.setWarning("You're comparing a model to itself.");
			}
			else if(!baseline.getTrainingTable().getAnnotations().equals(competitor.getTrainingTable().getAnnotations()))
			{
				loadCompetitor.setWarning("Class labels are not directly comparable.");
			}
			else
			{
				loadCompetitor.clearWarning();
			}
		}
		else
		{
			loadCompetitor.clearWarning();
		}
	}
}
