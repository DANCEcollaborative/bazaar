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
package basilica2.tutor.events;

import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class TutorTurnsEvent extends Event {

    public static String GENERIC_NAME = "TUTOR_TURNS_EVENT";
    private String[] tutorTurns = null;
    private boolean ackExpected = false;

    public TutorTurnsEvent(Component s, String[] ts) {
        super(s);
        tutorTurns = ts;
    }

    public boolean isAcknowledgementExpected() {
        return ackExpected;
    }

    public void setAcknoledgementExpected(boolean a) {
        ackExpected = a;
    }

    public String[] getTutorTurns() {
        return tutorTurns;
    }

    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    @Override
    public String toString() {
        String ret = "Tutor Turns ("+tutorTurns.length+"):\n{\n";
        for (int i = 0; i < tutorTurns.length; i++) {
            ret += "\t" + tutorTurns[i] + "\n";
        }
        ret += "}";
        return ret;
    }
}
