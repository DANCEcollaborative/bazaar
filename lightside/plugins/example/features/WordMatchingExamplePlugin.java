package example.features;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;

/*Package as a JAR, compiled against lightside, in your plugins folder,
  and add an entry to config.xml to point to your new plugin.*/

public class WordMatchingExamplePlugin extends FeaturePlugin
{
	// our toy example will extract hits that match this string exactly
	String matchString = "awesome";

	// this component will be displayed in the config pane
	Component configUI;

	/**
	 * @param documents the document list to extract features from
	 * @param update a hook back to the UI to provide progress updates
	 */
	@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
	{
		// all the feature hits to return for this document list.
		Collection<FeatureHit> hits = new ArrayList<FeatureHit>();

		// we want to maintain just one feature hit per document, with multiple
		// "hit locations" within each doc
		Map<Feature, FeatureHit> documentHits = new HashMap<Feature, FeatureHit>();

		// this is a map of document text-lists, keyed by column name
		Map<String, List<String>> coveredTextList = documents.getCoveredTextList();

		// iterate through each document
		for (int i = 0; i < documents.getSize(); i++)
		{
			//keep the user informed
			update.update("extracting from document", i, documents.getSize());
			
			// extract features from each text column
			for (String column : coveredTextList.keySet())
			{
				// text content for this column
				List<String> textField = coveredTextList.get(column);

				// text content for this column in document i
				String text = textField.get(i).toLowerCase();

				//starting index of the string we're matching
				int matchIndex = -1;
				
				// TODO: insert your clever extraction logic here.
				while ((matchIndex = text.indexOf(matchString, matchIndex+1)) >= 0)
				{
					// if this doc list differentiates text columns, ensure
					// unique feature names per column.
					String featureName = documents.getTextFeatureName(matchString, column);

					// always get Feature objects this way.
					// Feature.fetchFeature(extractor prefix, featureName, featureType, featureExtractor)
					Feature feature = Feature.fetchFeature("match", featureName, Type.BOOLEAN, this);

					// update the existing feature hit for this document
					if (documentHits.containsKey(feature))
					{
						LocalFeatureHit localHit = (LocalFeatureHit) documentHits.get(feature);

						// for later visualization, we keep track of the column,
						// start and end indices of each local feature hit.
						localHit.addHit(column, matchIndex, matchIndex + matchString.length());
					}
					// create a new feature hit for this document
					else
					{
						// LocalFeatureHit(feature, featureValue, docIndex, textColumn, startIndex (within column text), endIndex)
						LocalFeatureHit hit = new LocalFeatureHit(feature, Boolean.TRUE, i, column, matchIndex, matchIndex + matchString.length());
						documentHits.put(feature, hit);
					}

				}

			}
			// add the unique per-feature hits for this document to the big hitlist
			hits.addAll(documentHits.values());
			//clear the per-document cache
			documentHits.clear();
		}

		return hits;
	}

	/**
	 * @return a user interface component that can update the plugin settings
	 */
	@Override
	protected Component getConfigurationUIForSubclass()
	{
		if (configUI == null) configUI = makeConfigUI();
		return configUI;
	}

	/**
	 * @return a map of strings representing the plugin configuration settings
	 */
	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new HashMap<String, String>();
		settings.put("matchString", matchString);
		return settings;
	}

	/**
	 * @param settings a map of strings from which the plugin should update its configuration 
	 * used during model building as well as for saving feature table recipes
	 */
	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		matchString = settings.get("matchString");
	}

	/**
	 * @return a unique short name for this plugin
	 */
	@Override
	public String getOutputName()
	{
		return "match";
	}

	/**
	 * @return the plugin name that will appear in the LightSIDE UI
	 */
	public String toString()
	{
		return "String Match Plugin Example";
	}

	/**
	 * create the configuration UI, and hook it in to the plugin settings.
	 * @return the newly-created component that will serve as the configuration UI.
	 */
	private Component makeConfigUI()
	{
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final JTextField matchField = new JTextField(matchString, 20);

		matchField.addCaretListener(new CaretListener()
		{
			@Override
			public void caretUpdate(CaretEvent update)
			{
				//update the match string as the user types.
				matchString = matchField.getText();
			}
		});

		panel.add(new JLabel("Match String:"));
		panel.add(matchField);

		return panel;
	}

}
