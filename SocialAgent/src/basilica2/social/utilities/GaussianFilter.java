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
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author rohitk
 */
public class GaussianFilter {

    //Each Gaussian is nAs[n] * e^((-1 * (i - nTs[n])^2)/nCs[n])
    double[] nAs, nTs, nCs;

    public GaussianFilter(String filename) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(filename));
        int n = Integer.parseInt(in.readLine().trim());
        nAs = new double[n];
        nTs = new double[n];
        nCs = new double[n];
        for (int i = 0; i < n; i++) {
            String line = in.readLine().trim();
            String[] tokens = line.split("\t");
            nAs[i] = Double.parseDouble(tokens[0]);
            nTs[i] = Double.parseDouble(tokens[1]);
            nCs[i] = Double.parseDouble(tokens[2]);
        }
        in.close();
    }

    public double getThreshold(int i) {
        double ret = 0.0;
        for (int n = 0; n < nAs.length; n++) {
            ret += (nAs[n] * Math.exp(-1.0 * (i - nTs[n]) * (i - nTs[n]) / nCs[n]));
        }
        ret -= 0.05; //Some Magic Number
        return ret;
    }
}
