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
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.util.TimeoutAdapter;
import dadamson.words.SynonymSentenceMatcher;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public class AgreementActor extends BasilicaAdapter
{

	private static final double WINDOW_BUFFER = 0.1;
	private static final String AT_MOVE = "AT_MOVE";
	private static final String AGREE_CANDIDATE = "AGREE_CANDIDATE";
	private static final String SOURCE_NAME = "AccountableTalk";
	private static final int HISTORY_WINDOW = 60*90;

	private double feedbackWindow = 30;
	private double agreementWindow = 10;
	private double minimumMatch = 0.7; 
	private double blackoutTimeout = 15.0;
	private double agreementCheckPriority = 0.8;
	private String AGREE_DISAGREE_PROMPT = "AGREE_DISAGREE";
	
	InputCoordinator source;
	HashMap<String, Integer> studentScores = new HashMap<String, Integer>();
	int teamScore = 0;
	
	String status = "";
	
	PromptTable accountablePrompts;
	PromptTable nudgePrompts;
	private ArrayList<String> candidates = new ArrayList<String>();
	private SynonymSentenceMatcher sentenceMatcher;
	private Map<String, String> slots = new HashMap<String, String>();
	
	private boolean doAgree = true;
	
	public AgreementActor(Agent a)
	{
		super(a, SOURCE_NAME);

		String condition = System.getProperty("basilica2.agents.condition", "feedback revoice agree remind social cooperate");
		doAgree = condition.contains("agree");
		log(Logger.LOG_WARNING, "Agree/Disagree condition: "+doAgree);
		RollingWindow.sharedWindow().setWindowSize(HISTORY_WINDOW, 20);

		try
		{
			String expertPath = getProperties().getProperty("expert_statement_file", "expert_statements.txt");
			String dictionaryPath = getProperties().getProperty("synonym_file", "synonyms.txt");
			String contentDictionaryPath = getProperties().getProperty("content_synonym_file", "content_synonyms.txt");
			String stopwordsPath = getProperties().getProperty("stopwords_file", "stopwords.txt");
			loadExpertStatements(expertPath);
			
			accountablePrompts = new PromptTable(properties.getProperty("accountable_prompt_file", "accountabletalk/accountable_prompts.xml"));
			nudgePrompts = new PromptTable(properties.getProperty("nudge_prompt_file", "nudge_prompts.xml"));
			sentenceMatcher = new SynonymSentenceMatcher(contentDictionaryPath,stopwordsPath);
			sentenceMatcher.addDictionary(dictionaryPath, false);
			
			minimumMatch = Double.parseDouble(properties.getProperty("minimum_match", "0.6"));
			feedbackWindow = Double.parseDouble(properties.getProperty("feedback_window", "60"));
			agreementWindow = Double.parseDouble(properties.getProperty("agreement_window", "10"));
			blackoutTimeout = Double.parseDouble(properties.getProperty("blackout_timeout", "10.0"));
			agreementCheckPriority = Double.parseDouble(properties.getProperty("agreement_check_priority", "1.0"));
			AGREE_DISAGREE_PROMPT = getProperties().getProperty("agreement_prompt", "AGREE_DISAGREE");
			
			sentenceMatcher.enableWordNet(true);
		}
		catch(Exception e)
		{
			System.err.println("couldn't parse AgreementActor properties file:"+e.getMessage());
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
		if(event instanceof MessageEvent && doAgree)
		{
			checkForAgreeDisagree((MessageEvent)event);
		}
		
	}

	public String simAgree(String text, int verbosity)
	{
		sentenceMatcher.setVerbosity(verbosity);
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		if(match == null)
			return "?";
		String conceptForm = accountablePrompts.lookup(match);
		slots.put("[STUDENT]", "David");
		slots.put("[CONCEPT]", conceptForm);
		return accountablePrompts.lookup(AGREE_DISAGREE_PROMPT, slots);
	}
	
	private String testAgree(String text)
	{
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		return match;
	}
	
	private void checkForAgreeDisagree(final MessageEvent event)
	{
		if(event.hasAnnotations(AGREE_CANDIDATE))
		{
			final String match = sentenceMatcher.getMatch(event.getText(), minimumMatch, candidates);
			if(getFeedbackCount(match) < 1)
			{
				double sim = sentenceMatcher.getSentenceSimilarity(event.getText(), match);
				log(Logger.LOG_NORMAL,"Agreement check starting...");
				PriorityEvent pete = PriorityEvent.makeBlackoutEvent(SOURCE_NAME, new Event()
				{
					{
						sender = source;
					}
					
					@Override
					public String getName()
					{
						return "Do-Nothing";
					}
		
					@Override
					public String toString()
					{
						return "Do-Nothing-Event";
					}
				}, sim*agreementCheckPriority, 5, agreementWindow);
				
			
				pete.addCallback(new Callback()
				{
					@Override
					public void accepted(PriorityEvent p)
					{
						log(Logger.LOG_NORMAL,"Counting to "+agreementWindow+" before checking for agreement");
						new Timer(agreementWindow, new TimeoutAdapter()
						{
							@Override
							public void timedOut(String id)
							{
								AgreementActor.this.log(Logger.LOG_NORMAL,"checking for agreement");
								if(RollingWindow.sharedWindow().countAnyEvents(agreementWindow-WINDOW_BUFFER, "AGREE", "DISAGREE", AGREE_CANDIDATE, "CHALLENGE_CONTRIBUTION","QUESTION") < 1)
								{
									AgreementActor.this.log(Logger.LOG_NORMAL,"sending check...");
									makeAgreementCheckProposal(event, match);
								}
								else if(RollingWindow.sharedWindow().countAnyEvents(agreementWindow-WINDOW_BUFFER, "EXPLANATION_CONTRIBUTION", "CONTENT", AGREE_CANDIDATE) < 1)
								{
									if(RollingWindow.sharedWindow().countAnyEvents(agreementWindow-WINDOW_BUFFER,"DISAGREE", "CHALLENGE_CONTRIBUTION") > 0)
									{
										
										MessageEvent responseEvent = getResponseEvent(event.getFrom(), "DISAGREE", "CHALLENGE_CONTRIBUTION");
										String challenger = responseEvent.getFrom();
										if(challenger != null)
											makeFeedbackProposal(challenger, "", responseEvent, accountablePrompts, "REQUEST_DISAGREE_EXPLANATION", 0.2);
									}
									
									else
									{
										MessageEvent responseEvent = getResponseEvent(event.getFrom(), "AGREE");
										String challenger = responseEvent.getFrom();
										if(challenger != null)
											makeFeedbackProposal(challenger, "", responseEvent, accountablePrompts, "REQUEST_EXPLANATION", 0.2);
									}
								}
							}
						}).start();
					}
		
					@Override
					public void rejected(PriorityEvent p)
					{
						log(Logger.LOG_NORMAL,"Agreement check rejected!");
						
					}
				});
				
				source.addProposal(pete);
				
				/*new Timer(agreementWindow, new TimeoutAdapter()
				{
					@Override
					public void timedOut(String id)
					{
						AgreementActor.this.log(Logger.LOG_NORMAL,"checking for agreement");
						if(RollingWindow.sharedWindow().countAnyEvents(agreementWindow, "AFFIRMATIVE", "NEGATIVE", "QUESTION") < 1)
						{
							AgreementActor.this.log(Logger.LOG_NORMAL,"sending check...");
							makeAgreementCheckProposal(event.getFrom());
						}
					}
				}).start();*/
		
			}
		}
	}
	
	private void makeAgreementCheckProposal(final MessageEvent event, String match)
	{
		makeFeedbackProposal(event.getFrom(), match, event, accountablePrompts, AGREE_DISAGREE_PROMPT, 1.0);
	}

	private void makeFeedbackProposal(final String student, final String concept, final Event reference, PromptTable prompts, final String prompt, double priority)
	{
		String conceptForm = prompts.lookup(concept);
		slots.put("[STUDENT]", student);
		slots.put("[CONCEPT]", conceptForm);
		List<String> others = StateMemory.getSharedState(agent).getStudentNames();
		others.remove(student);
		if(!others.isEmpty()) slots.put("[OTHERSTUDENT]", others.get((int)(Math.random()*others.size())));
		else slots.put("[OTHERSTUDENT]", "Team");
		
		final MessageEvent me = new MessageEvent(source, this.getAgent().getUsername(), prompts.lookup(prompt, slots), prompt, student, AT_MOVE);
		me.setReference(reference);
		
		final BlacklistSource blacklistSource = new BlacklistSource(SOURCE_NAME, SOURCE_NAME, "");
		PriorityEvent pe = new PriorityEvent(source, me, priority, blacklistSource, 4.0);
		pe.setSource(SOURCE_NAME);
		
		source.pushProposal(pe);
		
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
				
				new Timer(blackoutTimeout, new TimeoutAdapter()
				{
					public void timedOut(String id)
					{
						blacklistSource.getBlacklist().remove("");
					}
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
		//log(Logger.LOG_NORMAL, text+":\t"+match);
		if(match != null)
		{
			me.addAnnotation(AGREE_CANDIDATE, Arrays.asList(match));
		}
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
	
	public static void test(AgreementActor revoicer, String file, int verbosity)
	{
		System.out.println("testing '"+file+"'...");
		try
		{
			revoicer.sentenceMatcher.setVerbosity(verbosity);
			Scanner scan = new Scanner(new File(file));
		
			while(scan.hasNextLine())
			{
				String next = scan.nextLine().trim();
				String match = revoicer.testAgree(next);
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



	public MessageEvent getResponseEvent(String original, String... annotations)
	{
		MessageEvent challenger = null;
		List<RollingWindow.Entry> events = RollingWindow.sharedWindow().getAnyEvents(agreementWindow-1, annotations);
		for(RollingWindow.Entry e : events)
		{
			MessageEvent me = (MessageEvent) e.event;
			String author= me.getFrom();
			if(!author.equals(original))
				challenger = me;
				
		}
		return challenger;
	}
	
	
	public static void main(String [] args) throws Exception
	{
		AgreementActor revoicer = new AgreementActor(null);
		
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
				revoicer =  new AgreementActor(null);
				System.out.println("reloading revoicer");
				
			}
			else try
			{
				verb = Integer.parseInt(s);
				System.out.println("verbosity set to "+verb);
			}
			catch(Exception e)	
			{
				System.out.println(revoicer.simAgree(s, verb));
			}
			
		}
	}
}
