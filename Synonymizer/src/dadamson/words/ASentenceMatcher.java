package dadamson.words;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public abstract class ASentenceMatcher
{
	public abstract double getWordSimilarity(String t1, String t2);
	public abstract boolean isKnownWord(String word, String sentence);

	protected   double STOPWORD_DISCOUNT = 0.66;
	protected   double CHARACTER_DISCOUNT = 0.33;
	public static   String SPLIT_REGEX = "((<[\\w/]+>)|([\\s\\p{Ps}\\p{Pe}\\p{Pi}\\p{Pf}\\p{Pc}\\p{Po}]))+";
	private static SnowballStemmer singleStemmer;
	
	//TODO: break out to file
//	private String[] negatives = {"inversely", "inverse", "decrease", "limit", "reduc", " less ", "lost", "lose", "smaller", "down","weaker",
//				"no ", "not ", "n't ", " dont ", " isnt ", " wont ", " never", "refute", "deny", "denied", "decreased" };
//	private Pattern negPattern = Pattern.compile("(in|un|dis)|inversely|inverse|decrease|limit|reduc|less|lost|lose|smaller|down|weaker|no|not|n't|dont|isnt|wont|never|refute|deny|denied|decreased|neither|cant");
	protected Pattern negPattern = Pattern.compile("((^| )(no|not|dont|isnt|wont|never|neither|can't|cant|don|won|don't|won't|aren't|arent|doesnt|doesn't)( |$))|((n't)( |$))");
	protected Collection<String> stopWords = new HashSet<String>();
	protected int verbose = 0;


	protected String[] tokenize(String sentence)
	{
		return sentence.toLowerCase().split(SPLIT_REGEX);
	}
	
	public Set<String> unknownWords(String sentence)
	{
		HashSet<String> unknown = new HashSet<String>();
		String[] words = tokenize(sentence);
		
		
		for(String w : words)
			if(w.length() > 0 && !isKnownWord(w, sentence))
				unknown.add(w.toLowerCase());
				
		if(!unknown.isEmpty() && verbose > 2)
			System.out.println("unknown: "+unknown);
		
		return unknown;
	}
	
	private static SnowballStemmer getStemmer()
	{
		if(singleStemmer == null)
			singleStemmer = new englishStemmer();
		
		return singleStemmer;
	}

	public int getVerbosity()
	{
		return verbose;
	}

	public void setVerbosity(int verbose)
	{
		this.verbose = verbose;
	}

	protected void setStopWords(String path)
	{
		stopWords.clear();
		addAllFromFile(path, stopWords);
	}

	
	public static void addAllFromFile(String path, Collection<String> list)
	{
		try
		{
			Scanner scanner = new Scanner(new File(path));
			while( scanner.hasNextLine())
			{
				list.add(scanner.nextLine());
			}
		}
		catch(Exception e)
		{
			System.err.println("error adding stopwords from "+path);
		}
	}

	protected static String stem(String one)
	{
		SnowballStemmer stemmer = getStemmer();
		stemmer.setCurrent(one);
		stemmer.stem();
	
		String oneS = stemmer.getCurrent();
		return oneS;
	}

	private double getBigramSimilarity(String a1, String a2, String b1, String b2)
	{
		return getWordSimilarity(a1, b1) * getWordSimilarity(a2, b2);
	}

	public static Collection<String> loadExpertStatements(String expertPath)
	{
		Collection<String> candidates = new ArrayList<String>();
		File expertFile = new File(expertPath);
		try
		{
			Scanner s = new Scanner(expertFile);
			while(s.hasNextLine())
			{
				candidates.add(s.nextLine());
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		return candidates;
	}

	public double getSentenceBigramSimilarity(String incoming, String target)
	{
		
		String[] tokens1 = tokenize(incoming);
		String[] tokens2 = tokenize(target);
		
		double sim = 0.0;
		double oneSum = 0.0;
		double twoSum = 0.0;
	
		for(int i = 0; i < tokens2.length - 1; i++)
		{
			double bSim = 0;
			
			for(int j = 0; j < tokens1.length - 1; j++)
			{
					bSim = Math.max(bSim, getBigramSimilarity(tokens2[i], tokens2[i+1], tokens1[j], tokens1[j+1]));
			}
	
			oneSum += bSim*bSim;
			twoSum += 1;
			sim+= bSim;
		}
		
		for(int i = 0; i < tokens1.length - 1; i++)
		{
			double bSim = 0;
			
			for(int j = 0; j < tokens2.length - 1; j++)
			{
					bSim = Math.max(bSim, getBigramSimilarity(tokens1[i], tokens1[i+1], tokens2[j], tokens2[j+1]));
			}
	
			twoSum += bSim*bSim;
			oneSum += 1;
			sim+= bSim;
		}
	
		double sSim = sim/(Math.sqrt(oneSum)*Math.sqrt(twoSum)); //cosine
		//double sSim = sim/(oneSum + twoSum - sim); //jaccard
		return sSim;
		
	}

	public double getCharacterSimilarity(String w1, String w2)
	{
	
		int[] oneCounts = getCharFrequency(w1);
		int[] twoCounts = getCharFrequency(w2);
		
		double sim = 0;
		double oneSum = 0;
		double twoSum = 0;
		
		for(int i = 0; i < 27; i++)
		{
			sim += oneCounts[i]*twoCounts[i];
			oneSum += oneCounts[i]*oneCounts[i];
			twoSum += twoCounts[i]*twoCounts[i];
		}
		
		double cSim = sim/(Math.sqrt(oneSum)*Math.sqrt(twoSum));
		//double cSim = sim/(oneSum + twoSum - sim); //jaccard
		if(verbose > 2)
			System.out.println("char sim: "+w1 +" vs "+w2+" = "+cSim);
		return cSim;
	}

	private int[] getCharFrequency(String w1)
	{
		int[] oneCounts = new int[27];
		//w1 = w1.toLowerCase();
		for(int i = 0; i < w1.length(); i++)
		{
			int cIndex = w1.charAt(i) - 'a';
			if(cIndex < 0 || cIndex > 25)
				cIndex = 26;
			oneCounts[cIndex]++;
		}
		return oneCounts;
	}

	public double getSentenceSimilarity(String incoming, String target)
	{
		incoming = incoming.toLowerCase();
		target = target.toLowerCase();
		
		String[] tokens1 = tokenize(incoming);
		String[] tokens2 = tokenize(target);
		
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
	
	public Object[] getMatch(String input, List<String> candidates)
	{
		double bestSimilarity = 0.0;
		String bestMatch = candidates.get(0);
		
		input = input.toLowerCase();
		
		Collection<String> inputs = new ArrayList<String>();
		inputs.add(input);
		
	
			if(verbose > 0)
				System.out.println("("+input+")");
			for(String can : candidates)
			{
				can = can.toLowerCase();
				double sim = //getSentenceBigramSimilarity(inpart, can);
						getSentenceSimilarity(input, can);
	
	
				if(verbose > 1 || (verbose > 0) )
					System.out.println(sim+"\t"+can);
				
				if(sim > bestSimilarity)
				{
					bestSimilarity = sim;
					bestMatch = can;
				}
			}
		
		if(verbose > 0)
		System.out.println("best match:\t"+bestSimilarity+"\t"+bestMatch);
		return new Object[]{bestMatch, bestSimilarity};
		
	}

	public String getMatch(String input, double threshold, Collection<String> candidates)
	{
		double bestSimilarity = threshold*.999999999;
		String bestMatch = null;
		
		input = input.toLowerCase();
		
		Collection<String> inputs = new ArrayList<String>();
		inputs.add(input);
		String[] split = input.split("(, and)|(, so)|((, )?because)|(i think( that)?)|(therefore)|((my)|(i) )?predict(ion)?( (is)|(that))?");
		if(split.length >0 && !split[0].equals(input))
			Collections.addAll(inputs, split);
		
		for(String inpart : inputs)
		{
			inpart = inpart.trim();
			if(inpart.isEmpty())
				continue;
	
			if(verbose > 0)
				System.out.println("("+inpart+")");
			for(String can : candidates)
			{
				String orig = can;
				can = can.toLowerCase();
				double sim = //getSentenceBigramSimilarity(inpart, can);
						getSentenceSimilarity(inpart, can);
	
	
				if(verbose > 1 || (verbose > 0 && sim > threshold) )
					System.out.println(sim+"\t"+orig);
				
				if(sim > bestSimilarity && negativesMatch(inpart, can))
				{
					bestSimilarity = sim;
					bestMatch = orig;
				}
			}
		}
		if(verbose > 0)
			System.out.println("best match:\t"+bestSimilarity+"\t"+bestMatch);
		if (bestSimilarity > threshold)
		{
			if(verbose < 0)
				System.out.println(input+"\t"+bestMatch+"\t"+bestSimilarity);
			return bestMatch;
		}
		return null;
	}
	
	public static class SentenceMatch implements Comparable<SentenceMatch>
	{
		public double sim;
		public String matchText;
		public SentenceMatch(String orig, double sim2)
		{
			matchText = orig;
			sim = sim2;
		}
		@Override
		public int compareTo(SentenceMatch arg0)
		{
			return -((Double)sim).compareTo(arg0.sim);
		}
	}
	
	public List<SentenceMatch> getMatches(String input, double threshold, Collection<String> candidates)
	{
		double bestSimilarity = threshold*.999999999;
		String bestMatch = null;
		
		input = input.toLowerCase();

		List<SentenceMatch> matches = new ArrayList<SentenceMatch>();
		Collection<String> inputs = new ArrayList<String>();
		inputs.add(input);
		String[] split = input.split("(, and)|(, so)|((, )?because)|(i think( that)?)|(therefore)|((my)|(i) )?predict(ion)?( (is)|(that))?");
		if(split.length >0 && !split[0].equals(input))
			Collections.addAll(inputs, split);
		
		for(String inpart : inputs)
		{
			inpart = inpart.trim();
			if(inpart.isEmpty())
				continue;
	
			if(verbose > 0)
				System.out.println("("+inpart+")");
			for(String can : candidates)
			{
				String orig = can;
				can = can.toLowerCase();
				double sim = //getSentenceBigramSimilarity(inpart, can);
						getSentenceSimilarity(inpart, can);
	
	
				if(verbose > 1 || (verbose > 0 && sim > threshold) )
					System.out.println(sim+"\t"+orig);
				
				if(sim >= threshold && negativesMatch(inpart, can))
				{
					matches.add(new SentenceMatch(orig, sim));
				}
			}
		}
		if(verbose > 0)
			System.out.println("best match:\t"+bestSimilarity+"\t"+bestMatch);
		if (bestSimilarity > threshold)
		{
			if(verbose < 0)
				System.out.println(input+"\t"+bestMatch+"\t"+bestSimilarity);
			
		}
		Collections.sort(matches);
		return matches;
	}

	public boolean negativesMatch(String s1, String s2)
	{
//		boolean neg1=false, neg2=false;
//		
//		for(String neg : negatives )
//		{
//			neg1 = neg1 || s1.contains(neg);
//			neg2 = neg2 || s2.contains(neg);
//		}
//		return !(neg2 ^ neg1);
		
		return negativeParity(s1, s2)%2 == 0;
	}

	public int negativeParity(String s1, String s2)
	{
		return negativeParity(tokenize(s1), tokenize(s2));
		
	}
	public int negativeParity(String[] s1, String[] s2)
	{
		int neg1=0, neg2=0;
		
		for(String s : s1)
		{
			neg1 += negPattern.matcher(s).find()?1:0;
			
		}
		for(String s : s2)
		{
			neg2 += negPattern.matcher(s).find()?1:0;
		}
		
		return (neg2 - neg1);
	}

	public static void main(String [] args) throws Exception
	{
		//Collection<String> candidates = loadExpertStatements("chemistry/chem_statements.txt");
	
		ASentenceMatcher matcher = new SynonymSentenceMatcher("stopwords.txt");
		//matcher.addDictionary("synonyms.txt", false);
		//matcher.testCandidatesForContent(candidates, true);
		
		matcher.verbose = 2;
		Scanner scan = new Scanner(System.in);
		
		String s, s2;
	
		System.out.println("enter a pair of sentences...");
		while(scan.hasNextLine())
		{
			s = scan.nextLine().trim().toLowerCase();
			s2 = scan.nextLine().trim().toLowerCase();
			System.out.println(s + " vs "+ s2);
			System.out.println(matcher.getSentenceSimilarity(s, s2));
			System.out.println("enter a pair of sentences...");
		}
		
	}

	public ASentenceMatcher(String stopwordPath)
	{
		setStopWords(stopwordPath);
	}
	
	public ASentenceMatcher()
	{
		super();
	}

	

}