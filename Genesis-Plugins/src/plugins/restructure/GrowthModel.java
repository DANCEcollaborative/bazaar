package plugins.restructure;

import java.awt.Component;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import plugins.restructure.multi.model.StructuredLevel;
import plugins.restructure.multi.view.MultilevelPanel;

import com.yerihyo.yeritools.text.Base64;

import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.FeatureFetcher;
import edu.cmu.side.plugin.RestructurePlugin;

public class GrowthModel extends RestructurePlugin {

	private static Map<FeatureTable, Collection<StructuredLevel>> levels = new HashMap<FeatureTable, Collection<StructuredLevel>>();
	
	private Collection<StructuredLevel> defaultLevels = new ArrayList<StructuredLevel>();
	
	static StructuredLevel levelA;
	static StructuredLevel levelB;
	
	public static Collection<StructuredLevel> getFinalLevels(){
		if(RestructureTablesControl.hasHighlightedFeatureTable()){
			return levels.get(RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable());        		
		}else return new ArrayList<StructuredLevel>();
	}

	public static void deleteLevel(StructuredLevel l){
		getFinalLevels().remove(l);
	}
	
	public static void addFinalLevel(StructuredLevel level){
		if(RestructureTablesControl.hasHighlightedFeatureTable()){
			levels.get(RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable()).add(level);
		}
	}

	MultilevelPanel panel = new MultilevelPanel(this);

	public static void setLevelA(StructuredLevel a){
		levelA = a;
	}
	
	public static void setLevelB(StructuredLevel b){
		levelB = b;
	}
	
	public static StructuredLevel getLevelA(){
		return levelA;
	}
	
	public static StructuredLevel getLevelB(){
		return levelB;
	}
	
	@Override
	protected FeatureTable restructureWithMaskForSubclass(FeatureTable original,
			boolean[] mask, int threshold, StatusUpdater progressIndicator) {

		Collection<FeatureHit> hits = new HashSet<FeatureHit>();
		for(int i = 0; i < original.getDocumentList().getSize(); i++){
			hits.addAll(original.getHitsForDocument(i));
		}
		Collection<StructuredLevel> currentLevels;
		if(levels.containsKey(original))
		{
			currentLevels = levels.get(original);
		}
		else
		{
			currentLevels = defaultLevels;
		}
		for(StructuredLevel level : currentLevels)
		{
			hits.addAll(level.model(original, this));
		}
		
		return new FeatureTable(original.getDocumentList(), hits, threshold, original.getAnnotation(), original.getClassValueType());
	}

	@Override
	protected FeatureTable restructureTestSetForSubclass(FeatureTable original,
			FeatureTable test, int threshold, StatusUpdater progressIndicator) {

		Collection<FeatureHit> hits = new HashSet<FeatureHit>();
		//TODO: actually restructure
		return new FeatureTable(test.getDocumentList(), hits, threshold, test.getAnnotation(), test.getClassValueType());

	}

	@Override
	public String toString(){
		return "Multilevel Modeling";
	}

	@Override
	public String getOutputName() {
		return "growth";
	}

	@Override
	protected Component getConfigurationUIForSubclass() {
		if(RestructureTablesControl.hasHighlightedFeatureTable()){
			FeatureTable t = RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable();
			if (!levels.containsKey(t)){
				levels.put(t, new ArrayList<StructuredLevel>());
			}
		}
		panel.refreshPanel();
		return panel;
	}
	
	public void addFromUI(FeatureTable table, Collection<StructuredLevel> selectLevels, Collection<FeatureSource> selectFeatures){
		Collection<Feature> features = new TreeSet<Feature>();
//		System.out.println("Starting add GM115");
		for(FeatureSource s : selectFeatures)
		{
			int orig = features.size();
			for(Feature f : table.getFeatureSet())
			{
					if(s.matches(f))
					{
						features.add(f);
					}
//				System.out.println((features.size()-orig) + " From plugin " + value);
			}
		}
		for(StructuredLevel level : selectLevels){
//			System.out.println("Adding level " + level);
			level.addAllFeatures(features);
			addFinalLevel(level);
		}
	}

	@Override
	public Map<String, String> generateConfigurationSettings() 
	{
		TreeMap<String, String> settings = new TreeMap<String, String>();
		
		//FIXME: don't cheat. XMLification (or something) goes here

		String levelString = Base64.encodeObject((Serializable) levels.get(RestructureTablesControl.getHighlightedFeatureTableRecipe().getTrainingTable()));
		settings.put("levels", levelString);
		
		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) 
	{
		Collection<StructuredLevel> currentLevels = (Collection<StructuredLevel>) Base64.decodeToObject(settings.get("levels"));
		defaultLevels = currentLevels;
	}
	
	public static interface FeatureSource extends Comparable
	{
		public boolean matches(Feature f);
	}
	
	public static class PluginFeatureSource implements FeatureSource
	{
		public FeatureFetcher extractor;

		@Override
		public boolean matches(Feature f)
		{
			return f.getExtractor().equals(extractor);
		}

		@Override
		public String toString()
		{
			return " All "+extractor.toString();
		}

		public PluginFeatureSource(FeatureFetcher extractor)
		{
			this.extractor = extractor;
		}

		@Override
		public int compareTo(Object o)
		{
			return toString().compareTo(o.toString());
		}
	}
	
	public static class SingleFeatureSource implements FeatureSource
	{
		Feature feature;
		public SingleFeatureSource(Feature feature)
		{
			this.feature = feature;
		}
		
		@Override
		public boolean matches(Feature f)
		{
			return f.equals(feature);
		}
		
		@Override
		public String toString()
		{
			return feature.toString();
		}

		@Override
		public int compareTo(Object o)
		{
			return toString().compareTo(o.toString());
		}
	}
}