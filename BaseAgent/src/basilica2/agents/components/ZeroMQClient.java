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
    private ZMQ.Socket publisher;
    private ZMQ.Socket subscriber; 
    private String subscribeTopic = "PSI_Bazaar_Text"; 
    private String publishTopic   = "Bazaar_PSI_Text"; 
    
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
        initZeroMQClient();
    }

    private void initZeroMQClient() {
        try (ZContext context = new ZContext()) {
            
        	// publisher
        	// publisher = context.createSocket(SocketType.PUB);
            // publisher.bind("tcp://*:5555");  
            
            // subscriber
            subscriber = context.createSocket(SocketType.SUB);
            // subscriber.setReceiveTimeOut(2000);
            subscriber.setReceiveTimeOut(-1);
            subscriber.connect("tcp://localhost:5556"); 
            subscriber.subscribe(subscribeTopic.getBytes(ZMQ.CHARSET));
            
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
			try (ZContext context = new ZContext()) { 
	            subscriber = context.createSocket(SocketType.SUB);
	            // subscriber.setReceiveTimeOut(2000);
	            subscriber.setReceiveTimeOut(-1);
	            // subscriber.connect("tcp://0.0.0.0:5556"); 
	            subscriber.connect("tcp://128.2.220.133:5556");              // bazaar.lti.cs.cmu.edu
	            subscriber.subscribe(subscribeTopic.getBytes(ZMQ.CHARSET));
				String psiMessage = subscriber.recvStr(0); 
				// System.err.println("ZeroMQClient, run - received message: " + psiMessage); 	
		    	MessageEvent me = new MessageEvent(this, "psiClient", psiMessage);
		    	// System.err.println("********* ZeroMQCient: About to BROADCAST message >>>   " + psiMessage);
		    	this.broadcast(me);
				
			} catch (Exception e) {
	            e.printStackTrace();
	        }	
		}
	}


	public void sendMessage(String message)
	{
		System.err.println("ZeroMQClient, sendMessage: enter"); 
		try (ZContext context = new ZContext()) { 
        	// publisher = context.createSocket(SocketType.PUB);
            // publisher.bind("tcp://*:5555");  
			String topicMessage = publishTopic + ":true" + message; 
			System.err.println("ZeroMQClient,sendMessage --  message: " + topicMessage);
            publisher.send(topicMessage, 0);
			
		} catch (Exception e) {
            e.printStackTrace();
        }	
		System.err.println("ZeroMQClient, sendMessage: exit"); 
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