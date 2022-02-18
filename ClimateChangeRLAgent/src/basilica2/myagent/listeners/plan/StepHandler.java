package basilica2.myagent.listeners.plan;

import basilica2.agents.components.InputCoordinator;

public interface StepHandler
{
	public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source);
}