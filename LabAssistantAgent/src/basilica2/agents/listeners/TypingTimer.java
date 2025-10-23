package basilica2.agents.listeners;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.TypingEvent;
import edu.cmu.cs.lti.basilica2.core.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;

public class TypingTimer implements BasilicaPreProcessor
{
	private Map<String, List<Date>> typingHistory = new TreeMap<String, List<Date>>(); 
	
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		if(event instanceof TypingEvent)
		{
			TypingEvent typing = (TypingEvent)event;
			String user = typing.getUser();
			if(!typingHistory.containsKey(user))
			{
				typingHistory.put(user, new ArrayList<Date>());
			}
			typingHistory.get(user).add(new Date());
		}
		
		if(event instanceof MessageEvent)
		{
			MessageEvent message = (MessageEvent)event;
			String user = message.getFrom();
			if(typingHistory.containsKey(user))
			{
				Date now = new Date();
				List<Date> userHistory = typingHistory.get(user);
				long millis = 0;
				if(!userHistory.isEmpty())
				{
					millis = now.getTime() - userHistory.get(0).getTime();
					userHistory.clear();
					message.setTypingDuration(millis);
				}
			}
		}
	}

	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{TypingEvent.class, MessageEvent.class};
	}

}
