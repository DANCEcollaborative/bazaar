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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class WrenchFeatureVector {

    private Vocab tutorVocab,  studentVocab;
    private GeneralInquirer generalInquirer;
    private DBAnnotator semanticAnnotator;
    private String[] discourseStates;
    //Feature Information
    public String[] previousTutorTurns;
    public String[] previousStudentTurns;
    public String previousDiscourseStateLabel,  currentDiscourseStateLabel;
    public double highestActivityLevel,  lowestActivityLevel;

    public WrenchFeatureVector(Vocab tutorVocab, Vocab studentVocab, GeneralInquirer generalInquirer, DBAnnotator semanticAnnotator, String[] discourseStates) {
        this.tutorVocab = tutorVocab;
        this.studentVocab = studentVocab;
        this.generalInquirer = generalInquirer;
        this.semanticAnnotator = semanticAnnotator;
        this.discourseStates = discourseStates;
    }

    private boolean[] stateLabelAsBinaryFeatures(String stateLabel) {
        boolean[] stateBinaryFeatures = new boolean[discourseStates.length];
        int count = 0;
        for (int j = 0; j < discourseStates.length; j++) {
            if (discourseStates[j].equals(stateLabel)) {
                stateBinaryFeatures[j] = true;
                count++;
            } else {
                stateBinaryFeatures[j] = false;
            }
        }
        if (count == 0) {
            System.err.println("No DiscourseState Applicable?! < Thats weird");
        }
        return stateBinaryFeatures;
    }

    public GenericFeatureVector getGenericFeatureVector() {
        List<String> featureIds = new ArrayList<String>();
        List<Double> featureValues = new ArrayList<Double>();

        String studentTurnsConcatenated = "", tutorTurnsConcatenated = "";
        for (int i = 0; i < previousStudentTurns.length; i++) {
            System.out.println("FEATURE_DEBUG: StudentTurns[" + i + "]=" + previousStudentTurns[i]);
            studentTurnsConcatenated += previousStudentTurns[i].trim() + " ";
        }
        for (int i = 0; i < previousTutorTurns.length; i++) {
            System.out.println("FEATURE_DEBUG:   TutorTurns[" + i + "]=" + previousTutorTurns[i]);
            tutorTurnsConcatenated += previousTutorTurns[i].trim() + " ";
        }
        System.out.println("FEATURE_DEBUG:   PreviousDS=" + previousDiscourseStateLabel);
        System.out.println("FEATURE_DEBUG:    CurrentDS=" + currentDiscourseStateLabel);
        System.out.println("FEATURE_DEBUG:   HighestAct=" + highestActivityLevel);
        System.out.println("FEATURE_DEBUG:   LowestAct=" + lowestActivityLevel);

        studentTurnsConcatenated = normalize(studentTurnsConcatenated);
        tutorTurnsConcatenated = normalize(tutorTurnsConcatenated);

        //Lexical Features
        int[] previousTutorTurnFeatures = tutorVocab.string2WordVector(tutorTurnsConcatenated);
        int[] previousStudentTurnFeatures = studentVocab.string2WordVector(studentTurnsConcatenated);
        for (int i = 0; i < previousTutorTurnFeatures.length; i++) {
            featureIds.add("lTT" + i);
            featureValues.add(new Double((double) previousTutorTurnFeatures[i]));
        }
        for (int i = 0; i < previousStudentTurnFeatures.length; i++) {
            featureIds.add("lST" + i);
            featureValues.add(new Double((double) previousStudentTurnFeatures[i]));
        }
        //Affect Features
        int[] affectFeatures = generalInquirer.getGIIndices(studentTurnsConcatenated);
        for (int i = 0; i < affectFeatures.length; i++) {
            featureIds.add("aST" + i);
            featureValues.add(new Double((double) affectFeatures[i]));
        }
        //Semantic Features
        double[] semanticFeatures = semanticAnnotator.getSemanticFeatures(studentTurnsConcatenated);
        for (int i = 0; i < semanticFeatures.length; i++) {
            featureIds.add("sST" + i);
            featureValues.add(new Double(semanticFeatures[i]));
        }
        //Discourse Features
        boolean[] previousDiscourseStateFeatures = stateLabelAsBinaryFeatures(previousDiscourseStateLabel);
        boolean[] currentDiscourseStateFeatures = stateLabelAsBinaryFeatures(currentDiscourseStateLabel);
        for (int i = 0; i < previousDiscourseStateFeatures.length; i++) {
            featureIds.add("dsP" + i);
            featureValues.add((previousDiscourseStateFeatures[i] ? 1.0 : 0.0));
        }
        for (int i = 0; i < currentDiscourseStateFeatures.length; i++) {
            featureIds.add("dsC" + i);
            featureValues.add((currentDiscourseStateFeatures[i] ? 1.0 : 0.0));
        }
        featureIds.add("dsCh");
        featureValues.add((previousDiscourseStateLabel.equalsIgnoreCase(currentDiscourseStateLabel)) ? 0.0 : 1.0);
        //Special Purpose Features
        featureIds.add("spHA");
        featureValues.add(highestActivityLevel);
        featureIds.add("spLA");
        featureValues.add(lowestActivityLevel);
        double rangeActivityLevel = highestActivityLevel - lowestActivityLevel;
        featureIds.add("spRA");
        featureValues.add(rangeActivityLevel);
        double normalizedRangeActivityLevel = (highestActivityLevel != 0) ? (rangeActivityLevel / highestActivityLevel) : 0.0;
        featureIds.add("spNRA");
        featureValues.add(normalizedRangeActivityLevel);

        GenericFeatureVector fv = new GenericFeatureVector();
        if (featureIds.size() != featureValues.size()) {
            System.err.println("ERROR: Feature Values and Identifiers count mismatch");
        }
        fv.featureIdentifiers = featureIds.toArray(new String[0]);
        fv.featureValues = new double[featureValues.size()];
        for (int i = 0; i < featureValues.size(); i++) {
            fv.featureValues[i] = featureValues.get(i).doubleValue();
        }
        return fv;
    }

    public static String normalize(String text) {
        String ntext = text.toLowerCase().trim();
        ntext = ntext.replaceAll("[^\\p{Alnum}]", " ");
        ntext = ntext.replace("  ", " ").replace("  ", " ").replace("  ", " ").replace("  ", " ").replace("  ", " ");
        return ntext;
    }
}
