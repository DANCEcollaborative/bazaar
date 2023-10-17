package basilica2.side.listeners;
import java.io.IOException;
import java.util.Scanner;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.FileEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.util.HttpUtility;
import java.net.URLEncoder;

public class ExternalStepNotifier extends BasilicaAdapter
{
	String host;
	String port; 
	String path;
//	String role = "Teacher";   // TEMPORARILY HARD-CODED
//	String delimiter; 
    String charset;
	
	public ExternalStepNotifier(Agent a)
	{
		super(a);
		host = getProperties().getProperty("host", host);
		port = getProperties().getProperty("port", port);
		path = getProperties().getProperty("path", path);
//		delimiter = getProperties().getProperty("delimiter", delimiter);
		charset = getProperties().getProperty("charset", charset);
		System.err.println("ExternalStepNotifier, host:port -- " + host + ":" + port);
		// System.err.println("ExternalStepNotifier, path: " + path + "  ---  delimiter: " + delimiter);	
		System.err.println("ExternalStepNotifier, path: " + path);
		System.err.println("ExternalStepNotifier, charset: " + charset);	
	}
 
	/**
	 * @param source
	 *            the InputCoordinator - to push new events to. (Modified events
	 *            don't need to be re-pushed).
	 * @param event
	 *            an incoming event which matches one of this preprocessor's
	 *            advertised classes (see getPreprocessorEventClasses)
	 * 
	 *            Preprocess an incoming event, by modifying this event or
	 *            creating a new event in response. All original and new events
	 *            will be passed by the InputCoordinator to the second-stage
	 *            Reactors ("BasilicaListener" instances).
	 */
	@Override
	public void preProcessEvent(InputCoordinator source, Event event)
	{
		FileEvent fe = (FileEvent) event;
		System.out.println(fe);
		String encodedRole; 
		String encodedText; 

		String filename = fe.getFileName();
		try {
//			encodedRole = URLEncoder.encode(role,charset).replace("+", "%20");  // role is hard-coded for now
//			encodedText = URLEncoder.encode(text,charset).replace("+", "%20");
			encodedText = URLEncoder.encode(filename,charset).replace("+", "%20");
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return; 
	    }
		
		String query = encodedRole + delimiter + encodedText; 		
		String label = annotateText(query);

		if (label != null)
		{
			fe.addAnnotations(label);			
		}
	}

	public String annotateText(String text)
	{
		try {
			String requestURL = host + ":" + port + path + text; 
			HttpUtility.sendGetRequest(requestURL); 
			String response = HttpUtility.readSingleLineResponse(); 
			// System.err.println("=== annotateText response: " + response); 
			return response; 
			
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return "annotateText returned an IOException"; 
	    }	
	}
	
	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		// only FileEvents will be delivered to this watcher.
		return new Class[] { FileEvent.class };
	}

	public static void main(String[] args)
	{
		Scanner input = new Scanner(System.in);
		ExternalStepNotifier annotator = new ExternalStepNotifier(null);

		while (input.hasNext())
		{
			String text = input.nextLine();
			String label = annotator.annotateText(text);
			// System.out.println("Label is " + label);
		}
		input.close();
	}

	@Override
	public Class[] getListenerEventClasses()
	{
		// no processing events.
		return new Class[]{};
	}

	@Override
	public void processEvent(InputCoordinator arg0, Event arg1)
	{
		//we do nothing
	}
	

}
