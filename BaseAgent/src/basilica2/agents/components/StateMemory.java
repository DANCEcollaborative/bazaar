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
package basilica2.agents.components;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.generics.Memory;
import basilica2.agents.data.State;

/**
 *
 * @author rohitk
 */
public class StateMemory extends Memory<State> {

    public StateMemory(Agent a, String n, String pf) {
        super(a, n, pf);
    }
    
    private static State dummyState = new State();
    private static String stateMemoryName = "stateMemory";
    
    public static State getSharedState(Agent a)
    {
    	System.err.println(">>> StateMemory getSharedState: agent name = " + a.getName());
    	if(!a.getAllConnections().isEmpty())
    	{
	    	State s = ((StateMemory)a.getComponent(stateMemoryName)).retrieve();
	    	if(s == null) {
	    		System.err.println(">>> StateMemory getSharedState: returning new State()! <<<");
	    		return new State();
	    	}
	    	else {
	    		System.err.println(">>> StateMemory getSharedState: returning copy of state <<<");
	    		return State.copy(s);
	    	}
    	}
    	else {
    		System.err.println(">>> StateMemory getSharedState: returning new DUMMY STATE! <<<");
    		return dummyState;
    	}
    }
    
    public static void commitSharedState(State s, Agent a)
    {
    	if(!a.getAllConnections().isEmpty())
    	{
    		((StateMemory)a.getComponent(stateMemoryName)).commit(s);
    	}
    	else {
    		dummyState = s;
        	System.err.println(">>> StateMemory commitSharedState: DUMMY STATE! <<<");
    	}
    }
    
}
