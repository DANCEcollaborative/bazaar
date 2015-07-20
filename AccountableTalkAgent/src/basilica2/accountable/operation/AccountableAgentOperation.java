package basilica2.accountable.operation;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.agents.operation.ConditionAgentUI;

public class AccountableAgentOperation extends BaseAgentOperation
{
    public static void main(final String[] args) 
    {
        initializeSystemProperties("system.properties");
        
        java.awt.EventQueue.invokeLater(new Runnable() 
        {

            @Override
            public void run() 
            {
            	AccountableAgentOperation thisOperation = new AccountableAgentOperation();
                BaseAgentUI thisUI = new ConditionAgentUI(thisOperation, "Test1");
                //thisUI.setLocation(windowLoc);
                thisOperation.setUI(thisUI);
                thisOperation.startOperation();
                thisUI.operationStarted();
                
                thisOperation.processArgs(args);
            }
        });
    }


}
