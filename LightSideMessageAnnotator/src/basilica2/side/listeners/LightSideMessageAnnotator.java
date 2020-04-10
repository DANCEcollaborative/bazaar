package basilica2.side.listeners;
import java.io.IOException;
// import java.util.List;
import java.util.*;
import java.util.Scanner;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.side.util.MultipartUtility;


public class LightSideMessageAnnotator extends BasilicaAdapter
{
	String pathToModel; 
	String modelName; 
	String modelNickname;
	String predictionCommand; 
	String classificationString; 
	
	String host = "http://localhost:8000";
    String charset = "UTF-8";
    MultipartUtility mUtil; 
    Hashtable<String, Double> classify_dict = new Hashtable<String, Double>();
	
	public LightSideMessageAnnotator(Agent a)
	{
		super(a);
		pathToModel = getProperties().getProperty("pathToModel", pathToModel);
		modelName = getProperties().getProperty("modelName", modelName);        
		modelNickname = getProperties().getProperty("modelNickname", modelNickname);
		predictionCommand = getProperties().getProperty("predictionCommand", predictionCommand);
		classificationString = getProperties().getProperty("classifications", classificationString);
		String[] classificationList = classificationString.split(","); 
		int listLength = classificationList.length; 
		for (int i=0; i<listLength; i+=2) {
			classify_dict.put(classificationList[i],Double.parseDouble(classificationList[i+1]));
		}
		
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
            String classifications = parseLightSideResponse(response);
            return classifications; 
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return "LightSide returned null"; 
	    }	
	}
	
	public String parseLightSideResponse(StringBuilder response)
	{
		String startFlag = "<h3>";
		String endFlag = "</h3>";
		String classSplit = "%<br>";
		String withinClassSplit = ": ";
		String[] classificationSpec;
		String classification; 
		Double classificationPercent; 
		Double classificationThreshold;
		StringBuilder annotation = new StringBuilder(""); 
		String plus = ""; 
		
		int start = response.indexOf(startFlag);
		int end = response.indexOf(endFlag,start);
		String classifications = response.substring((start+4),end);
		String[] classificationList = classifications.split(classSplit); 
		int listLength = classificationList.length; 
		for (int i=0; i < listLength; i++) {
			classificationSpec = classificationList[i].split(withinClassSplit);
			classification = classificationSpec[0];
			classificationPercent = Double.parseDouble(classificationSpec[1]);
			try {
				classificationThreshold = classify_dict.get(classification);
				if (classificationPercent >= classificationThreshold) {
					annotation.append(plus + classification.toUpperCase());
					plus = "+"; 					
				}
			}
			catch (Exception e) {
		    	System.out.println("LightSide classification \"" + classification + "\" not used"); 
			}			
		}
		return annotation.toString(); 	
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
