package edu.cmu.side.view.predict;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.PredictLabelsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.util.WarningButton;

public class PredictActionBar extends ActionBar
{
	JCheckBox showMaxScoreBox = new JCheckBox("Show Predicted Label's Score");
	JCheckBox showDistsBox = new JCheckBox("Show Label Distribution");
	JCheckBox overwriteBox = new JCheckBox("Overwrite Columns");
	WarningButton warn = new WarningButton();

	Recipe lastRecipe = null;
	boolean lastValidation = false;

	public PredictActionBar(StatusUpdater update)
	{
		super("prediction",Stage.PREDICTION_RESULT, update);
		actionButton.setIcon(new ImageIcon("toolkits/icons/application_form_edit.png"));
		actionButton.setText("Predict");
		actionButton.setEnabled(false);
		nameLabel.setText("New Column Name:");
		name.setColumns(10);
		name.setText("prediction");
		name.addCaretListener(new CaretListener()
		{
			@Override
			public void caretUpdate(CaretEvent arg0)
			{
				checkColumnName();
			}
		});
		
		overwriteBox.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				checkColumnName();
			}
		});
		
//		useEvaluationBox.setToolTipText("Just use the prediction results from the (cross-validated, etc) model evaluation.");
		
		settings.add("left", warn);
//		settings.add("left", showMaxScoreBox);
//		settings.add("left", useEvaluationBox);
		settings.add("left", showDistsBox);
		settings.add("left", overwriteBox);
		
//		useEvaluationBox.addActionListener(new ActionListener()
//		{
//			@Override
//			public void actionPerformed(ActionEvent arg0)
//			{
//				String nameSuggestion = getColumnNameSuggestion();
//				name.setText(nameSuggestion);
//				//setText!
//			}
//			
//		});
		
		actionButton.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				PredictLabelsControl.executePredictTask(PredictActionBar.this, name.getText(), showMaxScoreBox.isSelected(), showDistsBox.isSelected(), overwriteBox.isSelected(), PredictLabelsControl.shouldUseValidationResults());
			}
		});
		
	}

	@Override
	public void startedTask()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void endedTask()
	{
		Workbench.update(this);
		Workbench.update(Stage.DOCUMENT_LIST);
	}
	
	@Override
	public void refreshPanel()
	{
		Recipe trainRecipe = PredictLabelsControl.getHighlightedTrainedModelRecipe();
		Recipe unlabeledRecipe = PredictLabelsControl.getHighlightedUnlabeledData();

		boolean validation = PredictLabelsControl.shouldUseValidationResults();
		if(lastRecipe != trainRecipe || lastValidation != validation)
		{
			lastRecipe = trainRecipe;
			lastValidation = validation;
			name.setText(getColumnNameSuggestion());
		}
		checkColumnName();
	}

	protected boolean isPredictionPossible()
	{
		if( PredictLabelsControl.hasHighlightedTrainedModelRecipe() )
		{
			if(PredictLabelsControl.shouldUseValidationResults()) return true;
			if(!Workbench.getRecipesByPane(Stage.DOCUMENT_LIST).isEmpty())
			{
				Recipe docs = PredictLabelsControl.getHighlightedUnlabeledData();
				if(docs != null)
				{
					Collection<String> docsColumns = new HashSet<String>();
					docsColumns.addAll(docs.getDocumentList().allAnnotations().keySet());
					docsColumns.addAll(docs.getDocumentList().getTextColumns());
					Set<String> modelTextColumns = PredictLabelsControl.getHighlightedTrainedModelRecipe().getDocumentList().getTextColumns();
					if(!docsColumns.containsAll(modelTextColumns))
					{
						warn.setWarning("This data doesn't have the neccessary columns for text prediction:\n"+modelTextColumns );
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	protected void checkColumnName()
	{
		if(name.getText().isEmpty())
		{
			warn.setWarning("You need to provide a name for the predicted label.");
			actionButton.setEnabled(false);
		}
		else
		{
			boolean possible = isPredictionPossible();
			if(possible)
			{
				if(PredictLabelsControl.hasHighlightedUnlabeledData() &&
					PredictLabelsControl.getHighlightedUnlabeledData().getDocumentList().allAnnotations().containsKey(name.getText()))
				{
					warn.setWarning("This will over-write an existing column.");
				}
				else
				{
					warn.clearWarning();
				}
			}
			actionButton.setEnabled(possible);
		}
	}

	protected String getColumnNameSuggestion()
	{
		Recipe recipe = PredictLabelsControl.getHighlightedTrainedModelRecipe();
		String prefix = recipe == null ? "": recipe.getTrainingTable().getAnnotation()+"_";
		String suffix = PredictLabelsControl.shouldUseValidationResults()?"validation":"prediction";
		String nameSuggestion = prefix+suffix;
		return nameSuggestion;
	}

}
