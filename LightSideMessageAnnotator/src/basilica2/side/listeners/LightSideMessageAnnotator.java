package basilica2.side.listeners;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.*;
import java.util.List;
// import java.lang.ProcessBuilder.Redirect;
import java.util.Scanner;

import java.net.*; 

import org.simpleframework.http.Part;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.Status; 
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.Connection;
import org.simpleframework.transport.connect.SocketConnection;

import basilica2.agents.components.InputCoordinator;
import basilica2.agents.events.MessageEvent;
import basilica2.agents.listeners.BasilicaAdapter;
import basilica2.agents.listeners.BasilicaPreProcessor;
import edu.cmu.cs.lti.basilica2.core.Agent;
import edu.cmu.cs.lti.basilica2.core.Event;
import basilica2.side.util.MultipartUtility;;


public class LightSideMessageAnnotator extends BasilicaAdapter
{
	//String pathToLightSide = "/Users/researcher/Downloads/LightSide_2.3.1_20141107";
	//String pathToModel = "saved/test.xml";
	// String pathToLightSide = "../../LightSideMessageAnnotator/runtime/LightSide_2.3.1_20141107";
	String pathToLightSide = "/Users/rcmurray/git/DANCEcollaborative/bazaar/lightside/"; 
	// String pathToLightSide = "/Users/rcmurray/git/DANCEcollaborative/bazaar/LightSideMessageAnnotator/runtime/LightSide_2.3.1_20141107";
	// String pathToModel = "saved/gst_reasoning_model.model.xml";
	String pathToModel = "/Users/rcmurray/git/DANCEcollaborative/bazaar/lightside/models/negative.simple.model.xml";
	String predictionCommand = "/Users/rcmurray/git/DANCEcollaborative/bazaar/lightside/scripts/predict.sh";
	// String predictionCommand = "scripts/test.sh";
	
	OutputStream stdin;
	InputStream stderr;
	InputStream stdout;

	BufferedReader reader;
	BufferedWriter writer;

	Process process = null;
	
	// HTTPClient ls = null;
	String httpAddress = "127.0.0.1";
	int httpPort = 8000;
	String modelName; 
	String modelNickname; 
	
	String host = "http://localhost:8000";
    String charset = "UTF-8";
    MultipartUtility mUtil; 
    enum Status {
    	OK, NOT_FOUND
    }
	
	public LightSideMessageAnnotator(Agent a)
	{
		super(a);
		
		pathToLightSide = getProperties().getProperty("pathToLightSide", pathToLightSide);
		System.err.println("pathToLightSide: " + pathToLightSide);
		pathToModel = getProperties().getProperty("pathToModel", pathToModel);
		System.err.println("pathToModel: " + pathToModel);
		predictionCommand = getProperties().getProperty("predictionCommand", predictionCommand);
		System.err.println("predictionCommand: " + predictionCommand);
		
		modelName = getProperties().getProperty("modelName", modelName);
		System.err.println("modelName: " + modelName);		
		modelNickname = getProperties().getProperty("modelNickname", modelNickname);
		System.err.println("modelNickname: " + modelNickname);
		
		try
		{
			File lightSideLocation = new File(pathToLightSide);
			// process = Runtime.getRuntime().exec(new String[] { predictionCommand, pathToModel }, null, lightSideLocation); // ORIG
			// process = Runtime.getRuntime().exec(predictionCommand, null, lightSideLocation); 
			// process=Runtime.getRuntime().exec(predictionCommand);
			
			/// test replacement for "process = ..." line above /// >>> getting Instantiation exception
			try {
				// process=Runtime.getRuntime().exec(predictionCommand);
				// ProcessBuilder pb = new ProcessBuilder(predictionCommand);
				ProcessBuilder pb = new ProcessBuilder(predictionCommand,pathToModel);
				pb.directory(lightSideLocation);
				pb.inheritIO(); 
				process = pb.start();                  // DISABLED, AT LEAST TEMP
				// pb.redirectInput(Redirect.PIPE);
				// pb.redirectOutput(Redirect.PIPE);
				// process.waitFor();
				
			} 
			catch (Exception e)
			{
				System.err.println("LightSideMessageAnnotator, process creation error - place 1");
				e.printStackTrace();
			}
			// Process process = pb.start();
			///////////////////////////////////////////////////////
			
			/// test replacement for "process = ..." line above /// >>> getting Instantiation exception
			// ProcessBuilder pb = new ProcessBuilder(predictionCommand, pathToModel);
			// pb.directory(lightSideLocation);
			// What to do for the "null" in the original command? 
			// Process process = pb.start();
			///////////////////////////////////////////////////////
			
			/// test replacement for "process = ..." line above /// 
			// ProcessBuilder pb = new ProcessBuilder();
			// pb.command(predictionCommand);
			// pb.command(new String[] {predictionCommand,pathToModel});
			// pb.directory(lightSideLocation);
			// What to do for the "null" in the original command? 
			// Process process = pb.start();
			///////////////////////////////////////////////////////
			
			/// test replacement for "process = ..." line above /// 
			// ProcessBuilder pb = new ProcessBuilder(predictionCommand);
			// Process process = pb.start();
			///////////////////////////////////////////////////////
			
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
			System.err.println("LightSideMessageAnnotator, process creation error - place 2");
			e.printStackTrace();
		}

		
		System.err.println("LightSideMessageAnnotator, creating reader & writer");

		stdin = process.getOutputStream();  // this stdin is the output stream connected to the subprocess's input
		stderr = process.getErrorStream();
		stdout = process.getInputStream();  // this stdout is the input stream connected to the subprocess's output

		reader = new BufferedReader(new InputStreamReader(stdout));   // buffered reader for subprocess's output
		writer = new BufferedWriter(new OutputStreamWriter(stdin));   // buffered writer to the subprocess's input

/**
		reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
*/		
		System.err.println("LightSideMessageAnnotator, reader & writer creation complete");
		
		try {
			Boolean readerStatus = reader.ready();
			System.err.println("reader ready");
		}
		catch (IOException e) {
			System.err.println("reader NOT ready");
		}
		
		/** // First try
		ls = new HTTPClient(httpAddress,httpPort);
		ls.httpLoadModel(modelName,modelNickname);
		*/ 
	
		/** // Second try
		try {
			mUtil = new MultipartUtility(host+"/evaluate", charset);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }	
	    */

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
		String label = null;
		// String path = "saved/";
		System.err.println("==== In LightSideMessageAnnotator, annotateText - text: " + text);
		String path = "models/";

		try {
			// MultipartUtility mUtil = new MultipartUtility(host+"/evaluate/", charset);
			// MultipartUtility mUtil = new MultipartUtility(host+"/evaluate/" + path + modelName, charset);
			MultipartUtility mUtil = new MultipartUtility(host+"/evaluate/" + modelName, charset);
            mUtil.addFormField("sample", text);
            
            // URL url = new URL("file:///Users/rcmurray/git/DANCEcollaborative/bazaar/lightside/" + modelName);  // ??? Was:  URL url = new URL(lightD.getModel());
            // String path = url.getPath();
            // mUtil.addFormField("model", path );
            
            mUtil.addFormField("model", path + modelName );
            List<String> finish = mUtil.finish();
            StringBuilder response = new StringBuilder();
            for (String line : finish) {
                response.append(line);
                response.append('\r');
            }
            // return entity(response.toString());
            // return Response.status(Response.Status.OK).entity(response.toString()).type(MediaType.TEXT_PLAIN_TYPE).build();
            // return Response.status(Response.Status.OK).entity(response.toString());
            // return response.toString(); 
            // return ">>> still trying <<<"; 
            System.err.println(response.toString());
            return response.toString(); 
	    } catch (IOException e) {
	    	// e.printStackTrace();
            return ">>> still trying <<<"; 
	    }	

		/**	============== UPDATE ATTEMPT #1 ==============	
		if(process != null) try
		{
			

			// 
			try { 
				// System.err.println("LightSideMessageAnnotator, write text: " + text);  // TEMP
				writer.write(text + "\n");
				// writer.write(text);
				// writer.newLine();
				// writer.flush();
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				System.err.println("LightSideMessageAnnotator, write/newline error");
				e.printStackTrace();
			}
			
			try {
				String line = reader.readLine();
				if (line == null)
				{
					System.err.println("LightSideMessageAnnotator, read line: NULL");
				}	
				else {
					String[] response = line.split("\\s+");
					label = response[0];	
				}
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				System.err.println("LightSideMessageAnnotator, read/label error");
				e.printStackTrace();
			}
			// ============== UPDATE ATTEMPT #1 ==============
			*/
			
			
			/** ================= ORIG ==============
			// System.err.println("LightSideMessageAnnotator, write text: " + text);  // TEMP
			// writer.write(text + "\n");
			writer.write(text);
			writer.newLine();
			// writer.flush();
			 
			String line = reader.readLine();
			//  System.err.println("line = reader.readline(): " + line);        // TEMP
			if (line != null)
			{
				System.err.println("LightSideMessageAnnotator, read line: " + line);
				String[] response = line.split("\\s+");
				label = response[0];
			}
			else
			{
				System.err.println("response from LightSide is null!");
			}

		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			System.err.println("LightSideMessageAnnotator: Null process");
			e.printStackTrace();
		}
		*/ // ================= ORIG ==============
		
		// return label;
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
	

/**		  
	public class HTTPClient 
	{ 
	    // initialize socket and input output streams 
	    private Socket socket             = null; 
	    // private DataInputStream  httpIn   = null; 
	    // private DataOutputStream httpOut  = null; 
	    private BufferedReader  httpIn   = null; 
	    private BufferedWriter  httpOut  = null; 
	  
	    // constructor to put ip address and port 
	    public HTTPClient(String address, int port) 
	    { 
	        // establish a connection 
	        try
	        { 
	            socket = new Socket(address, port); 
	            System.err.println("===== HTTP client connected ====="); 
	  
	            // takes input from terminal 
	            httpIn = new BufferedReader(new InputStreamReader(socket.getInputStream())); 
	  
	            // sends output to the socket 
	            httpOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),"UTF-8")); 
	        } 
	        catch(UnknownHostException u) 
	        { 
	            System.out.println(u); 
	        } 
	        catch(IOException i) 
	        { 
	            System.out.println(i); 
	        } 
  
	        // string to read message from input 
	        String line = ""; 
	  
	        // keep reading until "Over" is input 
	        while (!line.equals("Over")) 
	        { 
	            try
	            { 
	                line = httpIn.readLine(); 
	                httpOut.writeUTF(line); 
	            } 
	            catch(IOException i) 
	            { 
	                System.out.println(i); 
	            } 
	        } 
  
	        // close the connection 
	        try
	        { 
	            httpIn.close(); 
	            httpOut.close(); 
	            socket.close(); 
	        } 
	        catch(IOException i) 
	        { 
	            System.out.println(i); 
	        } 

	    } 
  	    
	    public void httpLoadModel(String modelName, String modelNickname)
	    {
	    	System.err.println("===== httpLoadModel: loading model " + modelName + " with nickname " + modelNickname);
	    	String path = "/upload";
	    	try {
	    		
	    	    // My first version
	    	    StringBuilder parameters = new StringBuilder();	    	    
	    	    parameters.append("model=" + URLEncoder.encode(modelName, "UTF-8"));	    	    
	    	    parameters.append("&");
	    	    parameters.append("modelNick=" + URLEncoder.encode(modelNickname, "UTF-8"));	    	    
	    	    String parameterString = parameters.toString();	    	    
	    	    httpOut.write("POST " + path + " \r\n");   
	    	    httpOut.write("Content-Length: " + parameterString.length() + "\r\n");
	    	    httpOut.write("Content-Type: application/x-www-form-urlencoded\r\n");
	    	    httpOut.write("\r\n"); 	    
	    	    httpOut.write(parameterString);
	    	    httpOut.flush();

	    		
	    		
	    	}
	    	catch(IOException i) 
	        { 
	            System.out.println(i); 
	        }     	    	
	    }	
	}

*/	

}
