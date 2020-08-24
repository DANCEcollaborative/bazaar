package edu.cmu.side.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.WhitespaceTokenizer.WhitespaceTokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;

public class ChineseTokenizingTool extends AbstractTokenizingTool
{

	@Override
	public TokenizerFactory<CoreLabel> createTokenizerFactory()
	{
		return new ChineseTokenizer.ChineseTokenizerFactory<CoreLabel>(new CoreLabelTokenFactory(true)); 	
	}

	@Override
	protected MaxentTagger createTagger()
	{
		return new MaxentTagger("toolkits/maxent/chinese-distsim.tagger");
	}

	@Override
	public
	String stopwordsFilename()
	{
		return "toolkits/chinese.stp";
	}
	
	@Override
	public String punctuationFilename()
	{
		return "toolkits/punctuation_chinese.stp";
	}


} 
