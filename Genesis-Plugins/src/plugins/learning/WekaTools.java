package plugins.learning;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.core.StringEndsWith;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.core.converters.ArffSaver;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.view.util.DefaultMap;
import edu.cmu.side.view.util.Refreshable;
import edu.cmu.side.view.util.SystemMonitorPanel;

public class WekaTools
{

	private static int cacheLimit = 1; // arbitrary!
	private static Map<FeatureTable, FastVector> cache = new HashMap<FeatureTable, FastVector>();
	private static Map<FeatureTable, Map<Feature, Integer>> attributeMaps = new HashMap<FeatureTable, Map<Feature, Integer>>();

	{
		GenesisControl.addListenerToMap(SystemMonitorPanel.GARBAGE_COLLECTION_OBSERVABLE, new Refreshable()
		{

			@Override
			public void refreshPanel()
			{
				WekaTools.invalidateCache();
			}
		});
	}

	// TODO: make weka smart about folds on JustInTimeFeatures
	/**
	 * @param fold
	 * @param foldsMap
	 * @param train
	 * @return
	 */
	// public static boolean[] getMask(int fold, Map<Integer, Integer> foldsMap,
	// int size, boolean test)
	// {
	// boolean[] mask = new boolean[size];
	// for (Integer key : foldsMap.keySet())
	// {
	// mask[key] = foldsMap.get(key).equals(fold);
	// if(test)
	// mask[key] = !mask[key];
	// }
	// return mask;
	// }

	synchronized public static Instances getInstances(FeatureTable table, boolean[] mask)
	{
		DefaultMap<Integer, Integer> highlightMap = new DefaultMap<Integer, Integer>(0);
		for (int i = 0; i < mask.length; i++)
		{
			if (mask[i]) highlightMap.put(i, 1);
		}
		return getInstances(table, 1, highlightMap, false);
	}

	synchronized public static Instances getInstances(FeatureTable table, int fold, Map<Integer, Integer> foldsMap, boolean reverseMap)
	{
		if (!cache.containsKey(table))
		{
			buildAttributeMap(table);
		}
		return getInstances(table, attributeMaps.get(table), fold, foldsMap, reverseMap);
	}

	synchronized public static Map<Feature, Integer> getAttributeMap(FeatureTable table)
	{
		if (!cache.containsKey(table))
		{
			buildAttributeMap(table);
		}
		return attributeMaps.get(table);
	}

	synchronized public static Instances getInstances(FeatureTable table, Map<Feature, Integer> attributeMap, int fold, Map<Integer, Integer> foldsMap,
			boolean reverseMap)
	{
		Instances format = new Instances(table.getName(), cache.get(table), 0);
		return getInstances(table, attributeMap, fold, foldsMap, format, reverseMap);
	}

	synchronized public static void invalidateCache(FeatureTable table)
	{
		cache.remove(table);
		attributeMaps.remove(table);
	}

	synchronized public static void invalidateCache()
	{
		cache.clear();
		attributeMaps.clear();
	}

	/**
	 * @param table
	 * @param attributeMap
	 * @param mask
	 * @param format
	 * @return
	 */
	protected static Instances getInstances(FeatureTable table, Map<Feature, Integer> attributeMap, int fold, Map<Integer, Integer> foldsMap, Instances format,
			boolean reverseMap)
	{

		int trainingSize = 500;
		boolean limit = true;

		Feature.Type t = table.getClassValueType();
		DocumentList documents = table.getDocumentList();
		for (int i = 0; i < documents.getSize(); i++)
		{
			if ((foldsMap.get(i).equals(fold) && !reverseMap) || (!foldsMap.get(i).equals(fold) && reverseMap /*
																											 * &&
																											 * (
																											 * !
																											 * limit
																											 * ||
																											 * (
																											 * i
																											 * <
																											 * trainingSize
																											 * *
																											 * 1.11
																											 * )
																											 */))
			{
				format.add(fillInstance(attributeMap, table, documents, format, t, i));
			}
		}
		format.setClass(format.attribute("CLASS"));
		return format;
	}

	synchronized public static Instances getInstances(FeatureTable train, FeatureTable test, int fold, Map<Integer, Integer> foldsMap, boolean reverseMap)
	{

		if (!cache.containsKey(train))
		{
			buildAttributeMap(train);
		}

		Instances format = new Instances(train.getName(), cache.get(train), 0);
		Instances instances = getInstances(test, attributeMaps.get(train), fold, foldsMap, format, reverseMap);

		return instances;
	}

	synchronized public static Instance getInstance(FeatureTable train, FeatureTable test, Instances format, int i)
	{

		if (!cache.containsKey(train))
		{
			buildAttributeMap(train);
		}

		Instance instance = fillInstance(attributeMaps.get(train), test, test.getDocumentList(), format, test.getClassValueType(), i);

		return instance;
	}

	synchronized public static void shareAttributeMap(FeatureTable authentic, FeatureTable newTable)
	{
		if (!attributeMaps.containsKey(authentic))
		{
			buildAttributeMap(authentic);
		}
		cache.put(newTable, cache.get(authentic));
		attributeMaps.put(newTable, attributeMaps.get(authentic));
	}

	private static void buildAttributeMap(FeatureTable table)
	{
		long start = System.currentTimeMillis();
		Map<Feature, Integer> attributeMap = new HashMap<Feature, Integer>();
		FastVector attributes = new FastVector();
		int index = 0;
		Collection<Feature> featureSet = table.getSortedFeatures();
		double[] empty = new double[featureSet.size() + 1];
		for (Feature f : featureSet)
		{
			Attribute att = null;
			FastVector fv = new FastVector();
			switch (f.getFeatureType())
			{
				case BOOLEAN:
					fv.addElement(Boolean.FALSE.toString());
					fv.addElement(Boolean.TRUE.toString());
					att = new Attribute(f.getFeatureName(), fv);
					break;
				case NOMINAL:
					for (String s : f.getNominalValues())
						fv.addElement(s);
					att = new Attribute(f.getFeatureName(), fv);
					break;
				case NUMERIC:
					att = new Attribute(f.getFeatureName());
					break;
				case STRING:
					att = new Attribute(f.getFeatureName(), (FastVector) null);
					break;
			}
			if (att != null)
			{
				attributes.addElement(att);
				attributeMap.put(f, index++);
			}
		}
		switch (table.getClassValueType())
		{
			case STRING:
			case BOOLEAN:
			case NOMINAL:
				FastVector fv = new FastVector();
				for (String s : table.getLabelArray())
				{
					fv.addElement(s);
				}
				attributes.addElement(new Attribute("CLASS", fv));
				break;
			case NUMERIC:
				attributes.addElement(new Attribute("CLASS"));
				break;
		}

		if (cache.size() > cacheLimit)
		{
			// System.out.println("WekaTools: cache is full - purging!");
			invalidateCache();
		}

		cache.put(table, attributes);
		attributeMaps.put(table, attributeMap);
		// System.out.println("WekaTools: "+(System.currentTimeMillis()-start)+"ms to populate instances for uncached feature table '"+table.getName()+"'");
	}

	/**
	 * Generates Instance objects (weka format) for a document in the corpus.
	 * Actually, these objects already exist, we're just filling the value.
	 * 
	 * @param format
	 *            The Instances object to put this generated Instance in.
	 * @param i
	 *            The document to fill.
	 */
	private static Instance
			fillInstance(Map<Feature, Integer> attributeMap, FeatureTable table, DocumentList documents, Instances format, Feature.Type t, int i)
	{
		Collection<FeatureHit> hits = table.getHitsForDocument(i);
		double[] values = new double[Math.max(format.numAttributes(), attributeMap.size() + 1)];
		for (int j = 0; j < values.length; j++)
			values[j] = 0.0;
		try
		{
			for (FeatureHit hit : hits)
			{
				Feature f = hit.getFeature();
				Integer att = attributeMap.get(f);
				Feature.Type type = f.getFeatureType();

				if (att != null && hit.isValid()) values[att] = getHitValueForFastVector(hit);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		String annotationName = table.getAnnotation();
		if (annotationName != null && table.getAnnotations() != null)
		{
			String[] possibleLabels = table.getLabelArray();
			switch (t)
			{
				case NOMINAL:
				case BOOLEAN:
					for (int j = 0; j < possibleLabels.length; j++)
					{
						if (possibleLabels[j].equals(table.getAnnotations().get(i)))
						{
							values[values.length - 1] = j;
						}
					}
					break;
				case NUMERIC:
					values[values.length - 1] = table.getNumericConvertedClassValue(i, annotationName);
					// values[values.length - 1] =
					// Double.parseDouble(table.getAnnotations().get(i));
					break;
			}
		}
		Instance inst = new SparseInstance(1, values);
		return inst;
	}

	synchronized public static double getHitValueForFastVector(FeatureHit hit)
	{
		Type hitType = hit.getFeature().getFeatureType();
		Object value = hit.getValue();
		switch (hitType)
		{
			case NUMERIC:
				if (value instanceof Integer)
				{
					return 0.0 + (Integer) value;
				}
				else
				{
					return (Double) value;
				}
			case STRING:
			case NOMINAL:
				int index = 0;
				for (String val : hit.getFeature().getNominalValues())
				{
					if (val.equals(value)) { return index; }
					index++;
				}
			case BOOLEAN:
				return 1;
		}
		return 0;
	}

	public static int getCacheLimit()
	{
		return cacheLimit;
	}

	public static void setCacheLimit(int cacheLimit)
	{
		WekaTools.cacheLimit = cacheLimit;
	}

	public static void exportToARFF(FeatureTable ft, File out) throws IOException
	{

		boolean[] mask = new boolean[ft.getSize()];
		for (int i = 0; i < mask.length; i++)
		{
			mask[i] = true;
		}

		Instances instances = WekaTools.getInstances(ft, mask);

		System.out.println("Ensuring that attributes have unique ASCII names...");
		// ensure attribute names are unique ASCII
		for (int j = 0; j < instances.numAttributes(); j++)
		{
			Attribute att = instances.attribute(j);
			String name = att.name();
			String fixedName = encodeForASCII(name);
			instances.renameAttribute(att, fixedName);
		}

		ArffSaver arffSaver = new ArffSaver();
		arffSaver.setInstances(instances);
		arffSaver.setFile(out);
		arffSaver.writeBatch();
	}

	public static String encodeForASCII(String name)
	{
		StringBuilder newName = new StringBuilder();
		for (int i = 0; i < name.length(); i++)
		{
			char c = name.charAt(i);
			if (c >= 128)
			{
				String hexValue = Integer.toHexString(0x10000 | c).substring(1);
				newName.append("\\u");
				newName.append(hexValue);
			}
			else
			{
				newName.append(c);
			}
		}
		String fixedName = newName.toString();
		return fixedName;
	}

}
