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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author rohitk
 */
public class Vocab {

    Map<String, Unigram> unigrams = new LinkedHashMap<String, Unigram>();
    Map<String, Bigram> bigrams = new LinkedHashMap<String, Bigram>();
    Map<String, Trigram> trigrams = new LinkedHashMap<String, Trigram>();
    public List<String> topNUnigrams = new ArrayList<String>();
    public List<String> topNBigrams = new ArrayList<String>();
    public List<String> topNTrigrams = new ArrayList<String>();
    int UNIGRAM_THRESHOLD, BIGRAM_THRESHOLD, TRIGRAM_THRESHOLD;
    String[] stopwords;
    boolean doStemming, useStopwords, useBigrams, useTrigrams;

    public Vocab(int unigramThreshold, int bigramThreshold, int trigramThreshold, String[] stopwordList, boolean doStemming, boolean removeStopwords, boolean useBigrams, boolean useTrigrams) {
        UNIGRAM_THRESHOLD = unigramThreshold;
        BIGRAM_THRESHOLD = bigramThreshold;
        TRIGRAM_THRESHOLD = trigramThreshold;
        this.stopwords = stopwordList;
        this.doStemming = doStemming;
        this.useStopwords = removeStopwords;
        this.useBigrams = useBigrams;
        this.useTrigrams = useTrigrams;
    }

    public Vocab(boolean doStemming, String vocabFilename) {
        this.doStemming = doStemming;
        try {
            BufferedReader vocabFile = new BufferedReader(new FileReader(vocabFilename));
            String line = vocabFile.readLine().trim(); //Ignore the first one
            while (line != null) {
                String[] tokens = line.split("\t");
                if (tokens.length == 2) {
                    topNUnigrams.add(tokens[0].trim());
                } else if (tokens.length == 3) {
                    topNBigrams.add(tokens[0].trim() + "_" + tokens[1].trim());
                } else if (tokens.length == 4) {
                    topNTrigrams.add(tokens[0].trim() + "_" + tokens[1].trim() + "_" + tokens[2].trim());
                } else {
                    System.err.println("Invalid no. of tokens: " + line);
                }
                line = vocabFile.readLine();
            }
            vocabFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (topNBigrams.size() == 0) {
            useBigrams = false;
        } else {
            useBigrams = true;
        }
        if (topNTrigrams.size() == 0) {
            useTrigrams = false;
        } else {
            useTrigrams = true;
        }
        System.out.println("Read Unigrams: " + topNUnigrams.size() + " Bigrams:" + topNBigrams.size() + " Trigrams: " + topNTrigrams.size() + " | UsingStemmer: " + doStemming);
    }

    public int[] string2WordVector(String text) {
        return string2WordVector(text, false);
    }

    public int[] string2WordVector(String text, boolean giveCounts) {
        String[] tokens = text.split(" ");
        String[] stemmedTokens = new String[tokens.length];
        int vectorSize = topNUnigrams.size() + ((useBigrams) ? topNBigrams.size() : 0) + ((useTrigrams) ? topNTrigrams.size() : 0);
        int[] vector = new int[vectorSize];

        for (int i = 0; i < vector.length; i++) {
            vector[i] = 0;
        }

        for (int i = 0; i < tokens.length; i++) {
            if (doStemming) {
                stemmedTokens[i] = Stemmer.stem(tokens[i].trim());
            } else {
                stemmedTokens[i] = tokens[i].trim();
            }
        }

        for (int i = 0; i < tokens.length; i++) {
            int index = topNUnigrams.indexOf(stemmedTokens[i]);
            if (index != -1) {
                if (giveCounts) {
                    vector[index]++;
                } else {
                    vector[index] = 1;
                }
            }

            if (useBigrams) {
                if (i > 0) {
                    index = topNBigrams.indexOf(stemmedTokens[i - 1] + "_" + stemmedTokens[i]);
                    if (index != -1) {
                        if (giveCounts) {
                            vector[topNUnigrams.size() + index]++;
                        } else {
                            vector[topNUnigrams.size() + index] = 1;
                        }
                    }
                }
            }

            if (useTrigrams) {
                if (i > 1) {
                    index = topNTrigrams.indexOf(stemmedTokens[i - 2] + "_" + stemmedTokens[i - 1] + "_" + stemmedTokens[i]);
                    if (index != -1) {
                        if (giveCounts) {
                            vector[topNUnigrams.size() + topNBigrams.size() + index]++;
                        } else {
                            vector[topNUnigrams.size() + topNBigrams.size() + index] = 1;
                        }
                    }
                }
            }
        }
        return vector;
    }

    public void addItems(String text) {
        String[] tokens = text.split(" ");
        String[] stemmedTokens = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            String t = tokens[i].trim();
            if (doStemming) {
                t = Stemmer.stem(t);
            }
            stemmedTokens[i] = t;
            boolean stop = false;
            if (useStopwords) {
                for (int j = 0; j < stopwords.length; j++) {
                    if (stopwords[j].equalsIgnoreCase(tokens[i].trim())) {
                        stop = true;
                        break;
                    }
                }
            }
            //System.out.println("Processing Token: " + tokens[i]);
            if (stop) {
                continue;
            }
            if ((t.length() > 1) && (t.length() < 13)) { //Heuristic
                //Update Unigrams
                Unigram u = unigrams.get(t);
                if (u != null) {
                    u.count++;
                } else {
                    u = new Unigram();
                    u.index = unigrams.size();
                    u.count = 1;
                    u.word = t;
                    unigrams.put(t, u);
                //System.out.println("Added Unigram: " + t);
                }

                //Update Bigrams
                if (useBigrams) {
                    if (i > 0) {
                        Bigram b = bigrams.get(stemmedTokens[i - 1] + "_" + t);
                        if (b != null) {
                            b.count++;
                        } else {
                            if (stemmedTokens[i - 1].length() > 0) {
                                b = new Bigram();
                                b.index = bigrams.size();
                                b.word1 = stemmedTokens[i - 1];
                                b.word2 = t;
                                b.count = 1;
                                bigrams.put(stemmedTokens[i - 1] + "_" + t, b);
                            //System.out.println("Added Bigram: " + stemmedTokens[i - 1] + "_" + t);
                            }
                        }
                    }
                }

                //Update Trigrams
                if (useTrigrams) {
                    if (i > 1) {
                        Trigram tr = trigrams.get(stemmedTokens[i - 2] + "_" + stemmedTokens[i - 1] + "_" + t);
                        if (tr != null) {
                            tr.count++;
                        } else {
                            if (stemmedTokens[i - 2].length() > 0) {
                                if (stemmedTokens[i - 1].length() > 0) {
                                    tr = new Trigram();
                                    tr.index = bigrams.size();
                                    tr.word1 = stemmedTokens[i - 2];
                                    tr.word2 = stemmedTokens[i - 1];
                                    tr.word3 = t;
                                    tr.count = 1;
                                    trigrams.put(stemmedTokens[i - 2] + "_" + stemmedTokens[i - 1] + "_" + t, tr);
                                //System.out.println("Added Trigram: " + stemmedTokens[i - 2] + "_" + stemmedTokens[i - 1] + "_" + t);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void printSorted(String filename) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(filename));

            //Write the Unigram
            String[] words = unigrams.keySet().toArray(new String[0]);
            int[] counts1 = new int[words.length];
            for (int i = 0; i < words.length; i++) {
                counts1[i] = unigrams.get(words[i]).count;
            }

            for (int i = 0; i < words.length - 1; i++) {
                for (int j = i + 1; j < words.length; j++) {
                    if (counts1[i] < counts1[j]) {
                        int ck = counts1[i];
                        counts1[i] = counts1[j];
                        counts1[j] = ck;

                        String wk = words[i];
                        words[i] = words[j];
                        words[j] = wk;
                    }
                }
            }

            for (int i = 0; i < words.length; i++) {
                if (counts1[i] < UNIGRAM_THRESHOLD) {
                    break;
                }
                topNUnigrams.add(words[i]);
                out.write(words[i] + "\t" + counts1[i] + "\n");
            }
            System.out.println("Top Unigram Count: " + topNUnigrams.size());

            //Write the Bigram
            if (useBigrams) {
                words = bigrams.keySet().toArray(new String[0]);
                int[] counts2 = new int[words.length];
                for (int i = 0; i < words.length; i++) {
                    counts2[i] = bigrams.get(words[i]).count;
                }

                for (int i = 0; i < words.length - 1; i++) {
                    for (int j = i + 1; j < words.length; j++) {
                        if (counts2[i] < counts2[j]) {
                            int ck = counts2[i];
                            counts2[i] = counts2[j];
                            counts2[j] = ck;

                            String wk = words[i];
                            words[i] = words[j];
                            words[j] = wk;
                        }
                    }
                }

                for (int i = 0; i < words.length; i++) {
                    if (counts2[i] < BIGRAM_THRESHOLD) {
                        break;
                    }
                    topNBigrams.add(words[i]);
                    out.write(words[i].replace('_', '\t') + "\t" + counts2[i] + "\n");
                }
                System.out.println("Top Bigram Count: " + topNBigrams.size());
            }

            //Write the Trigram
            if (useTrigrams) {
                words = trigrams.keySet().toArray(new String[0]);
                int[] counts3 = new int[words.length];
                for (int i = 0; i < words.length; i++) {
                    counts3[i] = trigrams.get(words[i]).count;
                }

                for (int i = 0; i < words.length - 1; i++) {
                    for (int j = i + 1; j < words.length; j++) {
                        if (counts3[i] < counts3[j]) {
                            int ck = counts3[i];
                            counts3[i] = counts3[j];
                            counts3[j] = ck;

                            String wk = words[i];
                            words[i] = words[j];
                            words[j] = wk;
                        }
                    }
                }

                for (int i = 0; i < words.length; i++) {
                    if (counts3[i] < TRIGRAM_THRESHOLD) {
                        break;
                    }
                    topNTrigrams.add(words[i]);
                    out.write(words[i].replace('_', '\t') + "\t" + counts3[i] + "\n");
                }
                System.out.println("Top Trigram Count: " + topNTrigrams.size());
            }

            out.close();
        } catch (Exception ex) {
            System.err.println("Error while writing vocab index");
            ex.printStackTrace();
        }
    }
}