package basilica2.agents.listeners.plan;

import java.util.Properties;
import java.util.regex.Pattern;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.agents.listeners.Gatekeeper;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.project911.utils.log.Logger;

class ReadyOrSecretStepHandler implements StepHandler
{
    private final Properties planProperties;
    private final PromptTable promptTable;

    ReadyOrSecretStepHandler()
    {
        planProperties = PropertiesLoader.loadProperties("PlanExecutor.properties");
        promptTable = new PromptTable("plans/labassistant_plan_prompts.xml");
    }

    public static String getStepType()
    {
        return "ready_secret";
    }

    @Override
    public void execute(final Step currentStep, final PlanExecutor overmind, final InputCoordinator source)
    {
        final String secretFlagKey = currentStep.attributes.get("secret_flag");
        if (secretFlagKey != null && !secretFlagKey.isEmpty())
        {
            State state = StateMemory.getSharedState(overmind.getAgent());
            if (state != null)
            {
                state.more().put(secretFlagKey, Boolean.FALSE);
                StateMemory.commitSharedState(state, overmind.getAgent());
            }
        }

        final Gatekeeper gatekeeper = new Gatekeeper(overmind.getAgent());
        gatekeeper.setStepName(currentStep.name);

        String pattern = currentStep.attributes.get("pattern");
        if (pattern != null && !pattern.isEmpty())
        {
            gatekeeper.setKeyPhrase(pattern);
        }
        if (currentStep.attributes.containsKey("required_participants"))
        {
            try
            {
                int required = Integer.parseInt(currentStep.attributes.get("required_participants"));
                gatekeeper.setRequiredParticipants(required);
            }
            catch (NumberFormatException e)
            {
                Logger.commonLog(getClass().getSimpleName(), Logger.LOG_WARNING,
                        "Invalid required_participants value for " + currentStep.name);
            }
        }

        gatekeeper.resetGateForAllStudents();
        overmind.addHelper(gatekeeper);

        final String secretProperty = currentStep.attributes.get("secret_property");
        final String secretWord = resolveSecret(secretProperty);

        if (secretWord != null && !secretWord.isEmpty())
        {
            BasilicaAdapter secretListener = new SecretListener(overmind, source, secretWord, secretFlagKey);
            overmind.addHelper(secretListener);
        }

        String ackPromptId = currentStep.attributes.get("ack_prompt");
        if (pattern != null && ackPromptId != null && ackPromptId.length() > 0)
        {
            BasilicaAdapter ackListener = new ReadyAckListener(overmind, source, pattern, ackPromptId, promptTable);
            overmind.addHelper(ackListener);
        }
    }

    private String resolveSecret(String propertyKey)
    {
        if (propertyKey == null || propertyKey.isEmpty() || planProperties == null)
        {
            return null;
        }
        String value = planProperties.getProperty(propertyKey);
        return value == null ? null : value.trim();
    }

    private static class SecretListener extends BasilicaAdapter
    {
        private final PlanExecutor overmind;
        private final InputCoordinator source;
        private final Pattern secretPattern;
        private final String secretFlagKey;
        private boolean triggered = false;

        SecretListener(PlanExecutor overmind, InputCoordinator source, String secretWord, String secretFlagKey)
        {
            super(overmind.getAgent());
            this.overmind = overmind;
            this.source = source;
            this.secretFlagKey = secretFlagKey;
            String escaped = Pattern.quote(secretWord);
            this.secretPattern = Pattern.compile("^\\s*" + escaped + "\\s*$", Pattern.CASE_INSENSITIVE);
        }

        @Override
        public void processEvent(InputCoordinator source, edu.cmu.cs.lti.basilica2.core.Event event)
        {
            if (triggered) { return; }

            if (event instanceof MessageEvent)
            {
                MessageEvent me = (MessageEvent) event;
                if (source.isAgentName(me.getFrom())) { return; }

                String text = me.getText();
                if (text != null && secretPattern.matcher(text).matches())
                {
                    triggered = true;
                    markSecretTriggered();
                    overmind.stepDone();
                }
            }
        }

        private void markSecretTriggered()
        {
            if (secretFlagKey == null || secretFlagKey.isEmpty()) { return; }
            State state = StateMemory.getSharedState(getAgent());
            if (state != null)
            {
                state.more().put(secretFlagKey, Boolean.TRUE);
                StateMemory.commitSharedState(state, getAgent());
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

    private static class ReadyAckListener extends BasilicaAdapter
    {
        private final Pattern readyPattern;
        private final String promptId;
        private final InputCoordinator source;
        private final PromptTable promptTable;

        ReadyAckListener(PlanExecutor overmind, InputCoordinator source, String pattern, String promptId, PromptTable promptTable)
        {
            super(overmind.getAgent());
            this.source = source;
            this.promptId = promptId;
            this.promptTable = promptTable;
            this.readyPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
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
            if (readyPattern.matcher(text).matches())
            {
                String ackText = promptTable.lookup(promptId);
                MessageEvent ack = new MessageEvent(source, getAgent().getUsername(), ackText, promptId);
                source.pushProposal(PriorityEvent.makeBlackoutEvent("macro", "ReadyAck", ack,
                        OutputCoordinator.LOW_PRIORITY, 3.0, 2));
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
}
