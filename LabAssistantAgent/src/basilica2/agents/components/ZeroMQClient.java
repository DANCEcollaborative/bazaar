package basilica2.agents.components;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import edu.cmu.cs.lti.basilica2.core.Component;
import basilica2.agents.components.ChatClient;
import basilica2.agents.data.State.Student;
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;


public class ZeroMQClient extends Component implements ChatClient

{
    private String uri;
    public Agent psiAgent;
    private ZMQ.Socket subscriber; 
    private ZContext context; 
	private String psiHost = "*";							// This machine 
	private String psiPort = "5556"; 
	private String psiToBazaarTopic = "PSI_Bazaar_Text";
    
    @Override
	public void disconnect()
	{
		System.out.println("*** ZeroMQClient disconnecting... ***");		
	}

    public ZeroMQClient(Agent a, String n, String pf)
	{
        this(a,n,pf,5555);
    }

    public ZeroMQClient(Agent a, String n, String pf, int port) {
        this(a,n,pf,"tcp://localhost:" + port);
    }

    public ZeroMQClient(Agent a, String n, String pf, String uri) {
		super(a, n, pf);
		this.psiAgent = a;
        this.uri = uri;		
		if(myProperties!=null)
			try{psiHost = myProperties.getProperty("PSI_Host", psiHost);}
			catch(Exception e) {e.printStackTrace();}
			try{psiPort = myProperties.getProperty("PSI_Port", psiPort);}
			catch(Exception e) {e.printStackTrace();}
			try{psiToBazaarTopic = myProperties.getProperty("PSI_to_Bazaar_Topic", psiToBazaarTopic);}
			catch(Exception e) {e.printStackTrace();}
        initZeroMQClient();
    }

    private void initZeroMQClient() {
    	context = new ZContext(); 
        // try (ZContext context = new ZContext()) {
        try  {           
            // subscriber
            subscriber = context.createSocket(SocketType.SUB);
            // subscriber.setReceiveTimeOut(2000);						// wait at most 2 seconds to receive a message
            subscriber.setReceiveTimeOut(-1);							// wait indefintely to receive a message
            // subscriber.connect("tcp://128.2.220.133:5556"); 	     	// subscribe to server bazaar.lti.cs.cmu.edu
            subscriber.connect("tcp://" + psiHost + ":" + psiPort); 
            subscriber.subscribe(psiToBazaarTopic.getBytes(ZMQ.CHARSET));
            
            PresenceEvent e = new PresenceEvent(this,"psiAgent",PresenceEvent.PRESENT); 
            this.broadcast(e);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


	public void run()
	{
		while (!stopSignalled)
		{	
			String psiMessage = subscriber.recvStr(0); 
	    	MessageEvent me = new MessageEvent(this, "psiClient", psiMessage);
	    	this.broadcast(me);
		}
	}
    
    
    @Override
	protected void broadcast(Event e)
	{
		if (e.getSender() == null || !e.getSender().equals(this)) e.setSender(this);

		log(Logger.LOG_LOW, "<broadcasting>" + e.getName() + " on " + myOutgoingConnections.size() + " connections</broadcasting>");
		
		for (int i = 0; i < myOutgoingConnections.size(); i++)
		{
			myOutgoingConnections.get(i).communicate(e);
		}
		informObserversOfSending(e);
	}

	@Override
	public String getType()
	{
		// TODO Auto-generated method stub
		return "Client";
	}

	@Override
	public void login(String roomName)
	{
		
	}
    
    @Override
	protected void processEvent(Event e)
	{
		if(e instanceof MessageEvent)
		{

			MessageEvent me = (MessageEvent) e;
			getAgent().getComponent("inputCoordinator").receiveEvent(me);
		}

	}

}