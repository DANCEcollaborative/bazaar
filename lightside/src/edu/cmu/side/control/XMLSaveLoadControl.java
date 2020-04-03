package edu.cmu.side.control;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import edu.cmu.side.model.Recipe;

@Deprecated
public class XMLSaveLoadControl {
	//Returns true if saved successfully, false if not
	public Boolean saveFileToXML(Recipe toSave, String fileName){
		Document doc = buildDoc();
		if(doc==null){
			//We failed somewhere, let them know.
			return false;
		}
		//Now we start the saving here by telling the recipe to start saving its contents to doc
		//UNCOMMENT THIS WHEN READY
//		toSave.saveToXML(doc);
		
		//True if saved successfully, false if not.
		return saveFile(doc, fileName);
	}
	
	private Document buildDoc(){
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			docBuilder = null;
			e.printStackTrace();
		}
		return docBuilder != null?docBuilder.newDocument():null;
	}
	private Boolean saveFile(Document doc, String fileName){
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		try {
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			File toMake = new File("saved"+ File.separator + fileName +".xml");
			if(!toMake.exists()){
				toMake.createNewFile();
			}
			StreamResult result = new StreamResult(toMake);

			// Output to console for testing
			// StreamResult result = new StreamResult(System.out);

			transformer.transform(source, result);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		

		return true;
	}
}
