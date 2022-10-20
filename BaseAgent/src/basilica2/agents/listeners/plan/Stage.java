package basilica2.agents.listeners.plan;

import java.util.ArrayList;
import java.util.List;

import basilica2.agents.components.InputCoordinator;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.Timer;

class Stage
{

	/**
	 * 
	 */
	private final PlanExecutor planExecutor;

	/**
	 * @param modularPlanExecutor
	 */
	Stage(PlanExecutor modularPlanExecutor)
	{
		planExecutor = modularPlanExecutor;
	}

	public String name;
	public String type;
	public Step currentStep = null;
	public boolean isRunning = false;
	public boolean isDone = false;
	public int timeout = 0;
	int delay = 0;
	String nextStage = null;
	public List<Step> steps = new ArrayList<Step>();

	public void progressStage(InputCoordinator source)
	{
// 		System.err.println("Stage.java progressStage: about to clearHelpers"); 
// 		Logger.commonLog(this.getClass().getName(), Logger.LOG_NORMAL, "Stage.java progressStage: about to clearHelpers"); 
		planExecutor.clearHelpers();
		if (!isDone)
		{
// 			System.err.println("Stage.java progressStage:!isDone"); 
// 			Logger.commonLog(this.getClass().getName(), Logger.LOG_NORMAL, "Stage.java progressStage: !isDone"); 
			if (isRunning)
			{
// 				System.err.println("Stage.java progressStage: isRunning"); 
// 				Logger.commonLog(this.getClass().getName(), Logger.LOG_NORMAL, "Stage.java progressStage: isRunning"); 
				if (steps.size() > 0)
				{
					currentStep = steps.remove(0);
// 					System.err.println("Stage.java progressStage: currentStep removed, to be executed: " + currentStep.toString()); 
// 					Logger.commonLog(this.getClass().getName(), Logger.LOG_NORMAL, "Stage.java progressStage: currentStep removed, to be executed: \" + currentStep.toString()"); 
					currentStep.executeStep(source);
				}
				else if (nextStage == null || planExecutor.currentPlan.stages.get(nextStage).timeout == 0) //this stage has completed all of its steps and the next stage is not waiting on a timeout
				{
					Logger.commonLog(this.getClass().getName(), Logger.LOG_NORMAL,"finished stage "+this.name);
// 					System.err.println("finished stage "+this.name);
					if (nextStage != null)
					{
						if (delay == 0) {
							planExecutor.activateStage(nextStage);
						} else {
							Logger.commonLog(this.getClass().getName(), Logger.LOG_NORMAL, delay+" second delay before next stage = "+nextStage);
// 							System.err.println(delay+" second delay before next stage = "+nextStage);
							new Timer(delay, nextStage, planExecutor).start();
						}
					}
					else
					{
						planExecutor.setLaunched(false);
					}
				}
				//else wait for an absolute-time timer to fire
			}
		}
	}
}