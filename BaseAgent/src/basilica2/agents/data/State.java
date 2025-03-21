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
package basilica2.agents.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDateTime;

import basilica2.agents.events.PoseEvent.poseEventType;
import java.util.Collections;
/**
 * 
 * @author rohitk
 */
public class State
{

	public class Student
	{

		public String chatId;
		public String name;
		public String role = null; 
		public Set previousRoles = new HashSet();
		public int activityMetric = 0; 
		public boolean isPresent;
		public String speech;
		public String identity;
		public String location = null;
		public String facialExp;
		public poseEventType pose;
		public String emotion;
		@Override
		public String toString()
		{
			return ("<student id=\"" + chatId + "\" name=\"" + name + "\" role=\"" + role + "\" present=\"" + isPresent + "\" />");
		}
	}

	private List<Student> students = new ArrayList<Student>();
	private List<String> roles = new ArrayList<String>();
	private ArrayList<Student> randomizedStudentList = new ArrayList<Student>();
	private int nextStudentIndex; 
	private boolean initiated = false; 
	private String stageName = null;
	private String stageType = null;
	private String stepName = null;
	private String stepType = null;
	private poseEventType groupPose = poseEventType.none;
	private String identityAllUsers = "group";
	private int jointActivityMetric = 0; 
	private Boolean multimodalDontListenWhileSpeaking = true; 
	private LocalDateTime multimodalDontListenEnd = null; 
	public String globalActiveListener = "";

	// public String conceptId;
	// public String conceptExecutionStatus;

	private Map<String, Object> stateMap = new HashMap<String, Object>();

	public static State copy(State s)
	{
//		System.err.println("State.java, copy -- COPYING STATE");
		State news = new State();

		news.initiated = s.initiated;
		news.stageName = s.stageName;
		news.stageType = s.stageType;
		news.stepName = s.stepName;
		news.stepType = s.stepType;
		news.groupPose = s.groupPose;
			
		news.multimodalDontListenWhileSpeaking = s.multimodalDontListenWhileSpeaking;
		news.multimodalDontListenEnd = s.multimodalDontListenEnd;
		news.globalActiveListener = s.globalActiveListener;

		Map<String, Object> map = s.more();
		for (String k : map.keySet())
			news.stateMap.put(k, map.get(k));
		// news.conceptId = s.conceptExecutionStatus;
		// news.conceptExecutionStatus = s.conceptExecutionStatus;

		for (int i = 0; i < s.students.size(); i++)
		{
			news.students.add(s.students.get(i));
		}

		for (int i = 0; i < s.randomizedStudentList.size(); i++)
		{
			news.randomizedStudentList.add(s.randomizedStudentList.get(i));
		}

		for (int i = 0; i < s.roles.size(); i++)
		{
			news.roles.add(s.roles.get(i));
		}

		return news;
	}

	public Map<String, Object> more()
	{
		return stateMap;
	}

	public void initiate()
	{
		initiated = true;
	}

	public void setStepInfo(String stageName, String stageType, String stepName, String type)
	{
		this.stageName = stageName;
		this.stageType = stageType;
		this.stepName = stepName;
		this.stepType = type;
	}

	public void addStudent(String sid)
	{
//		System.err.println("===== State,addStudent - sid: " + sid); 
		if (!sid.contentEquals(identityAllUsers)) {
			Student s = new Student();
			boolean found = false;
			for (int i = 0; i < students.size(); i++)
			{
				if (sid.startsWith(students.get(i).chatId))
				{
					found = true;
					s = students.get(i);
				}
			}
			if (!found)
			{
				s.chatId = sid;
				s.name = sid;
				s.role = "UNASSIGNED";
				students.add(s);
			}
			s.isPresent = true;			
		}
	}

	public void removeStudent(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			if (s.chatId.equalsIgnoreCase(sid))
			{
				s.isPresent = false;
			}
		}
	}

	public String getStudentName(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			// if (s.isPresent)				// No need to check if student is present to return name
			if (true)
			{
				if (s.chatId.equalsIgnoreCase(sid)) { return s.name; }
			}
		}
		return sid;
	}

	public void setName(String sid, String name)
	{
//		System.err.println("===== State,setName - sid: " + sid + " -- name: " + name); 
		if (!sid.equals(identityAllUsers)) {
			for (int i = 0; i < students.size(); i++)
			{
				if (sid.startsWith(students.get(i).chatId))
				{
					students.get(i).name = name;
					return;
				}
			}
			addStudent(sid);
			setName(sid, name);			
		}
	}
	
	public void setStudentPose(String sid, poseEventType pose) {
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				students.get(i).pose = pose;
				return;
			}
		}
	}

	public poseEventType getStudentPose(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			if (s.chatId.equalsIgnoreCase(sid))
			{
//				System.out.println("State.java, getPose - sid/chatId: " + sid + " - pose: " + s.pose.toString());
				return s.pose;
			}
		}
		return null;
	}

	public String getLocation(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			if (s.chatId.equalsIgnoreCase(sid))
			{
//				System.out.println("State.java, getLocation - sid/chatId: " + sid + " - Location: " + s.location);
				return s.location;
			}
		}
		return null;
	}

	public void setLocation(String sid, String location)
	{
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				students.get(i).location = location;
//				System.out.println("State.java, setLocation - sid/chatId: " + sid + " - Location: " + location);
			}
		}
	}

	public void addRole(String role)
	{
		// System.out.println("===== State,addRole: " + role); 
		roles.add(role); 	
	}

	public void setRoles(String[] roles)
	{
		for (int i = 0; i < roles.length; i++)
		{
			addRole(roles[i]); 
			// System.out.println("State, setRoles: Added role " + roles[i]); 
		}
	}

	public int getNumRoles()
	{
		return roles.size(); 
	}

	public void setStudentRole(String sid, String role)
	{
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				students.get(i).role = role;
				students.get(i).previousRoles.add(role);
			}
		}
	}

	public String getStudentRole(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				return students.get(i).role; 
			}
		}
		return sid; 
	}
	
	public Set getStudentPreviousRoles(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				return students.get(i).previousRoles; 
			}
		}
		return Collections.emptySet();
	}
	
	
	public String getRolesString()
	{
		String roleString = "";
		for (String role : roles)
		{
			roleString += role + ", ";
		}
		if(roleString.length() > 2)
			roleString = roleString.substring(0, roleString.length() - 2);
		return roleString;
	}
	
	public int getStudentCount()
	{
		int c = 0;
		for (int i = 0; i < students.size(); i++)
		{
			if (students.get(i).isPresent)
			{
				c++;
			}
		}
		return c;
	}

	public String[] getStudentIds()
	{
		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < students.size(); i++)
		{
			if (students.get(i).isPresent)
			{
				ids.add(students.get(i).chatId);
			}
		}
		return ids.toArray(new String[0]);
	}

	public String[] getStudentIdsPresentOrNot()
	{
		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < students.size(); i++)
		{
			// if (students.get(i).isPresent)
			if (true)
			{
				ids.add(students.get(i).chatId);
			}
		}
		return ids.toArray(new String[0]);
	}

	public List<String> getStudentIdList()
	{
		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < students.size(); i++)
		{
			if (students.get(i).isPresent)
			{
				ids.add(students.get(i).chatId);
			}
		}
		return ids;
	}

	public String[] getRandomizedStudentIds()
	{
		setRandomizedStudentList(); 
		String[] ids = new String[this.randomizedStudentList.size()]; 
//		System.err.println("getRandomizedStudentIds(), this.randomizedStudentList.size() = " + String.valueOf(randomizedStudentList.size()));
		for (int i = 0; i < this.randomizedStudentList.size(); i++)
		{
//			System.err.println("getRandomizedStudentIds(), adding id " + this.randomizedStudentList.get(i).chatId);
//			System.err.println("getRandomizedStudentIds(), adding id for name " + this.randomizedStudentList.get(i).name);
			ids[i] = (this.randomizedStudentList.get(i).chatId);
//			if (students.get(i).isPresent)
//			{
//				ids[i] = (randomizedStudentList.get(i).chatId);
//			}
		}
//		System.err.println("getRandomizedStudentIds, returning ids = " Array.toString(ids));
//		for (int i = 0; i < ids.length; i++) {
//			System.err.println("getRandomizedStudentIds(), ids[" + String.valueOf(i) + "] = " + ids[i]); 
//		}
		return ids;
	}

	public void setRandomizedStudentList()
	{	
//		System.err.println("Student names: " + Arrays.toString(getStudentNames().toArray()));
		this.randomizedStudentList.clear(); 
		for (int i = 0; i < students.size(); i++)
		{
//			this.randomizedStudentList.add(students.get(i)); 
			if (students.get(i).isPresent)
			{
				this.randomizedStudentList.add(students.get(i)); 
			} else {
//				System.err.println("State, setRandomizedStudentList: student not present - chatId:" + students.get(i).chatId + "   - name:" + students.get(i).name);
			}
		}
		Collections.shuffle(this.randomizedStudentList);
		setNextStudentIndex(0); 
//		System.err.println("Randomized student names: " + Arrays.toString(getRandomizedStudentNames().toArray()));
	}

	public List<String> getStudentNames()
	{
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < students.size(); i++)
		{
			if (students.get(i).isPresent)
			{
				names.add(students.get(i).name);
			}
		}
		return names;
	}

	public List<String> getRandomizedStudentNames()
	{
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < randomizedStudentList.size(); i++)
		{
			if (randomizedStudentList.get(i).isPresent)
			{
				names.add(randomizedStudentList.get(i).name);
			}
		}
		return names;
	}
	
	public List<String> getAllStudentNames()
	{
		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < students.size(); i++)
		{
			ids.add(students.get(i).name);
		}
		return ids;
	}

	public String[] getStudentNamesByIds(String[] ids)
	{
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < ids.length; i++)
		{
			names.add(getStudentName(ids[i]));
		}
		return names.toArray(new String[0]);
	}

	public List<String> getStudentNamesByIdList(List<String> ids)
	{
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < ids.size(); i++)
		{
			names.add(getStudentName(ids.get(i)));
		}
		return names;
	}

	public String getStudentNamesString()
	{
		return getStudentNamesString(Arrays.asList(this.getStudentIds()));
	}

	public String getStudentNamesString(List<String> users)
	{
		String name = "";
		for (String id : users)
		{
			name += this.getStudentName(id) + ", ";
		}
		if(name.length() > 2)
			name = name.substring(0, name.length() - 2);
		return name;
	}

	public void setNextStudentIndex(int index)
	{
		this.nextStudentIndex = index; 
	}

	public int getNextStudentIndex()
	{
		return this.nextStudentIndex; 
	}

	public int advanceStudentIndex()
	{
		int nextIndex = this.nextStudentIndex + 1;
//		System.err.println("State.java, advanceStudentIndex: initial nextIndex = " + String.valueOf(nextIndex)); 
		if (nextIndex == students.size())
		{
			nextIndex = 0; 
		}
		setNextStudentIndex(nextIndex); 
//		System.err.println("State.java, advanceStudentIndex: final nextIndex = " + String.valueOf(nextIndex)); 
		return nextIndex; 
	}

	public Student getStudentById(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			if (s.isPresent)
			{
				if (s.chatId.equalsIgnoreCase(sid)) { return s; }
			}
		}
		return null;
	}

	public String getStudentByRole(String role)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			if (s.isPresent)
			{
				if (s.role.equalsIgnoreCase(role)) { return s.chatId; }
			}
		}
		return null;
	}


	public void setStudentActivityMetric(String sid, int metric)
	{
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				students.get(i).activityMetric = metric;
			}
		}
	}

	public int getStudentActivityMetric(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				return students.get(i).activityMetric; 
			}
		}
		return 0; 
	}

	public void setJointActivityMetric(int metric)
	{
		jointActivityMetric = metric; 
	}

	public int getJointActivityMetric()
	{
		return jointActivityMetric; 
	}

	public String getStageName()
	{
		if (stageName == null) { return ""; }
		return stageName;
	}

	public String getStageType()
	{
		if (stageType == null) { return ""; }
		return stageType;
	}

	public String getStepName()
	{
		if (stepName == null) { return ""; }
		return stepName;
	}

	public poseEventType getGroupPose()
	{
		return groupPose;
	}

	public void setGroupPose(poseEventType pose)
	{
		this.groupPose = pose;
	}

	public void setMultimodalDontListenWhileSpeaking(Boolean dontListenWhileSpeaking)
	{
		this.multimodalDontListenWhileSpeaking = dontListenWhileSpeaking;
	}

	public Boolean getMultimodalDontListenWhileSpeaking()
	{
		return multimodalDontListenWhileSpeaking;
	}

	public LocalDateTime getMultimodalDontListenWhileSpeakingEnd()
	{
		return multimodalDontListenEnd;
	}

	public void setMultimodalDontListenWhileSpeakingEnd(LocalDateTime dontListenEnd)
	{
		this.multimodalDontListenEnd = dontListenEnd;
	}
	
	public void setGlobalActiveListener(String name) {
		globalActiveListener = name;
	}
	
	public String getGlobalActiveListener() {
		return globalActiveListener;
	}
	


	@Override
	public String toString()
	{
		String ret = "<State students=\"" + students.size() + "\" initiated=\"" + initiated + "\">\n";
		ret += "\t<Stage name=\"" + stageName + "\" type=\"" + stageType + "\" />\n";
		ret += "\t<Step name=\"" + stepName + "\" type=\"" + stepType + "\"/>\n";
		for (int i = 0; i < students.size(); i++)
		{
			ret += ("\t" + students.get(i).toString());
		}
		ret += "</State>";
		return ret;
	}
}
