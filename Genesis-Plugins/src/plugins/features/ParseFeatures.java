package plugins.features;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.util.TokenizingTools;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreebankLanguagePack;
import edu.stanford.nlp.trees.TypedDependency;

public class ParseFeatures extends FeaturePlugin
{
	private static final String PARSER_MODEL_PATH = "toolkits/parser/englishPCFG.caseless.ser.gz";
	boolean dependencyFeatures = false;
	boolean productionRules = true;
	boolean leafProductions = false;

	LexicalizedParser parser;

	GrammaticalStructureFactory structureFactory;

	// this component will be displayed in the config pane
	Component configUI;

	public ParseFeatures()
	{

	}

	/**
	 * @param documents
	 *            the document list to extract features from
	 * @param update
	 *            a hook back to the UI to provide progress updates
	 */
	@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
	{

		System.out.println("PR "+productionRules);
		System.out.println("LR "+leafProductions);
		System.out.println("DF "+dependencyFeatures);
		
		if(parser == null)
		{
			parser = LexicalizedParser.loadModel(PARSER_MODEL_PATH);
			parser.setOptionFlags(new String[] { "-retainTmpSubcategories" });
			
			TreebankLanguagePack languagePack = new PennTreebankLanguagePack();
			structureFactory = languagePack.grammaticalStructureFactory();
		}

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
			
			
			// keep the user informed
			if (i % 5 == 0) 
				update.update("Extracting Parse Features", i+1, documents.getSize());

			// extract features from each text column
			for (String column : coveredTextList.keySet())
			{
				// text content for this column
				List<String> textField = coveredTextList.get(column);

				// text content for this column in document i
				String text = textField.get(i).toLowerCase();

				for (List<CoreLabel> sentence : TokenizingTools.splitSentences(text))
				{
					if(halt)
					{
						return new ArrayList<FeatureHit>();
					}
					
					Tree parse = parser.parse(sentence);

					List<String> rules = new ArrayList<String>();
					if (productionRules || leafProductions) getRulesForTree(parse, rules);

					if (dependencyFeatures)
					{
						GrammaticalStructure gs = structureFactory.newGrammaticalStructure(parse);
						List<TypedDependency> tdl = gs.typedDependenciesCCprocessed();

						for (TypedDependency dependency : tdl)
						{
							String dep = dependency.reln() + "(" + dependency.gov().toString() + " " + dependency.dep().toString() + ")";
							rules.add(dep);
//							System.out.println(dep);
						}
					}

					for (String rule : rules)
					{
						// if this doc list differentiates text columns, ensure
						// unique feature names per column.
						String featureName = documents.getTextFeatureName(rule, column);

						// always get Feature objects this way.
						// Feature.fetchFeature(extractor prefix, featureName,
						// featureType, featureExtractor)
						Feature feature = Feature.fetchFeature("parse", featureName, Type.BOOLEAN, this);

						// update the existing feature hit for this document
						// if (documentHits.containsKey(feature))
						// {
						// LocalFeatureHit localHit = (LocalFeatureHit)
						// documentHits.get(feature);
						//
						// // for later visualization, we keep track of the
						// column,
						// // start and end indices of each local feature hit.
						// localHit.addHit(column, c, c + n);
						// }
						// create a new feature hit for this document
						// else
						// {
						// LocalFeatureHit(feature, featureValue, docIndex,
						// textColumn, startIndex (within column text),
						// endIndex)
						FeatureHit hit = new FeatureHit(feature, Boolean.TRUE, i);
						documentHits.put(feature, hit);
						// }
					}

				}

			}

			// add the unique per-feature hits for this document to the big
			// hitlist
			hits.addAll(documentHits.values());
			// clear the per-document cache
			documentHits.clear();
		}

		update.update("Finalizing parse features");

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
		if (dependencyFeatures) settings.put("Dependency Features", "true");
		if (leafProductions) settings.put("Leaf Productions", "true");
		if (productionRules) settings.put("Production Rules", "true");
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
		dependencyFeatures = settings.containsKey("Dependency Features");
		leafProductions = settings.containsKey("Leaf Productions");
		productionRules = settings.containsKey("Production Rules");
	}

	/**
	 * @return a unique short name for this plugin
	 */
	@Override
	public String getOutputName()
	{
		return "parse";
	}

	/**
	 * @return the plugin name that will appear in the LightSIDE UI
	 */
	@Override
	public String toString()
	{
		return "English Parse Features";
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

		final JCheckBox productionBox = new JCheckBox("Production Rules");
		final JCheckBox leafRulesBox = new JCheckBox("Leaf Productions");
		final JCheckBox dependencyBox = new JCheckBox("Dependency Relations");

		ActionListener checkListener = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				productionRules = productionBox.isSelected();
				leafProductions = leafRulesBox.isSelected();
				dependencyFeatures = dependencyBox.isSelected();

			}
		};

		productionBox.addActionListener(checkListener);
		leafRulesBox.addActionListener(checkListener);
		dependencyBox.addActionListener(checkListener);

		productionBox.setSelected(true);

		panel.add("left", productionBox);
		panel.add("br left", leafRulesBox);
		panel.add("br left", dependencyBox);
		panel.add("br left", new JLabel("Parsing is pretty slow.  Go get a coffee or something.", new ImageIcon("toolkits/icons/error.png"), SwingConstants.LEFT));

		productionBox.setToolTipText("Extract parent-children subtrees, like NP -> DT NNS");
		leafRulesBox.setToolTipText("Extract leaf production rules, like NNS -> mountains");
		dependencyBox.setToolTipText("Extract dependency relations, like nsubj(love i)");
		
		return panel;
	}

	protected void getRulesForTree(Tree parse, List<String> rules)
	{
		String rule = parse.value() + " ->";
		for (Tree kid : parse.children())
		{
			rule += " " + kid.value();
			if (!kid.isLeaf()) 
				getRulesForTree(kid, rules);
		}

		if(parse.isPhrasal())
		{
			if(productionRules)
				rules.add(rule);
		}
		else if(parse.isPreTerminal() && leafProductions)
		{
			rules.add(rule);
		}
	}
	
	public static void main(String[] args)
	{
		StatusUpdater quietUpdater = new StatusUpdater()
		{
			@Override
			public void update(String update)
			{}
			
			@Override
			public void update(String updateSlot, int slot1, int slot2)
			{}
			
			@Override
			public void reset()
			{}
		};
		
		ParseFeatures plugin = new ParseFeatures();
		plugin.productionRules = false;
		plugin.leafProductions = false;
		plugin.dependencyFeatures = true;
		
		Scanner in = new Scanner(System.in);
		while(in.hasNextLine())
		{
			DocumentList doc = new DocumentList(in.nextLine());
			Collection<FeatureHit> hits = plugin.extractFeatureHitsForSubclass(doc, quietUpdater);
			for(FeatureHit h : hits)
			{
				System.out.println(h);
			}
		}
	}

}
