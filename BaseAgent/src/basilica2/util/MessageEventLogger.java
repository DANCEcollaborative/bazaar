package basilica2.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.StringEscapeUtils;

import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.events.MessageEvent;

public class MessageEventLogger
{
	private static HashMap<String, PrintWriter> agentLogs = new HashMap<String, PrintWriter>();
	private static String defaultAgentName = "Agent";
	private static boolean stampTime;

	public static void logMessageEvent(MessageEvent me)
	{
		Date now = new Date(me.getTimestamp());
		logMessageEvent(me, now, System.currentTimeMillis()+"");

	}
	
	public static void logMessageEvent(MessageEvent me, String comment)
	{
		Date now = new Date(me.getTimestamp());
		logMessageEvent(me, now, comment);

	}

	public static void logMessageEvent(MessageEvent me, Date now, String comment)
	{
		String agentName = defaultAgentName;
		if(me.getSender() != null)
			agentName = me.getSender().getAgent().getName();
		
		logMessageEvent(me, now, agentName, comment);
	}

	public static void logMessageEvent(MessageEvent me, Date now, String agentName, String comment)
	{
		PrintWriter agentLog = getLogger(agentName);
		if(agentLog != null)
		{
			String text = sanitize(me.getText());
			//System.out.printf("%1$td.%1$tm.%1$tY,%1$tH.%1$tM.%1$tS,%2$s,%3$s,%4$s\n", 
			//		now, me.getFrom(), text, me.getAnnotationString());
			agentLog.printf("%1$tY-%1$tm-%1$td,%1$tH:%1$tM:%1$tS,%2$d,%3$s,%4$s,%5$s,%6$s\n", 
					now, me.getTypingDuration(), me.getFrom(), comment, text, me.getAnnotationString());
			System.out.printf("%1$tY-%1$tm-%1$td,%1$tH:%1$tM:%1$tS,%2$d,%3$s,%4$s,%5$s,%6$s\n", 
					now, me.getTypingDuration(), me.getFrom(), comment, text, me.getAnnotationString());
			agentLog.flush();
		}
	}

	private static PrintWriter getLogger(String agentName)
	{
		if(agentLogs.containsKey(agentName))
			return agentLogs.get(agentName);
		else
		try
		{
			String timeStamp = "";
			if(stampTime)
				timeStamp = "_"+String.format("%1$tY-%1$tm-%1$td_%1$tH%1$tM%1$tS", new Date());
			File logFile = new File("logs/"+agentName+timeStamp+"_message_annotations"+".csv");
			boolean appending = logFile.exists();
			PrintWriter agentLog = new PrintWriter(new BufferedWriter(new FileWriter(logFile, true)));
			if(appending)
				agentLog.println("DATE,TIME,TYPING_DURATION,AUTHOR,NOTE,TEXT,ANNOTATIONS");
			agentLogs.put(agentName, agentLog);
			return agentLog;
		}
		catch (IOException e)
		{
			System.err.println("can't write agent behavior log file "+agentName+": "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	private static String sanitize(Object o) {
		if(null == o)
			return "";
		
		else 
		{
			String note = o.toString();
		
			/*if (note.contains(",") || note.contains("\""))
			{
				note=note.replaceAll("\"", "\"\"");
				note = "\""+note+"\"";
				
			}*/
			
			note = StringEscapeUtils.escapeCsv(note);
			
			return note;
		}
	}

	public static void setDefaultName(String name, boolean useTimeStamp)
	{
		defaultAgentName = name;
		stampTime = useTimeStamp;
	}

}
