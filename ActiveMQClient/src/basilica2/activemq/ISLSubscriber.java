package basilica2.activemq;

import javax.jms.Message;

interface ISLSubscriber<T> {
    void onReceive(String topic, T content);
}
