/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package basilica2.agents.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.PromptTable;
import basilica2.agents.data.State;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;
import edu.cmu.cs.lti.project911.utils.time.Timer;

/**
 *
 * @author dadamson
 */
public class IntroductionsHandler extends BasilicaAdapter
{
    Agent agent;
    
    final List<String> users = new ArrayList<String>();
    private double wait1 = 30;
    private PromptTable prompter;
    
    Map<String, String> slots = new HashMap<String, String>();
            
    public IntroductionsHandler(Agent a)
    {
        super(a, "INTRODUCTION");
        agent = a;
        prompter = new PromptTable(properties.getProperty("prompt_file", "plans/plan_prompts.xml"));
        wait1 = Integer.parseInt(properties.getProperty("timeout", ""+wait1));
        
        
        slots = new HashMap<String, String>();
		slots.put("[AGENT NAME]", a.getUsername().split(" ")[0]);
		
    }

    @Override
    public void processEvent(InputCoordinator source, Event event)
    {
        if(event instanceof MessageEvent)
        {
            MessageEvent me = (MessageEvent)event;
            
            String[] names = me.checkAnnotation("GIVING_NAME");
            
            if(names != null)
            {
                recognizeUser(names[0], me.getFrom(), source);
            }
        }
    }

    @Override
    public void preProcessEvent(InputCoordinator source, Event event)
    {
        if(event instanceof MessageEvent)
        {
            
            MessageEvent me = (MessageEvent)event;
            
            String normalizedText = MessageAnnotator.normalize(me.getText());
            List<String> namesFound = new ArrayList<String>();
            Pattern intro = Pattern.compile("((i am)|(i'm)|(im)|(name is)|(llama es)|(this is)) (\\w+)");
            Matcher nameMatcher = intro.matcher(normalizedText);
            if(nameMatcher.find())
            {
                namesFound.add(nameMatcher.group(nameMatcher.groupCount()));
            }

            if (namesFound.size() > 0) 
            {
                me.addAnnotation("GIVING_NAME", namesFound);
            }
        }
    }

    private void updateUsers()
    {
        synchronized(users)
        {
            State state = StateMemory.getSharedState(agent);
            String[] usernames = state.getStudentIds();

            users.clear();
            for(String name : usernames)
            {
                if(state.getStudentName(name).equals(name))
                {
                    users.add(name);
                }
            }
        }
    }
    

    @Override
    public void startListening(final InputCoordinator source)
    {
        super.startListening(source);
        
        
        MessageEvent me = new MessageEvent(source, agent.getUsername(), prompter.lookup("INTRODUCE", slots), "INTRODUCE");
        source.pushProposal(new PriorityEvent(source, me, 0.3, prioritySource));
        
        TimeoutReceiver tim = new TimeoutReceiver()
        {
            @Override
            public void timedOut(String id)
            {
                updateUsers();
                if(id.equals("WAIT FOR NAMES"))
                {
                    if(users.isEmpty())
                    {
                        return;
                    }
                                
                    HashMap<String, String> slots = new HashMap<String, String>();
                    
                    slots.put("[NAMES]", StateMemory.getSharedState(agent).getStudentNamesString(users));
                    slots.putAll(IntroductionsHandler.this.slots);
                    
                    MessageEvent me = new MessageEvent(source, agent.getUsername(), prompter.lookup("GIVE_UP_ON_INTRODUCTIONS", slots),"GIVE_UP_ON_INTRODUCTIONS");
                    source.pushProposal(new PriorityEvent(source, me, 0.3, prioritySource));
                    
                    //assign remaining names
                    //uninstall:
                    stopListening(source);
                }
            }

            @Override
            public void log(String from, String level, String msg)
            {
                agent.log(from, level, msg);
            }
        };
        
        new Timer(wait1, "WAIT FOR NAMES", tim).start();
    }

    private void recognizeUser(String name, String username, InputCoordinator source)
    {
        name = toProperCase(name);
        synchronized(users)
        {
            updateUsers();

            State s = State.copy(StateMemory.getSharedState(agent));
            s.setName(username, name);
            StateMemory.commitSharedState(s, agent);

            users.remove(username);
            
//            if(users.isEmpty())
//                this.stopListening(source);
        }
                
        HashMap<String, String> slots = new HashMap<String, String>();
        slots.put("[NAME]", name);
        MessageEvent me = new MessageEvent(source, agent.getUsername(), prompter.lookup("GREET", slots), "GREET");
        source.addProposal(new PriorityEvent(source, me, 0.3, prioritySource));

    }

    public static String toProperCase(String name) 
    {
        String ret = name.toLowerCase();
        if (name.length() > 0) 
        {
            ret = name.substring(0, 1).toUpperCase().concat(name.substring(1).toLowerCase());
        }
        return ret;
    }
    
    public String getStatus()
    {
        return StateMemory.getSharedState(agent).getStudentNamesString()+": "+(this.delegate == null ? "no delegate":("delegate="+delegate));
    }

    @Override
    public Class[] getPreprocessorEventClasses() 
    {
        return new Class[]{MessageEvent.class};
    }

    @Override
    public Class[] getListenerEventClasses() 
    {
        return new Class[]{MessageEvent.class};
    }
}
