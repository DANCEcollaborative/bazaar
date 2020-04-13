package basilica2.side.listeners;
import java.io.IOException;
import java.util.*;
import java.io.File;
import java.util.Scanner;
import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.side.util.MultipartUtility;


public class LightSideMessageAnnotator extends BasilicaAdapter
{
	String pathToLightSide; 
	String modelName; 
	String modelNickname;
	String predictionCommand; 
	String classificationString; 
	
	String host = "http://localhost";
	String port = "8000"; 
    String charset = "UTF-8";
	String modelPath = "models/";
    MultipartUtility mUtil; 
    Hashtable<String, Double> classify_dict = new Hashtable<String, Double>();
	
	public LightSideMessageAnnotator(Agent a)
	{
		super(a);
		port = getProperties().getProperty("port", port);
		pathToLightSide = getProperties().getProperty("pathToLightSide", pathToLightSide);
		modelPath = getProperties().getProperty("modelPath", modelPath);
		modelName = getProperties().getProperty("modelName", modelName);        
		modelNickname = getProperties().getProperty("modelNickname", modelNickname);
		predictionCommand = getProperties().getProperty("predictionCommand", predictionCommand);
		Process process;
		File lightSideLocation = new File(pathToLightSide);
		
		classificationString = getProperties().getProperty("classifications", classificationString);
		String[] classificationList = classificationString.split(","); 
		int listLength = classificationList.length; 
		for (int i=0; i<listLength; i+=2) {
			classify_dict.put(classificationList[i],Double.parseDouble(classificationList[i+1]));
		}
		
		try {
			ProcessBuilder pb = new ProcessBuilder(predictionCommand,port,modelNickname + ":" + modelPath + modelName);
			pb.directory(lightSideLocation);
			pb.inheritIO(); 
			process = pb.start(); 
						
			Boolean isAlive = process.isAlive();
			if (isAlive) {
				System.err.println("LightSide process is alive");
			}
			else {
				System.err.println("LightSide process is NOT alive");			
			}
			
		} 
		catch (Exception e)
		{
			System.err.println("LightSideMessageAnnotator, error starting LightSide");
			e.printStackTrace();
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

		try {
			MultipartUtility mUtil = new MultipartUtility(host + ":" + port + "/evaluate/" + modelName, charset);
            mUtil.addFormField("sample", text);
            mUtil.addFormField("model", modelPath + modelName );
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
