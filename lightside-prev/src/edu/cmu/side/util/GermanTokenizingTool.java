package edu.cmu.side.util;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.WhitespaceTokenizer.WhitespaceTokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class GermanTokenizingTool extends AbstractTokenizingTool
{

	@Override
	public TokenizerFactory<CoreLabel> createTokenizerFactory()
	{
		// FIXME: there is no special german tagger
		//return new WhitespaceTokenizerFactory<CoreLabel>(new CoreLabelTokenFactory(true), "invertible,unicodeQuotes=true,untokenizable=firstKeep");
		return PTBTokenizerFactory.newPTBTokenizerFactory(new CoreLabelTokenFactory(true), "invertible,unicodeQuotes=true,untokenizable=firstKeep");
	}

	@Override
	protected MaxentTagger createTagger()
	{
		return new MaxentTagger("toolkits/maxent/german-fast-caseless.tagger");
	}

}
