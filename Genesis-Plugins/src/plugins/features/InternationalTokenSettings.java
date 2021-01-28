package plugins.features;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.ParallelFeaturePlugin;
import edu.cmu.side.util.AbstractTokenizingTool;
import edu.cmu.side.util.GermanTokenizingTool;
import edu.cmu.side.util.TokenizingToolLanguage;
import edu.cmu.side.util.TokenizingTools;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasOffset;
import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;

/**
 * This is a bogus feature extractor that simply sets the language
 * model that other features will use for tokenization, part of speech tagging,
 * and (maybe someday) parsing.  It sets a global variable, the TokenizingTools, to have
 * a pointer to the language model needed.  This works because all feature extractor objects
 * are initialized before use, in both the GUI version of Lightside and the offline version.
 * 
 * This interaction between features is a little unsavory because the features are otherwise
 * independent, and it's a bit of a violation of the architecture to have a feature that relies
 * on global state to affect other features, but there isn't an obvious other place to put this, 
 * and I don't want to do a deep rethinking of the architecture.
 */
public class InternationalTokenSettings extends ParallelFeaturePlugin
{
	protected TokenizingToolLanguage language;
	protected Container wrapper;
	
	public InternationalTokenSettings()
	{	
		super();
		wrapper = new JPanel(new BorderLayout());
		
		final JComboBox<TokenizingToolLanguage> languageCombo = new JComboBox<TokenizingToolLanguage>(TokenizingToolLanguage.values());
		wrapper.add(languageCombo, BorderLayout.NORTH);
		//wrapper.add(panel, BorderLayout.CENTER);

		language = TokenizingToolLanguage.ENGLISH;
		TokenizingTools.setLanguage(language);
		languageCombo.setSelectedItem(language);
		
		languageCombo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				language = (TokenizingToolLanguage) languageCombo.getSelectedItem();
				TokenizingTools.setLanguage(language);
			}
		});

	}
	
	@Override
	protected Component getConfigurationUIForSubclass()
	{
		return wrapper;
	}
	
	protected  <T  extends HasWord & HasTag & HasOffset> List<T> tokenize(String text, boolean includePOS)
	{
		List<CoreLabel> tokens;
		tokens = language.getTool().tokenizeInvertible(text);
		
		if (includePOS)
		{
			tokens = language.getTool().tagInvertible(tokens);
		}
		
		return (List<T>) tokens;
		
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new TreeMap<String, String>();
		settings.put("tagger_language", language.name());
		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		language = TokenizingToolLanguage.valueOf(settings.get("tagger_language"));
		//this.loadStopWords(language.getTool().punctuationFilename(), language.getTool().stopwordsFilename());
	}
	
	public String getOutputName()
	{
		return "languagechoice";
	}
	
	public String toString()
	{
		return "Language for POS/Tokenizing";
	}

	/*
	 * This 
	 * (non-Javadoc)
	 * @see edu.cmu.side.plugin.ParallelFeaturePlugin#extractFeatureHitsFromDocument(edu.cmu.side.model.data.DocumentList, int)
	 */
	@Override
	public Collection<FeatureHit> extractFeatureHitsFromDocument(DocumentList documents, int i) {
		// TODO Auto-generated method stub
		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();
		return hits;
	}

}
