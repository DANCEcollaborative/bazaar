package dadamson.words;

public class MaxSentenceMatcher extends ASentenceMatcher
{
	ASentenceMatcher[] matchers;
	
	public MaxSentenceMatcher(ASentenceMatcher... matchers)
	{
		this.matchers = matchers;
	}
	
	@Override
	public double getWordSimilarity(String t1, String t2)
	{
		double sim = 0;
		for(ASentenceMatcher matcher : matchers)
		{
			sim = Math.max(sim, matcher.getWordSimilarity(t1, t2));
		}
		return sim;
	}

	@Override
	public boolean isKnownWord(String word, String sentence)
	{
		for(ASentenceMatcher matcher : matchers)
		{
			if(matcher.isKnownWord(word, sentence))
				return true;
		}
		return false;
	}

}
