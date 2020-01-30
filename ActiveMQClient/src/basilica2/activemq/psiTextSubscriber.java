package basilica2.activemq;

import java.util.Arrays;

public class psiTextSubscriber implements ISLTextSubscriber {
    String name;

    public psiTextSubscriber(String name) {
        this.name = name;
    }

    @Override
    public void onReceive(String topic, String content) {
        System.out.println("********* Received string message. Subscriber:" + this.name + "\tTopic: " + topic + "\tContent:" + content);
    }
}
