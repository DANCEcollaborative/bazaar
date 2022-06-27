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
public class LogEvent extends Event {

    public static String GENERIC_NAME = "LOG_EVENT";
    private static String logTag = null; 
    private static String logDetailsString = null; 
    private static Map<String, String> logDetailsMap = null; 
    private static Boolean detailIsString = false; 
    private static Boolean detailIsMap = false; 

    public LogEvent(Component s, String tag, String detailsString) {
        super(s);
        logTag = tag; 
        logDetailsString = detailsString; 
        detailIsString = true; 
//        System.err.println("LogEvent, constructor - LogEvent created: logTag: " + logTag + "   logDetailsString: " + logDetailsString);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogEvent, LogEvent - LogEvent created: logTag: " + logTag + "   logDetailsString: " + logDetailsString);
    }

    public LogEvent(Component s, String tag, Map<String, String> detailsMap) {
        super(s);
        logTag = tag; 
        logDetailsMap = detailsMap; 
        detailIsMap = true; 
//        System.err.println("LogEvent, constructor - LogEvent with Map created: logTag: " + logTag);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogEvent, LogEvent - LogEvent with Map created: logTag: " + logTag);
    }

    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    public String getLogTag() {
//        System.err.println("LogEvent, getLogTag - logTag: " + logTag);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogEvent, getLogTag - logTag: " + logTag);
        return logTag;
    }

    public String getLogDetails() {
//        System.err.println("LogEvent.java, getLogDetails - enter");
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogEvent.java, getLogDetails - enter");
        if (detailIsString) {
//            System.err.println("LogEvent, getLogDetails - from String: " + logDetailsString);
//            Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogEvent, getLogDetails - from String: " + logDetailsString);
        	return logDetailsString; 
        } else if (detailIsMap) {
        	JSONObject jsonObject = new JSONObject(logDetailsMap);
        	String jsonString = jsonObject.toString(); 
//            System.err.println("LogEvent, getLogDetails - from Map: " + jsonString);
//            Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogEvent, getLogDetails - from Map: " + jsonString);
        	return jsonString; 
        } else
        	return null; 
    }

    public void setLogTag(String tag) {
        logTag = tag;
    }

    public void setLogDetails(String detailsString) {
        logDetailsString = detailsString;
    }

    public void setLogDetails(Map<String, String> detailsMap) {
        logDetailsMap = detailsMap;
    }

    @Override
    public String toString() {
        return "<LOG_EVENT tag = \"" + logTag + "   details = \"" + getLogDetails() + "/>";
    }
}
