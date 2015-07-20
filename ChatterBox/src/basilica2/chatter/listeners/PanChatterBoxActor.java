package basilica2.chatter.listeners;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.*;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.util.CSVReader;
import basilica2.util.Timer;

public class PanChatterBoxActor extends BasilicaAdapter
{
	private List<Map<String, String>> rows = new ArrayList<Map<String,String>>();
	private boolean started;
	private long offset = 0;
	private Date zero;
	private DateFormat transcriptFormat = new SimpleDateFormat("hh.mm.ss");
	private long start;
	private String agentName;
	
	public PanChatterBoxActor(Agent a)
	{
		super(a);
		
		//String path = getProperties().getProperty("transcript");
		String path = System.getProperty("chatterbox.transcript");
		File fp = new File(path);
		if(fp.exists())
		{
			rows = CSVReader.readCSV(fp);
			agentName = a.getName();
			
			try
			{
				zero = transcriptFormat.parse(rows.get(0).get("TIME"));
				start = Long.parseLong(System.getProperty("chatterbox.start_time", ""+Timer.currentTimeMillis()));
				
				offset = start - zero.getTime();
				System.out.println(agentName+"\tZERO:\t"+zero.getTime());
				System.out.println(agentName+"\tSTART:\t"+start);
				System.out.println(agentName+"\tOFFSET:\t"+offset);
			}
			catch (Exception e)
			{e.printStackTrace();}
			
		}
	}

	@Override
	public void processEvent(final InputCoordinator source, Event event)
	{
		if(!started && !rows.isEmpty())
		{
			started = true;
			

			double time = getTimeForRow(0);
			
			new Timer(time, new TimeoutReceiver()
			{
				int i = 0;
				public void timedOut(String index)
				{
					Map<String, String> row = rows.get(i);
					String text = row.get("TEXT");
					System.out.println(agentName+":\tROW "+i+" TEXT="+text);
					source.pushEventProposal(new MessageEvent(source, row.get("AUTHOR"), text));
					i++;

					if(i < rows.size())
					{
						double time = getTimeForRow(i);
						new Timer(time, this).start();
					}
				}

				@Override
				public void log(String from, String level, String msg)
				{}
			}).start();
			
		}
	}

	private double getTimeForRow(int i)
	{
		try
		{
			double time=(offset + transcriptFormat.parse(rows.get(i).get("TIME")).getTime() - Timer.currentTimeMillis())/1000.0;
			System.out.println(agentName+":\tTIME UNTIL ROW "+i+":\t"+time);
			return time;
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 10; //if we can't figure out when to do it, do it in 10 seconds.
	}

	@Override
	public Class[] getListenerEventClasses()
	{return new Class[]{LaunchEvent.class};}


	
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{}

	@Override
	public Class[] getPreprocessorEventClasses()
	{return new Class[]{};}


}
