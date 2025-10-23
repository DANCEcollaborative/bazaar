package basilica2.agents.listeners.plan;

import java.awt.Point;

import edu.cmu.cs.lti.project911.utils.log.Logger;


import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.WhiteboardEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PrioritySource;

public class WhiteboardStepHandler implements StepHandler
{
	
	@Override
	public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
	{
		
		String label = "step image";
		Point loc = new Point();
		double scale = 1.0;
		
		if(currentStep.attributes.containsKey("label"))
			label = currentStep.attributes.get("label");

		if(currentStep.attributes.containsKey("loc"))
		{
			try
			{
				String[] loco = currentStep.attributes.get("loc").split(",");
				loc = new Point(Integer.parseInt(loco[0]), Integer.parseInt(loco[1]));
			}
			catch(NumberFormatException e)
			{
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_ERROR,currentStep.attributes.get("loc")+" could not be parsed as a Point.");
			}
		}
		
		if(currentStep.attributes.containsKey("scale"))
		{
			try
			{
				scale = Double.parseDouble(currentStep.attributes.get("scale"));
			}
			catch(NumberFormatException e)
			{
				Logger.commonLog(getClass().getSimpleName(),Logger.LOG_ERROR,currentStep.attributes.get("scale")+" could not be parsed as a Double.");
			}
		}
		
		String path = currentStep.name;
		
		if(currentStep.attributes.containsKey("path"))
		{
			path = currentStep.attributes.get("path");
		}
		
		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,currentStep.type+" "+label+":"+currentStep.name);
		
		source.pushProposal(new PriorityEvent(source, WhiteboardEvent.makeWhiteboardImage(path, label, loc, source, currentStep.attributes.containsKey("delete")), 
				1.0, new PrioritySource("Whiteboard", false), 60.0));
		
		overmind.stepDone();
	}
}
