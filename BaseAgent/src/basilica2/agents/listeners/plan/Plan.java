package basilica2.agents.listeners.plan;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

class Plan
{

	public String name;
	public int timedone = 0; //the number of seconds elapsed in the already-consumed script
	public Stage currentStage = null;
	public Map<String, Stage> stages = new LinkedHashMap<String, Stage>();

	public boolean removeStep(String s)
	{
		Logger.commonLog("Plan", Logger.LOG_NORMAL, "removing '"+s+"' according to the plan status file");
		Iterator<String> stageIt = stages.keySet().iterator();
		while(stageIt.hasNext())
		{
			String stageName = stageIt.next();
			Logger.commonLog("Plan", Logger.LOG_NORMAL, "looking at stage "+stageName);
			
			Stage stage = stages.get(stageName);
			if(stageName.equals(s))
			{
				Logger.commonLog("Plan", Logger.LOG_NORMAL, "removing stage "+stageName);
				stageIt.remove();
				if(stages.containsKey(stage.nextStage))
				{
					timedone = Math.max(timedone, stages.get(stage.nextStage).timeout);
					stages.get(stage.nextStage).timeout = 1;
					Logger.commonLog("Plan", Logger.LOG_NORMAL, "setting timedone to "+timedone);
				}
				
				return true;
			}
			
			
			for (int j = 0; j < stage.steps.size(); j++)
			{
				Step step = stage.steps.get(j);
				if (step.name.equalsIgnoreCase(s))
				{
					Logger.commonLog("Plan", Logger.LOG_NORMAL, "removing step "+s);
					stage.steps.remove(j);
					if (stage.steps.size() == 0)
					{
						Logger.commonLog("Plan", Logger.LOG_NORMAL, "clearing stage "+stageName);
						stageIt.remove();
						if(stages.containsKey(stage.nextStage))
						{
							timedone = Math.max(timedone, stages.get(stage.nextStage).timeout);
							stages.get(stage.nextStage).timeout = 1;
							Logger.commonLog("Plan", Logger.LOG_NORMAL, "setting timedone to "+timedone);
						}
					}

					
					
					return true;
				}
			}
		}
		return false;
	}
	
	// TODO: DON'T BE AN IDIOT
	public boolean removeStepOld(String s)
	{
		String[] snames = stages.keySet().toArray(new String[0]);
		for (int i = 0; i < snames.length; i++)
		{
			Stage st = stages.get(snames[i]);
			for (int j = 0; j < st.steps.size(); j++)
			{
				Step stp = st.steps.get(j);
				if (stp.name.equalsIgnoreCase(s))
				{
					st.steps.remove(j);
					if (st.steps.size() == 0)
					{
						stages.remove(snames[i]);
						//timedone = st.timeout;
					}
					timedone = stages.get(0).timeout;
					stages.get(0).timeout = 1;
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * set up kill-timers for every stage
	 * @param tr
	 */
	void launchStages(TimeoutReceiver tr)
	{
		String[] snames = stages.keySet().toArray(new String[0]);

		for (int i = 0; i < snames.length; i++)
		{
			Stage stage = stages.get(snames[i]);
			int time = 0;

			if (stage.timeout > 0 || i == 0)
			{
				time = Math.max(0, stage.timeout - timedone);

				tr.log("Plan", Logger.LOG_NORMAL, "<stage name=\"" + stage.name + "\" timeout=\"" + time + "\">set</stage>");
				Timer t = new Timer(time, snames[i], tr);
				t.start();
			}
		}
	}

	public String getCurrentStep()
	{
		if (currentStage != null) { return currentStage.currentStep.name; }
		return null;
	}
}