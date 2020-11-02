package edu.cmu.side.view.build;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.plugin.LearningPlugin;
import edu.cmu.side.plugin.WrapperPlugin;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.generic.GenericPluginConfigPanel;
import edu.cmu.side.view.generic.GenericTripleFrame;
import edu.cmu.side.view.util.Refreshable;

public class BuildModelPane extends JPanel{

	private static GenericTripleFrame top;
	private static BuildActionPanel action = new BuildActionPanel(BuildModelControl.getUpdater());
	private static BuildBottomPanel bottom = new BuildBottomPanel();

	private static GenericPluginConfigPanel<LearningPlugin> config = new GenericPluginConfigPanel<LearningPlugin>(){

		@Override
		public void refreshPanel() {
			refreshPanel(BuildModelControl.getLearningPlugins());
		}
		
	};
	public BuildModelPane(){
		setLayout(new BorderLayout());
		GenericLoadPanel load = new GenericLoadPanel("Feature Tables:") {
			@Override
			public void setHighlight(Recipe r) {
				BuildModelControl.setHighlightedFeatureTableRecipe(r);
				Workbench.update(this);
			}
			
			@Override
			public Recipe getHighlight() {
				return BuildModelControl.getHighlightedFeatureTableRecipe();
			}

			@Override
			public void refreshPanel() {
				Collection<Recipe> recipes = new ArrayList<Recipe>();
				recipes.addAll(GenesisControl.getFilterTables());
				recipes.addAll(GenesisControl.getFeatureTables());
				refreshPanel(recipes);
			}

			@Override
			public Stage getLoadableStage()
			{
				return Stage.FEATURE_TABLE;
			}

			@Override
			public void deleteHighlight()
			{
				BuildModelControl.setHighlightedFeatureTableRecipe(null);
			}
		};
		
		BuildPluginPanel checklist = new BuildPluginPanel(action.new NameListener());
		top = new GenericTripleFrame(load, checklist, config);
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		top.setSmallSplitPosition(400);
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER, top);
		panel.add(BorderLayout.SOUTH, action);
		pane.setTopComponent(panel);
		pane.setBottomComponent(bottom);
		pane.setDividerLocation(520);
		add(BorderLayout.CENTER, pane);

		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, load);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, load);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, checklist);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, checklist);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, config);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, config);

		GenesisControl.addListenerToMap(load, checklist);
		GenesisControl.addListenerToMap(load, action);
		GenesisControl.addListenerToMap(load, config);
		GenesisControl.addListenerToMap(checklist, config);
		GenesisControl.addListenerToMap(checklist, action);
		GenesisControl.addListenerToMap(config, action);
		
		for(WrapperPlugin plug : BuildModelControl.getWrapperPlugins().keySet())
		{
			if(plug instanceof Refreshable)
			{
				GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, (Refreshable)plug);
				GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, (Refreshable)plug);
				GenesisControl.addListenerToMap(load, (Refreshable)plug);
				GenesisControl.addListenerToMap(checklist, (Refreshable)plug);
			}
		}
		
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, action);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, action);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, action);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, bottom);
		
	}
}
