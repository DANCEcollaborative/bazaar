package basilica2.agents.components;

import basilica2.agents.listeners.BasilicaListener;
import basilica2.agents.listeners.BasilicaPreProcessor;
import basilica2.agents.data.RollingWindow;
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PrioritySource;
import basilica2.util.MessageEventLogger;
import edu.cmu.cs.lti.basilica2.core.*;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author dadamson
 */
public class InputCoordinator extends Component
{
    private final PrioritySource genericPrioritySource = new PrioritySource("Generic", false);
	HashMap<Class, ArrayList<BasilicaListener>> listeners = new HashMap<Class, ArrayList<BasilicaListener>>();
    HashMap<Class, ArrayList<BasilicaPreProcessor>> preprocessors = new HashMap<Class, ArrayList<BasilicaPreProcessor>>();
    Set<Event> preprocessedEvents = new HashSet<Event>();
    Set<PriorityEvent> proposals = new HashSet<PriorityEvent>();
    private OutputCoordinator outputCoordinator;
    
    
    
    
    public InputCoordinator(Agent a, String n, String pf) 
    {
        super(a, n, pf); 
        
        //showUI();
    }
    
    public void addPreProcessor(Class c, BasilicaPreProcessor prep)
    {
        if(preprocessors.get(c) == null)
            preprocessors.put(c, new ArrayList<BasilicaPreProcessor>());
        
        preprocessors.get(c).add(prep);
    }
    
    public void addListener(Class c, BasilicaListener blister)
    {
        if(listeners.get(c) == null)
            listeners.put(c, new ArrayList<BasilicaListener>());
        
        listeners.get(c).add(blister);
    }
    
    public void addPreprocessedEvent(Event e)
    {
        synchronized(this)
        {
            preprocessedEvents.add(e);
        }
		for (int i = 0; i < myObservers.size(); i++)
		{
			myObservers.get(i).eventReceived(this, e);
		}
    }
    
    public void addProposal(PriorityEvent e)
    {
        synchronized(this)
        {
            proposals.add(e);
        }
    }
    
	public void addEventProposal(Event e)
	{
		this.addProposal(new PriorityEvent(this, e, 0.5, genericPrioritySource, 10));
	}	
	
	public void pushEventProposal(Event e)
	{
		this.pushProposal(new PriorityEvent(this, e, 0.5, genericPrioritySource, 10));
	}
	
	public void addEventProposal(Event e, double priority, double timeout)
	{
		this.addProposal(new PriorityEvent(this, e, priority, genericPrioritySource, timeout));
	}

	public void pushEventProposal(Event e, double priority, double timeout)
	{
		this.pushProposal(new PriorityEvent(this, e, priority, genericPrioritySource, timeout));
	}
	
	/**
	 *for use from within threads/timers:  push an event past preprocessing, process by listeners NOW.
	 * @param e
	 */
    public void pushEvent(Event e)
    {
    	processOneEvent(e);
    	pushEventsToOutputCoordinator();
    }
    
    /**
     * for use from within threads/timers: push a proposal to the output queue NOW.
     * @param pe
     */
    public void pushProposal(PriorityEvent pe) 
    {
            outputCoordinator = (OutputCoordinator)myAgent.getComponent("outputCoordinator");
            outputCoordinator.addProposal(pe);
    }
    
    /**
     * schedule new event e for delivery to the InputCoordinator, for preprocessing and all the rest.
     * @param e
     */
    public void queueNewEvent(Event e)
    {
    	//this.processEvent(e);
    	this.dispatchEvent(this, e);
    }
    
    @Override /*from Component*/
    public void processEvent(Event event)
    {
		log(Logger.LOG_NORMAL, "InputCoordinator received event: "+event);
        if(event instanceof MessageEvent && isAgentName(((MessageEvent) event).getFrom())) 
        {
            event = new EchoEvent(this, (MessageEvent)event);
        }
        //System.err.println(event.getName()+": "+event.toString());
            
        synchronized(this)
        {
//            Class<? extends Event> eventClass = event.getClass();
//            if(preprocessors.containsKey(eventClass))

            preprocessedEvents.add(event);
            for(Class<? extends Event> keyClass : preprocessors.keySet())
            {
            	if(keyClass.isInstance(event))
            	{
		            for(BasilicaPreProcessor prep : preprocessors.get(keyClass))
		            {
		            	//System.err.println("****\n\nprocessing "+event+" for "+prep.getClass().getSimpleName()+": ");
		                prep.preProcessEvent(this, event);
		                //System.err.println(preprocessedEvents+"\n\n****");
		            }
            	}
            }
 
            //collect any annotations that the preprocessors have applied to this message, and log it.
            if(event instanceof MessageEvent)
        		MessageEventLogger.logMessageEvent((MessageEvent)event);
            
            processAllEvents();
        }
        
        
    }

	public boolean isAgentName(String from)
	{
		return from.trim().contains(getAgent().getUsername().trim()) || from.contains("Tutor") || from.trim().contains(System.getProperty("loginHandle", "Tutor"));
	}

    private void processAllEvents()
    {
        synchronized(this)
        {

			log(Logger.LOG_NORMAL, "Events After Preprocessing: " +preprocessedEvents);

    		RollingWindow window = RollingWindow.sharedWindow();
            for(Event eve : preprocessedEvents)
            {
                processOneEvent(eve);
                if(eve instanceof MessageEvent)
                {
                	MessageEvent me = (MessageEvent)eve;
	        		window.addEvent(me, me.getAllAnnotations());
	        		window.addEvent(me, "incoming", me.getFrom()+"_turn", "student_turn");
                }
//                else if(eve instanceof EchoEvent)
//                {
//                	window.addEvent(eve, "tutor_turn");
//                }
            }

            pushEventsToOutputCoordinator();
            preprocessedEvents.clear();
        }
    }

	private void pushEventsToOutputCoordinator()
	{

		log(Logger.LOG_NORMAL,"Events After Agent Processing:"+proposals);

		outputCoordinator = (OutputCoordinator)myAgent.getComponent("outputCoordinator");
		outputCoordinator.addAll(proposals);

		proposals.clear();
	}

	private void processOneEvent(Event eve)
	{
//		Class<? extends Event> eventClass = eve.getClass();
//		if(listeners.containsKey(eventClass))
		
		super.notifyEventObservers(eve);
		
        for(Class<? extends Event> keyClass : listeners.keySet())
        {
        	if(keyClass.isInstance(eve))
        	{
				List<BasilicaListener> blisters = listeners.get(keyClass);
			    for(int i = 0; i < blisters.size(); i++)
			    {
			    	BasilicaListener blister = blisters.get(i);
			        blister.processEvent(this, eve);
			    }
        	}
		}
	}
    
    /**
     * construct a single instance of each given class of event (pre)processor, and add to the appropriate event-mappings
     * @param preprocessors must implement BasilicaPreprocessor
     * @param processors must implement BasilicaListener
     */
    public void addListeners(Class[] preprocessors, Class[] processors)
    {
       
        Map<Class, Object> allMyChildren = new HashMap<Class, Object>();
        
        for(Class pp : preprocessors)
        {

        	if(pp == null)
        		continue;
            BasilicaPreProcessor preep = (BasilicaPreProcessor)allMyChildren.get(pp);
            if(preep == null) try
            {
                Constructor maker = pp.getConstructor(Agent.class);
                preep = (BasilicaPreProcessor)maker.newInstance(myAgent);
            }
            catch (Exception ex)
            {
                try
                {
                    preep = (BasilicaPreProcessor)pp.newInstance();
                }
                catch (Exception ex1)
                {
                    ex1.printStackTrace();
                }
            }
            if(preep != null)
            {
            	allMyChildren.put(pp, preep);
            	for(Class target : preep.getPreprocessorEventClasses())
            		this.addPreProcessor(target, preep);
            }
        }
        
        for(Class pp : processors)
        {
        	if(pp == null)
        		continue;
            BasilicaListener leep = (BasilicaListener)allMyChildren.get(pp);
            if(leep == null) try
            {
                Constructor maker = pp.getConstructor(Agent.class);
                leep = (BasilicaListener)maker.newInstance(myAgent);
            }
            catch (Exception ex)
            {
                try
                {
                    leep = (BasilicaListener)pp.newInstance();
                }
                catch (Exception ex1)
                {
                    ex1.printStackTrace();
                }
            }
            if(leep != null)
            {
            	for(Class target : leep.getListenerEventClasses())
            		this.addListener(target,leep);
            }
        }
        //input.addListener(MessageEvent.class, new Parrot());
    }
    
    public void writeLog(String level, String message)
    {    	
    	if(!(level.equals(Logger.LOG_LOW) || level.equals(Logger.LOG_NORMAL)))
		{
			log(level, message);
		}
    }

    @Override
    public String getType()
    {
        return "COORDINATOR";
    }

    public Agent getAgent()
    {
        return myAgent;
    }
    
    public void removeListener(Class key, BasilicaListener blister)
    {
        listeners.get(key).remove(blister);
    }
    
    public void removePreProcessor(Class key, BasilicaPreProcessor plister)
    {
        preprocessors.get(key).remove(plister);
        
    }
    
    public void removeListener(BasilicaListener blister)
    {
    	synchronized(this)
    	{
	        for(Class key : listeners.keySet())
	        {
	        	ArrayList<BasilicaListener> classListeners = listeners.get(key);
				if(classListeners.contains(blister))
	        		classListeners.remove(blister);
	        }
    	}
    }
    
    public void removePreProcessor(BasilicaPreProcessor plister)
    {        
    	synchronized(this)
        {
	        for(Class key : preprocessors.keySet())
	        {
	            ArrayList<BasilicaPreProcessor> classListeners = preprocessors.get(key);
				if(classListeners.contains(plister))
					classListeners.remove(plister);
	        }
        }
    }
    
//TODO: Make this a lot less ugly and usable - re-integrate with regular Basilica component UI
    private void showUI()
    {
        JPanel panel = new JPanel(new BorderLayout());
        final JButton updateButton = new JButton("Update");
        
        final String[] columns = {"listener", "trigger", "state"};
        
        final JTable listenersList = new JTable(mapTable(listeners), columns);
        final JTable ppList = new JTable(mapTable(preprocessors), columns);

        ppList.setModel(new DefaultTableModel(mapTable(preprocessors), columns));
        listenersList.setModel(new DefaultTableModel(mapTable(listeners), columns));
        
        JPanel tablePanel = new JPanel(new GridLayout(2, 0, 10, 10));
        
        tablePanel.add(new JScrollPane(ppList));
        tablePanel.add(new JScrollPane(listenersList));
        
        listenersList.setGridColor(Color.GRAY);
        ppList.setGridColor(Color.GRAY);
        
        listenersList.setShowGrid(true);
        ppList.setShowGrid(true);
        
        panel.add(tablePanel, BorderLayout.CENTER);
        panel.add(updateButton, BorderLayout.SOUTH);
        final ActionListener updater = new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent ae) {
                final String[][] mapTable = mapTable(preprocessors);
                ((DefaultTableModel)(ppList.getModel())).setDataVector(mapTable, columns);
                ((DefaultTableModel)(listenersList.getModel())).setDataVector(mapTable(listeners), columns);
                
            }
        };
        
        updateButton.addActionListener(updater);
        
        JFrame jim = new JFrame();
        jim.add(panel);
        jim.setSize(600, 200);
        jim.setVisible(true);
        
//        new java.util.Timer().scheduleAtFixedRate(new TimerTask(){
//
//            @Override
//            public void run() 
//            {
//                updater.actionPerformed(null);
//            }
//        }, 500, 10000);
        
    }
    
    private String[][] mapTable(HashMap<Class, ? extends Collection> map)
    {
        String[][] table = new String[map.size()][3];
        
        ArrayList<String[]> listenerInfo = new ArrayList<String[]>();
        
        ArrayList<Class> keys = new ArrayList<Class>(map.keySet());
        for(Class key : keys)
        {
            for(Object o : map.get(key))
            {
                Method statusMethod;
                String state;
                try 
                {
                    statusMethod = o.getClass().getMethod("getStatus");
  
                    state = statusMethod.invoke(o, null).toString();
                }
                 catch (Exception ex) 
                 {
                     state = o.toString();
                 }
                
                listenerInfo.add(new String[]{o.getClass().getSimpleName(), key.getSimpleName(), state});
            }
            
        }
        
        return listenerInfo.toArray(table);
    }

    @Override
    protected void notifyEventObservers(Event e)
    {
    	//we want to notify only when we're ready.
    }
    
}
