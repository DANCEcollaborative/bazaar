package edu.cmu.side.model.feature;

import java.util.Map;


public class RegroupFeatureHit extends FeatureHit{

	protected int originalIndex;
	public RegroupFeatureHit(FeatureHit original, Map<Integer, Integer> indexMap) {
		super(original.getFeature(), original.getValue(), indexMap.get(original.getDocumentIndex()));
		originalIndex = original.getDocumentIndex();
	}
	
	public RegroupFeatureHit(FeatureHit original, Map<Integer, Integer> indexMap, int index){
		super(original.getFeature(), original.getValue(), indexMap.get(original.getDocumentIndex()));
		originalIndex = index;
	}
	
	public int getOriginalIndex(){
		return originalIndex;
	}
	
	@Override
	public int compareTo(FeatureHit o)
	{
		int featureCompare = super.compareTo(o);
		if (o instanceof RegroupFeatureHit && featureCompare == 0)
		{
			return originalIndex - (((RegroupFeatureHit) o).originalIndex);
		}
		else
		{
			return featureCompare;
		}
	}
	
	@Override
	public String toString()
	{
		return this.feature+"@"+this.documentIndex+"/"+this.getOriginalIndex();
	}
}
