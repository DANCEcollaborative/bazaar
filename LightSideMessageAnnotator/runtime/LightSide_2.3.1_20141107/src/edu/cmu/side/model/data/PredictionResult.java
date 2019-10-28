package edu.cmu.side.model.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PredictionResult implements Serializable
{

	private String name;
	private List<? extends Comparable<?>> predictions;
	private Map<String, List<Double>> distributions;

	@Override
	public String toString()
	{
		if (name != null) return name;
		return "predictions";
	}

	public String getName()
	{
		return name;
	}

	public void setName(String n)
	{
		name = n;
	}

	public PredictionResult(List<? extends Comparable<?>> pred)
	{
		predictions = pred;
	}

	public PredictionResult(List<? extends Comparable<?>> pred, Map<String, List<Double>> dist)
	{
		this(pred);
		distributions = dist;
	}

	public List<Double> getScoresForLabel(String label)
	{
		return distributions.get(label);
	}

	public Map<String, List<Double>> getDistributions()
	{
		return distributions;
	}

	public List<? extends Comparable<?>> getPredictions()
	{
		return predictions;
	}

	public double[] getDistributionForInstance(int i, String[] labels)
	{
		double[] distro = new double[labels.length];
		
		
		for(int j = 0; j < labels.length; j++)
		{
			distro[j] = distributions.get(labels[j]).get(i);
		}
		
		return distro;
	}
	
	public Map<String, Double> getDistributionMapForInstance(int i)
	{
		Map<String, Double> distro = new TreeMap<String, Double>();
		
		
		for(String key : distributions.keySet())
		{
			distro.put(key, distributions.get(key).get(i));
		}
		
		return distro;
	}
}
