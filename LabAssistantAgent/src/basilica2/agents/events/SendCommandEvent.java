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
public class SendCommandEvent extends Event {

    public static String GENERIC_NAME = "SEND_COMMAND_EVENT";
    private static String command = null; 

    public SendCommandEvent(Component s, String commandArg) {
        super(s);
        command = commandArg; 
        System.err.println("SendCommandEvent, constructor - SendCommandEvent created: command: " + command);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"SendCommandEvent, constructor - SendCommandEvent created: command: " + command);
    }

    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    public String getCommand() {
        System.err.println("SendCommandEvent, getCommand: " + command);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"SendCommandEvent, getCommand: " + command);
        return command;
    }

    public void setCommand(String commandArg) {
        command = commandArg;
        System.err.println("SendCommandEvent, setCommand: " + command);
        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"SendCommandEvent, setCommand: " + command);
    }
    
    @Override
    public String toString() {
        return "<SEND_COMMAND_EVENT command = \"" + command + "/>";
    }
}