package plugins.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.yerihyo.yeritools.csv.CSVReader;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.plugin.FileParser;

public class CSVParser extends FileParser {


	@Override
	public DocumentList parseDocumentList(Set<String> filenames, Charset encoding) throws IOException
	{
		TreeMap<String, List<String>> allAnnotations = new TreeMap<String, List<String>>();
		if(filenames==null) return new DocumentList(null,null,null,null);
		CSVReader in;
		String currentAnnotation = null;
		int totalLines = 0;
		String localName = "";
		ArrayList<String> filenamesPerInstance = new ArrayList<String>();
		
		for(String filename : filenames)
		{
			//Essentially useless check at this point in time but the strip is necessary.
			int ending = filename.lastIndexOf(".csv");
			localName += filename.substring(filename.lastIndexOf("/")+1, ending==-1?filename.length():ending) + " ";
			//			ArrayList<Integer> blanks = new ArrayList<Integer>();
			//			ArrayList<Integer> extras = new ArrayList<Integer>();
			int lineID = 0;


			File f = new File(filename);
			if(!f.exists())
				f = new File(Workbench.dataFolder.getAbsolutePath(), filename.substring(Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\"))+1));
			in = new CSVReader(new InputStreamReader(new FileInputStream(f), encoding));//new FileReader(f));
			String[] headers = in.readNextMeaningful();
			
			// If the input file has a unicode BOM mark, simply remove it from the first header name
			if (headers[0].startsWith("\ufeff")) {
				headers[0] = headers[0].substring(1);
			}
			
			List<Integer> annotationColumns = new ArrayList<Integer>();
			for(int i = 0; i < headers.length; i++){
				headers[i] = headers[i].trim();
				if(headers[i].length()>0){
					annotationColumns.add(i);
				}
			}

			for(String annotation : headers){
				if(annotation.length() > 0 && !allAnnotations.containsKey(annotation)){
					allAnnotations.put(annotation, new ArrayList<String>());
					if(totalLines>0){
						String[] fill = new String[totalLines];
						Arrays.fill(fill, "");
						allAnnotations.get(annotation).addAll(Arrays.asList(fill));
					}
				}
			}

			String[] line;

			while((line = in.readNextMeaningful()) != null)
			{
				String[] instance = line;
//				String[] instance = new String[line.length];
//				for(int i = 0; i < line.length; i++){
//					instance[i] = line[i].replaceAll("[^\r\n\\p{ASCII}]", "");
//				}
				for(int i = 0; i < instance.length; i++){
					String value = instance[i];
					if(annotationColumns.contains(i)){
						if(value.length()>0){
							allAnnotations.get(headers[i]).add(value);
						}else{
							allAnnotations.get(headers[i]).add("");
							//								blanks.add(lineID);
						}
					}else{
						//							extras.add(lineID);
					}
				}

				filenamesPerInstance.add(filename);
				lineID++;
			}
			//Now, fill unfilled areas with empty strings
			Set<String> toRemoveSet = new HashSet<String>(Arrays.asList(headers));
			Set<String> removedAnnotations = new HashSet<String>(allAnnotations.keySet());
			removedAnnotations.removeAll(toRemoveSet);
			String[] empty = new String[lineID];
			Arrays.fill(empty, "");
			for(String emptyAnnotation : removedAnnotations){
				allAnnotations.get(emptyAnnotation).addAll(Arrays.asList(empty));
			}

			totalLines += lineID;
		}
		localName = localName.trim();
		localName=localName.substring(localName.lastIndexOf(java.io.File.separator)+1);
		
		DocumentList docList = new DocumentList(filenamesPerInstance, null, allAnnotations, currentAnnotation);
		docList.setName(localName);
		return docList;
	}

	@Override
	public boolean canHandle(String filename) 
	{
		return (filename.toLowerCase().endsWith(".csv"));
	}
	
	@Override
	public String getDescription()
	{
		return "CSV Files";
	}

}
