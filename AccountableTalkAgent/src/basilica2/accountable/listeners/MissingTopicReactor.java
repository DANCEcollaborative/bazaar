package basilica2.accountable.listeners;


import java.util.HashMap;
import java.util.Map;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.social.events.DormantGroupEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class MissingTopicReactor extends BasilicaAdapter
{

	private static final String REMIND_PROMPT = "REMIND";
	private static final String SOURCE_NAME = "MissingTopicReactor";
	private static final int HISTORY_WINDOW = 60*90;
	private static final String NUDGE_PROMPT = "NUDGE";

	private double feedbackWindow = 45;
	private double blackoutTimeout = 15.0;
	private double priority = 0.75;
	
	
	
	PromptTable revoicePrompts;
	PromptTable nudgePrompts;
	private Map<String, String> slots = new HashMap<String, String>();
	private InputCoordinator source;
	
	public MissingTopicReactor(Agent a)
	{
		super(a, SOURCE_NAME);
		RollingWindow.sharedWindow().setWindowSize(HISTORY_WINDOW, 20);

		try
		{			
			nudgePrompts = new PromptTable(properties.getProperty("nudge_prompt_file", "plans/nudge_prompts.xml"));
			
			feedbackWindow = Double.parseDouble(properties.getProperty("feedback_window", "6000"));
			blackoutTimeout = Double.parseDouble(properties.getProperty("blackout_timeout", "10.0"));
		}
		catch(Exception e)
		{
			System.err.println("couldn't parse RevoicingActor properties file:"+e.getMessage());
		}
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		this.source = source;
		if(event instanceof DormantGroupEvent)
		{
			remind((DormantGroupEvent)event);
		}
		
	}

	
	private void remind(DormantGroupEvent event)
	{
		log(Logger.LOG_NORMAL, "MissingTopicReactor considering a dormant group event");
		
		if(RollingWindow.sharedWindow().countEvents(feedbackWindow, "feedback") == 0)
		{
			for(String k : nudgePrompts.keySet())
			{
				if(!k.equals(NUDGE_PROMPT) && !k.equals(REMIND_PROMPT))
				{
					//log(Logger.LOG_NORMAL, k + "?");
					if(RollingWindow.sharedWindow().countEvents(HISTORY_WINDOW, k) < 2 //not been mentioned much ever
							&& getFeedbackCount(k) == 0) //never nudged for this before
					{
						log(Logger.LOG_NORMAL, k + " is a missing topic");
						makeReminderProposal(k);
					}
				}
			}
		}
		
	}
	
	private void makeReminderProposal(String concept)
	{
		makeFeedbackProposal("GROUP", concept, nudgePrompts, REMIND_PROMPT, priority);
	}

	private void makeFeedbackProposal(final String student, final String concept, PromptTable prompts, String prompt, double priority)
	{
		String conceptForm = prompts.lookup(concept);
		slots.put("[STUDENT]", student);
		slots.put("[CONCEPT]", conceptForm);
		
		final MessageEvent me = new MessageEvent(source, this.getAgent().getUsername(), prompts.lookup(prompt, slots), prompt, student);

		final BlacklistSource blacklistSource = new BlacklistSource(SOURCE_NAME, "");
		PriorityEvent pe = new PriorityEvent(source, me, 1.0, blacklistSource, 4.0);
		
//		blacklistSource.addExceptions(FeedbackActor.SOURCE_NAME);
//		PriorityEvent pe = PriorityEvent.makeSelfBlockingSourceEvent(SOURCE_NAME, 
//				me, 
//				1.0,  /*priority*/
//				5.0,  /*relevance window*/ 
//				FEEDBACK_WINDOW /*blocking window*/);
		
		source.addProposal(pe);
		
		pe.addCallback(new Callback()
		{

			@Override
			public void accepted(PriorityEvent p)
			{
				recordFeedback(me, student, concept);
				blacklistSource.setTimeout(feedbackWindow);
				
				new Timer(blackoutTimeout, new TimeoutReceiver()
				{
					public void timedOut(String id)
					{
						blacklistSource.getBlacklist().remove("");
					}
					public void log(String from, String level, String msg){}
				}).start();
			}

			@Override
			public void rejected(PriorityEvent p)
			{}});
	}

	private int getFeedbackCount(String key)
	{
		return RollingWindow.sharedWindow().countEvents(HISTORY_WINDOW, key, "feedback");
	}

	private void recordFeedback(MessageEvent me, String student, String feedbackType)
	{
		RollingWindow.sharedWindow().addEvent(me, student, feedbackType, "feedback");
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[]{DormantGroupEvent.class};
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{ 
		return new Class[]{};
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		// TODO Auto-generated method stub
		
	}

	
	
}
