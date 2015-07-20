/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.cs.lti.tutalk.module;

import java.util.Collection;
import java.util.Map;

import edu.cmu.cs.lti.tutalk.script.Concept;

/**
 *
 * @author dadamson
 */
public class ModelConcept extends Concept
{
    /**
     * a source of concept analysis/classification 
     */
    public interface Predictor
    {
        /**
         * 
         * @param instance the turn to classify
         * @return a map of concept-labels to prediction-likelihoods between zero (no match) and 1.0
         */
        public Map<String, Double> getPredictions(String instance);

        /**
         * 
         * @return a unique name for this predictor instance
         */
        public String getName();
    }
    
    private Predictor predictor;
    public ModelConcept(String label, Predictor predictor)
    {
        super(label);
        this.predictor = predictor;
    }

    @Override
    public double match(String instance, Collection<String> annotations) 
    {
        if(predictor.getPredictions(instance).containsKey(label))
            return predictor.getPredictions(instance).get(label);
        else 
            return 0;
    }
    public String toString()
    {
    	return "Classifier Concept "+label;
    }
}
