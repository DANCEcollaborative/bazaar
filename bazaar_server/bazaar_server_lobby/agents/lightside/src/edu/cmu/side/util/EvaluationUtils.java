package edu.cmu.side.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EvaluationUtils
{

	private static Map<String, Map<String, List<Integer>>> confusionMatrix;

	public static Map<String, Map<String, List<Integer>>> generateConfusionMatrix(List<String> actual, List<? extends Comparable<?>> predicted, String[] poss)
	{
		confusionMatrix = new TreeMap<String, Map<String, List<Integer>>>();
		for (String p : poss)
		{
			confusionMatrix.put(p, new TreeMap<String, List<Integer>>());
			for (String a : poss)
			{
				confusionMatrix.get(p).put(a, new ArrayList<Integer>());
			}
		}
		for (int i = 0; i < actual.size(); i++)
		{
			String pred;
			if (predicted.get(i) == null)
			{
				// TODO: Remember that this only applies to CSC data
				// System.out.println("TR 143 WRONG: "+i+" is null in the predictions list!");
				pred = "NA";
			}
			else
			{
				pred = predicted.get(i).toString();
			}
			String act = actual.get(i);
			confusionMatrix.get(pred).get(act).add(i);
		}
		return confusionMatrix;

	}
	
	public static double getAccuracy(Map<String, Map<String, List<Integer>>> matrix, String[] labelArray, int numInstances){
		double corr = 0;
		for(String s : labelArray){
			if(matrix.containsKey(s) && matrix.get(s).containsKey(s)){
				corr += matrix.get(s).get(s).size();				
			}
		}
		return corr/(numInstances);
	}

	public static double getKappa(Map<String, Map<String, List<Integer>>> matrix, String[] labelArray, int numInstances)
	{
		Map<String, Double> predProb = new TreeMap<String, Double>();
		Map<String, Double> actProb = new TreeMap<String, Double>();
		double correctCount = 0.0;
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
	
	public static String getHeader()
	{
		return "acc\tkappa\tmodelname";
	}
	
	public static String evaluate(List<String> actual, List<? extends Comparable<?>> predicted, String[] poss, String name)
	{
		generateConfusionMatrix(actual, predicted, poss);
		String evaluation = getAccuracy(confusionMatrix, poss, predicted.size())+"\t"+getKappa(confusionMatrix, poss, predicted.size())+"\t"+name;
		
		return evaluation;
	}
}
