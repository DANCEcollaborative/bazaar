package dadamson.words;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import edu.mit.jwi.*;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.*;

public class OntologySentenceMatcher extends ASentenceMatcher
{
	private static HitCountsComparator hitCountsComparator = new HitCountsComparator();

	protected static final class HitCountsComparator implements Comparator<Entry<String, Integer>>
	{
		@Override
		public int compare(Entry<String, Integer> a, Entry<String, Integer> b)
		{
			return a.getValue().compareTo(b.getValue());
		}
	}

	private static IDictionary singletonDict;
	private static POS[] allPOS = { POS.ADJECTIVE, POS.ADVERB, POS.NOUN, POS.VERB };
	private static OntologySentenceMatcher sentenceMatcher;

	protected static final double CONTENT_BONUS = 1.5;
	protected static final double CONTENT_PENALTY = 1.0;

	private Map<String, List<String>> contentDict = new HashMap<String, List<String>>();
	private Map<String, Double> wordCache = new HashMap<String, Double>();
	private Map<String, Integer> hitCounts = new HashMap<String, Integer>();

	Collection<String> contentWords = new HashSet<String>();
	private boolean usingSynonymy = true;

	public OntologySentenceMatcher(String dictPath, String stopwordPath)
	{
		setStopWords(stopwordPath);
		Collections.addAll(stopWords, "i", "think", "because", "s", ",", ".", "!", "?", ":", "-", "Ñ", "'", "\"");

		addDictionary(dictPath, true);
	}

	public OntologySentenceMatcher(String stopwordPath)
	{
		super(stopwordPath);
	}

	public void setContentWords(String contentWordPath)
	{
		contentWords.clear();
		ASentenceMatcher.addAllFromFile(contentWordPath, contentWords);
	}

	public void addDictionary(String path, boolean isContentWords)
	{
		try
		{
			Scanner scanner = new Scanner(new File(path));
			while (scanner.hasNextLine())
			{
				String[] tokens = scanner.nextLine().toLowerCase().split(SPLIT_REGEX);

				if (tokens.length > 0)
				{
					for (String t : tokens)
					{
						if (!contentDict.containsKey(t))
						{
							contentDict.put(t, new ArrayList<String>());
						}
						Collections.addAll(contentDict.get(t), tokens);

						if (isContentWords)
						{
							contentWords.add(t);
							if (stopWords.contains(t))
							{
								stopWords.remove(t);
							}
						}
					}
				}
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}

	}

	public Map<String, List<String>> getContentDictionary()
	{
		return contentDict;
	}

	private static IDictionary getDict()
	{
		if (singletonDict == null)
		{
			try
			{
				// construct the URL to the Wordnet dictionary directory
				String wnhome = System.getProperty("WNHOME");
				if (wnhome == null) wnhome = ".";
				String path = wnhome + File.separator + "dict";
				URL url = new URL("file", null, path);
				// construct the dictionary object and open it
				IDictionary dict = new Dictionary(url);
				dict.open();
				singletonDict = dict;
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return singletonDict;
	}

	public static void testDictionary(String word) throws IOException
	{

		IDictionary dict = getDict();
		IIndexWord idxWord = dict.getIndexWord(word, POS.NOUN);
		for (IWordID wordID : idxWord.getWordIDs())
		{
			IWord iword = dict.getWord(wordID);
			System.out.println("Id = " + wordID);
			System.out.println("Lemma = " + iword.getLemma());
			System.out.println("Gloss = " + iword.getSynset().getGloss());
			for (ISynsetID synset : iword.getSynset().getRelatedSynsets())
				for (IWord syn : dict.getSynset(synset).getWords())
				{
					System.out.print(syn.getLemma() + ", ");
				}
			System.out.println();
		}
	}

	@Override
	public boolean isKnownWord(String word, String sentence)
	{
		String lowerWord = word.toLowerCase();
		if (stopWords.contains(lowerWord) || contentDict.keySet().contains(lowerWord)) return true;

		if (word.matches("\\d.*") || word.matches("[A-Z].*")) return false;

		if (word.matches("\\w+"))
		{
			IDictionary dict = getDict();
			for (POS pos : allPOS)
			{

				try
				{
					dict.getIndexWord(lowerWord, pos);
					return true;
				}
				catch (Exception e)
				{

				}
			}
		}

		return false;
	}

	public double getSynonymSimilarity(String one, String two)
	{
		double sim = 0.0;

		List<String> lemmas = getSynonyms(one);
		List<String> lemmas2 = getSynonyms(two);

		if (lemmas.isEmpty())
		{
			String oneS = stem(one);
			lemmas = getSynonyms(oneS);
		}

		if (lemmas2.isEmpty())
		{
			String twoS = stem(two);
			lemmas2 = getSynonyms(twoS);
		}

		for (String lemma : lemmas2)
		{
			if (lemmas.contains(lemma))
			{
				sim++;
			}
		}

		if (sim == 0)
		{
			List<String> wordNetLemmas = getWordnetSynonyms(one);
			List<String> wordNetLemmas2 = getWordnetSynonyms(two);

			if (wordNetLemmas.isEmpty())
			{
				String oneS = stem(one);
				wordNetLemmas = getSynonyms(oneS);
			}

			if (wordNetLemmas2.isEmpty())
			{
				String twoS = stem(two);
				wordNetLemmas2 = getSynonyms(twoS);
			}

			lemmas.addAll(wordNetLemmas);
			lemmas2.addAll(wordNetLemmas2);

			for (String lemma : lemmas2)
			{
				if (lemmas.contains(lemma))
				{
					sim++;
				}
			}

		}

		if (lemmas.isEmpty() || lemmas2.isEmpty()) return Double.NaN; // punt to
																		// character
																		// similarity

		sim = Math.sqrt(2 * sim / (lemmas.size() + lemmas2.size()));
		// sim = (sim/Math.min(lemmas.size(), lemmas2.size()));

		return sim;
	}

	public List<String> getSynonyms(String word)
	{

		ArrayList<String> lemmas = new ArrayList<String>();
		List<String> list = getContentDictionary().get(word);
		if (list != null)
		{
			lemmas.addAll(list);
		}

		// lemmas.addAll(getWordnetSynonyms(word));

		return lemmas;
	}

	public static List<String> getWordnetSynonyms(String word)
	{
		IDictionary dict = getDict();
		ArrayList<String> lemmas = new ArrayList<String>();
		for (POS pos : allPOS)
		{

			IIndexWord idxWord = null;
			if (word.matches("\\w+"))
			{
				try
				{
					idxWord = dict.getIndexWord(word, pos);
				}
				catch (Exception e)
				{
					System.err.println("couldn't look up " + word + " in wordnet");
				}
				if (idxWord != null)
				{
					lemmas.add(idxWord.getLemma());
					for (IWordID wordID : idxWord.getWordIDs())
					{
						IWord iword = dict.getWord(wordID);
						lemmas.add(iword.getLemma());
						for (ISynsetID synset : iword.getSynset().getRelatedSynsets())
							for (IWord syn : dict.getSynset(synset).getWords())
							// for(IWord syn : iword.getSynset().getWords())
							{
								lemmas.add(syn.getLemma());
							}
					}
				}
			}
		}
		return lemmas;
	}

	@Override
	public double getSentenceSimilarity(String incoming, String target)
	{
		return this.getSentenceSimilarity(incoming, target, 0.6, true, true);
	}

	public double getSentenceSimilarity(String incoming, String target, double lengthThreshold, boolean polarityMustMatch, boolean removeStopwords)
	{
		incoming = incoming.toLowerCase();
		target = target.toLowerCase();

		if (polarityMustMatch && !negativesMatch(incoming, target)) { return 0.0; }

		ArrayList<String> tokens1 = new ArrayList<String>(Arrays.asList(incoming.split(SPLIT_REGEX)));
		ArrayList<String> tokens2 = new ArrayList<String>(Arrays.asList(target.split(SPLIT_REGEX)));

		double lengthRatio = Math.abs(tokens1.size() - tokens2.size()) / (double) Math.max(tokens1.size(), tokens2.size());
		if (lengthRatio > lengthThreshold)
		{
			if (verbose > 1) System.out.println("sentence word counts differ too much: " + lengthRatio);
			return 0.0;
		}

		if(removeStopwords)
		{
			tokens1.removeAll(stopWords);
			tokens2.removeAll(stopWords);
		}
		
		Set<String> allTokens = new HashSet<String>();
		allTokens.addAll(tokens1);
		allTokens.addAll(tokens2);


		double sim = 0.0;
		double oneSum = 0.0;
		double twoSum = 0.0;

		for (String t : allTokens)
		{
			if(t.isEmpty()) continue;
			double wsim2 = 0;
			double wsim1 = 0;

			for (String t1 : tokens1)
			{
				if(t1.isEmpty()) continue;
				double wordSimilarity1 = getWordSimilarity(t, t1);
				if (!Double.isNaN(wordSimilarity1)) wsim1 = Math.max(wsim1, wordSimilarity1);
			}

			for (String t2 : tokens2)
			{
				if(t2.isEmpty()) continue;
				double wordSimilarity2 = getWordSimilarity(t, t2);
				if (!Double.isNaN(wordSimilarity2)) wsim2 = Math.max(wsim2, wordSimilarity2);
			}

			if (verbose > 2) System.out.println(t + ": " + wsim1 + "\t" + wsim2);

			if (contentDict.containsKey(t) || isSpecial(t))// &&
															// target.contains(t))
			{

				if (wsim1 < 0.3 || wsim2 < 0.3)
				{
					if (verbose > 1) System.out.println("\t\tno match for content word " + t + ": " + Math.min(wsim1, wsim2));
					wsim1 -= CONTENT_PENALTY;
				}

				wsim1 *= CONTENT_BONUS;
				wsim2 *= CONTENT_BONUS;
			}

			// //discount stopwords - moved to getWordSimilarity
			// if(stopWords.contains(t))
			// {
			// wsim1 *= STOPWORD_DISCOUNT;
			// wsim2 *= STOPWORD_DISCOUNT;
			// if(verbose > 3)
			// System.out.println("discounting stopword "+t);
			// }
			oneSum += wsim1 * wsim1;
			twoSum += wsim2 * wsim2;
			sim += wsim1 * wsim2;

		}

		double sSim = sim / (Math.sqrt(oneSum) * Math.sqrt(twoSum)); // cosine
		// double sSim = sim/(oneSum + twoSum - sim); //jaccard
		return sSim;

	}

	private boolean isSpecial(String t)
	{
		return t.matches("(.*\\d.*)");
	}

	@Override
	public double getWordSimilarity(String t1, String t2)
	{

		String key1 = (t1.compareTo(t2) < 0) ? (t1 + ":" + t2) : (t2 + ":" + t1);
		if (wordCache.containsKey(key1)) 
		{
			updateCount(key1); 
			return wordCache.get(key1); 
		}

		double wsim1;
		if (t1.equals(t2) || stem(t1).equals(stem(t2)))
			wsim1 = 1.0;
		else
		{
			wsim1 = usingSynonymy?getSynonymSimilarity(t1, t2):0;
			if (!usingSynonymy || Double.isNaN(wsim1))
			{
				double characterSimilarity = getCharacterSimilarity(t1, t2);
				wsim1 = CHARACTER_DISCOUNT * characterSimilarity;
				// System.out.println("falling back on character sim for "+t1+", "+t2+": "+wsim1);
			}
		}

		if (stopWords.contains(t1) ^ stopWords.contains(t2)) wsim1 *= STOPWORD_DISCOUNT;

		updateCache(key1, wsim1);

		return wsim1;
	}

	private void updateCount(String key1)
	{
		Integer count = hitCounts.get(key1);
		hitCounts.put(key1, count == null ? 1 : (count + 1));	
	}

	protected void updateCache(String key1, double wsim1)
	{
		wordCache.put(key1, wsim1);
		Integer count = hitCounts.get(key1);
		hitCounts.put(key1, count == null ? 1 : (count + 1));
	}

	public void trimCache(double keepTopFraction)
	{
		ArrayList<Entry<String, Integer>> hits = new ArrayList<Entry<String, Integer>>(hitCounts.entrySet());
		Collections.sort(hits, hitCountsComparator);
		int keepN = (int) (keepTopFraction * hits.size());

		System.out.println("keeping " + keepN + " similarity pairs(" + (keepTopFraction * 100) + "%)");
		if (!hits.isEmpty()) System.out.println(hits.get(hits.size() - 1));

		for (Entry<String, Integer> entry : hits.subList(0, hits.size() - keepN))
		{
			String key = entry.getKey();
			wordCache.remove(key);
			hitCounts.remove(key);
		}
	}

	public void clearCache(int leastHits)
	{
		ArrayList<Entry<String, Integer>> hits = new ArrayList<Entry<String, Integer>>(hitCounts.entrySet());
		// Collections.sort(hits, hitCountsComparator);

		for (Entry<String, Integer> entry : hits)
		{
			String key = entry.getKey();
			if (hitCounts.get(key) < leastHits)
			{
				wordCache.remove(key);
				hitCounts.remove(key);
			}
			else
				System.out.println("keeping " + key + "(" + hitCounts.get(key) + "):\t" + wordCache.get(key));
		}
	}

	public void clearCache()
	{
		wordCache.clear();
	}

	public int testCandidatesForContent(Collection<String> candidates, boolean all)
	{
		int bad = 0;
		for (String can : candidates)
		{
			int hits = 0;
			for (String s : contentWords)
			{
				if (can.contains(s))
				{
					hits++;
				}

			}

			if (hits < 3)
			{
				System.out.println(can + "\t" + hits + " content words\t***********");
				bad++;
			}
			else if (all) System.out.println(can + "\t" + hits + " content words");
		}
		return bad;
	}

	public static void main(String[] args) throws Exception
	{
		Collection<String> candidates = loadExpertStatements("chemistry/chem_statements.txt");

		OntologySentenceMatcher matcher = new OntologySentenceMatcher("chemistry/chem_synonyms.txt", "chemistry/stopwords.txt");
		matcher.addDictionary("synonyms.txt", false);

		// matcher.testCandidatesForContent(candidates, true);

		matcher.verbose = 2;
		Scanner scan = new Scanner(System.in);

		String s;

		System.out.println("enter a statement...");
		while (scan.hasNext() && !((s = scan.nextLine().trim()).equals("q")))
		{
			if (s.startsWith("s "))
			{
				System.out.println(matcher.getSynonyms(s.substring(2)));
			}
			else
			{
				System.out.println(s + ":");
				System.out.println(matcher.getMatch(s, 0.6, candidates));
			}
			System.out.println("enter a statement...");
		}

		// System.out.println(matcher.getCharacterSimilarity("v",
		// "vanderwaals"));
	}

	/**
	 * 
	 * @param question
	 *            a shorter length query
	 * @param statement
	 *            a candidate answer
	 * @return something like similarity between them
	 */
	public double getQuestionSimilarity(String question, String statement)
	{
		question = question.toLowerCase().replaceAll("who|what|where|when|why|how", "");
		double sim = 0;
		int q = question.length();
		int s = statement.length();

		for (int i = statement.indexOf(' ', 0); i < s && i > -1; i = statement.indexOf(' ', i + 1))
		{
			int startL = Math.max(0, statement.indexOf(' ', i - q));
			int endR = Math.min(s, statement.indexOf(' ', i - q));
			if (endR > s) endR = s;

			if (i - startL >= 2 * q / 3 && i - startL < q * 2)
			{
				double simL = getSentenceSimilarity(question, statement.substring(startL, i), 1.0, false, true);
				if (simL > sim)
				{
					// System.out.println("best = "+ statement.substring(startL,
					// i) + " "+simL);
					sim = simL;
				}
			}
			if (endR - i >= 2 * q / 3 && endR - i < q * 2)
			{
				double simR = getSentenceSimilarity(question, statement.substring(i, endR), 1.0, false, true);

				if (simR > sim)
				{
					// System.out.println("best = "+ statement.substring(i) +
					// " "+simR);
					sim = simR;
				}
			}

		}
		return sim;
	}

	public static OntologySentenceMatcher getInstance()
	{
		if (sentenceMatcher == null)
		{
			if (!System.getProperties().containsKey("WNHOME"))
			{
				System.setProperty("WNHOME", "data/synonymizer/");
			}
			sentenceMatcher = new OntologySentenceMatcher("data/synonymizer/stopwords.txt");
		}
		return sentenceMatcher;
	}

	public void enableWordNet(boolean b)
	{
		usingSynonymy = b;
	}
}
