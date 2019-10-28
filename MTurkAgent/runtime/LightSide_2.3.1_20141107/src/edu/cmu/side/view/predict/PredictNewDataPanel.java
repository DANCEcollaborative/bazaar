package edu.cmu.side.view.predict;

import java.util.Collections;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.control.PredictLabelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.view.generic.GenericLoadCSVPanel;

public class PredictNewDataPanel extends GenericLoadCSVPanel
{
//
//	SelectPluginList textColumnsList = new SelectPluginList();
//	JScrollPane textColumnsScroll = new JScrollPane(textColumnsList);

	public PredictNewDataPanel()
	{
		super("New Data:", true, true, false, true);
		GenesisControl.addListenerToMap(RecipeManager.Stage.TRAINED_MODEL, this);
	}

	@Override
	public void setHighlight(Recipe r)
	{
		//if(this.isEnabled())
		{
			PredictLabelsControl.setHighlightedUnlabeledData(r);
			Workbench.update(this);
			verifyNewData();
		}
	}

	@Override
	public Recipe getHighlight()
	{
		return PredictLabelsControl.getHighlightedUnlabeledData();
	}

	@Override
	public void refreshPanel()
	{
		refreshPanel(Workbench.getRecipesByPane(Stage.DOCUMENT_LIST));
		setEnabled(isEnabled());
	}

	@Override
	public void loadNewItem()
	{
		loadNewDocumentsFromCSV();
	}
	
	protected void verifyNewData()
	{
		Recipe trainRecipe = PredictLabelsControl.getHighlightedTrainedModelRecipe();
		Recipe unlabeledRecipe = PredictLabelsControl.getHighlightedUnlabeledData();
		
		if(trainRecipe != null && unlabeledRecipe != null)
		{
			DocumentList trainList = trainRecipe.getDocumentList();
			DocumentList labelList = unlabeledRecipe.getDocumentList();
			if(!Collections.disjoint(trainList.getFilenames(), labelList.getFilenames()))
			{
				setWarning("Unlabeled data set overlaps with training set");
			}
			else
			{
				clearWarning();
			}
		}
		else
		{
			clearWarning();
		}
	}

	@Override
	public void deleteHighlight()
	{
		PredictLabelsControl.setHighlightedUnlabeledData(null);
	}

}
