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
package basilica2.agents.events;

import java.awt.Point;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

/**
 *
 * @author rohitk
 */
public class PresenceEvent extends Event
{

    public static String GENERIC_NAME = "PRESENCE_EVENT";
    public static String PRESENT = "PRESENT";
    public static String ABSENT = "ABSENT";
    private String agentname;
    private String agentid;
    private String agentperspective;
    private String agentupdate;
    private String type;
    private int numUsers;

    public PresenceEvent(Component s, String a, String t) {
        super(s);
        agentname = a;
        type = t;
    }

    public PresenceEvent(Component s, String a, String t, String id) {
        super(s);
        agentname = a;
        type = t;
        agentid = id;
	}
    
    public PresenceEvent(Component s, String a, String t, String id, String perspective) {
        super(s);
        agentname = a;
        type = t;
        agentid = id;
        agentperspective = perspective;
	}

    public PresenceEvent(Component s, String a, String t, String id, String perspective, String update) {
        super(s);
        agentname = a;
        type = t;
        agentid = id;
        agentperspective = perspective;
        agentupdate = update;
	}
    
    public PresenceEvent(Component s, String a, String t, int num) {
        super(s);
        agentname = a;
        type = t;
        numUsers = num;
	}

	public String getUsername() {
        return agentname;
    }
	
	public String getUserId() {
        return agentid;
    }

	public String getUserPerspective() {
        return agentperspective;
    }
	
	public String getUserUpdate() {
        return agentupdate;
    }
	
    public String getType() {
        return type;
    }

    public int getNumUsers() {
        return numUsers;
    }
    
    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    @Override
    public String toString() {
        return "Presence: " + agentname + " is now " + type;
    }
    
}
