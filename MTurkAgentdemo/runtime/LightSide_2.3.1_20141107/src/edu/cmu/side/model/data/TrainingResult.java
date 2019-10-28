package edu.cmu.side.model.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.side.model.feature.Feature;

/**
 * The result of the training process; doesn't actually contain the learning itself,
 * only the set of training tables, test tables, predictions, and confusion matrix.
 * @author emayfiel
 *
 */
public class TrainingResult implements Serializable
{ 
	//added for backwards-compatibility - change if the fields change!
	private static final long serialVersionUID = -6746152451579063766L;
	private String name;
	private String longDescriptionString;
	private FeatureTable train;
	private FeatureTable test;
	private List<? extends Comparable<?>> predictions;
	private Map<String, Map<String, List<Integer>>> confusionMatrix = new TreeMap<String, Map<String, List<Integer>>>();
	private Map<String, List<Double>> distributions;
	transient private Map<String, String> cachedEvaluations = new HashMap<String, String>();
	
	private Collection<String> possibleLabels = new TreeSet<String>();

	@Override
	public String toString(){
		return name;
	}

	public String getName(){
		return name;
	}

	public void setName(String n){
		name = n;
	}

	public Map<String, Map<String, List<Integer>>> getConfusionMatrix(){
		return confusionMatrix;
	}


	public FeatureTable getEvaluationTable(){
		return test;
	}

	public FeatureTable getTrainingTable(){
		return train;
	}

	public List<? extends Comparable<?>> getPredictions(){
		return predictions;
	}

	public int numEvaluationInstances(){
		return test.getDocumentList().getSize();
	}

	public String getLongDescriptionString(){
		return longDescriptionString;
	}

	public void setLongDescriptionString(String l){
		longDescriptionString = l;
	}
	public TrainingResult(FeatureTable tr, FeatureTable te, List<? extends Comparable<?>> pred, String longString){
		this(tr, te, pred);
		longDescriptionString = longString;
	}

	public TrainingResult(FeatureTable tr, List<? extends Comparable<?>> pred, String longString){
		this(tr, pred);
		longDescriptionString = longString;
	}

	public TrainingResult(FeatureTable tr, List<? extends Comparable<?>> pred){
		train = tr;
		test = tr;
		predictions = pred;
		generateConfusionMatrix(tr.getClassValueType(), tr.getAnnotations(), predictions);
	}

	/**
	 * Use with supplied test set.
	 */
	public TrainingResult(FeatureTable tr, FeatureTable te, List<? extends Comparable<?>> pred){
		train = tr;
		test = te;
		predictions = pred;
		generateConfusionMatrix(te.getClassValueType(), te.getAnnotations(), predictions);
	}

	public TrainingResult(FeatureTable table, PredictionResult pred)
	{
		this(table, pred.getPredictions());
		distributions = pred.getDistributions();
	}

	public TrainingResult(FeatureTable trainSet, FeatureTable testSet, PredictionResult pred)
	{
		this(trainSet, testSet, pred.getPredictions());
		distributions = pred.getDistributions();
		
	}

	public Map<String, List<Double>> getDistributions()
	{
		return distributions;
	}

	public void setDistributions(Map<String, List<Double>> distributions)
	{
		this.distributions = distributions;
	}
	
	public Map<String, Double> getDistributionForDocument(int i)
	{
		Map<String, Double> distro = new HashMap<String, Double>();
		
		if(distributions != null)
			for(String k : distributions.keySet())
			{
				distro.put(k, distributions.get(k).get(i));
			}
		
		return distro;
	}

	private void generateConfusionMatrix(Feature.Type type, List<String> actual, List<? extends Comparable<?>> predicted){
		//String[] poss = train.getLabelArray();
		
		possibleLabels.addAll(Arrays.asList(train.getLabelArray()));
		possibleLabels.addAll(Arrays.asList(test.getLabelArray()));
		
		switch(type){
		case NOMINAL:
		case STRING:
		case BOOLEAN:
			for(String p : possibleLabels){
				confusionMatrix.put(p, new TreeMap<String, List<Integer>>());
				for(String a : possibleLabels){
					confusionMatrix.get(p).put(a, new ArrayList<Integer>());
				}
			}
//			System.out.println("TR 145: "+predicted.size()+" predicted intances");
//			System.out.println("TR 145: "+actual.size()+" actual intances");
			for(int i = 0; i < actual.size(); i++)
			{
				String pred;
				if(predicted.get(i) == null)
				{
					//TODO: Remember that this only applies to CSC data
//					System.out.println("TR 143 WRONG: "+i+" is null in the predictions list!");
					pred = "NA";
				}
				else
				{
					pred = predicted.get(i).toString();
				}
				String act = actual.get(i);
				confusionMatrix.get(pred).get(act).add(i);
			}
			break;
			case NUMERIC:
				
				for(int i = 1; i <= 5; i++)
				{
					confusionMatrix.put("Q"+i, new TreeMap<String, List<Integer>>());
					for(int j = 1; j <= 5; j++)
					{
						confusionMatrix.get("Q"+i).put("Q"+j, new ArrayList<Integer>());
					}
				}
				
				for (int i = 0; i < actual.size(); i++)
				{
					Double predDbl = Double.parseDouble(predicted.get(i).toString());
					ArrayList<Double> breakpoints = getTrainingTable().getNumericBreakpoints();
					int Qpred = -1;
					int j = 0;
					while (j < 4 && predDbl > breakpoints.get(j))
					{
						j++;
					}
					Qpred = j;
					
					String pred = "Q" + (Qpred + 1);
					String act = getEvaluationTable().getNominalClassValues().get(i);
//					if (!confusionMatrix.containsKey(pred))
//					{
//						confusionMatrix.put(pred, new TreeMap<String, List<Integer>>());
//					}
//					if (!confusionMatrix.get(pred).containsKey(act))
//					{
//						confusionMatrix.get(pred).put(act, new ArrayList<Integer>());
//					}
					confusionMatrix.get(pred).get(act).add(i);
				}
				break;
		}

	}



	public String getTextConfusionMatrix(){
		return getTextConfusionMatrix(confusionMatrix.keySet().toArray(new String[0]), confusionMatrix);
	}

	protected static String getTextConfusionMatrix(String[] labelArray, Map<String, Map<String, List<Integer>>> confusion){
		StringBuilder sb = new StringBuilder();
		int max = 4;
		for(String p : labelArray){
			for(String a : labelArray){
				max = Math.max(max, Math.max(p.length(), a.length()));
				int numDigits = 1;
				int numHits = confusion.containsKey(p)?(confusion.get(p).containsKey(a)?confusion.get(p).get(a).size():0):0;
				while(numHits>=10){
					numHits /= 10;
					numDigits++;
				}
				max =  Math.max(max, numDigits);
			}
		}
		for(int i = 0; i < max; i++){
			sb.append(" ");
		}
		String format = "%"+max+"s";
		for(String p : labelArray){
			sb.append(String.format(format,p));
		}
		sb.append("\n");
		for(String a : labelArray){
			sb.append(String.format(format,a));
			for(String p : labelArray){
				int numHits = confusion.containsKey(p)?(confusion.get(p).containsKey(a)?confusion.get(p).get(a).size():0):0;
				sb.append(String.format(format,(""+numHits)));                  
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public void cacheEvaluations(Map<String, String> eval)
	{
		getCachedEvaluations().putAll(eval);
	}
	
	public Map<String, String> getCachedEvaluations()
	{
		if(cachedEvaluations == null)
			cachedEvaluations = new HashMap<String, String>();
		return cachedEvaluations;
	}
	public Boolean equals(TrainingResult other){
		Boolean toReturn = true;
//		toReturn=this.name.equals(other.getName())?toReturn:false;
		toReturn=this.longDescriptionString.equals(other.getLongDescriptionString())?toReturn:false;
		toReturn=(this.train==null?other.getTrainingTable()==null:this.train.equals(other.getTrainingTable())&&toReturn);
		toReturn=(this.test==null?other.getEvaluationTable()==null:this.test.equals(other.getEvaluationTable())&&toReturn);
		toReturn=(this.predictions==null?other.getPredictions()==null:this.predictions.equals(other.getPredictions())&&toReturn);
		toReturn=(this.confusionMatrix==null?other.getConfusionMatrix()==null:this.confusionMatrix.equals(other.getConfusionMatrix())&&toReturn);
		toReturn=(this.distributions==null?other.getDistributions()==null:this.distributions.equals(other.getDistributions())&&toReturn);
		return toReturn;
	}

	public Collection<String> getPossibleLabels()
	{
		return possibleLabels;
	}
	
	
}
