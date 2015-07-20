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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class GeneralInquirer {

    List<String> categoryList = new ArrayList<String>();
    List<String> words = new ArrayList<String>();
    List<String> category = new ArrayList<String>();

    public GeneralInquirer(String filename) {
        try {
            BufferedReader inFile = new BufferedReader(new FileReader(filename));
            String line = "";
            while ((line = inFile.readLine()) != null) {
                String[] tokens = line.trim().replace('\t', ' ').split(" ");
                if (tokens.length != 2) {
                    throw new Error("Not enough tokens in dictionary line: " + line);
                }
                if (tokens[1].length() < 3) {
                    continue;
                }
                String stemmedWord = Stemmer.stem(tokens[1].trim().toLowerCase());
                boolean dup = false;
                for (int i = words.size() - 1; i >= 0; i--) {
                    if (category.get(i).equals(tokens[0])) {
                        if (words.get(i).equals(stemmedWord)) {
                            dup = true;
                            break;
                        }
                    }
                }
                if (!dup) {
                    words.add(stemmedWord);
                    category.add(tokens[0]);
                }
                dup = false;
                for (int i = categoryList.size() - 1; i >= 0; i--) {
                    if (categoryList.get(i).equals(tokens[0])) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) {
                    categoryList.add(tokens[0]);
                }
            }
            inFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        System.out.println("GI: Read " + words.size() + " Lexicon Items. " + categoryList.size() + " Categories");
    }

    public String[] getCategorynames() {
        return categoryList.toArray(new String[0]);
    }

    public int[] getGIIndices(String text) {
        int[] indices = new int[categoryList.size()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = 0;
        }

        text = text.toLowerCase().trim().replaceAll("[^\\p{Alnum}^\'^-]", " ").replace('\t', ' ').replace("  ", " ").replace("  ", " ").replace("  ", " ").replace("  ", " ");
        text = text.replace("kind of", "kindof");
        String[] tokens = text.split(" ");

        for (int i = 0; i < words.size(); i++) {
            String w = words.get(i);
            for (int j = 0; j < tokens.length; j++) {
                String t = Stemmer.stem(tokens[j]);
                if (t.equals(w)) {
                    String c = category.get(i);
                    //System.out.println(t + " matches " + w + " > Cat=" + c);
                    for (int k = 0; k < categoryList.size(); k++) {
                        if (c.equalsIgnoreCase(categoryList.get(k))) {
                            indices[k]++;
                        }
                    }
                }
            }
        }
        return indices;
    }

    public void printIndices(int[] is) {
        String[] cs = getCategorynames();
        for (int i = 0; i < is.length; i++) {
            System.out.println(cs[i] + " = " + is[i]);
        }
    }
}
