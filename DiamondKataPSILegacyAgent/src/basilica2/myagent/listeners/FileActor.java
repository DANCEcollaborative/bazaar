package basilica2.myagent.listeners;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.FileEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class FileActor extends BasilicaAdapter
{
	private static final String SOURCE_NAME = "FileActor";
	InputCoordinator source;
	private String status = "";
	
	public FileActor(Agent a)
	{
		super(a, SOURCE_NAME);
		log(Logger.LOG_WARNING, "FileActor created");
		System.err.println("FileActor created"); 
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		System.err.println("FileActor: entering processEvent"); 
		this.source = source;
		if(event instanceof FileEvent)
		{
			System.err.println("FileActor: FileEvent received"); 
			fileEventResponse(source, (FileEvent)event);
		}
		
	}
	
	private void fileEventResponse(InputCoordinator source, FileEvent fileEvent)
	{
		String fileName = fileEvent.getFileName();		
		switch(fileName)
		{
		case "test-a":
			System.err.println("FileActor: test-a file received"); 
			respondTestA(source,fileEvent);
			break; 	
		case "test-b":
			System.err.println("FileActor: test-b file received"); 
			respondTestB(source,fileEvent);
			break; 	
		case "test-c":
			System.err.println("FileActor: test-c file received"); 
			respondTestC(source,fileEvent);
			break; 	
		case "test-d":
			System.err.println("FileActor: test-d file received"); 
			respondTestD(source,fileEvent);
			break; 	
		default:
			System.err.println("No response available for filename " + fileName);
		}		
	}
	
	private void respondTestA(InputCoordinator source, FileEvent event) {
		System.err.println("FileActor: entering respondTestA()"); 
		String prompt = "You've passed a test case for the number of lines your program will output. Do you need to know the number of output lines for an iterative or a recursive solution?"; 
		
		MessageEvent newMe = new MessageEvent(source, this.getAgent().getUsername(), prompt);
		// newMe.setDestinationUser(identity1 + withinModeDelim + identity2);
		PriorityEvent blackout = PriorityEvent.makeBlackoutEvent(SOURCE_NAME, newMe, 1.0, 5, 5);
		blackout.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p) {}
			@Override
			public void rejected(PriorityEvent p) {}  // ignore our rejected proposals
		});
		source.pushProposal(blackout);
	}
	
	private void respondTestB(InputCoordinator source, FileEvent event) {
		System.err.println("FileActor: entering respondTestB()"); 
		String prompt = "Now that you're ready to begin working on passing the next test case, it's time to switch the Driver and Navigator roles."; 
		
		MessageEvent newMe = new MessageEvent(source, this.getAgent().getUsername(), prompt);
		// newMe.setDestinationUser(identity1 + withinModeDelim + identity2);
		PriorityEvent blackout = PriorityEvent.makeBlackoutEvent(SOURCE_NAME, newMe, 1.0, 5, 5);
		blackout.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p) {}
			@Override
			public void rejected(PriorityEvent p) {}  // ignore our rejected proposals
		});
		source.pushProposal(blackout);
	}
	
	private void respondTestC(InputCoordinator source, FileEvent event) {
		System.err.println("FileActor: entering respondTestC()"); 
		String prompt = "You've passed another test case. For the next text case, we'll work on producing the upper half of the diamond."; 
		
		MessageEvent newMe = new MessageEvent(source, this.getAgent().getUsername(), prompt);
		// newMe.setDestinationUser(identity1 + withinModeDelim + identity2);
		PriorityEvent blackout = PriorityEvent.makeBlackoutEvent(SOURCE_NAME, newMe, 1.0, 5, 5);
		blackout.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p) {}
			@Override
			public void rejected(PriorityEvent p) {}  // ignore our rejected proposals
		});
		source.pushProposal(blackout);
	}
	
	private void respondTestD(InputCoordinator source, FileEvent event) {
		System.err.println("FileActor: entering respondTestD()"); 
		String prompt = "You've passed yet another test case. For the final case, we'll work on producing a complete diamond."; 
		
		MessageEvent newMe = new MessageEvent(source, this.getAgent().getUsername(), prompt);
		// newMe.setDestinationUser(identity1 + withinModeDelim + identity2);
		PriorityEvent blackout = PriorityEvent.makeBlackoutEvent(SOURCE_NAME, newMe, 1.0, 5, 5);
		blackout.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p) {}
			@Override
			public void rejected(PriorityEvent p) {}  // ignore our rejected proposals
		});
		source.pushProposal(blackout);
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		return new Class[]{FileEvent.class};
	}
	
	@Override
	public void preProcessEvent(InputCoordinator source, Event event) {}

	
	@Override
	public Class[] getPreprocessorEventClasses()
	{ 
		return null;
	}
	
	public String getStatus()
	{
		return status;
	}
}
