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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author rohitk
 */
public class RegExConcept extends Concept 
{
    private List<Pattern> patterns;
    private boolean exact = false;

    public RegExConcept(String label) 
    {
        this(label, false);
    }

    public RegExConcept(String label, boolean exact) 
    {
        super(label);
        this.exact = exact;
        patterns = new ArrayList<Pattern>();
    }


    public void addPattern(String p) 
    {
        patterns.add(Pattern.compile(p));
    }

    public List<Pattern> getPatterns() 
    {
        return patterns;
    }

    public double match(String instance, Collection<String> annotations) 
    {
            String turn = sanitize(instance);
            
            for (Pattern phrase : patterns) 
            {
            	Matcher m = phrase.matcher(turn);
				if(exact)
            	{
            		if(m.matches())
            			return 1.0;
            	}
            	else
                {
                   if(m.find())
                	   return 1.0;
                }
            }
            return 0.0;
    }


    public String getText() 
    {
        if(patterns.isEmpty())
            return label;
        else
            return patterns.get((int)(Math.random()*patterns.size())).toString();
    }
    
    public String toString()
    {
    	return "Regex Matcher Concept "+label+"="+patterns;
    }
}
