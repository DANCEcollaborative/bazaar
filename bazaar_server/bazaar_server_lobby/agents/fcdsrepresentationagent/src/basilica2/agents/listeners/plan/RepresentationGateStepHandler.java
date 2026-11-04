package basilica2.agents.listeners.plan;

import basilica2.agents.components.InputCoordinator;

public class RepresentationGateStepHandler implements StepHandler {
    public void execute(Step step, PlanExecutor executor, InputCoordinator source) {
        ((RepresentationPlanExecutor) executor).beginGate(step);
    }
}
