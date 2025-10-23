package basilica2.agents.listeners.plan;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.agents.listeners.BasilicaAdapter.ListenerListener;

public final class EndStepOnStopListeningDelegate implements ListenerListener
{
	private final PlanExecutor overmind;
	private final String myStep;

	public EndStepOnStopListeningDelegate(PlanExecutor overmind, String myStep)
	{
		this.overmind = overmind;
		this.myStep = myStep;
	}

	@Override
	 public void startedListening(BasilicaAdapter bask, InputCoordinator source)
	 {}

	@Override
	 public void stoppedListening(BasilicaAdapter bask, InputCoordinator source)
	 {
	     if(overmind.currentPlan.getCurrentStep().equals(myStep))
	     {
	    	 Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"ending step "+myStep+"...");
	    	 overmind.stepDone();
	     }
	 }
}