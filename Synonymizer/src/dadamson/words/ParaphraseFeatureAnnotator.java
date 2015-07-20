package dadamson.words;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class ParaphraseFeatureAnnotator
{
	public static class MatcherResult
	{
		double sim;
		int negation;
		double unusualRatio;
		int unusuals;
		
		public MatcherResult(String s1, String s2, ASentenceMatcher matcher)
		{
			
			Set<String> unusualTokens1 = matcher.unknownWords(s1);
			Set<String> unusualTokens2 = matcher.unknownWords(s2);
			Set<String> combinedUnusualTokens = new HashSet<String>(unusualTokens1);
			combinedUnusualTokens.addAll(unusualTokens2);
			
			Set<String> sharedUnusualTokens = new HashSet<String>(unusualTokens1);
			sharedUnusualTokens.retainAll(unusualTokens2);
			
			
			 sim = matcher.getSentenceSimilarity(s1, s2);
			 negation = Math.abs(matcher.negativeParity(s1, s2));
			 unusuals = combinedUnusualTokens.size() - sharedUnusualTokens.size();
			 
			 unusualRatio = sharedUnusualTokens.size()/(double)combinedUnusualTokens.size();
			if(Double.isNaN(unusualRatio))
				unusualRatio = 1.0;
		}
		
		public static String headers(String suffix)
		{
			return "sim"+suffix+",negation"+suffix+",neg_parity"+suffix+",unusuals"+suffix+",unusual_ratio"+suffix;
		}
		
		public String toString()
		{
			return sim+","+negation+","+negation%2+","+unusuals+","+unusualRatio;
		}
	}
	
	public static void annotate(String datafile, String outfile, boolean minAndMax, ASentenceMatcher... matchers)
	{
		try
		{
			PrintWriter p = new PrintWriter(outfile);
			p.print("quality,unique_words,length_ratio,");
					
			for(int i = 0; i < matchers.length; i++)
			{
				p.print(MatcherResult.headers("_"+i));
				p.print(",");
			}
			
			if(minAndMax)
				p.println("max_sim,min_sim");
		
			
			Scanner scan = new Scanner(new File(datafile));
			scan.nextLine();//consume header
			for(int i = 1; scan.hasNext(); i++)
			{
				String[] line = scan.nextLine().split("\t");
				
				String q = line[0];
				
				String s1 = line[3];
				String s2 = line[4];
				System.out.println(i);
//				System.out.println(s1);
//				System.out.println(s2);
				
				String[] s1Tokens = s1.split("\\s+");
				String[] s2Tokens = s2.split("\\s+");
				
				Set<String> sharedTokens = new HashSet<String>();
				Set<String> tokens1 = new HashSet<String>();
				Set<String> tokens2 = new HashSet<String>();
				Collections.addAll(sharedTokens, s1Tokens);
				Collections.addAll(tokens1, s1Tokens);
				Collections.addAll(tokens2, s2Tokens);

				double sharedRatio = sharedTokens.size()/(double)tokens2.size();
				double lengthRatio = Math.abs(1.0 - s1Tokens.length/(double)s2Tokens.length);
				
				p.printf("%s,%f,%f,", q,sharedRatio,lengthRatio);
				//System.out.printf("%s,%f,%f,", q,sharedRatio,lengthRatio);
			
				double max = 0;
				double min = Double.MAX_VALUE;
				for(ASentenceMatcher matcher : matchers)
				{
					MatcherResult result = new MatcherResult(s1, s2, matcher);
					p.print(result);
					//System.out.print(result);
					p.print(",");
					
					max = Math.max(max, result.sim);
					min = Math.min(min, result.sim);
				}
				
				if(minAndMax)
				{
					p.print(max+","+min);
				}
				p.println();
				//System.out.println();
				 	
				p.flush();
			}
			p.close();

		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		MatrixSentenceMatcher mMatcher  = new MatrixSentenceMatcher("msr_raw.words.txt", "msr_tfidf_matrix.txt", "stopwords.txt");
		MatrixSentenceMatcher mcMatcher = new MatrixSentenceMatcher("msr_raw.words.txt", "msr2ctx_matrix.txt", "stopwords.txt");
		MatrixSentenceMatcher mrMatcher = new MatrixSentenceMatcher("msr_raw.words.txt", "msr2rtw_matrix.txt", "stopwords.txt");
		SynonymSentenceMatcher sMatcher = new SynonymSentenceMatcher("stopwords.txt");
		MaxSentenceMatcher maxMatcher = new MaxSentenceMatcher(sMatcher, mMatcher, mcMatcher, mrMatcher);
		annotate("msr_paraphrase_train.txt", "results/matrix_combo_scores.csv", true, maxMatcher, sMatcher, mMatcher, mcMatcher, mrMatcher);
	}
}
