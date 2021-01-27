package plugins.input;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.plugin.FileParser;
import edu.cmu.side.recipe.Chef;
import edu.cmu.side.view.util.CSVExporter;
import edu.cmu.side.view.util.DocumentListTableModel;

public class CTBParser extends FileParser{

	public static void main(String[] args) throws IOException{
		Set<String> test = new TreeSet<String>();
		test.add("/Users/lightsidelabs/Documents/workspace/lightside/data/ctb/4_56263_TS_56468_1.xml");
		CTBParser parser = new CTBParser();
		DocumentList dl = parser.parseDocumentList(test, Charset.forName("UTF-8"));
		//		for(int i = 0; i < dl.getSize(); i++){
		//			System.out.println("Input doc " + i);
		//			for(String s : dl.allAnnotations().keySet()){
		//				if(dl.allAnnotations().get(s).size()>i){
		//					System.out.println("    " + s + ": " + dl.allAnnotations().get(s).get(i));					
		//				}
		//			}
		//		}
		//		for(String s : dl.allAnnotations().keySet()){
		//			System.out.println(s + ": " + dl.allAnnotations().get(s).size());
		//		}
	}

	@Override
	public DocumentList parseDocumentList(Set<String> filenames, Charset encoding)
			throws IOException {
		DocumentList done = null;
		for(String file : filenames){
			CTBHandler handler = new CTBHandler();
			handler.setTarget(file);
			handler.parseDocument();
			DocumentList docs = handler.getDocumentList();
			done=(done==null?docs:DocumentList.merge(done, docs));
		}
		return done;
	}

	@Override
	public boolean canHandle(String filename) 
	{
		return filename.endsWith(".xml");
	}

	private class CTBHandler extends DefaultHandler{


		List<String> text = new ArrayList<String>();
		Map<String, List<String>> annotations = new TreeMap<String, List<String>>();

		List<String> detailNodes = Arrays.asList("Student_Details", "Student_Test_Details", "Item_Details", "Item_DataPoint_Score_Details", "Score");
		String file;
		DocumentList result;

		Map<String, String> tempColumns = new TreeMap<String, String>();;
		int scoreCount = 0;
		String dataPoint = "";
		String tempText;
		boolean textAdded = false;

		public void setTarget(String s){
			file = s;
		}

		private void parseDocument() {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			try{
				SAXParser sp = spf.newSAXParser();
				sp.parse(new File(file), this);		
			}catch(Exception e){
				e.printStackTrace();
			}

			generateDocumentList();
		}

		private void generateDocumentList(){
			List<String> filenames = new ArrayList<String>();
			for(int i = 0; i < text.size(); i++){
				filenames.add(file);
			}
			Map<String, List<String>> allAnnotations = new TreeMap<String, List<String>>();

			Set<String> acceptable = new HashSet<String>(){{
				add("Final_ScoreA");
				add("Final_ScoreB");
				add("Final_ScoreC");
				add("Student_Test_ID");
				add("Vendor_Student_ID");
				add("Gender");
				add("Grade");
				add("IEP");
				add("Item_ID");
				add("LEP");
				add("Ethnicity");
			}};
			for(String s : annotations.keySet()){
				if(acceptable.contains(s)){
					allAnnotations.put(s, new ArrayList<String>());					
				}
			}
			allAnnotations.put("text", new ArrayList<String>());

			String check = annotations.containsKey("Final_ScoreA")?"Final_ScoreA":"Final_Score";
			for(int i = 0; i < text.size(); i++){
				String score = annotations.get(check).get(i);
				try{
					Double scoreDbl = Double.parseDouble(score);
				}catch(Exception e){
					continue;
				}
				for(String s : annotations.keySet()){
					if(allAnnotations.containsKey(s)){
						allAnnotations.get(s).add(annotations.get(s).get(i));
					}
				}
				allAnnotations.get("text").add(text.get(i).replaceAll("&nbsp;", " "));

			}
			result = new DocumentList(filenames, null, allAnnotations, null);
			result.setName(file);
		}

		public DocumentList getDocumentList(){
			return result;
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			//reset
			tempText = "";
			if(detailNodes.contains(qName)){
				int count = attributes.getLength();
				for(int i = count-1; i >= 0; i--){
					String key = attributes.getLocalName(i);
					if(key.equals("Data_Point")){
						dataPoint = attributes.getValue(i);
					}else{
						key += dataPoint;
						if(qName.equals("Score")){
							key += scoreCount;
						}
						tempColumns.put(key, attributes.getValue(i));						
					}
				}
			}
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			tempText = new String(ch,start,length);
			if(tempText.trim().length()>0){
				//				System.out.println("Adding text of length " + length + "!");
				text.add(tempText);
				textAdded = true;
			}
		}
		int index = 0;

		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(qName.equalsIgnoreCase("Score")){
				scoreCount++;
			}
			if(qName.equalsIgnoreCase("Student_Details")){
				for(String s : tempColumns.keySet()){
					if(!annotations.containsKey(s)){
						annotations.put(s, new ArrayList<String>());
						for(int i = 0; i < index; i++){
							annotations.get(s).add("");
						}		
					}
				}
				for(String s : annotations.keySet()){
					if(tempColumns.containsKey(s)){
						annotations.get(s).add(tempColumns.get(s));
					}else{
						annotations.get(s).add("");
					}
				}
				if(!textAdded){
					text.add("");
				}
				tempColumns.clear();
				textAdded = false;
				index++;
			}
			if(qName.equalsIgnoreCase("Item_DataPoint_Score_Details")){
				scoreCount = 0;
				dataPoint = "";
			}
		}
	}

	@Override
	public String getDescription()
	{
		return "CTB XML Files";
	}
}

class CTBExporter extends Chef{
	public static void main(String[] args){
		Set<String> test = new TreeSet<String>(){{
			File folder = new File("/Users/emayfiel/CTBSampleSize");
			for(File s : folder.listFiles()){
				if(!s.getName().startsWith(".") && s.getName().endsWith("xml")){
					add(s.getAbsolutePath());					
				}
			}
		}};
		CTBParser parser = new CTBParser();
		CSVExporter export = new CSVExporter();
		DocumentListTableModel model = new DocumentListTableModel(null);
		for(String t : test){
			try{
				DocumentList dl = parser.parseDocumentList(new TreeSet<String>(Arrays.asList(t)), Charset.forName("UTF-8"));
				System.out.println(dl.getSize());
				model.setDocumentList(dl);
				System.out.println(model.getColumnCount() + ", " + model.getRowCount());
				export.exportToCSV(model, new File(t.replace(".xml", ".convert.csv")));				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}