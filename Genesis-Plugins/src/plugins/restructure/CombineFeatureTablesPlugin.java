package plugins.restructure;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.RestructurePlugin;
import edu.cmu.side.view.generic.GenericLoadPanel;

public class CombineFeatureTablesPlugin extends RestructurePlugin
{
	
	JPanel configPanel;
	Recipe comboRecipe = null;
	GenericLoadPanel comboChooser = null;


	public CombineFeatureTablesPlugin()
	{
		makeConfigUI();
	}

	public void makeConfigUI()
	{
		configPanel = new JPanel(new BorderLayout());
		configPanel.add(new JLabel("whee!"));

		comboChooser = new GenericLoadPanel("Combine with...")
		{
			
			@Override
			public void setHighlight(Recipe r)
			{
				comboRecipe = r;
			}
			@Override
			public void deleteHighlight()
			{
				comboRecipe = null;
			}
			
			@Override
			public void refreshPanel()
			{
				super.refreshPanel(Workbench.getRecipesByPane(Stage.FEATURE_TABLE, Stage.MODIFIED_TABLE));
			}
			
			@Override
			public Stage getLoadableStage()
			{
				return Stage.FEATURE_TABLE;
			}
			
			@Override
			public Recipe getHighlight()
			{
				return comboRecipe;
			}
		};
		GenesisControl.addListenerToMap(Stage.FEATURE_TABLE, comboChooser);
		GenesisControl.addListenerToMap(Stage.MODIFIED_TABLE, comboChooser);
		comboChooser.refreshPanel();
		
		configPanel.add(comboChooser, BorderLayout.CENTER);
		configPanel.add(new JLabel("Make sure the documents and annotations match!"), BorderLayout.SOUTH);
	}

	@Override
	protected FeatureTable restructureWithMaskForSubclass(FeatureTable original, boolean[] mask, int threshold, StatusUpdater progressIndicator)
	{
		FeatureTable comboSource = comboRecipe.getTrainingTable();
		
		FeatureTable combination = original.clone();
		combination.setThreshold(threshold);
		for(Feature f : comboSource.getFeatureSet())
		{
			progressIndicator.update("updating feature "+f.getFeatureName());
			combination.addFeatureHits(comboSource.getHitsForFeature(f));
		}
		return combination;
	}

	@Override
	protected FeatureTable restructureTestSetForSubclass(FeatureTable original, FeatureTable test, int threshold, StatusUpdater progressIndicator)
	{
		throw new UnsupportedOperationException("Can't use CombineFeatureTables on test set - only Cross-Validation, for now.");
	}

	@Override
	public String getOutputName()
	{
		// TODO Auto-generated method stub
		return "combine_features";
	}
	
	@Override
	protected Component getConfigurationUIForSubclass()
	{
		
		if(configPanel == null)
			makeConfigUI();
		
		return configPanel;
		
			
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		// TODO Auto-generated method stub
		return new HashMap<String, String>();
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		// TODO Auto-generated method stub

	}
	
	@Override 
	public String toString()
	{
		return "Combine Feature Tables";
	}

}
