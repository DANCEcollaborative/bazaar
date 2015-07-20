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
package basilica2.agents.data;

import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * @author dadamson
 */
public class PromptTable
{

	public static String GENERIC_NAME = "PromptingActor";
	public static String GENERIC_TYPE = "Actor";
	protected String prompt_filename = "prompts.xml";
	protected String output_component_name = "myOutputCordinator";
	protected Map<String, List<String>> prompts = null;

	public PromptTable()
	{
		this("prompts.xml");
	}

	public PromptTable(String filename)
	{
		setPromptsFilename(filename);
	}

	public void setPromptsFilename(String filename)
	{
		prompt_filename = filename;
		loadPrompts(prompt_filename);
	}

	protected void loadPrompts(String filename)
	{
		prompts = new Hashtable<String, List<String>>();
		try
		{
			DOMParser parser = new DOMParser();
			parser.parse(filename);
			Document dom = parser.getDocument();
			NodeList ns1 = dom.getElementsByTagName("prompts");
			if ((ns1 != null) && (ns1.getLength() != 0))
			{
				Element promptsElement = (Element) ns1.item(0);
				NodeList ns2 = promptsElement.getElementsByTagName("prompt");
				if ((ns2 != null) && (ns2.getLength() != 0))
				{
					for (int i = 0; i < ns2.getLength(); i++)
					{
						Element promptElement = (Element) ns2.item(i);
						String promptId = promptElement.getAttribute("id");
						NodeList ns3 = promptElement.getElementsByTagName("text");
						if ((ns3 != null) && (ns3.getLength() != 0))
						{
							List<String> promptTexts = new ArrayList<String>();
							for (int j = 0; j < ns3.getLength(); j++)
							{
								Element textElement = (Element) ns3.item(j);
								promptTexts.add(textElement.getTextContent());
							}
							prompts.put(promptId, promptTexts);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			// log

			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Unable to load prompts (" + e.toString() + ")");
			e.printStackTrace();
		}
	}

	public String lookup(String promptName)
	{
		return lookup(promptName, null);
	}

	public String lookup(String promptName, Map<String, String> slotFillers)
	{
		// Do the Prompt
		List<String> promptTexts = prompts.get(promptName);
		if (promptTexts != null)
		{
			int promptIndex = (int) Math.floor(promptTexts.size() * Math.random());
			String promptText = promptTexts.get(promptIndex);
			if (slotFillers != null)
			{
				String[] slots = slotFillers.keySet().toArray(new String[0]);
				for (int i = 0; i < slots.length; i++)
				{
					String filler = slotFillers.get(slots[i]);
					if(filler != null)
						promptText = promptText.replace(slots[i], filler);
				}
			}
			return promptText;

		}
		else
		{
			return promptName;
		}
	}

	public Set<String> keySet()
	{
		return prompts.keySet();
	}

	public void add(String key, String value)
	{
		if(!prompts.containsKey(key))
			prompts.put(key, new ArrayList<String>());
		prompts.get(key).add(value);
	}
}
