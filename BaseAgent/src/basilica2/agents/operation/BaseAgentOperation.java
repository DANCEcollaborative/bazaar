/*
 *  Copyright (c), 2009 Carnegie Mellon University.
 *  All rights reserved.
 *  
 *  Use in source and binary forms, with or without modifications, are permitted
 *  provided that that following conditions are met:
 *  
 *  1. Source code must retain the above copyright notice, this list of
 *  conditions and the following disclaimer.
 *  
 *  2. Binary form must reproduce the above copyright notice, this list of
 *  conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *  
 *  Permission to redistribute source and binary forms, with or without
 *  modifications, for any purpose must be obtained from the authors.
 *  Contact Rohit Kumar (rohitk@cs.cmu.edu) for such permission.
 *  
 *  THIS SOFTWARE IS PROVIDED BY CARNEGIE MELLON UNIVERSITY ``AS IS'' AND
 *  ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 *  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 *  NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 */
package basilica2.agents.operation;

import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import basilica2.agents.components.ChatClient;
import basilica2.agents.components.ConcertChatActor;
import basilica2.agents.components.ConcertChatListener;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.JabberClient;
import basilica2.agents.components.StateMemory;
import basilica2.agents.data.State;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.factory.AgentFactory;
import edu.cmu.cs.lti.basilica2.support.AgentOperation;
import edu.cmu.cs.lti.basilica2.ui.AgentUI;
import edu.cmu.cs.lti.project911.utils.log.Logger;

import joptsimple.*;

/**
 * 
 * @author dadamson
 */
public class BaseAgentOperation extends AgentOperation
{
	public static String operation_name = null;
	protected String env_listener_name;
	protected String env_actor_name;
	protected String agent_definition_file;
	protected boolean use_debug_ui = true;
	protected BaseAgentUI myUI;
	protected AgentFactory myAgentFactory = new AgentFactory();
	protected Map<String, AgentUI> myAgentUIs;
	protected List<String> roomnameQueue;

	protected Class[] preprocessors = new Class[] {};
	protected Class[] processors = new Class[] {};
	protected Properties properties;

	public BaseAgentOperation()
	{
		this("operation.properties");

	}

	public BaseAgentOperation(String operationFile)
	{
		super(operation_name);
		log(Logger.LOG_NORMAL, "Operation Created");
		loadProperties("operation.properties");
		myAgentUIs = new HashMap<String, AgentUI>();
		roomnameQueue = new ArrayList<String>();

		myLogger.setConfiguration(true, true, true, true, false, true);
	}

	private void loadProperties(String pf)
	{
		properties = new Properties();
		try
		{
			if (!new File(pf).exists()) pf = "properties" + File.separator + pf;

			properties.load(new FileInputStream(pf));
			env_listener_name = properties.getProperty("operation.envlistener");
			env_actor_name = properties.getProperty("operation.envactor");
			agent_definition_file = properties.getProperty("operation.agentdefinition");
			use_debug_ui = Boolean.parseBoolean(properties.getProperty("operation.hasdebugui"));
			preprocessors = getClasses(properties.getProperty("operation.preprocessors"));
			processors = getClasses(properties.getProperty("operation.listeners"));
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	private Class[] getClasses(String classnames)
	{
		String[] names = classnames.split("(:|,|\\s)\\s*");
		Class[] classes = new Class[names.length];
		for (int i = 0; i < names.length; i++)
		{
			String name = names[i];
			try
			{
				classes[i] = Class.forName(name);
			}
			catch (ClassNotFoundException e)
			{
				log(Logger.LOG_ERROR, "Couldn't find class " + name);
			}
		}
		Logger.commonLog(getClass().getSimpleName(), Logger.LOG_NORMAL, "Classes: " + Arrays.toString(classes));
		return classes;
	}

	public void launchAgent(String room_name)
	{
		log(Logger.LOG_NORMAL, "setting basilica2.agents.room_name to " + room_name);
		System.setProperty("basilica2.agents.room_name", room_name);

		log(Logger.LOG_NORMAL, "<launching room=" + room_name + "/>");
		
		//TODO: verify that this sanitization isn't needed - it breaks VMT
		//room_name = room_name.replaceAll(":", "_").replaceAll(" ", "_").replaceAll(",", "_");
		roomnameQueue.add(room_name);

		Agent a = myAgentFactory.makeAgentFromXML(agent_definition_file, room_name);
		this.addAgent(a);
	}

	public void stopAgent(String agent_name)
	{

		this.removeAgent(agent_name);
		
	}

	@Override
	public void agentAdded(Agent a)
	{
		log(Logger.LOG_LOW, "Added. " + myAgents.size() + " agents now working");
		log(Logger.LOG_LOW, "Initializing new agent");
		a.initialize();

		if (use_debug_ui)
		{
			AgentUI aui = new AgentUI(a);
			myAgentUIs.put(a.getName(), aui);
		}

		InputCoordinator input = (InputCoordinator) a.getComponent("inputCoordinator");
		input.addListeners(preprocessors, processors);

		StateMemory.commitSharedState(new State(), a);

		// Custom operation on components happen here
		Component environmentListener = a.getComponent(env_listener_name);
		if (environmentListener instanceof ConcertChatListener)
		{
			ConcertChatListener myCCListener = (ConcertChatListener) environmentListener;
			ConcertChatActor myCCActor = (ConcertChatActor) a.getComponent(env_actor_name);
			myCCListener.connectToChatRoom(roomnameQueue.remove(0), myCCActor);
		} 
		/*
		 * else if (a.getComponent(env_listener_name) instanceof MuseListener) {
		 * MuseListener myCCListener = (MuseListener)
		 * a.getComponent(env_listener_name); MuseActor myCCActor = (MuseActor)
		 * a.getComponent(env_actor_name); String[] tokens =
		 * roomnameQueue.remove(0).split("_");
		 * myCCListener.connectToChatRoom(tokens[0], tokens[1], tokens[2],
		 * myCCActor); }
		 */
		else if(environmentListener instanceof ChatClient)
		{
			((ChatClient)environmentListener).login(roomnameQueue.remove(0));
		}
		else
			log(Logger.LOG_ERROR, "Don't know what to do for " + env_listener_name);

		log(Logger.LOG_LOW, "Starting new Agent");
		a.start();

		myUI.addAgentWidget(a.getName(), new AgentWidget(input));
		myUI.agentLaunched(a.getName());
		log(Logger.LOG_NORMAL, "notified UI of agent launch for "+a.getName());
	}
	
	public void stopOperation()
	{
		super.startOperation();

		for(Agent agentToKill : myAgents.values())
		{
			Component environmentListener = agentToKill.getComponent(env_listener_name);
			if (environmentListener instanceof ConcertChatListener)
			{
				ConcertChatListener myCCListener = (ConcertChatListener) environmentListener;
				myCCListener.logout();
			} 
			else if(environmentListener instanceof ChatClient)
			{
				((ChatClient)environmentListener).disconnect();
			}
			else
				log(Logger.LOG_ERROR, "Don't know how to disconnect" + env_listener_name);
		}
	}
	
	@Override
	public void agentRemoved(Agent a)
	{
		if (use_debug_ui)
		{
			AgentUI aui = myAgentUIs.get(a.getName());
			if (aui != null)
			{
				aui.dispose();
			}
		}

		myUI.agentStopped(a.getName());
		log(Logger.LOG_LOW, "Removed. " + myAgents.size() + " agents now working");
	}

	@Override
	public void tick()
	{
		myUI.tick();
	}

	@Override
	public void operationOver()
	{
		shutdown();
	}

	@Override
	public void receiveLoggerMessage(String msg)
	{

	}

	public void setUI(BaseAgentUI u)
	{
		myUI = u;
	}

	public static String getRoomNameFromArgs(String[] args)
	{
		final String roomName = args.length > 0 ? args[0] : "Test1";
		return roomName;
	}

	protected static void initializeSystemProperties(String pf)
	{
		Properties properties = new Properties();

		File pff = new File(pf);

		if (pff.exists() || (pff = new File("properties" + File.separator + pf)).exists())
		{
			try
			{
				properties.load(new FileReader(pff));
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}

			Enumeration propertiesKeys = properties.propertyNames();

			for (; propertiesKeys.hasMoreElements();)
			{
				String key = (String) propertiesKeys.nextElement();
				System.setProperty(key, properties.getProperty(key));
			}
		}
		else
			System.err.println("BaseAgentOperation: no system properties at " + pf);
	}
//	protected static Point getUILocationFromArgs(String[] args)
//	{
//		final Point windowLoc = new Point();
//    	if(args.length > 3)
//    	{
//    		try
//    		{
//    			int x = Integer.parseInt(args[2]);
//    			int y = Integer.parseInt(args[3]);
//    			windowLoc.setLocation(x, y);
//    		}
//    		catch(Exception e)
//    		{
//    			System.err.println(args[2]+", "+args[3]+" is not a valid location!");
//    		}
//    	}
//		return windowLoc;
//	}

	protected void setSystemOutput(String outLogDirectory, final String roomName)
	{
    	DateFormat timeFormat = new SimpleDateFormat("yyyyMMdd-HHmm-ss");
		File outDir = new File(outLogDirectory);
    	if(!outDir.exists())
    	{
    		outDir.mkdir();
    	}
    	else if(!outDir.isDirectory())
    	{
    		System.err.println("Not a Folder: "+outDir.getPath());
    	}
    	else
    	{
    		try
			{
				String outLogFilename = outDir+"/"+roomName+"-"+(timeFormat.format(new Date()))+".log";
				PrintStream logPrintStream = new PrintStream(outLogFilename);
				System.out.println("redirecting output to "+outLogFilename);
				System.setOut(logPrintStream);
			}
			catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
	}
	public static void main(final String[] args)
	{
		
		initializeSystemProperties("system.properties");
		java.awt.EventQueue.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				BaseAgentOperation thisOperation = new BaseAgentOperation();
				BaseAgentUI thisUI = new BaseAgentUI(thisOperation);
				thisOperation.setUI(thisUI);
				thisOperation.startOperation();
				thisUI.operationStarted();
				
				thisOperation.processArgs(args);
			}
		});
	}
	
	protected void processArgs(String[] args)
	{
		processArgs(args, "Test01");
	}
	
	protected void processArgs(String[] args, String roomname)
	{
		OptionParser parser = new OptionParser();
		parser.accepts("x").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("y").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("room").withRequiredArg().defaultsTo(roomname);
		parser.accepts("outdir").withRequiredArg();
		parser.accepts("condition").withRequiredArg();
		parser.accepts("launch");
		
		OptionSet options = parser.parse(args);

		String room = (String)options.valueOf("room");
		myUI.setRoomName(room);
		myUI.setLocation(new Point((Integer)options.valueOf("x"), (Integer)options.valueOf("y")));
		
		if(options.has("outdir"))
		{
			this.setSystemOutput((String)options.valueOf("outdir"), room);
		}

		
		if(options.has("condition"))
		{
			String condition = (String)options.valueOf("condition");
			System.out.println("setting basilica2.agents.condition to '"+condition+"'");
			log(Logger.LOG_NORMAL, "setting basilica2.agents.condition to '"+condition+"'");
			System.setProperty("basilica2.agents.condition", condition);
		}
		
		if(options.has("launch"))
		{
			System.out.println("launching...");
			log(Logger.LOG_NORMAL, "launching hands-free!");
			System.setProperty("basilica2.handsfree", "true");
			this.launchAgent(room);
		}
	}

	public Properties getProperties()
	{
		return properties;
	}
	
}
