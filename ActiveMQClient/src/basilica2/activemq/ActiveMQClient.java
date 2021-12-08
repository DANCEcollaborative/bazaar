package basilica2.activemq;

import org.apache.activemq.ActiveMQConnectionFactory;
import javax.jms.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane;
import basilica2.agents.data.State;
import basilica2.agents.components.StateMemory;
import edu.cmu.cs.lti.basilica2.core.Component;
import basilica2.agents.components.ChatClient;
import basilica2.agents.events.EchoEvent;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.events.PresenceEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import edu.cmu.cs.lti.project911.utils.log.Logger;
import java.time.LocalDateTime;
import edu.cmu.cs.lti.project911.utils.time.Timer;
import edu.cmu.cs.lti.project911.utils.time.TimeoutReceiver;

public class ActiveMQClient extends Component implements ChatClient
{	
    private ConnectionFactory factory;
    private Connection connection = null;
    private String uri;
    private Session session;
    private psiTextSubscriber textSubscriber; 
    public Agent psiAgent;
	protected enum multiModalTag  
	{
		PSI_Bazaar_Text, multimodal, identity, speech, location, facialExp, pose, emotion;
	}
	private String multiModalDelim = ";%;";
	private String withinModeDelim = ":";	

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
		if (e.getSender() == null || !e.getSender().equals(this)) 
			e.setSender(this);

		// Do not broadcast message if it is simply speech while the agent is speaking
    	Boolean doBroadcast = true; 
		if (e instanceof MessageEvent) {
			MessageEvent trimmedE = trimSpeechIfNotListening((MessageEvent)e);
			if (trimmedE.getText().equals("")) {
				doBroadcast = false; 
			}
		}
		
		if (doBroadcast) {	
			log(Logger.LOG_LOW, "<broadcasting>" + e.getName() + " on " + myOutgoingConnections.size() + " connections</broadcasting>");		
			for (int i = 0; i < myOutgoingConnections.size(); i++)
			{
				myOutgoingConnections.get(i).communicate(e);
			}
			informObserversOfSending(e);
		}
	}
    
    private MessageEvent trimSpeechIfNotListening(MessageEvent me) {
    	
		State currentState = StateMemory.getSharedState(psiAgent);
		if (!(currentState.getMultimodalDontListenWhileSpeaking())) {		
			System.err.println("ActiveMQClient trimSpeechIfNotListening: OKAY to listen while speaking ");
			return me; 
			
		} else {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime dontListenWhileSpeakingEnd = currentState.getMultimodalDontListenWhileSpeakingEnd(); 
			System.err.println("ActiveMQClient trimSpeechIfNotListening: now = " + now.toString());
			if (dontListenWhileSpeakingEnd == null) {
				System.err.println("ActiveMQClient trimSpeechIfNotListening: dontListenWhileSpeakingEnd = NULL");
				System.err.println("ActiveMQClient trimSpeechIfNotListening: NOT quashing speech input");
				return me; 
			} else {
				System.err.println("ActiveMQClient trimSpeechIfNotListening: dontListenWhileSpeakingEnd = " + dontListenWhileSpeakingEnd.toString());
				if (!(now.isBefore(dontListenWhileSpeakingEnd))) {
					System.err.println("ActiveMQClient trimSpeechIfNotListening: NOT quashing speech input");
					return me; 
					
				} else {
					System.err.println("ActiveMQClient trimSpeechIfNotListening: QUASHING any speech input");
					return trimSpeech(me);
				}
			}
		}
	}
    
    // Trim speech from a multimodal message
    private MessageEvent trimSpeech(MessageEvent me) {
    	
    	String origText = me.getText(); 
    	String[] origMessage = origText.split(multiModalDelim);
    
		if (origMessage.length == 0) {				// Don't trim if not a multimodal message
			return me; 
			
		} else {									// This is a multimodal message 
			Integer nonSpeechNonIdentityCount = 0; 	// count of tags that are neither "speech" nor "identity"
			multiModalTag tag; 
			String [] messagePart; 
			String newText = ""; 
			
			// Reformat message without speech field
			for (int i = 0; i < origMessage.length; i++) { 
				System.out.println("=====" + " Multimodal message entry -- " + origMessage[i] + "======");
				messagePart = origMessage[i].split(withinModeDelim,2);			
				tag = multiModalTag.valueOf(messagePart[0]);			
				switch (tag) {
				case multimodal:
					System.out.println("=========== multimodal message ===========");
					newText = newText + tag.name() + withinModeDelim + "true"; 
					break;
				case identity:  
					System.out.println("Identity: " + messagePart[1]);
					newText = newText + multiModalDelim + tag.name() + withinModeDelim + messagePart[1]; 
					break;					
				case speech:		// skipping speech in multimodal message 
					System.err.println("QUASHING SPEECH: " + messagePart[1]);
					break;
				default:
					System.out.println("Tag: " + messagePart[0] + "  -- value: " + messagePart[1]);
					newText = newText + multiModalDelim + tag.name() + withinModeDelim + messagePart[1]; 
					nonSpeechNonIdentityCount++; 
				}
			}	
			if (nonSpeechNonIdentityCount > 0) {
				me.setText(newText);
			} else {
				me.setText(""); 
			}
			return me; 
		}	
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
