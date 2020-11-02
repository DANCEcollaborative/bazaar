package plugins.metrics.models;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.regression.SimpleRegression;

import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.plugin.ModelMetricPlugin;

public class BasicModelEvaluations extends ModelMetricPlugin
{

	private Map<String, String> configuration = new TreeMap<String, String>();
	private Map<TrainingResult, Map<String, String>> cache = new HashMap<TrainingResult, Map<String, String>>();
	private ArrayList<String> available = new ArrayList<String>();

	public BasicModelEvaluations()
	{
		available.add("Accuracy");
		available.add("Kappa");
		available.add("Precision");
		available.add("Recall");
	}

	@Override
	public String getOutputName()
	{
		return "base";
	}

	@Override
	public String toString()
	{
		return "Basic Model Statistics";
	}

	@Override
	public Map<String, String> evaluateModel(TrainingResult model, Map<String, String> settings)
	{
		if (!cache.containsKey(model))
		{

			Map<String, String> evals =  new TreeMap<String, String>();
			cache.put(model, evals);
			switch (model.getTrainingTable().getClassValueType())
			{
				case NOMINAL:
				case BOOLEAN:
				case STRING:
					evals.put("Accuracy", "" + getAccuracy(model));
					evals.put("Kappa", "" + getKappa(model));
					break;
				case NUMERIC:
					calculateCorrelationFeatures(model);
			}
		}
		return cache.get(model);
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		return configuration;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		// TODO Auto-generated method stub
	}

	// Only called when class value is numeric.
	public void calculateCorrelationFeatures(TrainingResult model)
	{
		List<Double> doubles = (List<Double>) model.getPredictions();
		double[] actuals = model.getEvaluationTable().getNumericClassValues("numeric");

		SimpleRegression regress = new SimpleRegression();
		for (int i = 0; i < actuals.length; i++)
		{
			regress.addData(doubles.get(i), actuals[i]);
		}
		cache.get(model).put("Correlation", "" + regress.getR());
		cache.get(model).put("Mean Squared Error", "" + regress.getMeanSquareError());
	}

	public double getAccuracy(TrainingResult model)
	{
		double corr = 0;
		String[] labels = model.getEvaluationTable().getLabelArray();
		for (String s : labels)
		{
			if (model.getConfusionMatrix().containsKey(s) && model.getConfusionMatrix().get(s).containsKey(s))
			{
				corr += model.getConfusionMatrix().get(s).get(s).size();
			}
		}
		return corr / (0.0 + model.getEvaluationTable().getDocumentList().getSize());
	}

	public double getKappa(TrainingResult model)
	{
		Map<String, Double> predProb = new TreeMap<String, Double>();
		Map<String, Double> actProb = new TreeMap<String, Double>();
		double correctCount = 0.0;
		Map<String, Map<String, List<Integer>>> matrix = model.getConfusionMatrix();
		String[] labelArray = model.getEvaluationTable().getLabelArray();
		for (String pred : labelArray)
		{
			for (String act : labelArray)
			{
				if (matrix.containsKey(pred) && matrix.get(pred).containsKey(act))
				{
					List<Integer> cell = matrix.get(pred).get(act);
					if (!predProb.containsKey(pred))
					{
						predProb.put(pred, 0.0);
					}
					predProb.put(pred, predProb.get(pred) + cell.size());
					if (!actProb.containsKey(act))
					{
						actProb.put(act, 0.0);
					}
					actProb.put(act, actProb.get(act) + cell.size());
					if (act.equals(pred))
					{
						correctCount += cell.size();
					}
				}
			}
		}
		double chance = 0.0;
		int numInstances = model.numEvaluationInstances();
		for (String lab : labelArray)
		{
			if (numInstances > 0 && predProb.containsKey(lab) && actProb.containsKey(lab))
			{
				predProb.put(lab, predProb.get(lab) / (0.0 + numInstances));
				actProb.put(lab, actProb.get(lab) / (0.0 + numInstances));
				chance += (predProb.get(lab) * actProb.get(lab));
			}
		}
		correctCount /= (0.0 + numInstances);
		double kappa = (correctCount - chance) / (1 - chance);
		return kappa;
	}
}
