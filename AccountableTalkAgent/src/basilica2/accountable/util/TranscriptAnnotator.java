package basilica2.accountable.util;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import basilica2.accountable.listeners.AgreeDisagreeActor;
import basilica2.accountable.listeners.RevoiceActor;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.MessageAnnotator;
import basilica2.util.CSVReader;
import basilica2.util.MessageEventLogger;
import edu.cmu.cs.lti.project911.utils.log.LogUser;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class TranscriptAnnotator
{

	private static DateFormat transcriptFormat = new SimpleDateFormat("hh.mm.ss");
	private static MessageAnnotator mickey = new MessageAnnotator();
	private static RevoiceActor ricky = new RevoiceActor(null);
	private static AgreeDisagreeActor albert = new AgreeDisagreeActor(null);
	private static Logger logger = new Logger(new LogUser()
	{

		@Override
		public void receiveLoggerMessage(String msg)
		{
		}
	}, "PostHoc");

	public static List<Map<String, String>> annotate(File infile, String... ignoreAuthors)
	{


		List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
		List<String> blacklist = Arrays.asList(ignoreAuthors);
		// String infile = getProperties().getProperty("transcript");
		if (infile.exists())
		{
			rows = CSVReader.readCSV(infile);

			Iterator<Map<String, String>> rowIt = rows.iterator();
			
			
			while (rowIt.hasNext())
			{
				Map<String, String> row = rowIt.next();
				String author = row.get("AUTHOR");
				String text = row.get("TEXT");
				
				String[] annotations;
				String groupName;
				
				if(row.containsKey("ANNOTATIONS"))
					annotations = row.get("ANNOTATIONS").split("\\+");
				else 
					annotations = new String[0];
				
				
				if(row.containsKey("GROUP"))
					groupName = row.get("GROUP");
				else
				{
					groupName = infile.getName().replaceAll("(_message_annotations)?.csv", "");
				}
				
				MessageEventLogger.setDefaultName(groupName, false);

				String timeString = row.get("TIME");
				Date stamp;
				try
				{
					stamp = transcriptFormat.parse(timeString);
				}
				catch (ParseException e)
				{
					stamp = new Date();
					System.out.println("Unparseable Date: '"+timeString+"'");
					System.out.println(row);
				}

				MessageEvent me = new MessageEvent(null, author, text);

				if (!(blacklist.contains(author) || text.equals("joins the room") || text.equals("leaves the room")))
				{
					mickey.preProcessEvent(null, me);
					albert.preProcessEvent(null, me);
					ricky.preProcessEvent(null, me);
				}

				for(String a : annotations)
				{
					if(!a.isEmpty() && !a.equals("="))
					{
						me.addAnnotations(a);
					}
				}
				
				//if(me.hasAnnotations())
				//	logger.log("TranscriptAnnotator", Logger.LOG_NORMAL, me.getAnnotationString());
				MessageEventLogger.logMessageEvent(me, stamp, row.get("NOTE"));
			}
		}
		else
			System.err.println("no transcript file " + infile);

		return rows;
	}

	public static void main(String[] args)
	{
		File target = new File(args[0]);
		System.out.println(target + ": "+target.exists());
		if(target.exists())
		{
			if(target.isDirectory())
			{
				for(File f : target.listFiles())
				{
					annotate(f, "TUT");
					System.out.println("annotated the transcript at "+f);
				}
			}
			else
			{
				annotate(target, "TUT");
				System.out.println("annotated the transcript at "+target);
			}
		}
		System.exit(0);
	}

}
