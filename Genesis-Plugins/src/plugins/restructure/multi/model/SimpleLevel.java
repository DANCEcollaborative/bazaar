package plugins.restructure.multi.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;

public class SimpleLevel extends StructuredLevel{

	String className;
	
	@Override
	public String toString(){
		return className;
	}
	
	public SimpleLevel(String s){
		className = s;
	}

	@Override
	public String getPrefix(FeatureTable table, int index){
		if(table.getDocumentList().allAnnotations().keySet().contains(className)){
			return className+"::"+table.getDocumentList().getAnnotationArray(className).get(index);
		}else return "invalid";
	}
	
	@Override
	public Map<String, Collection<Feature>> getFeaturesByExtractor() {
		Map<String, Collection<Feature>> these = new TreeMap<String, Collection<Feature>>();
		for(Feature f : features){
			String ext = f.getExtractorPrefix();
			if(!these.containsKey(ext)){
				these.put(ext, new TreeSet<Feature>());
			}
			these.get(ext).add(f);
		}
		return these;
	}

}
