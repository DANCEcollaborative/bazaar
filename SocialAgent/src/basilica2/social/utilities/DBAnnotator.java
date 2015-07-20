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
package basilica2.social.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class DBAnnotator {

    private String directory;
    private List<String> namesDictionary,  affirmativeDictionary,  negativeDictionary;
    private List<String> tutorRefDictionary,  torqueCalculationDict,  costCalculationDict;
    private List<String> teasingDict,  discontentDict,  helpRequestDict,  positivityDict,  smilesDict;
    private List<String> groupBondingDict,  ideaContributionDict,  givingOpinionDict;
    private List<String> givingOrientationDict,  sillynessDict,  abuseDict;

    public DBAnnotator(String directory) {
        this.directory = directory;
        namesDictionary = loadDictionary("giving_name.txt");
        affirmativeDictionary = loadDictionary("affirmative.txt");
        negativeDictionary = loadDictionary("negative.txt");
        tutorRefDictionary = loadDictionary("tutor_reference.txt");
        torqueCalculationDict = loadDictionary("torque_calculation.txt");
        costCalculationDict = loadDictionary("cost_calculation.txt");
        teasingDict = loadDictionary("teasing.txt");
        discontentDict = loadDictionary("discontent.txt");
        helpRequestDict = loadDictionary("help_request.txt");
        positivityDict = loadDictionary("positivity.txt");
        smilesDict = loadDictionary("smiles.txt");
        abuseDict = loadDictionary("abuse.txt");
        sillynessDict = loadDictionary("sillyness.txt");
        groupBondingDict = loadDictionary("group_bonding.txt");
        ideaContributionDict = loadDictionary("idea_contribution.txt");
        givingOpinionDict = loadDictionary("giving_opinion.txt");
        givingOrientationDict = loadDictionary("giving_orientation.txt");
    }

    private List<String> loadDictionary(String filename) {
        List<String> dictionary = new ArrayList<String>();
        try {
            BufferedReader fr = new BufferedReader(new FileReader(this.directory + File.separator + "dictionaries" + File.separator + filename));

            String line = fr.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0) {
                    dictionary.add(line.trim());
                }
                line = fr.readLine();
            }
            fr.close();
        } catch (Exception e) {
            System.err.println("Error while reading Dictionary: " + filename + " (" + e.toString() + ")");
        }
        System.out.println("Dictionary Loaded: " + filename);
        return dictionary;
    }

    private List<String> matchDictionary(String text, List<String> dictionary) {
        text = " " + text;
        List<String> matchedTerms = new ArrayList<String>();
        for (int j = 0; j < dictionary.size(); j++) {
            if (text.contains(" " + dictionary.get(j))) {
                matchedTerms.add(dictionary.get(j));
            }
        }
        return matchedTerms;
    }

    public List<String> getAnnotations(String text) {
        String normalizedText = normalize(text);
        List<String> annotations = new ArrayList<String>();

        //GIVING_NAME
        List<String> namesFound = matchDictionary(normalizedText, namesDictionary);
        if (namesFound.size() > 0) {
            annotations.add("GIVING_NAME");
        }

        //AFFIRMATIVE
        List<String> yesFound = matchDictionary(normalizedText, affirmativeDictionary);
        if (yesFound.size() > 0) {
            annotations.add("AFFIRMATIVE");
        }

        //NEGATIVE
        List<String> noFound = matchDictionary(normalizedText, negativeDictionary);
        if (noFound.size() > 0) {
            annotations.add("NEGATIVE");
        }

        //TUTOR_REFERENCE
        List<String> trefFound = matchDictionary(normalizedText, tutorRefDictionary);
        if (trefFound.size() > 0) {
            annotations.add("TUTOR_REFERENCE");
        }

        //TORQUE_CALCULATIONS
        List<String> tCalcFound = matchDictionary(normalizedText, torqueCalculationDict);
        if (tCalcFound.size() > 0) {
            annotations.add("TORQUE_CALCULATIONS");
        }

        //COST_CALCULATIONS
        List<String> cCalcFound = matchDictionary(normalizedText, costCalculationDict);
        if (cCalcFound.size() > 0) {
            annotations.add("COST_CALCULATIONS");
        }

        //TEASING
        List<String> teasingFound = matchDictionary(normalizedText, teasingDict);
        if (teasingFound.size() > 0) {
            annotations.add("TEASING");
        }

        //DISCONTENT
        List<String> discontentFound = matchDictionary(normalizedText, discontentDict);
        if (discontentFound.size() > 0) {
            annotations.add("DISCONTENT");
        }

        //HELP_REQUEST
        List<String> hrFound = matchDictionary(normalizedText, helpRequestDict);
        if (hrFound.size() > 0) {
            annotations.add("HELP_REQUEST");
        }

        //POSITIVITY
        List<String> positivityFound = matchDictionary(text, positivityDict);
        if (positivityFound.size() > 0) {
            annotations.add("POSITIVITY");
        }

        //SMILES
        List<String> smilesFound = matchDictionary(text, smilesDict);
        if (smilesFound.size() > 0) {
            annotations.add("SMILES");
        }

        //SILLYNESS
        List<String> sillyFound = matchDictionary(normalizedText, sillynessDict);
        if (sillyFound.size() > 0) {
            annotations.add("SILLYNESS");
        }

        //ABUSE
        List<String> abuseFound = matchDictionary(text, abuseDict);
        if (abuseFound.size() > 0) {
            annotations.add("ABUSE");
        }

        //GROUP_BONDING
        List<String> gbFound = matchDictionary(normalizedText, groupBondingDict);
        if (gbFound.size() > 0) {
            annotations.add("GROUP_BONDING");
        }

        //IDEA_CONTRIBUTION
        List<String> ideaFound = matchDictionary(normalizedText, ideaContributionDict);
        if (ideaFound.size() > 0) {
            annotations.add("IDEA_CONTRIBUTION");
        }

        //GIVING_OPINION
        List<String> opinionFound = matchDictionary(normalizedText, givingOpinionDict);
        if (opinionFound.size() > 0) {
            annotations.add("GIVING_OPINION");
        }

        //GIVING_ORIENTATION
        List<String> orientFound = matchDictionary(normalizedText, givingOrientationDict);
        if (orientFound.size() > 0) {
            annotations.add("GIVING_ORIENTATION");
        }

        return annotations;
    }

    public double[] getSemanticFeatures(String t) {
        double[] semanticFeatures = new double[13];
        semanticFeatures[0] = getAnnotations(t).contains("GIVING_ORIENTATION") ? 1.0 : 0.0;
        semanticFeatures[1] = getAnnotations(t).contains("GIVING_OPINION") ? 1.0 : 0.0;
        semanticFeatures[2] = getAnnotations(t).contains("IDEA_CONTRIBUTION") ? 1.0 : 0.0;
        semanticFeatures[3] = getAnnotations(t).contains("GROUP_BONDING") ? 1.0 : 0.0;
        semanticFeatures[4] = getAnnotations(t).contains("ABUSE") ? 1.0 : 0.0;
        semanticFeatures[5] = getAnnotations(t).contains("SILLYNESS") ? 1.0 : 0.0;
        semanticFeatures[6] = getAnnotations(t).contains("SMILES") ? 1.0 : 0.0;
        semanticFeatures[7] = getAnnotations(t).contains("POSITIVITY") ? 1.0 : 0.0;
        semanticFeatures[8] = getAnnotations(t).contains("HELP_REQUEST") ? 1.0 : 0.0;
        semanticFeatures[9] = getAnnotations(t).contains("DISCONTENT") ? 1.0 : 0.0;
        semanticFeatures[10] = getAnnotations(t).contains("TEASING") ? 1.0 : 0.0;
        semanticFeatures[11] = getAnnotations(t).contains("TUTOR_REFERENCE") ? 1.0 : 0.0;
        semanticFeatures[12] = getAnnotations(t).contains("TUTOR_ERROR") ? 1.0 : 0.0;
        return semanticFeatures;
    }

    private String normalize(String text) {
        text = text.replace(",", " ");
        text = text.replace(".", " ");
        text = text.replace("?", " ");
        text = text.replace("!", " ");
        text = text.replace("\'", "");
        text = text.trim();
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.replace("  ", " ");
        text = text.toLowerCase();
        return text;
    }
}
