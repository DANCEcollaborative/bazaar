package basilica2.chatter.operation;

import java.io.File;
import java.util.HashMap;

import javax.swing.JFileChooser;

import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Connection;
import edu.cmu.cs.lti.basilica2.ui.AgentUI;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import basilica2.agents.components.ConcertChatActor;
import basilica2.agents.components.ConcertChatListener;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.components.OutputCoordinator;
import basilica2.agents.components.StateMemory;
import basilica2.agents.components.WhiteboardActor;
import basilica2.agents.data.State;
import basilica2.agents.operation.BaseAgentOperation;
import basilica2.agents.operation.BaseAgentUI;
import basilica2.util.Timer;

public class PanChatterBoxOperation extends BaseAgentOperation
{
	public void launchAgent(String room_name)
	{
		System.setProperty("chatterbox.transcript", properties.getProperty("chatterbox.transcript", "transcript.csv"));
		System.setProperty("chatterbox.start_time", "" + (Timer.currentTimeMillis() + 30000));

		super.launchAgent(room_name);

	}

	@Override
	public void agentAdded(Agent a)
	{
		a.initialize();

		if (use_debug_ui)
		{
			AgentUI aui = new AgentUI(a);
			myAgentUIs.put(a.getName(), aui);
		}

		InputCoordinator input = (InputCoordinator) a.getComponent("inputCoordinator");
		OutputCoordinator output = (OutputCoordinator) a.getComponent("outputCoordinator");
		input.addListeners(preprocessors, processors);

		StateMemory.commitSharedState(new State(), a);

		
		String roomName = roomnameQueue.remove(0);

		String listenerProperties = null;
		String actorProperties = "properties"+File.separator+"CCActor.properties";
		
		for (String user : properties.getProperty("chatterbox.users").split(","))
		{
			ConcertChatListener userIn = new ConcertChatListener(a, user, user+"_CCListener", listenerProperties);
			WhiteboardActor userOut = new WhiteboardActor(a, user, user+"_WhiteboardActor", actorProperties);
			userIn.connectToChatRoom(roomName, userOut);

			System.out.println("starting CC listener/actor for "+user);
			
			a.addComponent(userIn);
			a.addComponent(userOut);
			
			a.makeConnection(userIn, input);
			a.makeConnection(output, userOut);
			
			System.out.println("launched CC listener/actor for "+user);
			
			//multiple agents take time to launch
			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
		}
		
		a.start();

		myUI.agentLaunched(a.getName());
	}

	public static void main(String[] args)
	{
		System.out.println(Timer.currentTimeMillis());
		Timer.setTimeScale(1.0);
		System.out.println(Timer.currentTimeMillis());

		initializeSystemProperties("properties/system.properties");
		
		JFileChooser chooser = new JFileChooser();
		File transcriptFile = new File("transcript.csv");
		int status = chooser.showOpenDialog(null);
		if(status == JFileChooser.APPROVE_OPTION)
		{
			transcriptFile = chooser.getSelectedFile();
		}
		
		final String roomName = "Replay_"+transcriptFile.getName().replace(".csv", "");

		System.setProperty("chatterbox.transcript", transcriptFile.getPath());//properties.getProperty("chatterbox.transcript", transcriptFile));
		
		java.awt.EventQueue.invokeLater(new Runnable()
		{

			@Override
			public void run()
			{
				PanChatterBoxOperation thisOperation = new PanChatterBoxOperation();
				BaseAgentUI thisUI = new BaseAgentUI(thisOperation, roomName);
				thisOperation.setUI(thisUI);
				thisOperation.startOperation();
				thisUI.operationStarted();
			}
		});
	}
}
