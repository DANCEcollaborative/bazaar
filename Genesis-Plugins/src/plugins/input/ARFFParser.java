package plugins.input;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import com.yerihyo.yeritools.csv.CSVReader;

import edu.cmu.side.Workbench;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.plugin.FileParser;

public class ARFFParser extends FileParser {


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
			int ending = filename.lastIndexOf(".arff");
			localName += filename.substring(filename.lastIndexOf("/")+1, ending==-1?filename.length():ending) + " ";
			
			File f = new File(filename);
			if(!f.exists())
				f = new File(Workbench.dataFolder.getAbsolutePath(), filename.substring(Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\"))+1));
			

			try
			{
				
				DataSource source = new DataSource(f.getPath());
				Instances data = source.getDataSet();
				 // setting class attribute if the data format does not provide this information
				 // For example, the XRFF format saves the class attribute information as well
				 if (data.classIndex() == -1)
				   data.setClassIndex(data.numAttributes() - 1);
				 
				 currentAnnotation = data.classAttribute().name();
				 
				 //add filenames
				 for(int i = 0; i < data.numInstances(); i++)
				 {
					 filenamesPerInstance.add(f.getName());
				 }
				 
				 //add attributes
				 for(int a = 0; a < data.numAttributes(); a++)
				 {
					 Attribute att = data.attribute(a);
					 ArrayList<String> column = new ArrayList<String>(data.numInstances());
					allAnnotations.put(att.name(), column);
					 for(int i = 0; i < data.numInstances(); i++)
					 {
						 Instance inst = data.get(i);
						 if(inst.isMissing(att))
						 {
							 column.add("?");
						 }
						 else
						 switch(att.type())
						 {
							 case Attribute.DATE:
							 case Attribute.STRING:
							 case Attribute.NOMINAL:
								 column.add(inst.stringValue(att));
								 break;
							 
							 default: //numeric
								 column.add(Double.toString(inst.value(att)));
						 }
					 }
					 
					 
				 }
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
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
		return (filename.toLowerCase().endsWith(".arff"));
	}
	
	@Override
	public String getDescription()
	{
		return "ARFF (Weka) Files";
	}

}
