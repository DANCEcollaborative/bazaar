package edu.cmu.side.plugin;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.model.OrderedPluginMap;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.view.util.DefaultMap;
import edu.cmu.side.view.util.ParallelTaskUpdater;
import edu.cmu.side.view.util.ParallelTaskUpdater.Completion;

public abstract class LearningPlugin extends SIDEPlugin implements Serializable
{

	private static final long serialVersionUID = -7928450759075851993L;

	public static String type = "model_builder";

	public static StatusUpdater updater;
	
	public abstract LearningPlugin clone();
	
	protected static DecimalFormat print = new DecimalFormat("#.###");

	public String getType()
	{
		return type;
	}

	public abstract boolean supportsClassType(Feature.Type type);

	public TrainingResult train(FeatureTable table, Map<String, String> configuration, Map<String, Serializable> validationSettings, OrderedPluginMap wrappers,
			StatusUpdater progressIndicator) throws Exception
	{
		synchronized(this)
		{
		halt = false;
		if (table == null) { return null; }
		updater = progressIndicator;
		Map<Integer, Integer> defaultFoldMapZero = new DefaultMap<Integer, Integer>(0);

		this.configureFromSettings(configuration);
		for (SIDEPlugin wrapper : wrappers.keySet())
		{
			((WrapperPlugin) wrapper).configureFromSettings(wrappers.get(wrapper));
		}

		TrainingResult result = null;
		if (Boolean.TRUE.toString().equals(validationSettings.get("test")))
		{

			if (validationSettings.get("type").equals("CV"))
			{

				DocumentList docsForCV = table.getDocumentList();// (DocumentList)validationSettings.get("testSet");
				Map<Integer, Integer> foldsMap = new TreeMap<Integer, Integer>();
				int numFolds = -1;
				try
				{
					numFolds = Integer.parseInt(validationSettings.get("numFolds").toString());
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				progressIndicator.update("Generating Folds Map", 0, 0);
				if (validationSettings.get("source").equals("RANDOM"))
				{
					if (validationSettings.get("foldMethod").equals("AUTO"))
					{
						numFolds = Math.min(10, docsForCV.getSize());
					}
					foldsMap = BuildModelControl.getFoldsMapRandom(docsForCV, numFolds);
				}
				else if (validationSettings.get("source").equals("ANNOTATIONS"))
				{
					String annotation = validationSettings.get("annotation").toString();
					if (validationSettings.get("foldMethod").equals("AUTO"))
					{
						numFolds = docsForCV.getPossibleAnn(annotation).size();
					}
					foldsMap = BuildModelControl.getFoldsMapByAnnotation(docsForCV, annotation, numFolds);
				}
				else if (validationSettings.get("source").equals("FILES"))
				{
					if (validationSettings.get("foldMethod").equals("AUTO"))
					{
						numFolds = docsForCV.getFilenames().size();
					}
					foldsMap = BuildModelControl.getFoldsMapByFile(docsForCV, numFolds);
				}

				result = evaluateCrossValidation(table, foldsMap, wrappers, progressIndicator);

				progressIndicator.update("Training final model on all data");
				FeatureTable wrappedTable = wrapAndTrain(table, wrappers, progressIndicator, defaultFoldMapZero, 1);
			}
			else if (validationSettings.get("type").equals("SUPPLY"))
			{
				progressIndicator.update("Training model");
				FeatureTable pass = wrapAndTrain(table, wrappers, progressIndicator, defaultFoldMapZero, 1);
				progressIndicator.update("Testing model");
				FeatureTable passTest = (FeatureTable) validationSettings.get("testFeatureTable");
				result = evaluateTestSet(pass, passTest, wrappers, progressIndicator);
			}
			else if (validationSettings.get("type").equals("ALBERTA")){
				progressIndicator.update("Training model");
				String key = validationSettings.get("foldColumn").toString();
				List<String> annotations = table.getDocumentListQuickly().getAnnotationArray(key);
				Map<Integer, Integer> albertaFoldsMap = new TreeMap<Integer, Integer>();
				for(int i = 0; i < annotations.size(); i++){
					albertaFoldsMap.put(i, Integer.parseInt(annotations.get(i)));
				}
				FeatureTable albertaTrain = table.cloneTrainingFold(albertaFoldsMap, 0, true);
				logger.info("************************ Training set has " + albertaTrain.getSize() + " instances.");
				FeatureTable pass = wrapAndTrain(albertaTrain, wrappers, progressIndicator, defaultFoldMapZero, 1);

				progressIndicator.update("Testing model");
				FeatureTable albertaTest = table.cloneTrainingFold(albertaFoldsMap, 0, false);
				logger.info("************************ Test set has " + albertaTest.getSize() + " instances.");
				result = evaluateTestSet(pass, albertaTest, wrappers, progressIndicator);
			}
		}
		else
		{
			logger.info("Skipping evaluation -- validationSettings['test'] != 'true'");
			FeatureTable wrappedTable = wrapAndTrain(table, wrappers, progressIndicator, defaultFoldMapZero, 1);
			List<Comparable<Comparable>> blankPredictions = new ArrayList<Comparable<Comparable>>();
			Map<String, List<Double>> blankDistributions = new HashMap<String, List<Double>>();
			switch (wrappedTable.getClassValueType())
			{
				case NOMINAL:
				case BOOLEAN:
				case STRING:
					String[] labelArray = wrappedTable.getLabelArray();
					for (int i = 0; i < wrappedTable.getSize(); i++)
					{
						for (String label : labelArray)
						{
							if (!blankDistributions.containsKey(label))
							{
								blankDistributions.put(label, new ArrayList<Double>(wrappedTable.getSize()));
							}
							blankDistributions.get(label).add(1.0 / labelArray.length);
						}
						blankPredictions.add((Comparable) labelArray[0]);
					}
					break;
				case NUMERIC:
					for (int i = 0; i < wrappedTable.getSize(); i++)
					{
						blankPredictions.add((Comparable) new Double(0.0));
					}
					break;
			}
			result = new TrainingResult(wrappedTable, blankPredictions);
			result.setDistributions(blankDistributions);
		}
		result.setLongDescriptionString(getLongDescriptionString());
		return result;
		}
	}

	/**
	 * @param wrappers
	 * @param progressIndicator
	 * @param defaultFoldMapZero
	 * @param pass
	 * @param passTest
	 * @return
	 */
	// protected TrainingResult wrapAndTest(OrderedPluginMap wrappers,
	// StatusUpdater progressIndicator, Map<Integer, Integer>
	// defaultFoldMapZero,
	// FeatureTable pass, FeatureTable passTest)
	// {
	// TrainingResult result = evaluateTestSet(pass, passTest, wrappers,
	// progressIndicator);
	// return result;
	// }

	/**
	 * @param table
	 * @param wrappers
	 * @param progressIndicator
	 * @param foldMap
	 * @return
	 * @throws Exception
	 */
	public FeatureTable wrapAndTrain(FeatureTable table, OrderedPluginMap wrappers, StatusUpdater progressIndicator, Map<Integer, Integer> foldMap, int fold)
			throws Exception
	{
		FeatureTable wrapped = wrapTableBefore(table, fold, foldMap, progressIndicator, wrappers, true);
		prepareAndTrainAgainstFold(wrapped, fold, foldMap, progressIndicator);
		return wrapped;
	}

	public PredictionResult validateFold(int fold, FeatureTable table, Map<Integer, Integer> foldsMap, int numFolds, OrderedPluginMap wrappers,  StatusUpdater progressIndicator) throws Exception
	{		
		if (fold < 0)
		{
			System.err.println("LP 178: negative fold: " + fold + "!");
			throw new Exception("Invalid Fold Number");
		}
		if (halt)
		{
			throw new Exception("User Canceled");
		}

		
		FeatureTable wrappedTrain = table;

		wrappedTrain = wrapAndTrain(table, wrappers, progressIndicator, foldsMap, fold);

		if (halt)
		{
			throw new Exception("User Canceled");
		}
		
		if(progressIndicator instanceof ParallelTaskUpdater)
		{
			((ParallelTaskUpdater)progressIndicator).updateCompletion("Testing fold", fold, Completion.PROGRESS);
		} 
		else
		{
			progressIndicator.update("Testing fold", (fold + 1), numFolds);
		}

		// TODO: verify that passing the *unwrapped* table on to predict (as
		// the test set) is the right thing to do - it was wrappedTrain
		// before
		PredictionResult predictionResult = predictOnFold(wrappedTrain, table, fold, foldsMap, updater, wrappers);

//		double timeB = System.currentTimeMillis();
//		times.add((timeB - timeA) / 1000.0);
		return predictionResult;
	}
	
	public void aggregateFoldEvaluation(int fold, PredictionResult predictionResult, Map<Integer, Integer> foldsMap, FeatureTable table, Map<String, List<Double>> distributions, Comparable[] predictions)
	{
		List<? extends Comparable<?>> predictionsList = predictionResult.getPredictions();

		List<Comparable<?>> foldPredicted = new ArrayList<Comparable<?>>(500);
		List<String> foldActual = new ArrayList<String>(500);

		// int predictionIndex = 0;
//		double correct = 0;
//		double total = 0;

		Map<String, List<Double>> predictedDistros = predictionResult.getDistributions();
		if (distributions.isEmpty() && predictedDistros != null) distributions.putAll(predictedDistros);

		String[] labelArray = table.getLabelArray();
		
		for (int i = 0; i < predictionsList.size(); i++)
		{
			if (foldsMap.get(i).equals(fold))
			{
				if (predictedDistros != null) // numeric classifiers don't
												// produce distributions
					for (String label : labelArray)
					{
						Double scoreForLabel;
						if (i >= predictedDistros.get(label).size())
						{
							logger.info("LP 273: prediction result distribution size does not match label/doc array size: " + i + " >= "
									+ predictedDistros.get(label).size());
							scoreForLabel = 0.0;
							while (i >= predictedDistros.get(label).size())
								distributions.get(label).add(scoreForLabel);
						}
						else
							scoreForLabel = predictedDistros.get(label).get(i);
						distributions.get(label).set(i, scoreForLabel);
					}

				predictions[i] = predictionsList.get(i);
				foldPredicted.add(predictions[i]);
				foldActual.add(table.getAnnotations().get(i));

//				if (predictions[i].equals(table.getAnnotations().get(i))) correct++;
//				total++;
			}
			// predictionIndex++;
		}
		// logger.info("accuracy for fold #"+fold+": "+(100*correct/total)+"%");
		// if(table.getClassValueType() != Type.NUMERIC)
		// {
		// String evaluation = EvaluationUtils.evaluate(foldActual,
		// foldPredicted, labelArray,
		// BuildModelControl.getNewName()+".fold"+fold+".eval");
		// logger.info(evaluation);
		// if(out != null) out.println(evaluation);
		// }
	}
	
	public TrainingResult
			evaluateCrossValidation(FeatureTable table, Map<Integer, Integer> foldsMap, OrderedPluginMap wrappers, StatusUpdater progressIndicator)
					throws Exception
	{
		DocumentList localDocuments = table.getDocumentList();

		Set<Integer> folds = new TreeSet<Integer>();
		for (Integer key : foldsMap.keySet())
		{
			folds.add(foldsMap.get(key));
		}

		int numFolds = folds.size();
		List<PredictionResult> results = doCrossValidation(table, foldsMap, folds, wrappers, progressIndicator);

		Comparable[] predictions = new Comparable[localDocuments.getSize()];
		Map<String, List<Double>> distributions = new HashMap<String, List<Double>>();
		
		for(int fold = 0; fold < numFolds; fold++)
		{
			PredictionResult result = results.get(fold);
			aggregateFoldEvaluation(fold, result, foldsMap, table, distributions, predictions);
		}
		

		if (!halt)
		{
			progressIndicator.update("Generating confusion matrix");
			List<Comparable<Comparable>> predictionsList = new ArrayList<Comparable<Comparable>>();

			PredictionResult pred = new PredictionResult(predictionsList, distributions);
			for (Comparable s : predictions)
				predictionsList.add(s);
			return new TrainingResult(table, pred);
		}
		else
		{
			for (SIDEPlugin wrapper : wrappers.keySet())
			{
				wrapper.stopWhenPossible();
			}

			throw new Exception("User Canceled");
		}
	}

	public List<PredictionResult> doCrossValidation(FeatureTable table, Map<Integer, Integer> foldsMap, Set<Integer> folds, OrderedPluginMap wrappers,
			StatusUpdater progressIndicator) throws Exception
	{
//		ArrayList<Double> times = new ArrayList<Double>(folds.size());
		List<PredictionResult> results = new ArrayList<PredictionResult>();

		for(Integer fold : folds)
		{
			PredictionResult result = validateFold(fold, table, foldsMap, folds.size(), wrappers, progressIndicator);
			results.add(result);
		}
		return results;
	}

	public TrainingResult evaluateTestSet(FeatureTable wrappedTrain, FeatureTable testSet, OrderedPluginMap wrappers, StatusUpdater updater) throws Exception
	{
		DefaultMap<Integer, Integer> defaultFoldMap = new DefaultMap<Integer, Integer>(0);
		PredictionResult predictions = predictOnFold(wrappedTrain, testSet, 0, defaultFoldMap, updater, wrappers);
		TrainingResult training = new TrainingResult(wrappedTrain, testSet, predictions);
		return training;
	}

	public void prepareAndTrainAgainstFold(FeatureTable table, int fold, Map<Integer, Integer> foldsMap, StatusUpdater progressIndicator) throws Exception
	{
		for (int i = 0; i < table.getSize(); i++)
		{
			if (!foldsMap.get(i).equals(fold))
			{
				for (FeatureHit hit : table.getHitsForDocument(i))
				{
					hit.prepareForTraining(fold, foldsMap, table);
				}
			}
		}
		trainAgainstFold(table, fold, foldsMap, progressIndicator);
	}

	protected abstract void trainAgainstFold(FeatureTable table, int fold, Map<Integer, Integer> foldsMap, StatusUpdater progressIndicator) throws Exception;

	public PredictionResult predict(FeatureTable originalData, FeatureTable newData, Map<String, String> configuration, StatusUpdater progressIndicator,
			OrderedPluginMap wrappers) throws Exception
	{
		synchronized(this)
		{
//			logger.info("LP 344: wrap original table (for reconciliation)");
			FeatureTable wrappedTrain = wrapTableBefore(originalData, 0, new DefaultMap<Integer, Integer>(0), progressIndicator, wrappers, false);
			this.loadClassifierFromSettings(configuration);
			return predictOnFold(wrappedTrain, newData, 0, new DefaultMap<Integer, Integer>(0), progressIndicator, wrappers);
		}
	}

	public PredictionResult predictOnFold(FeatureTable originalData, FeatureTable newData, int fold, Map<Integer, Integer> foldsMap,
			StatusUpdater progressIndicator, OrderedPluginMap wrappers) throws Exception
	{
//		logger.info("LP 353: wrap new table (for classification)");
		newData = wrapTableBefore(newData, fold, foldsMap, progressIndicator, wrappers, false);

		Object predictionContext = prepareToPredict(originalData, newData, fold, foldsMap);

		PredictionResult prediction = actuallyPredict(originalData, newData, fold, foldsMap, predictionContext);

		prediction = wrapTableAfter(fold, foldsMap, wrappers, newData, prediction);

		return prediction;

	}

	/**
	 * @param fold
	 * @param foldsMap
	 * @param wrappers
	 * @param prediction
	 * @return
	 */
	public PredictionResult
			wrapTableAfter(int fold, Map<Integer, Integer> foldsMap, OrderedPluginMap wrappers, FeatureTable table, PredictionResult prediction)
	{
		for (SIDEPlugin wrapper : wrappers.keySet())
		{
			// logger.info("LP 382: wrapping prediction result with "+wrapper
			// + ": "+wrappers.get(wrapper));
			prediction = ((WrapperPlugin) wrapper).wrapResultAfter(prediction, table, fold, foldsMap, updater);
		}
		return prediction;
	}

	/**
	 * @param originalData
	 * @param newData
	 * @param fold
	 * @param foldsMap
	 * @param predictionContext
	 * @param prediction
	 * @return
	 */
	public PredictionResult
			actuallyPredict(FeatureTable originalData, FeatureTable newData, int fold, Map<Integer, Integer> foldsMap, Object predictionContext)
					throws Exception
	{
		PredictionResult prediction = null;
		switch (originalData.getClassValueType())
		{
			case NUMERIC:
				List<Double> numericPredictions = new ArrayList<Double>();
				for (int i = 0; i < newData.getSize(); i++)
				{
					if (foldsMap.get(i).equals(fold))
					{
						for (FeatureHit hit : newData.getHitsForDocument(i))
						{
							hit.prepareToPredict(fold, foldsMap, newData, numericPredictions);
						}

						// numericPredictions.add(classifier.classifyInstance(instance));
						numericPredictions.add(predictNumeric(i, originalData, newData, predictionContext));

					}
					else
						numericPredictions.add(0.0);
				}
				prediction = new PredictionResult(numericPredictions);
				break;
			case NOMINAL:
			case BOOLEAN:
				List<String> predictions = new ArrayList<String>();
				Map<String, List<Double>> distributions = new HashMap<String, List<Double>>();

				String[] labelArray = originalData.getLabelArray();
				for (String possible : labelArray)
				{
					distributions.put(possible, new ArrayList<Double>());
				}

				for (int i = 0; i < newData.getSize(); i++)
				{
					// Instance instance = inst.instance(i);
					// double[] distro =
					// classifier.distributionForInstance(instance);

					double[] distro;
					if (foldsMap.get(i).equals(fold))
					{
						for (FeatureHit hit : newData.getHitsForDocument(i))
						{
							hit.prepareToPredict(fold, foldsMap, newData, predictions);
						}

						distro = predictLabel(i, originalData, newData, predictionContext);
					}
					else
					{
						distro = new double[labelArray.length];
					}

					int index = -1;
					double max = Double.NEGATIVE_INFINITY;
					for (int j = 0; j < distro.length; j++)
					{
						if (distro[j] > max)
						{
							max = distro[j];
							index = j;
						}
						distributions.get(labelArray[j]).add(distro[j]);
					}
					predictions.add(labelArray[index]);
				}

				prediction = new PredictionResult(predictions, distributions);

		}
		return prediction;
	}

	/**
	 * @param newData
	 * @param fold
	 * @param foldsMap
	 * @param progressIndicator
	 * @param wrappers
	 * @return
	 */
	public FeatureTable wrapTableBefore(FeatureTable newData, int fold, Map<Integer, Integer> foldsMap, StatusUpdater progressIndicator,
			OrderedPluginMap wrappers, boolean learn)
	{
		for (SIDEPlugin wrapper : wrappers.keySet())
		{
			wrapper.configureFromSettings(wrappers.get(wrapper));
			if (learn)
			{
				((WrapperPlugin) wrapper).learnFromTrainingData(newData, fold, foldsMap, progressIndicator);
				//whatever the wrapper has learned is stored in configuration settings.
				wrappers.put(wrapper, wrapper.generateConfigurationSettings());
			}

			newData = ((WrapperPlugin) wrapper).wrapTableBefore(newData, fold, foldsMap, progressIndicator);
		}
		return newData;
	}

	/**
	 * predict the nominal/boolean class value of instance i in newData
	 * 
	 * @param i
	 * @param originalData
	 * @param newData
	 * @param mask
	 * @param predictionContext
	 * @return a label distribution
	 */
	public abstract double[] predictLabel(int i, FeatureTable originalData, FeatureTable newData, Object predictionContext) throws Exception;

	/**
	 * predict the numeric value of instance i in newData
	 * 
	 * @param i
	 * @param originalData
	 * @param newData
	 * @param mask
	 * @param predictionContext
	 * @return the predicted value for this instance
	 */
	public abstract double predictNumeric(int i, FeatureTable originalData, FeatureTable newData, Object predictionContext) throws Exception;

	/**
	 * do any setup before prediction, and keep the context of that setup in the
	 * returned object
	 * 
	 * @param originalData
	 *            - a reference to the training data
	 * @param newData
	 *            - the data to predict on
	 * @param mask
	 *            - a mask of the document instances that are actually being
	 *            classified on - does anybody use this?
	 * @param progressIndicator
	 * @return a context object that the subclass may use to inform its
	 *         per-document predictions
	 */
	public abstract Object prepareToPredict(FeatureTable originalData, FeatureTable newData, int i, Map<Integer, Integer> foldsMap);

	@Override
	public void stopWhenPossible(){
		halt = true;
	}

	public abstract String getLongDescriptionString();

	public abstract void loadClassifierFromSettings(Map<String, String> settings);

}
