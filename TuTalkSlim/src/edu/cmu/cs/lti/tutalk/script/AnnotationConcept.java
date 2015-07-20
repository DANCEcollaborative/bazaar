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
package edu.cmu.cs.lti.tutalk.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class AnnotationConcept extends Concept 
{

    private List<String> annotations;
    private boolean all = false; //when true, all annotations must match, not just one.

    public AnnotationConcept(String label) 
    {
        this(label, false);
    }

    public AnnotationConcept(String label, boolean all) 
    {
        super(label);
        this.all = all;
        annotations = new ArrayList<String>();
    }


    public void addAnnotation(String p) 
    {
        annotations.add(p);
    }

    public List<String> getPhrases() 
    {
        return annotations;
    }

    public double match(String instance, Collection<String> notes) 
    {
    		if(annotations.isEmpty())
    		{
    			System.err.println("Warning: annotation list is empty for concept "+label);
    		}
            for(String annotation : annotations)
            {
            	if(notes.contains(annotation))
            	{
            		if(!all)
            			return 1.0;
            	}
            	else if(all)
            		return 0.0;
            }
            return all?1.0:0.0;

//            boolean allMatch = true;
//            for (String note : notes) 
//            {
//            	boolean match = annotations.contains(note);
//            	if(match && !all) 
//            		return 1.0;
//            	
//            	allMatch = allMatch && match;
//            }
//            return allMatch?1.0:0.0;
    }

    public String getText() 
    {
        if(annotations.isEmpty())
            return label;
        else
            return annotations.get((int)(Math.random()*annotations.size()));
    }
    
    public String toString()
    {
    	return "Annotation Matcher Concept "+label+"="+annotations;
    }
}
