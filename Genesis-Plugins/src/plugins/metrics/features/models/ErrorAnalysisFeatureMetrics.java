package plugins.metrics.features.models;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import plugins.learning.WekaTools;
import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.ChiSquaredAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.FreqMap;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.plugin.ModelFeatureMetricPlugin;
import edu.cmu.side.view.util.DefaultMap;

public class ErrorAnalysisFeatureMetrics extends ModelFeatureMetricPlugin<Double>
{

	// Map Result -> prediction -> actual -> eval -> Feature -> double
	protected Map<TrainingResult, Map<String, Map<String, Map<String, Map<Feature, Double>>>>> evaluations = new HashMap<TrainingResult, Map<String, Map<String, Map<String, Map<Feature, Double>>>>>();
	protected Map<TrainingResult, Map<String, Map<String, Map<Feature, Double>>>> chiSqMatrix = new HashMap<TrainingResult, Map<String, Map<String, Map<Feature, Double>>>>();
	Map<String, Collection<Feature.Type>> metrics = new TreeMap<String, Collection<Feature.Type>>();

	public final static String freq = "Frequency";
	public final static String avg = "Average Cell Value";
	final static String horizAvg = "Average Actual Value";
	final static String horizSum = "Total Actual Value";
	final static String vertAvg = "Average Predicted Value";
	final static String vertSum = "Total Predicted Value";
	final static String plainBayes = "Plain Bayes";
	final static String normalBayes = "Normalized Bayes";
	final static String magicBayes = "Magic Bayes";
	final static String featureSurprise = "Feature Hit Surprise";
	// final static String horizChi = "Horizontal Comparison";
	// final static String vertChi = "Vertical Comparison";
	// final static String chara = "Most Characteristic";

	final static String horizDiff = "Horizontal Difference";
	final static String vertDiff = "Vertical Difference";
	final static String horizDiffAbs = "Horizontal Absolute Difference";
	final static String vertDiffAbs = "Vertical Absolute Difference";

	final static DefaultMap<Feature, Double> defaultMap = new DefaultMap<Feature, Double>(Double.NaN);

	public ErrorAnalysisFeatureMetrics()
	{

		Collection<Feature.Type> general = new HashSet<Feature.Type>();

		general.add(Feature.Type.NOMINAL);
		general.add(Feature.Type.NUMERIC);
		general.add(Feature.Type.BOOLEAN);
		general.add(Feature.Type.STRING);

		Collection<Feature.Type> nominal = new HashSet<Feature.Type>();

		general.add(Feature.Type.NOMINAL);
		general.add(Feature.Type.BOOLEAN);
		general.add(Feature.Type.STRING);

		metrics.put(freq, general);
		metrics.put(avg, general);
		metrics.put(horizDiff, general);
		metrics.put(vertDiff, general);
		metrics.put(horizDiffAbs, general);
		metrics.put(vertDiffAbs, general);
		// metrics.put(vertAvg, general);
		// metrics.put(vertSum, general);
		// metrics.put(horizAvg, general);
		// metrics.put(horizSum, general);
		// metrics.put(plainBayes, general);
//		 metrics.put(normalBayes, nominal);
//		 metrics.put(magicBayes, general);
//		 metrics.put(featureSurprise, general);
		// metrics.put(horizChi, general);
		// metrics.put(vertChi, general);
		// metrics.put(chara, general);

	}

	@Override
	public Map<Feature, Double> evaluateModelFeatures(Recipe recipe, boolean[] mask, String eval, String pred, String act, StatusUpdater update)
	{
		if (act != null && pred != null)
		{
			TrainingResult model = recipe.getTrainingResult();
			initiateMap(model, false);

			try
			{
				Map<String, Map<String, Map<String, Map<Feature, Double>>>> thisEval = evaluations.get(model);
				Map<Feature, Double> thisCell = thisEval.get(pred).get(act).get(avg);
				Map<Feature, Double> horizCell = thisEval.get(act).get(act).get(avg);
				Map<Feature, Double> vertCell = thisEval.get(pred).get(pred).get(avg);
				;

				Map<String, Map<Feature, Double>> differenceEvals = new HashMap<String, Map<Feature, Double>>();
				differenceEvals.put(horizDiff, new HashMap<Feature, Double>());
				differenceEvals.put(vertDiff, new HashMap<Feature, Double>());
				differenceEvals.put(horizDiffAbs, new HashMap<Feature, Double>());
				differenceEvals.put(vertDiffAbs, new HashMap<Feature, Double>());

				Set<Feature> features = model.getTrainingTable().getFeatureSet();
				for (String check : differenceEvals.keySet())
				{
					if (!thisEval.get(pred).get(act).containsKey(check))
					{
						for (Feature f : features)
						{
							Double horizVal = horizCell.get(f) - thisCell.get(f);
							Double vertVal = vertCell.get(f) - thisCell.get(f);
							// (thisCell.get(f) + vertCell.get(f)) -
							// Math.abs(thisCell.get(f) - vertCell.get(f));

							differenceEvals.get(horizDiff).put(f, horizVal);
							differenceEvals.get(vertDiff).put(f, vertVal);
							differenceEvals.get(horizDiffAbs).put(f, Math.abs(horizVal));
							differenceEvals.get(vertDiffAbs).put(f, Math.abs(vertVal));
						}

						for (String key : differenceEvals.keySet())
						{
							thisEval.get(pred).get(act).put(key, differenceEvals.get(key));
						}
					}
				}

				Map<String, Map<String, List<Integer>>> matrix = model.getConfusionMatrix();
				update.update("Calculating row and column values");
				// calculate row and column values
				String[] labels = model.getTrainingTable().getLabelArray();
				if (!thisEval.get(pred).get(act).containsKey(horizAvg))
				{

					for (String actualNow : labels)
					{
						Map<Feature, Double> horizAvgs = new DefaultMap<Feature, Double>(0.0);
						Map<Feature, Double> horizSums = new DefaultMap<Feature, Double>(0.0);

						for (Feature f : features)
						{
							double rowSum = 0;

							double rowTotal = 0;

							for (String predNow : labels)
							{
								Map<Feature, Double> rowCell = thisEval.get(predNow).get(actualNow).get(freq);
								rowSum += rowCell.get(f);
								rowTotal += matrix.get(predNow).get(actualNow).size();
							}
							horizSums.put(f, rowSum);
							horizAvgs.put(f, rowSum / rowTotal);
						}

						for (String predNow : labels)
						{
							thisEval.get(predNow).get(actualNow).put(horizAvg, horizAvgs);
							thisEval.get(predNow).get(actualNow).put(horizSum, horizSums);
						}
					}
				}

				if (metrics.containsKey(normalBayes))
				{
					update.update("Calculating Naive Bayes");

					double totalSize = model.getEvaluationTable().getSize();
					if (!thisEval.get(pred).get(act).containsKey(plainBayes))
					{
						for (String actNow : labels)
						{
							Map<Feature, Double> bayesMap = new HashMap<Feature, Double>();
							Map<String, Double> rowSizes = new DefaultMap<String, Double>(0.0);
							for (String predNow : labels)
								rowSizes.put(predNow, rowSizes.get(predNow) + matrix.get(predNow).get(actNow).size());

							for (Feature f : features)
							{
								double horizSumValue = thisEval.get(actNow).get(actNow).get(horizSum).get(f);
								double classSize = rowSizes.get(actNow);

								// with smoothing!
								double featureProb = (horizSumValue + 1) / (classSize + 2);
								double classProb = classSize / totalSize;
								bayesMap.put(f, featureProb * classProb);

								// bayesMap.put(f, (horizSumValue/classSize) *
								// (classSize/totalSize));
							}

							for (String predNow : labels)
								thisEval.get(predNow).get(actNow).put(plainBayes, bayesMap);
						}

						for (String actNow : labels)
						{
							Map<Feature, Double> normalBayesMap = new HashMap<Feature, Double>();
							Map<Feature, Double> magicBayesMap = new HashMap<Feature, Double>();
							Map<Feature, Double> surpriseMap = new HashMap<Feature, Double>();
							double magicLambda = 0.5;
							for (Feature f : features)
							{
								double bayesSum = 0;
								int totalHits = model.getEvaluationTable().getHitsForFeature(f).size();
								double expectedHits = totalHits / (double) labels.length;
								Double rowTotal = thisEval.get(actNow).get(actNow).get(horizSum).get(f);
								double surprise = (rowTotal - expectedHits) / totalHits;

								for (String predNow : labels)
								{
									bayesSum += thisEval.get(predNow).get(predNow).get(plainBayes).get(f);
								}

								for (String predNow : labels)
								{
									double normalBayesValue = thisEval.get(predNow).get(actNow).get(plainBayes).get(f) / bayesSum;
									double magicBayesValue = 2 * normalBayesValue * surprise * (rowTotal / totalSize);
									normalBayesMap.put(f, normalBayesValue);
									magicBayesMap.put(f, magicBayesValue);
									surpriseMap.put(f, surprise);
								}
							}
							for (String predNow : labels)
							{
								thisEval.get(actNow).get(predNow).put(normalBayes, normalBayesMap);
								thisEval.get(actNow).get(predNow).put(magicBayes, magicBayesMap);
								thisEval.get(actNow).get(predNow).put(featureSurprise, surpriseMap);
							}
						}

					}
				}

				return thisEval.get(pred).get(act).get(eval);
			}
			catch (Exception e)
			{
				return defaultMap;
			}

		}
		else
			return defaultMap;
	}

	// @Override
	// public Map<Feature, Double> evaluateModelFeatures(TrainingResult model,
	// boolean[] mask, String eval, String pred, String act, StatusUpdater
	// update){
	// if(act != null && pred != null){
	// initiateMap(model, false);
	//
	// Map<Feature, Double> thisCell = getChiSqCell(model, pred, act);
	// Map<Feature, Double> horizCell = getChiSqCell(model, act, act);
	// Map<Feature, Double> vertCell = getChiSqCell(model, pred, pred);
	//
	// Map<String, Map<Feature, Double>> chiSqEvals = new HashMap<String,
	// Map<Feature, Double>>();
	// chiSqEvals.put(horizChi, new HashMap<Feature, Double>());
	// chiSqEvals.put(vertChi, new HashMap<Feature, Double>());
	// chiSqEvals.put(chara, new HashMap<Feature, Double>());
	//
	// for (String check : chiSqEvals.keySet())
	// {
	// if (!evaluations.get(model).get(pred).get(act).containsKey(check))
	// {
	// for (Feature f : model.getEvaluationTable().getFeatureSet())
	// {
	// Double horizVal = horizCell.get(f) - thisCell.get(f);
	// Double vertVal = (thisCell.get(f) + vertCell.get(f)) -
	// Math.abs(thisCell.get(f) - vertCell.get(f));
	//
	// chiSqEvals.get(horizChi).put(f, horizVal);
	// chiSqEvals.get(vertChi).put(f, vertVal);
	// chiSqEvals.get(chara).put(f, thisCell.get(f));
	// }
	//
	// for (String key : chiSqEvals.keySet())
	// {
	// evaluations.get(model).get(pred).get(act).put(key, chiSqEvals.get(key));
	// }
	// }
	// }
	//
	//
	// return evaluations.get(model).get(pred).get(act).get(eval);
	//
	//
	// }else return new TreeMap<Feature, Double>();
	// }

	protected Map<Feature, Double> getChiSqCell(TrainingResult model, String pred, String act)
	{
		if (!chiSqMatrix.get(model).containsKey(pred) || !chiSqMatrix.get(model).get(pred).containsKey(act))
		{
			Collection<String> labels = model.getPossibleLabels();
			List<Integer> highlight = new ArrayList<Integer>();
			if (model.getConfusionMatrix().containsKey(pred) && model.getConfusionMatrix().get(pred).containsKey(act))
			{
				highlight = model.getConfusionMatrix().get(pred).get(act);
			}
			else
				System.out.println("ErrorAnalysisFeatures 180: label " + pred + " or " + act + " are not in training result " + model + "'s matrix");

			boolean[] inMask = new boolean[model.getEvaluationTable().getSize()];
			for (int i = 0; i < inMask.length; i++)
			{
				inMask[i] = false;
			}
			for (Integer i : highlight)
			{
				inMask[i] = true;
			}
			boolean[] outMask = new boolean[inMask.length];
			for (int i = 0; i < inMask.length; i++)
			{
				outMask[i] = !inMask[i];
			}
			if (!chiSqMatrix.get(model).containsKey(pred))
			{
				chiSqMatrix.get(model).put(pred, new HashMap<String, Map<Feature, Double>>());
			}
			if (!chiSqMatrix.get(model).get(pred).containsKey(act))
			{
				chiSqMatrix.get(model).get(pred).put(act, calculateSubsetChiSq(model, model.getEvaluationTable(), inMask, outMask));
			}
		}
		return chiSqMatrix.get(model).get(pred).get(act);
	}

	protected void initiateMap(TrainingResult model, boolean force)
	{
		Collection<String> labels = model.getPossibleLabels();
		for (String pred : labels)
		{
			for (String act : labels)
			{
				List<Integer> highlight = new ArrayList<Integer>();
				if (model.getConfusionMatrix().containsKey(pred) && model.getConfusionMatrix().get(pred).containsKey(act))
				{
					highlight = model.getConfusionMatrix().get(pred).get(act);
				}
				else
				{
					System.out.println("ErrorAnalysisFeatures 219: label " + pred + " or " + act + " is not in training result " + model + "'s matrix");
					System.out.println(model.getConfusionMatrix().keySet());
				}

				if (!evaluations.containsKey(model))
				{
					evaluations.put(model, new HashMap<String, Map<String, Map<String, Map<Feature, Double>>>>());
				}
				if (!evaluations.get(model).containsKey(pred))
				{
					evaluations.get(model).put(pred, new HashMap<String, Map<String, Map<Feature, Double>>>());
				}
				if (force || !evaluations.get(model).get(pred).containsKey(act))
				{
					evaluations.get(model).get(pred).put(act, new HashMap<String, Map<Feature, Double>>());
					calculateFrequencyEvaluations(model, evaluations.get(model).get(pred).get(act), highlight);
				}

			}
		}

		if (!chiSqMatrix.containsKey(model))
		{
			Map<String, Map<String, Map<Feature, Double>>> resultMap = new HashMap<String, Map<String, Map<Feature, Double>>>();
			chiSqMatrix.put(model, resultMap);
		}
	}

	protected static void calculateFrequencyEvaluations(TrainingResult model, Map<String, Map<Feature, Double>> map, List<Integer> highlight)
	{
		FreqMap<Feature> freqMap = new FreqMap<Feature>();
		Map<Feature, Double> avgMap = new HashMap<Feature, Double>();
		FeatureTable table = model.getEvaluationTable();

		for (Integer ind : highlight)
		{
			Collection<FeatureHit> hits = table.getHitsForDocument(ind);
			for (FeatureHit hit : hits)
			{
				Feature feat = hit.getFeature();
				Object val = hit.getValue();
				Feature.Type type = feat.getFeatureType();
				freqMap.count(feat);
				switch (type)
				{
					case NUMERIC:
						if (!avgMap.containsKey(feat))
							avgMap.put(feat, (Double) val);
						else
							avgMap.put(feat, avgMap.get(feat) + (Double) val);
						break;
					case NOMINAL:
						avgMap.put(feat, 0.0);
						break;
					case BOOLEAN:
						if (Boolean.TRUE.equals(val))
						{
							if (!avgMap.containsKey(feat))
								avgMap.put(feat, 1.0);
							else
								avgMap.put(feat, avgMap.get(feat) + 1);
						}
						break;
					case STRING:
						break;
				}
			}
		}

		Map<Feature, Double> freqsOut = new HashMap<Feature, Double>();
		for (Feature f : avgMap.keySet())
		{
			freqsOut.put(f, 0.0 + freqMap.get(f));
			double avg = avgMap.get(f) / highlight.size();
			avgMap.put(f, avg);
		}

		for (Feature f : table.getFeatureSet())
		{
			if (!freqsOut.containsKey(f))
			{
				freqsOut.put(f, 0.0);
				avgMap.put(f, 0.0);
			}
		}
		map.put(avg, avgMap);
		map.put(freq, freqsOut);
	}

	private Map<Feature, Double> calculateSubsetChiSq(TrainingResult model, FeatureTable table, boolean[] inMask, boolean[] outMask)
	{
		Instances inCell = WekaTools.getInstances(table, inMask);
		Instances outCell = WekaTools.getInstances(table, outMask);
		Instances comboInst = new Instances(inCell, 0);
		Map<Feature, Double> charaOut = new HashMap<Feature, Double>();

		for (int i = 0; i < inCell.numInstances(); i++)
		{
			Instance inst = (Instance) inCell.instance(i).copy();
			comboInst.add(inst);
		}
		for (int i = 0; i < outCell.numInstances(); i++)
		{
			Instance inst = (Instance) outCell.instance(i).copy();
			comboInst.add(inst);
		}
		if (comboInst.size() > 0)
		{
			FastVector fast = new FastVector(2);
			fast.addElement("out");
			fast.addElement("in");
			Attribute matrix = new Attribute("LightSIDE confusion matrix ingroup", fast);
			comboInst.insertAttributeAt(matrix, comboInst.numAttributes());
			comboInst.setClassIndex(comboInst.numAttributes() - 1);
			for (int i = 0; i < inCell.numInstances(); i++)
			{
				comboInst.instance(i).setValue(comboInst.classIndex(), 1.0);
			}
			for (int i = 0; i < outCell.numInstances(); i++)
			{
				comboInst.instance(inCell.numInstances() + i).setValue(comboInst.classIndex(), 0.0);
			}
			ChiSquaredAttributeEval chisq = new ChiSquaredAttributeEval();
			Ranker rank = new Ranker();
			AttributeSelection select = new AttributeSelection();
			select.setEvaluator(chisq);
			select.setSearch(rank);
			try
			{
				select.SelectAttributes(comboInst);
				double[][] ranks = select.rankedAttributes();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			Map<Feature, Integer> attributeMap = WekaTools.getAttributeMap(table);
			try
			{
				for (Feature f : attributeMap.keySet())
				{
					double val = chisq.evaluateAttribute(attributeMap.get(f));
					charaOut.put(f, val);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			for (Feature f : table.getFeatureSet())
			{
				charaOut.put(f, 0.0);
			}
		}

		return charaOut;
	}

	@Override
	public String toString()
	{
		return "Feature Confusion Ranking";
	}

	@Override
	public String getOutputName()
	{
		return "error";
	}

	@Override
	public Map<String, Collection<Feature.Type>> getAvailableEvaluations()
	{
		return metrics;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		return new TreeMap<String, String>();
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Double targetedFeatureEvaluation(Recipe model, boolean[] mask, String eval, String pred, String act, Feature f, StatusUpdater update)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHighlightedRow()
	{
		return ExploreResultsControl.getHighlightedRow();
	}

	@Override
	public String getHighlightedColumn()
	{
		return ExploreResultsControl.getHighlightedColumn();
	}

	@Override
	public boolean canEvaluateRecipe(Recipe r, String evaluationKey)
	{
		if (evaluationKey.equals(normalBayes)) return r.getTrainingTable().getClassValueType() != Type.NUMERIC;

		return true;
	}

}
