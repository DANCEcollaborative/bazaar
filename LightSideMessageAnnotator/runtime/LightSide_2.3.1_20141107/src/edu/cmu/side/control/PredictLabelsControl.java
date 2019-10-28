package edu.cmu.side.control;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.recipe.Predictor;
import edu.cmu.side.view.generic.ActionBarTask;
import edu.cmu.side.view.predict.PredictActionBar;
import edu.cmu.side.view.util.SwingUpdaterLabel;

public class PredictLabelsControl extends GenesisControl{

	private static Recipe trainedModel;
	private static Recipe highlightedUnlabeledData;
	
	private static Collection<Recipe> unlabeledDataRecipes = new ArrayList<Recipe>();
	private static StatusUpdater update = new SwingUpdaterLabel();
	private static boolean useValidationResults;

	public static StatusUpdater getUpdater()
	{
		return update;
	}

	public static boolean hasHighlightedTrainedModelRecipe(){
		return trainedModel!=null;
	}

	public static Recipe getHighlightedTrainedModelRecipe(){
		return trainedModel;
	}

	public static void setHighlightedTrainedModelRecipe(Recipe highlight){
		trainedModel = highlight;
		if(useValidationResults)
		{
			setHighlightedUnlabeledData(highlight);
			Workbench.update(Stage.DOCUMENT_LIST);
		}
	}
	
	public static boolean hasHighlightedUnlabeledData()
	{
		return highlightedUnlabeledData != null;
	}
	
	public static Recipe getHighlightedUnlabeledData()
	{
			return highlightedUnlabeledData;
	}
	
	public static void setHighlightedUnlabeledData(Recipe r){
		highlightedUnlabeledData = r;
	}
	
	public static Collection<Recipe> getUnlabeledDataRecipes(){
		return unlabeledDataRecipes;
	}

	public static void executePredictTask(final PredictActionBar predictActionBar, final String name, final boolean showMaxScore, final boolean showDists, final boolean overwrite,final boolean useEvaluation)
	{
		new ActionBarTask(predictActionBar)
		{
			DocumentList newDocs = null;
			Exception ex = null;

			@Override
			public void requestCancel()
			{
				halt = true; //not presently respected by the Predictor
			}
			
			@Override
			protected void finishTask()
			{
				super.finishTask();

				if(ex == null && newDocs != null)
				{
					RecipeManager manager = Workbench.getRecipeManager();
					Recipe fetched = manager.fetchDocumentListRecipe(newDocs);
					newDocs.setName(newDocs.getName() + " (" + name + ")");
					
					setHighlightedUnlabeledData(fetched);
					Workbench.update(Stage.DOCUMENT_LIST);
					predictActionBar.setEnabled(true);
				}
				else
				{
					if(ex == null) //something outside of the thread canceled the prediction
					{
						JOptionPane.showMessageDialog(null, "Prediction has been stopped.", "Prediction Stopped", JOptionPane.ERROR_MESSAGE);
					}
//					else if(ex.getMessage().equals("User Canceled"))
//					{
//						JOptionPane.showMessageDialog(null, "Prediction has been canceled.", "Prediction Canceled", JOptionPane.INFORMATION_MESSAGE);
//					}
					else
					{
						JOptionPane.showMessageDialog(null, "Couldn't finish the prediction.\n"+ex.getLocalizedMessage(), "Prediction Error", JOptionPane.ERROR_MESSAGE);
					}

					Workbench.update(Stage.DOCUMENT_LIST);
				}
			}
			
			@Override
			protected void doTask()
			{
				DocumentList originalDocs;
				ex = null;
				try
				{
					if(useEvaluation)
					{
						originalDocs = trainedModel.getTrainingResult().getEvaluationTable().getDocumentList();
	
						TrainingResult results = trainedModel.getTrainingResult();
						List<String> predictions = (List<String>) results.getPredictions();
						newDocs = addLabelsToDocs(name, showDists, overwrite, originalDocs, results, predictions);
					}
					else
					{
						originalDocs = highlightedUnlabeledData.getDocumentList();
	
						Predictor predictor = new Predictor(trainedModel, name);
						newDocs = predictor.predict(originalDocs, name, showDists, overwrite);
					}
				}
				catch(Exception e)
				{
					ex = e;
					
				}
				
				
				
				/*PredictionResult results;
				
				if(useEvaluation)
				{
					TrainingResult trainingResult = trainedModel.getTrainingResult();
					results = new PredictionResult(trainingResult.getPredictions(), trainingResult.getDistributions());
				}	
				else
				{
					Predictor predictor = new Predictor(trainedModel, name);
					results = predictor.predict(docs);
				}
				
				List<String> predictions = (List<String>) results.getPredictions();
				
				
				docs.addAnnotation(name, predictions, overwrite);
				

				Map<String, List<Double>> distributions = results.getDistributions();
				if(distributions != null)
				{
					List<String> likely = new ArrayList<String>();
					if(showMaxScore)
					{
						for(int i = 0; i < predictions.size(); i++)
						{
							likely.add(String.format("%.3f", distributions.get(predictions.get(i)).get(i)));
						}
						docs.addAnnotation(name+"_score", likely, overwrite);
					}
					
					if(showDists)
					{
						for(String label : trainedModel.getTrainingTable().getLabelArray())
						{
							List<String> dist = new ArrayList<String>();

							for(int i = 0; i < predictions.size(); i++)
							{
								dist.add(String.format("%.3f", distributions.get(label).get(i)));
							}
							
							docs.addAnnotation(name+"_"+label+"_score", dist, overwrite);
						}
					}
				}*/
						
			}

			protected DocumentList addLabelsToDocs(final String name, final boolean showDists, final boolean overwrite,
					DocumentList docs, TrainingResult results, List<String> predictions)
			{
				Map<String, List<Double>> distributions = results.getDistributions();
				DocumentList newDocs = docs.clone();
				newDocs.addAnnotation(name, predictions, overwrite);
				if(distributions != null)
				{	
					if(showDists)
					{
						for(String label : trainedModel.getTrainingTable().getLabelArray())
						{
							List<String> dist = new ArrayList<String>();

							for(int i = 0; i < predictions.size(); i++)
							{
								dist.add(String.format("%.3f", distributions.get(label).get(i)));
							}
							
							newDocs.addAnnotation(name+"_"+label+"_score", dist, overwrite);
						}
					}
				}
				return newDocs;
			}
		}.executeActionBarTask();
	}
	
	public static String getColumnNameSuggestion()
	{
		Recipe recipe = PredictLabelsControl.getHighlightedTrainedModelRecipe();
		String prefix = recipe == null ? "": recipe.getTrainingTable().getAnnotation()+"_";
		String suffix = useValidationResults?"validation":"prediction";
		String nameSuggestion = prefix+suffix;
		return nameSuggestion;
	}

	public static boolean shouldUseValidationResults()
	{
		return useValidationResults;
	}

	public static void setUseValidationResults(boolean validate)
	{
		useValidationResults = validate;
	}

	public static void setRetestModel(boolean selected)
	{
		// TODO Auto-generated method stub
		
	}
	
}
