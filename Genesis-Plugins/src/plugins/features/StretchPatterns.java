package plugins.features;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import plugins.view.util.RangeSlider;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.model.FreqMap;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.util.TokenizingTools;
import edu.cmu.side.view.util.WarningButton;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasOffset;
import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;

public class StretchPatterns<T> extends FeaturePlugin
{

	static class WrappingListCellRenderer extends DefaultListCellRenderer
	{
		public static final String HTML_1 = "<html><body style='width: ";
		public static final String HTML_2 = "px'>";
		public static final String HTML_3 = "</html>";

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			String text = HTML_1 + (list.getWidth()-75) + HTML_2 + value.toString() + HTML_3;
			Component cell = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
//			Rectangle bounds = cell.getBounds();
//			cell.setBounds(bounds.x, bounds.y, bounds.width, 8*(int)(Math.ceil(bounds.height/8.0)));
			return cell;
		}

	}
	
	static final String stretchyAboutText = "<html><body style='width: 400px'><p>Stretchy patterns are features that represent a sequence " +
			"of words, word-categories, POS tags, with variable-length 'gaps' within them.</p><br><p>This plugin attempts to extract ALL " +
			"the possible stretchy patterns in your documents. <b>For documents longer than a few sentences, this can be VERY memory intensive. " +
			"Make sure you have enough memory.</b></p><br><p>For more information, see " +
			"Gianfortoni et al, <i>Modeling of Stylistic Variation in Social Media with Stretchy Patterns</i>, " +
			"Proceedings of the First Workshop on Algorithms and Resources for Modeling of Dialects and Language Varieties, 2011.</p></body></html>";

	protected static class CategoryInstance
	{
		public String name;
		public String[] tokens;

		public CategoryInstance(String name, String... tokens)
		{
			this.name = name;
			this.tokens = tokens;
		}

		@Override
		public String toString()
		{
			return name + ": " + Arrays.toString(tokens);
		}
	}

	static class PatternInfo implements Comparable<PatternInfo>
	{
		public String pattern;
		public int start;
		public int end;

		PatternInfo(String p, int start, int end)
		{
			this.pattern = p;
			this.start = start;
			this.end = end;
		}

		@Override
		public int compareTo(PatternInfo o)
		{
			int patternCompare = pattern.compareTo(o.pattern);

			if (patternCompare == 0) if (this.start == o.start)
				return this.end - o.end;
			else
				return this.start - o.start;
			return patternCompare;
		}
	}

	protected JPanel config = new JPanel();
	// JTextField gapMin = new JTextField(3);
	// JTextField gapMax = new JTextField(3);
	// JTextField patMin = new JTextField(3);
	// JTextField patMax = new JTextField(3);
	protected JCheckBox extractAllPOSBox = new JCheckBox("Include POS tags in patterns");
	protected JCheckBox extractAllWordsBox = new JCheckBox("Include surface words in patterns");
	protected JCheckBox extractWordCategoriesBox = new JCheckBox("Categories match against surface words");
	protected JCheckBox extractPOSCategoriesBox = new JCheckBox("Categories match against POS tags");
	protected JCheckBox requireCategoriesBox = new JCheckBox("Require at least one category per pattern");
	protected JCheckBox hideCategorySurfaceFormsBox = new JCheckBox("<html>Don't include surface/POS form<br> when a category matches</html>");
	protected JCheckBox countHitsBox = new JCheckBox("Count pattern hits");

	protected RangeSlider patternLengthSlider = new RangeSlider(0, 8);
	protected RangeSlider gapLengthSlider = new RangeSlider(0, 8);
	protected JSlider thresholdSlider = new JSlider(0, 4);
	protected WarningButton warn = new WarningButton();
	protected JButton helpButton;

	protected JFileChooser chooser;
	protected JList categoryNameList = new JList();
	protected int gMin = 1;
	protected int gMax = 2;
	protected int pMin = 2;
	protected int pMax = 4;
	protected double rareThreshold = 0;
	protected boolean extractAllPOSPatterns = false;
	protected boolean extractAllWordPatterns = true;
	protected boolean extractPOSCategories = false;
	protected boolean extractWordCategories = true;
	protected boolean hideSurfaceFormsOfCategoryHits = true;
	protected boolean requireCategoryHits = false;
	protected boolean countHits = false;

	protected boolean halt = false;
	

	protected Map<String, Set<CategoryInstance>> categories = new HashMap<String, Set<CategoryInstance>>();
	protected Map<String, Set<String>> categoryNames = new TreeMap<String, Set<String>>();
	protected List<String> fileNames = new ArrayList<String>();

	@Override
	public String toString()
	{
		return "Stretchy Patterns";
	}

	public StretchPatterns()
	{
		configureUI();

	}

	protected void configureUI()
	{
		config.setLayout(new RiverLayout());

		patternLengthSlider.setSnapToTicks(true);
		patternLengthSlider.setPaintLabels(true);
		patternLengthSlider.setMinorTickSpacing(1);
		patternLengthSlider.setMajorTickSpacing(1);
		patternLengthSlider.setValue(pMin);
		patternLengthSlider.setHighValue(pMax);

		gapLengthSlider.setSnapToTicks(true);
		gapLengthSlider.setPaintLabels(true);
		gapLengthSlider.setMinorTickSpacing(1);
		gapLengthSlider.setMajorTickSpacing(1);
		gapLengthSlider.setValue(gMin);
		gapLengthSlider.setHighValue(gMax);

		Dictionary<Integer, Component> labels = new Hashtable<Integer, Component>();
		labels.put(0, new JLabel("0"));
		labels.put(1, new JLabel("100"));
		labels.put(2, new JLabel("200"));
		labels.put(3, new JLabel("500"));
		labels.put(4, new JLabel("1000"));

		thresholdSlider.setSnapToTicks(true);
		thresholdSlider.setPaintLabels(true);
		thresholdSlider.setPaintTicks(true);
		thresholdSlider.setMinorTickSpacing(1);
		thresholdSlider.setMajorTickSpacing(5);
		thresholdSlider.setLabelTable(labels);
		thresholdSlider.setValue(0);

		ChangeListener sliderWarningListener = new ChangeListener()
		{

			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				warn.clearWarning();

				if (extractAllWordPatterns || extractAllPOSPatterns)
				{
					if (patternLengthSlider.getHighValue() > 5)
					{
						warn.setWarning("<html>This extractor exhaustively generates stretchy patterns.<br>"
								+ "Especially if your documents are each several sentences long,<br>"
								+ "extracting long patterns may eat up all your RAM!</html>");
					}
					else if (patternLengthSlider.getHighValue() - patternLengthSlider.getLowValue() > 3
							|| gapLengthSlider.getHighValue() - gapLengthSlider.getLowValue() > 2)
					{
						warn.setWarning("<html>This extractor exhaustively generates stretchy patterns.<br>"
								+ "Especially if your documents are each several sentences long,<br>"
								+ "extracting a wide range of patterns may eat up all your RAM!</html>");
					}
				}

			}
		};

		patternLengthSlider.addChangeListener(sliderWarningListener);
		gapLengthSlider.addChangeListener(sliderWarningListener);
		extractAllPOSBox.addChangeListener(sliderWarningListener);
		extractAllWordsBox.addChangeListener(sliderWarningListener);
		
		
		helpButton = new JButton("About Stretchy Patterns", new ImageIcon("toolkits/icons/help.png"));
		helpButton.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				JOptionPane.showMessageDialog(null, stretchyAboutText, "About Stretchy Patterns", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		config.add("left", patternLengthSlider);
		JLabel patternLengthLabel = new JLabel("Pattern Length");
		config.add("left", patternLengthLabel);
		config.add("br left", gapLengthSlider);
		JLabel gapLengthLabel = new JLabel("Gap Length");
		config.add("left", gapLengthLabel);

		config.add("br left", helpButton);

		extractAllWordsBox.setSelected(extractAllWordPatterns);
		extractAllPOSBox.setSelected(extractAllPOSPatterns);
		extractWordCategoriesBox.setSelected(extractWordCategories);
		extractPOSCategoriesBox.setSelected(extractPOSCategories);
		requireCategoriesBox.setSelected(requireCategoryHits);
		hideCategorySurfaceFormsBox.setSelected(hideSurfaceFormsOfCategoryHits);
		countHitsBox.setSelected(countHits);
		
		categoryNameList.setCellRenderer(new WrappingListCellRenderer());
		
		// config.add("br left", ignoreCaseBox);
		config.add("br left", extractAllWordsBox);
		config.add("left", warn);
		config.add("br left", extractAllPOSBox);

		JButton loadCatsButton = new JButton("Add...");
		JButton clearCatsButton = new JButton("Clear");

		// config.add("br left", new JLabel("Categories:"));
		JScrollPane categoryScroll = new JScrollPane(categoryNameList);
		categoryScroll.setPreferredSize(new Dimension(0, 75));
//		categoryScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		JLabel categoriesLabel = new JLabel("Categories: ");
		config.add("br left", categoriesLabel);
		config.add("left", loadCatsButton);
		config.add("hfill", new JLabel(""));
		config.add("right", clearCatsButton);
		config.add("br hfill", categoryScroll);
		config.add("br left", requireCategoriesBox);
		config.add("br left", hideCategorySurfaceFormsBox);
		config.add("br left", extractWordCategoriesBox);
		config.add("br left", extractPOSCategoriesBox);
		config.add("br left", countHitsBox);

		JLabel cutoffLabel = new JLabel("Prune Rare Features after N documents:");
		config.add("br left", cutoffLabel);
		config.add("br left", thresholdSlider);

		String patternTooltipText = "<html><body style='width: 250px'>This is how many top-level 'tokens' can be in each stretchy pattern."
				+ "Category instances and gaps count as one token each, no matter how many words they cover"
				+ "The number of patterns explodes exponentially with pattern length - "
				+ "unless your documents' vocabulary is extremely limited, use a small max pattern length.</body></html>";

		String gapTooltipText = "<html><body style='width: 250px'>This is how many surface-level words can be covered by a single gap."
				+ "Gaps count as one top-level pattern token each, no matter how many words they cover."
				+ "The number of patterns increases rapidly with the range of allowable gap sizes - "
				+ "unless your documents' vocabulary is extremely limited, use a narrow range of gap sizes.</body></html>";

		String cutoffTooltipText = "<html><body style='width: 250px'>" +
				"In order to keep the total number of patterns in check, we can periodically "
				+ "remove extremely rare patterns. If a pattern has only occured once " +
				"within this many documents, we don't keep it.</body></html>";
		

		String categoryTooltipText = "<html><body style='width: 250px'>" +
				"Word categories are loaded from plain text files: The first line of a category file contains the name of the category," +
				" and every following line should contain one word or POS tag that belongs to that category.";


		patternLengthLabel.setToolTipText(patternTooltipText);
		patternLengthSlider.setToolTipText(patternTooltipText);

		gapLengthLabel.setToolTipText(gapTooltipText);
		gapLengthSlider.setToolTipText(gapTooltipText);

		cutoffLabel.setToolTipText(cutoffTooltipText);
		thresholdSlider.setToolTipText(cutoffTooltipText);
		
		categoryScroll.setToolTipText(categoryTooltipText);
		categoryNameList.setToolTipText(categoryTooltipText);
		loadCatsButton.setToolTipText(categoryTooltipText);
		categoriesLabel.setToolTipText(categoryTooltipText);
		
		loadCatsButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (chooser == null)
				{
					chooser = new JFileChooser("toolkits/categories");
					chooser.setMultiSelectionEnabled(true);
				}
				int selection = chooser.showOpenDialog(config);
				if (selection != JFileChooser.APPROVE_OPTION) { return; }
				for (File f : chooser.getSelectedFiles())
					getCategories(f);
			}
		});

		clearCatsButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				categories.clear();
				categoryNames.clear();
				fileNames.clear();
				categoryNameList.setListData(new String[0]);
			}
		});
	}

	public void getCategories(File cats)
	{
		fileNames.add(cats.getPath());
		try
		{
			BufferedReader read = new BufferedReader(new FileReader(cats));
			String line;
			boolean newCat = true;
			String cat = "";

			while ((line = read.readLine()) != null)
			{
				if (line.length() == 0)
				{
					newCat = true;
				}
				else if (newCat)
				{
					cat = line;
					newCat = false;
				}
				else
				{

					String[] tokens = line.split("\\s+");
					CategoryInstance instance = new CategoryInstance(cat, tokens);

					if (!categories.containsKey(tokens[0]))
					{
						categories.put(tokens[0], new HashSet<CategoryInstance>());
					}

					if (!categoryNames.containsKey(cat))
					{
						categoryNames.put(cat, new TreeSet<String>());
					}

					categories.get(tokens[0]).add(instance);
					categoryNames.get(cat).add(line);
				}
			}
			read.close();

			String[] catArray = new String[categoryNames.size()];
			int i = 0;
			for (String c : categoryNames.keySet())
			{
				catArray[i] = c + ": " + categoryNames.get(c);
				i++;
			}
			categoryNameList.setListData(catArray);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		// logger.fine("categories:");
		// for(String key : categories.keySet())
		// {
		// logger.fine(key+" --> "+categories.get(key));
		// }
	}

	public void uiToMemory()
	{
		// gMin = Integer.parseInt(gapMin.getText());
		// gMax = Integer.parseInt(gapMax.getText());
		// pMin = Integer.parseInt(patMin.getText());
		// pMax = Integer.parseInt(patMax.getText());
		gMin = gapLengthSlider.getValue();
		gMax = gapLengthSlider.getHighValue();
		pMin = patternLengthSlider.getValue();
		pMax = patternLengthSlider.getHighValue();
		rareThreshold = Double.parseDouble(((JLabel) thresholdSlider.getLabelTable().get(thresholdSlider.getValue())).getText());// thresholdSlider.getValue()/100.0;
		extractAllPOSPatterns = extractAllPOSBox.isSelected();
		extractPOSCategories = extractPOSCategoriesBox.isSelected();
		extractAllWordPatterns = extractAllWordsBox.isSelected();
		extractWordCategories = extractWordCategoriesBox.isSelected();
		requireCategoryHits = requireCategoriesBox.isSelected();
		hideSurfaceFormsOfCategoryHits = hideCategorySurfaceFormsBox.isSelected();
		countHits = countHitsBox.isSelected();
	}

	public void addPattern(final String pattern, final int start, final int end, final Map<Feature, LocalFeatureHit> patterns, final int docIndex,
			final String column, final DocumentList docs)
	{
		Type type = countHits?Type.NUMERIC:Type.BOOLEAN;
		Feature feature = Feature.fetchFeature(getOutputName(), docs.getTextFeatureName(pattern, column), type, this);

		// if (!allFeatures.containsKey(hit.pattern))
		if (!patterns.containsKey(feature))
		{
			LocalFeatureHit newHit = new LocalFeatureHit(feature, countHits?1.0:Boolean.TRUE, docIndex, column, start, end);
			patterns.put(feature, newHit);
		}
		else
		{
			LocalFeatureHit hit = patterns.get(feature);
			hit.addHit(column, start, end);
			if(countHits)
			{
				hit.setValue((Double)hit.getValue()+1.0);
			}
		}

	}

	@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater progress)
	{
		return extractFeatureHitsForSubclass(documents, progress, null);
	}

	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater progress, Set<Feature> onlyTheseFeatures)
	{
		Collection<FeatureHit> docHits = new HashSet<FeatureHit>();
		Map<Feature, LocalFeatureHit> localHitCache = new HashMap<Feature, LocalFeatureHit>();
		FreqMap<Feature> subsetFeatures = new FreqMap<Feature>();
		Set<Feature> retainedFeatures = onlyTheseFeatures == null ? new HashSet<Feature>() : onlyTheseFeatures;
		// Set<Feature> cull = new HashSet<Feature>();

		long start = System.currentTimeMillis();
		int checkedDoc = -1;

		int numDocs = documents.getSize();
		List<Integer> indices = new ArrayList<Integer>(numDocs);

		for (int i = 0; i < numDocs; i++)
		{
			indices.add(i);
		}

		Collections.shuffle(indices, new Random(1l));

		for (int index = 0; index < numDocs; index++)
		{

			if (onlyTheseFeatures == null)
			{
				progress.update("Extracting Stretchy Patterns", index + 1, numDocs);
			}
			else
			{
				progress.update("Finalizing Stretchy Patterns", index + 1, numDocs);
			}
			Integer docID = indices.get(index);

			if (halt)
			{
				halt = false;
				return null;
			}

			extractPatternsForDocument(docID, documents, localHitCache);
			docHits.addAll(localHitCache.values());
			localHitCache.clear();

			// rareThreshold <= .02? 100 : 50;
			int checkPoint = (int) rareThreshold;
			
			// docHits.size() > 1000000  + checked)
			if (rareThreshold > 0 && (index == numDocs - 1 || (index % checkPoint == checkPoint - 1)))
			{
				List<Integer> subset = indices.subList(checkedDoc + 1, index + 1);
				logger.fine("SP 445: Preemptively removing rare patterns in docs " + (checkedDoc + 1) + "-" + (index));
				if (onlyTheseFeatures == null)
				{
					progress.update("Preemptively removing rare patterns", index + 1, numDocs);
					for (FeatureHit hit : docHits)
					{
						if (subset.contains(hit.getDocumentIndex()))
						{
							subsetFeatures.count(hit.getFeature());
						}
					}

					for (Feature feature : subsetFeatures.keySet())
					{
						int featureCount = subsetFeatures.get(feature);
						// featureCount/(float)(index+1 - checkedDoc) > rareThreshold )
						if (featureCount > 1)
						{
							retainedFeatures.add(feature);
						}
					}
				}
				logger.fine("SP 455: keeping " + retainedFeatures.size() + " total features.");

				int culled = 0;

				Iterator<FeatureHit> hiterator = docHits.iterator();
				while (hiterator.hasNext())
				{
					FeatureHit hit = hiterator.next();
					if (subset.contains(hit.getDocumentIndex()) && !retainedFeatures.contains(hit.getFeature()))
					{
						hiterator.remove();
						culled++;
					}
				}
				logger.fine("SP 478: culled " + culled + " hits this time - now " + docHits.size() + " total hits.");
				subsetFeatures.clear();
				attemptGC();
				checkedDoc = index;
			}
		}

		if (onlyTheseFeatures == null)
		{
			extractFeatureHitsForSubclass(documents, progress, retainedFeatures);
		}

		long delta = System.currentTimeMillis() - start;
		logger.fine(delta + " ms, to extract " + docHits.size() + " hits from " + numDocs + " documents.\n" + (delta / numDocs) + " ms/doc.\n"
				+ (docHits.isEmpty() ? "0" : (1000 * delta / docHits.size())) + " ms/kilohit.");

		progress.update("");

		return docHits;
	}

	protected <T extends HasWord & HasTag & HasOffset> void extractPatternsForDocument(Integer docID, DocumentList documents, Map<Feature, LocalFeatureHit> localHitCache)
	{
		Map<String, List<String>> docMap = documents.getCoveredTextList();
		for (String column : docMap.keySet())
		{
			String doc = docMap.get(column).get(docID);

			List<T> tokens = tokenize(doc, extractAllPOSPatterns || extractPOSCategories);

			extract(tokens, gMin, gMax, pMin, pMax, localHitCache, documents, docID, column);

		} // end column loop
	}

	protected <T  extends HasWord & HasTag & HasOffset> List<T> tokenize(String doc, boolean includePOS)
	{
		List<CoreLabel> tokens = TokenizingTools.tokenizeInvertible(doc);
		if (includePOS)
		{
			tokens = TokenizingTools.tagInvertible(tokens);
		}
		return (List<T>) tokens;
	}

	public <T extends HasWord & HasTag & HasOffset> void extract(final Map<Feature, LocalFeatureHit> patterns, final DocumentList docs, final int docIndex, final String column, final boolean justGap,
			final String patternSoFar, final int minGapSize, final int maxGapSize, final int minPatternSize, final int currentPatternSize,
			final int remainingPatternSize, final List<T> tokens, final int currentIndex, final int startingIndex, boolean hasCategory)
	{
		if (currentPatternSize >= minPatternSize && patternSoFar.length() > 0 && !patternSoFar.endsWith("[GAP] ") && !patternSoFar.startsWith("[GAP] ")
				&& (hasCategory || !requireCategoryHits || categories.isEmpty()))
		{
			// currentIndex is the token index of the *next* token.
			addPattern(patternSoFar, tokens.get(startingIndex).beginPosition(), tokens.get(currentIndex - 1).endPosition(), patterns, docIndex, column, docs);
		}
		
		//Add BOL and EOL tags
		if (currentIndex > 0 && startingIndex == 0 && currentPatternSize + 1>= minPatternSize  && remainingPatternSize >= 1 && !patternSoFar.endsWith("[GAP] ")
				&& (hasCategory || !requireCategoryHits || categories.isEmpty()))	
		{
			// currentIndex is the token index of the *next* token.
			addPattern("BOL "+patternSoFar, tokens.get(startingIndex).beginPosition(), tokens.get(currentIndex - 1).endPosition(), patterns, docIndex, column, docs);
		}

		if (currentIndex > 0 && currentIndex == tokens.size() && currentPatternSize +1 >= minPatternSize && remainingPatternSize >= 1 &&  !patternSoFar.startsWith("[GAP] ")
				&& (hasCategory || !requireCategoryHits || categories.isEmpty()))
		{
			// currentIndex is the token index of the *next* token.
			addPattern(patternSoFar+" EOL", tokens.get(startingIndex).beginPosition(), tokens.get(currentIndex - 1).endPosition(), patterns, docIndex, column, docs);
		}
		
		int numTokens = tokens.size();
		if (remainingPatternSize > 0 && currentIndex < numTokens)
		{

			String currentWord = tokens.get(currentIndex).word();
			String currentPOS = extractAllPOSPatterns || extractPOSCategories ? tokens.get(currentIndex).tag() : null;
			if(currentWord.equals("newturn") || currentWord.equals("otherturn") || currentWord.equals("tutorturn"))
				currentPOS = "|EOT|";

			boolean categoryHit = false;

			if (extractWordCategories && categories.containsKey(currentWord))
			{
				for (CategoryInstance category : categories.get(currentWord))
				{
					boolean allTokens = true;
					for (int i = 0; i < category.tokens.length; i++)
					{
						if (i + currentIndex >= tokens.size() || !category.tokens[i].equals(tokens.get(currentIndex + i).word()))
						{
							allTokens = false;
							break;
						}
					}
					if (allTokens)
					{

						categoryHit = true;

						extract(patterns, docs, docIndex, column, false, patternSoFar + category.name + " ", minGapSize, maxGapSize, minPatternSize,
								currentPatternSize + 1, remainingPatternSize - 1, tokens, currentIndex + category.tokens.length, startingIndex, true);
					}
				}
			}

			// extract POS categories
			if (extractPOSCategories && categories.containsKey(currentPOS))
			{
				categoryHit = true;
				for (CategoryInstance category : categories.get(currentPOS))
				{
					boolean allTokens = true;
					for (int i = 0; i < category.tokens.length; i++)
					{
						if (i + currentIndex >= tokens.size() || !category.tokens[i].equals(tokens.get(currentIndex + i).word()))
						{
							allTokens = false;
							break;
						}
					}
					if (allTokens)
					{

						categoryHit = true;

						extract(patterns, docs, docIndex, column, false, patternSoFar + category.name + " ", minGapSize, maxGapSize, minPatternSize,
								currentPatternSize + 1, remainingPatternSize - 1, tokens, currentIndex + category.tokens.length, startingIndex, true);
					}
				}
			}

			if (!hideSurfaceFormsOfCategoryHits || !categoryHit)
			{
				if (extractAllWordPatterns)
					extract(patterns, docs, docIndex, column, false, patternSoFar + currentWord + " ", minGapSize, maxGapSize, minPatternSize,
							currentPatternSize + 1, remainingPatternSize - 1, tokens, currentIndex + 1, startingIndex, hasCategory);

				if (extractAllPOSPatterns)
					extract(patterns, docs, docIndex, column, false, patternSoFar + currentPOS + " ", minGapSize, maxGapSize, minPatternSize,
							currentPatternSize + 1, remainingPatternSize - 1, tokens, currentIndex + 1, startingIndex, hasCategory);
			}

			// extract gaps
			if (maxGapSize > 0)
				for (int i = minGapSize; i <= maxGapSize; i++)
				{
					if (currentIndex + i < numTokens && !justGap)
					{
						extract(patterns, docs, docIndex, column, true, patternSoFar + "[GAP] ", minGapSize, maxGapSize, minPatternSize,
								currentPatternSize + 1, remainingPatternSize - 1, tokens, currentIndex + i, startingIndex, hasCategory);
					}
				}
			// attemptGC();
		}
	}

	public <T extends HasWord & HasTag & HasOffset> Map<Feature, LocalFeatureHit> extract(final List<T> tokens, final int gMin, final int gMax, final int pMin, final int pMax,
			final Map<Feature, LocalFeatureHit> patterns, final DocumentList docs, final int index, final String column)
	{
		for (int i = 0; i < tokens.size(); i++)
		{
			extract(patterns, docs, index, column, false, "", gMin, gMax, pMin, 0, pMax, tokens, i, i, false);
		}
		return patterns;
	}

	@Override
	public String getOutputName()
	{
		return "stretch";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		return config;
	}

	@Override
	public void stopWhenPossible()
	{
		halt = true;
	}

	protected void memoryToUI()
	{
		// nothing.
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new HashMap<String, String>();

		uiToMemory();
		settings.put("pMin", "" + this.pMin);
		settings.put("pMax", "" + this.pMax);
		settings.put("gMin", "" + this.gMin);
		settings.put("gMax", "" + this.gMax);
		settings.put("extractAllWordPatterns", "" + this.extractAllWordPatterns);
		settings.put("extractAllPOSPatterns", "" + this.extractAllPOSPatterns);
		settings.put("extractPOSCategories", "" + this.extractPOSCategories);
		settings.put("extractWordCategories", "" + this.extractWordCategories);
		settings.put("requireCategoryHits", "" + this.requireCategoryHits);
		settings.put("hideSurfaceFormsOfCategoryHits", "" + this.hideSurfaceFormsOfCategoryHits);
		settings.put("countHits", "" + this.countHits);
		settings.put("rareThreshold", "" + this.rareThreshold);

		for (String file : fileNames)
		{
			settings.put("file-" + file, "file");
		}

		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{

		this.pMin = Integer.parseInt(settings.get("pMin"));
		this.pMax = Integer.parseInt(settings.get("pMax"));
		this.gMin = Integer.parseInt(settings.get("gMin"));
		this.gMax = Integer.parseInt(settings.get("gMax"));

		this.extractAllWordPatterns = Boolean.parseBoolean(settings.get("extractAllWordPatterns"));
		this.extractWordCategories = Boolean.parseBoolean(settings.get("extractWordCategories"));
		this.extractAllPOSPatterns = Boolean.parseBoolean(settings.get("extractAllPOSPatterns"));
		this.extractPOSCategories = Boolean.parseBoolean(settings.get("extractPOSCategories"));

		this.requireCategoryHits = Boolean.parseBoolean(settings.get("requireCategoryHits"));
		this.hideSurfaceFormsOfCategoryHits = Boolean.parseBoolean(settings.get("hideSurfaceFormsOfCategoryHits"));
		this.countHits =  Boolean.parseBoolean(settings.get("countHits"));

		this.rareThreshold = Double.parseDouble(settings.get("rareThreshold"));

		for (String key : settings.keySet())
		{
			if (key.startsWith("file-"))
			{
				getCategories(new File(key.substring(5)));
			}
		}
		memoryToUI();
	}

	public static void main(String[] args)
	{
		JFrame freddy = new JFrame("Test!");
		freddy.setContentPane(new StretchPatterns().config);
		freddy.setPreferredSize(new Dimension(400, 600));
		freddy.setVisible(true);
		freddy.pack();
		freddy.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	@Override
	public boolean isTokenized(Feature f)
	{
		return false;
	}

	// @Override
	// public List<String> tokenize(Feature f, String s)
	// {
	// return TokenizingTools.tokenize(s);
	// }

	int gcCount = 0;
	long usedNow = 1024;

	protected void attemptGC()
	{
		gcCount++;
		Runtime runtime = Runtime.getRuntime();
		long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
		if (used - usedNow > 400)
		{
			logger.fine("attempting GC " + gcCount + ": " + used + "M used before");
			System.gc();

			usedNow = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
			logger.fine("attempting GC " + gcCount + ": " + usedNow + "M used now\t(" + (used - usedNow) + "M saved)");
		}
	}
}
