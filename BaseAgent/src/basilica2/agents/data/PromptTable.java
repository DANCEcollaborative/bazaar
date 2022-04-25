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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    protected Properties properties;
	protected Map<String, String> intentions = null;
	protected Boolean includeIntention = false; 

	public PromptTable()
	{
		this("prompts.xml");
	}

	public PromptTable(String filename)
	{
		initProperties("PromptTable.properties"); 
		try{includeIntention = Boolean.parseBoolean(getProperties().getProperty("include_intention", "false"));}
		catch(Exception e) {e.printStackTrace();}
		
		setPromptsFilename(filename);
		
	}

	public void setPromptsFilename(String filename)
	{
		prompt_filename = filename;
		loadPrompts(prompt_filename);
		if (includeIntention) {
			loadIntentions(prompt_filename);
		}
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


	protected void loadIntentions(String filename)
	{
		intentions = new Hashtable<String, String>();
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
						String intention = promptElement.getAttribute("intention");
						intentions.put(promptId,intention);
					}
				}
			}
			
			// ============ TEMPORARY FOR DEBUGGING =========== //
//			System.out.println("=== INTENTIONS ==="); 
//			for (Map.Entry<String,String> entry : intentions.entrySet()) {			
//				String intention = entry.getValue();
//				if (intention == null) 
//					intention = ""; 
//	            System.out.println("prompt ID:  " + entry.getKey() +
//	                             "    Intention: " + intention);
//			}
			// ============ TEMPORARY FOR DEBUGGING =========== //
		}
		
		catch (Exception e)
		{
			// log

			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_ERROR, "Unable to load intentions (" + e.toString() + ")");
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
		List<String> promptTexts = prompts.get(promptName);	 	// There may be multiple prompt options per prompt name
		if (promptTexts != null)
		{
			int promptIndex = (int) Math.floor(promptTexts.size() * Math.random());   // Select randomly if there are multiple prompt options
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
			if (includeIntention) {
				promptText = addIntention(promptName,promptText); 
			}
			return promptText;

		}
		else
		{
			return promptName;
		}
	}
	
	public String addIntention(String promptName, String promptText) {
		String intentionTag = "intention"; 
	    String multiModalDelim = ";%;";
		String withinModeDelim = ":::";	
		String withinPromptDelim = "|||";	
		
		String intention = lookupIntention(promptName);
		if (intention.length() == 0) {
			return promptText; 
		} else {
			String intentionString = multiModalDelim + intentionTag + withinModeDelim + intention; 
			if (!promptText.contains(withinPromptDelim)) {
				return promptText + intentionString; 
			} else {
				String returnText = ""; 
				String[] textParts = promptText.split(withinPromptDelim);
				String textPart; 
				for (int i = 0; i < textParts.length; i++)
				{
					textPart = textParts[i].trim();
					returnText += textPart + intentionString + withinPromptDelim; 
				}
				return returnText; 
			}					
		}
	}
	
	public String lookupIntention(String promptName)
	{
		// Do the Prompt
		String intention = intentions.get(promptName);	 	
		if (intention != null && intention.length() > 0)
		{
			return intention;
		}
		else
		{
			return "";
		}
	}	

	public String match(String promptName, String[] studentIds, String[] roles, int maxMatches, State state)
	{

		List<String> promptTexts = prompts.get(promptName);	 	// There may be multiple prompt options per prompt name
		if (promptTexts != null)
		{
			int promptIndex = (int) Math.floor(promptTexts.size() * Math.random());   // Select randomly if there are multiple prompt options
			String promptText = promptTexts.get(promptIndex);
			if (studentIds != null && roles != null)			// Replace all [NAME#] and [ROLE#] in prompt with names and roles
			{
				String nameKey, roleKey, name, role; 
				for (int i = 0; i < maxMatches; i++)
				{
					nameKey = "[NAME" + Integer.toString(i+1) + "]"; 
					name = state.getStudentName(studentIds[i]);					
					roleKey = "[ROLE" + Integer.toString(i+1) + "]"; 
					role = roles[i];
					if(name != null && role != null)
						promptText = promptText.replace(nameKey, name);
						promptText = promptText.replace(roleKey, role);
						state.setStudentRole(studentIds[i], roles[i]);
				}
			}
			if (includeIntention) {
				promptText = addIntention(promptName,promptText); 
			}
			return promptText;

		}
		else
		{
			return promptName;
		}
	}

	// Version of match with a default role
	public String match(String promptName, String[] studentIds, String[] roles, String defaultRole, int maxMatches, State state)
	{

		List<String> promptTexts = prompts.get(promptName);	 	// There may be multiple prompt options per prompt name
		if (promptTexts != null)
		{
			int promptIndex = (int) Math.floor(promptTexts.size() * Math.random());   // Select randomly if there are multiple prompt options
			String promptText = promptTexts.get(promptIndex);
			if (studentIds != null && roles != null)			// Replace all [NAME#] and [ROLE#] in prompt with names and roles
			{
				String nameKey, roleKey, name, role; 
				for (int i = 0; i < maxMatches; i++)
				{
					nameKey = "[NAME" + Integer.toString(i+1) + "]"; 
					name = state.getStudentName(studentIds[i]);					
					roleKey = "[ROLE" + Integer.toString(i+1) + "]"; 
					role = roles[i];
					if(name != null && role != null)
						promptText = promptText.replace(nameKey, name);
						promptText = promptText.replace(roleKey, role);
						state.setStudentRole(studentIds[i], roles[i]);
				}
				promptText = promptText.replace("[DEFAULTROLE]", defaultRole);
			}
			if (includeIntention) {
				promptText = addIntention(promptName,promptText); 
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

    
    public void initProperties(String pf)
    {
        properties = new Properties();
        if ((pf != null) && (pf.trim().length() != 0)) 
        {
        	File propertiesFile = new File(pf);
			if(!propertiesFile.exists())
			{
				propertiesFile = new File("properties"+File.separator+pf);
				if(!propertiesFile.exists())
					return;
			}
            try 
            {
                properties.load(new FileReader(propertiesFile));
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
                return;
            }
        }
    }

	public Properties getProperties()
	{
		return properties;
	}
}
