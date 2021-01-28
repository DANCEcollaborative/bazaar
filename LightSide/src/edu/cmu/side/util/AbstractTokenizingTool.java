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

public abstract class AbstractTokenizingTool
{
	private MaxentTagger tagger;
	private TokenizerFactory<CoreLabel> factory;
	protected static final String stopwordsFilename = "toolkits/english.stp";
	protected static final String punctuationFilename = "toolkits/punctuation.stp";
	public abstract TokenizerFactory<CoreLabel> createTokenizerFactory();
	
	protected abstract MaxentTagger createTagger();
	
	protected TokenizerFactory<CoreLabel> getTokenizerFactory()
	{
		if (factory == null)//does this duplicated outer check save lock-time?
		{
			//factory = PTBTokenizerFactory.newPTBTokenizerFactory(false, true);
			synchronized(TokenizingTools.class)
			{
				if(factory == null) 
				factory = createTokenizerFactory();
			}
			
		}
		return factory;
	}

	protected MaxentTagger getTagger()
	{
		if (tagger == null) try//does this duplicated outer check save lock-time?
		{
			synchronized(AbstractTokenizingTool.class)
			{
				if(tagger == null)
					tagger = createTagger();
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

	public List<CoreLabel> tokenizeInvertible(String s)
	{
		StringReader reader = new StringReader(s.toLowerCase());
		Tokenizer<CoreLabel> tokenizer = getTokenizerFactory().getTokenizer(reader);

		List<CoreLabel> tokens = tokenizer.tokenize();
		return tokens;
	}

	public List<CoreLabel> tagInvertible(List<CoreLabel> tokens)
	{
		
		if(tokens.isEmpty())
			return tokens;
		
		getTagger().tagCoreLabels(tokens);
		return tokens;
	}

	public List<List<CoreLabel>> splitSentences(String s)
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

	public List<String> tokenize(String s)
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

	public Map<String, List<String>> tagAndTokenize(String s)
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
	
	public String stopwordsFilename()
	{
		return stopwordsFilename;
	}
	
	public String punctuationFilename() {
		return punctuationFilename;
	}

	public static void main(String[] args)
	{
		Scanner skinner = new Scanner(System.in);

		while (skinner.hasNextLine())
		{
			String line = skinner.nextLine();
			if (line.equals("q")) return;

			AbstractTokenizingTool tagHelper = new EnglishTokenizingTool();
			
			List<String> tokenized = tagHelper.tokenize(line);
			List<CoreLabel> tokenizedToo = tagHelper.tagInvertible(tagHelper.tokenizeInvertible(line));
			Map<String, List<String>> posTokens = tagHelper.tagAndTokenize(line);

			System.out.println(tokenized.size() + ":\t" + tokenized);
			System.out.println(posTokens.get("POS").size() + ":\t" + posTokens);

			System.out.println(tokenizedToo.size());
			System.out.println(tokenizedToo);
		}
		
		skinner.close();
	}
}