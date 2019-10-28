package edu.cmu.side.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.OrderedPluginMap;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.plugin.LearningPlugin;
import edu.cmu.side.plugin.ModelMetricPlugin;
import edu.cmu.side.plugin.RestructurePlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.plugin.WrapperPlugin;
import edu.cmu.side.plugin.control.PluginManager;
import edu.cmu.side.view.generic.ActionBar;
import edu.cmu.side.view.generic.ActionBarTask;
import edu.cmu.side.view.util.DefaultMap;
import edu.cmu.side.view.util.ParallelTaskUpdater;

public class BuildModelControl extends GenesisControl
{

	private static Recipe highlightedFeatureTable;
	private static Recipe highlightedTrainedModel;

	private static Map<String, Serializable> validationSettings;
	private static Map<LearningPlugin, Boolean> learningPlugins;
	private static Map<WrapperPlugin, Boolean> wrapperPlugins;
	private static LearningPlugin highlightedLearningPlugin;

	private static Collection<ModelMetricPlugin> modelEvaluationPlugins;
	private static StatusUpdater update = new ParallelTaskUpdater(10);//new SwingUpdaterLabel();
	private static String newName = "model";

	private static boolean currentlyTraining = false;
	protected static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static
	{
		validationSettings = new TreeMap<String, Serializable>();
		learningPlugins = new HashMap<LearningPlugin, Boolean>();
		SIDEPlugin[] learners = PluginManager.getSIDEPluginArrayByType("model_builder");
		for (SIDEPlugin le : learners)
		{
			learningPlugins.put((LearningPlugin) le, true);
		}

		modelEvaluationPlugins = new ArrayList<ModelMetricPlugin>();
		SIDEPlugin[] tableEvaluations = PluginManager.getSIDEPluginArrayByType("model_evaluation");
		for (SIDEPlugin fe : tableEvaluations)
		{
			modelEvaluationPlugins.add((ModelMetricPlugin) fe);
		}

		wrapperPlugins = new HashMap<WrapperPlugin, Boolean>();
		SIDEPlugin[] wrappers = PluginManager.getSIDEPluginArrayByType("learning_wrapper");
		for (SIDEPlugin wr : wrappers)
		{
			wrapperPlugins.put((WrapperPlugin) wr, false);
		}
	}

	public static Collection<ModelMetricPlugin> getModelEvaluationPlugins()
	{
		return modelEvaluationPlugins;
	}

	public static void setUpdater(StatusUpdater up)
	{
		update = up;
	}

	public static void setNewName(String n)
	{
		newName = n;
	}

	public static String getNewName()
	{
		return newName;
	}

	public static StatusUpdater getUpdater()
	{
		return update;
	}

	public static Map<String, Serializable> getValidationSettings()
	{
		return validationSettings;
	}

	public static void updateValidationSetting(String key, Serializable value)
	{
		validationSettings.put(key, value);
	}

	public static class ValidationButtonListener implements ActionListener
	{

		String key;
		String value;

		public ValidationButtonListener(String k, String v)
		{
			key = k;
			value = v;
		}

		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			BuildModelControl.updateValidationSetting(key, value);
		}

		public void setValue(String v)
		{
			value = v;
		}
	}

	public static Map<Integer, Integer> getFoldsMapRandom(DocumentList documents, int num)
	{
		Map<Integer, Integer> foldsMap = new TreeMap<Integer, Integer>();

		for (int i = 0; i < documents.getSize(); i++)
		{
			foldsMap.put(i, i % num);
		}
		return foldsMap;
	}

	public static Map<Integer, Integer> getFoldsMapByAnnotation(DocumentList documents, String annotation, int num)
	{
		Map<Integer, Integer> foldsMap = new TreeMap<Integer, Integer>();

		int foldNum = 0;

		// System.out.println("BMC 131: fold by annotation using up to "+num+" folds");

		Map<String, Integer> foldsByLabel = new TreeMap<String, Integer>();
		List<String> annotationValues = documents.getAnnotationArray(annotation);
		for (int i = 0; i < documents.getSize(); i++)
		{
			String annotationValue = annotationValues.get(i);
			if (!foldsByLabel.containsKey(annotationValue))
			{
				foldsByLabel.put(annotationValue, foldNum++);
				// System.out.println("BMC 141: "+annotationValue+" is assigned to fold #"+foldsByLabel.get(annotationValue)
				// % num);
			}
			foldsMap.put(i, foldsByLabel.get(annotationValue) % num);
		}

		return foldsMap;
	}

	public static Map<Integer, Integer> getFoldsMapByFile(DocumentList documents, int num)
	{
		Map<Integer, Integer> foldsMap = new TreeMap<Integer, Integer>();
		int foldNum = 0;
		Map<String, Integer> folds = new TreeMap<String, Integer>();
		for (int i = 0; i < documents.getSize(); i++)
		{
			String filename = documents.getFilename(i);
			if (!folds.containsKey(filename))
			{
				folds.put(filename, foldNum++);
			}
			foldsMap.put(i, folds.get(filename) % num);
		}
		return foldsMap;
	}

	public static class TrainModelListener implements ActionListener
	{

		ActionBar action;
		JTextField name;

		public TrainModelListener(ActionBar action, JTextField n)
		{
			this.action = action;
			name = n;
		}

		@Override
		public void actionPerformed(ActionEvent arg0)
		{
			Workbench.update(action);

			try
			{
				if (Boolean.TRUE.toString().equals(validationSettings.get("test")))
				{
					if (validationSettings.get("type").equals("CV"))
					{
						validationSettings.put("testSet", getHighlightedFeatureTableRecipe().getDocumentList());
					}
				}

				// System.out.println("BMC 198: wrappers="+wrapperPlugins);

				Recipe newRecipe = getHighlightedFeatureTableRecipe();
				LearningPlugin learner = getHighlightedLearningPlugin();
				Map<String, String> settings = learner.generateConfigurationSettings();

				newRecipe = Recipe.addLearnerToRecipe(newRecipe, learner, settings);
				newRecipe.setValidationSettings(new TreeMap<String, Serializable>(validationSettings));

				for (WrapperPlugin wrap : wrapperPlugins.keySet())
				{
					if (wrapperPlugins.get(wrap))
					{
						newRecipe.addWrapper(wrap, wrap.generateConfigurationSettings());
					}
				}

				BuildModelControl.BuildModelTask task = new BuildModelControl.BuildModelTask(action, newRecipe, name.getText());
				task.executeActionBarTask();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e.getMessage(), "Build Model", JOptionPane.ERROR_MESSAGE);
			}
		}

	}

	protected static FeatureTable prepareTestFeatureTable(Recipe recipe, DocumentList test, StatusUpdater updater)
	{
		prepareDocuments(test); // assigns classes, annotations.

		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();
		OrderedPluginMap extractors = recipe.getExtractors();
		for (SIDEPlugin plug : extractors.keySet())
		{
			Collection<FeatureHit> extractorHits = ((FeaturePlugin) plug).extractFeatureHits(test, extractors.get(plug), updater);
			hits.addAll(extractorHits);
		}
		FeatureTable originalTable = recipe.getTrainingTable();
		FeatureTable ft = new FeatureTable(test, hits, 0, originalTable.getAnnotation(), originalTable.getClassValueType());
		for (SIDEPlugin plug : recipe.getFilters().keySet())
		{
			ft = ((RestructurePlugin) plug).filterTestSet(originalTable, ft, recipe.getFilters().get(plug), recipe.getFilteredTable().getThreshold(), updater);
		}

		ft.reconcileFeatures(originalTable.getFeatureSet());

		return ft;

	}

	public static class BuildModelTask extends ActionBarTask
	{
		Recipe plan;
		String name;
		Exception ex;
		private boolean audioEnabled = true && new File("toolkits/train.wav").exists();

		public BuildModelTask(ActionBar action, Recipe newRecipe, String n)
		{
			super(action);
			plan = newRecipe;
			name = n;
		}

		@Override
		protected void finishTask()
		{
			super.finishTask();

			if (ex != null)
			{
				Exception exception = ex;
				ex = null;
				if (exception.getMessage() != null && exception.getMessage().equals("User Canceled"))
					JOptionPane.showMessageDialog(null, "Model Training Canceled.", "Train Stop", JOptionPane.INFORMATION_MESSAGE);

				else
					JOptionPane.showMessageDialog(null, "Model Training Failed.\n" + exception.getLocalizedMessage(), "Train Wreck", JOptionPane.ERROR_MESSAGE);
			}
			else if (plan != null)
			{
				BuildModelControl.setHighlightedTrainedModelRecipe(plan);
				Workbench.getRecipeManager().addRecipe(plan);
				ExploreResultsControl.setHighlightedTrainedModelRecipe(plan);
				CompareModelsControl.setCompetingTrainedModelRecipe(plan);
				PredictLabelsControl.setHighlightedTrainedModelRecipe(plan);
				Workbench.update(Stage.TRAINED_MODEL);

				if (audioEnabled) try
				{
					AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("toolkits/train.wav"));
					Clip clip = AudioSystem.getClip();
					clip.open(audioInputStream);
					clip.start();
				}
				catch (Exception ex)
				{
					logger.warning("Error playing choo-choo sound.");
					ex.printStackTrace();
				}
			}

			currentlyTraining = false;

		}

		@Override
		public void forceCancel()
		{
			ex = new RuntimeException("User Canceled");
			plan = null;
			super.forceCancel();
		}

		@Override
		protected void doTask()
		{
			try
			{
				FeatureTable current = plan.getTrainingTable();
				if (current != null)
				{
					TrainingResult results = null;
					if (validationSettings.get("type").equals("SUPPLY"))
					{
						DocumentList test = (DocumentList) validationSettings.get("testSet");
						FeatureTable extractTestFeatures = prepareTestFeatureTable(plan, test, update);
						validationSettings.put("testFeatureTable", extractTestFeatures);

						// if we've already trained the exact same model, don't
						// do it again. Just evaluate.
						update.update("Checking cache for pre-trained model...");
						Recipe cached = checkForCachedModel();
						if (cached != null)
						{
							update.update("Copying cached model for evaluation");
							logger.info("Cached model '"+cached.getRecipeName()+"' matches current settings - using it instead of training again.");
							results = evaluateUsingCachedModel(current, extractTestFeatures, cached, plan);
						}
					}

					if (results == null)
					{
						logger.info("Training new model.");
						results = plan.getLearner().train(current, plan.getLearnerSettings(), validationSettings, plan.getWrappers(),
								BuildModelControl.getUpdater());
					}

					if (results != null)
					{
						plan.setTrainingResult(results);
						results.setName(name);

						plan.setLearnerSettings(plan.getLearner().generateConfigurationSettings());
						plan.setValidationSettings(new TreeMap<String, Serializable>(validationSettings));
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				plan = null;
				ex = e;
			}
		}

		/**
		 * Copy the trained models and learned wrapper settings from the cached model into the newRecipe,
		 * and evaluate on the provided test set.
		 * @param trainFeatures
		 * @param testFeatures
		 * @param cached
		 * @param newRecipe
		 * @return
		 * @throws Exception
		 */
		public static TrainingResult evaluateUsingCachedModel(FeatureTable trainFeatures, FeatureTable testFeatures, Recipe cached, Recipe newRecipe) throws Exception
		{
			TrainingResult results;
			//update our recipe to include the trained model and learned wrapper.
			TreeMap<String, String> clonedLearnerSettings = new TreeMap<String, String>(cached.getLearnerSettings());
			newRecipe.setLearnerSettings(clonedLearnerSettings);
			newRecipe.getLearner().configureFromSettings(clonedLearnerSettings);
			newRecipe.getLearner().loadClassifierFromSettings(clonedLearnerSettings);
			
			//load the learned settings from the cached model's plugins
			OrderedPluginMap clonedWrappers = new OrderedPluginMap(cached.getWrappers());
			newRecipe.setWrappers(clonedWrappers);
			FeatureTable wrapped = trainFeatures;
			for(SIDEPlugin plug : clonedWrappers.keySet())
			{
				WrapperPlugin wrapper = (WrapperPlugin) plug;
				wrapper.configureFromSettings(clonedWrappers.get(wrapper));
				wrapped = wrapper.wrapTableBefore(trainFeatures, 1, new DefaultMap<Integer, Integer>(0), update);
			}
			
			//evaluate the new data on the cached model.
			update.update("Evaluating test set on cached model...");
			results = newRecipe.getLearner().evaluateTestSet(wrapped, testFeatures, newRecipe.getWrappers(), update);
			return results;
		}

		/*
		 * Check if there's already a trained model for this feature table and
		 * learner+wrapper settings. If so, don't retrain, just evaluate.
		 */
		private Recipe checkForCachedModel()
		{
			Collection<Recipe> alreadyTrainedModels = Workbench.getRecipesByPane(Stage.TRAINED_MODEL);
			for (Recipe trained : alreadyTrainedModels)
			{
				if (trained.getLearner().getClass().equals(plan.getLearner().getClass()))
				{
					//make sure that the learner settings (except for classifier) all match.
					Map<String, String> planLearnerSettings = plan.getLearnerSettings();
					
					if(!plan.getLearner().settingsMatch(planLearnerSettings, trained.getLearnerSettings()))
						continue;
					
					if (trained.getWrappers().equals(plan.getWrappers()) && trained.getTrainingTable().equals(plan.getTrainingTable()))
					{
						logger.info("Cached model matches "+trained.getRecipeName());
						return trained;
					}
				}
			}
			return null;
		}

		@Override
		public void requestCancel()
		{
			plan.getLearner().stopWhenPossible();
		}
	}

	// TODO: learn from the changes made in chef.Predictor
	// protected void prepareTestSet(Recipe train, DocumentList test){
	// Collection<FeatureHit> hits = new TreeSet<FeatureHit>();
	// for(SIDEPlugin plug : train.getExtractors().keySet()){
	// plug.configureFromSettings(train.getExtractors().get(plug));
	// hits.addAll(((FeaturePlugin)plug).extractFeatureHits(test,
	// train.getExtractors().get(plug), update));
	// }
	// FeatureTable ft = new FeatureTable(test, hits,
	// train.getFeatureTable().getThreshold());
	// for(SIDEPlugin plug : train.getFilters().keySet()){
	// ft = ((RestructurePlugin)plug).filterTestSet(train.getTrainingTable(),
	// ft, train.getFilters().get(plug), update);
	// }
	// }

	public static Map<LearningPlugin, Boolean> getLearningPlugins()
	{
		return learningPlugins;
	}

	public static Map<WrapperPlugin, Boolean> getWrapperPlugins()
	{
		return wrapperPlugins;
	}

	public static int numLearningPlugins()
	{
		return learningPlugins.size();
	}

	public static void setHighlightedLearningPlugin(LearningPlugin l)
	{
		highlightedLearningPlugin = l;
		for (LearningPlugin plug : learningPlugins.keySet())
		{
			learningPlugins.put(plug, plug == l);
		}
	}

	public static LearningPlugin getHighlightedLearningPlugin()
	{
		return highlightedLearningPlugin;
	}

	public static boolean hasHighlightedFeatureTableRecipe()
	{
		return highlightedFeatureTable != null;
	}

	public static boolean hasHighlightedTrainedModelRecipe()
	{
		return highlightedTrainedModel != null;
	}

	public static Recipe getHighlightedFeatureTableRecipe()
	{
		return highlightedFeatureTable;
	}

	public static Recipe getHighlightedTrainedModelRecipe()
	{
		return highlightedTrainedModel;
	}

	public static void setHighlightedFeatureTableRecipe(Recipe highlight)
	{
		highlightedFeatureTable = highlight;
	}

	public static void setHighlightedTrainedModelRecipe(Recipe highlight)
	{
		highlightedTrainedModel = highlight;
	}

	public static void prepareDocuments(DocumentList test) throws IllegalStateException
	{
		Recipe rec = getHighlightedFeatureTableRecipe();
		prepareDocuments(rec, validationSettings, test);
	}

	public static Map<String, Serializable> prepareDocuments(Recipe recipe, Map<String, Serializable> validation, DocumentList test)
			throws IllegalStateException
	{
		DocumentList train = recipe.getDocumentList();

		try
		{
			test.setCurrentAnnotation(recipe.getTrainingTable().getAnnotation(), recipe.getTrainingTable().getClassValueType());
			test.setTextColumns(new HashSet<String>(train.getTextColumns()));
			test.setDifferentiateTextColumns(train.getTextColumnsAreDifferentiated());

			Collection<String> trainColumns = train.allAnnotations().keySet();
			Collection<String> testColumns = test.allAnnotations().keySet();
			if (!testColumns.containsAll(trainColumns))
			{
				ArrayList<String> missing = new ArrayList<String>(trainColumns);
				missing.removeAll(testColumns);
				throw new java.lang.IllegalStateException("Test set annotations do not match training set.\nMissing columns: " + missing);
			}

			validationSettings.put("testSet", test);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new java.lang.IllegalStateException("Could not prepare test set.\n" + e.getMessage(), e);
		}
		return validationSettings;

	}

	public static boolean isCurrentlyTraining()
	{
		return currentlyTraining;
	}
}
