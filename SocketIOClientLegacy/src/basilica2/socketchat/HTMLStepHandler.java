package basilica2.socketchat;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PrioritySource;
import basilica2.agents.listeners.plan.PlanExecutor;
import basilica2.agents.listeners.plan.Step;
import basilica2.agents.listeners.plan.StepHandler;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class HTMLStepHandler implements StepHandler
{
	private String source_path = "activity_html/activity.html"; //FIXME: this belongs in a property
	Document soup = null;
	
	public HTMLStepHandler()
	{

		java.util.logging.Logger sioLogger = java.util.logging.Logger.getLogger("io.socket");
		sioLogger.setLevel(Level.SEVERE);
	
		
		try
		{
			soup = Jsoup.parse(new File(source_path), "UTF-8");
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void execute(Step currentStep, PlanExecutor overmind, InputCoordinator source)
	{
		if(soup != null && currentStep.attributes.containsKey("id"))
		{
			String id = currentStep.attributes.get("id");
		
			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,currentStep.type+" "+id+":"+currentStep.name);
			
			Elements selection = soup.select("#"+id);
			if(selection.size() > 0)
			{
				Element e = selection.get(0);
				String html = e.html();
				source.pushProposal(new PriorityEvent(source, 
						new DisplayHTMLEvent(source, source.getAgent().getUsername(), html), 
						1.0, new PrioritySource("Whiteboard", false), 120.0));
			}
		
		}
		overmind.stepDone();
		
		
//		String path = currentStep.attributes.get("path");
//		
//		Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,currentStep.type+" "+path+":"+currentStep.name);
//		
//		try
//		{
//			String text = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
//			
//			source.pushProposal(new PriorityEvent(source, 
//					new DisplayHTMLEvent(source, source.getAgent().getUsername(), text), 
//					1.0, new PrioritySource("Whiteboard", false), 60.0));
//		}
//		catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		overmind.stepDone();
	}
}
