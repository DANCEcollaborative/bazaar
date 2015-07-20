
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

import javax.swing.JFileChooser;

import org.apache.commons.lang3.StringEscapeUtils;

import basilica2.accountable.listeners.AgreeDisagreeActor;
import basilica2.accountable.listeners.AskForRestateActor;
import basilica2.accountable.listeners.PressForReasoningActor;
import basilica2.accountable.listeners.RevoiceActor;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.MessageAnnotator;
import basilica2.util.CSVReader;
import basilica2.util.MessageEventLogger;
import edu.cmu.cs.lti.project911.utils.log.LogUser;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class TranscriptAnnotator
{

	private static DateFormat timeFormat = new SimpleDateFormat("YYYY-MM-DD hh:mm:ss");
	private static DateFormat fallbackTimeFormat = new SimpleDateFormat("DD.MM.YYYY hh.mm.ss");
	private static MessageAnnotator mickey = new MessageAnnotator();
	private static RevoiceActor ricky = new RevoiceActor(null);
	private static AgreeDisagreeActor albert = new AgreeDisagreeActor(null);
	private static PressForReasoningActor preston = new PressForReasoningActor(null);
	private static AskForRestateActor alice = new AskForRestateActor(null);
	private static PostHocPromptMatcher coulson = new PostHocPromptMatcher("accountable/accountable_prompts.xml");
	
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
		ArrayList<String> empty = new ArrayList<String>();

		
		// String infile = getProperties().getProperty("transcript");
		if (infile.exists())
		{
			rows = CSVReader.readCSV(infile);

			Iterator<Map<String, String>> rowIt = rows.iterator();
			String groupName = infile.getName().replaceAll("\\.csv", "");
			//groupName = groupName.replace("chem-f12-", "");
			groupName = groupName.replace("-", "");
			
			MessageEventLogger.setDefaultName("PostHoc "+groupName, false);
			
			while (rowIt.hasNext())
			{
				Map<String, String> row = rowIt.next();
				String author = row.get("AUTHOR");
				String text = row.get("TEXT");
				String type = row.get("TYPE");
				if(type == null)
					type = "text";

				String timeString = row.get("DATE")+" "+row.get("TIME");
				Date stamp;
				try
				{
					stamp = timeFormat.parse(timeString);
				}
				catch (ParseException e)
				{
					try
					{
						stamp = fallbackTimeFormat.parse(timeString);
					}
					catch (ParseException p)
					{
						stamp = new Date();
						p.printStackTrace();
					}
				}

				MessageEvent me = new MessageEvent(null, author, text);

				text = text.replaceAll("LINEBREAK", "\n");
				text = StringEscapeUtils.unescapeHtml4(text); //TODO: add this to incoming chat
				me.setText(text);
				
				if(!text.equals("joins the room") && !text.equals("leaves the room"))
				{
					if (!blacklist.contains(author))
					{
						mickey.preProcessEvent(null, me);
						ricky.preProcessEvent(null, me);
						albert.preProcessEvent(null, me);
						preston.preProcessEvent(null, me);
						alice.preProcessEvent(null, me);
					}
					else
					{
						String match = coulson.match(text);
						if(match != null)
						{
							me.addAnnotation(match, empty);
							me.addAnnotation("AT_MOVE", empty);
						}
					}
				}
				
				me.addAnnotation("TYPE="+type, empty);
				
				//if(me.hasAnnotations())
				//	logger.log("TranscriptAnnotator", Logger.LOG_NORMAL, me.getAnnotationString());
				MessageEventLogger.logMessageEvent(me, stamp, groupName);
			}
		}
		else
			System.err.println("no transcript file " + infile);

		return rows;
	}

	public static void main(String[] args)
	{
		System.out.println("Annotates a given transcript file using the Accountable Talk preprocessors."
				+ "\n Typically, you'll set the working directory for this project"
				+ "\n to the runtime/ directory of some agent,"
				+ "\n to use its particular configuration."
				+ "\n the annotations will be written to the agent's runtime/logs/ directory.");
		File target = null;
		if(args.length < 1)
		{
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int status = chooser.showOpenDialog(null);
			if(status == JFileChooser.APPROVE_OPTION)
			{
				target = chooser.getSelectedFile();
			}
		}
		else
		{
			target = new File(args[0]);
		}
		
		if(target != null && target.exists())
		{
			if(target.isDirectory())
			{
				for(File f : target.listFiles())
				{
					if(f.getName().endsWith("csv"))
					{
						annotate(f, "TUTOR", "Sage", "System");
						System.out.println("annotated the transcript at "+f);
					}
				}
			}
			else
			{
				annotate(target, "TUTOR", "Sage", "System");
				System.out.println("annotated the transcript at "+target);
			}
		}
		System.exit(0);
	}

}
