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
    // private String subscribeTopic = "PSI_BAZAAR_TEXT"; 
    private String subscribeTopic = "PSI_Bazaar_Text"; 
    private String publishTopic   = "Bazaar_PSI_Text"; 

    // private ArrayList<MessageConsumer> consumers;
    
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
        // this.consumers = new ArrayList<>();
        System.err.println("*** ZeroMQClient: about to initiale ***");
        initZeroMQClient();
        System.err.println("*** ZeroMQClient: initialization complete ***");
        // textSubscriber = new psiTextSubscriber("psiSubscriber",this,psiAgent); 
        // System.out.println("*** ZeroMQClient: subscribing to 'test' ***");
        // subscribe(textSubscriber, "PSI_Bazaar_Text");
        // System.out.println("*** ZeroMQClient: subscribe to 'test' complete ***");
    }

    private void initZeroMQClient() {
        System.err.println("*** ZeroMQClient, initZeroMQClient: start ***");
        try (ZContext context = new ZContext()) {
            
        	// publisher
        	// publisher = context.createSocket(SocketType.PUB);
            // publisher.bind("tcp://*:5555");   // >>> CHANGE TO 5556 <<< 
            
            // subscriber
            // subscriber = context.createSocket(SocketType.SUB);
            // subscriber.setReceiveTimeOut(-1);
            // subscriber.connect("tcp://localhost:5555");   // >>> CHANGE TO 5556 <<< 
            // subscriber.subscribe(subscribeTopic.getBytes(ZMQ.CHARSET));
            
            // connection = factory.createConnection();
            // connection.setClientID("Customer");
            // connection.start();
            // session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            System.err.println("*** ZeroMQClient initZeroMQClient: about to create PRESENCE EVENT ***");
            PresenceEvent e = new PresenceEvent(this,"psiAgent",PresenceEvent.PRESENT); 
            this.broadcast(e);
            System.err.println("*** ZeroMQClient initZeroMQClient: PRESENCE EVENT created ***");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("*** ZeroMQClient, initZeroMQClient: end ***");
    }


	public void run()
	{
		// String psiMessage; 
		// myThread = new Thread(this);
		// running = true;
		// myThread.start();
		
		while (!stopSignalled)
		{	
			try (ZContext context = new ZContext()) { 
	            subscriber = context.createSocket(SocketType.SUB);
	            // subscriber.setReceiveTimeOut(2000);
	            subscriber.setReceiveTimeOut(-1);
	            subscriber.connect("tcp://localhost:5556"); 
	            subscriber.subscribe(subscribeTopic.getBytes(ZMQ.CHARSET));
				// Thread.sleep(1000);
				// myThread.sleep(1000);
				// System.err.println("ZeroMQClient, run - about to call recvStr"); 
				// String psiMessage = subscriber.recvStr(0).trim(); 
				String psiMessage = subscriber.recvStr(0); 
				System.err.println("ZeroMQClient, run - received message: " + psiMessage); 	
				

		    	MessageEvent me = new MessageEvent(this, "piClient", psiMessage);
		    	System.err.println("********* ZeroMQCient: About to BROADCAST message >>>   " + psiMessage);
		    	// System.out.println("********* psiTextSubscriber: MessageEvent               >>>   " + me);
		    	this.broadcast(me);
		    	System.err.println("********* ZeroMQClient: MessageEvent sent >>>   " + me);
				
			} catch (Exception e) {
	            e.printStackTrace();
	        }	
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
			
			// getAgent().getComponent("inputCoordinator").receiveEvent(e);

			// MessageEvent me = (MessageEvent) e;
			// EchoEvent ee = new EchoEvent(e.getSender(), me);
			// getAgent().getComponent("inputCoordinator").receiveEvent(ee);
		}

	}

}