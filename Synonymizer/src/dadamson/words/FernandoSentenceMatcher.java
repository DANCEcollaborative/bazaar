package dadamson.words;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;


import edu.mit.jwi.*;
import edu.mit.jwi.Dictionary;
import edu.mit.jwi.item.*;



public class FernandoSentenceMatcher extends ASentenceMatcher
{
	private static IDictionary singletonDict;
	private static POS[] allPOS = {POS.ADJECTIVE, POS.ADVERB, POS.NOUN, POS.VERB};

	protected static final double CONTENT_BONUS = 1.5;
	protected static final double CONTENT_PENALTY = 1.0;
	
	private Map<String, List<String>> contentDict = new HashMap<String, List<String>>();
	private Map<String, Double> wordCache = new HashMap<String, Double>();
	
	Collection<String> contentWords = new HashSet<String>();
	public FernandoSentenceMatcher(String dictPath, String stopwordPath)
	{
		setStopWords(stopwordPath);
		Collections.addAll(stopWords, "think", "because", "predict", "s", ",", ".","!", "?",":","-","'","\"");
		
		addDictionary(dictPath,true);
	}
	
	public FernandoSentenceMatcher(String stopwordPath)
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
			while( scanner.hasNextLine())
			{
				String[] tokens = scanner.nextLine().toLowerCase().split(SPLIT_REGEX);
				if(tokens.length > 0)
				{
					for(String t : tokens)
					{
						if(!contentDict.containsKey(t))
						{
							contentDict.put(t, new ArrayList<String>());
						}
						Collections.addAll(contentDict.get(t), tokens);
						
						if(isContentWords && !stopWords.contains(t))
							contentWords.add(t);
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
		if(singletonDict == null)
		{
			try
			{
				// construct the URL to the Wordnet dictionary directory
				String wnhome = System.getenv("WNHOME");
				if(wnhome == null)
					wnhome = ".";
				String path = wnhome + File.separator + "dict"; URL url = new URL("file", null, path);
				// construct the dictionary object and open it
				IDictionary dict = new Dictionary(url); 
				dict.open();
				singletonDict = dict;
			}
			catch(IOException e)
			{e.printStackTrace();}
		}
		return singletonDict;
	}
	
	public static void testDictionary(String word) throws IOException 
	{

		IDictionary dict = getDict();
		IIndexWord idxWord = dict.getIndexWord(word, POS.NOUN);
		for(IWordID wordID : idxWord.getWordIDs())
		{
			IWord iword = dict.getWord(wordID);
			System.out.println("Id = " + wordID); 
			System.out.println("Lemma = " + iword.getLemma()); 
			System.out.println("Gloss = " + iword.getSynset().getGloss());
			for(ISynsetID synset : iword.getSynset().getRelatedSynsets())
			for(IWord syn : dict.getSynset(synset).getWords())
			{
				System.out.print(syn.getLemma()+", ");
			}
			System.out.println();
		}
	}
	
	@Override
	public boolean isKnownWord(String word, String sentence)
	{
		String lowerWord = word.toLowerCase();
		if(stopWords.contains(lowerWord) || contentDict.keySet().contains(lowerWord))
			return true;
		
		if(word.matches("\\d.*") || word.matches("[A-Z].*"))
			return false;

		if(word.matches("\\w+") )
		{
			IDictionary dict = getDict();
			for(POS pos : allPOS)
			{
				
					try
					{
						dict.getIndexWord(lowerWord, pos);
						return true;
					}
					catch(Exception e)
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
		
		if(lemmas.isEmpty())
		{
			String oneS = stem(one);
			lemmas = getSynonyms(oneS);
		}

		if(lemmas2.isEmpty())
		{
			String twoS = stem(two);
			lemmas2 = getSynonyms(twoS);
		}
		
		if(lemmas.isEmpty() || lemmas2.isEmpty())
			return Double.NaN;
		
		for(String lemma : lemmas2)
		{
			if(lemmas.contains(lemma))
			{
				sim++;
			}
		}
		
		sim = Math.sqrt(2*sim/(lemmas.size() + lemmas2.size()));
//		sim = (sim/Math.min(lemmas.size(), lemmas2.size()));

		return sim;
	}

	public List<String> getSynonyms(String word)
	{

		ArrayList<String> lemmas = new ArrayList<String>();
		List<String> list = getContentDictionary().get(word);
		if(list != null)
			lemmas.addAll(list);

		lemmas.addAll(getWordnetSynonyms(word));
		
		return lemmas;
	}

	private static List<String> getWordnetSynonyms(String word)
	{
		IDictionary dict = getDict();		
		ArrayList<String> lemmas = new ArrayList<String>();
		for(POS pos : allPOS)
		{
			
			IIndexWord idxWord = null;
			if(word.matches("\\w+"))
			{
				try
				{
						idxWord = dict.getIndexWord(word, pos);
				}
				catch(Exception e)
				{
					System.err.println("couldn't look up "+word+ " in wordnet");
				}
				if(idxWord != null)
				{
					lemmas.add(idxWord.getLemma());
					for(IWordID wordID : idxWord.getWordIDs())
					{
						IWord iword = dict.getWord(wordID);
						lemmas.add(iword.getLemma());
						for(ISynsetID synset : iword.getSynset().getRelatedSynsets())
						for(IWord syn : dict.getSynset(synset).getWords())
						//for(IWord syn : iword.getSynset().getWords())
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
	public double getSentenceSimilarity(String incoming, String target) //swapped?
	{

//		if(!negativesMatch(incoming, target))
//		{
//			return 0.0;
//		}
		
		String[] tokens1 = incoming.split(SPLIT_REGEX);
		String[] tokens2 = target.split(SPLIT_REGEX);
		
		Set<String> allTokens = new HashSet<String>();
		Collections.addAll(allTokens, tokens1);
		Collections.addAll(allTokens, tokens2);
		
		//allTokens.removeAll(stopWords);
		//System.out.println(allTokens);
		
		double sim = 0.0;
		double oneSum = 0.0;
		double twoSum = 0.0;
		
		for(String t: allTokens)
		{
			double wsim2 = 0;
			double wsim1 = 0;
			
			for(String t1: tokens1)
			{
					double wordSimilarity1 = getWordSimilarity(t, t1);
					if(!Double.isNaN(wordSimilarity1))
						wsim1 = Math.max(wsim1, wordSimilarity1);
			}
			
			for(String t2: tokens2)
			{
					double wordSimilarity2 = getWordSimilarity(t, t2);
					if(!Double.isNaN(wordSimilarity2))
						wsim2 = Math.max(wsim2, wordSimilarity2);
			}
			
			if(verbose > 2)
				System.out.println(t+": "+wsim1+"\t"+wsim2);
			
			if(contentDict.containsKey(t) )//&& target.contains(t))
			{
				
				if(wsim1 < 0.3)
				{
					if(verbose > 1)
					System.out.println("\t\tno match for content word "+t+": "+wsim1);
					wsim1 -= CONTENT_PENALTY;
				}
				
				wsim1 *= CONTENT_BONUS;
				wsim2 *= CONTENT_BONUS;
			}
			
			//discount stopwords
			if(stopWords.contains(t))
			{
				wsim1 *= STOPWORD_DISCOUNT;
				wsim2 *= STOPWORD_DISCOUNT;
			}

			oneSum += wsim1*wsim1;
			twoSum += wsim2*wsim2;
			sim+= wsim1*wsim2;
		}

		double sSim = sim/(Math.sqrt(oneSum)*Math.sqrt(twoSum)); //cosine
		//double sSim = sim/(oneSum + twoSum - sim); //jaccard
		return sSim;
		
	}

	@Override
	public double getWordSimilarity(String t1, String t2)
	{
		//TODO: sim = 1/[IC(t1)+IC(t2)+2xIC(LCS(t1, t2))]
		//IC(t) = -log(P(t)) wrt to some (weighted) collection of corpora
		
		String key1 = t1+":"+t2;
		if(wordCache.containsKey(key1))
			return wordCache.get(key1);
		
		double wsim1;
		if(t1.equals(t2) || stem(t1).equals(stem(t2)))
			wsim1 = 1.0;
		else
		{
			wsim1 = getSynonymSimilarity(t1, t2);
			if(Double.isNaN(wsim1))
			{
				double characterSimilarity = getCharacterSimilarity(t1, t2);
				wsim1 = CHARACTER_DISCOUNT*characterSimilarity;
//				System.out.println("falling back on character sim for "+t1+", "+t2+": "+wsim1);
			}
		}
		
		
		if(wsim1 < 0.8)
			wsim1 = 0;

		String key2 = t2+":"+t1;
		wordCache.put(key1, wsim1);
		wordCache.put(key2, wsim1);
		
		
		return wsim1;
	}
	
	public int testCandidatesForContent(Collection<String> candidates, boolean all)
	{
		int bad = 0;
		for(String can : candidates)
		{
			int hits = 0;
			for(String s : contentWords)
			{
				if(can.contains(s))
				{
					hits++;
				}
					
			}
			
	
			if(hits < 3)
			{
				System.out.println(can+"\t"+hits+" content words\t***********");
				bad ++;
			}
			else if(all)
				System.out.println(can+"\t"+hits+" content words");
		}
		return bad;
	}
	
	public static void main(String [] args) throws Exception
	{
		Collection<String> candidates = loadExpertStatements("chemistry/chem_statements.txt");
	
		FernandoSentenceMatcher matcher = new FernandoSentenceMatcher("chemistry/chem_synonyms.txt", "chemistry/stopwords.txt");
		matcher.addDictionary("synonyms.txt", false);
		
		//matcher.testCandidatesForContent(candidates, true);
		
		matcher.verbose = 2;
		Scanner scan = new Scanner(System.in);
		
		String s;
	
		System.out.println("enter a statement...");
		while(scan.hasNext() && !((s = scan.nextLine().trim()).equals("q")))
		{
			if(s.startsWith("s "))
			{
				System.out.println(matcher.getSynonyms(s.substring(2)));
			}
			else
			{
				System.out.println(s+":");
				System.out.println(matcher.getMatch(s, 0.6, candidates));
			}
			System.out.println("enter a statement...");
		}
		
		//System.out.println(matcher.getCharacterSimilarity("v", "vanderwaals"));
	}
}
