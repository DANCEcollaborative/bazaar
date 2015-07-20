package basilica2.social.operation;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;

public class SocialAgentOperation extends BaseAgentOperation
{
    public static void main(String[] args) 
    {
        initializeSystemProperties("system.properties");
        java.awt.EventQueue.invokeLater(new Runnable() 
        {

            @Override
            public void run() 
            {
                BaseAgentOperation thisOperation = new BaseAgentOperation("operation.properties");
                BaseAgentUI thisUI = new BaseAgentUI(thisOperation);
                thisOperation.setUI(thisUI);
                thisOperation.startOperation();
                thisUI.operationStarted();
            }
        });
    }
}
