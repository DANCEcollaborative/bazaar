package plugins.learning;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;

import se.datadosen.component.RiverLayout;
import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializedObject;

import com.yerihyo.yeritools.text.Base64;

import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.plugin.LearningPlugin;
import edu.cmu.side.plugin.ParallelLearningPlugin;

public abstract class WekaCore extends ParallelLearningPlugin
{

	protected Feature.Type visibleType;

	protected Classifier classifier;
	
	static boolean caching = false;
	
	Map<Integer, Classifier> classifierCache = new HashMap<Integer, Classifier>();

	protected JPanel panel;
	protected JPanel nominal = new JPanel(new RiverLayout());
	protected JPanel numeric = new JPanel(new RiverLayout());

	public WekaCore(){
		panel = new JPanel(new RiverLayout());
		nominal.add("left", new JLabel("This classifier doesn't handle nominal class values."));
		numeric.add("left", new JLabel("This classifier doesn't handle numeric class values."));
	}


	@Override
	protected Component getConfigurationUIForSubclass() {
		panel.removeAll();
		if(BuildModelControl.hasHighlightedFeatureTableRecipe()){
			FeatureTable ft = BuildModelControl.getHighlightedFeatureTableRecipe().getTrainingTable();
			visibleType = ft.getClassValueType();
			switch(visibleType){
			case NOMINAL:
			case BOOLEAN:
			case STRING:
				panel.add(BorderLayout.CENTER, nominal);
				break;
			case NUMERIC:
				panel.add(BorderLayout.CENTER, numeric);
				break;
			}
		}
		return panel;
	}
	
	public Classifier getClassifier()
	{
		return classifier;
	}

	// TODO: make weka smart about folds on JustInTimeFeatures
	@Override
	protected void trainAgainstFold(FeatureTable table, int fold, Map<Integer, Integer> foldsMap, StatusUpdater progressIndicator) throws Exception
	{
		Instances instances = WekaTools.getInstances(table, fold, foldsMap, true);
		classifier.buildClassifier(instances);
	}

	@Override
	public String getLongDescriptionString(){
		return classifier.toString();
	}

	@Override
	public Object prepareToPredict(FeatureTable originalData, FeatureTable newData, int fold, Map<Integer, Integer> foldsMap)
	{
		boolean[] mask = new boolean[newData.getSize()];
		
		Instances inst = WekaTools.getInstances(originalData, newData, fold, foldsMap, false);
		return inst;
	}

	@Override
	public double[] predictLabel(int i, FeatureTable originalData, FeatureTable newData, Object predictionContext) throws Exception
	{
		Instances format = (Instances) predictionContext;
		Instance instance = WekaTools.getInstance(originalData, newData, format, i);
		instance.setDataset(format);
		try
		{
			return classifier.distributionForInstance(instance);
		}
		catch(Exception e)
		{
			double[] distro = new double[originalData.getLabelArray().length];
			int label = (int) classifier.classifyInstance(instance);
			distro[label] = 1.0;
			return distro;
		}
	}

	@Override
	public double predictNumeric(int i, FeatureTable originalData, FeatureTable newData, Object predictionContext) throws Exception
	{
		Instances format = (Instances) predictionContext;
		Instance instance = WekaTools.getInstance(originalData, newData, format, i);
		instance.setDataset(format);
		return classifier.classifyInstance(instance);
	}

	@Override
	public String getOutputName() {
		return "weka";
	}

	@Override
	public  Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> config = new TreeMap<String, String>();
		if(classifier != null)
		{
			String classifierString = Base64.encodeObject((Serializable) classifier);
			config.put("classifier", classifierString);
		}
		return config;
	}

	@Override
	public void loadClassifierFromSettings(Map<String, String> settings)
	{
		if(settings.containsKey("classifier"))
		{
			long start = System.currentTimeMillis();
			logger.fine("WekaCore: loading classifier from settings... ");
			String classifierValue = settings.get("classifier");
			Integer classifierKey = classifierValue.hashCode();
			
			if(caching)
			{
				if(classifierCache.containsKey(classifierKey))
				{
					logger.fine("WekaCore: Found classifier in cache!");
					classifier = classifierCache.get(classifierKey);
				}
				else
				{
					classifier = (Classifier)Base64.decodeToObject(classifierValue);
					classifierCache.put(classifierKey, classifier);
				}
			}
			else
				classifier = (Classifier)Base64.decodeToObject(classifierValue);
			
			logger.fine("WekaCore: done loading classifier in "+(System.currentTimeMillis() - start) +"ms");
			
		}
		else
		{
			// Hiding this for now - is it necessary?
//			System.err.println("No classifier stored in settings!");
		}
	}

	@Override
	public  void configureFromSettings(Map<String, String> settings)
	{

	}

	static public void setCaching(boolean cache)
	{
		caching = cache;
	}
	static public boolean isCaching()
	{
		return caching;
	}
	
	public void setVisibleType( Feature.Type type ){
		
		visibleType = type;
		
	}

	public boolean supportsClassType(Feature.Type type)
	{
		if(classifier == null)
			return true;
		
		Capabilities capabilities = classifier.getCapabilities();
		switch(type)
		{
			case NUMERIC:
				return capabilities.handles(Capability.NUMERIC_CLASS);
			case NOMINAL:
				return capabilities.handles(Capability.NOMINAL_CLASS);
			case BOOLEAN:
				return capabilities.handles(Capability.BINARY_CLASS);
			default:
				return true;
		}
	}

	@Override
	public boolean settingsMatch(Map<String, String> settingsA, Map<String, String> settingsB)
	{
		for(String key : settingsA.keySet())
		{
			if(!key.equals("classifier") && !settingsA.get(key).equals(settingsB.get(key)))
				return false;
		}

		for(String key : settingsB.keySet())
		{
			if(!key.equals("classifier") && !settingsB.get(key).equals(settingsA.get(key)))
				return false;
		}
		return true;
	}

	
	@Override
	public LearningPlugin clone()
	{
		WekaCore clonedLearner;
		try
		{
			clonedLearner = getClass().newInstance();
			clonedLearner.configureFromSettings(this.generateConfigurationSettings());
			clonedLearner.classifier = (Classifier)new SerializedObject(this.classifier).getObject();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Can't clone learner "+this, e);
		}
		return clonedLearner;
	}
}

