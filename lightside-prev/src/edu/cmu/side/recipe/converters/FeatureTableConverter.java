package edu.cmu.side.recipe.converters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit.HitLocation;

public class FeatureTableConverter implements Converter{

	@Override
	public boolean canConvert(Class clazz) {
		return clazz.equals(FeatureTable.class);
	}

	@Override
	public void marshal(Object obj, HierarchicalStreamWriter writer,
			MarshallingContext context) 
	{
		if(obj==null)
		{
			return;
		}
		FeatureTable table = (FeatureTable) obj;

		if(table.getName().endsWith(Recipe.PREDICTION_SUFFIX))
		{
			convertPrediction(table,writer,context);
		}
		//If we need more models in the future, put them here
		//Obviously the docList null check is a short-sighted, quickfix.
		//In the future we should TODO: make an identifiable save type located in the object.
		else
		{
			convertModel(table,writer,context);
		}
	}
	public void convertPrediction(FeatureTable table, HierarchicalStreamWriter writer, MarshallingContext context){
		writer.addAttribute("type", "prediction");
		
		writer.startNode("name");
		writer.setValue(table.getName());
		writer.endNode();
		
		//should be dummy document list, with size = 0
		DocumentList docList = table.getDocumentList();
		if(docList.getSize() > 0)
			System.out.println("FeatureTableConverter: prediction table has doclist size "+docList.getSize());
		
		writer.startNode("DocumentList");
		context.convertAnother(docList);
		writer.endNode();
		
		writer.startNode("Threshold");
		writer.setValue(((Integer)table.getThreshold()).toString());
		writer.endNode();
		
		writer.startNode("Annotation");
		writer.setValue(table.getAnnotation());
		writer.endNode();
		
		writer.startNode("Type");
		writer.setValue(table.getClassValueType().toString());
		writer.endNode();
		
		writer.startNode("LabelArray");
		for(String label: table.getLabelArray()){
			writer.startNode("Label");
			writer.setValue(label);
			writer.endNode();
		}
		writer.endNode();
		
		writer.startNode("Features");
		Set<Feature> featureSet = table.getFeatureSet();
		ArrayList<Feature> featureList = new ArrayList<Feature>(featureSet);
		for (Feature feat : featureList) {
			writer.startNode("Feature");
			writer.addAttribute("value", feat.encode());
			/*writer.addAttribute("Type", feat.getFeatureType().toString());
			writer.addAttribute("Prefix", feat.getExtractorPrefix());
			if(feat.getFeatureType().equals(Feature.Type.NOMINAL)){
				for(String value: feat.getNominalValues()){
					writer.startNode("Value");
					writer.setValue(value);
					writer.endNode();
				}
			}
			writer.startNode("FeatureValue");
			writer.setValue(feat.toString());
			writer.endNode();*/

			writer.endNode();
		}
		writer.endNode();
		
		
	}
	public void convertModel(FeatureTable table, HierarchicalStreamWriter writer, MarshallingContext context){
		writer.addAttribute("type", "default");
		
		writer.startNode("name");
		writer.setValue(table.getName());
		writer.endNode();
		
		DocumentList docList = table.getDocumentList();
		writer.startNode("DocumentList");
		context.convertAnother(docList);
		writer.endNode();
		
		writer.startNode("Threshold");
		writer.setValue(((Integer)table.getThreshold()).toString());
		writer.endNode();
		
		writer.startNode("Annotation");
		writer.setValue(table.getAnnotation());
		writer.endNode();
		
		writer.startNode("Type");
		writer.setValue(table.getClassValueType().toString());
		writer.endNode();
		
		writer.startNode("Features");
		Set<Feature> featureSet = table.getFeatureSet();
		ArrayList<Feature> featureList = new ArrayList<Feature>(featureSet);
		for (Feature feat : featureList) {
			writer.startNode("Feature");
			writer.addAttribute("value", feat.encode());
			/*writer.addAttribute("Type", feat.getFeatureType().toString());
			writer.addAttribute("Prefix", feat.getExtractorPrefix());
			if(feat.getFeatureType().equals(Feature.Type.NOMINAL)){
				for(String value: feat.getNominalValues()){
					writer.startNode("Value");
					writer.setValue(value);
					writer.endNode();
				}
			}
			writer.startNode("FeatureValue");
			writer.setValue(feat.toString());
			writer.endNode();*/
			
			
			for(FeatureHit hit: table.getHitsForFeature(feat)){
				writer.startNode("Hit");
				writer.addAttribute("doc", ((Integer)hit.getDocumentIndex()).toString());
				writer.addAttribute("value", hit.getValue().toString());
				if(hit.getClass().equals(LocalFeatureHit.class))
				{
					writer.startNode("loc");
					String hitLocs= "";
					for(HitLocation hitLoc: ((LocalFeatureHit) hit).getHits()){
						hitLocs+=hitLoc.getColumn() +",";
						hitLocs+=((Integer) hitLoc.getStart()).toString()+",";
						hitLocs+=((Integer) hitLoc.getEnd()).toString()+";";
					}
					if(!hitLocs.isEmpty())
						hitLocs=hitLocs.substring(0,hitLocs.length()-1);
					writer.setValue(hitLocs);
					writer.endNode();
				}
				writer.endNode();
			}
			writer.endNode();
		}
		writer.endNode();




	}
	//DocumentList sdl, 
	//Collection<FeatureHit> hits, int thresh, 
	//String annotation, Feature.Type type
	@Override
	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		Object toReturn = null;
		if(reader.getAttribute("type").equals("default")){
			toReturn = readDefault(reader,context);
		} else if (reader.getAttribute("type").equals("prediction")){
			toReturn = readPrediction(reader,context);
		} else {
			throw new UnsupportedOperationException("XML file cannot be parsed");
		}
		return toReturn;
	}
	
	private Object readPrediction(HierarchicalStreamReader reader, UnmarshallingContext context){
		reader.moveDown();
		String tableName = reader.getValue();
		reader.moveUp();
		
		reader.moveDown();
		//Need to do a null check here
		DocumentList docList =(DocumentList)context.convertAnother(null, DocumentList.class);
		reader.moveUp();
		
		reader.moveDown();
		Integer threshold = Integer.parseInt(reader.getValue());
		reader.moveUp();
		reader.moveDown();
		String currentAnnotation = reader.getValue();
		reader.moveUp();
		reader.moveDown();
		String typeString = reader.getValue();
		Feature.Type featTypeTotal = Feature.Type.valueOf(typeString);
		reader.moveUp();
		ArrayList<String> labelArrayList = new ArrayList<String>();
		reader.moveDown();
		while(reader.hasMoreChildren()){
			reader.moveDown();
			labelArrayList.add(reader.getValue());
			reader.moveUp();
		}
		reader.moveUp();
		String[] labelArray = Arrays.copyOf(labelArrayList.toArray(), labelArrayList.size(),String[].class);
		
		
		reader.moveDown();
		ArrayList<Feature> features = new ArrayList<Feature>();
		while(reader.hasMoreChildren()){
			reader.moveDown();
			String encoded = reader.getAttribute("value");
			features.add(Feature.fetchFeature(encoded));
			/*String type = reader.getAttribute("Type");
			Feature.Type featType = Feature.Type.valueOf(type);
			String prefix = reader.getAttribute("Prefix");
			reader.moveDown();

			String value = reader.getValue();
			FeatureFetcher fetcher = new BasicFeatures();
			Feature toAdd = Feature.fetchFeature(prefix, value, featType, fetcher);
			features.add(toAdd);
			reader.moveUp();*/
			reader.moveUp();
		}
		reader.moveUp();
		
		
		FeatureTable predictTable = new FeatureTable(tableName, threshold, currentAnnotation, featTypeTotal, labelArray, features, docList);
//		FeatureTable predictTable = new FeatureTable(tableName, threshold, currentAnnotation, featTypeTotal, labelArray, features, nominalClassValues, numClassValues, docList);
		
		return predictTable;
	}
	
	private Object readDefault(HierarchicalStreamReader reader, UnmarshallingContext context){
		
		reader.moveDown();
		String tableName = reader.getValue();
		reader.moveUp();
		

		ArrayList<Feature> features = new ArrayList<Feature>();
		ArrayList<FeatureHit> hits = new ArrayList<FeatureHit>();
		
		reader.moveDown();
		//Need to do a null check here
		DocumentList docList =(DocumentList)context.convertAnother(null, DocumentList.class);
		reader.moveUp();
		
		reader.moveDown();
		Integer threshold = Integer.parseInt(reader.getValue());
		reader.moveUp();
		reader.moveDown();
		String annotation = reader.getValue();
		reader.moveUp();
		reader.moveDown();
		Feature.Type largerType = Feature.Type.valueOf(reader.getValue());
		reader.moveUp();
		reader.moveDown();
		while(reader.hasMoreChildren()){
			reader.moveDown();
			String encoded = reader.getAttribute("value");
			Feature feature = Feature.fetchFeature(encoded);
			
			/*String type = reader.getAttribute("Type");
			Feature.Type featType = Feature.Type.valueOf(type);
			String prefix = reader.getAttribute("Prefix");
			
			reader.moveDown();
			String featureName = reader.getValue();
			FeatureFetcher fetcher = new BasicFeatures();
			Feature toAdd = Feature.fetchFeature(prefix, featureName, featType, fetcher);
			features.add(toAdd);
			reader.moveUp();*/
			
//			ArrayList<FeatureHit> localFeatureHits = new ArrayList<FeatureHit>();
			while(reader.hasMoreChildren()){
				reader.moveDown();
				Integer docIndex = Integer.parseInt(reader.getAttribute("doc"));
				Object finalValue = "";
				String stringValue = reader.getAttribute("value");
				switch(feature.getFeatureType()){
				case BOOLEAN:
					Boolean boolValue = Boolean.valueOf(stringValue);
					finalValue = (Object) boolValue;
					break;
				case NOMINAL: case STRING:
					finalValue = (Object) stringValue;
					break;
				case NUMERIC:
					Number numericValue = Double.parseDouble(stringValue);
					finalValue = (Object) numericValue;
					break;
				}
				
				if(reader.hasMoreChildren())
				{
					//there may be zero or more hit locations
					reader.moveDown();
					String locations = reader.getValue();
					ArrayList<HitLocation> hitLoc = new ArrayList<HitLocation>();
					if(!locations.isEmpty())
					{
						String[] parsed = locations.split(";");
						for(String str: parsed){
							String[] information = str.split(",");
							if(information.length==3){
								String column = information[0];
								Integer start = Integer.parseInt(information[1]);
								Integer end = Integer.parseInt(information[2]);
								hitLoc.add(new HitLocation(column,start,end));
							}
						}
					}
					hits.add(new LocalFeatureHit(feature, finalValue, docIndex, hitLoc));
					reader.moveUp();
				}
				else	
				{
					//this is not a local feature hit.
					hits.add(new FeatureHit(feature, finalValue, docIndex));
				}
				reader.moveUp();
			}
			reader.moveUp();
		}
		reader.moveUp();
		FeatureTable table = new FeatureTable(docList, hits, threshold, annotation, largerType);
		table.setName(tableName);
		return table;
	}

}
