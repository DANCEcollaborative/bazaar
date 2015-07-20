package basilica2.chatter.operation;

import java.io.File;

import javax.swing.JFileChooser;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.LaunchEvent;
import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.chatter.listeners.ChatterBoxActor;
import basilica2.util.Timer;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.project911.utils.log.Logger;

public class ChatterBoxOperation extends BaseAgentOperation
{
	public void launchAgent(String room_name) 
    {

		//super.launchAgent(room_name);
		
		double warpFactor = Double.parseDouble(properties.getProperty("chatterbox.time_scale", "1.0"));
		log(Logger.LOG_NORMAL, "replaying scenario at "+warpFactor+"x original speed");
		Timer.setTimeScale(warpFactor);
		
		
//		System.setProperty("chatterbox.transcript", transcriptFile);//properties.getProperty("chatterbox.transcript", transcriptFile));
		System.setProperty("chatterbox.start_time", ""+(Timer.currentTimeMillis()+10000));
		String users = properties.getProperty("chatterbox.users", "rsajedia,jamiek");
		System.setProperty("chatterbox.users", users);
        room_name = room_name.replaceAll(":", "_").replaceAll(" ", "_").replaceAll(",", "_");
		for(String user : users.split(",\\s*"))
		{
			log(Logger.LOG_NORMAL, "launching "+ room_name + " for "+user);
	        roomnameQueue.add(room_name);
	        log(Logger.LOG_NORMAL, agent_definition_file);
	        Agent a = myAgentFactory.makeAgentFromXML(agent_definition_file, room_name);
	        a.setName(user);
	        this.addAgent(a);

	        
	        
	        
	        try
			{
				Thread.sleep(2000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
	        
		}
		
    }
	
    public static void main(String[] args) 
    {

		JFileChooser chooser = new JFileChooser();
		File transcriptFile = new File("transcript.csv");
		int status = chooser.showOpenDialog(null);
		if(status == JFileChooser.APPROVE_OPTION)
		{
			transcriptFile = chooser.getSelectedFile();
		}
		
		final String roomName = "Replay_"+transcriptFile.getName().replace(".csv", "");
		
        initializeSystemProperties("properties/system.properties");

		System.setProperty("chatterbox.transcript", transcriptFile.getPath());//properties.getProperty("chatterbox.transcript", transcriptFile));
		
        java.awt.EventQueue.invokeLater(new Runnable() 
        {

            @Override
            public void run() 
            {
            	ChatterBoxOperation thisOperation = new ChatterBoxOperation();
            
                BaseAgentUI thisUI = new BaseAgentUI(thisOperation, roomName);
                thisOperation.setUI(thisUI);
                thisOperation.startOperation();
                thisUI.operationStarted();
            }
        });
    }
}
