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
package edu.cmu.cs.lti.tutalk.script;

import edu.cmu.cs.lti.tutalk.slim.ExecutionState;

/**
 *
 * @author rohitk
 */
public class SubGoalStep extends Step {

    private Goal subGoal;

    public SubGoalStep(Goal sg) {
        subGoal = sg;
    }

    public Goal getSubGoal() {
        return subGoal;
    }

    @Override
    public Concept execute(ExecutionState state) {
        if(subGoal == null)
        {
        	System.out.println(this + " subgoal is null");
        	return null;
        }
    	if (!isDone()) {

            state.log("step.subgoal." + subGoal.getName(), "Executing ... ", false);
            state.push(subGoal);

            Concept ret = subGoal.execute(state);
            if (subGoal.isDone()) {
                done = true;
                state.pop();
                state.addDone(this);
                state.log("step.subgoal." + subGoal.getName(), "Done", true);
            }
            return ret;
        }
        return null;
    }

    @Override
    public Concept execute(Concept responseConcept, ExecutionState state) {
        if (!isDone()) 
        {
        	if(subGoal == null)
        		return null;
        	
            state.log("step.subgoal." + subGoal.getName(), "Executing w/Response ... ", false);
            state.push(subGoal);

            Concept ret = subGoal.execute(responseConcept, state);
            if (subGoal.isDone()) {
                done = true;
                state.addDone(this);
                state.log("step.subgoal." + subGoal.getName(), "Done", true);
            }
            return ret;
        }
        return null;
    }
}
