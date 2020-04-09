package edu.cmu.side.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class EnglishTokenizingTool extends AbstractTokenizingTool
{

	@Override
	public TokenizerFactory<CoreLabel> createTokenizerFactory()
	{
		return PTBTokenizerFactory.newPTBTokenizerFactory(new CoreLabelTokenFactory(true), "invertible,unicodeQuotes=true,untokenizable=firstKeep");
	}

	@Override
	protected MaxentTagger createTagger()
	{
		return new MaxentTagger("toolkits/maxent/english-caseless-left3words-distsim.tagger");
	}

}
