package edu.cmu.side.util;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JOptionPane;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.StringUtils;

public class TokenizingTools
{
	public static final String DEFAULT_TAGGER_MODEL = "toolkits/maxent/english-caseless-left3words-distsim.tagger";
	private static MaxentTagger tagger;
	private static TokenizerFactory<CoreLabel> factory;
	private static TokenizingToolLanguage language = TokenizingToolLanguage.ENGLISH;
	
	public static void setLanguage(TokenizingToolLanguage language) {
		synchronized(TokenizingTools.class)
		{
			TokenizingTools.language = language;
			TokenizingTools.factory = null;
			TokenizingTools.tagger = null;  
		}
	}
	public static TokenizingToolLanguage getLanguage() { return language; }

	protected static TokenizerFactory<CoreLabel> getTokenizerFactory()
	{
		if (factory == null)//does this duplicated outer check save lock-time?
		{
			synchronized(TokenizingTools.class)
			{
				if(factory == null) factory = language.getTool().createTokenizerFactory();
			}
			
		}
		return factory;
	}
	
	protected static MaxentTagger getTagger()
	{
		if (tagger == null) try//does this duplicated outer check save lock-time?
		{
			synchronized(TokenizingTools.class)
			{
				if(tagger == null)
					tagger = language.getTool().getTagger(); //  new MaxentTagger(DEFAULT_TAGGER_MODEL);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not find MaxentTagger files", "ERROR", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		return tagger;
	}

	// static
	// {
	// try
	// {
	// // TaggerConfig config = new TaggerConfig(args);
	// tagger = new
	// MaxentTagger("toolkits/maxent/english-caseless-left3words-distsim.tagger");
	// // tagger = new
	// //
	// MaxentTagger("toolkits/maxent/wsj-0-18-caseless-left3words-distsim.tagger");
	// // tagger = new
	// // MaxentTagger("toolkits/maxent/wsj-0-18-left3words.tagger");
	// factory = PTBTokenizerFactory.newPTBTokenizerFactory(false, true);
	// // factory = PTBTokenizerFactory.newTokenizerFactory();
	// // check if we are to use a custom stoplist
	// //
	// // this should be only a file name with the file being present in
	// // the etc/ directory of TagHelperTools2
	// }
	// catch (Exception e)
	// {
	// e.printStackTrace();
	// JOptionPane.showMessageDialog(null, "Could not find MaxentTagger files",
	// "ERROR", JOptionPane.ERROR_MESSAGE);
	// System.exit(0);
	// }
	// }

	public static List<CoreLabel> tokenizeInvertible(String s)
	{
		StringReader reader = new StringReader(s.toLowerCase());
		Tokenizer<CoreLabel> tokenizer = getTokenizerFactory().getTokenizer(reader);

		List<CoreLabel> tokens = tokenizer.tokenize();
		return tokens;
	}

	public static List<CoreLabel> tagInvertible(List<CoreLabel> tokens)
	{
		
		if(tokens.isEmpty())
			return tokens;
		
		getTagger().tagCoreLabels(tokens);
		return tokens;
	}

	public static List<List<CoreLabel>> splitSentences(String s)
	{
		DocumentPreprocessor p = new DocumentPreprocessor(new StringReader(s));
		p.setTokenizerFactory(getTokenizerFactory());

		List<List<CoreLabel>> sentences = new ArrayList<List<CoreLabel>>();
		Iterator<?> pit = p.iterator();

		while (pit.hasNext())
		{
			List<CoreLabel> sentence = (List<CoreLabel>) pit.next();
			sentences.add(sentence);
		}
		return sentences;
	}

	public static String getPunctuationFilename() {
		return language.getTool().punctuationFilename();
	}
	public static String getStopwordsFilename() {
		return language.getTool().stopwordsFilename();		
	}
	public static List<String> tokenize(String s)
	{
		StringReader reader = new StringReader(s.toLowerCase());
		Tokenizer<CoreLabel> tokenizer = getTokenizerFactory().getTokenizer(reader);
		List<String> tokens = new ArrayList<String>();

		while (tokenizer.hasNext())
		{
			CoreLabel token = tokenizer.next();

			tokens.add(token.word());
		}
		return tokens;
	}

	public static Map<String, List<String>> tagAndTokenize(String s)
	{

		Map<String, List<String>> tagsAndTokens = new HashMap<String, List<String>>();
		List<String> posTags = new ArrayList<String>();
		List<String> surfaceTokens = tokenize(s);

		String tokenized = StringUtils.join(surfaceTokens, " ");
		String tagged = getTagger().tagTokenizedString(tokenized);

		// String tagged = tagger.tagString(s);

		String[] taggedTokens = tagged.split("\\s+");
		tagsAndTokens.put("tokens", surfaceTokens);
		tagsAndTokens.put("POS", posTags);

		for (String t : taggedTokens)
		{
			if (t.contains("_"))
			{
				String[] parts = t.split("_");
				posTags.add(parts[1]);
			}
			else
			{
				System.out.println("TT 84: no POS tag? " + t);
				posTags.add(t);
			}
		}

		return tagsAndTokens;
	}

	public static void main(String[] args)
	{
		Scanner skinner = new Scanner(System.in);

		while (skinner.hasNextLine())
		{
			String line = skinner.nextLine();
			if (line.equals("q")) return;

			List<String> tokenized = tokenize(line);
			List<CoreLabel> tokenizedToo = tagInvertible(tokenizeInvertible(line));
			Map<String, List<String>> posTokens = tagAndTokenize(line);

			System.out.println(tokenized.size() + ":\t" + tokenized);
			System.out.println(posTokens.get("POS").size() + ":\t" + posTokens);

			System.out.println(tokenizedToo.size());
			System.out.println(tokenizedToo);
		}
	}
}