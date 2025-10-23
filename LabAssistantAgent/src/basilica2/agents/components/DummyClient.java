package basilica2.agents.components;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import de.fhg.ipsi.utils.StringUtilities;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Component;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;


public class DummyClient extends Component implements ChatClient
{	
	
	@Override
	public void disconnect()
	{
		System.out.println("Dummy chat client disconnecting...");
		
	}

	//TODO: poll for incoming messages
	//TODO: report presence events
	//TODO: report extant students at login


	public DummyClient(Agent a, String n, String pf)
	{
		super(a, n, pf);
	}

	@Override
	protected void processEvent(Event e)
	{
		if(e instanceof MessageEvent)
		{
			MessageEvent me = (MessageEvent) e;
			EchoEvent ee = new EchoEvent(e.getSender(), me);
			
			getAgent().getComponent("inputCoordinator").receiveEvent(ee);
		}
		//TODO: private messages? "beeps"?

	}
	
	/* (non-Javadoc)
	 * @see basilica2.agents.components.ChatClient#login(java.lang.String)
	 */
	@Override
	public void login(String roomName)
	{
		
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return "Client";
	}

}
