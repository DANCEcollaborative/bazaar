package edu.cmu.side.util;

import edu.stanford.nlp.international.arabic.process.ArabicTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class ArabicTokenizingTool extends AbstractTokenizingTool
{

	@Override
	public TokenizerFactory<CoreLabel> createTokenizerFactory()
	{
		// TODO may want to switch to a WordSegmenter for Arabic and Chinese
		return ArabicTokenizer.factory();
	}

	@Override
	protected MaxentTagger createTagger()
	{
		return new MaxentTagger("toolkits/maxent/arabic.tagger");
	}

}
