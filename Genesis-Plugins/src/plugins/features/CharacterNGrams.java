package plugins.features;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.view.util.RangeSlider;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.plugin.ParallelFeaturePlugin;

public class CharacterNGrams extends ParallelFeaturePlugin
{
	public static final String CHAR_FEATURE_SUFFIX = "__char";
	protected int minGram = 3;
	protected int maxGram = 4;

	protected boolean includePunctuation = true;
	protected boolean stayWithinWords = false;
	
	protected boolean trackHitLocation = false;

	protected Pattern punctFinderPattern = Pattern.compile("([\\p{Punct}\\s]+)");
	protected Pattern punctSplitPattern = Pattern.compile("([^\\p{Punct}\\s]+)");
	protected Pattern spaceSplitPattern = Pattern.compile("(\\S+)");

	// this component will be displayed in the config pane
	Component configUI;

	public CharacterNGrams()
	{
		this(3, 4, true, false);
	}
	
	//Not the normal constructor, but available for chefs.
	public CharacterNGrams(int min, int max, boolean punct, boolean within){
		minGram = min;
		maxGram = max;
		includePunctuation = punct;
		stayWithinWords = within;
	}

	/**
	 * @param documents
	 *            the document list to extract features from
	 * @param update
	 *            a hook back to the UI to provide progress updates
	 */
	/*@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
	{
//		System.out.printf("CNG 68: min %d max %d\n", minGram, maxGram);
		// all the feature hits to return for this document list.
		Collection<FeatureHit> hits = new ArrayList<FeatureHit>();
		// iterate through each document
		int size = documents.getSize();
		for (int i = 0; i < size; i++)
		{
			// keep the user informed
			if ((i+1) % 50 == 0 || i == size-1) update.update("Extracting Character N-Grams", i+1, size);

			if(halt)
				throw new RuntimeException("User Canceled");
		

			Collection<FeatureHit> localHitValues = extractFeatureHitsFromDocument(documents, i);
			hits.addAll(localHitValues);
			// clear the per-document cache
			documentHits.clear();
		}
		update.update("Finished Character N-Gram extraction");

		return hits;
	}*/

	@Override
	public Collection<FeatureHit> extractFeatureHitsFromDocument(DocumentList documents, int i)
	{
		// we want to maintain just one feature hit per document, with multiple
		// "hit locations" within each doc
		Map<Feature, FeatureHit> documentHits = new HashMap<Feature, FeatureHit>();

		// this is a map of document text-lists, keyed by column name
		Map<String, List<String>> coveredTextList = documents.getCoveredTextList();
		
		// extract features from each text column
		for (String column : coveredTextList.keySet())
		{
			// text content for this column
			List<String> textField = coveredTextList.get(column);

			// text content for this column in document i
			String text = textField.get(i).toLowerCase();

			if (stayWithinWords)
				extractWithinWords(documents, documentHits, i, column, text);
			else
				extractAcrossText(documents, documentHits, i, column, text);

		}
		// add the unique per-feature hits for this document to the big
		// hitlist
		Collection<FeatureHit> localHitValues = documentHits.values();
		return localHitValues;
	}

	protected void extractWithinWords(DocumentList documents, Map<Feature, FeatureHit> documentHits, int i, String column, String text)
	{
		Pattern splitPattern = includePunctuation ? spaceSplitPattern : punctSplitPattern;

		Matcher matcher = splitPattern.matcher(text);
		
		while (matcher.find())
		{
			String word = matcher.group();
			
			for (int c = 0; c < (word.length() - maxGram)+1; c++)
			{
				for (int n = minGram; n < maxGram+1; n++)
				{
					extractSingleHit(documents, documentHits, i, column, text, matcher.start() + c, n);
				}
			}

		}
	}

	protected void extractAcrossText(DocumentList documents, Map<Feature, FeatureHit> documentHits, int i, String column, String text)
	{

		if(!includePunctuation)
		{
			//find all punctuation, replace it with a space. This will slightly screw up the highlighting in explore results.
			Matcher matcher = punctFinderPattern.matcher(text);
			text = matcher.replaceAll(" ");
		}
		
		for (int c = 0; c < (text.length() - maxGram) + 1; c++)
		{
			for (int n = minGram; n < maxGram+1; n++)
			{
				extractSingleHit(documents, documentHits, i, column, text, c, n);
			}
		}
	}

	protected void extractSingleHit(DocumentList documents, Map<Feature, FeatureHit> documentHits, int i, String column, String text, int c, int n)
	{
		String characters = text.substring(c, c + n);
		// if this doc list differentiates text columns, ensure
		// unique feature names per column.
		String featureName = documents.getTextFeatureName(characters + CHAR_FEATURE_SUFFIX+n, column);

		// always get Feature objects this way.
		// Feature.fetchFeature(extractor prefix, featureName, featureType,
		// featureExtractor)
		Feature feature = Feature.fetchFeature("char", featureName, Type.BOOLEAN, this);

		// update the existing feature hit for this document
		if (documentHits.containsKey(feature))
		{
			if(trackHitLocation)
			{
				LocalFeatureHit localHit = (LocalFeatureHit) documentHits.get(feature);
	
				// for later visualization, we keep track of the column,
				// start and end indices of each local feature hit.
				localHit.addHit(column, c, c + n);
			}
		}
		// create a new feature hit for this document
		else
		{
			FeatureHit hit;
			if(trackHitLocation)
			{
				// LocalFeatureHit(feature, featureValue, docIndex, textColumn,
				// startIndex (within column text), endIndex)
				hit = new LocalFeatureHit(feature, Boolean.TRUE, i, column, c, c + n);
			}
			else
			{
				hit = new FeatureHit(feature, Boolean.TRUE, i);
			}
			documentHits.put(feature, hit);
		}
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
		settings.put("minGram", minGram + "");
		settings.put("maxGram", maxGram + "");

		if (includePunctuation) settings.put("Include Punctuation", "true");
		if (stayWithinWords) settings.put("Extract Only Within Words", "true");
		if (trackHitLocation) settings.put("Track Feature Hit Location", "true");
		return settings;
	}

	/**
	 * @param settings
	 *            a map of strings from which the plugin should update its
	 *            configuration used during model building as well as for saving
	 *            feature table recipes
	 */
	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		minGram = Integer.parseInt(settings.get("minGram"));
		maxGram = Integer.parseInt(settings.get("maxGram"));
		includePunctuation = settings.containsKey("Include Punctuation");
		stayWithinWords = settings.containsKey("Extract Only Within Words");
		trackHitLocation = settings.containsKey("Track Feature Hit Location");
	}

	/**
	 * @return a unique short name for this plugin
	 */
	@Override
	public String getOutputName()
	{
		return "char";
	}

	/**
	 * @return the plugin name that will appear in the LightSIDE UI
	 */
	@Override
	public String toString()
	{
		return "Character N-Grams";
	}
	
	public String getDefaultName()
	{
		if(minGram == maxGram)
			return "char"+minGram;
		else
			return "char"+minGram+maxGram;
	}

	/**
	 * create the configuration UI, and hook it in to the plugin settings.
	 * 
	 * @return the newly-created component that will serve as the configuration
	 *         UI.
	 */
	private Component makeConfigUI()
	{
		JPanel panel = new JPanel(new RiverLayout());

		final RangeSlider ngramRangeSlider = new RangeSlider(1, 5);
		ngramRangeSlider.setPaintLabels(true);
		ngramRangeSlider.setPaintTicks(true);
		ngramRangeSlider.setSnapToTicks(true);
		ngramRangeSlider.setLowValue(minGram);
		ngramRangeSlider.setHighValue(maxGram);
		ngramRangeSlider.setMajorTickSpacing(1);

		final JCheckBox punctBox = new JCheckBox("Include Punctuation");
		final JCheckBox wordBox = new JCheckBox("Extract Across Whitespace");
		final JCheckBox trackBox = new JCheckBox("Track Hit Locations");

		ActionListener boxListener = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				setStayWithinWords(!wordBox.isSelected());
				setIncludePunctuation(punctBox.isSelected());
				setTrackHitLocation(trackBox.isSelected());
			}

		};
		wordBox.addActionListener(boxListener);
		punctBox.addActionListener(boxListener);
		trackBox.addActionListener(boxListener);

		wordBox.setSelected(!stayWithinWords);
		punctBox.setSelected(includePunctuation);
		punctBox.setSelected(trackHitLocation);

		panel.add("left", new JLabel("N="));
		panel.add("left", ngramRangeSlider);
		panel.add("br left", wordBox);
		panel.add("br left", punctBox);
		panel.add("br left", trackBox);

		ngramRangeSlider.addChangeListener(new ChangeListener()
		{

			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				minGram = ngramRangeSlider.getLowValue();
				maxGram = ngramRangeSlider.getHighValue();
			}
		});

		ngramRangeSlider.setToolTipText("Set the minimum and maximum character n-gram size.");
		wordBox.setToolTipText("When checked, character n-grams do not respect word boundaries.");
		punctBox.setToolTipText("When checked, character n-grams include punctuation.");
		trackBox.setToolTipText("<html>Uncheck this box to save memory with large datasets.<br>You won't be able to see highlighted character n-grams in Explore Results</html>");
		
		return panel;
	}

	public int getMinGram()
	{
		return minGram;
	}

	public void setMinGram(int minGram)
	{
		this.minGram = minGram;
	}

	public int getMaxGram()
	{
		return maxGram;
	}

	public void setMaxGram(int maxGram)
	{
		this.maxGram = maxGram;
	}

	public boolean isIncludePunctuation()
	{
		return includePunctuation;
	}

	public void setIncludePunctuation(boolean includePunctuation)
	{
		this.includePunctuation = includePunctuation;
	}

	public boolean isStayWithinWords()
	{
		return stayWithinWords;
	}

	public void setStayWithinWords(boolean stayWithinWords)
	{
		this.stayWithinWords = stayWithinWords;
	}

	public boolean isTrackHitLocation()
	{
		return trackHitLocation;
	}

	public void setTrackHitLocation(boolean trackHitLocation)
	{
		this.trackHitLocation = trackHitLocation;
	}

}
