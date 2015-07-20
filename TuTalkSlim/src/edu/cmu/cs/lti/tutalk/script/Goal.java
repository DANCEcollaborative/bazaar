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
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author rohitk
 */
public class Goal extends Executable
{

	private String myName;
	private List<Step> mySteps;
	private int currentStep = 0;

	public Goal(String n)
	{
		myName = n;
		mySteps = new ArrayList<Step>();
	}

	public String getName()
	{
		return myName;
	}

	public void addStep(Step s)
	{
		mySteps.add(s);
	}

	public Step getStep()
	{
		if (mySteps.size() > currentStep) { return mySteps.get(currentStep++); }

		return null;
	}

	@Override
	public Concept execute(ExecutionState state)
	{
		if (!isDone())
		{

			state.log("goal." + myName, "Executing step " + currentStep + " ...", false);

			Step s = mySteps.get(currentStep);
			Concept ret = s.execute(state);

			if (s.isDone())
			{
				currentStep++;
			}

			if (currentStep >= mySteps.size())
			{
				done = true;
				state.pop();
				state.log("goal." + myName, "Done", true);
			}

			return ret;
		}
		else
		{
			state.log("goal."+myName, "should I be done?", false);
//			currentStep = 0;
//			done = false;
			state.pop();
		}
		return null;
	}

	@Override
	public Concept execute(Concept responseConcept, ExecutionState state)
	{
		if (!isDone())
		{

			state.log("goal." + myName, "Executing w/Response step=" + currentStep + " ...", false);

			Step s = mySteps.get(currentStep);
			Concept ret = s.execute(responseConcept, state);

			if (s.isDone())
			{
				currentStep++;
			}

			if (currentStep >= mySteps.size())
			{
				done = true;
				state.pop();
				state.log("goal." + myName, "Done", true);
			}

			return ret;
		}
		return null;
	}

	public boolean isInProgress()
	{
		return currentStep > 0;
	}
}
