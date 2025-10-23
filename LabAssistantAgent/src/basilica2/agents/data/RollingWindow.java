package basilica2.agents.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import basilica2.util.Timer;


public class RollingWindow implements TimeoutReceiver
{
	private static RollingWindow window = null;
	private static long start = Timer.currentTimeMillis();
	
	public static RollingWindow sharedWindow()
	{
		if(window == null)
			window = new RollingWindow();
		
		return window;
	}
	
	public class Entry
	{	
		public Event event;
		public long timestamp;
		public HashSet<String> keys = new HashSet<String>();
		
		public Entry(Event e, String... keys)
		{
			event = e;
			timestamp = System.currentTimeMillis();
			this.keys.add("all");
			Collections.addAll(this.keys, keys);
		}
		
		public String toString()
		{
			return (timestamp-start)/1000.0+": "+keys+" = "+event;
		}
	}
	
	private List<Entry> allEntries = new ArrayList<Entry>();
	private HashMap<Event, Entry> entryMap = new HashMap<Event, Entry>();
	private double windowSeconds;
	private double updateIntervalSeconds;
	private boolean rolling = false;
	
	public RollingWindow(double windowSeconds, double updateIntervalSeconds)
	{
		setWindowSize(windowSeconds, updateIntervalSeconds);
		setRolling(true);
	}
	
	/**
	 * default to a five-minute window.
	 */
	private RollingWindow()
	{
		setWindowSize(5*60, 30);
		setRolling(true);
	}
	
	public void setWindowSize(double windowSeconds, double updateIntervalSeconds)
	{
		this.windowSeconds = windowSeconds;
		this.updateIntervalSeconds = updateIntervalSeconds;
	}
	
	public boolean isRolling()
	{
		return rolling;
	}
	
	public void setRolling(boolean roll)
	{
		if(roll && !rolling)
			new Timer(updateIntervalSeconds, this).start();
		rolling = roll;
	}

	public int countEventsByTurns(int turnsAgo, String...keys)
	{		
		log(getClass().getSimpleName(),Logger.LOG_NORMAL, turnsAgo+" turn window");
		int count = 0;
		
		List<String> keyList = Arrays.asList(keys);
		
		for(int i = Math.max(0, allEntries.size() - turnsAgo); i < allEntries.size() ; i++)
		{
			Entry edwin = allEntries.get(i);
			if(edwin.keys.containsAll(keyList))
					count++;
		}

		return count;
	}
	
	public int countEvents(String...keys)
	{
		return countEvents(windowSeconds, keys);
	}
	
	public List<Entry> getEvents(String...keys)
	{
		return getEvents(windowSeconds, keys);
	}

	/**returns a list of events occurring in the last secondsAgo seconds matching ALL keys**/
	public List<Entry> getAnyEvents(double secondsAgo, String...keys)
	{
		long now = System.currentTimeMillis();
		long then = (long) (now - 1000*secondsAgo);

		log(getClass().getSimpleName(),Logger.LOG_NORMAL,(now-then)+" ms rolling window");
		List<String> keyList = Arrays.asList(keys);
		ArrayList<Entry> entries = new ArrayList<Entry>();
		
		for(int i = allEntries.size(); i > 0; i--)
		{
			Entry edwin = allEntries.get(i-1);
			if(edwin.timestamp >= then)
			{
				for(String k : keys)
				{
					if(edwin.keys.contains(k))
					{
						entries.add(edwin);
						break;
					}
				}
							
			}
			else
			{
				break;
			}
		}

		return entries;
	}
	
	/**returns a list of events occurring in the last secondsAgo seconds matching ALL keys**/
	public List<Entry> getEvents(double secondsAgo, String...keys)
	{
		long now = System.currentTimeMillis();
		long then = (long) (now - 1000*secondsAgo);

		log(getClass().getSimpleName(),Logger.LOG_NORMAL,(now-then)+" ms rolling window");
		List<String> keyList = Arrays.asList(keys);
		ArrayList<Entry> entries = new ArrayList<Entry>();
		
		for(int i = allEntries.size(); i > 0; i--)
		{
			Entry edwin = allEntries.get(i-1);
			if(edwin.timestamp >= then)
			{
				if(edwin.keys.containsAll(keyList))
					entries.add(edwin);
			}
			else
			{
				break;
			}
		}

		return entries;
	}

	/**returns a count of events occurring in the last secondsAgo seconds matching ALL keys**/
	public int countEvents(double secondsAgo, String...keys)
	{
		long now = System.currentTimeMillis();
		long then = (long) (now - 1000*secondsAgo);
		log(getClass().getSimpleName(),Logger.LOG_NORMAL,(now-then)+" ms window");
		int count = 0;
		
		List<String> keyList = Arrays.asList(keys);
		
		for(int i = allEntries.size(); i > 0; i--)
		{
			Entry edwin = allEntries.get(i-1);
			if(edwin.timestamp >= then)
			{
				if(edwin.keys.containsAll(keyList))
					count++;
			}
			else
			{
				break;
			}
		}

		return count;
	}
	
	/**returns a count of events occurring in the last secondsAgo seconds matching ALL keys**/
	public int countAnyEvents(double secondsAgo, String...keys)
	{
		long now = System.currentTimeMillis();
		long then = (long) (now - 1000*secondsAgo);
		log(getClass().getSimpleName(),Logger.LOG_NORMAL,(now-then)+" ms window");
		int count = 0;
		
		List<String> keyList = Arrays.asList(keys);
		
		for(int i = allEntries.size(); i > 0; i--)
		{
			Entry edwin = allEntries.get(i-1);
			if(edwin.timestamp >= then)
			{
				for(String k : keys)
					if(edwin.keys.contains(k))
					{
						count++;
						break;
					}
			}
			else
			{
				break;
			}
		}

		return count;
	}
	
	public void addEvent(Event e, String... keys)
	{
		if(entryMap.containsKey(e))
			Collections.addAll(entryMap.get(e).keys, keys);
		
		else
		{
			Entry edwin = new Entry(e, keys);
			allEntries.add(edwin);
			entryMap.put(e, edwin);
		}
	}
	
	/**
	 * remove all event entries older than secondsAgo seconds. 
	 * @param secondsAgo
	 */
	public void purge(double secondsAgo)
	{
		long then = (long) (System.currentTimeMillis() - 1000*secondsAgo);
		int newZero = 0;
		for(int i = allEntries.size(); i > 0; i--)
		{
			Entry edwin = allEntries.get(i-1);
			if(edwin.timestamp < then)
			{
				entryMap.remove(edwin.event);
			}
			else
			{
				newZero = i-1;
			}
		}
		if(newZero > 0)
		{
			allEntries = allEntries.subList(newZero, allEntries.size());
		}
	}

	public void timedOut(String id)
	{
		if(rolling)
		{
			purge(windowSeconds);
			new Timer(updateIntervalSeconds, this).start();
		}
	}

	public void log(String from, String level, String msg)
	{
	}
	
	
//	public static void main(String[] args) throws InterruptedException
//	{
//		RollingWindow window = RollingWindow.sharedWindow();
//		
//		window.setWindowSize(3, 0.5);
//		
//		System.out.println("Window Size = 3 seconds");
//		
//		for(int i = 1; i <= 50; i++)
//		{
//			Thread.sleep(100);
//			System.out.println(i/10.0+" seconds: "+window.countEvents("all") + "/"+i+" events");
//			window.addEvent(new MessageEvent(null, "me", "tick "+i));
//		}
//		System.out.println(window.getEvents("all").size());
//		window.setRolling(false);
//	}
}
