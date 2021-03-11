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

public class Register implements BasilicaPreProcessor, TimeoutReceiver
{
	
    public void startTimer()
    {
    	timer = new Timer(3, this);
    	timer.start();
    }
    
 	

	public Register() 
    {
    	
    	topicList = new ArrayList<Topic>();
    	userList = new ArrayList<User>();
    	lastConsolidation = 0;
    	reasoning = false;
    	
    	perspective_map = new HashMap<Integer, String>();
    	perspective_map.put(0,"what would be the most economical");
    	perspective_map.put(1,"what would be most environmentally friendly and with lowest startup costs");
    	perspective_map.put(2,"carbon neutrality and what is best economically in the long run");
    	perspective_map.put(3,"environmental friendliness and reliability");

    	plan_map = new ArrayList<Map<String, Integer>>();
    	Map<String, Integer> temp1 = new HashMap<String, Integer>();
    	temp1.put("reasoning",0);
    	temp1.put("non_reasoning",0);
    	
    	Map<String, Integer> temp2 = new HashMap<String, Integer>();
    	temp2.put("reasoning",0);
    	temp2.put("non_reasoning",0);
    	
    	Map<String, Integer> temp3 = new HashMap<String, Integer>();
    	temp3.put("reasoning",0);
    	temp3.put("non_reasoning",0);
    	
    	Map<String, Integer> temp4 = new HashMap<String, Integer>();
    	temp4.put("reasoning",0);
    	temp4.put("non_reasoning",0);
    	
    	plan_map.add(temp1);
    	plan_map.add(temp2);
    	plan_map.add(temp3);
    	plan_map.add(temp4);
    	
    	planList = new ArrayList<String>();
    	planList.add("PLAN1");
    	planList.add("PLAN2");
    	planList.add("PLAN3");
    	planList.add("PLAN4");
    	  	
    	lightSidePrompts = new PromptTable(lightSidePromptsPath);
    	
		String dialogueConfigFile="dialogues/dialogues-example.xml";
    	loadconfiguration(dialogueConfigFile);
    	startTimer();
	}    
    
	public ArrayList<Topic> topicList;	
	public ArrayList<User> userList;
	public ArrayList<String> planList;
	public int lastConsolidation;
    public boolean reasoning;
    public String reasoning_from;
    public String reasoning_type;
    public static Timer timer;
    public InputCoordinator src;
    public Map<Integer, String> perspective_map;
    public ArrayList<Map<String, Integer>> plan_map;
    public int dormantGroupCount=0;
    public int reasoningPromptCount=0;
    public int reasoningchoicePromptCount=0;
   
    public int evaluatechoicePromptCount=0;
    public int evaluateplanPromptCount=0;
    
    public int[] planflag = {0,0,0,0};
    public int reasoningflag= 0;
    public int noreasoningflag=0;
         
    public int bazaarstate=1;//bazaarstate= 1, prompt, bazaarstate=0, not prompt, after 10 minutes.
    public int totalseconds= 600;// countdown of 10 minutes;
   
    String lightSidePromptsPath="dialogues/lightside-prompts.xml";
	private PromptTable lightSidePrompts;
    
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
		src = source;
		if(bazaarstate==1){
		if (event instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent)event;
			String[] annotations = me.getAllAnnotations();
			
			User user = getUser(me.getFrom());
			if(user == null) return;

			Boolean promptFound = false; 
			String promptRaw = ""; 
			if (me.hasAnnotations("DETECTED")) {
				promptRaw=lightSidePrompts.lookup("DETECTED");
				if (promptRaw != "DETECTED") {
					promptFound = true; 
				}
				else {
					System.err.println("Prompt for DETECTED not found");
				}
					
			}
			else if (me.hasAnnotations("NOTDETECTED")) {
				promptRaw=lightSidePrompts.lookup("NOTDETECTED");
				if (promptRaw != "NOTDETECTED") {
					promptFound = true; 
				}
				else {
					System.err.println("Prompt for _NOT_DETECTED not found");
				}
			}	
			if (promptFound) {

				
				int plan = 0; 
				if (me.hasAnnotations("PLAN1"))
				{
					plan = 1;
				}
				else if (me.hasAnnotations("PLAN2"))
				{
					plan = 2;
				}
				else if (me.hasAnnotations("PLAN3"))
				{
					plan = 3;
				}						
				else if (me.hasAnnotations("PLAN4"))
				{
					plan = 4;
				}

				Map<String, String> lightSidePromptVariables = new HashMap<String, String>();
				lightSidePromptVariables.put("%this_student%",me.getFrom());
				lightSidePromptVariables.put("%this_student_plan%",String.valueOf(plan));
				String prompt_message = promptRaw;
				for (Map.Entry<String, String> entry : lightSidePromptVariables.entrySet())
				    prompt_message = prompt_message.replace(entry.getKey(), entry.getValue().toString());
				
				System.err.println("=== prompt_message: " + prompt_message); 
				PromptEvent prompt = new PromptEvent(source,prompt_message,"plan_reasoning");
				
				
				source.queueNewEvent(prompt);
			}
			
			if(me.hasAnnotations("pos"))
			{
				if(userList.size() > 1)
				{		
					int plan = 0;
					int count = 0;
						
					if (me.hasAnnotations("PLAN1"))
					{
						plan = 1;
						count++;
					}
					if (me.hasAnnotations("PLAN2"))
					{
						plan = 2;
						count++;
					}
					if (me.hasAnnotations("PLAN3"))
					{
						plan = 3;
						count++;
					}						
					if (me.hasAnnotations("PLAN4"))
					{
						plan = 4;
						count++;
					}
					
					if (plan == 0 && user.reasoning)
					{
						if (user.reasoning_type.contains("PLAN1"))
						{
							plan = 1;
							count++;
						}
						if (user.reasoning_type.contains("PLAN2"))
						{
							plan = 2;
							count++;
						}
						if (user.reasoning_type.contains("PLAN3"))
						{
							plan = 3;
							count++;
						}						
						if (user.reasoning_type.contains("PLAN4"))
						{
							plan = 4;
							count++;
						}
					}
					
					if (plan!=0  && !me.hasAnnotations("NEGATIVE"))
					{
							if(count==1){		
								if (planflag[plan-1]==0 & reasoningflag==0){

									user.plan=plan;
									planflag[plan-1]=1;
									reasoningflag=0;
									User userwithplan = choose_user_with_plan(me.getFrom(), plan);
									
									if(userwithplan!=null){
										String prompt_message_="";
										prompt_message_="Hey " + userwithplan.name+", you have proposed plan "+userwithplan.plan+", and "+me.getFrom()+ " has proposed plan "+plan+". Can you"
												+ " compare the two plans from your perpsective of "+ perspective_map.get(userwithplan.perspective) + " ?";
									
										PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
										source.queueNewEvent(prompt);
                                    planflag[plan-1]=2;
                                    planflag[userwithplan.plan-1]=2;
                                    reasoningflag=1;
                                    userwithplan.promptflag=true;
									}
								}
								
								else if (planflag[plan-1]==0 & reasoningflag==1){
                                   
                                    User selected_user = choose_random_user(me.getFrom(), plan);

								String prompt_message_="";
									if(selected_user!=null){
								switch(evaluateplanPromptCount%4){
								case 0: 
									 prompt_message_ = "Hey " + selected_user.name + ", Can you evaluate " + me.getFrom() + "'s plan from your perspective of " + 
											perspective_map.get(selected_user.perspective) + " ?";
									break;
								case 1: 
									 prompt_message_ = "Hey " + selected_user.name+ ", what do you think are the pros and cons of "+me.getFrom()+"'s plan from your perspective of "+
											perspective_map.get(selected_user.perspective)+ " ?";
									break;
								case 2:
									 prompt_message_ = selected_user.name+", how do you like "+me.getFrom()+"'s plan from your perspective of "+
											perspective_map.get(selected_user.perspective)+ " ?";
								    break;
								case 3:
									 prompt_message_ = selected_user.name+", will you recommend "+ me.getFrom()+"'s plan from your perspective of "+
										    perspective_map.get(selected_user.perspective)+ " ?";
									 break;
								}
								evaluateplanPromptCount++;						
								PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
								source.queueNewEvent(prompt);

								 selected_user.promptflag = true;
								 user.plan= plan;
								 planflag[plan-1]=2;
								 reasoningflag =0;
								}
								}
							
								else if(planflag[plan-1]==1){
									user.plan=plan;

								User userwithplan = choose_user_with_plan(me.getFrom(), plan);
								if(userwithplan!=null){
									String prompt_message_="";
									prompt_message_="Hey " + userwithplan.name+", you have proposed plan "+userwithplan.plan+", and "+me.getFrom()+ " has proposed plan "+plan+". Can you"
											+ " compare the two plans from your perpsective of "+ perspective_map.get(userwithplan.perspective) + " ?";
								
									PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
									source.queueNewEvent(prompt);
                                planflag[plan-1]=2;
                                planflag[userwithplan.plan-1]=2;
                                reasoningflag=1;
                                userwithplan.promptflag=true;
								}
							}
							
							}
						
					}
					
				}		
					
				if(user.reasoning) 
				{
					user.reasoning = false;
					user.wait_duration = 0;
				}
			}
			else if(me.hasAnnotations("PLAN"))
			{
				if(user.reasoning)
				{

						int plan = 0;
						int count = 0;
						
						if (me.hasAnnotations("PLAN1"))
						{
							plan = 1;
							count++;
						}
						if (me.hasAnnotations("PLAN2"))
						{
							plan = 2;
							count++;
						}
						if (me.hasAnnotations("PLAN3"))
						{
							plan = 3;
							count++;
						}						
						if (me.hasAnnotations("PLAN4"))
						{
							plan = 4;
							count++;
						}

						
						if (plan!=0 && !me.hasAnnotations("NEGATIVE"))
						{
							if(count==1)
							{					
							if(planflag[plan-1]==0 && noreasoningflag==0){
								planflag[plan-1]=1;
							    user.plan= plan;
							    
								if(user.promptflag==false){
									String prompt_message_="";

								switch(reasoningPromptCount%3){
								case 0:
									prompt_message_ = "Hey " + me.getFrom() + ", can you elaborate on the reason you chose plan " + Integer.toString(plan) + " from your perspective of " + 
											perspective_map.get(user.perspective) + " ?";
									break;
								case 1:
									prompt_message_ = me.getFrom() + ", can you be more specific about why you chose plan " + Integer.toString(plan) + " from your perspective of " + 
											perspective_map.get(user.perspective) + " ?";
									break;			
								case 2: 
									prompt_message_ = "Hey "+ me.getFrom() + ", what do you think are the pros and cons of plan " + Integer.toString(plan) + " from your perspective of " + 
											perspective_map.get(user.perspective) + " ?";
									break;		
								}
								reasoningPromptCount++;
								PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
								source.queueNewEvent(prompt);						
							    noreasoningflag=1;
							    user.promptflag=true;
								}
								
							}
							
							else if (planflag[plan-1]==0 && noreasoningflag==1){
								user.plan=plan;
								planflag[plan-1]=1;
								User userwithplan = choose_user_with_plan_noreasoning(me.getFrom(), plan);
								if(user.promptflag==false&&userwithplan!=null){
									String prompt_message_="";
									prompt_message_="Hey " + me.getFrom()+", you have proposed plan "+plan+", and "+userwithplan.name+ " has proposed plan "+userwithplan.plan+". What do"
											+ " you think are the most important trade-offs between the two plans from your perspective of "+ perspective_map.get(user.perspective) + " ?";
								
									PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
									source.queueNewEvent(prompt);
                                planflag[plan-1]=2;
                                planflag[userwithplan.plan-1]=2;
                                noreasoningflag=0;
                                user.promptflag=true;
								}
							}
							
							else if (planflag[plan-1]==1){
								user.plan=plan;

								User userwithplan = choose_user_with_plan_noreasoning(me.getFrom(), plan);
								if(user.promptflag==false && userwithplan!=null){
									String prompt_message_="";
									prompt_message_="Hey " + me.getFrom()+", you have proposed plan "+plan+", and "+userwithplan.name+ " has proposed plan "+userwithplan.plan+". What do"
											+ " you think are the most important trade-offs between the two plans from your perspective of "+ perspective_map.get(user.perspective) + " ?";
								
									PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
									source.queueNewEvent(prompt);
                                planflag[plan-1]=2;
                                planflag[userwithplan.plan-1]=2;
                                noreasoningflag=0;
                                user.promptflag=true;
								}
								
							}	
							}
						}
						
					user.reasoning = false;
					user.wait_duration = 0;
				}
				else
				{
					if(!me.hasAnnotations("NEGATIVE")){
					user.reasoning = true;

					user.reasoning_type = "";
					if (me.hasAnnotations("PLAN1"))
					{
						user.reasoning_type += "PLAN1";
					}
					if (me.hasAnnotations("PLAN2"))
					{
						user.reasoning_type += "PLAN2";
					}
					if (me.hasAnnotations("PLAN3"))
					{
						user.reasoning_type += "PLAN3";
					}						
					if (me.hasAnnotations("PLAN4"))
					{
						user.reasoning_type += "PLAN4";
					}
					
					user.wait_duration = 10;
					}
				}
			}
			else if(user.reasoning)
			{
				

					int plan = 0;
					int count = 0;

						if (user.reasoning_type.contains("PLAN1"))
						{
							plan = 1;
							count++;
						}
						if (user.reasoning_type.contains("PLAN2"))
						{
							plan = 2;
							count++;
						}
						if (user.reasoning_type.contains("PLAN3"))
						{
							plan = 3;
							count++;
						}						
						if (user.reasoning_type.contains("PLAN4"))
						{
							plan = 4;
							count++;
						}
					
					if (plan!=0 && !me.hasAnnotations("NEGATIVE"))
					{
						if(count==1)
						{
							if(planflag[plan-1]==0 && noreasoningflag==0){
								 planflag[plan-1]=1;
								    user.plan= plan;
								    
								if(user.promptflag==false){
									String prompt_message_="";

								switch(reasoningPromptCount%3){
								case 0:
									prompt_message_ = "Hey " + me.getFrom() + ", can you elaborate on the reason you chose plan " + Integer.toString(plan) + " from your perspective of " + 
											perspective_map.get(user.perspective) + " ?";
									break;
								case 1:
									prompt_message_ = me.getFrom() + ", can you be more specific about why you chose plan " + Integer.toString(plan) + " from your perspective of " + 
											perspective_map.get(user.perspective) + " ?";
									break;			
								case 2: 
									prompt_message_ = "Hey "+ me.getFrom() + ", what do you think are the pros and cons of plan " + Integer.toString(plan) + " from your perspective of " + 
											perspective_map.get(user.perspective) + " ?";
									break;		
								}
								noreasoningflag=1;
								reasoningPromptCount++;
								PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
								source.queueNewEvent(prompt);						   
							    user.promptflag=true;
								}
								
							}
							else if (planflag[plan-1]==0 && noreasoningflag==1){
								user.plan=plan;
								planflag[plan-1]=1;
								User userwithplan = choose_user_with_plan_noreasoning(me.getFrom(), plan);
								if(user.promptflag==false&&userwithplan!=null){
									String prompt_message_="";
									prompt_message_="Hey " + me.getFrom()+", you have proposed plan "+plan+", and "+userwithplan.name+ " has proposed plan "+userwithplan.plan+". What do"
											+ " you think are the most important trade-offs between the two plans from your perspective of "+ perspective_map.get(user.perspective) + " ?";
								
									PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
									source.queueNewEvent(prompt);
                                planflag[plan-1]=2;
                                planflag[userwithplan.plan-1]=2;
                                noreasoningflag=0;
                                user.promptflag=true;
								}
							}
							
							else if (planflag[plan-1]==1){
								user.plan=plan;

								User userwithplan = choose_user_with_plan_noreasoning(me.getFrom(), plan);
								if(user.promptflag==false && userwithplan!=null){
									String prompt_message_="";
									prompt_message_="Hey " + me.getFrom()+", you have proposed plan "+plan+", and "+userwithplan.name+ " has proposed plan "+userwithplan.plan+". What do"
											+ " you think are the most important trade-offs between the two plans from your perspective of "+ perspective_map.get(user.perspective) + " ?";
								
									PromptEvent prompt = new PromptEvent(source,prompt_message_,"plan_reasoning");
									source.queueNewEvent(prompt);
                                planflag[plan-1]=2;
                                planflag[userwithplan.plan-1]=2;
                                noreasoningflag=0;
                                user.promptflag=true;
								}
								
							}		
							
							
			
						}
					}
	


				user.reasoning = false;
				user.wait_duration = 0;
			}
			
			
	    }
		else if (event instanceof DormantGroupEvent)
		{
			
			User selected_user_dormant = choose_user_dormant();
			if(selected_user_dormant!=null){
			
            String prompt_message;
		//	String prompt_message = "It looks like no one is using the chat. Use this space to discuss and come to a consensus about which plan you prefer while writing the proposal.";
			if(dormantGroupCount%2==0)
			{
			 prompt_message = "Hey " + selected_user_dormant.name + ", which of the plans seems to be the best from your perspective of " + 
					perspective_map.get(selected_user_dormant.perspective) + " ?";
			dormantGroupCount++;
			}
			else{
			 prompt_message = "Hey " + selected_user_dormant.name + ", which plan do you recommend from your perspective of "+
					perspective_map.get(selected_user_dormant.perspective) + " ?";
			dormantGroupCount++;
			}
			PromptEvent prompt = new PromptEvent(source, prompt_message , "POKING");
			source.queueNewEvent(prompt);
			}			
		}
		else if (event instanceof PresenceEvent)
		{
			PresenceEvent pe = (PresenceEvent) event;

			if (!pe.getUsername().contains("Agent") && !source.isAgentName(pe.getUsername()))
			{

				String username = pe.getUsername();
				String userid = pe.getUserId();

				// TEMPORARILY OVERRIDE userperspective since it's getting a null value
				// int userperspective = Integer.parseInt(pe.getUserPerspective());
				int userperspective = 1;
				
				if(userid == null)
					return;
				Date date= new Date();
				Timestamp currentTimestamp= new Timestamp(date.getTime());
				int userIndex = IsInUserList(userid);
				if (pe.getType().equals(PresenceEvent.PRESENT))
				{
					System.out.println("Someone present");
					if(userIndex == -1)
					{
						String prompt_message = "Welcome, " + username + "\n";
						
						User newuser = new User(username, userid, currentTimestamp, userperspective);
						userList.add(newuser);
						System.out.println("Someone joined with id = " + userid);
						
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
	
	public User getUser(String name)
	{
		for (int i = 0; i < userList.size(); i++)
		{
			System.out.println(userList.get(i).name);;
			if (userList.get(i).name.equals(name))
			{
				return userList.get(i);
			}
		}
		
		return null;
		
	}
	
	public User choose_user_with_plan(String name, int plan){
		for(int i =0; i< userList.size(); i++){
			User usertemp= userList.get(i);
			if (usertemp.plan!=0){
			if(!usertemp.name.equals(name)&&usertemp.plan!=plan && planflag[usertemp.plan-1]==1 && !usertemp.promptflag){
				return usertemp;
			}
			}
		}
	return null;
	}
	
	public User choose_user_with_plan_noreasoning(String name, int plan){
		for(int i =0; i< userList.size(); i++){
			User usertemp= userList.get(i);
			if (usertemp.plan!=0){
			if(!usertemp.name.equals(name)&&usertemp.plan!=plan && planflag[usertemp.plan-1]==1){
				return usertemp;
			}
			}
		}
	return null;
	}
	
	public User choose_user_dormant(){
		for(int i=0; i< userList.size(); i++){
			User userdormant = userList.get(i);
			if(!userdormant.promptflag){
				return userdormant;
			}
		}
		return null;
	}
	public User choose_random_user(String name, int plan )
	{
		int index = (int) (Math.random() * (userList.size() - .1));
		boolean found = false;
		int tries = 0;
		while((!found) && tries < 10)
		{
			index = (int) (Math.random() * (userList.size() - .1));
			User user = userList.get(index);
			if(!userList.get(index).name.equals(name) && !user.user_flag.containsKey(name) && !user.promptflag)
			{
				
				found = true;
				user.user_flag.put(name, 1);
				return userList.get(index);
			}
			tries++;
		}
		return null;
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


	@Override
	public void log(String arg0, String arg1, String arg2) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void timedOut(String arg0) {
		for(int i =0; i< 4; i++){
			System.out.println(planflag[i]);
		}
		
		totalseconds-=3;
		if (totalseconds==0){
		bazaarstate=0;
		}
		// TODO Auto-generated method stub
		
		for (int i = 0; i < userList.size(); i++)
		{
			if (userList.get(i).wait_duration == 0)
			{
				User user = userList.get(i);
				if(user.reasoning)
				{
						int plan = 0;
						int count = 0;
						if (plan == 0 && user.reasoning)
						{
							if (user.reasoning_type.contains("PLAN1"))
							{
								plan = 1;
								count++;
							}
							if (user.reasoning_type.contains("PLAN2"))
							{
								plan = 2;
								count++;
							}
							if (user.reasoning_type.contains("PLAN3"))
							{
								plan = 3;
								count++;
							}						
							if (user.reasoning_type.contains("PLAN4"))
							{
								plan = 4;
								count++;
							}
						}
						
						if (plan!=0 )
						{
							if(count==1)
							{
								if(planflag[plan-1]==0 && user.promptflag==false && noreasoningflag==0){
									
								String prompt_message_="";
							switch(reasoningPromptCount%3){
							case 0:
								prompt_message_ = "Hey " + user.name + ", can you elaborate on the reason you chose plan " + Integer.toString(plan) + " from your perspective of " + 
										perspective_map.get(user.perspective) + " ?";
								break;
							case 1:
								prompt_message_ = user.name + ", can you be more specific about why you chose plan " + Integer.toString(plan) + " from your perspective of " + 
										perspective_map.get(user.perspective) + " ?";
								break;
								
							case 2: 
								prompt_message_ = "Hey "+ user.name + ", what do you think are the pros and cons of plan " + Integer.toString(plan) + " from your perspective of " + 
										perspective_map.get(user.perspective) + " ?";
								break;
								
							}
							    reasoningPromptCount++;
								PromptEvent prompt = new PromptEvent(src,prompt_message_,"plan_reasoning");
								src.queueNewEvent(prompt);
								planflag[plan-1]=1;
								user.promptflag=true;
							    user.plan= plan;
								noreasoningflag=1;
								}
								
						
								else if (planflag[plan-1]==0 && noreasoningflag==1){
									user.plan=plan;
									planflag[plan-1]=1;
									User userwithplan = choose_user_with_plan_noreasoning(user.name, plan);
									if(user.promptflag==false&&userwithplan!=null){
										String prompt_message_="";
										prompt_message_="Hey " + user.name+", you have proposed plan "+plan+", and "+userwithplan.name+ " has proposed plan "+userwithplan.plan+". What do"
												+ " you think are the most important trade-offs between the two plans from your perspective of "+ perspective_map.get(user.perspective) + " ?";
									
										PromptEvent prompt = new PromptEvent(src,prompt_message_,"plan_reasoning");
										src.queueNewEvent(prompt);
	                                planflag[plan-1]=2;
	                                planflag[userwithplan.plan-1]=2;
	                                noreasoningflag=0;
	                                user.promptflag=true;
									}
								}
								
								else if (planflag[plan-1]==1){
									user.plan=plan;

									User userwithplan = choose_user_with_plan_noreasoning(user.name, plan);
									if(user.promptflag==false && userwithplan!=null){
										String prompt_message_="";
										prompt_message_="Hey " + user.name+", you have proposed plan "+plan+", and "+userwithplan.name+ " has proposed plan "+userwithplan.plan+". What do"
												+ " you think are the most important trade-offs between the two plans from your perspective of "+ perspective_map.get(user.perspective) + " ?";
									
										PromptEvent prompt = new PromptEvent(src,prompt_message_,"plan_reasoning");
										src.queueNewEvent(prompt);
	                                planflag[plan-1]=2;
	                                planflag[userwithplan.plan-1]=2;
	                                noreasoningflag=0;
	                                user.promptflag=true;
									}
									
								}		
							}
						}
				
					user.reasoning = false;
					user.wait_duration = 0;
				}
			}
			else
			{
				userList.get(i).wait_duration = userList.get(i).wait_duration - 5;
			}
		}
		
		startTimer();
		
	}
}
