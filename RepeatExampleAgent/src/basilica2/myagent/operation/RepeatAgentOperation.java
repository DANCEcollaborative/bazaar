package basilica2.myagent.operation;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;

public class RepeatAgentOperation extends BaseAgentOperation
{
    public static void main(String[] args) 
    {
        initializeSystemProperties("system.properties");
        java.awt.EventQueue.invokeLater(new Runnable() 
        {
            @Override
            public void run() 
            {
            	RepeatAgentOperation thisOperation = new RepeatAgentOperation();
            	
            	//launch a new agent UI with a default room name, and a list of possible conditions/
            	//selected conditions are accessible through System.getProperty("basilica2.agents.condition")
                BaseAgentUI thisUI = new ConditionAgentUI(thisOperation, "Test01", "rude");
                thisOperation.setUI(thisUI);
                thisOperation.startOperation();
                thisUI.operationStarted();
            }
        });
    }
}
