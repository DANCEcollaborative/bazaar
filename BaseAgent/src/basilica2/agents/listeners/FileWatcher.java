package basilica2.agents.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;

import java.util.Map;

import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.events.TypingEvent;
import basilica2.agents.events.WhiteboardEvent;
import basilica2.agents.events.ReadyEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.events.FileEvent;
import basilica2.agents.listeners.PresenceWatcher;
import basilica2.agents.listeners.plan.StepHandler;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

import java.util.Hashtable;
import java.util.Map;
import java.lang.Math; 
import java.io.*;


public class FileWatcher extends BasilicaAdapter
{ 
	private InputCoordinator source;
	private String status = "";
	String filePath; 
	String fileSuffix;
	String[] fileNames; 
	Boolean[] fileCompleted; 
	String roomName; 

	public FileWatcher(Agent a) 
	{
		super(a);
		if (properties != null)
		{
			try{filePath = getProperties().getProperty("filePath", "./");}
			catch(Exception e) {e.printStackTrace();}
			try{fileSuffix = getProperties().getProperty("fileSuffix", ".txt");}
			catch(Exception e) {e.printStackTrace();}			
			fileNames = properties.getProperty("filenames", "").split("[\\s,]+");
			fileCompleted = new Boolean[fileNames.length]; 
			for (int i=0; i < fileCompleted.length; i++) {
				fileCompleted[i] = false; 
			}				
		}
		roomName = a.getRoomName();
	}

	public String getStatus()
	{
		return status;
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		File file; 
		FileEvent.fileEventType eventType = FileEvent.fileEventType.valueOf("created"); 
		for (int i=0; i < fileCompleted.length; i++) {
			if (!fileCompleted[i]) {
				file = new File(filePath + "room-" + roomName + "-" + fileNames[i] + fileSuffix);
				System.err.println("Checking file: " + file.getPath()); 
				if (file.exists()) {
					fileCompleted[i] = true;
					System.err.println("File newly exists: " + file.getPath()); 
					FileEvent fEvent = new FileEvent(source,fileNames[i],eventType);
					source.pushEvent(fEvent);
				}				
			}
		}
	}
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class, ReadyEvent.class, PresenceEvent.class, WhiteboardEvent.class, TypingEvent.class};
	}


	@Override
	public void processEvent(InputCoordinator source, Event event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return null;
	}

}
