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

/**
 *
 * @author rohitk
 */
public class SimpleMIRAModel {

    public final static String LOGIT_SHELL = "LOGIT";
    public final static String LINEAR_SHELL = "LINEAR";
    private String modelIdentifier;
    private String shell = LINEAR_SHELL;
    private String[] featureIdentifiers;
    private double[] weights;

    public SimpleMIRAModel(String filename) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        modelIdentifier = in.readLine().trim();
        shell = in.readLine().trim();
        double w0 = Double.parseDouble(in.readLine().trim());
        int n = Integer.parseInt(in.readLine().trim());
        featureIdentifiers = new String[n];
        weights = new double[n + 1];
        weights[0] = w0;
        for (int i = 0; i < n; i++) {
            String line = in.readLine().trim();
            String[] tokens = line.split("\t");
            featureIdentifiers[i] = tokens[0];
            weights[i + 1] = Double.parseDouble(tokens[1]);
        }
        in.close();
    }

    public SimpleMIRAModel(String id, String[] featureIds, String shell) {
        modelIdentifier = id;
        featureIdentifiers = new String[featureIds.length];
        weights = new double[featureIdentifiers.length + 1];
        weights[0] = 0.0;
        for (int i = 0; i < featureIdentifiers.length; i++) {
            featureIdentifiers[i] = featureIds[i];
            weights[i + 1] = 0.0;
        }
        this.shell = shell;
    }

    public void updateWeight(int i, double value) {
        weights[i] = value;
    }

    public void updateWeights(double[] newWeights) {
        for (int i = 0; i < newWeights.length; i++) {
            weights[i] = newWeights[i];
        }
    }

    public void divideBy(double z) {
        for (int i = 0; i < weights.length; i++) {
            weights[i] /= z;
        }
    }

    public void add(double[] newWeights) {
        for (int i = 0; i < newWeights.length; i++) {
            weights[i] += newWeights[i];
        }
    }

    public void add(SimpleMIRAModel m) {
        double[] newWeights = m.getWeights();
        this.add(newWeights);
    }

    public String getIdentifier() {
        return modelIdentifier;
    }

    public String getShell() {
        return shell;
    }

    public String[] getFeatureIdentifiers() {
        return featureIdentifiers;
    }

    public double[] getWeights() {
        return weights;
    }

    public double applyTo(GenericFeatureVector fv) {
        double ret = weights[0];
        for (int i = 0; i < featureIdentifiers.length; i++) {
            ret += (fv.getValueFor(featureIdentifiers[i]) * weights[i + 1]);
        }
        if (shell.equals(LOGIT_SHELL)) {
            ret = 1.0 / (1.0 + Math.exp(-1 * ret));
        }
        return ret;
    }

    public double applyTo(double[] values) {
        double ret = weights[0];
        for (int i = 0; i < values.length; i++) {
            ret += (values[i] * weights[i + 1]);
        }
        if (shell.equals(LOGIT_SHELL)) {
            ret = 1.0 / (1.0 + Math.exp(-1 * ret));
        }
        return ret;
    }

    public void writeTo(String filename) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        out.write(modelIdentifier + "\n");
        out.write(shell + "\n");
        out.write(weights[0] + "\n");
        out.write(featureIdentifiers.length + "\n");
        for (int i = 0; i < featureIdentifiers.length; i++) {
            out.write(featureIdentifiers[i] + "\t" + weights[i + 1] + "\n");
        }
        out.close();
    }
}
