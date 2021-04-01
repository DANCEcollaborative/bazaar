package basilica2.myagent.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.listeners.MessageAnnotator;
import basilica2.social.events.DormantGroupEvent;
import basilica2.social.events.DormantStudentEvent;
import basilica2.socketchat.WebsocketChatClient;
import basilica2.tutor.events.DoTutoringEvent;
import basilica2.agents.data.PromptTable;

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import basilica2.myagent.Topic;
import basilica2.myagent.User;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class Register implements BasilicaPreProcessor
{

	public Register() {}

	/**
	 * @param source the InputCoordinator - to push new events to. (Modified events don't need to be re-pushed).
	 * @param event an incoming event which matches one of this preprocessor's advertised classes (see getPreprocessorEventClasses)
	 * 
	 * Preprocess an incoming event, by modifying this event or creating a new event in response. 
	 * All original and new events will be passed by the InputCoordinator to the second-stage Reactors ("BasilicaListener" instances).
	 */
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		String annotations = ""; 
		if (event instanceof MessageEvent)
		{
			String prompt_message = ""; 
			MessageEvent me = (MessageEvent)event;
			annotations = me.getAnnotationString();
			System.err.println("In MLAgent, Register.java preProcessEvent, annotations: " + annotations); 
			
			// User user = getUser(me.getFrom());

			Boolean promptFound = false; 
			if (annotations != "") {
				prompt_message = annotations;
				System.err.println("=== prompt_message: " + prompt_message); 
				PromptEvent prompt = new PromptEvent(source,prompt_message,"");
				source.queueNewEvent(prompt);
			}			
	    }
	}
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		//only MessageEvents will be delivered to this watcher.
		return new Class[]{MessageEvent.class, DormantGroupEvent.class, PresenceEvent.class, DormantStudentEvent.class};
	}

}
