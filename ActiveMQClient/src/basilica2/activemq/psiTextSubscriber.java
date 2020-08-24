package basilica2.activemq;

import java.util.Arrays;

import basilica2.agents.events.MessageEvent;
import edu.cmu.cs.lti.basilica2.core.Agent;

public class psiTextSubscriber implements ISLTextSubscriber {
    String name;
    ActiveMQClient m;
    Agent a; 

    public psiTextSubscriber(String name) {
        this.name = name;
    }

    public psiTextSubscriber(String name, ActiveMQClient m, Agent a) {
        this.name = name;
        this.m = m;
        this.a = a; 
    }

    @Override
    public void onReceive(String topic, String content) {
        System.out.println("********* psiTextSubscriber received message. Subscriber:" + this.name + "\tTopic: " + topic + "\tContent:" + content);
    	// System.out.println("********* psiTextSubscriber: About to CREATE message >>>   " + content);
    	// MessageEvent me = new MessageEvent(m, a.getUsername(), content);     // worked
    	MessageEvent me = new MessageEvent(m, "psiAgent", content);
    	// System.out.println("********* psiTextSubscriber: About to BROADCAST message >>>   " + content);
    	// System.out.println("********* psiTextSubscriber: MessageEvent               >>>   " + me);
    	m.broadcast(me);
    	System.out.println("********* psiTextSubscriber: MessageEvent sent          >>>   " + me);
    }
}
