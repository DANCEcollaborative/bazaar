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
package edu.cmu.cs.lti.tutalk.slim;

import edu.cmu.cs.lti.tutalk.script.Executable;
import edu.cmu.cs.lti.tutalk.script.Goal;
import edu.cmu.cs.lti.tutalk.script.Initiation;
import edu.cmu.cs.lti.tutalk.script.Response;
import edu.cmu.cs.lti.tutalk.script.Step;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author rohitk
 */
public class ExecutionState {

    public final static int STATUS_NOT_INITIATED = 0;
    public final static int STATUS_INITIATED = 1;
    public final static int STATUS_DONE = 2;
    private int status;
    private List<Goal> todoStack;
    private List<Executable> doneList;
    private List<Response> expected;

    public ExecutionState() {
        todoStack = new ArrayList<Goal>();
        doneList = new ArrayList<Executable>();
        status = STATUS_NOT_INITIATED;
    }

    public List<Goal> getTodos() {
        return todoStack;
    }

    public Goal getCurrentGoal() {
        if (todoStack.size() != 0) {
            return todoStack.get(0);
        }
        return null;
    }

    public void push(Goal g) {
        if (status == STATUS_NOT_INITIATED) {
            status = STATUS_INITIATED;
        }

        boolean found = false;
        for (int i = 0; i < todoStack.size(); i++) {
            if (todoStack.get(i).getName().equals(g.getName())) {
                found = true;
                break;
            }
        }

        if (!found) {
            todoStack.add(0, g);
//            System.out.println("=============================");
//            System.out.println(this.toString());
//            System.out.println("=============================");
        }
    }

    public void pop() {
        if (todoStack.size() > 0) {
            Goal g = todoStack.remove(0);
            doneList.add(g);
            if (todoStack.size() == 0) {
                status = STATUS_DONE;
            }
        }
    }

    public List<Executable> getDone() {
        return doneList;
    }

    public void addDone(Executable e) {
        doneList.add(e);
    }

    public void setExpected(List<Response> rs) {
        expected = rs;
    }

    public List<Response> getExpected() {
        return expected;
    }

    @Override
    public String toString() {
        String ret = "<state status=\"" + status + "\">\n";
        ret += "\t<todo>\n";
        for (int i = 0; i < todoStack.size(); i++) {
            ret += "\t\t<goal name=\"" + todoStack.get(i).getName() + "\">\n";
        }
        ret += "\t</todo>\n";
        ret += "\t<done>\n";
        for (int i = 0; i < doneList.size(); i++) {
            Executable e = doneList.get(i);
            if (e instanceof Goal) {
                ret += "\t\t<goal name=\"" + ((Goal) e).getName() + "\">\n";
            } else if (e instanceof Step) {
                ret += "\t\t\t<step>\n";
            } else if (e instanceof Initiation) {
                ret += "\t\t\t<initiation concept=\"" + ((Initiation) e).getConcept().getLabel() + "\">\n";
            }
        }
        ret += "\t</done>\n";
        if (expected != null) {
            ret += "\t<expected>\n";
            for (int i = 0; i < expected.size(); i++) {
                ret += "\t\t<response name=\"" + expected.get(i).getConcept().getLabel() + "\">\n";
            }
            ret += "\t</expected>\n";
        }
        ret += "</state>";
        return ret;
    }

    public void log(String from, String message, boolean stateInfo) {
//        System.out.println("<" + from + ">\t\t" + message);
//        if (stateInfo) {
//            System.out.println("=============================");
//            System.out.println(this.toString());
//            System.out.println("=============================");
//        }
    }
}
