package basilica2.zeromq;

import java.util.StringTokenizer;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

//
//  Weather update client in Java
//  Connects SUB socket to tcp://localhost:5556
//  Collects weather updates and finds avg temp in zipcode
//
public class testclient
{
    public static void main(String[] args)
    {
        try (ZContext context = new ZContext()) {
            //  Socket to talk to server
            System.out.println("Receiving messages as PSI subscriber");
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.setReceiveTimeOut(-1);
            // subscriber.connect("tcp://localhost:5555");					// local machine
            // subscriber.connect("tcp://128.2.220.133:5555");              // bazaar.lti.cs.cmu.edu
            subscriber.connect("tcp://128.2.220.52:5555");              // forum.lti.cs.cmu.edu

            String subscribeTopic = "Bazaar_PSI_Text";
            // String subscribeTopic = "PSI_Bazaar_Text";    // TEMP test receive from wuserver
            
            subscriber.subscribe(subscribeTopic.getBytes(ZMQ.CHARSET));

            while (true == true) {
	            // subscriber.subscribe(subscribeTopic.getBytes(ZMQ.CHARSET));
				// String psiMessage = subscriber.recvStr(0); 
            	
                //  Use trim to remove the tailing '0' character
				// String string = subscriber.recvStr(0); 
                try {
                	Thread.sleep(1000);              	
    			} catch (Exception e) {
    	            e.printStackTrace();
    	        }	
                // String message = subscriber.recvStr(0).trim();
                String message = subscriber.recvStr(0);

                System.out.println("testclient, message received: " + message); 
            }
        } catch (Exception e) {
            e.printStackTrace();
        }	
    }
}
