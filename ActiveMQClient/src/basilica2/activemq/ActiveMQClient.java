package basilica2.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;
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

public class ActiveMQClient extends Component implements ChatClient
{	
    private ConnectionFactory factory;
    private Connection connection = null;
    private String uri;
    private Session session;
    private psiTextSubscriber textSubscriber; 
    public Agent psiAgent;

    private ArrayList<MessageConsumer> consumers;
    
    @Override
	public void disconnect()
	{
		System.out.println("*** ActiveMQ client disconnecting... ***");
		
	}

    public ActiveMQClient(Agent a, String n, String pf)
	{
        this(a,n,pf,61616);
    }

    public ActiveMQClient(Agent a, String n, String pf, int port) {
        this(a,n,pf,"tcp://localhost:" + port);
    }

    public ActiveMQClient(Agent a, String n, String pf, String uri) {
		super(a, n, pf);
		this.psiAgent = a;
        this.uri = uri;
        this.consumers = new ArrayList<>();
        // System.out.println("*** ActiveMQServer: initializing ***");
        initActiveMQServer();
        // System.out.println("*** ActiveMQServer: initialization complete ***");
        textSubscriber = new psiTextSubscriber("psiSubscriber",this,psiAgent); 
        // System.out.println("*** ActiveMQServer: subscribing to 'test' ***");
        subscribe(textSubscriber, "PSI_Bazaar_Text");
        // System.out.println("*** ActiveMQServer: subscribe to 'test' complete ***");
    }

    private void initActiveMQServer() {
        factory = new ActiveMQConnectionFactory(this.uri);
        try {
            connection = factory.createConnection();
            //connection.setClientID("Customer");
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            System.out.println("*** ActiveMQServer: about to create PRESENCE EVENT ***");
            PresenceEvent e = new PresenceEvent(this,"psiAgent",PresenceEvent.PRESENT); 
            this.broadcast(e);
            System.out.println("*** ActiveMQServer: PRESENCE EVENT created ***");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subscribe(ISLSubscriber subscriber, String topicString) {
        try {
            for (MessageConsumer consumer: consumers) {
                TopicListener listener = (TopicListener)consumer.getMessageListener();
                if (listener.getSubscriber().hashCode() == subscriber.hashCode()
                        && listener.getTopic().getTopicName().equals(topicString)) {
                    System.out.println(subscriber.toString() + "already subscribed topic " + topicString);
                    return;
                }
            }
            Topic destination = session.createTopic(topicString);
            MessageConsumer consumer = session.createConsumer(destination);
            consumers.add(consumer);
            consumer.setMessageListener(new TopicListener(subscriber, destination));
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(ISLSubscriber subscriber, String topicString) {
        for (MessageConsumer consumer: consumers) {
            try {
                TopicListener listener = (TopicListener)consumer.getMessageListener();
                if (listener.getSubscriber().hashCode() == subscriber.hashCode()
                    && listener.getTopic().getTopicName().equals(topicString)) {
                    consumers.remove(consumer);
                    break;
                }
            } catch (JMSException e) {
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
