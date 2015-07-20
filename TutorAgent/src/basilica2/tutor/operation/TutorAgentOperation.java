package basilica2.tutor.operation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.ui.AgentUI;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.components.ConcertChatActor;
import basilica2.agents.components.ConcertChatListener;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.components.WhiteboardActor;
import basilica2.agents.data.State;
import basilica2.agents.operation.AgentWidget;
import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;

public class TutorAgentOperation extends BaseAgentOperation
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
