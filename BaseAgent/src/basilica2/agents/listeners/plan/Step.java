package basilica2.agents.listeners.plan;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.cs.lti.project911.utils.log.Logger;

import basilica2.util.Timer;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.agents.listeners.IntroductionsHandler;
import basilica2.agents.listeners.BasilicaAdapter.ListenerListener;
import basilica2.agents.listeners.plan.PlanExecutor.PlanSource;

public class Step {

	/**
	 * 
	 */
	private final PlanExecutor planExecutor;
	//private PromptTable prompter;

	/**
	 * @param modularPlanExecutor
	 */
	Step(PlanExecutor modularPlanExecutor)
	{
		planExecutor = modularPlanExecutor;

       // prompter = new PromptTable(planExecutor.getProperties().getProperty("planexecutor.plan_prompts"));
	}

	public String type;
    public boolean isDone = false;
    public String name;

	/**seconds after step is done before progressing. 0 means immediately.**/
    public int delay = 0;
	public Map<String, String> attributes;
	
	/**seconds until step is forcefully abandoned. 0 means never.**/
	public int timeout = 0;

    public void executeStep(InputCoordinator source) 
    {
		State news = StateMemory.getSharedState(planExecutor.getAgent());
        news.setStepInfo(planExecutor.currentPlan.currentStage.name, planExecutor.currentPlan.currentStage.type, name, type);
        StateMemory.commitSharedState(news, planExecutor.getAgent());
        
        planExecutor.interstepDelay = this.delay;

        if(timeout > 0)
        {
        	Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"starting "+timeout+" second timeout for "+name);
        	new Timer(timeout, this.name, planExecutor).start();
        }
        
        this.executeStepHandlers(source, type);
    }

	public void setAttributes(Map<String, String> attributes)
	{
		this.attributes = attributes;
	}

	public void executeStepHandlers(InputCoordinator source, String stepType)
	{

        Collection<StepHandler> handlers = planExecutor.getHandlers(stepType);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"Handlers for "+this.name+"("+stepType+"): "+handlers);
        
		if(handlers != null && !handlers.isEmpty())
	        for(StepHandler mrHandy : handlers)
	        {
	        	mrHandy.execute(this, planExecutor, planExecutor.source);
	        }
		else
			Logger.commonLog(getClass().getSimpleName(), Logger.LOG_WARNING, "No step handlers for step type "+stepType+" ("+this.name+")");

        planExecutor.updatePlanStatusFile(name);
		
	}
	
	public String toString()
	{
		return "Plan Step "+name;
		
	}
}