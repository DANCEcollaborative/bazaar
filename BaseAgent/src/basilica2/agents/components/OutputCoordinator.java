package basilica2.agents.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smack.packet.Message;

import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PrivateMessageEvent;
import basilica2.agents.events.priority.AbstractPrioritySource;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.util.MessageEventLogger;
import basilica2.util.Timer;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import smartlab.communication.CommunicationManager; 

// import VHjava.VHSender;
// import VHjava.VHReceiver;
// import VHjava.VHjava.*;
// import VHjava.VHJava.MessageProcessor;
// import MessageProcessor;
// import VHjava.MessageProcessor; 
// import VHjava.RendererController;
// import VHjava.*; 

/**
 * @author dadamson
 */
public class OutputCoordinator extends Component implements TimeoutReceiver
{
	public static final int HISTORY_SIZE = 3;

	final private ArrayList<PriorityEvent> proposalQueue = new ArrayList<PriorityEvent>();
	private double delay = 2.0;
	private ArrayList<AbstractPrioritySource> recentSources = new ArrayList<AbstractPrioritySource>();
	private Map<String, AbstractPrioritySource> activeSources = new HashMap<String, AbstractPrioritySource>();

	public static final double LEAST_PRIORITY = 0.001;
	public static final double LOW_PRIORITY = 0.25;
	public static final double MEDIUM_PRIORITY = 0.5;
	public static final double HIGH_PRIORITY = .75;
	public static final double HIGHEST_PRIORITY = 1.0;
	
	private Boolean outputToPSI = false; 
	CommunicationManager psiCommunicationManager; 
	private String bazaarToPSITopic = "Bazaar_PSI_Text";
	
	public OutputCoordinator(Agent agent, String s1, String s2)
	{
		// s1 = name; s2 = properties file name
		super(agent, s1, s2);
		Timer timer = new Timer(delay, "Output Queue", this);
		timer.start();
		
		if(myProperties!=null)
			try{outputToPSI = Boolean.parseBoolean(myProperties.getProperty("output_to_PSI", "false"));}
			catch(Exception e) {e.printStackTrace();}
		if (outputToPSI) {
			initializePSI(); 
		}
	}
	
	private void initializePSI() {
		psiCommunicationManager = new CommunicationManager();
	}

	public void addAll(Collection<PriorityEvent> events)
	{
		synchronized (proposalQueue)
		{
			proposalQueue.addAll(events);
			log(Logger.LOG_NORMAL, "Added to Proposal Queue: " + events);
		}
	}

	@Override
	protected void processEvent(Event event)
	{
		throw new UnsupportedOperationException("Not supported anymore.");
	}

	@Override
	public String getType()
	{
		return "COORDINATOR";
	}

	public void timedOut(String id)
	{
		synchronized (proposalQueue)
		{
			if (!proposalQueue.isEmpty())
			{

				log(Logger.LOG_LOW, "Proposal Queue: " + proposalQueue);
				cleanUp();

				PriorityEvent best = null;
				double bestBelief = 0;

				for (PriorityEvent p : proposalQueue)
				{
					double belief = beliefGivenHistory(p);
					double d = belief * p.getPriority();

					log(Logger.LOG_LOW, "Proposal " + p + "belief*priority: " + belief + "*" + p.getPriority() + "=" + d);

					if (d > 0 && (d > bestBelief))// || (bestBelief - d) < Math.random() / 100.0))
					{
						best = p;
						bestBelief = d;
					}

				}

				if (best != null)
				{
					best.getCallback().accepted(best);
					publishEvent(best.getEvent());
					proposalQueue.remove(best);

					AbstractPrioritySource source = best.getSource();

					activeSources.put(source.getName(), source);

					if (recentSources.size() > HISTORY_SIZE) recentSources.remove(0);

					recentSources.add(source);
				}
			}
		}

		new Timer(delay, "Output Queue", this).start();
	}

	private void cleanUp()
	{
		Iterator<PriorityEvent> pit = proposalQueue.iterator();
		long now = Timer.currentTimeMillis();

		synchronized (proposalQueue)
		{
			while (pit.hasNext())
			{
				PriorityEvent p = pit.next();

				if (p.getInvalidTime() < now)
				{
					p.getCallback().rejected(p);
					pit.remove();

					// String sourceName = p.getSource().getName();
					// if(activeSources.containsKey(sourceName))
					// {
					// activeSources.remove(sourceName); //this is weird -
					// presumes only one active source of the same name at once.
					// }
				}
			}

			Iterator<String> pat = new ArrayList<String>(activeSources.keySet()).iterator();
			while (pat.hasNext())
			{
				String key = pat.next();
				AbstractPrioritySource source = activeSources.get(key);

				if (!source.isBlocking())
				{
					activeSources.remove(key);
				}
			}
		}
	}

	protected void publishEvent(Event e)
	{

		if (e instanceof MessageEvent)
		{
			publishMessage((MessageEvent) e);
		}
		else
		{
			broadcast(e);
		}
	}

	private void publishMessage(MessageEvent me)
	{
		// When a message comes in, tell the Actor to start typing
		// (Future) If other messages pile up while still typing, run some rules
		// to delete/re-order certain messages
		// Might be a better idea to merge output cordinator and actor, or
		// connect them directly

		if (!me.getText().contains("|"))
		{
			broadcast(me);
			if (outputToPSI)
				publishMessageToPSI(me);	
			MessageEventLogger.logMessageEvent(me);
		}

		else
		{
			String[] messageParts = me.getParts();
			String[] allAnnotations = me.getAllAnnotations();
			for (int i = 0; i < messageParts.length; i++)
			{
				String mappedPrompt = messageParts[i].trim();
				MessageEvent newme;
				try
				{
					newme = me.cloneMessage(mappedPrompt);
				}
				catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
					newme = new MessageEvent(this, me.getFrom(), mappedPrompt, allAnnotations);
				}

				if (me.isAcknowledgementExpected())
				{
					if (i == (messageParts.length - 1))
					{
						newme.setAcknowledgementExpected(true);
					}
				}
				newme.setReference(me.getReference());
				broadcast(newme);			
				MessageEventLogger.logMessageEvent(newme);
				if (outputToPSI)
					publishMessageToPSI(newme);			
					try       											// Don't send message parts too quickly
					{
						Thread.sleep(6000);
						tick();
					}
					catch (Exception e)
					{
						log(Logger.LOG_WARNING, "<warning>Throttling problem</warning>");
						e.printStackTrace();
					}		
			}
		}
	}

	// Designed for multiple fields. Currently only location and message text are supported.
	private void publishMessageToPSI(MessageEvent me)
	{
		Boolean multimodalMessage = false; 
		String multiModalField = "multimodal"; 
		String speechField = "speech";
		String locationField = "location";
	    String multiModalDelim = ";%;";
		String withinModeDelim = ":";	
		String location = null; 
		String messageString; 
		
		String text = me.getText();
		
		// Get location if it is known
		String to = me.getDestinationUser();
		if (to != null) {
			State state = StateMemory.getSharedState(this.getAgent());
			location = state.getLocation(to);
			if (location != null) {
				multimodalMessage = true; 
			}
		}	
		
		// Format multimodal message if appropriate
		if (multimodalMessage) {
			StringBuilder messageBuilder = new StringBuilder(""); 
			messageBuilder.append(multiModalField);  
			messageBuilder.append(multiModalDelim + speechField + withinModeDelim + text);  
			if (location != null) {
				messageBuilder.append(multiModalDelim + locationField + withinModeDelim + location); 					
			}
			messageString = messageBuilder.toString(); 			
		} 
		else {
			messageString = text; 
		}
		System.out.println("OutputCoordinator, publishMessagetoPSI: " + messageString);
		psiCommunicationManager.msgSender(bazaarToPSITopic,messageString);
	}

	
	public void log(String from, String level, String msg)
	{
		log(level, msg);
	}

	private double beliefGivenHistory(PriorityEvent p)
	{
		double belief = 1.0;

		for (AbstractPrioritySource source : activeSources.values())
		{
			if (!source.allows(p))
			{
				log(Logger.LOG_LOW, "Proposal not allowed by " + source + ": " + p);
				belief = 0;
				return belief;
			}
		}

		for (AbstractPrioritySource source : recentSources)
		{
			belief *= source.likelyNext(p);
		}
		// log(Logger.LOG_NORMAL,("belief = "+belief + " for "+p);
		return belief;
	}

	void addProposal(PriorityEvent pe)
	{
		synchronized (proposalQueue)
		{
			proposalQueue.add(pe);
		}
	}
}
