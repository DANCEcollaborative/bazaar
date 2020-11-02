package plugins.metrics.features.models;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import plugins.learning.WekaCore;
import plugins.wrappers.FeatureSelection;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.Kernel;
import weka.classifiers.functions.supportVector.PolyKernel;
import de.bwaldvogel.liblinear.Model;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.LearningPlugin;
import edu.cmu.side.plugin.ModelFeatureMetricPlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.plugin.WrapperPlugin;
import edu.cmu.side.recipe.Predictor;
import edu.cmu.side.view.util.DefaultMap;

public class TrainedModelFeatureEvaluations extends ModelFeatureMetricPlugin<Double>
{
	// Map Result -> prediction -> actual -> eval -> Feature -> double
	protected static Map<Recipe, Map<String, Map<String, Map<String, Map<Feature, Double>>>>> evaluations = new HashMap<Recipe, Map<String, Map<String, Map<String, Map<Feature, Double>>>>>();

	Map<String, Collection<Feature.Type>> metrics = new TreeMap<String, Collection<Feature.Type>>();
	Map<Feature, Double> defaultMap = new DefaultMap<Feature, Double>(Double.NaN);

	public final static String influenceKey = "Feature Influence";
	public final static String featureWeightKey = "Feature Weight";
	public final static String featureSelectionKey = "Feature Selection";
//	public final static String modelImpactKey = "Feature Impact";

	public TrainedModelFeatureEvaluations()
	{

		Collection<Feature.Type> general = new HashSet<Feature.Type>();

		general.add(Feature.Type.NOMINAL);
		general.add(Feature.Type.BOOLEAN);
		general.add(Feature.Type.STRING);

		metrics.put(influenceKey, general);
		metrics.put(featureWeightKey, general);
		metrics.put(featureSelectionKey, general);
//		metrics.put(modelImpactKey, general);
	}

	protected synchronized void initiateMap(Recipe recipe, boolean force, StatusUpdater updater)
	{
		TrainingResult model = recipe.getTrainingResult();
		String[] labels = model.getTrainingTable().getLabelArray();
		
		if (!evaluations.containsKey(recipe))
		{
			Map<String, Map<String, Map<String, Map<Feature, Double>>>> recipeEvalCache = new HashMap<String, Map<String, Map<String, Map<Feature, Double>>>>();
			Map<String, Map<String, Map<Feature, Double>>> featureWeights = null;
			Map<String, Map<Feature, Double>> influence = null;
//			Map<String, Map<String, Map<Feature, Double>>> featureImpact = null;
			
			Map<Feature, Double> selectionMap = getFeatureSelectionMap(recipe, updater);

			LearningPlugin learner = recipe.getLearner();
			if (learnerMightHaveLinearWeights(recipe))
			{
				try
				{
					featureWeights = getModelWeights(recipe, labels, updater);
//					featureImpact = calculateFeatureImpact(recipe, labels, svmWeights, updater);
				}
				catch(Exception e)
				{
					Map<String, String> settings = learner.generateConfigurationSettings();
					if(settings.containsKey("classifier"))
						settings.remove("classifier");
					System.err.println("TrainedModelFeatures 87: Could not evaluate model weights for "+learner.getClass().getSimpleName() + ":\n"+settings);
					e.printStackTrace();
				}
			}

			if(recipe.getClassValueType() != Type.NUMERIC) try
			{
				influence = evaluateInfluence(recipe, labels, updater);
			}
			catch(Exception e)
			{
				Map<String, String> settings = learner.generateConfigurationSettings();
				if(settings.containsKey("classifier"))
					settings.remove("classifier");
				System.err.println("TrainedModelFeatures 101: Could not evaluate influence for "+learner.getClass().getSimpleName() + ":\n"+settings);
				e.printStackTrace();
			}

			for (String pred : labels)
			{

				for (String act : labels)
				{
					// List<Integer> highlight = new ArrayList<Integer>();
					// if (model.getConfusionMatrix().containsKey(pred) &&
					// model.getConfusionMatrix().get(pred).containsKey(act))
					// {
					// highlight =
					// model.getConfusionMatrix().get(pred).get(act);
					// }
					if (!recipeEvalCache.containsKey(pred))
					{
						recipeEvalCache.put(pred, new HashMap<String, Map<String, Map<Feature, Double>>>());
					}

					if (force || !recipeEvalCache.get(pred).containsKey(act))
					{
						Map<String, Map<Feature, Double>> evaluationMap = new HashMap<String, Map<Feature, Double>>();
						if (influence != null)
						{
							evaluationMap.put(influenceKey, influence.get(pred));
						}
						if (featureWeights != null)
						{

//							evaluationMap.put(modelImpactKey, featureImpact.get(pred).get(act));
							
							if (featureWeights.containsKey(pred) && featureWeights.get(pred).containsKey(act))
								evaluationMap.put(featureWeightKey, featureWeights.get(pred).get(act));
							else
								evaluationMap.put(featureWeightKey, defaultMap);

						}
						if(!selectionMap.isEmpty())
						{
							evaluationMap.put(featureSelectionKey, selectionMap);
						}

						recipeEvalCache.get(pred).put(act, evaluationMap);
					}
				}

			}
			evaluations.put(recipe, recipeEvalCache);
		}
	}

	public Map<Feature, Double> getFeatureSelectionMap(Recipe recipe, StatusUpdater update)
	{
		Map<Feature, Double> evalMap = new DefaultMap<Feature, Double>(0.0);
		
		for(SIDEPlugin plug : recipe.getWrappers().keySet())
		{
			if(plug instanceof FeatureSelection)
			{
				plug.configureFromSettings(recipe.getWrappers().get(plug));
				FeatureSelection selector = (FeatureSelection)plug;
				
				for(Feature f : selector.getSelectedFeatures())
				{
					evalMap.put(f, 1.0);
				}
			}
		}
		
		return evalMap;
	}
	
	protected boolean learnerMightHaveLinearWeights(Recipe recipe)
	{
	
		if(recipe.getLearner() instanceof WekaCore)
		{
			WekaCore learner = (WekaCore) recipe.getLearner();
			learner.loadClassifierFromSettings(recipe.getLearnerSettings());
			Classifier classifier = learner.getClassifier();
			
			if(classifier instanceof SMO)
			{
				SMO smo = (SMO) classifier;
				Kernel kernel = smo.getKernel();
				if(kernel instanceof PolyKernel && ((PolyKernel)kernel).getExponent() == 1.0)
				{
					return true;
				}
			}
			else if(classifier instanceof LibLINEAR)
			{
//				LibLINEAR linear = (LibLINEAR) classifier;
				return true;
			}
			else if(classifier instanceof LinearRegression)
			{
				LinearRegression regression = (LinearRegression)classifier;
				return true;
			}
		}
			
		return false;
	}

	private Map<String, Map<String, Map<Feature, Double>>> calculateFeatureImpact(Recipe recipe, String[] labels, Map<String, Map<String, Map<Feature, Double>>> svmWeights, StatusUpdater updater)
	{
		Map<String, Map<String, Map<Feature, Double>>> impacts = new HashMap<String, Map<String, Map<Feature, Double>>>();
		
		for (String labelA : labels)
		{
			impacts.put(labelA, new HashMap<String, Map<Feature, Double>>());
			for (String labelB : labels)
			{
				impacts.get(labelA).put(labelB, new DefaultMap<Feature, Double>(0.0));
			}
		}
		
		for (int i = 0; i < labels.length; i++)
		{
			String iLabel = labels[i];

			for (int j = 0; j < labels.length; j++)
			{
				String jLabel = labels[j];

				// System.out.println(iLabel+" vs "+jLabel);

				TrainingResult trainingResult = recipe.getTrainingResult();
				FeatureTable evaluationTable = trainingResult.getEvaluationTable();
				List<String> actualLabels = evaluationTable.getAnnotations();
				List<? extends Comparable<?>> predictedLabels = trainingResult.getPredictions();
				
				for (Feature feature : evaluationTable.getFeatureSet())
				{
					double impact = 0.0;

					for(FeatureHit hit : evaluationTable.getHitsForFeature(feature))
					{
						Comparable<?> actualLabel = actualLabels.get(hit.getDocumentIndex());
						Comparable<?> predictedLabel = predictedLabels.get(hit.getDocumentIndex());
						if(actualLabel.equals(jLabel) && predictedLabel.equals(iLabel))
						{
							double value = 1.0;
							Object objectValue = hit.getValue();
							if(objectValue instanceof Double)
							{
								value = (Double)objectValue;
							}
							else if(objectValue instanceof Boolean)
							{
								value = (Boolean)objectValue == true ? 1.0 : 0.0;
							}
							
							impact += value * svmWeights.get(iLabel).get(jLabel).get(feature);
						}
					}	
					
					impact /= evaluationTable.getSize();
					impact *= 100;
					
					impacts.get(iLabel).get(jLabel).put(feature, impact);
				}
			}
		}
		
		return impacts;
	}

	private Map<String, Map<String, Map<Feature, Double>>> getModelWeights(Recipe recipe, String[] labels, StatusUpdater updater)
	{
		Map<String, Map<String, Map<Feature, Double>>> weights = new HashMap<String, Map<String, Map<Feature, Double>>>();

		WekaCore learner = (WekaCore) recipe.getLearner();
		learner.loadClassifierFromSettings(recipe.getLearnerSettings());

		Classifier classifier = learner.getClassifier();
		
		Feature[] features = getWrappedFeaturesFromTable(recipe, updater);
		
		if (classifier instanceof SMO && ((PolyKernel) (((SMO) classifier).getKernel())).getExponent() == 1.0)
		{
			SMO smore = (SMO) classifier;

			int[][][] featureIndex = smore.sparseIndices();
			double[][][] featureWeights = smore.sparseWeights();
			String[][][] attributeNames = smore.attributeNames();

			
			
			for (String labelA : labels)
			{
				weights.put(labelA, new HashMap<String, Map<Feature, Double>>());
				for (String labelB : labels)
				{
					weights.get(labelA).put(labelB, new DefaultMap<Feature, Double>(Double.NaN));
				}
			}

			for (int i = 0; i < labels.length; i++)
			{
				String iLabel = labels[i];

				for (int j = i + 1; j < labels.length; j++)
				{
					String jLabel = labels[j];

					updater.update("Getting SMO weight for "+iLabel+" vs "+jLabel);
					//System.out.println("Getting SMO weight for "+iLabel+" vs "+jLabel);
					
					Map<String, Integer> featureWeightIndices = new HashMap<String, Integer>();
					
					for (int k = 0; k < featureIndex[i][j].length; k++)
					{
						String attributeName = attributeNames[i][j][k];
						featureWeightIndices.put(attributeName, k);
					}
						
					for(Feature feat : features)
					{
						//featureIndex[i][j][k] is the internal, nominal-exploded-to-boolean feature index


						String attributeName = feat.getFeatureName();
						if(featureWeightIndices.containsKey(attributeName))
						{
							double weight = featureWeights[i][j][featureWeightIndices.get(attributeName)];

							//System.out.println(attributeName+": "+weight);
							weights.get(iLabel).get(jLabel).put(feat, -weight);
							weights.get(jLabel).get(iLabel).put(feat, weight);
						}
						//else System.out.println(attributeName+": Nobody Home");
					}
				}
			}

		}

		else if (classifier instanceof LibLINEAR)
		{
			LibLINEAR liner = (LibLINEAR) classifier;

			Model model = liner.getModel();
			int numClasses = model.getNrClass();
			int numFeatures = model.getNrFeature();
			int[] modelLabels = model.getLabels();
			double[] featureWeights = model.getFeatureWeights();

//			System.out.println(modelLabels.length + " classes " + Arrays.toString(modelLabels));
//			System.out.println(numFeatures + " features in LibLINEAR table vs "+features.length + " in LightSide");
//			System.out.println(featureWeights.length + " weights");
//			System.out.println(model.getBias() + " = bias");

			for (String labelA : labels)
			{
				weights.put(labelA, new HashMap<String, Map<Feature, Double>>());
				for (String labelB : labels)
				{
					weights.get(labelA).put(labelB, new DefaultMap<Feature, Double>(Double.NaN));
				}
			}

			for (int i = 0; i < numClasses; i++)
			{
				String weightLabel = labels[modelLabels[i]];
				updater.update("Getting LibLINEAR weight for "+weightLabel);

				//actual FeatureTable feature index - "j" is the internal, possibly-expanded index used by LibLinear
				int featureIndex = 0;
				for (int j = 0; j < numFeatures-1; j++)
				{
					int modelIndex = i + numClasses * j;
					Feature feature = features[featureIndex];
					double weight;

					//capture nominal expansions
					if(feature.getFeatureType() == Type.NOMINAL && feature.getNominalValues().size() > 2)
					{
						Collection<String> values = feature.getNominalValues();
						for(String value : values)
						{
//							System.out.println(j+":\t"+featureWeights[j]+"\t"+feature+"="+value);
							j++;
						}
						j--; //offset the j++ at the end of the loop 
						featureIndex ++;
					}
					else
					{
						if (numClasses > 2)
						{
							weight = featureWeights[modelIndex];
						}
						else
						{
							weight = featureWeights[j];
							if (i > 0) weight = -weight;
						}

//						System.out.println(j+":\t"+featureWeights[j]+"\t"+feature);
						
						// System.out.println(feature+"("+weightLabel+"):\t"+weight);
	
						for (String label : labels)
						{
							weights.get(weightLabel).get(label).put(feature, weight);
						}
						featureIndex ++;
					}
				}
			}
		}
		else if(classifier instanceof LinearRegression)
		{
			double[] coefficients = ((LinearRegression)classifier).coefficients();
			
			Map<Feature, Double> weightWeightDontTellMe = new DefaultMap<Feature, Double>(Double.NaN);
			
			if(features.length != coefficients.length - 2)
			{
				System.out.println("TrainedModelFeatureEvaluation 436: expected "+features.length+" (+2) coefficients, found "+coefficients.length);
			}
			for(int i = 0; i < features.length; i++)
			{
				weightWeightDontTellMe.put(features[i], coefficients[i]);
			}
			

			for (String labelA : labels)
			{
				weights.put(labelA, new HashMap<String, Map<Feature, Double>>());
				for (String labelB : labels)
				{
					weights.get(labelA).put(labelB, weightWeightDontTellMe);
				}
			}
		}
		else
		{
			System.out.println("Don't know what to do with "+classifier);
		}

		return weights;
	}

	protected Feature[] getWrappedFeaturesFromTable(Recipe recipe, StatusUpdater updater)
	{
		FeatureTable trainingTable = recipe.getTrainingTable();
		for(SIDEPlugin plug : recipe.getWrappers().keySet())
		{
			plug.configureFromSettings(recipe.getWrappers().get(plug));
			
			WrapperPlugin wrapper = (WrapperPlugin)plug;
			trainingTable = wrapper.wrapTableBefore(trainingTable, 0, new DefaultMap<Integer, Integer>(0), updater);
		}
		
		Feature[] features = trainingTable.getFeatureSet().toArray(new Feature[0]);
		return features;
	}

	protected Map<String, Map<Feature, Double>> evaluateInfluence(Recipe trainedRecipe, String[] labels, StatusUpdater updater) throws Exception
	{
		Map<String, Map<Feature, Double>> influences = new HashMap<String, Map<Feature, Double>>();

		updater.update("Building hypothetical instances for influence");

		Feature[] features = getWrappedFeaturesFromTable(trainedRecipe, updater);
		updater.update("Building influence table for "+features.length+" features");
		FeatureTable evalTable = this.makeEvaluationTable(trainedRecipe.getTrainingTable(), updater, features);

		Predictor predictor = null;
		System.out.println("Evaluating "+features.length+" features for influence.");
		predictor = new Predictor(trainedRecipe, "evaluated_class");

		PredictionResult predictionResult = predictor.predictFromTable(evalTable);

		SummaryStatistics allStats = new SummaryStatistics();
		for (String pred : labels)
		{

			updater.update("Evaluating instances for "+pred);
			System.out.println("Evaluating instances for "+pred);
			allStats.clear();
			influences.put(pred, new DefaultMap<Feature, Double>(0.0));

			List<Double> distribution = predictionResult.getDistributions().get(pred);
			for (double score : distribution)
			{
				allStats.addValue(score);
			}
//			double mean = allStats.getMean();
			double baseline = distribution.get(distribution.size() - 1);
			double stddev = allStats.getStandardDeviation();
			double maxDelta = Math.max(allStats.getMax() - baseline, baseline - allStats.getMin());

			//Iterator<Feature> fiterator = evalTable.getFeatureSet().iterator();
			//for (int i = 0; fiterator.hasNext(); i++)
			for(int i = 0; i < features.length; i++)
			{
				if (i % 100 == 0 || i == evalTable.getSize()) updater.update("Evaluating feature", i + 1, evalTable.getSize());

				//Feature fit = fiterator.next();
				Feature fit = features[i];
				double value;
				
				if (stddev == 0) 
				{
					value = maxDelta == 0? 0 : 10*(distribution.get(i) - baseline) / maxDelta;
//					System.out.println("Backoff Influence for "+fit+": "+value+" = 10*("+distribution.get(i)+" - "+baseline+") / "+maxDelta);	
				}
				else
				{
					value = (distribution.get(i) - baseline) / stddev;
//					System.out.println("Influence for "+fit+": "+value+" = ("+distribution.get(i)+" - "+baseline+") / "+stddev);	
				}

				influences.get(pred).put(fit, value);
			}

		}

		updater.update("Finishing influence evaluation");
		return influences;

	}

	/**
	 * make a one-hit-per-document, one feature-per-document feature table to do
	 * magic on.
	 * 
	 * @param table
	 *            the original table
	 * @return
	 */
	private FeatureTable makeEvaluationTable(FeatureTable original, StatusUpdater update, Feature... features)
	{
		int numFeatures = features.length;//table.getFeatureSet().size();
		List<String> dummies = new ArrayList<String>(numFeatures + 1);
		List<String> dummyClass = new ArrayList<String>(numFeatures + 1);
		Collection<FeatureHit> soloHits = new ArrayList<FeatureHit>(numFeatures);
		String annotation = original.getAnnotation();
		List<String> allAnnotations = original.getDocumentList().getAnnotationArray(annotation);
		Map<String, List<String>> dummyColumns = new HashMap<String, List<String>>();
		dummyColumns.put(annotation, dummyClass);
		
		for (int i = 0; i < numFeatures; i++)//table.getFeatureSet())
		{
			if(i+1 % 100 == 0 || i+1 == numFeatures)
				update.update("Adding Hypothetical Instance", (i+1), numFeatures);
			
			Feature f = features[i];
			if (f.getFeatureType() == Type.BOOLEAN || f.getFeatureType() == Type.NUMERIC)
			{
				FeatureHit hit = new FeatureHit(f, f.getFeatureType() == Type.BOOLEAN ? true : 1.0, dummies.size());
				soloHits.add(hit);
			}
			dummies.add(f.getFeatureName()); // just for sanity
			dummyClass.add(allAnnotations.get(0));
		}
		dummies.add("BLANK");
		dummyClass.add(allAnnotations.get(0));

		DocumentList dummyDocs = new DocumentList(dummies, dummyColumns);

		update.update("Actually Building Feature Table");
		FeatureTable soloTable = new FeatureTable(dummyDocs, soloHits, 0, annotation, original.getClassValueType(), original.getFeatureTableLabelArray());
		return soloTable;
	}

	@Override
	public String toString()
	{
		return "Model Analysis";
	}

	@Override
	public String getOutputName()
	{
		return "influence";
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
	public Map<Feature, Double> evaluateModelFeatures(Recipe trainedRecipe, boolean[] mask, String eval, String pred, String act, StatusUpdater update)
	{
		if (!evaluations.containsKey(trainedRecipe))
		{
			initiateMap(trainedRecipe, false, update);
		}
		if (trainedRecipe == null) return defaultMap;

		try
		{
			return evaluations.get(trainedRecipe).get(pred).get(act).get(eval);
		}
		catch (Exception e)
		{
			return defaultMap;
		}
	}

	@Override
	public boolean canEvaluateRecipe(Recipe r, String evaluationKey)
	{
		if(evaluationKey.equals(influenceKey))
			return r.getTrainingTable().getClassValueType() == Type.NOMINAL;
		else if(evaluationKey.equals(featureWeightKey))
			return learnerMightHaveLinearWeights(r);
		else if(evaluationKey.equals(featureSelectionKey))
		{
			return recipeUsesFeatureSelection(r);
		}
		
		return false;
	}

	protected boolean recipeUsesFeatureSelection(Recipe r)
	{
		for(SIDEPlugin plug : r.getWrappers().keySet())
		{
			if(plug instanceof FeatureSelection)
				return true;
		}
		return false;
	}

}
