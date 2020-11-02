package plugins.metrics.models;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.plugin.ModelMetricPlugin;

public class PrecisionRecallModelEvaluations extends ModelMetricPlugin
{

	private Map<String, String> configuration = new TreeMap<String, String>();
	private Map<TrainingResult, Map<String, String>> cache = new HashMap<TrainingResult, Map<String, String>>();
	private ArrayList<String> available = new ArrayList<String>();

	public PrecisionRecallModelEvaluations()
	{
		available.add("Precision");
		available.add("Recall");
		available.add("F1");
		available.add("F2");
	}

	@Override
	public String getOutputName()
	{
		return "precision_recall";
	}

	@Override
	public String toString()
	{
		return "More Model Metrics";
	}

	@Override
	public Map<String, String> evaluateModel(TrainingResult model, Map<String, String> settings)
	{
		if (!cache.containsKey(model))
		{
			Map<String, Double> precisionRecallMetrics = getPrecisionRecallMetrics(model);

			Map<String, String> evals =  new TreeMap<String, String>();
			cache.put(model, evals);
			switch (model.getTrainingTable().getClassValueType())
			{
				case NOMINAL:
				case BOOLEAN:
				case STRING:
					evals.put("Precision", "" + precisionRecallMetrics.get("Precision"));
					evals.put("Recall", "" + precisionRecallMetrics.get("Recall"));
					evals.put("F1", "" + precisionRecallMetrics.get("F1"));
					evals.put("F2", "" + precisionRecallMetrics.get("F2"));
					break;
				case NUMERIC:
					break;
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


	public Map<String, Double> getPrecisionRecallMetrics(TrainingResult model)
	{
		String[] labels = model.getEvaluationTable().getLabelArray();
		Map<String, Map<String, List<Integer>>> matrix = model.getConfusionMatrix();
		HashMap<String, Double> labelMetrics = new HashMap<String, Double>();
		Map<String, Double> metrics = labelMetrics;
		
		double precision = 0;
		double recall = 0;
		double f1 = 0;
		double f2 = 0;
		
		double total =  model.getEvaluationTable().getDocumentList().getSize();
		
		for (String s : labels)
		{
			double tp = 0;
			double fp = 0;
			double tn = 0;
			double fn = 0;
			
			if (model.getConfusionMatrix().containsKey(s) && matrix.get(s).containsKey(s))
			{
				tp += matrix.get(s).get(s).size();
			}
			
			for(String other : labels)
			{
				if(other.equals(s))
					continue;
				
				if (model.getConfusionMatrix().containsKey(s) && matrix.get(s).containsKey(other))
				{
					fp += matrix.get(s).get(other).size();
				}

				if (model.getConfusionMatrix().containsKey(other) && matrix.get(other).containsKey(s))
				{
					fn += matrix.get(other).get(s).size();
				}
			}
			
			tn = total - (fp + fn + tp);

			double num_actual = tp+fn;
			double weight = num_actual / total;
			
			double label_recall = (tp+fn == 0) ? 0 : (tp/(tp+fn));
			double label_precision = (tp+fp == 0) ? 0 : (tp/(tp+fp));
			double label_f1 = (label_precision + label_recall == 0) ? 0 : (2 * label_precision * label_recall) / (label_precision + label_recall);
			double label_f2 = (4*label_precision + label_recall == 0)? 0 : (5 * label_precision * label_recall) / (4*label_precision + label_recall);
			
			f1 += weight * label_f1;
			f2 += weight * label_f2; 
			
			recall += label_recall*weight;
			precision += label_precision*weight;

			
			/*logger.log(Level.FINEST, "class weight for "+s+" = "+weight + "("+(int)num_actual+" actual instances)\n"
			+"TP: "+tp/num_actual+"\n"
			+"FP: "+fp/(fp + tn)+"\n"
			+"Prec: "+label_precision+"\n"
			+"Recall: "+label_recall+"\n"
			+"F1: "+label_f1+"\n"
			+"F2: "+label_f2+"\n");*/

		}
		
		
		metrics.put("Precision", precision);
		metrics.put("Recall", recall);
		metrics.put("F1", f1);
		metrics.put("F2", f2);
		
		return metrics;
	}

}
