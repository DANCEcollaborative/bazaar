package basilica2.myagent.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import basilica2.myagent.Topic;
import basilica2.myagent.User;


public class Register implements BasilicaPreProcessor
{

	
	public Register() 
    {
    	
    	topicList = new ArrayList<Topic>();
    	userList = new ArrayList<User>();
    	lastConsolidation = 0;
		String dialogueConfigFile="dialogues/dialogues-example.xml";
    	loadconfiguration(dialogueConfigFile);

	}    
    
	public ArrayList<Topic> topicList;	
	public ArrayList<User> userList;
	public int lastConsolidation;

	private void loadconfiguration(String f)
	{
		try
		{
			DOMParser parser = new DOMParser();
			parser.parse(f);
			Document dom = parser.getDocument();
			NodeList dialogsNodes = dom.getElementsByTagName("dialogs");
			if ((dialogsNodes != null) && (dialogsNodes.getLength() != 0))
			{
				Element conceptNode = (Element) dialogsNodes.item(0);
				NodeList conceptNodes = conceptNode.getElementsByTagName("dialog");
				if ((conceptNodes != null) && (conceptNodes.getLength() != 0))
				{
					for (int i = 0; i < conceptNodes.getLength(); i++)
					{
						Element conceptElement = (Element) conceptNodes.item(i);
						String conceptName = conceptElement.getAttribute("concept");
						String conceptDetailedName = conceptElement.getAttribute("description"); 
						Topic topic = new Topic(conceptName, conceptDetailedName);
						topicList.add(topic);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}	    
	}
	
	

	public Topic IsInTopicList(String concept)
	{
		for (int i = 0; i < topicList.size(); i++)
		{
			if (topicList.get(i).name.equals(concept))
			{
				return topicList.get(i);
			}
		}
		
		return null;
	}
	
	public int IsInUserList(String id)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			//change it to id later
			if (userList.get(i).id.equals(id))
			{
				return i;
			}
		}
		
		return -1;
	}
	
	public void incrementScore(int increment)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			userList.get(i).score += increment;
		}
	}
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

		if (event instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent)event;
			String[] annotations = me.getAllAnnotations();

			
			for (String s: annotations)
		    {
				for (String a: annotations)
			    {
					Topic TopicDetected = IsInTopicList(s+"_"+a);
					if(TopicDetected != null && TopicDetected.topic_detected == null
							                 && TopicDetected.topic_prompted == null)
					{
						Date date= new Date();
						TopicDetected.topic_detected = new Timestamp(date.getTime());
						
						String prompt_message = "I noticed that you are talking about " + TopicDetected.detailed_name +
								                ". If you want to check your knowledge on " + TopicDetected.detailed_name + 
								                " with me say 'LET'S DISCUSS ABOUT " + TopicDetected.detailed_name + "' and I will ask some questions on it.";
						PromptEvent prompt = new PromptEvent(source,prompt_message,"TOPIC_ELICITATION");
						source.queueNewEvent(prompt);
						incrementScore(1);
						lastConsolidation++;
					}
					else if(s.equals("TOPIC_REQUEST"))
					{
						for (String ss: annotations)
					    {
							System.out.println(a+"_"+ss);
							Topic t = IsInTopicList(a+"_"+ss);
							if(t != null && t.topic_requested == null)
							{
								Date date= new Date();
								Timestamp currentTimestamp= new Timestamp(date.getTime());
								t.topic_requested = currentTimestamp;
							    	
								DoTutoringEvent toot = new DoTutoringEvent(source, t.name);
								source.addPreprocessedEvent(toot);
								incrementScore(2);
								break;
					
							}
					    }
					}
			    }	
	        }								    
	    }
		else if ((event instanceof DormantGroupEvent) || 
				 (event instanceof DormantStudentEvent && userList.size() == 1))
		{
			System.out.println("Nothing happened since long.");
			for (int i = 0; i < topicList.size(); i++)
			{
				Topic topic = topicList.get(i);
				if (topic.topic_detected == null && topic.topic_prompted == null)
				{
					String prompt_message = "You can discuss about " + topic.detailed_name +
			                ". If you want to check your knowledge on " + topic.detailed_name + 
			                " with me say 'LET'S DISCUSS ABOUT " + topic.detailed_name + "'";
					PromptEvent prompt = new PromptEvent(source, prompt_message , "TOPIC_PROMPTED");
					source.queueNewEvent(prompt);
					
			    	//DoTutoringEvent toot = new DoTutoringEvent(source, topic.name);
					//source.addPreprocessedEvent(toot);
					
					Date date= new Date();
					Timestamp currentTimestamp= new Timestamp(date.getTime());
					topic.topic_prompted = currentTimestamp;
					break;
				}
			}
		}
		else if (event instanceof PresenceEvent)
		{
			PresenceEvent pe = (PresenceEvent) event;
			if (!pe.getUsername().contains("Tutor") && !source.isAgentName(pe.getUsername()))
			{

				String username = pe.getUsername();
				String userid = pe.getUserId();
				if(userid == null)
					return;
				Date date= new Date();
				Timestamp currentTimestamp= new Timestamp(date.getTime());
				int userIndex = IsInUserList(userid);
				if (pe.getType().equals(PresenceEvent.PRESENT))
				{
					System.out.println("Someone present.");
					if(userIndex == -1)
					{
						String prompt_message = "Welcome, " + username + "\n";
						
						User newuser = new User(username, userid, currentTimestamp);
						userList.add(newuser);
						System.out.println("Someone joined with id = " + userid);
						
						String discussed_topics = discussedTopics();
						if(discussed_topics == null)
						{
							if(userList.size() < 2)
							{
								prompt_message = prompt_message + "I'm VirtualCarolyn. I'm here to have an interactive dialogue with you to assist you in your discussion on philosophical concepts\n";
								prompt_message = prompt_message + "You can wait for another person to join or start discussing with me a philosophical concept.";
							}
							else
							{
								prompt_message = prompt_message + "We are just starting the discussion on philosophical concepts. Can one of you start talking about a philosophical concept. ?";
							}
						}
						else
						{
							if(lastConsolidation > 1 && userList.size() > 2)
							{
								lastConsolidation = 0;
								prompt_message = prompt_message + "Can any one of you provide a summary of our discussion till now to " + username;
								
							}
							else
							{
								prompt_message = prompt_message + "We have been discussing on topics like  " + discussed_topics + "\n";
								prompt_message = prompt_message + "Please join in.";
							}
							
													
						}
						
						
						PromptEvent prompt = new PromptEvent(source, prompt_message , "INTRODUCTION");
						source.queueNewEvent(prompt);
					}
					
				}
				else if (pe.getType().equals(PresenceEvent.ABSENT))
				{
					System.out.println("Someone left");
					if(userIndex != -1)
					{
					    System.out.println("Someone left with id = " + userid);
						userList.remove(userIndex);
	     				checkOutdatedTopics();
					}
				}
			}
		}
	}
	
    public String discussedTopics()
    {
    	ArrayList<String> discussed_topics = new ArrayList<String>();
    	for (int i = 0; i < topicList.size(); i++)
		{
			Topic topic = topicList.get(i);
			if (topic.topic_detected  != null ||
			    topic.topic_discussed != null || 
			    topic.topic_requested != null 
				)
				{
					discussed_topics.add(topic.detailed_name);
				}
		}
    	
    	return  discussed_topics.size() > 0 ? StringUtils.join(discussed_topics) : null;
    }
	public void checkOutdatedTopics()
	{
		Timestamp oldestStudent = oldestStudent();
		
		if(oldestStudent == null)
		{
			for (int i = 0; i < topicList.size(); i++)
			{
				topicList.get(i).topic_detected = null;
				topicList.get(i).topic_discussed = null;
				topicList.get(i).topic_prompted = null;
				topicList.get(i).topic_requested = null;
			}
		}
		else
		{
			for (int i = 0; i < topicList.size(); i++)
			{
				Topic topic = topicList.get(i);
				if ((topic.topic_detected  == null || topic.topic_detected.before(oldestStudent))  &&
					(topic.topic_discussed == null || topic.topic_discussed.before(oldestStudent)) &&
					(topic.topic_prompted  == null || topic.topic_prompted.before(oldestStudent))  &&
					(topic.topic_requested == null || topic.topic_requested.before(oldestStudent))
				   )
				{
					topicList.get(i).topic_detected = null;
					topicList.get(i).topic_discussed = null;
					topicList.get(i).topic_prompted = null;
					topicList.get(i).topic_requested = null;					
				}
			}
		}
	}
	
	public Timestamp oldestStudent()
	{
		Timestamp minTimestamp = null;
		
		for (int i = 0; i < userList.size(); i++)
		{
			if (minTimestamp == null || userList.get(i).time_of_entry.before(minTimestamp))
			{
				minTimestamp = userList.get(i).time_of_entry;
			}
		}
		
		return minTimestamp;
	}
	
	public String oldestStudentName()
	{
		Timestamp minTimestamp = null;
		String name = "";
		
		for (int i = 0; i < userList.size(); i++)
		{
			if (minTimestamp == null || userList.get(i).time_of_entry.before(minTimestamp))
			{
				minTimestamp = userList.get(i).time_of_entry;
				name = userList.get(i).name;
			}
		}
		
		return name;
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
