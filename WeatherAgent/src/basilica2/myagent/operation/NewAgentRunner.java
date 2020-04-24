package basilica2.myagent.operation;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;
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
		parser.accepts("hasUI").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		parser.accepts("launch");
		OptionSet options = parser.parse(args);
				
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				NewAgentRunner thisOperation = new NewAgentRunner();
				if (options.has("launch")) {
					thisOperation.processArgs(args);
				}
				else {
					String[] conditions = thisOperation.getProperties().getProperty("operation.conditions", "")
							.split("[\\s,]+");
					String room_name = thisOperation.getProperties().getProperty("operation.room", "Try");
					if (thisOperation.no_condition_ui) {
						String conditionString = getConditionString(conditions);
						System.setProperty("basilica2.agents.condition", conditionString);
						Logger.commonLog("Launching without dialog", Logger.LOG_NORMAL, "Conditions set to " + conditionString);
						// thisOperation.startOperation();
						thisOperation.launchAgent(room_name);
					} 
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
	
}
