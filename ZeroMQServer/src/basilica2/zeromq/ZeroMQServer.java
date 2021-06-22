package basilica2.zeromq;

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
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;

//
//  Weather update server in Java
//  Binds PUB socket to tcp://*:5556
//  Publishes random weather updates
//
public class ZeroMQServer extends Component implements ChatClient

{
    private String uri;
    public Agent psiAgent;

    // private ArrayList<MessageConsumer> consumers;
    
    @Override
	public void disconnect()
	{
		System.out.println("*** MQ client disconnecting... ***");
		
	}

    public ZeroMQServer(Agent a, String n, String pf)
	{
        this(a,n,pf,5555);
    }

    public ZeroMQServer(Agent a, String n, String pf, int port) {
        this(a,n,pf,"tcp://localhost:" + port);
    }

    public ZeroMQServer(Agent a, String n, String pf, String uri) {
		super(a, n, pf);
		this.psiAgent = a;
        this.uri = uri;
        // this.consumers = new ArrayList<>();
        System.err.println("*** ZeroMQServer: about to initiate ***");
        initZeroMQServer();
        System.err.println("*** ZeroMQServer: initialization complete ***");
        // textSubscriber = new psiTextSubscriber("psiSubscriber",this,psiAgent); 
        // System.out.println("*** ZeroMQServer: subscribing to 'test' ***");
        // subscribe(textSubscriber, "PSI_Bazaar_Text");
        // System.out.println("*** ZeroMQServer: subscribe to 'test' complete ***");
    }

    private void initZeroMQServer() {
        System.err.println("*** ZeroMQServer, initZeroMQServer: start ***");
        try (ZContext context = new ZContext()) {
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:5555");
            String filter = "PSI_BAZAAR_TEXT";
            publisher.subscribe(filter.getBytes(ZMQ.CHARSET));
            
            // connection = factory.createConnection();
            // connection.setClientID("Customer");
            // connection.start();
            // session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            System.err.println("*** ZeroMQServer initZeroMQServer: about to create PRESENCE EVENT ***");
            PresenceEvent e = new PresenceEvent(this,"psiAgent",PresenceEvent.PRESENT); 
            this.broadcast(e);
            System.out.println("*** ZeroMQServer initZeroMQServer: PRESENCE EVENT created ***");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.err.println("*** ZeroMQServer, initZeroMQServer: end ***");
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