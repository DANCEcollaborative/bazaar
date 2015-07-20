package basilica2.socketchat.operation;

import java.util.logging.Level;
import java.util.logging.Logger;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;

public class SocketIORunner extends BaseAgentOperation
{
    public static void main(final String[] args) 
    {
        initializeSystemProperties("system.properties");
        java.awt.EventQueue.invokeLater(new Runnable() 
        {

            @Override
            public void run() 
            {
            	SocketIORunner thisOperation = new SocketIORunner();
                String[] conditions = thisOperation.getProperties().getProperty("operation.conditions", "").split("[\\s,]+");
                String room = thisOperation.getProperties().getProperty("operation.room", "Test01");
                BaseAgentUI thisUI = new ConditionAgentUI(thisOperation, room, conditions);
                thisOperation.setUI(thisUI);
                thisOperation.startOperation();
                thisUI.operationStarted();
                
                thisOperation.processArgs(args);
                

        		Logger sioLogger = java.util.logging.Logger.getLogger("io.socket");
        		sioLogger.setLevel(Level.SEVERE);
            }
        });
    }
}
