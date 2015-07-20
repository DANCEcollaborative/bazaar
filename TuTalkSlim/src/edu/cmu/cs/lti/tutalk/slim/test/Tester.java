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
package edu.cmu.cs.lti.tutalk.slim.test;

import edu.cmu.cs.lti.tutalk.script.Concept;
import edu.cmu.cs.lti.tutalk.script.DictionaryConcept;
import edu.cmu.cs.lti.tutalk.script.Response;
import edu.cmu.cs.lti.tutalk.script.Scenario;
import edu.cmu.cs.lti.tutalk.slim.EvaluatedConcept;
import edu.cmu.cs.lti.tutalk.slim.FuzzyTurnEvaluator;
import edu.cmu.cs.lti.tutalk.slim.TuTalkAutomata;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 *
 * @author rohitk
 */
public class Tester {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Scanner in = new Scanner(System.in);
        Scenario sc = Scenario.loadScenario("dialogs/simple_SIDE_example.xml");
        TuTalkAutomata auto = new TuTalkAutomata("tutor", "group");
        auto.setScenario(sc);
        auto.setEvaluator(new FuzzyTurnEvaluator());

        // Test replacement variables
        auto.addReplacementVariable("lol", "Laughing out Loud");

        List<String> turns = auto.start();
        while (true) 
        {
            System.out.println("=============================");
            for (int i = 0; i < turns.size(); i++) {
                System.out.println("Tutor: " + turns.get(i));
            }
            System.out.println("=============================");
            List<Response> expected = auto.getState().getExpected();
            if (expected.isEmpty()) {
                break;
            }
            System.out.println("=============================");
            for (int i = 0; i < expected.size(); i++) 
            {
                final Concept concept = expected.get(i).getConcept();
                if(concept instanceof DictionaryConcept)
                {
                    List<String> phrases = ((DictionaryConcept)concept).getPhrases();
                    for (int j = 0; j < phrases.size(); j++) 
                    {
                        System.out.println("Valid: " + phrases.get(j));
                    }
                }
                else System.out.println("Matching: "+concept.toString());
            }
            System.out.println("=============================");
            List<EvaluatedConcept> matchingConcepts = new ArrayList<EvaluatedConcept>();
            while (matchingConcepts.size() == 0) 
            {
                //Get Input
                String input = in.nextLine();
                System.out.println("Evaluating: " + input.trim());
                matchingConcepts = auto.evaluateTuteeTurn(input, new ArrayList<String>(0));
            }
            System.out.println("Matched Concept " + matchingConcepts.get(0).concept.getLabel());
            turns = auto.progress(matchingConcepts.get(0).concept);
        }
    }
}
