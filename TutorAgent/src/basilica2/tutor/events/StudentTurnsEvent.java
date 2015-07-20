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

import java.util.List;

import de.fhg.ipsi.utils.StringUtilities;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;

public class StudentTurnsEvent extends Event {

    public static String GENERIC_NAME = "STUDENT_TURNS_EVENT";
    private List<String> studentTurns = null;
    private List<String> contributors = null;
    private List<String> annotations = null;

    public StudentTurnsEvent(Component s, List<String> studentTurns,List<String> contributors, List<String> annotations) {
        super(s);
        this.studentTurns = studentTurns;
        this.contributors = contributors;
        this.annotations = annotations;
    }

    public List<String> getStudentTurns() {
        return studentTurns;
    }

    public List<String> getContributors() {
        return contributors;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    @Override
    public String getName() {
        return GENERIC_NAME;
    }

    @Override
    public String toString() 
    {
        String ret = "Student Turns Chunk ("+studentTurns.size()+"): [";
        for (String turn : studentTurns) {
            ret += turn + " | ";
        }
        ret += "]";
        return ret;
    }
}
