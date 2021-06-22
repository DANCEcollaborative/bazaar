package basilica2.zeromq;

import java.util.Random;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

//
//  Weather update server in Java
//  Binds PUB socket to tcp://*:5556
//  Publishes random weather updates
//
public class wuserver
{
    public static void main(String[] args) throws Exception
    {
        //  Prepare our context and publisher
        try (ZContext context = new ZContext()) {
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:5556");         		// local
            // publisher.bind("tcp://128.2.220.133:5556"); 	// bazaar.lti     
            // publisher.bind("tcp://128.2.220.52:5556"); 	 	// forum.lti         
            publisher.bind("ipc://weather");

            //  Initialize random number generator
            Random srandom = new Random(System.currentTimeMillis());
            while (!Thread.currentThread().isInterrupted()) {
                //  Get values that will fool the boss
            	Thread.sleep(3000);

                //  Send message to all subscribers
                String update = "PSI_Bazaar_Text:true;%;multimodal:true;%;identity:group;%;speech:I am Haogang;%;location:0:0:0"; 
                System.err.println("wuserver, sending message: " + update);
                publisher.send(update, 0);
            }
        }
    }
}
