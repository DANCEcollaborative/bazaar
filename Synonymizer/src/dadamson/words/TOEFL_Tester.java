package dadamson.words;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;

public class TOEFL_Tester
{
	public static void test(String questionFile, String outfile)
	{
		ASentenceMatcher matcher = new SynonymSentenceMatcher("stopwords.txt");
		try
		{
			PrintWriter p = new PrintWriter(outfile);
		
			Scanner scan = new Scanner(new File(questionFile));
			String[] labels = {"a","b","c","d"};
			for(int i = 1; scan.hasNext(); i++)
			{
				String q = scan.nextLine();
				System.out.println(q);
				
				ArrayList<String>choices = new ArrayList<String>();
				Collections.addAll(choices, scan.nextLine(),scan.nextLine(),scan.nextLine(),scan.nextLine());
				System.out.println(choices);
				Object[] match = matcher.getMatch(q, choices);
				
				String output = i+"\t"+labels[choices.indexOf(match[0])]+"\t"+match[1];
				p.println(output);
				p.flush();
				System.out.println(output);
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
		test(args[0], args[1]);
	}
}
