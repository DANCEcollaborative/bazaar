package basilica2.agents.listeners;

import edu.cmu.cs.lti.basilica2.core.Event;

import java.util.Map;

import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.PromptEvent;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.PresenceWatcher;
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


public class MultiModalFilter extends BasilicaAdapter
{ 

	public static String GENERIC_NAME = "MultiModalFilter";
	public static String GENERIC_TYPE = "Filter";
	protected enum multiModalTag  
	{
		multimodal, identity, speech, location, facialExp, bodyPos, emotion;
	}
	private String multiModalDelim = ";%;";
	private String withinModeDelim = ":";	
	private boolean trackLocation = true;
	private boolean checkDistances = true;
	private Double minDistanceApart = 182.88;
	private InputCoordinator source;
	private String status = "";
	private boolean isTrackingLocation = false;
	private String sourceName; 

	public MultiModalFilter(Agent a) 
	{
		super(a);
		
		// get location-related properties
		try{trackLocation = Boolean.parseBoolean(getProperties().getProperty("track_location", "true"));}
		catch(Exception e) {e.printStackTrace();}
		try{checkDistances = Boolean.parseBoolean(getProperties().getProperty("check_distances", "true"));}
		catch(Exception e) {e.printStackTrace();}
		try{minDistanceApart = Double.valueOf(getProperties().getProperty("minimum_distance_apart", "182.88"));}
		catch(Exception e) {e.printStackTrace();}
		try{sourceName = getProperties().getProperty("source_name", "agent");}
		catch(Exception e) {e.printStackTrace();}
	}

	public void setTrackMode(boolean m)
	{
		trackLocation = m;
	}

	public String getStatus()
	{
		return status;
	}

	private void handleLaunchEvent(LaunchEvent le)
	{
//		startLocationTracking();
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent)
		{
			handleMessageEvent(source, (MessageEvent) e);
		}
	}

	// Handles multimodal messages 
	private void handleMessageEvent(InputCoordinator source, MessageEvent me)
	{
		String text = me.getText();
		String[] multiModalMessage = text.split(multiModalDelim);
		if (multiModalMessage.length > 1) {
			multiModalTag tag; 
			String [] messagePart; 

			// First check for identity since all updates would be for that identity
			Boolean identityFound = false;
			for (int i = 0; i < multiModalMessage.length && !identityFound; i++) {
				messagePart = multiModalMessage[i].split(withinModeDelim,2);
				tag = multiModalTag.valueOf(messagePart[0]);
				if (tag == (multiModalTag.identity)) {
					identityFound = true; 
					System.err.println("identify found: " + messagePart[1]);
					me.setFrom(messagePart[1]);
					checkPresence(source,me);					
				}
			}
			
			// Update the message sender's properties based on multimodal updates
			for (int i = 0; i < multiModalMessage.length; i++) {
				System.out.println("=====" + " Multimodal message entry -- " + multiModalMessage[i] + "======");
				messagePart = multiModalMessage[i].split(withinModeDelim,2);
				
				tag = multiModalTag.valueOf(messagePart[0]);
				
				switch (tag) {
				case multimodal:
					System.out.println("=========== multimodal message ===========");
					break;
				case identity:  // already handled above
					System.out.println("Identity: " + messagePart[1]);
					break;
				case speech:
					System.out.println("Speech: " + messagePart[1]);
					me.setText(messagePart[1]);
					break;
				case location:
					System.out.println("Location: " + messagePart[1]);
					if (trackLocation)
						updateLocation(source,me,messagePart[1]);
					break;
				case facialExp:
					System.out.println("Facial expression: " + messagePart[1]);
					break;
				case bodyPos:
					System.out.println("Body position: " + messagePart[1]);
					break;
				case emotion:
					System.out.println("Emotion: " + messagePart[1]);
					break;
				default:
					System.out.println(">>>>>>>>> Invalid multimodal tag: " + messagePart[0] + "<<<<<<<<<<");
				}
			}
		}  
    }
		

	private void checkPresence(InputCoordinator source, MessageEvent me) {
		String identity = me.getFrom();
        State state = State.copy(StateMemory.getSharedState(agent));
        State.Student user = state.getStudentById(identity);
        if (user == null) {
        	PresenceEvent pe = new PresenceEvent(source,identity,PresenceEvent.PRESENT);
        	handlePresenceEvent(source,pe);
        }     	
	}
	
	private void handlePresenceEvent(final InputCoordinator source, PresenceEvent pe)
	{
		State olds = StateMemory.getSharedState(agent);
		State news;
		if (pe.getType().equals(PresenceEvent.PRESENT))
		{
			if (olds != null)
			{
				news = State.copy(olds);
			}
			else
			{
				news = new State();
			}
			news.addStudent(pe.getUsername());
			Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"STUDENTS COUNT: " + news.getStudentCount());
			StateMemory.commitSharedState(news, agent);

		}
		else if (pe.getType().equals(PresenceEvent.ABSENT))
		{
			State updateState = State.copy(olds);
			updateState.removeStudent(pe.getUsername());
			StateMemory.commitSharedState(updateState, agent);
		}
	}

	private void updateLocation(InputCoordinator source, MessageEvent me, String location)
	{		
		System.err.print("Updating location for " + me.getFrom() + ": " + location);
		String identity = me.getFrom();
        State s = State.copy(StateMemory.getSharedState(agent));
        s.setLocation(identity, location);
        StateMemory.commitSharedState(s, agent);
        if (checkDistances) {
            checkDistances(source, me, identity);            	
        }   
	}
	
	private void checkDistances (InputCoordinator source, MessageEvent me, String identity) {
		System.err.print("===== Checking distances ======");
		Double[] myCoordinates = getLocationCoordinates(identity);	
		
		StringBuilder myCoordinatesString = new StringBuilder("");
		myCoordinatesString.append("x: " + Double.toString(myCoordinates[0]));
		myCoordinatesString.append("  --  y: " + Double.toString(myCoordinates[1]));
		myCoordinatesString.append("  --  z: " + Double.toString(myCoordinates[2]));
		System.err.println("checkDistances, myCoordinates  --  " + myCoordinatesString.toString());
		
		Double[] otherCoordinates; 
		State s = StateMemory.getSharedState(agent);
		String[] studentIDs = s.getStudentIds(); 
		String otherStudentID; 
		Double distance; 
		
		for (int i = 0; i < studentIDs.length; i++)  {
			System.out.println("Checking distances from other students ..."); 
            otherStudentID = studentIDs[i];
            if (!otherStudentID.equals(identity)) {
                otherCoordinates = getLocationCoordinates(otherStudentID);
        		if (otherCoordinates != null) {
            		StringBuilder otherCoordinatesString = new StringBuilder("");
            		otherCoordinatesString.append("x: " + Double.toString(otherCoordinates[0]));
            		otherCoordinatesString.append("  --  y: " + Double.toString(otherCoordinates[1]));
            		otherCoordinatesString.append("  --  z: " + Double.toString(otherCoordinates[2]));
            		System.err.println("checkDistances, otherCoordinates for " + otherStudentID + ", student " + Integer.toString(i) + "  --  " + otherCoordinatesString.toString());
            		
                    distance = calculateDistance(myCoordinates,otherCoordinates); 
                    System.err.println("Distance between " + s.getStudentName(identity) + " and " + s.getStudentName(otherStudentID) + ": " + Double.toString(distance));
                    if (distance > minDistanceApart) {
                    	System.err.println("Issuing distance warning"); 
                    	issueDistanceWarning(source,me,identity,otherStudentID);
                    }        			
        		}

            }
		}
		
	}
	
	private Double[] getLocationCoordinates (String identity) {
		Double[] locationCoordinates = new Double[3]; 
		String[] locationStrings = new String[3];
        State s = StateMemory.getSharedState(agent);
		String rawLocation = s.getLocation(identity); 
		if (rawLocation != null) {
			System.err.print("raw location coordinates for " + identity + ": " + rawLocation);
			locationStrings = rawLocation.split(withinModeDelim,3);
			for (int i=0; i<3; i++)
				locationCoordinates[i] = Double.valueOf(locationStrings[i]);
			return locationCoordinates; 
		}
		else return null;		
	}
	
	private Double calculateDistance (Double[] coordinatesA, Double[] coordinatesB ) {
		Double xDistance = coordinatesA[0] - coordinatesB[0];
		Double yDistance = coordinatesA[1] - coordinatesB[1];
		return Math.sqrt(xDistance*xDistance + yDistance*yDistance);
	}
	
	private void issueDistanceWarning (InputCoordinator source, MessageEvent me, String identity1, String identity2) {
        State s = StateMemory.getSharedState(agent);
		String prompt = s.getStudentName(identity1) + " and " + s.getStudentName(identity2) + ", remember to keep social-distancing in mind."; 
		MessageEvent newMe = new MessageEvent(source, this.getAgent().getUsername(), prompt);
		PriorityEvent blackout = PriorityEvent.makeBlackoutEvent(sourceName, newMe, 1.0, 5, 5);
		blackout.addCallback(new Callback()
		{
			@Override
			public void accepted(PriorityEvent p) {}
			@Override
			public void rejected(PriorityEvent p) {}  // ignore our rejected proposals
		});
		source.addProposal(blackout);
	}
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		//only MessageEvents will be delivered to this watcher.
		return new Class[]{MessageEvent.class};
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
