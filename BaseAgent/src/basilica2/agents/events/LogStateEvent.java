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
    private static String[] logStateListValue;  
    private static String jsonValue;
    private static Boolean valueIsString = false; 
    private static Boolean valueIsMap = false; 
    private static Boolean valueIsList = false;     
    private static Boolean sendLogEvent = true; 
    private static String logEventTag; 

    public LogStateEvent(Component s, String tag, String stringValue, Boolean sendLog, String logTag) {
        super(s);
        logStateTag = tag; 
        logStateStringValue = stringValue;
        valueIsString = true; 
        sendLogEvent = sendLog; 
        logEventTag = logTag; 
        System.err.println("LogStateEvent, LogStateEvent - LogStateEvent created: logStateTag= " + logStateTag + "  logStateStringValue = " + logStateStringValue);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, LogStateEvent - LogStateEvent created: logStateTag= " + logStateTag + "  logStateStringValue = " + logStateStringValue);
    }

    public LogStateEvent(Component s, String tag, Map<String, String> mapValue, Boolean sendLog, String logTag) {
        super(s);
        logStateTag = tag; 
        logStateMapValue = new HashMap<>(); 
        logStateMapValue = mapValue; 
        valueIsMap = true; 
        sendLogEvent = sendLog; 
        logEventTag = logTag; 
        System.err.println("LogStateEvent, LogStateEvent - LogStateEvent with Map created: logStateTag= " + logStateTag);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, LogStateEvent - LogStateEvent with Map created: logStateTag= " + logStateTag);
    }

    public LogStateEvent(Component s, String tag, String[] listValue, Boolean sendLog, String logTag) {
        super(s);
        logStateTag = tag; 
        logStateListValue = new String[listValue.length];        
        valueIsList = true; 
        sendLogEvent = sendLog; 
        logEventTag = logTag; 
        System.err.println("LogStateEvent, LogStateEvent - LogStateEvent with String[] created: logStateTag= " + logStateTag);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, LogStateEvent - LogStateEvent with String[] created: logStateTag= " + logStateTag);
    }

    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    public String getLogStateTag() {
        System.err.println("LogStateEvent, getlogStateTag - logStateTag: " + logStateTag);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getlogStateTag - logStateTag=" + logStateTag);
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
        System.err.println("LogStateEvent, getLogStateValue - enter");
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getLogStateValue - enter");
        if (valueIsString) {
            System.err.println("LogStateEvent, getLogStateValue - from String: " + logStateStringValue);
            Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getLogStateValue - rom String: " + logStateStringValue);
        	return logStateStringValue; 
        } else if (valueIsMap) {
        	JSONObject jsonObject = new JSONObject(logStateMapValue);
        	String jsonString = jsonObject.toString(); 
            System.err.println("LogStateEvent, getLogStateValue - from Map: " + jsonString);
            Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getLogStateValue - from Map: " + jsonString);
        	return jsonString; 
        } else if (valueIsList) {
	    	JSONObject jsonObject = new JSONObject(logStateListValue);
	    	String jsonString = jsonObject.toString(); 
	        System.err.println("LogStateEvent, getLogStateValue - from String[]: " + jsonString);
	        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getLogStateValue - rom String[]: " + jsonString);
	    	return jsonString; 
    }

    // Returning Boolean as String for use by WebsocketChatClient to send string to socket
    public String getLogStateSendLog() {
        System.err.println("LogStateEvent, getLogStateSendLog - sendLogEvent: " + sendLogEvent);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getLogStateSendLog - sendLogEvent = " + String.valueOf(sendLogEvent));
        return String.valueOf(sendLogEvent);
    }

    public String getLogEventTag() {
        System.err.println("LogStateEvent, getlogEventTag - logEventTag: " + logEventTag);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"LogStateEvent, getlogEventTag - logEventTag=" + logEventTag);
        return logEventTag;
    }
    
    @Override
    public String toString() {
        return "<LOG_STATE_EVENT event -- logStateTag=\"" + logStateTag + "   logStateValue = \"" + getLogStateValue() + " sendLogEvent = \"" + String.valueOf(sendLogEvent) + " logEventTag = \"" + logEventTag + "/>";
    }
}
