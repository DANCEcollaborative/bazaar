package plugins.restructure.filter;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.plugin.RestructurePlugin;

public class FeatureValueFilter extends RestructurePlugin
{
	static class FeatureValue
	{

		public FeatureValue(Feature f, Object v)
		{
			this(f, v, 0, false, false);
		}

		public FeatureValue(Feature f, Object v, int comp, boolean hitsOnly, boolean reverseFilter)
		{
			feature = f;
			value = v;
			comparison = comp;
			filterHits = hitsOnly;
			reverse = reverseFilter;
		}

		Feature feature;
		Object value;
		int comparison;
		boolean filterHits; // versus whole documents
		boolean reverse; // remove instances that *don't* contain this feature

		@Override
		public String toString()
		{
			return (reverse ? "Retain" : "Remove") + " " + (filterHits ? "hits" : "documents") + " where " + feature + "=" + (value == null ? "any" : value);
		}
	}

	Set<FeatureValue> filters = new HashSet<FeatureValue>();

	public Set<FeatureValue> getFilters()
	{
		return filters;
	}

	FeatureValueFilterPanel filterUI = null;

	public FeatureValueFilter()
	{
		// TODO Auto-generated constructor stub
	}

	@Override
	protected FeatureTable restructureWithMaskForSubclass(FeatureTable original, boolean[] mask, int threshold, StatusUpdater progressIndicator)
	{
		return generateFeatureTable(mask, original, threshold, progressIndicator);
	}

	@Override
	protected FeatureTable restructureTestSetForSubclass(FeatureTable original, FeatureTable test, int threshold, StatusUpdater progressIndicator)
	{
		boolean[] mask = new boolean[test.getSize()];
		for (int i = 0; i < mask.length; i++)
			mask[i] = true;
		return generateFeatureTable(mask, test, threshold, progressIndicator);
	}

	private FeatureTable generateFeatureTable(boolean[] mask, FeatureTable original, int threshold, StatusUpdater update)
	{
		int newIndex = 0;
		DocumentList originalDocuments = original.getDocumentList();
		TreeSet<Integer> filteredDocuments = new TreeSet<Integer>();
		TreeSet<Integer> retainedDocuments = new TreeSet<Integer>();
		Map<Integer, Integer> oldIndexToNew = new TreeMap<Integer, Integer>();
		Collection<FeatureHit> filteredHits = new ArrayList<FeatureHit>();
		Collection<FeatureHit> retainedHits = new ArrayList<FeatureHit>();

		int hitCount = 0;
		for (FeatureValue filterItem : filters)
		{
			Feature feature = filterItem.feature;

			Collection<FeatureHit> hitsForFeature = original.getHitsForFeature(feature);
			if(hitsForFeature != null)
			for (FeatureHit hit : hitsForFeature)
			{
				int filterIndex = hit.getDocumentIndex();

				update.update("Filtering " + filterItem.feature.getFeatureName(), hitCount++, hitsForFeature.size());

				if (!mask[filterIndex]) continue;

				if (filterItem.value == null || filterItem.value.equals(hit.getValue()))
				{
					if (filterItem.reverse)
					{
						if (filterItem.filterHits)
							retainedHits.add(hit);
						else
							retainedDocuments.add(filterIndex);
					}
					else
					{
						if (filterItem.filterHits)
							filteredHits.add(hit);
						else
							filteredDocuments.add(filterIndex);
					}
				}
			}
		}

		System.out.println("FVF 100: filtered " + filteredDocuments.size() + " documents and " + filteredHits.size() + " feature hits");
		System.out.println("FVF 100: retained " + retainedDocuments.size() + " documents and " + retainedHits.size() + " feature hits");

		int newSize = originalDocuments.getSize() - filteredDocuments.size();

		List<String> fileNames = new ArrayList<String>();
		Map<String, List<String>> texts = new HashMap<String, List<String>>();
		Map<String, List<String>> annotations = new HashMap<String, List<String>>();
		Collection<FeatureHit> hits = new ArrayList<FeatureHit>();

		Map<String, List<String>> originalTexts = originalDocuments.getCoveredTextList();
		Map<String, List<String>> originalAnnotations = originalDocuments.allAnnotations();

		for (String key : originalTexts.keySet())
		{
			texts.put(key, new ArrayList<String>(newSize));
		}

		for (String key : originalAnnotations.keySet())
		{
			annotations.put(key, new ArrayList<String>(newSize));
		}
		
		for (int originalIndex = 0; originalIndex < originalDocuments.getSize(); originalIndex++)
		{
			update.update("Rebuilding Instance ", newIndex, newSize);
			if (!filteredDocuments.contains(originalIndex) && (retainedDocuments.isEmpty() || retainedDocuments.contains(originalIndex)))
			{
				oldIndexToNew.put(originalIndex, newIndex);

				fileNames.add(originalDocuments.getFilename(originalIndex));

				for (String key : originalTexts.keySet())
				{
					texts.get(key).add(originalTexts.get(key).get(originalIndex));
				}

				for (String key : originalAnnotations.keySet())
				{
					annotations.get(key).add(originalAnnotations.get(key).get(originalIndex));
				}

				for (FeatureHit hit : original.getHitsForDocument(originalIndex))
				{
					FeatureHit newHit;
					if (!filteredHits.contains(hit) && (retainedHits.isEmpty() || retainedHits.contains(hit)))
					{
						if (hit instanceof LocalFeatureHit)
						{
							newHit = new LocalFeatureHit(hit.getFeature(), hit.getValue(), newIndex, ((LocalFeatureHit) hit).getHits());
						}
						else
							newHit = new FeatureHit(hit.getFeature(), hit.getValue(), newIndex);

						hits.add(newHit);
					}
				}

				newIndex++;
			}
		}

		DocumentList newDocs = new DocumentList(fileNames, texts, annotations, original.getAnnotation());
		newDocs.setName("Filtered " + originalDocuments.getName());
		FeatureTable newTable = new FeatureTable(newDocs, hits, original.getThreshold(), original.getAnnotation(), original.getClassValueType());

		return newTable;

	}

	@Override
	public String toString()
	{
		return "Filter Feature Values";
	}

	@Override
	public String getOutputName()
	{
		return "filter-value";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		if (filterUI == null) filterUI = new FeatureValueFilterPanel(this);
		return filterUI;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new TreeMap<String, String>();

		for (FeatureValue filter : filters)
		{
			Feature f = filter.feature;
			String featureID = f.getExtractorPrefix() + "_" + f.getFeatureType() + "_" + f.getFeatureName();

			String config = (filter.filterHits ? "hits" : "docs") + "_" + (filter.reverse ? "retain" : "remove") + "_" + filter.comparison + "_" + filter.value;
			settings.put(featureID, config);
		}

		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		filters.clear();
		for (String featureID : settings.keySet())
		{
			String[] ptf = featureID.split("_", 3);
			Feature f = Feature.fetchFeature(ptf[0], ptf[2], Type.valueOf(ptf[1]));
			if (f == null)
			{
				System.out.println("FVF 242: feature " + featureID + " is not cached.");
			}
			else
			{
				String[] config = settings.get(featureID).split("_", 4);
				Object value = config[3].equals("null") ? null : f.getFeatureType() == Type.NUMERIC ? Double.parseDouble(config[3])
						: f.getFeatureType() == Type.BOOLEAN ? Boolean.parseBoolean(config[3]) : config[3];
				FeatureValue filter = new FeatureValue(f, value, Integer.parseInt(config[2]), config[0].equals("hits"), config[1].equals("retain"));
				filters.add(filter);
			}
		}
	}

}
