/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.cmu.cs.lti.tutalk.script;

import java.util.Collection;

/**
 *
 * @author dadamson
 */
public abstract class Concept 
{
    protected String label;
    
    public Concept(String label)
    {
        this.label = label;
    }
    /**
     * 
     * @param instance the text to be evaluated for this concept
     * @param annotations any externally-processed annotations for this turn.
     * @return Zero if the concept doesn't match the instance.
     * if it does match, return a double between 0 and 1.0 representing the confidence/probability in the concept.
     */
    public abstract double match(String instance, Collection<String> annotations);
    
    /**
     * 
     * @return the referencing label for this concept.
     */
    public String getLabel()
    {
        return label;
    }
    
    /**
     * 
     * @return an output-friendly representation of this concept, for output by the tutor.
     */
    public String getText()
    {
        return getLabel();
    }
    
    public String toString()
    {
        return "Concept:"+getLabel();
    }
    

    protected String sanitize(String phrase) 
    {
        return " "+phrase.toLowerCase().replaceAll("\\s+", " ").replaceAll("\\|", " ");
    }
}
