package plugins.features;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.plugin.control.PluginManager;

public class SequentialFeatures extends FeaturePlugin
{

	int mostRecent = 1, leastRecent = 1;
	JTextField mostField = new JTextField("1");
	JTextField leastField = new JTextField("1");
	Component configPanel = createUI();
	
	Map<FeaturePlugin, Boolean> extractors;
	
	private void configureFromUI()
	{
		try
		{
			mostRecent = Integer.parseInt(mostField.getText());
			leastRecent = Integer.parseInt(leastField.getText());
		}
		catch(NumberFormatException e)
		{
			JOptionPane.showMessageDialog(mostField, "Please enter numbers only.", "Configuration Error", JOptionPane.ERROR_MESSAGE);
		}

		extractors = ExtractFeaturesControl.getFeaturePlugins();
		//extractors = PluginManager.getSIDEPluginArrayByType(FeaturePlugin.type);
	}
	
	@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
	{
		//TODO: this doesn't belong here.
		configureFromUI();
		
		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();
		
		for(FeaturePlugin extractor : extractors.keySet())
		{
			if(extractor != this && extractors.get(extractor))
			{
				update.update("extracting features from "+extractor.getOutputName());
				Collection<FeatureHit> extractorHits = extractor.extractFeatureHitsForSubclass(documents, update);
				Collection<FeatureHit> shiftedHits = shiftHits(extractorHits, mostRecent, leastRecent, documents.getSize(), update);
				hits.addAll(shiftedHits);
			}
		}
		
		return hits;
	}

	private Collection<FeatureHit> shiftHits(Collection<FeatureHit> extractorHits, int mostRecent, int leastRecent, int docSize, StatusUpdater update)
	{

		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();
		for(FeatureHit hit : extractorHits)
		{
			int docIndex = hit.getDocumentIndex();
			Feature original = hit.getFeature();
			Feature shifted;

			if(original.getFeatureType() == Feature.Type.NOMINAL)
				shifted	= Feature.fetchFeature("seq", "prev_"+original.getFeatureName(), original.getNominalValues(), this);
			else 
				shifted = Feature.fetchFeature("seq", "prev_"+original.getFeatureName(), original.getFeatureType(), this);
			
			update.update("shifting for instance ", docIndex, docSize);
			{
				for(int n = mostRecent; n <= leastRecent && n+docIndex < docSize; n++)
				{
					
					FeatureHit clone = new FeatureHit(shifted, hit.getValue(), docIndex+n);
					hits.add(clone);
				}
			}
		}
		return hits;
	}

	@Override
	public String getOutputName()
	{
		return "seq";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		return configPanel;
	}

	/**
	 * @return
	 */
	protected Component createUI()
	{
		JPanel panel = new JPanel(new RiverLayout());
		
		panel.add(new JLabel("Extract features from "));
		panel.add(mostField, "br");
		panel.add(new JLabel(" to "));
		panel.add(leastField);
		panel.add(new JLabel(" previous instances"));
		panel.add(new JLabel(" using all other selected extractors"), "br");
		
		return panel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		configureFromUI();
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("mostRecent", ""+mostRecent);
		map.put("leastRecent", ""+leastRecent);
		return map;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		String most = settings.get("mostRecent");
		String least = settings.get("leastRecent");
		mostField.setText(most);
		leastField.setText(least);
		configureFromUI();

	}
	
	@Override
	public String toString()
	{
		return "Sequential Features";
	}

}
