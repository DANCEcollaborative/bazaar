package plugins.restructure.multi.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.RestructurePlugin;

public class NestedLevel extends StructuredLevel {

	public NestedLevel(StructuredLevel a, StructuredLevel b){
		classA = a;
		classB = b;
	}
	
	@Override
	public Collection<FeatureHit> model(FeatureTable table, RestructurePlugin plugin) {
		Collection<FeatureHit> hits = new HashSet<FeatureHit>();
		hits.addAll(classA.model(table, plugin));
		hits.addAll(classB.model(table, plugin));
		return hits;
	}
	
	@Override
	public String toString(){
		return classA.toString() + "[" + classB.toString() + "]";
	}
	

	@Override
	public void addAllFeatures(Collection<Feature> feats){
		features.addAll(feats);
		classA.addAllFeatures(feats);
		classB.addAllFeatures(feats);
	}

	@Override
	public Map<String, Collection<Feature>> getFeaturesByExtractor() {
		Map<String, Collection<Feature>> feats = new TreeMap<String, Collection<Feature>>();
		Map<String, Collection<Feature>> featsHigh = classA.getFeaturesByExtractor();
		Map<String, Collection<Feature>> featsLow = classB.getFeaturesByExtractor();
		for(String s : featsHigh.keySet()){
			if(!feats.containsKey(s)){
				feats.put(s, new TreeSet<Feature>());
			}
			feats.get(s).addAll(featsHigh.get(s));
		}
		for(String s : featsLow.keySet()){
			if(!feats.containsKey(s)){
				feats.put(s, new TreeSet<Feature>());
			}
			feats.get(s).addAll(featsHigh.get(s));
		}
		return feats;
	}

	@Override
	public String getPrefix(FeatureTable table, int index) {
		return "nest";
	}

}
