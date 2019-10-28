package edu.cmu.side.model.feature;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import edu.cmu.side.plugin.FeatureFetcher;
import edu.cmu.side.plugin.control.PluginManager;
import edu.stanford.nlp.util.StringUtils;

public class Feature implements Serializable, Comparable<Feature>
{
	private static final String FEATURE_JOIN = "_";
	private static final String LABEL_JOIN = "Nominal Label: \t";
	private static final long serialVersionUID = -7567947807818964630L;

	/**
	 * 
	 * FeatureHits for a given Feature must be assigned values corresponding to these types.
	 * NOMINAL Features may only be assigned the values enumerated in the Feature's nominalValues field.
	 *
	 */
	public enum Type 
	{
		NUMERIC(Number.class), BOOLEAN(Boolean.class), STRING(String.class), NOMINAL(String.class);
		
		private Class<?> classForType;
		Type(Class<?> c){classForType = c;}
		
		public Class<?> getClassForType()
		{return classForType;}
	};
	
	protected String featureName;
	protected String extractorPrefix;
	protected Feature.Type featureType;
	protected Collection<String> nominalValues;
	
	protected transient FeatureFetcher extractor;
	
	private static Map<String, Map<String, Feature>> featureCache = new TreeMap<String, Map<String, Feature>>();
	
	public static Feature fetchFeature(String prefix, String name, Feature.Type type)
	{
		String featureName = name+FEATURE_JOIN+type;
		if(featureCache.containsKey(prefix) && featureCache.get(prefix).containsKey(featureName))
		{
			return featureCache.get(prefix).get(featureName);
		}
		return null;
	}
	
	public String encode()
	{
		String typeString = getFeatureType().toString();
		if(getFeatureType() == Type.NOMINAL)
		{
			typeString+=LABEL_JOIN+StringUtils.join(getNominalValues(), LABEL_JOIN);
		}
		return getExtractor().getClass().getName()+FEATURE_JOIN+getExtractorPrefix()+FEATURE_JOIN+getFeatureName()+FEATURE_JOIN+typeString;
	}
	
	public static Feature fetchFeature(String encoded) throws IllegalStateException
	{
//		System.out.println("fetching "+encoded);
		String[] split = encoded.split(FEATURE_JOIN, 3);

		String extractorName = split[0];
		String prefix = split[1];
		String nameAndType = split[2];
		
		if(featureCache.containsKey(prefix) && featureCache.get(prefix).containsKey(nameAndType))
		{
			return featureCache.get(prefix).get(nameAndType);
		}
		else
		{
//			System.out.println("creating "+extractorName + " " + nameAndType);
			FeatureFetcher extractor = (FeatureFetcher) PluginManager.getPluginByClassname(extractorName);
			int typeSplit = nameAndType.lastIndexOf(FEATURE_JOIN);
			String featureName = nameAndType.substring(0, typeSplit);
			String typeString = nameAndType.substring(typeSplit+FEATURE_JOIN.length());
			if(typeString.startsWith(Type.NOMINAL.toString()))
			{
				typeString = typeString.substring(Type.NOMINAL.toString().length()+LABEL_JOIN.length());
				String[] labels = typeString.split(LABEL_JOIN);
				
				return fetchFeature(prefix, featureName, Arrays.asList(labels), extractor);
			}
			else
			{
				return fetchFeature(prefix, featureName, Type.valueOf(typeString), extractor);
			}
		}
		
		
	}
	
	public static Feature fetchFeature(String prefix, String name, Feature.Type type, FeatureFetcher extractorPlugin){
		if(!featureCache.containsKey(prefix))
		{
			synchronized(Feature.class)
			{
				if(!featureCache.containsKey(prefix))
					featureCache.put(prefix, new HashMap<String, Feature>(100000));
			}
		}
		if(!featureCache.get(prefix).containsKey(name+FEATURE_JOIN+type.toString()))
		{
			synchronized(Feature.class)
			{
				if(!featureCache.get(prefix).containsKey(name+FEATURE_JOIN+type.toString()))
				{
					Feature newFeat = new Feature(prefix, name, type, extractorPlugin);
					featureCache.get(prefix).put(name+FEATURE_JOIN+type.toString(), newFeat);
					return newFeat;
				}
			}
		}
		return featureCache.get(prefix).get(name+FEATURE_JOIN+type.toString());
		
	}
	
	public static Feature fetchFeature(String prefix, String name, Collection<String> nominals, FeatureFetcher extractorPlugin){
		Feature f = fetchFeature(prefix, name, Feature.Type.NOMINAL, extractorPlugin);
		if(f.nominalValues == null){
			f.setNominalValues(nominals);
		}
		return f;
	}
	
	public void setNominalValues(Collection<String> nom){
		nominalValues = nom;
	}
	
	/**
	 * Construct a String, Boolean, or Numeric Feature.
	 * @param prefix the unique prefix for the extractor that produces this feature
	 * @param name a prefix-unique name for this feature
	 * @param type a hint for feature handling - Feature.Type.NUMERIC, BOOLEAN, or STRING
	 */
	private Feature(String prefix, String name, Feature.Type type, FeatureFetcher extractorPlugin)
	{
		this.featureName = name;
		this.extractorPrefix = prefix;
		this.featureType = type;
		this.extractor = extractorPlugin;
	}
	
	protected Feature(){
		this("none","none",Type.BOOLEAN, null);
	}
	/**
	 * Construct a Nominal (enumerated type) Feature.
	 * @param prefix the unique prefix for the the extractor that produces this feature
	 * @param name a prefix-unique name for this feature
	 * @param nominals the possible values this Feature can express
	 */
	private Feature(String prefix, String name, Collection<String> nominals, FeatureFetcher extractorPlugin)
	{
		this.featureName = name;
		this.extractorPrefix = prefix;
		this.featureType = Feature.Type.NOMINAL;
		this.nominalValues = nominals;
	}
	
	@Override
	public String toString()
	{
		return featureName;
	}
	
	public Feature clone(String prefix){
		if (featureType == Feature.Type.NOMINAL)
			return new Feature(extractorPrefix, prefix+featureName, nominalValues, extractor);
		return new Feature(extractorPrefix, prefix+featureName, featureType, extractor);
	}
	
	/**
	 * @return the prefix-unique name for this feature.
	 */
	public String getFeatureName()
	{
		return featureName;
	}
	
	/**
	 * 
	 * @return the extractor prefix - indicates which extractor plugin instantiated this feature.
	 */
	public String getExtractorPrefix()
	{
		return extractorPrefix;
	}

	/**
	 * 
	 * @return Number, Boolean, String, or Nominal
	 */
	public Feature.Type getFeatureType()
	{
		return featureType;
	}
	
	/**
	 * 
	 * @return the possible values of this nominal feature.
	 */
	public Collection<String> getNominalValues()
	{
		if(featureType != Feature.Type.NOMINAL)
			throw new IllegalArgumentException(this+" is not a nominal feature.");
		return nominalValues;
	}

	@Override
	public int compareTo(Feature o) {
		if(extractorPrefix.equals(o.extractorPrefix)){
			return featureName.compareTo(o.featureName);
		}else return extractorPrefix.compareTo(o.extractorPrefix);
	}
	
	@Override
	public int hashCode()
	{
		return (this.extractorPrefix+this.featureName).hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return (o instanceof Feature)&&(this.compareTo((Feature)o)==0)&&this.featureType.equals(((Feature)o).featureType);
	}
	
	//For prediction, newly extracted features may not have some nominalvalues as original one
	public static Feature reconcile(Feature a, Feature b){
		if (!a.equals(b))
			throw new IllegalStateException(a + " is different from " + b);
		if (b.getFeatureType() == Feature.Type.NOMINAL) 
			b.setNominalValues(a.getNominalValues());
		return b;
	}

	public FeatureFetcher getExtractor()
	{
		return extractor;
	}
	
	public boolean isTokenized()
	{
		return extractor.isTokenized(this);
	}
	

	private void writeObject(ObjectOutputStream out) throws IOException
	{
		out.writeObject(featureName);
		out.writeObject(extractorPrefix);
		out.writeObject(featureType);
		out.writeObject(nominalValues);
		out.writeObject(extractor.getClass().getName());
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
	{
		featureName = (String) in.readObject();
		extractorPrefix = (String) in.readObject();;
		featureType = (Type) in.readObject();;
		nominalValues = (Collection<String>) in.readObject();
		
		extractor = (FeatureFetcher) PluginManager.getPluginByClassname((String) in.readObject());
	}

}
