package edu.cmu.side.recipe;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.plugin.LearningPlugin;
import edu.cmu.side.plugin.control.ImportController;
import edu.cmu.side.view.util.CSVExporter;
import edu.cmu.side.view.util.DocumentListTableModel;

/**
 * loads a model trained using lightSIDE uses it to label new instances.
 * 
 * @author dadamson
 */
public class Predictor
{
	/**
	 * 
	 * @param modelFilePath
	 *            the path to the SIDE model file
	 */
	String modelPath;
	String predictionAnnotation = "predicted";
	String corpusCurrentAnnot = "class";

	// File name/location is defined in parameter map
	Recipe recipe;
	private boolean quiet = true;
	protected static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	StatusUpdater textUpdater = new StatusUpdater()
	{

		@Override
		public void update(String updateSlot, int slot1, int slot2)
		{
			if (!isQuiet()) System.err.println(updateSlot + ": " + slot1 + "/" + slot2);
		}

		@Override
		public void update(String update)
		{
			if (!isQuiet()) System.err.println(update);
		}

		@Override
		public void reset()
		{

		}
	};

	public Predictor(Recipe r, String p)
	{
		this.recipe = r;
		this.predictionAnnotation = p;
		setQuiet(true);
	}

	public Predictor(Map<String, String> params) throws IOException, FileNotFoundException
	{

		if (!isQuiet()) logger.info(params.toString());

		this.modelPath = params.get("path");
		this.predictionAnnotation = params.get("prediction");
		this.corpusCurrentAnnot = params.get("currentAnnotation");
		loadModel();
	}

	public Predictor(String modelPath, String annotationName) throws IOException, FileNotFoundException
	{
		this.modelPath = modelPath;
		this.predictionAnnotation = "predicted";
		this.corpusCurrentAnnot = annotationName;

		loadModel();
	}

	/**
	 * 
	 * @param instance
	 *            the string to classify.
	 * 
	 * @return a map of predicted category-labels to associated probabilities.
	 *         Note that SIDE doesn't yet expose the probability distribution of
	 *         its predictions, so this might just be a single entry
	 *         (predicted_label, 1.0)
	 */
	public List<? extends Comparable> predict(List<String> instances)
	{

		DocumentList corpus = null;
		corpus = new DocumentList(instances);

		return predict(corpus).getPredictions();

	}

	public double predictScore(String instance, String label)
	{
		DocumentList corpus = null;
		corpus = new DocumentList(instance);

		PredictionResult predictionResult = predict(corpus);

		return predictionResult.getDistributions().get(label).get(0);
	}

	/**
	 * @param corpus
	 * @return a DocumentList with new columns!
	 */
	public DocumentList predict(DocumentList corpus, String predictionColumn, boolean addDistributionColumns, boolean overWrite)
	{

		PredictionResult result = null;
		Recipe newRecipe = null;
		try
		{
			Chef.quiet = isQuiet();
			newRecipe = Chef.followRecipe(recipe, corpus, Stage.MODIFIED_TABLE, 0);
			FeatureTable predictTable = newRecipe.getTrainingTable();

			if (!isQuiet())
			{
				logger.info(predictTable.getFeatureSet().size() + " features total");
				logger.info(predictTable.getHitsForDocument(0).size() + " feature hits in document 0");
			}
			calculatePredictionStats(predictTable);

			result = predictFromTable(predictTable);

			DocumentList newDocs = newRecipe.getDocumentList().clone();
			// newDocs = new DocumentList(new
			// ArrayList(newDocs.getFilenameList()), new TreeMap<String,
			// List<String>>(newDocs.getCoveredTextList()), new TreeMap<String,
			// List<String>>(newDocs.allAnnotations()),
			// predictTable.getAnnotation());

			return Predictor.addPredictionsToDocumentList(predictionColumn, addDistributionColumns, overWrite, result, newDocs);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public static DocumentList addPredictionsToDocumentList(String predictionColumn, boolean addDistributionColumns, boolean overWrite, PredictionResult result,
			DocumentList newDocs)
	{
		List<String> annotationStrings = new ArrayList<String>(newDocs.getSize());
		
		for(Comparable c : result.getPredictions())
		{
			annotationStrings.add(c.toString());
		}
		
		newDocs.addAnnotation(predictionColumn, annotationStrings, overWrite);
		int size = newDocs.getSize();
		if (addDistributionColumns)
		{
			Map<String, List<Double>> distributions = result.getDistributions();
			for (String label : distributions.keySet())
			{
				List<String> stringDists = new ArrayList<String>(size);
				for (Double d : distributions.get(label))
				{
					stringDists.add(d.toString());
				}
				newDocs.addAnnotation(predictionColumn + "_" + label, stringDists, overWrite);
			}
		}

		return newDocs;
	}

	public void calculatePredictionStats(FeatureTable predictTable)
	{
		SummaryStatistics hitStats = new SummaryStatistics();
		SummaryStatistics densityStats = new SummaryStatistics();
		SummaryStatistics lengthStats = new SummaryStatistics();

		DocumentList docs = predictTable.getDocumentList();

		for (int i = 0; i < docs.getSize(); i++)
		{
			double hitCount = predictTable.getHitsForDocument(i).size();
			hitStats.addValue(hitCount);
			double length = docs.getPrintableTextAt(i).length();
			densityStats.addValue(hitCount / (1.0 + length));

			double wordLength = docs.getPrintableTextAt(i).split("\\s+").length;
			lengthStats.addValue(wordLength);
		}

		logger.info("Feature Density Mean: " + densityStats.getMean());
		logger.info("Feature Density Deviation: " + densityStats.getStandardDeviation());
		//
	}

	/**
	 * @param corpus
	 * @return
	 */
	public PredictionResult predict(DocumentList corpus)
	{
		PredictionResult result = null;
		try
		{
			Chef.quiet = isQuiet();
			Long when = System.currentTimeMillis();
			Recipe newRecipe = Chef.followRecipe(recipe, corpus, Stage.MODIFIED_TABLE, 0);
			System.out.println("followRecipe took " + (System.currentTimeMillis() - when) / 1000.0 + " seconds");

			when = System.currentTimeMillis();
			FeatureTable predictTable = newRecipe.getTrainingTable();
			System.out.println("getTrainingTable took " + (System.currentTimeMillis() - when) / 1000.0 + " seconds");

			if (!isQuiet())
			{
				logger.info(predictTable.getFeatureSet().size() + " features total");
				logger.info(predictTable.getHitsForDocument(0).size() + " feature hits in document 0");
			}

			result = predictFromTable(predictTable);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return result;
	}

	public PredictionResult predictFromTable(FeatureTable predictTable) throws Exception
	{
		PredictionResult result = null;
		FeatureTable trainingTable = recipe.getTrainingTable();
		predictTable.reconcileFeatures(trainingTable.getFeatureSet());

		if (!isQuiet())
		{
			logger.info(predictTable.getHitsForDocument(0).size() + " feature hits in document 0 after reconciliation");
			logger.info(predictTable.getFeatureSet().size() + " features total");
		}

		Long when = System.currentTimeMillis();
		LearningPlugin lp = recipe.getLearner();
		System.out.println("getLearner() took " + (System.currentTimeMillis() - when) / 1000.0 + " seconds");

		when = System.currentTimeMillis();
		result = lp.predict(trainingTable, predictTable, recipe.getLearnerSettings(), textUpdater, recipe.getWrappers());
		System.out.println("learner.predict took " + (System.currentTimeMillis() - when) / 1000.0 + " seconds");

		return result;
	}

	public String prettyPredict(String instance)
	{

		DocumentList corpus = null;
		corpus = new DocumentList(instance);
		String prediction = "?";

		PredictionResult predictionResult = predict(corpus);

		prediction = predictionResult.getPredictions().get(0).toString();
		if (predictionResult.getDistributions() != null)
		{
			prediction = prediction + "\t " + (int) (predictionResult.getDistributions().get(prediction).get(0) * 100) + "%";
		}

		return prediction;

	}

	public String predict(String instance)
	{

		DocumentList corpus = null;
		corpus = new DocumentList(instance);
		String prediction = "?";

		PredictionResult predictionResult = predict(corpus);
		prediction = predictionResult.getPredictions().get(0).toString();

		return prediction;

	}

	/**
	 * @throws FileNotFoundException
	 * 
	 */
	protected void loadModel() throws IOException, FileNotFoundException
	{
		recipe = Chef.loadRecipe(modelPath);
	}

	public String getModelPath()
	{
		return modelPath;
	}

	public void setModelPath(String modelPath)
	{
		this.modelPath = modelPath;
	}

	public String getPredictionAnnotation()
	{
		return predictionAnnotation;
	}

	public void setPredictionAnnotation(String predictionAnnotation)
	{
		this.predictionAnnotation = predictionAnnotation;
	}

	public static void main(String[] args) throws Exception
	{
		String modelPath = "saved/bayes.model.side";
		if (args.length < 1 || args.length == 2)
		{
			printUsage();
			System.exit(1);
		}
		else
			modelPath = args[0];

		String annotation = "predicted";

		// to swallow all output except for the classifications
		PrintStream actualOut = System.out;

		try
		{
			String outLogFilename = "predict.log";
			PrintStream logPrintStream = new PrintStream(outLogFilename);
			System.setOut(logPrintStream);
			System.setErr(logPrintStream);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

		try
		{
		logger.info("loading predictor from " + modelPath);
		Predictor predictor = new Predictor(modelPath, annotation);

		if (args.length > 2)
		{
			Set<String> corpusFiles = new HashSet<String>();

			corpusFiles.add(args[2]);
			
			Charset encoding = Charset.forName(args[1]);
			logger.info("loading docs from " + corpusFiles);
			DocumentList docs = ImportController.makeDocumentList(corpusFiles, encoding);

			logger.info("predicting...");
			PredictionResult predicted = predictor.predict(docs);
			
			if(args.length > 3)
			{
				String outputFilename = args[3];
				logger.info("saving prediction results to "+outputFilename);
				docs = Predictor.addPredictionsToDocumentList("predicted", false, false, predicted, docs);
				DocumentListTableModel docTable = new DocumentListTableModel(docs);
				docTable.setDocumentList(docs);
				CSVExporter.exportToCSV(docTable, new File(outputFilename));
			}
			
			else
			{
				List<? extends Comparable<?>> predictions = predicted.getPredictions();
				for (int i = 0; i < docs.getSize(); i++)
				{
					String text = docs.getPrintableTextAt(i);
	//				logger.info(predictions.get(i) + "\t" + text.substring(0, Math.min(100, text.length())));
					actualOut.println(i+"\t"+predictions.get(i) + "\t" + text.substring(0, Math.min(100, text.length())));
				}
			}
		}
		else
		{
			Scanner input = new Scanner(System.in);

			while (input.hasNextLine())
			{
				String sentence = input.nextLine();
				String answer = predictor.prettyPredict(sentence);
				actualOut.println(answer);
//				logger.info(answer + "\t" + sentence.substring(0, Math.min(sentence.length(), 100)));
			}
		}

		System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace(actualOut);
			if(e.getCause() != null)
			{
				actualOut.println("Caused by");
				e.getCause().printStackTrace(actualOut);
			}
		}
	}

	public static void printUsage()
	{
		System.out.println("Usage: ./scripts/predict.sh path/to/saved/model.xml [{data-encoding} path/to/unlabeled/data.csv [path/to/output/file.csv]]");
		System.out.println("Outputs tab-separated predictions for new instances, using the given model.");
		System.out.println("If no new data file is given, instances are read from the standard input.");
		System.out.println("Common data encodings are UTF-8, windows-1252, and MacRoman.");
		System.out.println("Make sure that the text columns and any columns used as features have the same names in the new data as they did in the training set.)");
	}

	public boolean isQuiet()
	{
		return quiet;
	}

	public void setQuiet(boolean quiet)
	{
		this.quiet = quiet;
	}

	public Map<String, Double> getScores(String sample)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public String[] getLabelArray()
	{
		return recipe.getTrainingTable().getLabelArray();
	}

}
