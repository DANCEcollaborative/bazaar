package plugins.metrics.features.tables;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

import edu.cmu.side.model.FreqMap;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.TableFeatureMetricPlugin;

public class BasicFeatureEvaluations extends TableFeatureMetricPlugin<Double>
{

	Map<String, Collection<Feature.Type>> evaluationNames = new TreeMap<String, Collection<Feature.Type>>();

	Map<String, String> configuration = new TreeMap<String, String>();
	// Map Table -> Target -> Eval -> Feature -> Double
	Map<FeatureTable, Map<String, Map<String, Map<Feature, Double>>>> evaluations = new HashMap<FeatureTable, Map<String, Map<String, Map<Feature, Double>>>>();

	PearsonsCorrelation correlator = new PearsonsCorrelation();

	public BasicFeatureEvaluations()
	{
		super();
		Collection<Feature.Type> general = new HashSet<Feature.Type>();
		Collection<Feature.Type> nominal = new HashSet<Feature.Type>();
		Collection<Feature.Type> numeric = new HashSet<Feature.Type>();

		general.add(Feature.Type.NOMINAL);
		general.add(Feature.Type.NUMERIC);
		general.add(Feature.Type.BOOLEAN);
		general.add(Feature.Type.STRING);

		nominal.add(Feature.Type.NOMINAL);
		nominal.add(Feature.Type.BOOLEAN);
		nominal.add(Feature.Type.STRING);

		numeric.add(Feature.Type.NUMERIC);

		evaluationNames.put("Total Hits", general);
		evaluationNames.put("Target Hits", general);
		evaluationNames.put("Kappa", nominal);
		evaluationNames.put("Precision", nominal);
		evaluationNames.put("Recall", nominal);
		evaluationNames.put("F-Score", nominal);
		evaluationNames.put("Correlation", general);
	}

	@Override
	public String toString()
	{
		return "Basic Table Statistics";
	}

	@Override
	public String getOutputName()
	{
		return "base";
	}

	@Override
	public Map<String, Collection<Feature.Type>> getAvailableEvaluations()
	{
		return evaluationNames;
	}

	@Override
	public Map<Feature, Double> evaluateTableFeatures(FeatureTable table, boolean[] mask, String eval, String target, StatusUpdater update)
	{
		halt = false;
		if (!evaluations.containsKey(table))
		{
			evaluations.put(table, new HashMap<String, Map<String, Map<Feature, Double>>>());
//			System.out.println("BFE 84: table label array: "+Arrays.toString(table.getLabelArray()));
			for (String s : table.getLabelArray())
			{
				evaluations.get(table).put(s, new TreeMap<String, Map<Feature, Double>>());
				for (String e : getAvailableEvaluations().keySet())
				{
					evaluations.get(table).get(s).put(e, new TreeMap<Feature, Double>());
				}
			}
			int index = 0;
			for (Feature f : table.getFeatureSet())
			{
				if (!halt)
				{
					index++;
					if(index%50==0){
						update.update("Evaluating feature " + index + "/" + table.getFeatureSet().size());						
					}
					addEvaluation(table, mask, f);
				}
				else
				{
					evaluations.remove(table);
					return new HashMap<Feature, Double>();
				}
			}

			Map<String, FreqMap<Feature>> countsByLabel = new TreeMap<String, FreqMap<Feature>>();

//			System.out.println("BFE 114: table nominal label array: "+Arrays.toString(table.getNominalLabelArray()));
			for (String s : table.getNominalLabelArray())
			{
				countsByLabel.put(s, new FreqMap<Feature>());
			}
			FreqMap<Feature> totalHits = new FreqMap<Feature>();
			for (Feature f : table.getFeatureSet())
			{
				for (FeatureHit fh : table.getHitsForFeature(f))
				{
					if (mask[fh.getDocumentIndex()])
					{
						totalHits.count(f);
						String nom = table.getNominalClassValues().get(fh.getDocumentIndex());
						countsByLabel.get(nom).count(f);
					}
				}
			}
			update.update("");
			for (String s : countsByLabel.keySet())
			{
				evaluations.get(table).get(s).put("Target Hits", new TreeMap<Feature, Double>());
				evaluations.get(table).get(s).put("Total Hits", new TreeMap<Feature, Double>());
				for (Feature f : table.getFeatureSet())
				{
					try
					{
						evaluations.get(table).get(s).get("Target Hits").put(f, 0.0 + countsByLabel.get(s).safeGet(f));
						evaluations.get(table).get(s).get("Total Hits").put(f, 0.0 + totalHits.get(f));
					}
					catch (NullPointerException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
//		System.out.println("BFE 126: table " + table + ", target " + target + ", eval " + eval);

		Map<String, Map<String, Map<Feature, Double>>> evalForTable = evaluations.get(table);
		if (evalForTable.containsKey(target))
		{
			Map<String, Map<Feature, Double>> evalForTarget = evalForTable.get(target);
			if (evalForTarget.containsKey(eval))
			{
				Map<Feature, Double> evalForFeature = evalForTarget.get(eval);
				return evalForFeature;
			}
		}
		return new HashMap<Feature, Double>();
	}

	protected void addEvaluation(FeatureTable table, boolean[] mask, Feature f)
	{
		Map<String, Map<Feature, Comparable>> hitsByLabelMap = new TreeMap<String, Map<Feature, Comparable>>();

		String[] possibleLabels = table.getNominalLabelArray();
		DocumentList documents = table.getDocumentListQuickly();
		switch (f.getFeatureType())
		{
			case NUMERIC:
				double[] featureDoubleValues = new double[documents.getSize()];
				for (int i = 0; i < documents.getSize(); i++)
				{
					featureDoubleValues[i] = 0.0;
				}
				for (FeatureHit hit : table.getHitsForFeature(f))
				{
					featureDoubleValues[hit.getDocumentIndex()] = (Double) hit.getValue();
				}
				for (String s : possibleLabels)
				{
					double corr = correlator.correlation(featureDoubleValues, table.getNumericClassValues(s));
					evaluations.get(table).get(s).get("Correlation").put(f, corr);
				}
				break;
			case NOMINAL:
			case BOOLEAN:
			case STRING:
				switch (table.getClassValueType())
				{
					case NUMERIC:
						double correlation = calculateFeatureCorrelation(table, f, "numeric");
						for (String s : possibleLabels)
						{
							evaluations.get(table).get(s).get("Correlation").put(f, correlation);
						}
						break;
					case NOMINAL:
					case BOOLEAN:
					case STRING:
						Collection<FeatureHit> hits = table.getHitsForFeature(f);
						for (String label : possibleLabels)
						{
							double correlationForLabel = calculateFeatureCorrelation(table, f, label);
							
							if (!hitsByLabelMap.containsKey(label))
							{
								hitsByLabelMap.put(label, new HashMap<Feature, Comparable>());
							}
							double[][] kappaMatrix = new double[2][2];
							for (int i = 0; i < 2; i++)
							{
								for (int j = 0; j < 2; j++)
								{
									kappaMatrix[i][j] = 0;
								}
							}
							boolean[] hit = new boolean[documents.getSize()];
							int count = 0;
							for (FeatureHit fh : hits)
							{
								if (checkHitMatch(f, fh.getValue()))
								{
									if (!mask[fh.getDocumentIndex()]) continue;
									hit[fh.getDocumentIndex()] = true;
									if (table.getAnnotations().get(fh.getDocumentIndex()).equals(label))
									{
										count++;
									}
								}
							}
							hitsByLabelMap.get(label).put(f, count);
							double all = 0;
							for (int i = 0; i < documents.getSize(); i++)
							{
								if (!mask[i]) continue;
								kappaMatrix[table.getAnnotations().get(i).equals(label) ? 0 : 1][hit[i] ? 0 : 1]++;
								all++;
							}
							double rightHits = kappaMatrix[0][0];
							double wrongHits = kappaMatrix[1][0];
							double featHits = kappaMatrix[0][0] + kappaMatrix[1][0];
							double actHits = kappaMatrix[0][0] + kappaMatrix[0][1];
							double accuracy = (kappaMatrix[0][0] + kappaMatrix[1][1]) / all;
							double pChance = ((featHits / all) * (actHits / all)) + (((all - featHits) / all) * ((all - actHits) / all));

							Double prec = rightHits / (rightHits + wrongHits);
							Double rec = rightHits / actHits;
							Double fmeasure = (2 * prec * rec) / (prec + rec);
							Double kappa = (accuracy - pChance) / (1 - pChance);

							if (Double.NaN == rec) rec = 0.0;
							if (Double.NaN == fmeasure) fmeasure = 0.0;

							try
							{
								evaluations.get(table).get(label).get("Precision").put(f, prec);
								evaluations.get(table).get(label).get("Recall").put(f, rec);
								evaluations.get(table).get(label).get("F-Score").put(f, fmeasure);
								evaluations.get(table).get(label).get("Kappa").put(f, kappa);
								evaluations.get(table).get(label).get("Correlation").put(f, correlationForLabel);
							}
							catch (NullPointerException e)
							{
								e.printStackTrace();
							}
						}
						break;
				}
				break;
		}
	}

	public double calculateFeatureCorrelation(FeatureTable table, Feature f, String label)
	{
		DocumentList documents = table.getDocumentListQuickly();
		double[] featureConvertedDoubleValues = new double[documents.getSize()];
		double[] numericClassValues = table.getNumericClassValues().get(label);
		for (int i = 0; i < documents.getSize(); i++)
		{
			featureConvertedDoubleValues[i] = 0.0;
		}
		for (FeatureHit hit : table.getHitsForFeature(f))
		{
			//FIXME: this does not address String or Nominal feature types correctly.
			//solution: explode nominal feature types to boolean early?
			featureConvertedDoubleValues[hit.getDocumentIndex()] = 1.0;
		}

		double corr = correlator.correlation(featureConvertedDoubleValues, numericClassValues);
		
		return corr;
	}

	/**
	 * Checks whether this feature "hit" a document, for the purpose of
	 * converting all these different feature types into a boolean check for
	 * basic evaluations.
	 */
	public boolean checkHitMatch(Feature f, Object value)
	{
		switch (f.getFeatureType())
		{
			case BOOLEAN:
				return Boolean.TRUE.equals(value);
			case NOMINAL:
				return false;
			case NUMERIC:
				return ((Number) value).doubleValue() > 0;
			case STRING:
				return value.toString().length() > 0;
		}
		return false;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		return configuration;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{

	}

	@Override
	public boolean canEvaluateRecipe(Recipe r, String evaluationKey)
	{
		Feature.Type classType = r.getTrainingTable().getClassValueType();
		return evaluationNames.get(evaluationKey).contains(classType);
	}

}
