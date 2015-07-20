package dadamson.words;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MatrixSentenceMatcher extends ASentenceMatcher
{

	double[][] similarities;
	Map<String, Integer> wordIndex = new HashMap<String, Integer>();
	
	public MatrixSentenceMatcher(String wordIndexFile, String matrixFile, String stopwordsFile)
	{
		super(stopwordsFile);
		loadWordIndex(wordIndexFile);
		System.out.println("loaded word index.");
		loadSimilarityMatrix(matrixFile);
		System.out.println("loaded matrix.");
	}
	
	public void loadSimilarityMatrix(String path)
	{//zero-indexed
		
		int n = wordIndex.size();
		similarities = new double[n][n];
		try
		{
			Scanner scanner = new Scanner(new File(path));
			for(int i = 0; i < n; i++)
			{
				if(i%100 == 0)
					System.out.println(i);
				
				for(int j = 0; j < i; j++)
				{
					similarities[i][j] = scanner.nextDouble();
					similarities[j][i] = similarities[i][j];
				}
			}
		}
		catch(Exception e)
		{
			System.err.println("error loading matrix from "+path);
		}
	}
	
	public void loadWordIndex(String path)
	{//zero-indexed
		try
		{
			Scanner scanner = new Scanner(new File(path));
			for(int i = 0; scanner.hasNextLine(); i++)
			{
				wordIndex.put(scanner.nextLine().trim(), i);
			}
		}
		catch(Exception e)
		{
			System.err.println("error loading word index from "+path);
		}
	}
	
	@Override
	public double getWordSimilarity(String t1, String t2)
	{
		if(wordIndex.containsKey(t1) && wordIndex.containsKey(t2))
		{
			return Math.abs(similarities[wordIndex.get(t1)][wordIndex.get(t2)]);
		}
		else return CHARACTER_DISCOUNT * getCharacterSimilarity(t1, t2);
	}

	@Override
	public boolean isKnownWord(String word, String sentence)
	{
		return wordIndex.containsKey(word.toLowerCase());
	}
	public static void main(String [] args) throws Exception
	{

		MatrixSentenceMatcher matcher = new MatrixSentenceMatcher("msr_raw.words.txt", "msr_raw_matrix.txt", "stopwords.txt");
		
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
			matcher.unknownWords(s);
			matcher.unknownWords(s2);
			System.out.println("enter a pair of sentences...");
		}
		
	}

}
