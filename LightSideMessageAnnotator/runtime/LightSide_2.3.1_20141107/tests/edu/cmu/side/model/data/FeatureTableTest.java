package edu.cmu.side.model.data;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JPanel;

import junit.framework.TestCase;

import org.junit.Test;

import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.plugin.FeaturePlugin;

public class FeatureTableTest extends TestCase{
	private static final int NUM_GALLUP_INSTANCES = 942;
	private static final int NUM_GALLUP_FEATURES = 347;
	static StatusUpdater textUpdater = new StatusUpdater()
	{

		@Override
		public void update(String updateSlot, int slot1, int slot2)
		{
			if(!quiet)
				System.err.println(updateSlot+": "+slot1 + "/"+slot2);
		}

		@Override
		public void update(String update)
		{
			if(!quiet)
				System.err.println(update);	
		}

		@Override
		public void reset()
		{
			// TODO Auto-generated method stub

		}
	};
	static String[] files = {"testData/Gallup.csv"};
//	static String[] numericFiles = {"heuristicTest.csv"};
	static DocumentList docList;
//	static DocumentList numericDocList;
	Boolean hasChanged = true;
	static Boolean quiet = false;
	
	//a legitimate unigram feature extractor.
	static FeaturePlugin featureMaker = new FeaturePlugin()
	{
		
		@Override
		public List<String> tokenize(Feature f, String s)
		{
			return Arrays.asList(s);
		}
		
		@Override
		public boolean isTokenized(Feature f)
		{
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
		{
			Collection<FeatureHit> hits = new ArrayList<FeatureHit>();
			
			Map<String, List<String>> textColumns = documents.getCoveredTextList();
			for(String column : textColumns.keySet())
			{
				List<String> textColumn = textColumns.get(column);
				for(int i = 0; i < textColumn.size(); i++)
				{
					String text = textColumn.get(i);
					HashSet<String> words = new HashSet(Arrays.asList(text.split("\\s+")));
					for(String word : words)
					{
						Feature foo = Feature.fetchFeature("dummy", word, Type.BOOLEAN, this);
						hits.add(new FeatureHit(foo, Boolean.TRUE, i));
					}
				}
			}
			return hits;
			
		}

		@Override
		public String getOutputName()
		{
			// TODO Auto-generated method stub
			return "Dummy Extractor";
		}

		@Override
		protected Component getConfigurationUIForSubclass()
		{
			// TODO Auto-generated method stub
			return new JPanel();
		}

		@Override
		public Map<String, String> generateConfigurationSettings()
		{
			// TODO Auto-generated method stub
			return new HashMap<String, String>();
		}

		@Override
		public void configureFromSettings(Map<String, String> settings)
		{
			// TODO Auto-generated method stub
			
		}
	};
	static Collection<FeatureHit> featureHits;
//	static Collection<FeatureHit> numericFeatureHits;


	@Override
	public void setUp(){
		if(hasChanged){
			docList = new DocumentList(new HashSet<String>(Arrays.asList(files)));
			docList.guessTextAndAnnotationColumns();
			featureHits = featureMaker.extractFeatureHitsForSubclass(docList, textUpdater);
//			numericDocList = new DocumentList(new HashSet<String>(Arrays.asList(numericFiles)));
//			numericDocList.guessTextAndAnnotationColumns();
//			numericFeatureHits = featureMaker.extractFeatureHitsForSubclass(numericDocList, textUpdater);
			hasChanged=false;
			
		}
	}
	@Test
	public void testFeatureTableConstructionNominal(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		assertNotNull(ft);
		assertEquals(ft.getSize(),NUM_GALLUP_INSTANCES);
	}
	@Test
	public void testFeatureTableConstructionNullAnnotation(){
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, null, Feature.Type.NOMINAL);
		assertNotNull(ft);
		assertEquals(ft.getSize(),NUM_GALLUP_INSTANCES);
	}
	@Test
	public void testFeatureTableConstructionNullType(){
		int thresh = 5;
		String annotation = docList.currentAnnotation;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, null);
		assertNotNull(ft);
		assertEquals(ft.getSize(),NUM_GALLUP_INSTANCES);
	}
	@Test
	public void testFeatureTableConstructionBoolean(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.BOOLEAN);
		assertNotNull(ft);
		assertEquals(ft.getSize(),NUM_GALLUP_INSTANCES);
	}
	/*
	@Test
	public void testFeatureTableConstructionNumeric(){
		String annotation = numericDocList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(numericDocList, numericFeatureHits, thresh, annotation, Feature.Type.NUMERIC);
		assertNotNull(ft);
		assertEquals(ft.getSize(),33);
	}
	@Test
	public void testGetNumericBreakPoints(){
		String annotation = numericDocList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(numericDocList, numericFeatureHits, thresh, annotation, Feature.Type.NUMERIC);
		ArrayList<Double> numericBreakPoints = ft.getNumericBreakpoints();
		assertEquals(numericBreakPoints.size(), 4);
	}
	@Test
	public void testGetNumericBreakClassValuesNum(){
		String annotation = numericDocList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(numericDocList, numericFeatureHits, thresh, annotation, Feature.Type.NUMERIC);
		assertEquals(ft.getNumericClassValues("irrelevant").length, 33);
	}
	@Test
	public void testGetNumericBreakClassValuesNom(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		int numberOfPOS = 0;
		
		for(Double dub: ft.getNumericClassValues(" NEG")){
			if(!dub.equals(1.0)){
				numberOfPOS++;
			}
		}
		assertEquals(ft.getNumericClassValues(" NEG").length-numberOfPOS, 150);
	}
	*/
	@Test
	public void testGetNominalClassValues(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		assertEquals(ft.getNominalClassValues().size(), NUM_GALLUP_INSTANCES);
	}
	@Test
	public void testGetSizeWithDocuments(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		assertEquals(ft.getSize(), NUM_GALLUP_INSTANCES);
	}
	@Test
	public void testSetAndGetName(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		ft.setName("newName");
		assertEquals("newName", ft.getName());
	}
	@Test
	public void testSetAndGetThreshold(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		ft.setThreshold(10);
		assertEquals(ft.getThreshold(), 10);
	}
	@Test
	public void testGetDocumentsQuickly(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		assertEquals(ft.getDocumentListQuickly(), ft.getDocumentList());
	}
	@Test
	public void testGetFeatureSet(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		assertEquals(NUM_GALLUP_FEATURES, ft.getFeatureSet().size());
	}
	@Test
	public void testGetSortedFeatures(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		assertEquals(NUM_GALLUP_FEATURES, ft.getSortedFeatures().size());
		assertEquals(".", ((TreeSet<Feature>) ft.getSortedFeatures()).first().toString());
	}
	@Test
	public void testGetHitsForFeature(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		Iterator<Feature> iter = ft.getFeatureSet().iterator();
		
		Collection<FeatureHit> hitsForFeat = ft.getHitsForFeature(Feature.fetchFeature("dummy", ".", Type.BOOLEAN));
		System.out.println(hitsForFeat.toString());
		assertEquals(6, hitsForFeat.size());
	}
	@Test
	public void testGetHitsForDocument(){
		String annotation = docList.currentAnnotation;
		int thresh = 5;
		FeatureTable ft = new FeatureTable(docList, featureHits, thresh, annotation, Feature.Type.NOMINAL);
		Collection<FeatureHit> features = ft.getHitsForDocument(0);
		System.out.println(features.size());
		System.out.println(features);
		assertEquals(7, features.size());
	}
	
	
}