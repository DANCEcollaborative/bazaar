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
package edu.cmu.cs.lti.tutalk.slim;

import edu.cmu.cs.lti.tutalk.script.Concept;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class FuzzyTurnEvaluator extends TurnEvaluator 
{

	public FuzzyTurnEvaluator()
	{
		MATCH_THRESHOLD = 0.0;
	}
    /*@Override
    public List<EvaluatedConcept> evaluateTurn(String turn, List<Concept> validConcepts) 
    {
        List<EvaluatedConcept> matched = new ArrayList<EvaluatedConcept>();
        List<Double> scores = new ArrayList<Double>();

        if (turn.trim().length() == 0) {
            return matched;
        }

        boolean unAnticipatedValid = false;
        Concept unAnticipatedConcept = null;

        for (Concept concept : validConcepts) 
        {
            if (concept.getLabel().equals("unanticipated-response")) 
            {
                //System.out.println("Unanticipated Response is Valid");
                unAnticipatedValid = true;
                unAnticipatedConcept = concept;
                scores.add(0.0);
                System.out.println("Score for " + concept.getLabel() + " is " + 0);
                continue;
            }

            else
            {
                double score = concept.match(turn);

                System.out.println("Score for " + concept.getLabel() + " is " + score);
                scores.add(score);
            }
        }

        //Sort
        for (int i = 0; i < validConcepts.size(); i++) {
            for (int j = 0; j < validConcepts.size(); j++) {
                if (scores.get(i) > scores.get(j)) {
                    Concept tc = validConcepts.get(i);
                    double ts = scores.get(i);
                    validConcepts.set(i, validConcepts.get(j));
                    scores.set(i, scores.get(j));
                    validConcepts.set(j, tc);
                    scores.set(j, ts);
                }
            }
        }

        for (int i = 0; i < validConcepts.size(); i++) 
        {
            if (scores.get(i) > 0) 
            {
                matched.add(validConcepts.get(i));
                //System.out.println("Matched: " + validConcepts.get(i).getLabel() + "=" + scores.get(i));
            }
        }


        if (unAnticipatedValid) 
        {
            if (matched.isEmpty()) 
            {
                matched.add(unAnticipatedConcept);
            }
        }

        return matched;
    }*/
}
