package basilica2.myagent.operation;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;

public class FancyTalkRunner extends BaseAgentOperation
{
    public static void main(String[] args) 
    {
        initializeSystemProperties("system.properties");
        java.awt.EventQueue.invokeLater(new Runnable() 
        {

            @Override
            public void run() 
            {
            	FancyTalkRunner thisOperation = new FancyTalkRunner();
                String[] conditions = thisOperation.getProperties().getProperty("operation.conditions", "").split("[\\s,]+");
                String room = thisOperation.getProperties().getProperty("operation.room", "Test01");
                BaseAgentUI thisUI = new ConditionAgentUI(thisOperation, room, conditions);
                thisOperation.setUI(thisUI);
                thisOperation.startOperation();
                thisUI.operationStarted();
            }
        });
    }
}
