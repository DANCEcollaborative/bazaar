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
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class MultiStateMIRAModel {

    String[] modelIdentifiers;
    SimpleMIRAModel[] simpleModels;
    private String shell;

    public MultiStateMIRAModel(String filename) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        int nModels = Integer.parseInt(in.readLine().trim());
        modelIdentifiers = new String[nModels];
        simpleModels = new SimpleMIRAModel[nModels];
        for (int j = 0; j < nModels; j++) {
            String modelIdentifier = in.readLine().trim();
            shell = in.readLine().trim();
            double w0 = Double.parseDouble(in.readLine().trim());
            int n = Integer.parseInt(in.readLine().trim());
            String[] featureIdentifiers = new String[n];
            double[] weights = new double[n + 1];
            weights[0] = w0;
            for (int i = 0; i < n; i++) {
                String line = in.readLine().trim();
                String[] tokens = line.split("\t");
                featureIdentifiers[i] = tokens[0];
                weights[i + 1] = Double.parseDouble(tokens[1]);
            }
            SimpleMIRAModel model = new SimpleMIRAModel(modelIdentifier, featureIdentifiers, shell);
            model.updateWeights(weights);
            modelIdentifiers[j] = modelIdentifier;
            simpleModels[j] = model;
        }
        in.close();
    }

    public MultiStateMIRAModel(String[] states, String[] featureIds, String shell) {
        this.shell = shell;
        modelIdentifiers = new String[states.length];
        simpleModels = new SimpleMIRAModel[states.length];
        for (int j = 0; j < states.length; j++) {
            SimpleMIRAModel model = new SimpleMIRAModel(states[j], featureIds, shell);
            modelIdentifiers[j] = states[j];
            simpleModels[j] = model;
        }
    }

    public SimpleMIRAModel getSimpleMIRAModel(String state) {
        for (int i = 0; i < modelIdentifiers.length; i++) {
            if (modelIdentifiers[i].equals(state)) {
                return simpleModels[i];
            }
        }
        return null;
    }

    public void setWeights(String modelId, double[] weights) {
        SimpleMIRAModel model = getSimpleMIRAModel(modelId);
        if (model != null) {
            model.updateWeights(weights);
        }
    }

    private double getAlpha(int indexDifference) {
        if (indexDifference == 0) {
            throw new IllegalStateException("Index Difference cannot be " + indexDifference);
        } else if (indexDifference == 1) {
            return 0.3;
        } else if (indexDifference == 2) {
            return 0.1;
//        } else if (indexDifference == 3) {
//            return 0.08;
        } else {
            return 0.0;
        }
    }

    public double[] applyToSequence(List<GenericFeatureVector> sequence) {
        double[] predictedConfidences = new double[sequence.size()];
        String currentState = "__NONE__";
        String previousState = "__NONE__";
        String immediatelyPreviousState = "__NONE__";
        int lastPreviousStateIndex = -1;
        for (int sequenceIterator = 0; sequenceIterator < sequence.size(); sequenceIterator++) {
            GenericFeatureVector gfv = sequence.get(sequenceIterator);
            currentState = gfv.descriptors[4];

            if (!immediatelyPreviousState.equals(currentState)) {
                previousState = immediatelyPreviousState;
                lastPreviousStateIndex = sequenceIterator - 1;
            }

            //System.out.println("CurrentIndex: " + sequenceIterator + " CurrentState: " + currentState + " ImmediatelyPreviousState: " + immediatelyPreviousState + " PreviousState: " + previousState + " IndexOfPreviousState: " + lastPreviousStateIndex);

            SimpleMIRAModel currentStateModel = getSimpleMIRAModel(currentState);
            SimpleMIRAModel previousStateModel = getSimpleMIRAModel(previousState);

            if (currentStateModel == null) {
                predictedConfidences[sequenceIterator] = 0.0;
                continue;
            }

            double alpha = 0.0;
            if (lastPreviousStateIndex != -1) {
                alpha = getAlpha(sequenceIterator - lastPreviousStateIndex);
            }

            double currentStateModelPrediction = computeLinearPredictionWithModel(gfv, currentStateModel);
            double previousStateModelPrediction = 0.0;
            if (previousStateModel != null) {
                previousStateModelPrediction = computeLinearPredictionWithModel(gfv, previousStateModel);
            }

            predictedConfidences[sequenceIterator] = ((1.0 - alpha) * currentStateModelPrediction) + (alpha * previousStateModelPrediction);

            if (shell.equals(SimpleMIRAModel.LOGIT_SHELL)) {
                predictedConfidences[sequenceIterator] = 1.0 / (1.0 + Math.exp(-1 * predictedConfidences[sequenceIterator]));
            }

            immediatelyPreviousState = currentState;
        }
        return predictedConfidences;
    }

    private double computeLinearPredictionWithModel(GenericFeatureVector fv, SimpleMIRAModel model) {
        double[] weights = model.getWeights();
        //Uncomment this for deployement: String[] featuresIds = model.getFeatureIdentifiers();
        double ret = weights[0];
        for (int i = 1; i < weights.length; i++) {
            //ret += (fv.getValueFor(featuresIds[i - 1]) * weights[i]); //Use this for deployment
            ret += (fv.featureValues[i - 1] * weights[i]); //Use this for speed (Training)
        }
        return ret;
    }

    public void writeTo(String filename) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        out.write(modelIdentifiers.length + "\n");
        for (int i = 0; i < modelIdentifiers.length; i++) {
            SimpleMIRAModel m = simpleModels[i];
            out.write(m.getIdentifier() + "\n");
            out.write(m.getShell() + "\n");
            double[] modelWeights = m.getWeights();
            String[] modeFIs = m.getFeatureIdentifiers();
            out.write(modelWeights[0] + "\n");
            out.write(modeFIs.length + "\n");
            for (int j = 0; j < modeFIs.length; j++) {
                out.write(modeFIs[j] + "\t" + modelWeights[j + 1] + "\n");
            }
        }
        out.close();
    }
}
