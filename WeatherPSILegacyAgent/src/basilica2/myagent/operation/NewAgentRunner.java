package basilica2.myagent.operation;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;

public class NewAgentRunner extends BaseAgentOperation {
	public static void main(final String[] args) {
		initializeSystemProperties("system.properties");
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				NewAgentRunner thisOperation = new NewAgentRunner();
				String[] conditions = thisOperation.getProperties().getProperty("operation.conditions", "")
						.split("[\\s,]+");
				String room = thisOperation.getProperties().getProperty("operation.room", "Try");
				BaseAgentUI thisUI = new ConditionAgentUI(thisOperation, room, conditions);
				thisOperation.setUI(thisUI);
				thisOperation.startOperation();
				thisUI.operationStarted();
				thisOperation.processArgs(args);
			}
		});
	}
}
