package basilica2.accountable.listeners;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.swing.JFileChooser;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.util.TimeoutAdapter;
import dadamson.words.ASentenceMatcher.SentenceMatch;
import dadamson.words.SynonymSentenceMatcher;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

public abstract class AbstractAccountableActor extends BasilicaAdapter
{

	protected static final double WINDOW_BUFFER = 0.1;
	protected static final String AT_MOVE = "AT_MOVE";
	protected static final String SOURCE_NAME = "AccountableTalk";
	protected static final int HISTORY_WINDOW = 60 * 90;

	protected double feedbackWindow = 30;
	protected double candidateWindow = 10;
	protected double minimumMatch = 0.7;
	protected double blackoutTimeout = 15.0;
	protected double candidateCheckPriority = OutputCoordinator.HIGH_PRIORITY;
	protected double skipRatio = 0.0;
	protected double targetRatio = 0.5; // proportion of student moves of some
										// sort that this component wants as a
										// trigger threshold
	protected double ratioWindowTime = 60 * 5;

	protected String promptLabel;
	protected String candidateLabel;

	protected String[] productiveStudentTurns; // when detected in the check
												// window, prevent the AT move
												// from triggering
	protected String[] productiveFollowupTurns; // when detected in the check
												// window, prevent the followup
												// method from triggering.

	protected InputCoordinator source;
	protected HashMap<String, Integer> studentScores = new HashMap<String, Integer>();
	protected int teamScore = 0;

	protected String status = "";

	protected PromptTable accountablePrompts;
	protected ArrayList<String> candidates = new ArrayList<String>();
	protected SynonymSentenceMatcher sentenceMatcher;
	protected Map<String, String> slots = new HashMap<String, String>();

	protected boolean conditionActive = true;

	public AbstractAccountableActor(Agent a)
	{
		super(a, SOURCE_NAME);

		String condition = System.getProperty("basilica2.agents.condition", "feedback revoice agree remind social cooperate accountable_talk");
		Properties actorProperties = getProperties();
		String conditionFlag = actorProperties.getProperty("condition_flag", "accountable_talk");
		conditionActive = condition.contains(conditionFlag);
		log(Logger.LOG_WARNING, conditionFlag + " condition: " + conditionActive);
		RollingWindow.sharedWindow().setWindowSize(HISTORY_WINDOW, 20);

		try
		{
			String expertPath = actorProperties.getProperty("expert_statement_file", "accountable/exemplar_statements.txt");
			String dictionaryPath = actorProperties.getProperty("synonym_file", "accountable/synonyms.txt");
			String contentDictionaryPath = actorProperties.getProperty("content_synonym_file", "accountable/content_synonyms.txt");
			String stopwordsPath = actorProperties.getProperty("stopwords_file", "stopwords.txt");
			loadExpertStatements(expertPath);

			accountablePrompts = new PromptTable(properties.getProperty("accountable_prompt_file", "accountable/accountable_prompts.xml"));
			sentenceMatcher = new SynonymSentenceMatcher(contentDictionaryPath, stopwordsPath);
			sentenceMatcher.addDictionary(dictionaryPath, false);
			sentenceMatcher.enableWordNet(actorProperties.getProperty("use_wordnet", "false").contains("true"));

			minimumMatch = Double.parseDouble(properties.getProperty("minimum_match", "0.6"));
			feedbackWindow = Double.parseDouble(properties.getProperty("feedback_window", "60"));
			candidateWindow = Double.parseDouble(properties.getProperty("candidate_window", "10"));
			blackoutTimeout = Double.parseDouble(properties.getProperty("blackout_timeout", "15.0"));
			ratioWindowTime = Double.parseDouble(properties.getProperty("ratio_window_time", "" + ratioWindowTime));
			candidateCheckPriority = Double.parseDouble(properties.getProperty("candidate_check_priority", "" + candidateCheckPriority));
			promptLabel = actorProperties.getProperty("prompt_label", "AT_MOVE");
			candidateLabel = actorProperties.getProperty("candidate_label", "AT_CANDIDATE");
			targetRatio = Double.parseDouble(properties.getProperty("target_ratio", "" + targetRatio));
			skipRatio = Double.parseDouble(properties.getProperty("skip_ratio", "" + skipRatio));

			productiveStudentTurns = actorProperties.getProperty("productive_student_turns",
					"AGREE DISAGREE CHALLENGE_CONTRIBUTION QUESTION CONTENT REVOICABLE" + candidateLabel).split("\\s");
			productiveFollowupTurns = actorProperties.getProperty("productive_followup_turns", "EXPLANATION_CONTRIBUTION CONTENT REVOICABLE" + candidateLabel)
					.split("\\s");

		}
		catch (Exception e)
		{
			System.err.println("couldn't parse AccountableActor properties file:" + e.getMessage());
		}
	}

	protected void loadExpertStatements(String expertPath)
	{
		File expertFile = new File(expertPath);
		try
		{
			Scanner s = new Scanner(expertFile);
			while (s.hasNextLine())
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
		if (event instanceof MessageEvent && conditionActive)
		{
			checkForCandidate((MessageEvent) event);
		}

	}

	public String simAT(String text, int verbosity)
	{
		sentenceMatcher.setVerbosity(verbosity);
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		if (match == null) return "?";
		String conceptForm = accountablePrompts.lookup(match);
		slots.put("[STUDENT]", "David");
		slots.put("[CONCEPT]", conceptForm);
		return accountablePrompts.lookup(promptLabel, slots);
	}

	protected String testAT(String text)
	{
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);
		return match;
	}

	protected void checkForCandidate(final MessageEvent event)
	{
		if (event.hasAnnotations(candidateLabel) && shouldTriggerOnCandidate(event))
		{
			log(Logger.LOG_NORMAL, "received " + candidateLabel + " event");
			final String match = sentenceMatcher.getMatch(event.getText(), minimumMatch, candidates);
			if (getFeedbackCount(match) < 1)
			{
				log(Logger.LOG_NORMAL, "no previous match for " + match);
				double sim = sentenceMatcher.getSentenceSimilarity(event.getText(), match);
				if (shouldPromptForMove(event, sim, match))
				{
					log(Logger.LOG_NORMAL, "AT check starting...");
					PriorityEvent pete = PriorityEvent.makeBlackoutEvent(SOURCE_NAME, new Event()
					{
						{
							sender = source;
						}

						@Override
						public String getName()
						{
							return candidateLabel + " check";
						}

						@Override
						public String toString()
						{
							return "Hold-the-floor Event for " + candidateLabel + " check";
						}
					}, sim * candidateCheckPriority, 5, candidateWindow);

					pete.addCallback(new Callback()
					{
						@Override
						public void accepted(PriorityEvent p)
						{
							log(Logger.LOG_NORMAL, "Counting to " + candidateWindow + " before checking for " + promptLabel + " opportunity");
							new Timer(candidateWindow, new TimeoutAdapter()
							{
								@Override
								public void timedOut(String id)
								{
									AbstractAccountableActor.this.log(Logger.LOG_NORMAL, "checking for " + promptLabel + " opportunity");
									if (RollingWindow.sharedWindow().countAnyEvents(candidateWindow - WINDOW_BUFFER, productiveStudentTurns) < 1)
									{
										AbstractAccountableActor.this.log(Logger.LOG_NORMAL, "proposing " + promptLabel + " move...");
										makeAccountableProposal(event, match);
									}
									else if (RollingWindow.sharedWindow().countAnyEvents(candidateWindow - WINDOW_BUFFER, productiveFollowupTurns) < 1)
									{
										performFollowupCheck(event);
									}
								}
							}).start();
						}

						@Override
						public void rejected(PriorityEvent p)
						{
							log(Logger.LOG_NORMAL, "AT check rejected!");

						}
					});

					source.addProposal(pete);
				}

			}
		}
	}

	protected void makeAccountableProposal(final MessageEvent event, String match)
	{
		makeFeedbackProposal(event.getFrom(), match, event, accountablePrompts, promptLabel, OutputCoordinator.HIGHEST_PRIORITY);
	}

	protected void makeFeedbackProposal(final String student, final String concept, final Event reference, PromptTable prompts, final String prompt,
			double priority)
	{
		String studentName = StateMemory.getSharedState(agent).getStudentName(student);
		String conceptForm = prompts.lookup(concept);
		slots.put("[STUDENT]", studentName);
		slots.put("[CONCEPT]", conceptForm);
		List<String> others = StateMemory.getSharedState(agent).getStudentNames();
		others.remove(student);
		if (!others.isEmpty())
			slots.put("[OTHERSTUDENT]", others.get((int) (Math.random() * others.size())));
		else
			slots.put("[OTHERSTUDENT]", "Team");

		final MessageEvent me = new MessageEvent(source, this.getAgent().getUsername(), prompts.lookup(prompt, slots), prompt, student, AT_MOVE);
		me.setReference(reference);

		final BlacklistSource blacklistSource = new BlacklistSource(SOURCE_NAME, SOURCE_NAME, ""); // block
																									// messages
																									// from
																									// ourselves
																									// and
																									// everybody
		PriorityEvent pe = new PriorityEvent(source, me, priority, blacklistSource, 1.0);
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

				if (candidates.contains(concept))
				{
					candidates.remove(concept);
				}

				new Timer(blackoutTimeout, new TimeoutAdapter() // block
																// additional AT
																// messages for
																// a while
																// longer
						{
							public void timedOut(String id)
							{
								blacklistSource.getBlacklist().remove("");
							}
						}).start();
			}

			@Override
			public void rejected(PriorityEvent p)
			{
			}
		});
	}

	protected int getFeedbackCount(String key)
	{
		return RollingWindow.sharedWindow().countEvents(HISTORY_WINDOW, key, "feedback");
	}

	// protected int getFeedbackCount(String student, String feedbackType)
	// {
	// return RollingWindow.sharedWindow().countEvents(HISTORY_WINDOW, student,
	// "feedback", feedbackType);
	// }

	protected void recordFeedback(MessageEvent me, String student, String feedbackType)
	{
		RollingWindow.sharedWindow().addEvent(me, student, feedbackType, "feedback");
		status = feedbackType + "\n" + status;
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[] { MessageEvent.class };
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		MessageEvent me = (MessageEvent) event;
		String text = me.getText();
		String match = sentenceMatcher.getMatch(text, minimumMatch, candidates);

		List<SentenceMatch> matches = sentenceMatcher.getMatches(text, minimumMatch, candidates);
		// for(SentenceMatch m : matches)
		// System.out.println("match: "+m.sim + "\t" + m.matchText);

		if (match != null && shouldAnnotateAsCandidate(me))
		{
			me.addAnnotation(candidateLabel, Arrays.asList(match));
		}
	}

	/**
	 * candidate is a match - additional checks go here.
	 * 
	 * @param me
	 * @return
	 */
	public abstract boolean shouldAnnotateAsCandidate(MessageEvent me);

	/**
	 * candidate is a match - additional checks go here.
	 * 
	 * @param me
	 * @return
	 */
	public abstract boolean shouldTriggerOnCandidate(MessageEvent me);

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[] { MessageEvent.class };
	}

	public String getStatus()
	{
		return status;
	}

	public static void test(AbstractAccountableActor revoicer, String file, int verbosity)
	{
		System.out.println("testing '" + file + "'...");
		try
		{
			revoicer.sentenceMatcher.setVerbosity(verbosity);
			Scanner scan = new Scanner(new File(file));

			while (scan.hasNextLine())
			{
				String next = scan.nextLine().trim();
				String match = revoicer.testAT(next);
				double sim = 0;
				if (match != null) sim = revoicer.sentenceMatcher.getSentenceSimilarity(next, match);
				System.out.println(next + "\t" + match + "\t" + sim);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public MessageEvent getResponseEvent(String original, String... annotations)
	{
		MessageEvent challenger = null;
		List<RollingWindow.Entry> events = RollingWindow.sharedWindow().getAnyEvents(candidateWindow - 1, annotations);
		for (RollingWindow.Entry e : events)
		{
			MessageEvent me = (MessageEvent) e.event;
			String author = me.getFrom();
			if (!author.equals(original)) challenger = me;

		}
		return challenger;
	}

	/**
	 * this AT move could have been triggered, but wasn't because the student
	 * discussion is productive. Are there other ways to provide support?
	 * 
	 * @param event
	 */
	public abstract void performFollowupCheck(final MessageEvent event);

	/**
	 * We've identified a candidate. Should we prepare to perform this move?
	 * 
	 * @param me
	 *            the candidate message
	 * @param sim
	 *            similarity to a target statement
	 * @param match
	 *            the target statement that matched
	 * @return
	 */
	private boolean shouldPromptForMove(MessageEvent me, double sim, String match)
	{
		if (Math.random() > skipRatio)
		{
			return true;
		}
		else
		{
			log(Logger.LOG_NORMAL, "Skipping move opportunity.");
			return false;
		}
	}

	public static void main(String[] args) throws Exception
	{
		AbstractAccountableActor revoicer = new RevoiceActor(null);

		// JFileChooser chooser = new JFileChooser();
		// int result = chooser.showOpenDialog(null);

		Scanner scan;
		// if(result != chooser.APPROVE_OPTION)
		scan = new Scanner(System.in);
		// else scan = new Scanner(chooser.getSelectedFile());

		String s;
		int verb = 0;

		System.out.println("enter a statement...");
		while (scan.hasNext())
		{
			s = scan.nextLine().trim();
			if (s.isEmpty()) continue;

			if (s.startsWith("t "))
			{
				test(revoicer, s.substring(2).trim(), verb);
			}

			else if (s.equals("c"))
			{
				System.out.println(revoicer.sentenceMatcher.testCandidatesForContent(revoicer.candidates, verb > 0));
			}

			else if (s.startsWith("s "))
			{
				System.out.println("Dictionary:");
				System.out.println(revoicer.sentenceMatcher.getSynonyms(s.substring(2)));
				System.out.println("Wordnet:");
				System.out.println(SynonymSentenceMatcher.getWordnetSynonyms(s.substring(2)));
			}

			else if (s.startsWith("w "))
			{
				String[] split = s.substring(2).split(" ");
				System.out.println(revoicer.sentenceMatcher.getWordSimilarity(split[0], split[1]));
			}

			else if (s.matches("(reset)|(reload)"))
			{
				revoicer = new RevoiceActor(null);
				System.out.println("reloading revoicer");

			}
			else
				try
				{
					verb = Integer.parseInt(s);
					System.out.println("verbosity set to " + verb);
				}
				catch (Exception e)
				{
					try
					{
						double thresh = Double.parseDouble(s);
						revoicer.minimumMatch = thresh;
						System.out.printf("minimum threshold set to %.3f\n", thresh);
					}
					catch (Exception ex)
					{
						System.out.println(revoicer.simAT(s, verb));
					}
				}

		}

		scan.close();

		// System.out.println(matcher.getCharacterSimilarity("v",
		// "vanderwaals"));
	}
}
