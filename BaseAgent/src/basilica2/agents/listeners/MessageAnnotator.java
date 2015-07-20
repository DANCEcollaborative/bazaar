/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.agents.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author rohitk
 */
public class MessageAnnotator implements BasilicaPreProcessor
{

	public static String GENERIC_NAME = "MessageAnnotator";
	public static String GENERIC_TYPE = "Filter";

	private Map<String, List<String>> dictionaries = new HashMap<String, List<String>>();

	public MessageAnnotator()
	{
		// super(a, n, pf);

		File dir = new File("dictionaries");

		loadDictionaryFolder(dir);
	}

	private void loadDictionaryFolder(File dir)
	{
		File[] dictNames = dir.listFiles();

		for (File dictFile : dictNames)
		{
			if (dictFile.isDirectory())
				loadDictionaryFolder(dictFile);
			else if(dictFile.getName().endsWith(".txt"))
			{
				String name = dictFile.getName().replace(".txt", "").toUpperCase();
				dictionaries.put(name, loadDictionary(dictFile));
			}
		}
	}

	private void updateAnnotations(String from, String text, String annotations, String agentName)
	{
		try
		{
			String filename = "behaviors" + File.separator + "students" + File.separator + agentName + ".studentannotations.txt";
			FileWriter fw = new FileWriter(filename, true);
			fw.write(agentName + "," + from + "," + text.replace(",", " ") + "," + annotations + "\n");
			fw.flush();
			fw.close();
		}
		catch (Exception e)
		{
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Error while updating Status File (" + e.toString() + ")");
		}
	}

	private List<String> loadDictionary(File dict)
	{
		List<String> dictionary = new ArrayList<String>();
		try
		{
			BufferedReader fr = new BufferedReader(new FileReader(dict));

			String line = fr.readLine();
			while (line != null)
			{
				line = line.trim();
				if (line.length() > 0)
				{
					dictionary.add(line.trim());
				}
				line = fr.readLine();
			}
			fr.close();
		}
		catch (Exception e)
		{
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Error while reading Dictionary: " + dict.getName() + " (" + e.toString() + ")");
		}
		return dictionary;
	}

	private List<String> matchDictionary(String text, List<String> dictionary)
	{
		String text2 = " " + text;
		List<String> matchedTerms = new ArrayList<String>();
		for (int j = 0; j < dictionary.size(); j++)
		{
			String entry = dictionary.get(j);
			try
			{
				if (entry.startsWith("/") && entry.endsWith("/"))
				{
					String regex = ".*" + entry.substring(1, entry.length() - 1) + ".*";
					if (text.matches(regex))// text.contains(" "
																							// +
																							// dictionary.get(j)))
					{
						matchedTerms.add(entry);
					}
				}
				else if (text2.contains(" " + entry))
				{
					matchedTerms.add(entry);
				}
			}
			catch (Exception e)
			{
				Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "problem matching against line " + j + ": " + entry);
			}

		}
		return matchedTerms;
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent)
		{
			handleMessageEvent(source, (MessageEvent) e);
		}
	}

	private void handleMessageEvent(InputCoordinator source, MessageEvent me)
	{
		String text = me.getText();
		String normalizedText = normalize(text);
		MessageEvent newme = me;// new MessageEvent(source, me.getFrom(),
								// me.getText());

		// NORMAL ANNOTATIONS
		for (String key : dictionaries.keySet())
		{
			List<String> dictionary = dictionaries.get(key);
			List<String> namesFound = matchDictionary(normalizedText, dictionary);
			if (namesFound.size() > 0)
			{
				newme.addAnnotation(key, namesFound);
			}
		}


		
		//old rohitk
//		updateAnnotations(me.getFrom(), me.getText(), newme.getAnnotationString(), source.getAgent().getName());

		//Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL, "annotations:" + newme.getAnnotationString());
		// source.addPreProcessingEvent(newme);
	}

	public static String normalize(String text)
	{
		if(text == null)
			return text;
		
		String rettext = text.replace(",", " , ");
		rettext = rettext.replace(".", " . ");
		rettext = rettext.replace("?", " ? ");
		rettext = rettext.replace("!", " ! ");
		rettext = rettext.replace("'", "'");
		rettext = rettext.replace("\"", " \" ");
		rettext = rettext.trim();
		rettext = rettext.replace("  ", " ");
		rettext = rettext.replace("  ", " ");
		rettext = rettext.replace("  ", " ");
		rettext = rettext.replace("\t", " ");
		rettext = rettext.toLowerCase();
		return rettext;
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { MessageEvent.class };
	}

}
