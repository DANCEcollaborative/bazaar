package edu.cmu.side.view.restructure;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.plugin.RestructurePlugin;
import edu.cmu.side.view.generic.GenericLoadPanel;
import edu.cmu.side.view.generic.GenericPluginChecklistPanel;
import edu.cmu.side.view.generic.GenericPluginConfigPanel;
import edu.cmu.side.view.generic.GenericTripleFrame;
import edu.cmu.side.view.util.Refreshable;

public class RestructureFeaturesPane extends JPanel{

	private static GenericTripleFrame top;
	private static RestructureActionPanel action = new RestructureActionPanel(RestructureTablesControl.getUpdater());
	private static RestructureBottomPanel bottom = new RestructureBottomPanel(action);

	public RestructureFeaturesPane(){
		
		RestructureTablesControl.setActionBar(action);

		setLayout(new BorderLayout());
		
		GenericPluginChecklistPanel<RestructurePlugin> checklist = new GenericPluginChecklistPanel<RestructurePlugin>("Filters Available:"){
			@Override
			public Map<RestructurePlugin, Boolean> getPlugins() {
				return RestructureTablesControl.getFilterPlugins();
			}
		};
		
		final GenericPluginConfigPanel<RestructurePlugin> config = new GenericPluginConfigPanel<RestructurePlugin>(){
			@Override
			public void refreshPanel() {
				refreshPanel(RestructureTablesControl.getFilterPlugins());
			}
		};
		
		GenericLoadPanel load = new GenericLoadPanel("Feature Tables:"){

			@Override
			public void setHighlight(Recipe r) {
				RestructureTablesControl.setHighlightedFeatureTableRecipe(r);
				Workbench.update(this);
			}

			@Override
			public Recipe getHighlight() {
				return RestructureTablesControl.getHighlightedFeatureTableRecipe();
			}

			@Override
			public void refreshPanel() {
				refreshPanel(GenesisControl.getTrainingTables());
				Map<RestructurePlugin, Boolean> plugins = RestructureTablesControl.getFilterPlugins();
				for(RestructurePlugin plug : plugins.keySet())
				{
					if(plugins.get(plug))
					{
						Component plugUI = plug.getConfigurationUI();
						if(plugUI instanceof Refreshable)
						{
							((Refreshable)plugUI).refreshPanel();
						}
					}
				}
			}

			@Override
			public Stage getLoadableStage()
			{
				return Stage.FEATURE_TABLE;
			}

			@Override
			public void deleteHighlight()
			{
				RestructureTablesControl.setHighlightedFeatureTableRecipe(null);
			}
			
		};
		
		
		top = new GenericTripleFrame(load, checklist, config);
		JSplitPane pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.CENTER, top);
		panel.add(BorderLayout.SOUTH, action);
		pane.setTopComponent(panel);
		pane.setBottomComponent(bottom);
		//top.setPreferredSize(new Dimension(950,500));
		//bottom.setPreferredSize(new Dimension(950,200));
		pane.setDividerLocation(450);
		add(BorderLayout.CENTER, pane);
		

		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, load);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, checklist);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, config);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, action);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, load);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, checklist);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, config);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, action);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, action);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, bottom);
		GenesisControl.addListenerToMap(load, checklist);
		GenesisControl.addListenerToMap(load, config);
		GenesisControl.addListenerToMap(checklist, config);
		GenesisControl.addListenerToMap(checklist, action);
		
	}
}
