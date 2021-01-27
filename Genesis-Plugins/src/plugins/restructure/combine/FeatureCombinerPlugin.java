package plugins.restructure.combine;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import plugins.restructure.combine.FeatureCombiner.AndCombiner;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.RestructurePlugin;

public class FeatureCombinerPlugin extends RestructurePlugin
{
	List<FeatureCombiner> combiners = new ArrayList<FeatureCombiner>();
	protected Map<Recipe, Collection<FeatureCombiner>> combinerCache = new HashMap<Recipe, Collection<FeatureCombiner>>();
	protected Map<Recipe, Collection<FeatureHit>> comboHitCache = new HashMap<Recipe, Collection<FeatureHit>>();

	public Collection<FeatureCombiner> getCombiners()
	{
		return combiners;
	}

	protected FeatureCombinerPanel comboUI = null;
	protected Collection<FeatureHit> comboHits = new HashSet<FeatureHit>();

	public FeatureCombinerPlugin()
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


		Collection<FeatureHit> comboHits = getNewHits(original);

		System.out.println("FCP 61: created "+ comboHits.size() + " new feature hits");

		FeatureTable newTable = original.clone();
		newTable.setThreshold(threshold);
		newTable.addFeatureHits(comboHits);

		return newTable;

	}

	@Override
	public String toString()
	{
		return "Combine Features";
	}

	@Override
	public String getOutputName()
	{
		return "combo";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		if (comboUI == null) comboUI = new FeatureCombinerPanel(this);
		return comboUI;
	}

	@Override //FIXME
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new TreeMap<String, String>();

//		System.out.println("FCP 91: generating combiner config settings");
		for (FeatureCombiner combo : comboUI.getCombiners())
		{
			String config = combo.encode();
			settings.put(combo.toString(), config);
		}

		return settings;
	}

	@Override //FIXME
	public void configureFromSettings(Map<String, String> settings)
	{
//		System.out.println("FCP 104: loading combiner config settings");
		combiners.clear();
		for (String key : settings.keySet())
		{
			FeatureCombiner combiner;
			try
			{
				combiner = (FeatureCombiner) FeatureCombiner.decode(settings.get(key));
				combiner.restoreFeatures();
				
				combiners.add(combiner);
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public Collection<FeatureHit> getNewHits(FeatureTable original)
	{
		//make a transformed copy with the intermediate hits as well, to allow nesting
		FeatureTable buildupTable = original.clone(); 
		Collection<FeatureHit> combinedHits = new ArrayList<FeatureHit>();
		System.out.println("FCPlug: combiners="+combiners);
		for (FeatureCombiner combo : combiners)
		{
				Collection<FeatureHit> combinersHits = (Collection<FeatureHit>) combo.combine(buildupTable);
				buildupTable.addFeatureHits(combinersHits);

				if(!combo.isHidden())
				{
					combinedHits.addAll(combinersHits);
				}
		}
		
		return combinedHits;
	}

	public void addCombiner(FeatureCombiner combo)
	{
		if(combiners.contains(combo))
		{
			combiners.get(combiners.indexOf(combo)).setHidden(false);
		}
		else
		{
			combo.setHidden(false);
			combiners.add(combo);
		}
	}

}
