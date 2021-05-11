package basilica2.myagent.operation;

import java.awt.Point;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class NewAgentRunner extends BaseAgentOperation {
	
	public static void main(final String[] args) {
		initializeSystemProperties("system.properties");
		
		OptionParser parser = new OptionParser();
		parser.accepts("x").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("y").withRequiredArg().ofType(Integer.class).defaultsTo(0);
		parser.accepts("room").withRequiredArg().defaultsTo("Test01");
		parser.accepts("outdir").withRequiredArg();
		parser.accepts("condition").withRequiredArg();
		parser.accepts("launch");
		OptionSet options = parser.parse(args);
				
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				NewAgentRunner thisOperation = new NewAgentRunner();
				
				// Launch from command line without UI
				if (options.has("launch")) {
					// Set conditions from agent's operations.properties file
					String[] conditions = thisOperation.getProperties().getProperty("operation.conditions", "")
							.split("[\\s,]+");
					String conditionString = getConditionString(conditions);
					thisOperation.processArgsNoUIConstantConditions(args,"Test01",conditionString);   
				}
				
				else {
					String[] conditions = thisOperation.getProperties().getProperty("operation.conditions", "")
							.split("[\\s,]+");
					String room_name = thisOperation.getProperties().getProperty("operation.room", "Test01");
					
					// Launch from IDE or command line with no UI, using agent parameters for room name and conditions
					if (thisOperation.no_condition_ui) {
						String conditionString = getConditionString(conditions);
						System.setProperty("basilica2.agents.condition", conditionString);
						// Logger.commonLog("Launching without dialog", Logger.LOG_NORMAL, "Conditions set to " + conditionString);
						// thisOperation.startOperation();
						thisOperation.launchAgent(room_name,false);
					} 
					
					// Launch from command line or IDE with UI
					else {
						BaseAgentUI thisUI = new ConditionAgentUI(thisOperation, room_name, conditions);
						thisOperation.setUI(thisUI);
						thisOperation.startOperation();
						thisUI.operationStarted();
						thisOperation.processArgs(args);
					}
				}
			}
		});
	}
	
	public static String getConditionString(String[] conditions)  // "static" fixed the call to this; is it okay?
	{
		String conditionString = "";
		for(String condition : conditions)
		{
			conditionString += condition + " ";
		}
		if (conditionString.isEmpty()) return "none";
		else return conditionString.trim();
	}
	
	protected void processArgsNoUIConstantConditions(String[] args, String roomname, String conditionString)
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

		// Set constant conditions
		System.out.println("setting basilica2.agents.condition to '"+conditionString+"'");
		log(Logger.LOG_NORMAL, "setting basilica2.agents.condition to '"+conditionString+"'");
		System.setProperty("basilica2.agents.condition", conditionString);

		
		if(options.has("launch"))
		{
			System.out.println("launching...");
			log(Logger.LOG_NORMAL, "launching hands-free!");
			System.setProperty("basilica2.handsfree", "true");
			this.launchAgent(room,false);
		}
	}
	

	public void launchAgent(String room_name, Boolean hasUI)
	{
		// log(Logger.LOG_NORMAL, "setting basilica2.agents.room_name to " + room_name);
		System.setProperty("basilica2.agents.room_name", room_name);
		// System.err.println("NewAgentRunner, launchAgent: room_name = " + room_name); 

		// log(Logger.LOG_NORMAL, "<launching room=" + room_name + "/>");
		
		//TODO: verify that this sanitization isn't needed - it breaks VMT
		//room_name = room_name.replaceAll(":", "_").replaceAll(" ", "_").replaceAll(",", "_");
		roomnameQueue.add(room_name);

		Agent a = myAgentFactory.makeAgentFromXML(agent_definition_file, room_name);
		a.hasUI = hasUI; 
		this.addAgent(a);
	}
	

	
	protected void processArgs(String[] args)
	{
		processArgs(args, "Test01", true);
	}
	
	protected void processArgs(String[] args, String roomname, Boolean hasUI)
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
		
		if (hasUI) {
			myUI.setRoomName(room);
			myUI.setLocation(new Point((Integer)options.valueOf("x"), (Integer)options.valueOf("y")));
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
			this.launchAgent(room,hasUI);
		}
	}
	
	
}
