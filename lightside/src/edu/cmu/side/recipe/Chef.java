package edu.cmu.side.recipe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Logger;

import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.model.OrderedPluginMap;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.plugin.ModelMetricPlugin;
import edu.cmu.side.plugin.RestructurePlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.plugin.control.ImportController;
import edu.cmu.side.plugin.control.PluginManager;
import edu.cmu.side.recipe.converters.ConverterControl;
import edu.cmu.side.recipe.converters.ConverterControl.RecipeFileFormat;
import edu.cmu.side.view.util.RecipeExporter;

/**
 * loads a model trained using LightSide and uses it to label new instances.
 * 
 * @author dadamson
 */
public class Chef
{
	// static
	// {
	// System.setProperty("java.awt.headless", "true");
	// logger.info(java.awt.GraphicsEnvironment.isHeadless() ?
	// "Running in headless mode." : "Not actually headless");
	// }

	static boolean quiet = true;
	static final protected Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	static StatusUpdater textUpdater = new StatusUpdater()
	{

		@Override
		public void update(String updateSlot, int slot1, int slot2)
		{
			if (!quiet) logger.info(updateSlot + ": " + slot1 + "/" + slot2);
		}

		@Override
		public void update(String update)
		{
			if (!quiet) logger.info(update);
		}

		@Override
		public void reset()
		{
			// TODO Auto-generated method stub

		}
	};


	protected static void simmerFeatures(Recipe recipe, int threshold, String annotation, Type type)
	{
		simmerFeatures(recipe, threshold, annotation, type, recipe.getStage());
	}
	
	// Extract Features
	protected static void simmerFeatures(Recipe recipe, int threshold, String annotation, Type type, Stage finalStage)
	{
		final DocumentList corpus = recipe.getDocumentList();
		final OrderedPluginMap extractors = recipe.getExtractors();
		final ConcurrentSkipListSet<String> hitChunks = new ConcurrentSkipListSet<String>();
		final Collection<FeatureHit> hits = new TreeSet<FeatureHit>();

		for (final SIDEPlugin plug : extractors.keySet())
		{

			if (!quiet) logger.info("Chef: Simmering features with " + plug + "...");
			// logger.info("Extractor Settings: "+extractors.get(plug));
			Collection<FeatureHit> extractorHits = ((FeaturePlugin) plug).extractFeatureHits(corpus, extractors.get(plug), textUpdater);
			hits.addAll(extractorHits);

			if (!quiet) logger.info("Chef: Finished simmering with " + plug + "...");
		}

		if (!quiet) logger.info("Chef: Done simmering with plugins!");

		if (!quiet) logger.info("Chef: Building feature table...");

		FeatureTable ft = new FeatureTable(corpus, hits, threshold, annotation, type);
		recipe.setFeatureTable(ft);

		if (!quiet) logger.info("Chef: Done building feature table!");
		if (finalStage.compareTo(Stage.MODIFIED_TABLE) >= 0) 
		{
			for (SIDEPlugin plug : recipe.getFilters().keySet())
			{
				if (!quiet) logger.info("Restructuring features with " + plug + "...");
				ft = ((RestructurePlugin) plug).restructure(recipe.getTrainingTable(), recipe.getFilters().get(plug), threshold, textUpdater);
			}
			recipe.setFilteredTable(ft);
		}
		ft.setName(recipe.getRecipeName() + " features");
	}

	public static Recipe followSimmerSteps(Recipe originalRecipe, DocumentList corpus, Stage finalStage, int newThreshold)
	{
		Recipe newRecipe = Recipe.copyEmptyRecipe(originalRecipe);

		prepareDocumentList(originalRecipe, corpus);
		newRecipe.setDocumentList(corpus);
		printMemoryUsage();

		if (finalStage == Stage.DOCUMENT_LIST) return newRecipe;

		String annotation = originalRecipe.getAnnotation();

		if (!corpus.allAnnotations().containsKey(annotation)) annotation = null;

		simmerFeatures(newRecipe, newThreshold, annotation, originalRecipe.getClassValueType(), finalStage);

		return newRecipe;
	}

	public static Recipe followRecipeWithTestSet(Recipe originalRecipe, DocumentList corpus, DocumentList testSet, Stage finalStage, int newThreshold)
			throws Exception
	{
		Recipe newRecipe = followSimmerSteps(originalRecipe, corpus, finalStage, newThreshold);

		Map<String, Serializable> validationSettings = new TreeMap<String, Serializable>();
		validationSettings.put("test", Boolean.TRUE);
		validationSettings.put("type", "SUPPLY");

		// Creates a reconciled test set feature table.
		validationSettings = BuildModelControl.prepareDocuments(newRecipe, validationSettings, testSet);

		newRecipe.setValidationSettings(validationSettings);

		newRecipe = broilModel(newRecipe);
		return newRecipe;
	}

	// TODO: be more consistent in parameters to recipe stages
	public static Recipe followRecipe(Recipe originalRecipe, DocumentList corpus, Stage finalStage, int newThreshold) throws Exception
	{
		Recipe newRecipe = followSimmerSteps(originalRecipe, corpus, finalStage, newThreshold);
		
		if (finalStage.compareTo(Stage.MODIFIED_TABLE) > 0)
		{
			broilModel(newRecipe);
		}
		return newRecipe;
	}

	/**
	 * Build model and update recipe settings to include the new classifier.
	 * 
	 * @param newRecipe
	 * @throws Exception
	 */
	// Build Model
	protected static Recipe broilModel(Recipe newRecipe) throws Exception
	{
		if (!quiet) logger.info("Training model with " + newRecipe.getLearner() + "...");
		FeatureTable trainingTable = newRecipe.getTrainingTable();
		Map<String, String> learnerSettings = newRecipe.getLearnerSettings();
		Map<String, Serializable> validationSettings = newRecipe.getValidationSettings();
		OrderedPluginMap wrappers = newRecipe.getWrappers();
		
		TrainingResult trainResult = newRecipe.getLearner().train(trainingTable, learnerSettings,
				validationSettings, wrappers, textUpdater);
		newRecipe.setTrainingResult(trainResult);
		newRecipe.setLearnerSettings(newRecipe.getLearner().generateConfigurationSettings());
		return newRecipe;
	}

	/**
	 * @param originalRecipe
	 * @param corpus
	 */
	protected static void prepareDocumentList(Recipe originalRecipe, DocumentList corpus)
	{
		if (!quiet) logger.info("Preparing documents...");
		DocumentList original = originalRecipe.getDocumentList();
		FeatureTable originalTable = originalRecipe.getTrainingTable();
		String currentAnnotation = originalTable.getAnnotation();
		if (corpus.allAnnotations().containsKey(currentAnnotation))
		{
			corpus.setCurrentAnnotation(currentAnnotation, originalRecipe.getClassValueType());
		}
		else
		{
			// System.err.println("Warning: data has no "+currentAnnotation+" annotation. You can't train a new model on this data (only predict)");
		}
		corpus.setLabelArray(originalRecipe.getLabelArray());
		corpus.setTextColumns(new HashSet<String>(originalRecipe.getTextColumns()));
	}

	public static Recipe loadRecipe(String recipePath) throws IOException, FileNotFoundException
	{

		return ConverterControl.loadRecipe(recipePath);
	}

	public static void saveRecipe(Recipe recipe, File target, RecipeFileFormat exportFormat) throws IOException
	{
		ConverterControl.writeRecipeToFile(target.getPath(), recipe, exportFormat);
	}

	public static void main(String[] args) throws Exception
	{
		quiet = false;
		String recipePath, outPath;
		if (args.length < 5 || !Arrays.asList("predict", "full").contains(args[0]))
		{
			printUsage();
			System.exit(1);
		}

		try
		{

			recipePath = args[2];
			outPath = args[3];
			boolean predictOnly = args[0].equals("predict");
			Charset encoding = Charset.forName(args[1]);

			Set<String> corpusFiles = new HashSet<String>();

			for (int i = 4; i < args.length; i++)
			{
				corpusFiles.add(args[i]);
			}

			if (!quiet) logger.info("Loading " + recipePath);
			Recipe recipe = loadRecipe(recipePath);

			if (!quiet) logger.info("Loading documents: " + corpusFiles);
			DocumentList newDocs = ImportController.makeDocumentList(corpusFiles, encoding);
			Recipe result = followRecipe(recipe, newDocs, recipe.getStage(), recipe.getFeatureTable().getThreshold());

			if (result.getStage().compareTo(Stage.TRAINED_MODEL) >= 0)
			{
				displayTrainingResults(result);
			}
			
			result.setRecipeName(new File(args[4]).getName()+" Model");

			if(!outPath.toLowerCase().endsWith(".xml"))
			{
				outPath += predictOnly?".predict.xml":".xml";
			}

			logger.info("Saving finished recipe to " + outPath);
			if (predictOnly)
			{
				Recipe predict = Recipe.copyPredictionRecipe(result);
				saveRecipe(predict, new File(outPath), RecipeFileFormat.XML);
			}
			else
				saveRecipe(result, new File(outPath), RecipeFileFormat.XML);

			System.exit(0);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("\n****");
			printUsage();
			System.exit(1);
		}
	}

	public static void printUsage()
	{
		System.err.println("Usage: scripts/train.sh {full|predict} {data-encoding} saved/template.model.xml saved/new.model.xml data.csv...");
		System.out.println("Follows a trained model template on a new data set.");
		System.out.println("Model can be saved in full (for error analysis), or in a prediction-only format.");
		System.out.println("Common data encodings are UTF-8, windows-1252, and MacRoman.");
		System.out.println("(Make sure that the text columns, class column, and any columns used as features have the same names in the new data as they did for the template.)");
	}

	protected static void printMemoryUsage()
	{
		if (quiet) return;

		double gigs = 1024 * 1024 * 1024;
		MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

		double beanMax = usage.getMax() / gigs;
		double beanUsed = usage.getUsed() / gigs;

		logger.info(String.format("%.1f/%.1f GB used", beanUsed, beanMax));
	}

	/**
	 * @param recipe
	 */
	protected static void displayTrainingResults(Recipe recipe)
	{
		if (recipe.getStage().compareTo(Stage.TRAINED_MODEL) >= 0)
		{
			TrainingResult trainingResult = recipe.getTrainingResult();
			printEvaluations(trainingResult);
		}
	}

	public static void printEvaluations(TrainingResult trainingResult)
	{
		System.out.println("Confusion Matrix (act \\ pred):");
		System.out.println(trainingResult.getTextConfusionMatrix());
		
		SIDEPlugin[] plugins = PluginManager.getSIDEPluginArrayByType(ModelMetricPlugin.type);
		
		for(SIDEPlugin plug : plugins)
		{
			ModelMetricPlugin metricPlugin = (ModelMetricPlugin) plug;
			Map<String, String> evaluations = metricPlugin.evaluateModel(trainingResult, null);
			System.out.println(plug.toString());
			for (Entry<String, String> eval : evaluations.entrySet())
			{
				try
				{
					System.out.printf("%10s:\t%.4f\n", eval.getKey(), Double.parseDouble(eval.getValue()));
				}
				catch(NumberFormatException e)
				{
					System.out.printf("%10s:\t%s\n", eval.getKey(),eval.getValue());
				}
			}
			System.out.println();
		}
	}
	
	public static void setQuiet(boolean b)
	{
		quiet = b;
	}

}
