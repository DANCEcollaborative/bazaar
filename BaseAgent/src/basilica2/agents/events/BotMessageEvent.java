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

/**
 *
 * @author rcmurray
 */
public class BotMessageEvent extends Event {

    public static String GENERIC_NAME = "BOT_MESSAGE_EVENT";
    private String botMessage = "NOT FOUND";
    private String from = "NOT FOUND";
//	public enum fileEventType  
//	{
//		created, changed, deleted;
//	}
//	private fileEventType eventType; 

    public BotMessageEvent(Component s, String from, String n) {
        super(s);
        botMessage = n;
        this.from = from;

//        System.err.println("FileEvent created: file: " + n + " -- event type:" + t);
//        Logger.commonLog(getClass().getSimpleName(),Logger.LOG_NORMAL,"FileEvent created - file: " + n + "   -- event type: " + t);
    }

    public String getText() {
        return botMessage;
    }

	public void setFrom(String newFrom)
	{
		from = newFrom;
	}

	public String getFrom()
	{
		return from;
	}

    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    @Override
    public String toString() {
        return "botMessageEvent: " + botMessage;
    }
}
