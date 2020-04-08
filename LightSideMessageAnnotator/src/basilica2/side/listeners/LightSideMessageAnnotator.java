package basilica2.side.listeners;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.side.util.MultipartUtility;;


public class LightSideMessageAnnotator extends BasilicaAdapter
{
	String pathToModel; 
	String modelName; 
	String predictionCommand; 
	String host = "http://localhost:8000";
    String charset = "UTF-8";
    MultipartUtility mUtil; 
	
	public LightSideMessageAnnotator(Agent a)
	{
		super(a);
		pathToModel = getProperties().getProperty("pathToModel", pathToModel);
		modelName = getProperties().getProperty("modelName", modelName);
		predictionCommand = getProperties().getProperty("predictionCommand", predictionCommand);
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
		MessageEvent me = (MessageEvent) event;
		System.out.println(me);

		String text = me.getText();
		String label = annotateText(text);

		if (label != null)
		{
			me.addAnnotations(label);
			
		}
	}

	public String annotateText(String text)
	{
		String path = "models/";

		try {
			MultipartUtility mUtil = new MultipartUtility(host+"/evaluate/" + modelName, charset);
            mUtil.addFormField("sample", text);
            mUtil.addFormField("model", path + modelName );
            List<String> finish = mUtil.finish();
            StringBuilder response = new StringBuilder();
            for (String line : finish) {
                response.append(line);
                response.append('\r');
            }
            // return Response.status(Response.Status.OK).entity(response.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();
            System.err.println(">>>> LightSide response: "+ response.toString());
            return response.toString(); 
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return "LightSide returned null"; 
	    }	

	}

	/**
	 * @return the classes of events that this Preprocessor cares about
	 */
	@Override
	public Class[] getPreprocessorEventClasses()
	{
		// only MessageEvents will be delivered to this watcher.
		return new Class[] { MessageEvent.class };
	}

	public static void main(String[] args)
	{
		Scanner input = new Scanner(System.in);
		LightSideMessageAnnotator annotator = new LightSideMessageAnnotator(null);

		while (input.hasNext())
		{
			String text = input.nextLine();
			String label = annotator.annotateText(text);
			System.out.println("Label is " + label);
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
