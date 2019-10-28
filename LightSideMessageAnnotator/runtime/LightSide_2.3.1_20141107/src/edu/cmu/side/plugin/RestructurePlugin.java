package edu.cmu.side.plugin;

import java.io.Serializable;
import java.util.Map;

import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.plugin.FeatureFetcher.AbstractFeatureFetcherPlugin;
import edu.cmu.side.plugin.control.PluginManager;

/**
 * Filter plugins are used in the Modify Features panel. Given a feature table,
 * along with a list of options specified within a user interface, they return a
 * new set of FeatureHits that will be used in a new FeatureTable.
 * 
 * Remember that filter plugin UI components can't be static because we might have
 * multiples!
 * 
 */
public abstract class RestructurePlugin extends AbstractFeatureFetcherPlugin implements Serializable{

	@Override
	public String getType() {
		return "restructure_table";
	}
	
	public FeatureTable restructure(FeatureTable original, Map<String, String> configuration, int threshold, StatusUpdater progressIndicator)
	{
		synchronized(this)
		{
			this.configureFromSettings(configuration);
			boolean[] allTrue = new boolean[original.getSize()];
			for(int i = 0; i < allTrue.length; i++){ allTrue[i] = true; }
			return restructureWithMaskForSubclass(original, allTrue, threshold, progressIndicator);
		}
	}
	
	public FeatureTable filterTestSet(FeatureTable original, FeatureTable test, Map<String, String> configuration, int threshold, StatusUpdater progressIndicator){
		this.configureFromSettings(configuration);
		return restructureTestSetForSubclass(original, test, threshold, progressIndicator);
	}
	
	protected abstract FeatureTable restructureWithMaskForSubclass(FeatureTable original, boolean[] mask, int threshold, StatusUpdater progressIndicator);

	protected abstract FeatureTable restructureTestSetForSubclass(FeatureTable original, FeatureTable test, int threshold, StatusUpdater progressIndicator);
	
	
}
