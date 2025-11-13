package basilica2.agents.listeners.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
        List<String> expectedSpecs = new ArrayList<String>();

        if (currentStep.attributes.containsKey("expected_reactions"))
        {
            String resolved = resolveTemplate(currentStep.attributes.get("expected_reactions"));
            currentStep.attributes.put("expected_reactions", resolved);
            expectedSpecs.addAll(splitExpectedList(resolved));
        }
        else if (currentStep.attributes.containsKey("expected"))
        {
            String resolved = resolveTemplate(currentStep.attributes.get("expected"));
            currentStep.attributes.put("expected", resolved);
            expectedSpecs.addAll(splitExpectedList(resolved));
        }

        List<ParsedReaction> expectedReactions = new ArrayList<ParsedReaction>();
        for (String spec : expectedSpecs)
        {
            if (spec == null || spec.trim().isEmpty()) { continue; }
            try
            {
                expectedReactions.add(parseReactionFormula(spec));
            }
            catch (Exception e)
            {
                Logger.commonLog(getClass().getSimpleName(), Logger.LOG_WARNING,
                        "Unable to parse expected reaction '" + spec + "' for step " + currentStep.name);
            }
        }

        boolean unlimitedAttempts = false;
        int attempts = 1;
        if (currentStep.attributes.containsKey("attempts"))
        {
            String attemptAttr = currentStep.attributes.get("attempts");
            if (attemptAttr != null && attemptAttr.trim().equalsIgnoreCase("unlimited"))
            {
                unlimitedAttempts = true;
            }
            else
            {
                try
                {
                    attempts = Integer.parseInt(attemptAttr);
                }
                catch (NumberFormatException e)
                {
                    Logger.commonLog(getClass().getSimpleName(), Logger.LOG_WARNING,
                            "Invalid attempts value '" + attemptAttr + "' for step " + currentStep.name);
                }
            }
        }

        String successPrompt = currentStep.attributes.get("success_prompt");
        String retryPrompt = currentStep.attributes.get("retry_prompt");
        String failurePrompt = currentStep.attributes.get("failure_prompt");
        String secretFlag = currentStep.attributes.get("secret_flag");
        String reactantFeedbackPrompt = currentStep.attributes.get("reactant_feedback_prompt");
        String productFeedbackPrompt = currentStep.attributes.get("product_feedback_prompt");
        String stoichiometryFeedbackPrompt = currentStep.attributes.get("stoichiometry_feedback_prompt");
        String generalFeedbackPrompt = currentStep.attributes.get("general_feedback_prompt");

        String retryChoicePrompt = currentStep.attributes.get("retry_choice_prompt");
        String retryChoiceAckPrompt = currentStep.attributes.get("retry_choice_ack_prompt");
        String retryChoiceYesPattern = currentStep.attributes.get("retry_choice_yes_pattern");
        String retryChoiceNoPattern = currentStep.attributes.get("retry_choice_no_pattern");
        String invalidSpeciesPrompt = currentStep.attributes.get("invalid_species_prompt");
        String overlapSpeciesPrompt = currentStep.attributes.get("overlap_species_prompt");
        Set<String> allowedSpecies = parseAllowedSpecies(currentStep.attributes.get("allowed_species"));

        ReactionMonitor monitor = new ReactionMonitor(overmind, source, currentStep, prompter, expectedReactions,
                secretFlag, attempts, unlimitedAttempts, successPrompt, retryPrompt, failurePrompt,
                reactantFeedbackPrompt, productFeedbackPrompt, stoichiometryFeedbackPrompt, generalFeedbackPrompt,
                retryChoicePrompt, retryChoiceAckPrompt, retryChoiceYesPattern, retryChoiceNoPattern,
                invalidSpeciesPrompt, allowedSpecies, overlapSpeciesPrompt);
        overmind.addHelper(monitor);
        monitor.checkForSecretSkip();
    }

    private static final double COEFF_TOLERANCE = 1e-6;
    private static final Pattern TERM_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)?(.*)$");

    private static class ParsedReaction
    {
        final Map<String, Double> reactants;
        final Map<String, Double> products;

        ParsedReaction(Map<String, Double> reactants, Map<String, Double> products)
        {
            this.reactants = reactants;
            this.products = products;
        }
    }

    private static ParsedReaction parseReactionFormula(String reactionText)
    {
        if (reactionText == null)
        {
            throw new IllegalArgumentException("Reaction text is null");
        }

        String cleaned = reactionText.trim();
        if (cleaned.isEmpty())
        {
            throw new IllegalArgumentException("Reaction text is empty");
        }

        cleaned = cleaned.toUpperCase();
        cleaned = cleaned.replaceAll("\\s+", "");

        String[] sides = cleaned.split("(?:->|=)");
        if (sides.length != 2)
        {
            throw new IllegalArgumentException("Reaction must contain exactly one arrow");
        }

        Map<String, Double> reactants = parseSide(sides[0]);
        Map<String, Double> products = parseSide(sides[1]);

        if (reactants.isEmpty() || products.isEmpty())
        {
            throw new IllegalArgumentException("Reaction sides cannot be empty");
        }

        return new ParsedReaction(reactants, products);
    }

    private static Map<String, Double> parseSide(String side)
    {
        Map<String, Double> sideMap = new HashMap<String, Double>();
        String[] terms = side.split("\\+");
        for (String term : terms)
        {
            if (term == null || term.trim().isEmpty())
            {
                continue;
            }
            Matcher matcher = TERM_PATTERN.matcher(term.trim());
            if (!matcher.matches())
            {
                throw new IllegalArgumentException("Unable to parse term: " + term);
            }

            double coefficient = 1.0;
            if (matcher.group(1) != null && !matcher.group(1).isEmpty())
            {
                coefficient = Double.parseDouble(matcher.group(1));
            }

            String species = matcher.group(2).trim();
            if (species.isEmpty())
            {
                throw new IllegalArgumentException("Missing species for term: " + term);
            }

            Double existing = sideMap.get(species);
            if (existing == null)
            {
                sideMap.put(species, coefficient);
            }
            else
            {
                sideMap.put(species, existing + coefficient);
            }
        }
        return sideMap;
    }

    private static boolean reactionsMatch(ParsedReaction expected, ParsedReaction candidate)
    {
        return compareSides(expected.reactants, candidate.reactants)
                && compareSides(expected.products, candidate.products);
    }

    private static boolean compareSides(Map<String, Double> expected, Map<String, Double> actual)
    {
        if (expected.size() != actual.size())
        {
            return false;
        }

        for (Map.Entry<String, Double> entry : expected.entrySet())
        {
            Double actualValue = actual.get(entry.getKey());
            if (actualValue == null)
            {
                return false;
            }
            if (Math.abs(actualValue - entry.getValue()) > COEFF_TOLERANCE)
            {
                return false;
            }
        }
        return true;
    }

    private static List<String> splitExpectedList(String value)
    {
        List<String> items = new ArrayList<String>();
        if (value == null)
        {
            return items;
        }

        String[] parts = value.split("[;,]\\s*");
        for (String part : parts)
        {
            if (part != null && !part.trim().isEmpty())
            {
                items.add(part.trim());
            }
        }
        return items;
    }

    private static Set<String> parseAllowedSpecies(String value)
    {
        Set<String> allowed = new HashSet<String>();
        if (value == null)
        {
            return allowed;
        }
        String[] parts = value.split("[,;\\s]+");
        for (String part : parts)
        {
            if (part == null)
            {
                continue;
            }
            String token = part.trim().toUpperCase();
            if (!token.isEmpty())
            {
                allowed.add(token);
            }
        }
        return allowed;
    }

    private static class ReactionMonitor extends BasilicaAdapter
 	{
		private final PlanExecutor overmind;
		@SuppressWarnings("unused")
		private final Step step;
		private final PromptTable prompter;
		private final InputCoordinator source;
		private final List<ParsedReaction> expectedReactions;
		private final String secretFlagKey;
        private final String successPrompt;
        private final String retryPrompt;
        private final String failurePrompt;
        private final String reactantFeedbackPrompt;
        private final String productFeedbackPrompt;
        private final String stoichiometryFeedbackPrompt;
        private final String generalFeedbackPrompt;
        private final boolean unlimitedAttempts;
        private final String retryChoicePromptId;
        private final String retryChoiceAckPromptId;
        private final Pattern retryYesPattern;
        private final Pattern retryNoPattern;
        private final String invalidSpeciesPrompt;
        private final String overlapSpeciesPrompt;
        private final Set<String> allowedSpecies;
        private final int maxAttemptsPerCycle;
        private int attemptsRemaining;
        private boolean finished = false;
        private boolean awaitingRetryChoice = false;
        private RetryChoiceListener retryChoiceListener;

        ReactionMonitor(PlanExecutor overmind, InputCoordinator source, Step step, PromptTable prompter,
            List<ParsedReaction> expectedReactions, String secretFlagKey, int maxAttempts, boolean unlimitedAttempts, String successPrompt,
            String retryPrompt, String failurePrompt, String reactantFeedbackPrompt,
            String productFeedbackPrompt, String stoichiometryFeedbackPrompt, String generalFeedbackPrompt,
            String retryChoicePromptId, String retryChoiceAckPromptId, String retryYesPattern, String retryNoPattern,
            String invalidSpeciesPrompt, Set<String> allowedSpecies, String overlapSpeciesPrompt)
        {
            super(overmind.getAgent());
            this.overmind = overmind;
            this.source = source;
            this.step = step;
            this.prompter = prompter;
            this.expectedReactions = expectedReactions;
            this.secretFlagKey = secretFlagKey;
            this.successPrompt = successPrompt;
            this.retryPrompt = retryPrompt;
            this.failurePrompt = failurePrompt;
            this.reactantFeedbackPrompt = reactantFeedbackPrompt;
            this.productFeedbackPrompt = productFeedbackPrompt;
            this.stoichiometryFeedbackPrompt = stoichiometryFeedbackPrompt;
            this.generalFeedbackPrompt = generalFeedbackPrompt;
            this.unlimitedAttempts = unlimitedAttempts;
            this.maxAttemptsPerCycle = unlimitedAttempts ? Integer.MAX_VALUE : Math.max(1, maxAttempts);
            this.attemptsRemaining = this.maxAttemptsPerCycle;
            this.retryChoicePromptId = retryChoicePromptId;
            this.retryChoiceAckPromptId = retryChoiceAckPromptId;
            this.retryYesPattern = (retryYesPattern != null && retryYesPattern.length() > 0) ? Pattern.compile(retryYesPattern, Pattern.CASE_INSENSITIVE) : null;
            this.retryNoPattern = (retryNoPattern != null && retryNoPattern.length() > 0) ? Pattern.compile(retryNoPattern, Pattern.CASE_INSENSITIVE) : null;
            this.invalidSpeciesPrompt = invalidSpeciesPrompt;
            if (allowedSpecies != null)
            {
                this.allowedSpecies = new HashSet<String>(allowedSpecies);
            }
            else
            {
                this.allowedSpecies = new HashSet<String>();
            }
            this.overlapSpeciesPrompt = overlapSpeciesPrompt;
        }

		void checkForSecretSkip()
		{
			if (isSecretTriggered())
			{
				finishViaSecret();
			}
		}

		@Override
		public void processEvent(InputCoordinator source, edu.cmu.cs.lti.basilica2.core.Event event)
		{
			if (finished) { return; }
			if (isSecretTriggered())
			{
				finishViaSecret();
				return;
			}
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

			String candidate = extractCandidate(trimmed);
			if (candidate.length() == 0)
			{
                sendFormatReminder(trimmed);
                return;
            }

			evaluateCandidate(candidate);
		}

        private String extractCandidate(String trimmed)
        {
            if (trimmed == null)
            {
                return "";
            }

            String normalized = trimmed.trim();
            if (normalized.length() == 0)
            {
                return "";
            }

            String lower = normalized.toLowerCase();
            int prefixIndex = lower.indexOf("reaction");
            if (prefixIndex != 0)
            {
                return "";
            }

            int colonIndex = lower.indexOf(':', "reaction".length());
            if (colonIndex == -1)
            {
                return "";
            }

            String afterColon = normalized.substring(colonIndex + 1).trim();
            return afterColon;
        }

        private void sendFormatReminder(String originalText)
        {
            if (step.attributes.containsKey("format_prompt") && isPotentialReaction(originalText))
            {
                String promptId = step.attributes.get("format_prompt");
                sendPrompt(promptId, null);
            }
        }

        private boolean isPotentialReaction(String text)
        {
            if (text == null)
            {
                return false;
            }
            String simplified = text.replaceAll("\\s+", "").toUpperCase();
            int arrowIndex = simplified.indexOf("->");
            int equalsIndex = simplified.indexOf('=');
            int splitIndex;
            int delimiterLength;
            if (arrowIndex >= 0)
            {
                splitIndex = arrowIndex;
                delimiterLength = 2;
            }
            else if (equalsIndex >= 0)
            {
                splitIndex = equalsIndex;
                delimiterLength = 1;
            }
            else
            {
                return false;
            }

            String lhs = simplified.substring(0, splitIndex);
            String rhs = simplified.substring(splitIndex + delimiterLength);
            return isReactionSide(lhs) && isReactionSide(rhs);
        }

        private boolean isReactionSide(String side)
        {
            if (side == null || side.length() == 0)
            {
                return false;
            }
            String[] species = side.split("\\+");
            if (species.length == 0)
            {
                return false;
            }
            for (String sp : species)
            {
                if (sp.length() == 0)
                {
                    return false;
                }
                if (!sp.matches("[A-Z0-9()]+"))
                {
                    return false;
                }
            }
            return true;
        }

        private void evaluateCandidate(String candidate)
        {
            ParsedReaction candidateReaction;
            try
            {
                candidateReaction = parseReactionFormula(candidate);
            }
            catch (Exception e)
            {
                provideFeedback(MatchOutcome.NO_MATCH);
                handleIncorrectAttempt();
                return;
            }
            if (hasInvalidSpecies(candidateReaction))
            {
                if (invalidSpeciesPrompt != null && invalidSpeciesPrompt.length() > 0)
                {
                    sendPrompt(invalidSpeciesPrompt, null);
                }
                else
                {
                    provideFeedback(MatchOutcome.NO_MATCH);
                }
                handleIncorrectAttempt();
                return;
            }
            if (hasOverlappingSpecies(candidateReaction))
            {
                if (overlapSpeciesPrompt != null && overlapSpeciesPrompt.length() > 0)
                {
                    sendPrompt(overlapSpeciesPrompt, null);
                }
                else
                {
                    provideFeedback(MatchOutcome.NO_MATCH);
                }
                handleIncorrectAttempt();
                return;
            }

            MatchOutcome outcome = evaluateMatch(expectedReactions, candidateReaction);
            if (outcome == MatchOutcome.EXACT_MATCH)
            {
                sendPrompt(successPrompt, null);
                finishStep();
                return;
            }

			provideFeedback(outcome);
			handleIncorrectAttempt();
		}

        private void provideFeedback(MatchOutcome outcome)
        {
            String promptId = null;
            switch (outcome)
            {
                case STOICHIOMETRY_MISMATCH:
                    promptId = stoichiometryFeedbackPrompt;
                    break;
                case REACTANT_MATCH_ONLY:
                    promptId = reactantFeedbackPrompt;
                    break;
                case PRODUCT_MATCH_ONLY:
                    promptId = (productFeedbackPrompt != null && productFeedbackPrompt.length() > 0)
                            ? productFeedbackPrompt : stoichiometryFeedbackPrompt;
                    break;
                default:
                    promptId = generalFeedbackPrompt;
                    break;
            }

            if (promptId != null && promptId.length() > 0)
            {
                sendPrompt(promptId, null);
            }
        }

        private MatchOutcome evaluateMatch(List<ParsedReaction> expectedList, ParsedReaction candidate)
        {
            if (expectedList == null || expectedList.isEmpty())
            {
                return MatchOutcome.EXACT_MATCH;
            }

            MatchOutcome best = MatchOutcome.NO_MATCH;
            for (ParsedReaction expected : expectedList)
            {
                boolean reactantSpecies = speciesMatch(expected.reactants, candidate.reactants);
                boolean productSpecies = speciesMatch(expected.products, candidate.products);
                boolean reactantStoichiometry = reactantSpecies && compareSides(expected.reactants, candidate.reactants);
                boolean productStoichiometry = productSpecies && compareSides(expected.products, candidate.products);

                if (reactantStoichiometry && productStoichiometry)
                {
                    return MatchOutcome.EXACT_MATCH;
                }

                if (reactantSpecies && productSpecies)
                {
                    best = upgradeOutcome(best, MatchOutcome.STOICHIOMETRY_MISMATCH);
                }
                else if (reactantSpecies)
                {
                    best = upgradeOutcome(best, MatchOutcome.REACTANT_MATCH_ONLY);
                }
                else if (productSpecies)
                {
                    best = upgradeOutcome(best, MatchOutcome.PRODUCT_MATCH_ONLY);
                }
            }
            return best;
        }

        private boolean hasInvalidSpecies(ParsedReaction reaction)
        {
            if (allowedSpecies.isEmpty())
            {
                return false;
            }
            for (String species : reaction.reactants.keySet())
            {
                if (!allowedSpecies.contains(species))
                {
                    return true;
                }
            }
            for (String species : reaction.products.keySet())
            {
                if (!allowedSpecies.contains(species))
                {
                    return true;
                }
            }
            return false;
        }

        private boolean hasOverlappingSpecies(ParsedReaction reaction)
        {
            for (String species : reaction.reactants.keySet())
            {
                if (reaction.products.containsKey(species))
                {
                    return true;
                }
            }
            return false;
        }

        private MatchOutcome upgradeOutcome(MatchOutcome current, MatchOutcome candidate)
        {
            if (candidate == MatchOutcome.NO_MATCH)
            {
                return current;
            }
            if (current == MatchOutcome.NO_MATCH)
            {
                return candidate;
            }
            return (priority(candidate) < priority(current)) ? candidate : current;
        }

        private int priority(MatchOutcome outcome)
        {
            switch (outcome)
            {
                case STOICHIOMETRY_MISMATCH:
                    return 1;
                case REACTANT_MATCH_ONLY:
                    return 2;
                case PRODUCT_MATCH_ONLY:
                    return 3;
                default:
                    return 99;
            }
        }

        private boolean speciesMatch(Map<String, Double> expected, Map<String, Double> actual)
        {
            if (expected.size() != actual.size())
            {
                return false;
            }
            return actual.keySet().equals(expected.keySet());
        }

        private void handleIncorrectAttempt()
        {
            if (!unlimitedAttempts)
            {
                attemptsRemaining = Math.max(0, attemptsRemaining - 1);

                if (attemptsRemaining > 0)
                {
                    Map<String, String> slots = new HashMap<String, String>();
                    slots.put("[ATTEMPTS_LEFT]", Integer.toString(attemptsRemaining));
                    sendPrompt(retryPrompt, slots);
                }
                else
                {
                    if (!tryOfferExtraAttempts())
                    {
                        sendPrompt(failurePrompt, null);
                        finishStep();
                    }
                }
            }
            else
            {
                if (retryPrompt != null && retryPrompt.length() > 0)
                {
                    sendPrompt(retryPrompt, null);
                }
            }
        }

        private boolean tryOfferExtraAttempts()
        {
            if (retryChoicePromptId == null || retryYesPattern == null || retryNoPattern == null || unlimitedAttempts)
            {
                return false;
            }
            if (awaitingRetryChoice)
            {
                return true;
            }
            awaitingRetryChoice = true;
            sendPrompt(retryChoicePromptId, null);
            retryChoiceListener = new RetryChoiceListener(overmind, retryYesPattern, retryNoPattern);
            overmind.addHelper(retryChoiceListener);
            return true;
        }

        private void resetAttemptsForRetry()
        {
            attemptsRemaining = maxAttemptsPerCycle;
            awaitingRetryChoice = false;
            if (retryChoiceListener != null)
            {
                retryChoiceListener.stopListening(source);
                retryChoiceListener = null;
            }
            if (retryChoiceAckPromptId != null && retryChoiceAckPromptId.length() > 0)
            {
                sendPrompt(retryChoiceAckPromptId, null);
            }
        }

        private void declineExtraAttempts()
        {
            awaitingRetryChoice = false;
            if (retryChoiceListener != null)
            {
                retryChoiceListener.stopListening(source);
                retryChoiceListener = null;
            }
            finishStep();
        }

        private class RetryChoiceListener extends BasilicaAdapter
        {
            private final Pattern yesPattern;
            private final Pattern noPattern;

            RetryChoiceListener(PlanExecutor overmind, Pattern yesPattern, Pattern noPattern)
            {
                super(overmind.getAgent());
                this.yesPattern = yesPattern;
                this.noPattern = noPattern;
            }

            @Override
            public void processEvent(InputCoordinator source, edu.cmu.cs.lti.basilica2.core.Event event)
            {
                if (!(event instanceof MessageEvent))
                {
                    return;
                }
                MessageEvent me = (MessageEvent) event;
                if (source.isAgentName(me.getFrom()))
                {
                    return;
                }
                String text = me.getText();
                if (text == null)
                {
                    return;
                }
                String trimmed = text.trim();
                if (yesPattern.matcher(trimmed).matches())
                {
                    resetAttemptsForRetry();
                }
                else if (noPattern.matcher(trimmed).matches())
                {
                    declineExtraAttempts();
                }
            }

            @Override
            public Class[] getListenerEventClasses()
            {
                return new Class[] { MessageEvent.class };
            }

            @Override
            public Class[] getPreprocessorEventClasses()
            {
                return new Class[0];
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

		private boolean isSecretTriggered()
		{
			if (secretFlagKey == null || secretFlagKey.isEmpty())
			{
				return false;
			}
			State state = StateMemory.getSharedState(overmind.getAgent());
			if (state == null)
			{
				return false;
			}
			Object value = state.more().get(secretFlagKey);
			if (value instanceof Boolean)
			{
				return ((Boolean) value).booleanValue();
			}
			return value != null && "true".equalsIgnoreCase(value.toString());
		}

		private void finishViaSecret()
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

    private enum MatchOutcome
    {
        EXACT_MATCH,
        STOICHIOMETRY_MISMATCH,
        REACTANT_MATCH_ONLY,
        PRODUCT_MATCH_ONLY,
        NO_MATCH
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
