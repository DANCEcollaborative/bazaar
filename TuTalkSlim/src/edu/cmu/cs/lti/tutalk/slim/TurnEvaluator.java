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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class TurnEvaluator 
{
    protected double MATCH_THRESHOLD = 0.5;

    
    
    public List<EvaluatedConcept> evaluateTurn(String turn, List<Concept> validConcepts, Collection<String> annotations) 
    {
        List<EvaluatedConcept> matched = new ArrayList<EvaluatedConcept>();

        if (turn.trim().length() == 0) 
        {
            return matched;
        }

        boolean unAnticipatedValid = false;
        Concept unAnticipatedConcept = null;

        for (Concept concept : validConcepts) 
        {
            double matchValue = concept.match(turn, annotations);

			if(matchValue > MATCH_THRESHOLD)
            {
                matched.add(new EvaluatedConcept(concept, matchValue));
            }
			
            if (concept.getLabel().equals("unanticipated-response")) 
            {
                unAnticipatedValid = true;
                unAnticipatedConcept = concept;
            }
        }

        if (unAnticipatedValid && matched.isEmpty()) 
        {
            matched.add(new EvaluatedConcept(unAnticipatedConcept, 1.0));
        }
        
        Collections.sort(matched);
        Collections.reverse(matched);
        
        System.out.println(matched);

        return matched;
    }
}
