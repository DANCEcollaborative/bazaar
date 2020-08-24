package edu.cmu.side.util;

import edu.stanford.nlp.international.french.process.FrenchTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class FrenchTokenizingTool extends AbstractTokenizingTool
{

	@Override
	public TokenizerFactory<CoreLabel> createTokenizerFactory()
	{
		// TODO Auto-generated method stub
		return FrenchTokenizer.factory();
	}

	@Override
	protected MaxentTagger createTagger()
	{
		return new MaxentTagger("toolkits/maxent/french.tagger");
	}

}
