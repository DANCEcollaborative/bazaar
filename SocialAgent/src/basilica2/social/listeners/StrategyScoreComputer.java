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

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.social.data.StrategyScores;
//import basilica2.social.data.TriggerInfo;
//import basilica2.social.events.CheckBlockingEvent;
import basilica2.social.events.DormantGroupEvent;
import basilica2.social.events.DormantStudentEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;

/**
 *
 * @author rohitk
 */
public class StrategyScoreComputer extends BasilicaAdapter 
{

    public static String GENERIC_NAME = "StrategyScoreComputer";
    public static String GENERIC_TYPE = "Computer";
    //private String currentDiscourseState = "__NONE__";
    private String previousDiscourseState = "__NONE__";

    //Strategy Scores
    StrategyScores currentStrategyScores = new StrategyScores();

    public StrategyScoreComputer(Agent a) 
    {
        super(a);
    }

    public StrategyScores getScores() {
        return currentStrategyScores;
    }

    private void handleMessageEvent(MessageEvent me) 
    {
        double scoreStrategy2a = currentStrategyScores.get2aScore();
        double scoreStrategy2b = currentStrategyScores.get2bScore();
        double scoreStrategy2c = currentStrategyScores.get2cScore();
        double scoreStrategy2d = currentStrategyScores.get2dScore();
        double scoreStrategy2e = currentStrategyScores.get2eScore();
        double scoreStrategy2f = currentStrategyScores.get2fScore();
        double scoreStrategy2g = currentStrategyScores.get2gScore();
        double scoreStrategy3a = currentStrategyScores.get3aScore();
        double scoreStrategy3cd = currentStrategyScores.get3cdScore();
        double scoreStrategy4a = currentStrategyScores.get4aScore();
        double scoreStrategy4b = currentStrategyScores.get4bScore();
        double scoreStrategy6 = currentStrategyScores.get6Score();
        double scoreStrategy7 = currentStrategyScores.get7Score();

        String[] givingOrientation = me.checkAnnotation("GIVING_ORIENTATION");
        String[] givingOpinion = me.checkAnnotation("GIVING_OPINION");
        String[] ideaContribution = me.checkAnnotation("IDEA_CONTRIBUTION");
        String[] groupBonding = me.checkAnnotation("GROUP_BONDING");
        String[] abuse = me.checkAnnotation("ABUSE");
        String[] sillyness = me.checkAnnotation("SILLYNESS");
        String[] smiles = me.checkAnnotation("SMILES");
        String[] positivity = me.checkAnnotation("POSITIVITY");
        String[] helpRequest = me.checkAnnotation("HELP_REQUEST");
        String[] discontent = me.checkAnnotation("DISCONTENT");
        String[] teasing = me.checkAnnotation("TEASING");
        String[] tutorReference = me.checkAnnotation("TUTOR_REFERENCE");
        String[] tutorError = me.checkAnnotation("TUTOR_ERROR");

        //If Teasing, Abuse or Sillyness
        if ((teasing != null) || (abuse != null) || (sillyness != null)) {
            //Teasing the tutor?
            if (tutorReference != null) {
                scoreStrategy6 = 1.0;
            } else {
                scoreStrategy2a = 1.0;
            }
        }

        //If discontent or help needed
        if ((discontent != null) || (helpRequest != null)) {
            scoreStrategy2b = 1.0;
        }

        //If smiles or positivity
        if ((smiles != null) || (positivity != null)) {
            scoreStrategy2f = 1.0;
        }

        //If group_bonding
        if (groupBonding != null) {
            scoreStrategy2g = 1.0;
        }

        //If idea contribution
        if (ideaContribution != null) {
            scoreStrategy4a = 1.0;
        }

        //If giving opinion or orientation
        if ((givingOrientation != null) || (givingOpinion != null)) {
            scoreStrategy4b = 1.0;
        }

        if (tutorError != null) {
            scoreStrategy7 = 1.0;
        }

//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"2a\" score=\"" + scoreStrategy2a + "\" />");
//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"2b\" score=\"" + scoreStrategy2b + "\" />");
//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"2f\" score=\"" + scoreStrategy2f + "\" />");
//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"2g\" score=\"" + scoreStrategy2g + "\" />");
//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"4a\" score=\"" + scoreStrategy4a + "\" />");
//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"4b\" score=\"" + scoreStrategy4b + "\" />");
//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"6\" score=\"" + scoreStrategy6 + "\" />");
//        agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"7\" score=\"" + scoreStrategy7 + "\" />");

        currentStrategyScores = new StrategyScores(scoreStrategy2a, scoreStrategy2b, scoreStrategy2c, scoreStrategy2d, scoreStrategy2e, scoreStrategy2f, scoreStrategy2g, scoreStrategy3a, scoreStrategy3cd, scoreStrategy4a, scoreStrategy4b, scoreStrategy6, scoreStrategy7, currentStrategyScores.hasStateChanged(), currentStrategyScores.isStudentDormant(), currentStrategyScores.isGroupDormant(), true);
        commitStrategyScores();
    }

//    private void handleCheckBlockingEvent(CheckBlockingEvent cbe) {
//        double scoreStrategy2a = currentStrategyScores.get2aScore();
//        double scoreStrategy2b = currentStrategyScores.get2bScore();
//        double scoreStrategy2c = currentStrategyScores.get2cScore();
//        double scoreStrategy2d = currentStrategyScores.get2dScore();
//        double scoreStrategy2e = currentStrategyScores.get2eScore();
//        double scoreStrategy2f = currentStrategyScores.get2fScore();
//        double scoreStrategy2g = currentStrategyScores.get2gScore();
//        double scoreStrategy3a = currentStrategyScores.get3aScore();
//        double scoreStrategy3cd = currentStrategyScores.get3cdScore();
//        double scoreStrategy4a = currentStrategyScores.get4aScore();
//        double scoreStrategy4b = currentStrategyScores.get4bScore();
//        double scoreStrategy6 = currentStrategyScores.get6Score();
//        double scoreStrategy7 = currentStrategyScores.get7Score();
//
//        previousDiscourseState = ModelBasedTriggerComputer.mapStateF09ToModel(cbe.getDoneStep());
//
//        if (previousDiscourseState.equalsIgnoreCase("DO_CALCULATIONS1_CONFIRM_TORQUE")) {
//            scoreStrategy3a = 1.0;
//        } else if (previousDiscourseState.equalsIgnoreCase("DO_CALCULATIONS1_CONFIRM_COST_PRICE")) {
//            scoreStrategy3a = 1.0;
//        } else if (previousDiscourseState.equalsIgnoreCase("DO_CALCULATIONS2_CONFIRM_COST")) {
//            scoreStrategy3a = 1.0;
//        } else if (previousDiscourseState.equalsIgnoreCase("REVIEW_DESIGN2")) {
//            scoreStrategy3cd = 1.0;
//        } else if (previousDiscourseState.startsWith("CONCEPT")) {
//            scoreStrategy2c = 1.0;
//        }
//
//        log(Logger.LOG_NORMAL, "<strategy id=\"3a\" score=\"" + scoreStrategy3a + "\" />");
//        log(Logger.LOG_NORMAL, "<strategy id=\"3cd\" score=\"" + scoreStrategy3cd + "\" />");
//        log(Logger.LOG_NORMAL, "<strategy id=\"2c\" score=\"" + scoreStrategy2c + "\" />");
//
//        currentStrategyScores = new StrategyScores(scoreStrategy2a, scoreStrategy2b, scoreStrategy2c, scoreStrategy2d, scoreStrategy2e, scoreStrategy2f, scoreStrategy2g, scoreStrategy3a, scoreStrategy3cd, scoreStrategy4a, scoreStrategy4b, scoreStrategy6, scoreStrategy7, true, currentStrategyScores.isStudentDormant(), currentStrategyScores.isGroupDormant(), currentStrategyScores.isNewMessage());
//        commitStrategyScores();
//    }

    private void handleDormantGroupEvent(DormantGroupEvent dge) {
        double scoreStrategy2a = currentStrategyScores.get2aScore();
        double scoreStrategy2b = currentStrategyScores.get2bScore();
        double scoreStrategy2c = currentStrategyScores.get2cScore();
        double scoreStrategy2d = currentStrategyScores.get2dScore();
        double scoreStrategy2e = currentStrategyScores.get2eScore();
        double scoreStrategy2f = currentStrategyScores.get2fScore();
        double scoreStrategy2g = currentStrategyScores.get2gScore();
        double scoreStrategy3a = currentStrategyScores.get3aScore();
        double scoreStrategy3cd = currentStrategyScores.get3cdScore();
        double scoreStrategy4a = currentStrategyScores.get4aScore();
        double scoreStrategy4b = currentStrategyScores.get4bScore();
        double scoreStrategy6 = currentStrategyScores.get6Score();
        double scoreStrategy7 = currentStrategyScores.get7Score();

        scoreStrategy2e = 1.0; //Wait till task block is lifted
        //agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"2e\" score=\"" + scoreStrategy2e + "\" />");

        currentStrategyScores = new StrategyScores(scoreStrategy2a, scoreStrategy2b, scoreStrategy2c, scoreStrategy2d, scoreStrategy2e, scoreStrategy2f, scoreStrategy2g, scoreStrategy3a, scoreStrategy3cd, scoreStrategy4a, scoreStrategy4b, scoreStrategy6, scoreStrategy7, currentStrategyScores.hasStateChanged(), currentStrategyScores.isStudentDormant(), true, currentStrategyScores.isNewMessage());
        commitStrategyScores();
    }

    private void handleDormantStudentEvent(DormantStudentEvent dse) {
        double scoreStrategy2a = currentStrategyScores.get2aScore();
        double scoreStrategy2b = currentStrategyScores.get2bScore();
        double scoreStrategy2c = currentStrategyScores.get2cScore();
        double scoreStrategy2d = currentStrategyScores.get2dScore();
        double scoreStrategy2e = currentStrategyScores.get2eScore();
        double scoreStrategy2f = currentStrategyScores.get2fScore();
        double scoreStrategy2g = currentStrategyScores.get2gScore();
        double scoreStrategy3a = currentStrategyScores.get3aScore();
        double scoreStrategy3cd = currentStrategyScores.get3cdScore();
        double scoreStrategy4a = currentStrategyScores.get4aScore();
        double scoreStrategy4b = currentStrategyScores.get4bScore();
        double scoreStrategy6 = currentStrategyScores.get6Score();
        double scoreStrategy7 = currentStrategyScores.get7Score();

        scoreStrategy2d = 1.0; //Wait till task block is lifted
        //agent.log("StrategyScoreComputer", Logger.LOG_NORMAL, "<strategy id=\"2d\" score=\"" + scoreStrategy2d + "\" />");

        currentStrategyScores = new StrategyScores(scoreStrategy2a, scoreStrategy2b, scoreStrategy2c, scoreStrategy2d, scoreStrategy2e, scoreStrategy2f, scoreStrategy2g, scoreStrategy3a, scoreStrategy3cd, scoreStrategy4a, scoreStrategy4b, scoreStrategy6, scoreStrategy7, currentStrategyScores.hasStateChanged(), true, currentStrategyScores.isGroupDormant(), currentStrategyScores.isNewMessage());
        commitStrategyScores();
    }

    private void commitStrategyScores() {
//        TriggerInfo ti = new TriggerInfo(currentStrategyScores);
//        TriggerInfoMemory tim = (TriggerInfoMemory) this.getAgent().getComponent("myTriggerInfoMemory");
//        tim.commit(ti);
        
        State s = StateMemory.getSharedState(getAgent());
        s.more().put("StrategyScores", currentStrategyScores);
        StateMemory.commitSharedState(s, getAgent());
        
    }

    @Override
    public void processEvent(InputCoordinator source, Event event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void preProcessEvent(InputCoordinator source, Event e) 
    {
        if (e instanceof MessageEvent) 
        {
            handleMessageEvent((MessageEvent) e);
        }
        else if (e instanceof DormantStudentEvent) 
        {
            handleDormantStudentEvent((DormantStudentEvent) e);
        } 
        else if (e instanceof DormantGroupEvent) 
        {
            handleDormantGroupEvent((DormantGroupEvent) e);
        }
    }

    public Class[] getPreprocessorEventClasses() 
    {
        return new Class[]{MessageEvent.class, DormantStudentEvent.class, DormantGroupEvent.class};
    }

    public Class[] getListenerEventClasses() 
    {
        return new Class[]{};
    }
}
