/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.social.listeners;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.social.data.Message;
import basilica2.social.data.ModelInfo;
import basilica2.social.data.PerformanceInfo;
import basilica2.social.data.StateInfo;
import basilica2.social.data.TriggerInfo;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.social.events.SocialTriggerEvent;
import basilica2.social.events.SocialTurnPerformedEvent;
import basilica2.social.utilities.*;
import basilica2.util.Timer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class ModelBasedTriggerComputer extends BasilicaAdapter 
{

    class ChatContribution 
    {
        long time;
        String from, text;
    };
    
    public static String GENERIC_NAME = "ModelBasedTriggerComputer";
    public static String GENERIC_TYPE = "Computer";
    private double confidenceThreshold = 0.55;
    private String modelFilename;
    private SimpleMIRAModel model;
    private String filterFilename;
    private GaussianFilter filter;
    private String agentName = "Tutor";

    //Information about social turns performed by Tutor
    private List<String> performedSocialTurns = new ArrayList<String>();

    //Persistent Information for Feature Extraction
    private List<ChatContribution> chatContributions = new ArrayList<ChatContribution>();
    private List<Boolean> contributionIsSocial = new ArrayList<Boolean>();
    private long startTime = -1;
    private boolean tutorStarted = true;
    private String[] recentStudentTurns = new String[3];
    private String[] recentTutorTurns = new String[3];
    private String currentDiscourseState = "__NONE__";
    private String previousDiscourseState = "__NONE__";

    //Utilities for feature extraction
    private Vocab studentVocab,  tutorVocab;
    private List<String> participants = new ArrayList<String>();
    private GeneralInquirer generalInquirer;
    private DBAnnotator semanticAnnotator;
    private String[] discouseStates;
    private InputCoordinator source;

    public ModelBasedTriggerComputer(Agent a) {
        super(a);
        //Load model from properties
        modelFilename = getProperties().getProperty("model.triggercomputer.modelfile");
        try {
            model = new SimpleMIRAModel(modelFilename);
        } catch (IOException ex) {
            ex.printStackTrace();
            log(Logger.LOG_ERROR, "Unable to load SimpleMIRA model");
        }

        //Load filter from properties
        filterFilename = getProperties().getProperty("model.triggercomputer.filterfile");
        try {
            filter = new GaussianFilter(filterFilename);
        } catch (IOException ex) {
            ex.printStackTrace();
            log(Logger.LOG_ERROR, "Unable to load Filter");
        }

        String sVocabFile = getProperties().getProperty("model.triggercomputer.studentvocab");
        studentVocab = new Vocab(false, sVocabFile);

        String tVocabFile = getProperties().getProperty("model.triggercomputer.tutorvocab");
        tutorVocab = new Vocab(false, tVocabFile);

        String giLexFile = getProperties().getProperty("model.triggercomputer.gilex");
        generalInquirer = new GeneralInquirer(giLexFile);

        String dbDir = getProperties().getProperty("model.triggercomputer.dictionarydir");
        semanticAnnotator = new DBAnnotator(dbDir);

        confidenceThreshold = Double.parseDouble(getProperties().getProperty("model.triggercomputer.triggerthreshold"));

        String statesFile = getProperties().getProperty("model.triggercomputer.stateslist");
        try {
            BufferedReader statesIn = new BufferedReader(new FileReader(statesFile));
            int nStates = Integer.parseInt(statesIn.readLine().trim());
            discouseStates = new String[nStates];
            for (int i = 0; i < nStates; i++) {
                discouseStates[i] = statesIn.readLine().trim();
            }
            statesIn.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            log(Logger.LOG_ERROR, "Unable to load Filter");
        }

        for (int i = 0; i < recentStudentTurns.length; i++) {
            recentStudentTurns[i] = "";
        }

        for (int i = 0; i < recentTutorTurns.length; i++) {
            recentTutorTurns[i] = "";
        }

        agentName = a.getName().substring(0, a.getName().indexOf("_"));
    }

//    private TriggerInfoMemory getTriggerInfoMemory() {
//        String triggerMemoryName = properties.getProperty("model.triggercomputer.triggerinfomemory");
//        return (TriggerInfoMemory) this.agent.getComponent(triggerMemoryName);
//    }
//
//    private SocialController getSocialController() {
//        String socialControllerName = properties.getProperty("model.triggercomputer.socialcontroller");
//        return (SocialController) this.agent.getComponent(socialControllerName);
//    }



    private void computeAndDispatchTrigger(GenericFeatureVector gfv) {
        double predictedConfidence = model.applyTo(gfv);
        double socialRatioThreshold = filter.getThreshold(contributionIsSocial.size());

        //Compute over the last 20 events
        int turns = 0, socialTurns = 0;
        for (int i = contributionIsSocial.size() - 1; i >= 0 && turns <= 20; i--, turns++) {
            if (contributionIsSocial.get(i)) {
                socialTurns++;
            }
        }
        double currentSocialRatio = (turns == 0) ? 0.0 : ((double) socialTurns / (double) turns);

        //Compute over the last 3 events
        int recentSocialTurns = 0;
        for (int i = contributionIsSocial.size() - 3; i < contributionIsSocial.size(); i++) {
            if (i >= 0) {
                if (contributionIsSocial.get(i)) {
                    recentSocialTurns++;
                }
            }
        }

        informObservers("Computing Trigger: Confidence=" + predictedConfidence + " Threshold=" + socialRatioThreshold + " Ratio=" + currentSocialRatio + " RecentTurns=" + recentSocialTurns);

        ModelInfo mi = new ModelInfo(TriggerInfo.TYPE_MODEL_INFO, predictedConfidence, currentSocialRatio, socialRatioThreshold, recentSocialTurns);
        TriggerInfo ti1 = new TriggerInfo(mi);
        
        commitTrigger(ti1);

        if (predictedConfidence >= confidenceThreshold) {
            if (currentSocialRatio < socialRatioThreshold) {
                if (recentSocialTurns <= 0) {
                    PerformanceInfo pi = new PerformanceInfo("", TriggerInfo.TRIGGER_MODEL, predictedConfidence);
                    TriggerInfo ti2 = new TriggerInfo(pi);
                    commitTrigger(ti2);
                    SocialTriggerEvent tigger = new SocialTriggerEvent(source, TriggerInfo.TRIGGER_MODEL, predictedConfidence);
                    source.queueNewEvent(tigger);
                    
                    //this.dispatchEvent(getSocialController(), new SocialTriggerEvent(this, TriggerInfo.TRIGGER_MODEL, predictedConfidence));
                } else {
                    informObservers("Trigger denied because of RecentTurns=" + recentSocialTurns);
                }
            } else {
                informObservers("Trigger denied because of SocialRatio=" + currentSocialRatio);
            }
        } else {
            informObservers("Trigger=" + predictedConfidence + " denied because of ConfidenceThreshold=" + confidenceThreshold);
        }
    }

	private void commitTrigger(TriggerInfo ti1)
	{
		State s = StateMemory.getSharedState(agent);
		s.more().put("TriggerInfo", ti1);
        StateMemory.commitSharedState(s, agent);
	}

    public static String mapStateF09ToModel(String s) {
        if (s.equalsIgnoreCase("START_DESIGN2")) {
            return "HINT_MATERIAL";
        }
        return s;
    }

    private void handleMessageEvent(MessageEvent me, boolean echo) 
    {
        ChatContribution c = new ChatContribution();
        c.time = Timer.currentTimeMillis();
        c.from = me.getFrom();
        c.text = me.getText();

        Message m = new Message(c.from, c.text);
        TriggerInfo ti = new TriggerInfo(m);
        commitTrigger(ti);

        if (startTime < 0) {
            startTime = c.time;
        }

        if (c.text.trim().length() == 0) {
            return;
        }

        System.out.println("FEATURE_DEBUG:   AgentName=" + agentName + " NewMessage: From=" + c.from + " Text=" + c.text);

        WrenchFeatureVector fv = new WrenchFeatureVector(tutorVocab, studentVocab, generalInquirer, semanticAnnotator, discouseStates);
        if (echo) 
        {
            if (!tutorStarted) {
                tutorStarted = true;
                startTime = c.time;
                informObservers("Tutor Started");
            }
            if (performedSocialTurns.contains(c.text.trim())) {
                log(Logger.LOG_LOW, "Received Tutor Turn is Social");
                contributionIsSocial.add(new Boolean(true));
                performedSocialTurns.remove(c.text.trim());
            } else {
                log(Logger.LOG_LOW, "Received Tutor Turn is Not Social");
                contributionIsSocial.add(new Boolean(false));
            }
            chatContributions.add(c);
            for (int k = 1; k < recentTutorTurns.length; k++) 
            {
                recentTutorTurns[k - 1] = recentTutorTurns[k];
            }
            recentTutorTurns[recentTutorTurns.length - 1] = c.text;
        } 
        else 
        {
            chatContributions.add(c);
            contributionIsSocial.add(new Boolean(false));
            //Include the most recent thing the student said
            for (int k = 1; k < recentStudentTurns.length; k++) {
                recentStudentTurns[k - 1] = recentStudentTurns[k];
            }
            recentStudentTurns[recentStudentTurns.length - 1] = c.text;
            System.out.println("FEATURE_DEBUG:   Adding new Message to Student Turns");
            
	        //Lexical, Affect and Semantic Features
	        fv.previousTutorTurns = recentTutorTurns;
	        fv.previousStudentTurns = recentStudentTurns;
	        //Discourse Features
	        fv.previousDiscourseStateLabel = previousDiscourseState;
	        fv.currentDiscourseStateLabel = currentDiscourseState;
	        //Special Purpose Features
	        double highestActivityLevel = 0, lowestActivityLevel = 0;
	        double[] activityLevels = new double[participants.size()];
	        double activitySum = 0.0;
	        if (activityLevels.length > 0 && chatContributions.size() > 0) {
	            for (int k = 0; k < activityLevels.length; k++) {
	                activityLevels[k] = 0.0;
	            }
	            int rj = chatContributions.size() - 1;
	            ChatContribution crj = chatContributions.get(rj);
	            while (rj >= 0) {
	                crj = chatContributions.get(rj);
	                if ((crj.time - c.time) > 300000) { //5 minutes = 300 seconds = 300000 ms
	                    break;
	                }
	                for (int k = 0; k < activityLevels.length; k++) {
	                    if (crj.from.equalsIgnoreCase(participants.get(k))) {
	                        activityLevels[k] += 1.0;
	                        activitySum += 1.0;
	                    }
	                }
	                rj--;
	            }
	            highestActivityLevel = activityLevels[0];
	            lowestActivityLevel = activityLevels[0];
	            for (int k = 1; k < activityLevels.length; k++) {
	                if (activityLevels[k] > highestActivityLevel) {
	                    highestActivityLevel = activityLevels[k];
	                }
	                if (activityLevels[k] < lowestActivityLevel) {
	                    lowestActivityLevel = activityLevels[k];
	                }
	            }
	            double dTime = (double) (crj.time - c.time) / 1000.0; //Milliseconds to seconds
	            if (dTime != 0) {
	                highestActivityLevel = highestActivityLevel / dTime;
	                lowestActivityLevel = lowestActivityLevel / dTime;
	            }
	        }
	        fv.highestActivityLevel = highestActivityLevel;
	        fv.lowestActivityLevel = lowestActivityLevel;
	
	        if (tutorStarted) 
	        {
	            GenericFeatureVector gfv = fv.getGenericFeatureVector();
	            gfv.groupIdentifier = this.agent.getName() + "#" + currentDiscourseState;
	            informObservers("Computing Trigger");
	            computeAndDispatchTrigger(gfv);
	        }
	        informObservers("ChatContributions Size=" + chatContributions.size());
        }

    }



	private void handlePresenceEvent(PresenceEvent pe) {
        if (!pe.getUsername().equalsIgnoreCase(agentName)) {
            if (pe.getType().equals(PresenceEvent.PRESENT)) {
                participants.add(pe.getUsername());
            } else if (pe.getType().equals(PresenceEvent.ABSENT)) {
                participants.remove(pe.getUsername());
            }
        }
    }

    private void handleSocialTurnPerformedEvent(SocialTurnPerformedEvent stpe) 
    {
        String[] turns = stpe.getText().split("\\|");
        for (int i = 0; i < turns.length; i++) 
        {
            performedSocialTurns.add(turns[i].trim());
        }
        informObservers(turns.length + " SocialTurns Enlisted");
    }

	@Override
	public void processEvent(InputCoordinator source, Event event)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		// TODO Auto-generated method stub
		return new Class[0];
	}

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		this.source = source;
		
		 if (e instanceof MessageEvent) 
	        {
	            handleMessageEvent((MessageEvent) e, false);
	        } 
		 	else if (e instanceof EchoEvent) 
	        {
	            handleMessageEvent(((EchoEvent) e).getEvent(), true);
	        } 
	        else if (e instanceof PresenceEvent) 
	        {
	            handlePresenceEvent((PresenceEvent) e);
	        } 
	        else if (e instanceof SocialTurnPerformedEvent) 
	        {
	            handleSocialTurnPerformedEvent((SocialTurnPerformedEvent) e);
	        }
	        else if (e instanceof LaunchEvent)
	        {
	        	if (!tutorStarted) 
	        	{
	                tutorStarted = true;
	                startTime = Timer.currentTimeMillis();
	                informObservers("Tutor Started");
	            }
	        }
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{EchoEvent.class, MessageEvent.class, PresenceEvent.class, SocialTurnPerformedEvent.class, LaunchEvent.class};
	}

//    private void handleStateUpdateEvent(StateUpdateEvent sue) {
//        previousDiscourseState = currentDiscourseState;
//        currentDiscourseState = mapStateF09ToModel(sue.getState());
//
//        StateInfo si = new StateInfo(currentDiscourseState);
//        TriggerInfo ti = new TriggerInfo(si);
//        getTriggerInfoMemory().commit(ti);
//
//        informObservers("StateUpdated: Prev=" + previousDiscourseState + " Current=" + currentDiscourseState);
//    }
}
