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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		public String role;
		public boolean isPresent;
		public String speech;
		public String identity;
		public String location = null;
		public String facialExp;
		public String bodyPos;
		public String emotion;

		@Override
		public String toString()
		{
			return ("<student id=\"" + chatId + "\" name=\"" + name + "\" role=\"" + role + "\" present=\"" + isPresent + "\" />");
		}
	}

	private List<Student> students = new ArrayList<Student>();
	private boolean initiated = false;
	private String stageName = null;
	private String stageType = null;
	private String stepName = null;
	private String stepType = null;
	// public String conceptId;
	// public String conceptExecutionStatus;

	private Map<String, Object> stateMap = new HashMap<String, Object>();

	public static State copy(State s)
	{
		State news = new State();

		news.initiated = s.initiated;
		news.stageName = s.stageName;
		news.stageType = s.stageType;
		news.stepName = s.stepName;
		news.stepType = s.stepType;
		Map<String, Object> map = s.more();
		for (String k : map.keySet())
			news.stateMap.put(k, map.get(k));
		// news.conceptId = s.conceptExecutionStatus;
		// news.conceptExecutionStatus = s.conceptExecutionStatus;

		for (int i = 0; i < s.students.size(); i++)
		{
			news.students.add(s.students.get(i));
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
			if (s.isPresent)
			{
				if (s.chatId.equalsIgnoreCase(sid)) { return s.name; }
			}
		}
		return sid;
	}

	public void setName(String sid, String name)
	{
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

	public String getLocation(String sid)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			if (s.isPresent)
			{
				System.out.println("State.java, getLocation - sid/chatId: " + sid + " - Location: " + s.location);
				if (s.chatId.equalsIgnoreCase(sid)) { return s.location; }
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
				System.out.println("State.java, setLocation - sid/chatId: " + sid + " - Location: " + location);
			}
		}
	}

	public void setRole(String sid, String role)
	{
		for (int i = 0; i < students.size(); i++)
		{
			if (sid.startsWith(students.get(i).chatId))
			{
				students.get(i).role = role;
			}
		}
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

	public List<String> getStudentNames()
	{
		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < students.size(); i++)
		{
			if (students.get(i).isPresent)
			{
				ids.add(students.get(i).name);
			}
		}
		return ids;
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

	public Student getStudentByRole(String role)
	{
		for (int i = 0; i < students.size(); i++)
		{
			Student s = students.get(i);
			if (s.isPresent)
			{
				if (s.role.equalsIgnoreCase(role)) { return s; }
			}
		}
		return null;
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
