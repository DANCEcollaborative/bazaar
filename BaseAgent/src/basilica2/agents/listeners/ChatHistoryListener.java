package basilica2.agents.listeners;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import basilica2.agents.events.BotMessageEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.priority.PriorityEvent;
import basilica2.agents.events.priority.PriorityEvent.Callback;
import basilica2.util.HttpUtility;
import basilica2.util.PropertiesLoader;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;


import org.json.JSONException;


public class ChatHistoryListener extends BasilicaAdapter
{
	public String host;
	public String port; 
	public String path;
	public String charset;
	public String delimiter;
	public String start_flag;

	


	
	public ChatHistoryListener(Agent a)
	{
		super(a);
		Properties properties = PropertiesLoader.loadProperties(this.getClass().getSimpleName() + ".properties");
		path = properties.getProperty("path","runtime/chatHistory/");
		

	}
	

	@Override
	public void preProcessEvent(InputCoordinator source, Event e)
	{
		if (e instanceof MessageEvent) {

				try {
					handleMessageEvent(source, (MessageEvent) e);
				} catch (JSONException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}	
		} else if (e instanceof BotMessageEvent) {

			try {
				handleBotMessageEvent(source, (BotMessageEvent) e);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}	
	}
	}
	
	
	public void handleMessageEvent(InputCoordinator source, MessageEvent me) throws JSONException {
		String sender = me.getFrom();
		String content = me.getText();

	    Logger.commonLog("chatHistoryListener", Logger.LOG_NORMAL, "chatHistoryListener heard from " + sender + ": " + me.getText()); 
	}
	
	public void handleBotMessageEvent(InputCoordinator source, BotMessageEvent me) throws JSONException {
		String sender = me.getFrom();
		String content = me.getText();

	    Logger.commonLog("chatHistoryListener", Logger.LOG_NORMAL, "chatHistoryListener heard from : " + content); 
	}


	


	
	@Override
	public void processEvent(InputCoordinator source, Event e) {
		if (e instanceof BotMessageEvent) {
				
	//			handleMessageEvent(source, (BotMessageEvent) e);
			BotMessageEvent bm = (BotMessageEvent) e;
			Logger.commonLog("chatHistoryListener", Logger.LOG_NORMAL, "chatHistoryListener got BotMessageEvent " + bm.getText()); 
			System.err.print("chatHistoryListener got BotMessageEvent " + bm.getText());
				
			
		}
		
	}	
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		return new Class[]{MessageEvent.class};
	}


	@Override
	public Class[] getListenerEventClasses() {
		// TODO Auto-generated method stub
		return new Class[]{BotMessageEvent.class};
	}
}