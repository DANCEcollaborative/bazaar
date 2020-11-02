package plugins.restructure.multi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.plugin.RestructurePlugin;

public abstract class StructuredLevel implements Serializable, Comparable<StructuredLevel>
{

	protected StructuredLevel classA;
	protected StructuredLevel classB;
	protected boolean slope;
	protected boolean intercept;

	Set<Feature> features = new TreeSet<Feature>();

	public DefaultMutableTreeNode getTreeRepresentation(){
		DefaultMutableTreeNode node = new DefaultMutableTreeNode(this);
		DefaultMutableTreeNode intercept = new DefaultMutableTreeNode("Intercepts");
		DefaultMutableTreeNode slope = new DefaultMutableTreeNode("Slopes");
		DefaultMutableTreeNode features = new DefaultMutableTreeNode("Features");
		Map<String, Collection<Feature>> exts = getFeaturesByExtractor();
		for(String s : exts.keySet()){
			DefaultMutableTreeNode ext = new DefaultMutableTreeNode(s + ": " + exts.get(s).size() + " Features");
			features.add(ext);
		}
		node.add(intercept);
		node.add(slope);
		node.add(features);
		return node;
	}

	public abstract Map<String, Collection<Feature>> getFeaturesByExtractor();

	public Collection<FeatureHit> model(FeatureTable table, RestructurePlugin plugin) {
		Collection<FeatureHit> hits = new HashSet<FeatureHit>();
		for(Feature f : features)
		{
			
			if(f.getExtractorPrefix().equals("growth_column"))
			{
				boolean isNumeric = true;
				ArrayList<Double> numbers = new ArrayList<Double>();
				try{
					for(int i = 0; i < table.getDocumentList().getSize(); i++){
						numbers.add(Double.parseDouble(table.getDocumentList().getAnnotationArray(f.getFeatureName()).get(i)));
					}
				}catch(Exception e){
					isNumeric = false;
				}
				if(isNumeric){
					for(int i = 0; i < table.getDocumentList().getSize(); i++){
						Feature newF = Feature.fetchFeature("growth", getPrefix(table, i)+"_"+f.getFeatureName(), Feature.Type.NUMERIC, f.getExtractor());
						hits.add(new FeatureHit(newF, numbers.get(i), i));
					}
				}else{
					for(int i = 0; i < table.getDocumentList().getSize(); i++){
						Feature newF = Feature.fetchFeature("growth", getPrefix(table, i)+"_"+f.getFeatureName(), Feature.Type.BOOLEAN, f.getExtractor());
						hits.add(new FeatureHit(newF, Boolean.TRUE, i));
					}
				}
			}
			else
			{
				/**
				 * DSA 8/15/15 -- 
				 * Some of the features we're trying to do FEDA on might be missing in datasets beyond the training set.
				 * Ignore them -- they won't be present in any domain if they're not present at all.
				 */
				if(table.getHitsForFeature(f) == null)
				{
					System.out.println("SL 79: Feature "+f+" doesn't occur naturally in this feature table -- ignoring.");
					continue;
				}
				for(FeatureHit hit : table.getHitsForFeature(f)){
					int ind = hit.getDocumentIndex();
					Feature newF;
					if(f.getFeatureType() == Feature.Type.NOMINAL)
						newF = Feature.fetchFeature("growth", getPrefix(table, ind)+"_"+f.getFeatureName(), f.getNominalValues(), f.getExtractor());						
					
					else
						newF = Feature.fetchFeature("growth", getPrefix(table, ind)+"_"+f.getFeatureName(), f.getFeatureType(), f.getExtractor());						

//					System.out.println("SL 83: trying to add new feature hit for feature "+newF+" value "+hit.getValue());
					if(hit instanceof LocalFeatureHit)
					{
						hits.add(new LocalFeatureHit(newF, hit.getValue(), ind, ((LocalFeatureHit) hit).getHits()));
					}
					else
					{
						hits.add(new FeatureHit(newF, hit.getValue(), ind));
					}
				}
			}
		}
		for(int i = 0; i < table.getDocumentList().getSize(); i++){
			Feature f = Feature.fetchFeature("growth", getPrefix(table, i)+" (Intercept)", Feature.Type.BOOLEAN, plugin);
			hits.add(new FeatureHit(f, Boolean.TRUE, i));
		}
		System.out.println(hits.size() + " Hits added");
		return hits;
	}
	public abstract String getPrefix(FeatureTable table, int index);

	public void addAllFeatures(Collection<Feature> feats){
		features.addAll(feats);
	}
	
	@Override
	public int compareTo(StructuredLevel other)
	{
		return this.toString().compareTo(other.toString());
	}

}
