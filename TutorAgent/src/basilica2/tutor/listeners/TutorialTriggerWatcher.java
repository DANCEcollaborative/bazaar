package basilica2.tutor.listeners;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.tutor.events.DoTutoringEvent;
import basilica2.tutor.listeners.TutorActor.Dialog;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;

public class TutorialTriggerWatcher extends BasilicaAdapter
{
	Map<String[], String> dialogueTriggers = new HashMap<String[], String>();
	String dialogueConfigFile="dialogues/dialogues-example.xml";
	private String tutorialCondition = "tutorial";
	public TutorialTriggerWatcher(Agent a)
	{
		super(a);
		dialogueConfigFile = properties.getProperty("dialogue_config_file",dialogueConfigFile);
		tutorialCondition = properties.getProperty("tutorial_condition",tutorialCondition );
		loadDialogConfiguration(dialogueConfigFile);
	}
	
	private void loadDialogConfiguration(String f)
	{
		try
		{
			DOMParser parser = new DOMParser();
			parser.parse(f);
			Document dom = parser.getDocument();
			NodeList dialogsNodes = dom.getElementsByTagName("dialogs");
			if ((dialogsNodes != null) && (dialogsNodes.getLength() != 0))
			{
				Element dialogsNode = (Element) dialogsNodes.item(0);
				NodeList dialogNodes = dialogsNode.getElementsByTagName("dialog");
				if ((dialogNodes != null) && (dialogNodes.getLength() != 0))
				{
					for (int i = 0; i < dialogNodes.getLength(); i++)
					{
						Element dialogElement = (Element) dialogNodes.item(i);
						String conceptName = dialogElement.getAttribute("concept");
//						String filename = dialogElement.getAttribute("scenario");
						
						NodeList triggerNodes = dialogElement.getElementsByTagName("trigger");
						if ((triggerNodes != null) && (triggerNodes.getLength() != 0))
						{
							int length = triggerNodes.getLength();
							String[] triggers = new String[length];
							for(int j = 0; j < length; j++)
							{
								Element triggerMeElmo = (Element) triggerNodes.item(j);
								String trigger = triggerMeElmo.getAttribute("annotation");
								triggers[j] = trigger;
							}
							
							dialogueTriggers.put(triggers, conceptName);
							
						}
					}
				}
				


			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	

	
	@Override
	public void processEvent(InputCoordinator source, Event event)
	{}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}

	@Override
	public Class[] getListenerEventClasses()
	{ return new Class[]{}; }

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		MessageEvent me = (MessageEvent)event;

		if(!System.getProperty("basilica2.agents.condition").contains(tutorialCondition))
		{
			return;
		}
		for(String[] trigger : dialogueTriggers.keySet())
		{
			
			if(me.hasAnyAnnotations(trigger))
			{
			    // gst edit	
				DoTutoringEvent toot = new DoTutoringEvent(source, dialogueTriggers.get(trigger), me);
				source.addPreprocessedEvent(toot);
			}
		}
		
	}

}
