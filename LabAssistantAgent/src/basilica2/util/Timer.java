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
package basilica2.util;

import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;

public class Timer extends Thread 
{

    double timeout;
    TimeoutReceiver myReceiver;
    double timeLeft;
    boolean over;
    String timerId;
    
	private static long millisPerSecond = 1000;
	private static double scale = 1.0;
	
	public static long currentTimeMillis()
	{
		return (long) (System.currentTimeMillis()*scale);
	}

	public static void setTimeScale(double scale)
	{
		Timer.scale = scale;
		millisPerSecond = (long)(1000/scale);
	}
	
    public Timer(double t, String id, TimeoutReceiver r) {
        timeout = t;
        timerId = id;
        myReceiver = r;
        over = false;
    }

    public Timer(double t, TimeoutReceiver r) {
        timeout = t;
        myReceiver = r;
        over = false;
    }

    @Override
    public void run() {
        timeLeft = timeout;

        while (timeLeft > 0) {
            double time = (timeLeft > 1.0) ? 1.0 : timeLeft;

            try {
                Thread.sleep(Math.round(time * millisPerSecond));
            } catch (InterruptedException e) {
                return;
            }

            timeLeft -= time;

            if (over) 
            {
                break;
            }
        }

        if (over) {
            return;
        }

        myReceiver.timedOut(timerId);
    }

    public String getTimerId() {
        return timerId;
    }

    public void restart() {
        timeLeft = timeout;
    }

    public void stopAndQuit() {
        over = true;
    }

    public double getTimeLeft() {
        return timeLeft;
    }
}
