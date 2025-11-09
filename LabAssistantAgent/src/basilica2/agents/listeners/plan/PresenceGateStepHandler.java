package basilica2.agents.listeners.plan;

import java.util.HashSet;
import java.util.Set;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.listeners.BasilicaAdapter;

class PresenceGateStepHandler implements StepHandler
{
    @Override
    public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
    {
        int required = 2;
        if (currentStep.attributes.containsKey("required_participants"))
        {
            try
            {
                required = Integer.parseInt(currentStep.attributes.get("required_participants"));
            }
            catch (NumberFormatException ignored) {}
        }

        PresenceWatcher watcher = new PresenceWatcher(overmind, source, currentStep.name, required);
        overmind.addHelper(watcher);
        watcher.checkExisting();
    }

    private static class PresenceWatcher extends BasilicaAdapter
    {
        private final InputCoordinator coordinator;
        private final PlanExecutor overmind;
        private final String stepName;
        private final int required;
        private final Set<String> present = new HashSet<String>();

        PresenceWatcher(PlanExecutor overmind, InputCoordinator coordinator, String stepName, int required)
        {
            super(overmind.getAgent());
            this.coordinator = coordinator;
            this.overmind = overmind;
            this.stepName = stepName;
            this.required = Math.max(1, required);
        }

        void checkExisting()
        {
            State state = StateMemory.getSharedState(overmind.getAgent());
            if (state != null)
            {
                present.addAll(state.getStudentIdList());
            }
            evaluate();
        }

        private void evaluate()
        {
            if (present.size() >= required)
            {
                stopListening(coordinator);
                overmind.stepDone(stepName);
            }
        }

        @Override
        public void processEvent(InputCoordinator source, edu.cmu.cs.lti.basilica2.core.Event event)
        {
            if (event instanceof PresenceEvent)
            {
                PresenceEvent pe = (PresenceEvent) event;
                if (PresenceEvent.PRESENT.equals(pe.getType()))
                {
                    present.add(pe.getUsername());
                    evaluate();
                }
                else if (PresenceEvent.ABSENT.equals(pe.getType()))
                {
                    present.remove(pe.getUsername());
                }
            }
        }

        @Override
        public Class[] getListenerEventClasses()
        {
            return new Class[] { PresenceEvent.class };
        }

        @Override
        public Class[] getPreprocessorEventClasses()
        {
            return new Class[0];
        }
    }
}
