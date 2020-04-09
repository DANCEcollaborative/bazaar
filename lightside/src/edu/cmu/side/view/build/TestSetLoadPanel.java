package edu.cmu.side.view.build;

import java.awt.Dimension;
import java.util.Collections;
import java.util.Set;

import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.view.generic.GenericLoadCSVPanel;

public class TestSetLoadPanel extends GenericLoadCSVPanel
{

	public TestSetLoadPanel(String s)
	{
		super(s);
		load.setToolTipText("Load Test Set (CSV)");

//		Dimension v = combo.getPreferredSize();
//		v.width = 100;
//		combo.setPreferredSize(v);
		describePanel.setMinimumSize(new Dimension(120, 120));
//		describePanel.setPreferredSize(new Dimension(150, 150));

		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, this);
		GenesisControl.addListenerToMap(RecipeManager.Stage.FEATURE_TABLE, this);
		GenesisControl.addListenerToMap(RecipeManager.Stage.MODIFIED_TABLE, this);
	
		revalidate();

	}

	@Override
	public void setHighlight(Recipe r)
	{
		if(r != null)
		{
			BuildModelControl.updateValidationSetting("testRecipe", r);
			BuildModelControl.updateValidationSetting("testSet", r.getDocumentList());
		}
		else
		{
			BuildModelControl.updateValidationSetting("testRecipe", null);
			BuildModelControl.updateValidationSetting("testSet", null);
		}
		verifyTestSet();
	}

	@Override
	public Recipe getHighlight()
	{
		return (Recipe) BuildModelControl.getValidationSettings().get("testRecipe");
	}

	@Override
	public void refreshPanel()
	{
		refreshPanel(GenesisControl.getDocumentLists());
		
		revalidate();
	}

	/**
	 * 
	 */
	public boolean verifyTestSet()
	{
		Recipe trainRecipe = BuildModelControl.getHighlightedFeatureTableRecipe();
		DocumentList testList= (DocumentList) BuildModelControl.getValidationSettings().get("testSet");
		
		if(trainRecipe != null && testList != null)
		{
			DocumentList trainList = trainRecipe.getDocumentList();
			if(!Collections.disjoint(trainList.getFilenames(), testList.getFilenames()))
			{
				setWarning("Test set overlaps with training set.");
				return false;
			}
			else
			{
				String annotation = trainRecipe.getTrainingTable().getAnnotation();
				
				Set<String> trainText = trainList.getTextColumns();
				Set<String> testAnnotations = testList.allAnnotations().keySet();
				Set<String> testText = testList.getTextColumns();
				if(!testAnnotations.containsAll(trainText) && !testText.containsAll(trainText))
				{
					setWarning("This test set does not have the same text columns '"+trainText+ "' as your training data.");
					return false;
				}
				else if(!testAnnotations.contains(annotation) && ! testText.contains(annotation))
				{
					setWarning("This test set does not have a column for your class annotation, '"+annotation+ "'.");
					return false;
				}
				else
				{
					testList.setCurrentAnnotation(annotation, trainRecipe.getTrainingTable().getClassValueType());
					Set<String> trainLabels = trainRecipe.getDocumentList().getPossibleAnn(annotation);
					Set<String> testLabels = testList.getPossibleAnn(annotation);
					if(trainRecipe.getClassValueType() == Type.NOMINAL && !trainLabels.equals(testLabels))
					{
						setWarning("<html>Class labels in train and test data do not match:<br>Train="+trainLabels+"<br>Test="+testLabels+"</html>");
						return true;
					}
					else
					{
						clearWarning();
						return true;
					}
				}
			}
		}
		else
		{
			clearWarning();
			return false;
		}
		
	}

	@Override
	public void loadNewItem()
	{
		loadNewDocumentsFromCSV();
	}

	@Override
	public void deleteHighlight()
	{
		BuildModelControl.updateValidationSetting("testRecipe", null);
		BuildModelControl.updateValidationSetting("testSet", null);
	}
	
}
