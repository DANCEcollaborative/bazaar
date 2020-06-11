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
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PoseEvent;
import basilica2.agents.events.PoseEvent.poseEventType;
import basilica2.agents.events.priority.BlacklistSource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class PoseActor extends BasilicaAdapter
{
	private static final String SOURCE_NAME = "PoseActor";
	InputCoordinator source;
	private String status = "";
	private String identityAllUsers = "group";
	
	public PoseActor(Agent a)
	{
		super(a, SOURCE_NAME);
		log(Logger.LOG_WARNING, "PoseActor created");
		System.err.println("PoseActor created"); 
	}

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		System.err.println("PoseActor: entering processEvent"); 
		this.source = source;
		if(event instanceof PoseEvent)
		{
			System.err.println("PoseActor: PoseEvent received"); 
			poseEventResponse(source, (PoseEvent)event);
		}
		
	}
	
	private void poseEventResponse(InputCoordinator source, PoseEvent poseEvent)
	{
		String identity = poseEvent.getIdentity();	
		if (identity.equals(identityAllUsers)) {
			poseEventResponseGroup(source, poseEvent);
		}
		else {
			poseEventResponseIndividual(source, poseEvent, identity);
		}
	}
	
	private void poseEventResponseGroup(InputCoordinator source, PoseEvent poseEvent)
	{
		poseEventType prevPose = StateMemory.getSharedState(agent).getGroupPose(); 
		poseEventType poseType = poseEvent.getPoseEventType();
		System.err.println("PoseActor, poseEventResponseGroup -- prevPose: " + prevPose.toString() + " -- newPose: " + poseType.toString());

		// TEMPORARY CHANGE UNTIL VISUAL DETECTOR SENDS poseEventType.seated
		// if (prevPose == poseEventType.too_close && poseType == poseEventType.seated) {
		if (poseType == poseEventType.too_close) {
			issueDistanceWarning(source,poseEvent,identityAllUsers);
		}
		
		State s = State.copy(StateMemory.getSharedState(agent));
        s.setGroupPose(poseType);
        StateMemory.commitSharedState(s, agent);		
	}
	
	private void poseEventResponseIndividual(InputCoordinator source, PoseEvent poseEvent, String identity)
	{
		if (poseEvent.getPoseEventType() == poseEventType.seated) {
			poseEventResponseIndividualSeated(source, poseEvent, identity); 
		}	
		State s = State.copy(StateMemory.getSharedState(agent));
        s.setStudentPose(identity,poseEvent.getPoseEventType());
        StateMemory.commitSharedState(s, agent);
	}
	
	private void poseEventResponseIndividualSeated(InputCoordinator source, PoseEvent poseEvent, String thisIdentity)
	{
		poseEventType prevGroupPose = StateMemory.getSharedState(agent).getGroupPose(); 	
		if (poseEvent.getPoseEventType() == poseEventType.seated) {
			if (prevGroupPose == poseEventType.too_close) {		// Check if users were too close and now are all seated
				State s = StateMemory.getSharedState(agent);
				String[] studentIDs = s.getStudentIds(); 
				poseEventType poseType; 
				Boolean allStudentsSeated = true; 
				System.err.println("poseEventResponseIndividualSeated: checking if all students seated"); 
				for (int i = 0; i < studentIDs.length; i++)  {
					String otherIdentity = studentIDs[i]; 
					if (otherIdentity != thisIdentity) {
						poseType = s.getStudentPose(otherIdentity); 
						if (poseType != poseEventType.seated) {
							allStudentsSeated = false; 
						}						
					}
				}
				if (allStudentsSeated) {
					issueDistanceWarning(source,poseEvent,identityAllUsers); 
					State sCopy = State.copy(StateMemory.getSharedState(agent));
			        sCopy.setGroupPose(poseEventType.seated);
			        StateMemory.commitSharedState(sCopy, agent);		
				}
			}
		}
	}
	
	private void issueDistanceWarning(InputCoordinator source, PoseEvent poseEvent, String to) {
		System.err.println("====== PoseActor: ISSUING DISTANCE WARNING ===");
		String prompt = "In the future, please be more careful about social distancing when you rotate roles."; 
		MessageEvent newMe = new MessageEvent(source, agent.getName(), prompt);
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
		return new Class[]{PoseEvent.class};
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
