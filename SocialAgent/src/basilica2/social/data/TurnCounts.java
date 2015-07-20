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
package basilica2.social.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;

/**
 * 
 * @author rohitk
 */
public class TurnCounts
{

	private static TurnCounts sharedTurnCounts;
	private int totalTutorTurns = 1;
	private int socialTurns = 0;
	private List<String> requesters;
	private Map<String, Integer> dialogAnswerers;

	public TurnCounts(int total, int social)
	{
		totalTutorTurns = total;
		socialTurns = social;
		requesters = new ArrayList<String>();
		dialogAnswerers = new HashMap<String, Integer>();
	}

	public void incrementSocialCount()
	{
		socialTurns++;
	}

	public void incrementTotalCount()
	{
		totalTutorTurns++;
	}

	public int getSocialTurnCount()
	{
		return socialTurns;
	}

	public int getTotalTurnCount()
	{
		return totalTutorTurns;
	}

	public int getTaskTurnCount()
	{
		return totalTutorTurns - socialTurns;
	}

	public void addRequester(String r)
	{
		requesters.add(r);
	}

	public String getLastRequester()
	{
		if (requesters.size() > 0) { return requesters.get(requesters.size() - 1); }

		return null;
	}

	public void addAnswerer(String a)
	{
		Integer i = dialogAnswerers.get(a);
		if (i != null)
		{
			i++;
		}
		else
		{
			i = new Integer(1);
		}
		dialogAnswerers.put(a, i);
	}

	public void resetAnswerers()
	{
		dialogAnswerers.clear();
	}

	public Map<String, Integer> getDialogAnswerer()
	{
		return dialogAnswerers;
	}

	public static TurnCounts copy(TurnCounts t)
	{
		TurnCounts tc = new TurnCounts(t.getTotalTurnCount(), t.getSocialTurnCount());
		tc.addRequester(t.getLastRequester());
		tc.dialogAnswerers = t.dialogAnswerers;
		return tc;
	}

	@Override
	public String toString()
	{
		return "<TurnCounts social=\"" + socialTurns + "\" total=\"" + totalTutorTurns + "\" lastRequester=\"" + getLastRequester() + "\" />";
	}

	public static TurnCounts getTurnCounts()
	{
		if (sharedTurnCounts == null)
		{
			sharedTurnCounts = new TurnCounts(0, 0);
		}
		return TurnCounts.copy(sharedTurnCounts);
	}
	
	public static void commitTurnCounts(TurnCounts tc)
	{
		sharedTurnCounts = tc;
	}
}
