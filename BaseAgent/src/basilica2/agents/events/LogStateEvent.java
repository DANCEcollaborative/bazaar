/*
 *  Copyright (c), 2020 Carnegie Mellon University.
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
package basilica2.agents.events;

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.HashMap;
import java.util.Map; 

/**
 *
 * @author rcmurray
 */
public class LogStateEvent extends Event {

    public static String GENERIC_NAME = "LOG_STATE_EVENT";; 
    private static String logStateTag; 
    private static String logStateStringValue; 
    private static Map<String, String> logStateMapValue;  
    private static Boolean valueIsString; 

    public LogStateEvent(Component s, String tag, String stringValue) {
        super(s);
        logStateTag = tag; 
        logStateStringValue = stringValue;
        valueIsString = true; 
        System.err.println("LogStateEvent.java, LogStateEvent - LogStateEvent created: logStateTag= " + logStateTag + "  logStateStringValue = " + logStateStringValue);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent.java, LogStateEvent - LogStateEvent created: logStateTag= " + logStateTag + "  logStateStringValue = " + logStateStringValue);
    }

    public LogStateEvent(Component s, String tag, Map<String, String> mapValue) {
        super(s);
        logStateTag = tag; 
        logStateMapValue = new HashMap<>(); 
        logStateMapValue = mapValue; 
        valueIsString = false; 
        System.err.println("LogStateEvent.java, LogStateEvent - LogStateEvent with Map created: logStateTag= " + logStateTag);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent.java, LogStateEvent - LogStateEvent with Map created: logStateTag= " + logStateTag);
    }

    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    public String getLogStateTag() {
        System.err.println("LogStateEvent.java, getlogStateTag - logStateTag: " + logStateTag);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent.java, getlogStateTag - logStateTag=" + logStateTag);
        return logStateTag;
    }

    public void setLogStateTag(String tag) {
        logStateTag = tag;
    }

    public void setLogStateStringValue(String stringValue) {
        logStateStringValue = stringValue;
        valueIsString = true; 
    }
    
    public void setLogStateMapValue(Map<String, String> mapValue) {
        logStateMapValue = new HashMap<>(); 
        logStateMapValue = mapValue; 
        valueIsString = false; 
    }

    public String getLogStateValue() {
        System.err.println("LogStateEvent.java, getLogStateValue - enter");
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent.java, getLogStateValue - enter");
        if (valueIsString) {
            System.err.println("LogStateEvent, getLogStateValue - from String: " + logStateStringValue);
            Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getLogStateValue - rom String: " + logStateStringValue);
        	return logStateStringValue; 
        } else {
        	JSONObject jsonObject = new JSONObject(logStateMapValue);
        	String jsonString = jsonObject.toString(); 
            System.err.println("LogStateEvent, getLogStateValue - from Map: " + jsonString);
            Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getLogStateValue - from Map: " + jsonString);
        	return jsonString; 
        }
    }

    @Override
    public String toString() {
        return "<LOG_STATE_EVENT event -- logStateTag=\"" + logStateTag + "   logStateValue = \"" + getLogStateValue() + "/>";
    }
}
