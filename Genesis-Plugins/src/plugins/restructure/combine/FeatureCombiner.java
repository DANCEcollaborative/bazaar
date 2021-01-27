package plugins.restructure.combine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit.HitLocation;
import edu.cmu.side.plugin.FeatureFetcher;
import edu.cmu.side.plugin.control.PluginManager;

public abstract class FeatureCombiner implements Comparable<FeatureCombiner>, Serializable
{

	private static final String HIDDEN_KEY = "hidden";

	protected static final String FEATURE_JOINER = " \n\t, ";
	
	protected static FeatureCombinerPlugin plugin = (FeatureCombinerPlugin) PluginManager.getPluginByClassname("plugins.restructure.combine.FeatureCombinerPlugin");
	protected Collection<Feature> comboFeatures;
	protected boolean hidden = false;
	
	@Override
	public int compareTo(FeatureCombiner foo)
	{
		return toString().compareTo(foo.toString());
	}
	
	protected Feature getFeature()
	{
		return Feature.fetchFeature("combo", toString(), Type.BOOLEAN, plugin);
	}
	
	public String encode()
	{
		StringBuilder stringer = new StringBuilder();
		stringer.append(getClass().getName());
		stringer.append(":");
		stringer.append(hidden?HIDDEN_KEY:"");
		stringer.append(":");
		
		for(Feature f : comboFeatures)
		{
			stringer.append(f.encode());
			stringer.append(FEATURE_JOINER);
		}
		return stringer.toString();
	}

	public static FeatureCombiner decode(String encoded) throws Exception
	{
		String[] classSplit = encoded.split(":", 3);
		
		Collection<Feature> features = new ArrayList<Feature>();
		
		for(String encodedFeature : classSplit[2].split(FEATURE_JOINER))
		{
			if(!encodedFeature.isEmpty())
			{
				features.add(Feature.fetchFeature(encodedFeature));
			}
		}
		
		Class<Collection> c = Collection.class;
		FeatureCombiner combo = (FeatureCombiner) Class.forName(classSplit[0]).getConstructor(c).newInstance(features);
		combo.setHidden(classSplit[1].equals(HIDDEN_KEY));
		return combo;
	}
	
	public void restoreFeatures()
	{
		Collection<Feature> newFeatures = new ArrayList<Feature>();
		for(Feature original : comboFeatures)
		{
			Feature fresh = Feature.fetchFeature(original.getExtractorPrefix(), original.getFeatureName(), original.getFeatureType());
			newFeatures.add(fresh);
		}
		comboFeatures = newFeatures;
	}
	
	protected Collection<? extends FeatureHit> recombineHits(Collection<FeatureHit> hits, Feature feature)
	{
		Map<Integer, LocalFeatureHit> docHits = new HashMap<Integer, LocalFeatureHit>();
		
		for(FeatureHit h : hits)
		{
			int docIndex = h.getDocumentIndex();
			if(!docHits.containsKey(docIndex))
			{
				LocalFeatureHit andHit;
				if(h instanceof LocalFeatureHit)
				{
					LocalFeatureHit lh = (LocalFeatureHit)h;
					andHit = new LocalFeatureHit(feature, Boolean.TRUE, docIndex, new ArrayList<HitLocation>(lh.getHits()));
				}
				else
				{
					andHit = new LocalFeatureHit(feature, Boolean.TRUE, docIndex, new ArrayList<HitLocation>(0));
				}
				docHits.put(docIndex, andHit);
			}
			if(h instanceof LocalFeatureHit)
			{
				LocalFeatureHit lh = (LocalFeatureHit)h;
				docHits.get(docIndex).getHits().addAll(lh.getHits());
			}
			
		}
		
		return docHits.values();
	}

	public Collection<? extends FeatureHit> getOrCreateHitsForFeature(FeatureTable table, Feature f)
	{
		if(table.getFeatureSet().contains(f))
			return table.getHitsForFeature(f);
		else if(f.getExtractorPrefix().equals("combo"))
		{
			for(FeatureCombiner combiner : plugin.combiners)
			{
				if(combiner.getFeature().equals(f))
				{
					return combiner.combine(table);
				}
			}
		}
		System.out.println("Can't find feature "+f+" to combine with");
		return new ArrayList<FeatureHit>();
		
	}

	public abstract Collection<? extends FeatureHit> combine(FeatureTable table);
	
	public static class AndCombiner extends FeatureCombiner
	{
		
		public AndCombiner(Collection<Feature> features)
		{
			comboFeatures = features;
		}
		
		@Override
		public String toString()
		{
			return "AND"+comboFeatures;
		}
		
		@Override
		public Collection<? extends FeatureHit> combine(FeatureTable table)
		{
			Set<FeatureHit> hits = new HashSet<FeatureHit>();
			Set<Integer> hitDocs = new HashSet<Integer>();
			boolean firstFeature = true;
			Feature andComboFeature = getFeature();
			
			for(Feature f : comboFeatures)
			{
				
				Collection<Integer> featureDocs = new ArrayList<Integer>();
				Collection<? extends FeatureHit> hitsForFeature = getOrCreateHitsForFeature(table, f);
				
				if(hitsForFeature != null)
				for(FeatureHit h : hitsForFeature)
				{
					featureDocs.add(h.getDocumentIndex());
				}
				
				if(firstFeature)
				{
					hitDocs.addAll(featureDocs);
					firstFeature = false;
				}
				else
				{
					hitDocs.retainAll(featureDocs);
				}
			}
			
			for(Feature f : comboFeatures)
			{
				Collection<? extends FeatureHit> hitsForFeature = getOrCreateHitsForFeature(table, f);

				if(hitsForFeature != null)
				for(FeatureHit h : hitsForFeature)
				{
					if(hitDocs.contains(h.getDocumentIndex()))
					{
						hits.add(h);
					}
				}
			}

			return recombineHits(hits, andComboFeature);
		}

		
	}
	
	public static class NotCombiner extends FeatureCombiner
	{
		
		public NotCombiner(Collection<Feature> notFeatures)
		{
			this.comboFeatures = notFeatures;
		}
		
		@Override
		public String toString()
		{
			return "NOT"+comboFeatures;
		}
		
		@Override
		public Collection<? extends FeatureHit> combine(FeatureTable table)
		{
			Set<Integer> docHits = new HashSet<Integer>();
			
			Feature notComboFeature = Feature.fetchFeature("combo", toString(), Type.BOOLEAN, plugin);
			
			
			for(Feature f : comboFeatures)
			{
				Collection<? extends FeatureHit> hitsForFeature = getOrCreateHitsForFeature(table, f);

				if(hitsForFeature != null)
				for(FeatureHit h : hitsForFeature)
				{
					docHits.add(h.getDocumentIndex());
				}
			}
			
			Collection<FeatureHit> hits = new ArrayList<FeatureHit>();
			for(int i = 0; i < table.getSize(); i++)
			{
				if(!docHits.contains(i))
				{
					hits.add(new FeatureHit(notComboFeature, Boolean.TRUE, i));
				}
			}
			return hits;
		}
		
	}
	

	public static class OrCombiner extends FeatureCombiner
	{
		
		public OrCombiner(Collection<Feature> features)
		{
			comboFeatures = features;
		}
		
		@Override
		public String toString()
		{
			return "OR"+comboFeatures;
		}
		
		@Override
		public Collection<? extends FeatureHit> combine(FeatureTable table)
		{
			Set<FeatureHit> hits = new HashSet<FeatureHit>();
			
			Feature orComboFeature = Feature.fetchFeature("combo", toString(), Type.BOOLEAN, plugin);
			
			for(Feature f : comboFeatures)
			{
				Collection<? extends FeatureHit> hitsForFeature = getOrCreateHitsForFeature(table, f);;

				if(hitsForFeature != null)
				hits.addAll(hitsForFeature);
			}

			return recombineHits(hits, orComboFeature);
		}
		
	}


	public boolean isHidden()
	{
		return hidden;
	}

	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}
}
