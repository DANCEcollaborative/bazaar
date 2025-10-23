package basilica2.agents.listeners.plan;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.StepDoneEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class ReactionStepHandler implements StepHandler
{
    private PromptTable prompter;
    private static final String DEFAULT_PROMPT_FILE = "plans/plan_prompts.xml";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}|]+)(?:\\|([^}]*))?\\}");

	public ReactionStepHandler()
	{
		String promptsPath = DEFAULT_PROMPT_FILE;
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		if (properties != null)
		{
			promptsPath = properties.getProperty("prompt_file", promptsPath);
		}
		prompter = new PromptTable(promptsPath);
	}

	@Override
	public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
	{
        String expected = "";
        if (currentStep.attributes.containsKey("expected"))
        {
            expected = resolveTemplate(currentStep.attributes.get("expected"));
            currentStep.attributes.put("expected", expected);
        }

        if(currentStep.attributes.containsKey("expected_reaction"))
        {
            currentStep.attributes.put("expected_reaction", resolveTemplate(currentStep.attributes.get("expected_reaction")));
        }

        if(currentStep.attributes.containsKey("reaction_format"))
        {
            currentStep.attributes.put("reaction_format", resolveTemplate(currentStep.attributes.get("reaction_format")));
        }

		int attempts = 1;
		if (currentStep.attributes.containsKey("attempts"))
		{
			try
			{
				attempts = Integer.parseInt(currentStep.attributes.get("attempts"));
			}
			catch (NumberFormatException e)
			{
				Logger.commonLog(getClass().getSimpleName(), Logger.LOG_WARNING,
						"Invalid attempts value '" + currentStep.attributes.get("attempts") + "' for step " + currentStep.name);
			}
		}

		String successPrompt = currentStep.attributes.get("success_prompt");
		String retryPrompt = currentStep.attributes.get("retry_prompt");
		String failurePrompt = currentStep.attributes.get("failure_prompt");

        ReactionMonitor monitor = new ReactionMonitor(overmind, source, currentStep, prompter, expected, attempts,
                successPrompt, retryPrompt, failurePrompt);
        overmind.addHelper(monitor);
    }

	private static String normalize(String candidate)
	{
		if (candidate == null) { return ""; }
		return candidate.replaceAll("\\s+", "").toUpperCase();
	}

	private static class ReactionMonitor extends BasilicaAdapter
	{
		private final PlanExecutor overmind;
		@SuppressWarnings("unused")
		private final Step step;
		private final PromptTable prompter;
		private final InputCoordinator source;
		private final String expected;
		private final String successPrompt;
		private final String retryPrompt;
		private final String failurePrompt;
		private int attemptsRemaining;
		private boolean finished = false;

		ReactionMonitor(PlanExecutor overmind, InputCoordinator source, Step step, PromptTable prompter,
				String expectedReaction, int maxAttempts, String successPrompt, String retryPrompt, String failurePrompt)
		{
			super(overmind.getAgent());
			this.overmind = overmind;
			this.source = source;
			this.step = step;
			this.prompter = prompter;
			this.expected = normalize(expectedReaction);
			this.successPrompt = successPrompt;
			this.retryPrompt = retryPrompt;
			this.failurePrompt = failurePrompt;
			this.attemptsRemaining = Math.max(1, maxAttempts);
		}

		@Override
		public void processEvent(InputCoordinator source, edu.cmu.cs.lti.basilica2.core.Event event)
		{
			if (finished) { return; }
			if (event instanceof MessageEvent)
			{
				handleMessage((MessageEvent) event);
			}
		}

		private void handleMessage(MessageEvent me)
		{
			if (source.isAgentName(me.getFrom())) { return; }
			String text = me.getText();
			if (text == null) { return; }

			String trimmed = text.trim();
			if (trimmed.length() == 0) { return; }

			String lower = trimmed.toLowerCase();
			if (!lower.startsWith("reaction:")) { return; }

			String candidate = trimmed.substring("reaction:".length()).trim();
			if (candidate.length() == 0) { return; }

			evaluateCandidate(candidate);
		}

		private void evaluateCandidate(String candidate)
		{
			String normalizedCandidate = normalize(candidate);
		if (normalizedCandidate.equals(expected))
		{
			sendPrompt(successPrompt, null);
			finishStep();
				return;
			}

			attemptsRemaining = Math.max(0, attemptsRemaining - 1);

			if (attemptsRemaining > 0)
			{
				Map<String, String> slots = new HashMap<String, String>();
				slots.put("[ATTEMPTS_LEFT]", Integer.toString(attemptsRemaining));
				sendPrompt(retryPrompt, slots);
			}
			else
			{
				sendPrompt(failurePrompt, null);
				finishStep();
			}
		}

		private void sendPrompt(String promptId, Map<String, String> slots)
		{
			if (promptId == null || promptId.length() == 0) { return; }

            Map<String, String> filledSlots = (slots == null) ? new HashMap<String, String>() : new HashMap<String, String>(slots);
			State state = StateMemory.getSharedState(overmind.getAgent());
			if (state != null)
			{
				filledSlots.put("[NAMES]", state.getStudentNamesString());
				for (int i = 0; i < state.getStudentNames().size(); i++)
				{
					String studentName = state.getStudentNames().get(i);
					filledSlots.put("[NAME" + (i + 1) + "]", studentName);
				}
			}

			String text = prompter.lookup(promptId, filledSlots);
			MessageEvent response = new MessageEvent(source, overmind.getAgent().getUsername(), text, promptId);
			source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "ReactionStep", response,
					OutputCoordinator.HIGH_PRIORITY, 5.0, 3));
		}

		private void finishStep()
		{
			if (!finished)
			{
				finished = true;
				source.pushEvent(new StepDoneEvent(source, step.name));
			}
		}

		@Override
		public Class[] getListenerEventClasses()
		{
			return new Class[] { MessageEvent.class };
		}

		@Override
		public void preProcessEvent(InputCoordinator source, edu.cmu.cs.lti.basilica2.core.Event event)
		{}

		@Override
		public Class[] getPreprocessorEventClasses()
		{
			return new Class[] {};
		}
	}

    private static String resolveTemplate(String template)
    {
        if (template == null)
        {
            return "";
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        if (!matcher.find())
        {
            return template;
        }

        String key = matcher.group(1).trim();
        String defaultValue = matcher.group(2) != null ? matcher.group(2) : "";

        String replacement = null;
        Properties planProps = PropertiesLoader.loadProperties("PlanExecutor.properties");
        if (planProps != null)
        {
            replacement = planProps.getProperty(key);
        }
        if (replacement == null)
        {
            replacement = System.getProperty(key);
        }
        if (replacement == null)
        {
            replacement = System.getenv(key);
        }
        if (replacement == null)
        {
            replacement = defaultValue;
        }
        if (replacement == null)
        {
            replacement = "";
        }
        return replacement;
    }
}
