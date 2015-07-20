package basilica2.accountable.listeners;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import dadamson.words.SynonymSentenceMatcher;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class RevoicingActor extends BasilicaAdapter
{

	private static final String REVOICE_PROMPT = "REVOICE";
	private static final String NUDGE_PROMPT = "NUDGE";
	private static final String SOURCE_NAME = "AccountableTalk";
	private static final int HISTORY_WINDOW = 60*90;

	private double feedbackWindow = 30;
	private double minimumMatch = 0.75; 
	private double blackoutTimeout = 15.0;
	private double revoicePriority = 1.0;
	private double nudgePriority = 0.5;
	
	private boolean nudging = false;
	
	InputCoordinator source;
	HashMap<String, Integer> studentScores = new HashMap<String, Integer>();
	int teamScore = 0;
	
	String status = "";
	
	PromptTable revoicePrompts;
	PromptTable nudgePrompts;
	private ArrayList<String> candidates = new ArrayList<String>();
	private SynonymSentenceMatcher sentenceMatcher;
	private Map<String, String> slots = new HashMap<String, String>();
	private boolean doRevoice = true;
	
	public RevoicingActor(Agent a)
	{
		super(a, SOURCE_NAME);
		RollingWindow.sharedWindow().setWindowSize(HISTORY_WINDOW, 20);
		String condition = System.getProperty("basilica2.agents.condition", "feedback revoice agree remind social cooperate");
		doRevoice = condition.contains("revoice");
		log(Logger.LOG_WARNING, "Revoice Condition: "+doRevoice);

		try
		{
			String expertPath = getProperties().getProperty("expert_statement_file", "expert_statements.txt");
			String dictionaryPath = getProperties().getProperty("synonym_file", "synonyms.txt");
			String contentDictionaryPath = getProperties().getProperty("content_synonym_file", "content_synonyms.txt");
			String stopwordsPath = getProperties().getProperty("stopwords_file", "stopwords.txt");
			loadExpertStatements(expertPath);
			
			revoicePrompts = new PromptTable(properties.getProperty("revoice_prompt_file", "revoicing_prompts.xml"));
			nudgePrompts = new PromptTable(properties.getProperty("nudge_prompt_file", "nudge_prompts.xml"));
			sentenceMatcher = new SynonymSentenceMatcher(contentDictionaryPath,stopwordsPath);
			sentenceMatcher.addDictionary(dictionaryPath, false);
			sentenceMatcher.enableWordNet(true);
			
			minimumMatch = Double.parseDouble(properties.getProperty("minimum_match", "0.75"));
			feedbackWindow = Double.parseDouble(properties.getProperty("feedback_window", "6000"));
			blackoutTimeout = Double.parseDouble(properties.getProperty("blackout_timeout", "10.0"));
			revoicePriority = Double.parseDouble(properties.getProperty("revoice_priority", "1.0"));
			nudgePriority = Double.parseDouble(properties.getProperty("nudge_priority", "0.5"));
			nudging = Boolean.parseBoolean(properties.getProperty("do_nudge", "false"));
		}
		catch(Exception e)
		{
			System.err.println("couldn't parse RevoicingActor properties file:"+e.getMessage());
		}
	}

	private void loadExpertStatements(String expertPath)
	{
		File expertFile = new File(expertPath);
		try
		{
			Scanner s = new Scanner(expertFile);
			while(s.hasNextLine())
			{
				candidates.add(s.nextLine());
			}
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		this.source = source;
		if(event instanceof MessageEvent && doRevoice)
		{
			revoice((MessageEvent)event);
		}
		
	}

	public String simRevoice(String text, int verbosity)
	{
		sentenceMatcher.setVerbosity(verbosity);
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		if(match == null)
			return "?";
		String conceptForm = revoicePrompts.lookup(match);
		slots.put("[STUDENT]", "David");
		slots.put("[CONCEPT]", conceptForm);
		return revoicePrompts.lookup(REVOICE_PROMPT, slots);
	}
	
	private String testRevoice(String text)
	{
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		return match;
	}
	
	private void revoice(MessageEvent event)
	{
		final String student = event.getFrom();
		
		String text = event.getText();
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		if(match != null)
		{
			if(getFeedbackCount(match) < 1 && RollingWindow.sharedWindow().countEvents(feedbackWindow, REVOICE_PROMPT) < 1)
			{
				double sim = sentenceMatcher.getSentenceSimilarity(text, match);
				makeRevoiceProposal(student, match, event, sim);
			}
			log(Logger.LOG_NORMAL, "REVOICE: '"+text+"'+ = '"+match+"'");
		}	
		else if(nudging)
		{
			for(String k : nudgePrompts.keySet())
			{
				if(event.hasAnnotations(k) && !event.hasAnnotations("EXPLANATION_CONTRIBUTION")
						&& RollingWindow.sharedWindow().countEvents(feedbackWindow, k) < 2 //not been mentioned recently
						&& RollingWindow.sharedWindow().countEvents(HISTORY_WINDOW, k) < 5 //not been mentioned much ever
						&& getFeedbackCount(k) < 1) //never nudged for this before
				{
					makeNudgeProposal(student, k, event);
				}
			}
		}
	}
	
	private void makeNudgeProposal(String student, String concept, MessageEvent reference)
	{
		makeFeedbackProposal(student, concept, reference, nudgePrompts, NUDGE_PROMPT, nudgePriority);
	}

	private void makeRevoiceProposal(final String student, final String concept, MessageEvent reference, double sim)
	{
		makeFeedbackProposal(student, concept, reference, revoicePrompts, REVOICE_PROMPT, sim*revoicePriority);
	}

	private void makeFeedbackProposal(final String student, final String concept, final MessageEvent reference, PromptTable prompts, final String prompt, double priority)
	{
		String conceptForm = prompts.lookup(concept);
		slots.put("[STUDENT]", student);
		slots.put("[CONCEPT]", conceptForm);
		
		int revoiceIndex = candidates.indexOf(concept);
		final MessageEvent me = new MessageEvent(source, this.getAgent().getUsername(), prompts.lookup(prompt, slots), prompt, student, "RV"+revoiceIndex, "AT_MOVE");
		me.setReference(reference);
		
		final BlacklistSource blacklistSource = new BlacklistSource(SOURCE_NAME, SOURCE_NAME, "");
		PriorityEvent pe = new PriorityEvent(source, me, priority, blacklistSource, 4.0);
		pe.setSource(SOURCE_NAME);
		
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
				recordFeedback(me, student, prompt);
				blacklistSource.setTimeout(feedbackWindow);
				
				if(candidates.contains(concept))
				{
					candidates.remove(concept);
				}
				
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
	
//	private int getFeedbackCount(String student, String feedbackType)
//	{
//		return RollingWindow.sharedWindow().countEvents(HISTORY_WINDOW, student, "feedback", feedbackType);
//	}

	private void recordFeedback(MessageEvent me, String student, String feedbackType)
	{
		RollingWindow.sharedWindow().addEvent(me, student, feedbackType, "feedback");
		status = feedbackType + "\n" + status;
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}
	
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		MessageEvent me = (MessageEvent)event;
		String text = me.getText();
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		if(match != null)
		{
			me.addAnnotation("REVOICABLE", Arrays.asList(match));
		}
		annotateNudgeable(me);
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{ 
		return new Class[]{MessageEvent.class};
	}
	
	public String getStatus()
	{
		return status;
	}
	
	public static void test(RevoicingActor revoicer, String file, int verbosity)
	{
		System.out.println("testing '"+file+"'...");
		try
		{
			revoicer.sentenceMatcher.setVerbosity(verbosity);
			Scanner scan = new Scanner(new File(file));
		
			while(scan.hasNextLine())
			{
				String next = scan.nextLine().trim();
				String match = revoicer.testRevoice(next);
				double sim = 0;
				if(match != null)
					sim = revoicer.sentenceMatcher.getSentenceSimilarity(next, match);
				System.out.println(next+"\t"+match+"\t"+sim);
			}

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void annotateNudgeable(MessageEvent e)
	{
		List<String> evidence = Arrays.asList(e.getText());
		boolean nudgeable = false;
		for(String k : nudgePrompts.keySet())
		{
			if(e.hasAnnotations(k) && !e.hasAnnotations("EXPLANATION_CONTRIBUTION")//not been mentioned recently
					&& RollingWindow.sharedWindow().countEventsByTurns(9999, k) < 5 //not been mentioned much ever
					&& RollingWindow.sharedWindow().countEventsByTurns(9999, "feedback", k) == 0) //never nudged for this before
			{
				e.addAnnotation("NUDGEABLE_"+k, evidence);
				nudgeable = true;
			}
		}
		if(nudgeable)
			e.addAnnotation("NUDGEABLE", evidence);
			
	}
	
	public static void main(String [] args) throws Exception
	{
		RevoicingActor revoicer = new RevoicingActor(null);
		
		Scanner scan = new Scanner(System.in);
		
		String s;
		int verb = 0;

		System.out.println("enter a statement...");
		while(scan.hasNext())
		{
			s = scan.nextLine().trim();
			if(s.isEmpty())
				continue;

			
			if(s.startsWith("t "))
			{
				test(revoicer, s.substring(2).trim(), verb);
			}
			
			else if(s.equals("c"))
			{
				System.out.println(revoicer.sentenceMatcher.testCandidatesForContent(revoicer.candidates, verb > 0));
			}
			
			else if(s.startsWith("s "))
			{
				System.out.println("Dictionary:");
				System.out.println(revoicer.sentenceMatcher.getSynonyms(s.substring(2)));
				System.out.println("Wordnet:");
				System.out.println(SynonymSentenceMatcher.getWordnetSynonyms(s.substring(2)));
			}

			else if(s.startsWith("w "))
			{
				String[] split = s.substring(2).split(" ");
				System.out.println(revoicer.sentenceMatcher.getWordSimilarity(split[0], split[1]));
			}
			
			else if(s.matches("(reset)|(reload)"))
			{
				revoicer =  new RevoicingActor(null);
				System.out.println("reloading revoicer");
				
			}
			else try
			{
				verb = Integer.parseInt(s);
				System.out.println("verbosity set to "+verb);
			}
			catch(Exception e)	
			{
				System.out.println(revoicer.simRevoice(s, verb));
			}
			
		}
		
		//System.out.println(matcher.getCharacterSimilarity("v", "vanderwaals"));
	}
}
