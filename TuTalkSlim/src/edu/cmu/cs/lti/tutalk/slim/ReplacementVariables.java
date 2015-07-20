/*
 *  Copyright (c), 2011 Carnegie Mellon University.
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

package edu.cmu.cs.lti.tutalk.slim;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

/** Replacement Variables
 *
 * A fast implementation to replace variables in Tutalk scripts.
 *
 * @author Huan Truong | huant@andrew.cmu.edu
 */
public class ReplacementVariables {

    public ReplacementVariables() {
        tokens = new HashMap<String,String>();
        isDirty = false;
    }

    public void resetReplacementVariables() {
        tokens = new HashMap<String,String>();
        isDirty = true;
    }

    public void addReplacementVariable(String pKey, String pValue) {
        tokens.put(pKey, pValue);
        isDirty = true;
    }

    public void compileReplacementVariables() {
        String patternString = "(" + StringUtils.join(tokens.keySet(), "|") + ")";
        pattern = Pattern.compile(patternString);
        isDirty = false;
    }
    
    public String renderTemplate(String input) {

        if (isDirty) compileReplacementVariables();

        if(pattern == null)
            return input;
        
        Matcher matcher = pattern.matcher(input);

        StringBuffer sb = new StringBuffer();
        while(matcher.find()) 
        {
            matcher.appendReplacement(sb, tokens.get(matcher.group(1)));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    // Local variables
    Pattern pattern;
    private Map<String,String> tokens;
    boolean isDirty;
}
