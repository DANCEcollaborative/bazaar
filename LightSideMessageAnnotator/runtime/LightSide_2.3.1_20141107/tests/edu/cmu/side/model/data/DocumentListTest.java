package edu.cmu.side.model.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import junit.framework.TestCase;

import org.hibernate.transform.PassThroughResultTransformer;
import org.junit.Test;

import edu.cmu.side.model.feature.Feature;

public class DocumentListTest extends TestCase{
	String delimiter = System.getProperty("file.separator");
	List<Map<String,String>> rows;
	Collection<String> columns;
	DocumentList dList;
	List<String> instances;
	
	List<String> fileNames;
	Set<String> fileSet;
	@Override
	public void setUp(){
		//Row and Columns
		rows = new ArrayList<Map<String,String>>();
		columns = new ArrayList<String>();
		
		//instances
		instances = new ArrayList<String>();
	}
	/*
	 * Testing for constructor #1:
	 * Arguments for constructor #1:
	 * List<Map<String, String>> rows, Collection<String> columns
	 * This essentially creates the table
	 * 
	 */
	@Test
	public void testRowColumnConstructor(){
		columns.add("text");
		columns.add("value");
		Map<String,String> row1 = new HashMap<String,String>();
		Map<String,String> row2 = new HashMap<String,String>();
		Map<String,String> row3 = new HashMap<String,String>();
		row1.put("text", "First");
		row1.put("value", "FirstVal");
		row2.put("text", "Second");
		row3.put("value", "SecondVal");
		rows.add(row1);
		rows.add(row2);
		rows.add(row3);
		dList = new DocumentList(rows, columns);
		assertEquals(dList.allAnnotations.size(), 2);
		assertEquals(dList.getTextColumns().size(), 0);
		assertEquals(dList.filenameList.size(), 3);
	}

	@Test
	public void testRowColumnEmpty(){
		dList = new DocumentList(rows, columns);
		assertEquals(dList.allAnnotations.size(),0);
		assertEquals(dList.getTextColumns().size(),0);
		assertEquals(dList.filenameList.size(),0);
	}
	/*
	 * Testing for constructor #2:
	 * Arguments for constructor #2:
	 * List<String> instances
	 * Add text columns
	 * Side-effects to test:
	 * annotation, textcolumn, fileNameList
	 */

	@Test
	public void testInstancesConstructor(){
		for(int i = 0; i < 5; i++){
			instances.add("i");
		}
		dList = new DocumentList(instances);
		assertEquals(dList.allAnnotations.size(),0);
		assertEquals(dList.getTextColumns().size(),1);
		assertEquals(dList.getFilenameList().size(),5);
	}
	@Test
	public void testInstancesEmpty(){
		dList = new DocumentList(instances);
		assertEquals(dList.allAnnotations.size(),0);
		assertEquals(dList.getTextColumns().size(),1);
		assertEquals(dList.getFilenameList().size(),0);
	}
	/*
	 * Testing for constructor #3:
	 * Arguments for constructor #3:
	 * List<String> text, Map<String, List<String>> annotations
	 * Add text columns and annotated data
	 * Side-effects to test:
	 * 
	 */

	@Test
	public void testInstancesAndAnnotationsConstruction(){
		for(int i = 0; i < 5; i++){
			instances.add("i");
		}
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		annMap.put("otherValue", instances);
		dList = new DocumentList(instances, annMap);
		assertEquals(dList.allAnnotations.size(),2);
		assertEquals(dList.getTextColumns().size(),1);
		assertEquals(dList.getFilenameList().size(),5);
	}
	/*
	 * Testing for constructor #4:
	 * Arguments for constructor #4:
	 * List<String> filenames, Map<String, List<STring>> texts, Map<String, List<String>> Annotations,
	 * |-> String currentAnnot
	 * This is a simple assignment constructor that assigns the parameters to their respective fields
	 * |-> Without doing anything else.
	 * Side-effects to test:
	 * 
	 */

	@Test
	public void testAssignmentConstructor(){
		String[] files = {"Doc","Doc","Doc"};
		fileNames = new ArrayList<String>(Arrays.asList(files));
		Map<String, List<String>> texts = new HashMap<String, List<String>>();
		String[] values = {"one","two","three"};
		ArrayList<String> firstVals = new ArrayList<String>(Arrays.asList(values));
		texts.put("Value", firstVals);
		String currentAnnot = "Value";
		dList = new DocumentList(fileNames, texts,texts,currentAnnot);
		assertEquals(dList.filenameList, fileNames);
		assertTrue(dList.getTextColumns().contains("Value"));
		assertEquals(dList.currentAnnotation,currentAnnot);
	}
	/*
	 * Testing for constructor #5:
	 * Arguments for constructor #5:
	 * Set<String> filenames
	 * Parse and create whole DocumentList out of file list
	 * Side-effects to test: annotationList. Size. AssureNull on currentAnnotation and size 0 of textColumns.
	 * 
	 */
	@Test
	public void testSingleFileName(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		assertNotNull(docList);
		assertEquals(docList.getSize(),942);
		String[] annNames = docList.getAnnotationNames();
		assertEquals(annNames.length,5);
		assertEquals(docList.getAnnotationNames()[0], "Age");
		assertEquals(docList.getAnnotationNames()[1], "Gender");
		assertNull(docList.currentAnnotation);
		assertEquals(docList.getTextColumns().size(),0);
	}
	/* Needs to be fixed with new test files.
	@Test
	public void testMultipleFileNameDifferentHeaders(){
		Set<String> fileNames = new TreeSet<String>();
<<<<<<< local
		fileNames.add("testData/Gallup.csv");
		fileNames.add("test2.csv");
=======
		fileNames.add("testData"+delimiter+"MovieReviews.csv");
		fileNames.add("testData"+delimiter+"test2.csv");
>>>>>>> other
		DocumentList docList = new DocumentList(fileNames);
		int size = 0;
		for(List<String> arString: docList.allAnnotations.values()){
			if(size==0){
				size=arString.size();
			} else {
				assertEquals(arString.size(),size);
			}
		}
		assertEquals(docList.getSize(), 302);
		assertEquals(docList.getAnnotationNames().length, 4);

	}
*/
	@Test
	public void testMultipleFileNameSameHeaders(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData"+delimiter+"sentiment_documents.csv");
		fileNames.add("testData"+delimiter+"sentiment_sentences.csv");
		DocumentList docList = new DocumentList(fileNames);
		int size = 0;
		for(List<String> arString: docList.allAnnotations.values()){
			if(size==0){
				size=arString.size();
			} else {
				assertEquals(arString.size(),size);
			}
		}
		assertEquals(docList.getSize(), 12662);
		assertEquals(docList.getAnnotationNames().length, 2);
	}

	/*
	 * Testing for constructor #6:
	 * Arguments for constructor #6:
	 * Set<String> filenames, String textCol
	 * Parse and create whole DocumentList out of filelist and assign textColumn
	 * Side-effects to test:
	 * 
	 */
	@Test
	public void testSingleFileNameWithText(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames, "text");
		assertNotNull(docList);
		assertEquals(docList.getSize(),942);
		String[] annNames = docList.getAnnotationNames();
		assertEquals(annNames.length,4);
		assertEquals(docList.getAnnotationNames()[0],"Age");
		assertNull(docList.currentAnnotation);
		assertEquals(docList.getTextColumns().size(),1);
		assertTrue(docList.getTextColumns().contains("text"));
	}
	@Test
	public void testSingleFileNameWithInvalidText(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		try{
			DocumentList docList = new DocumentList(fileNames, "invalid");
			fail("No illegal state exception caught");
		} catch (IllegalStateException e){
			
		}
	}
	/*
	 * Testing for constructor #7:
	 * Arguments for constructor #7:
	 * Set<STring> Filenames, String currentAnnot, String textCol
	 * Parse and create whole DocumentList out of filelist and assign textColumn as well as the current annotation
	 * Side-effects to test:
	 * 
	 */
	@Test
	public void testFilesAndTextAndCurrentAnnot(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames, "Vote","text");
		assertNotNull(docList);
		assertEquals(docList.getSize(),942);
		String[] annNames = docList.getAnnotationNames();
		assertEquals(annNames.length,4);
		assertEquals(docList.currentAnnotation,"Vote");
		assertEquals(docList.getAnnotationNames()[0],"Age");
		assertEquals(docList.getTextColumns().size(),1);
		assertTrue(docList.getTextColumns().contains("text"));
	}
	@Test
	public void testInvalidCurrentAnnot(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		try{
			DocumentList docList = new DocumentList(fileNames, "INVALID!","text");
			fail("should've caught IllegalStateException but didn't");
		} catch (IllegalStateException e){
			assertEquals(e.getMessage(), "Can't find the label column named INVALID! in provided file");
		}
	}

	/*
	 * Testing for constructor #8:
	 * Arguments for constructor #8:
	 * String Instance
	 * Add text columns after wrapping instance into List.
	 */

	@Test
	public void testTextColumnConstructor(){
		String instance = "instance";
		DocumentList dList = new DocumentList(instance);
		assertEquals(dList.allAnnotations.size(),0);
		assertEquals(dList.getTextColumns().size(),1);
		assertEquals(dList.getFilenameList().size(),1);
	}
	/*---------------------------------------------------------------------------------------------------*/
	//Various Method Tests Begins
	
	@Test
	public void testGetPossibleAnn(){
		List<String> instances = new ArrayList<String>();
		for(int i = 0; i < 5; i++){
			instances.add("i");
		}
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		annMap.put("otherValue", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		assertEquals(dList.getPossibleAnn("value"),new HashSet<String>(instances));
	}
	@Test
	public void testGetPossibleAnnNull(){
		List<String> instances = new ArrayList<String>();
		for(int i = 0; i < 5; i++){
			instances.add("i");
		}
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		annMap.put("otherValue", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		assertEquals(dList.getPossibleAnn("shouldBeNull"), new HashSet<String>());
	}
	@Test
	public void testGetValueTypeNumeric(){
		List<String> instances = new ArrayList<String>();
		for(int i = 0; i < 5; i++){
			instances.add("1");
		}
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		assertEquals(dList.getValueType("value"),Feature.Type.NUMERIC);
	}
	@Test 
	public void testGetValueTypeNominal(){
		List<String> instances = new ArrayList<String>();
		for(int i = 0; i < 5; i++){
			instances.add("i");
		}
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		assertEquals(dList.getValueType("value"),Feature.Type.NOMINAL);
	}
	@Test
	public void testGetValueTypeNull(){
		List<String> instances = new ArrayList<String>();
		for(int i = 0; i < 5; i++){
			instances.add("1");
		}
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		assertEquals(dList.getValueType(null),Feature.Type.NOMINAL);
	}
	@Test
	public void testSetNameAndGetName(){
		List<String> instances = new ArrayList<String>();
		instances.add("i");
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		dList.setName("testName");
		assertEquals("testName", dList.getName());
	}
	@Test
	public void testAllAnnotations(){
		List<String> instances = new ArrayList<String>();
		instances.add("i");
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		dList.setName("testName");
		assertEquals(dList.allAnnotations, dList.allAnnotations());
	}

	/* Needs to be rethought for better overall robustness.
	@Test
	public void testGuessTextColumnsAndAnnots(){
		Set<String> fileNames = new TreeSet<String>();
<<<<<<< local
		fileNames.add("testData/Gallup.csv");
=======
		fileNames.add("testData"+delimiter+"MovieReviews.csv");
>>>>>>> other
		DocumentList docList = new DocumentList(fileNames);
		assertNull(docList.currentAnnotation);
		assertEquals(docList.getTextColumns().size(),0);
		docList.guessTextAndAnnotationColumns();
		assertEquals(docList.currentAnnotation, "Vote");
		assertTrue(docList.getTextColumns().contains("text"));
	}
	@Test
	public void testGuessTextAndAnnotsNoClass(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData"+delimiter+"test1.csv");
		DocumentList docList = new DocumentList(fileNames);
		assertNull(docList.currentAnnotation);
		assertEquals(docList.getTextColumns().size(),0);
		docList.guessTextAndAnnotationColumns();
	}
	@Test
	public void testAlreadyHaveTextAndAnnots(){
		Set<String> fileNames = new TreeSet<String>();
<<<<<<< local
		fileNames.add("testData/Gallup.csv");
=======
		fileNames.add("testData"+delimiter+"MovieReviews.csv");
>>>>>>> other
		DocumentList docList = new DocumentList(fileNames);
		assertNull(docList.currentAnnotation);
		assertEquals(docList.getTextColumns().size(),0);
		docList.guessTextAndAnnotationColumns();
		assertEquals(docList.currentAnnotation, "Vote");
		assertTrue(docList.getTextColumns().contains("text"));
		docList.guessTextAndAnnotationColumns();
		assertEquals(docList.currentAnnotation, "Vote");
		assertTrue(docList.getTextColumns().contains("text"));
	}
<<<<<<< local
	
=======
	@Test
	public void testNotInClassList(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData"+delimiter+"heuristicTest.csv");
		DocumentList dList = new DocumentList(fileNames);
		assertNull(dList.currentAnnotation);
		assertEquals(dList.getTextColumns().size(),0);
		dList.guessTextAndAnnotationColumns();
		assertEquals(dList.currentAnnotation, "sbv");
		assertTrue(dList.getTextColumns().contains("sbt"));
	}
>>>>>>> other
	@Test
	public void testGuessText(){
		Set<String> fileNames = new TreeSet<String>();
<<<<<<< local
		fileNames.add("testData/Gallup.csv");
=======
		fileNames.add("testData"+delimiter+"heuristicTest.csv");
>>>>>>> other
		DocumentList dList = new DocumentList(fileNames);
		dList.setCurrentAnnotation("Vote");
		assertEquals(dList.currentAnnotation, "Vote");
		assertEquals(dList.getTextColumns().size(),0);
		dList.guessTextAndAnnotationColumns();
		assertEquals(dList.currentAnnotation, "text");
		assertTrue(dList.getTextColumns().contains("text"));
	}
	*/
	@Test
	public void testCurrAnnot(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList dList = new DocumentList(fileNames);
		assertEquals(0, dList.getTextColumns().size());
		assertNull(dList.currentAnnotation);
		dList.setTextColumn("text", true);
		dList.setCurrentAnnotation("Age");
		assertEquals(1, dList.getTextColumns().size());
		dList.guessTextAndAnnotationColumns();
		assertEquals("Age", dList.currentAnnotation);
		assertTrue(dList.getTextColumns().contains("text"));
	}
	

	
	@Test
	public void testGuessAnnot(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList dList = new DocumentList(fileNames);
		assertNull(dList.currentAnnotation);
		assertEquals(0, dList.getTextColumns().size());
		dList.guessTextAndAnnotationColumns();
		assertEquals("Vote", dList.currentAnnotation);
		assertEquals(1, dList.getTextColumns().size());
		assertTrue(dList.getTextColumns().contains("text"));
	}
	
	@Test
	public void testAddAnnotationsUpdateExisting(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.addAnnotation("exists", new ArrayList<String>(), false);
		docList.addAnnotation("exists", new ArrayList<String>(), false);
		docList.addAnnotation("exists", new ArrayList<String>(), false);
		assertTrue(docList.allAnnotations().containsKey("exists"));
		assertTrue(docList.allAnnotations().containsKey("exists (new)"));
		assertTrue(docList.allAnnotations().containsKey("exists (new) (new)"));
	}
	@Test
	public void testAddAnnotationNotUpdateExisting(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.addAnnotation("exists", new ArrayList<String>(), true);
		docList.addAnnotation("exists", new ArrayList<String>(), true);
		docList.addAnnotation("exists", new ArrayList<String>(), true);
		assertTrue(docList.allAnnotations().containsKey("exists"));
		assertFalse(docList.allAnnotations().containsKey("exists (new)"));
		assertFalse(docList.allAnnotations().containsKey("exists (new) (new)"));
	}
	@Test
	public void testGetCoveredTextList(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setTextColumn("text", true);
		List<String> textList = docList.textColumns.get("text");
		assertEquals(textList, docList.getCoveredTextList().get("text"));
	}
	@Test
	public void testGetPrintableTextAtSingleColumn(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setTextColumn("text", true);
		String expected = docList.getCoveredTextList().get("text").get(3);
		assertEquals(docList.getPrintableTextAt(3), expected);
	}
	@Test
	public void testGetPrintableTextAtMultipleColumns(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setTextColumn("text", true);
		docList.setTextColumn("State", true);
		String expected = "State:\n" + docList.getCoveredTextList().get("State").get(3) + "\n";
		expected += "text:\n" + docList.getCoveredTextList().get("text").get(3) + "\n";
		assertEquals(docList.getPrintableTextAt(3), expected);
	}
	
	@Test
	public void testTextColumnsAreDifferentiatedSetterAndGetters(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		boolean areDiff = docList.getTextColumnsAreDifferentiated();
		docList.setDifferentiateTextColumns(!areDiff);
		assertEquals(!areDiff, docList.getTextColumnsAreDifferentiated());
	}
	
	@Test
	public void testGetTextFeatureNameDifferentiated(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setDifferentiateTextColumns(true);
		String expected = "column:basename";
		assertEquals(expected, docList.getTextFeatureName("basename","column"));
	}
	@Test
	public void testGetTextFeatureNameNonDifferentiated(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setDifferentiateTextColumns(false);
		String expected = "basename";
		assertEquals(expected, docList.getTextFeatureName("basename","column"));
	}
	@Test
	public void testGetFileName(){
		String[] files = {"Doc","Doc2","Doc"};
		List<String> fileNames = new ArrayList<String>(Arrays.asList(files));
		Map<String, List<String>> texts = new HashMap<String, List<String>>();
		String[] values = {"one","two","three"};
		ArrayList<String> firstVals = new ArrayList<String>(Arrays.asList(values));
		texts.put("Value", firstVals);
		String currentAnnot = "Value";
		DocumentList dList = new DocumentList(fileNames, texts,texts,currentAnnot);
		assertEquals(dList.getFilename(1), "Doc2");
	}
	@Test
	public void testGetFileList(){
		String[] files = {"Doc","Doc2","Doc"};
		List<String> fileNames = new ArrayList<String>(Arrays.asList(files));
		Map<String, List<String>> texts = new HashMap<String, List<String>>();
		String[] values = {"one","two","three"};
		ArrayList<String> firstVals = new ArrayList<String>(Arrays.asList(values));
		texts.put("Value", firstVals);
		String currentAnnot = "Value";
		DocumentList dList = new DocumentList(fileNames, texts,texts,currentAnnot);
		assertEquals(dList.getFilenameList(), Arrays.asList(files));
	}
	
	@Test
	public void testGetFileNames(){
		String[] files = {"Doc","Doc2","Doc"};
		List<String> fileNames = new ArrayList<String>(Arrays.asList(files));
		Map<String, List<String>> texts = new HashMap<String, List<String>>();
		String[] values = {"one","two","three"};
		ArrayList<String> firstVals = new ArrayList<String>(Arrays.asList(values));
		texts.put("Value", firstVals);
		String currentAnnot = "Value";
		DocumentList dList = new DocumentList(fileNames, texts,texts,currentAnnot);
		Set<String> fileList = dList.getFilenames();
		for(String str: files){
			assertTrue(fileList.contains(str));
		}
	}
	
	@Test
	public void testGetLabelArrayNominal(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		String[] expected = {"Negative","Positive"};
		assertTrue(Arrays.equals(docList.getLabelArray("Vote", Feature.Type.NOMINAL), expected));
	}
	
	@Test
	public void testGetLabelArrayNumeric(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		String[] expected = {"Q1","Q2","Q3","Q4","Q5"};
		assertTrue(Arrays.equals(docList.getLabelArray("Age", Feature.Type.NUMERIC), expected));
	}
	@Test
	public void testGetLabelArrayNull(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		String[] expected = {};
		assertTrue(Arrays.equals(docList.getLabelArray("Fail", Feature.Type.BOOLEAN), expected));
	}
	@Test
	public void testSetLabelArray(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		String[] newLabelArray = {"Positive", "Negative"};
		docList.setCurrentAnnotation("Vote");
		docList.setLabelArray(newLabelArray);
		assertTrue(Arrays.equals(newLabelArray, docList.getLabelArray("Vote", Feature.Type.NOMINAL)));
	}
	@Test
	public void testGetSizeFromText(){
		List<String> instances = new ArrayList<String>();
		for(int i = 0; i < 5; i++){
			instances.add("i");
		}
		DocumentList dList = new DocumentList(instances);
		assertEquals(dList.getSize(), 5);
	}
	@Test
	public void testGetSizeEmptyText(){
		List<String> instances = new ArrayList<String>();
		DocumentList dList = new DocumentList(instances);
		assertEquals(dList.getSize(),0);
	}
	@Test
	public void testGetSizeByAnnotations(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		assertEquals(docList.getSize(),942);
	}
	@Test
	public void testGetSizeByAnnotationsNoAnnotation(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		Map<String, List<String>> newAnnotList = new TreeMap<String, List<String>>();
		newAnnotList.put("test", new ArrayList<String>());
		docList.allAnnotations = newAnnotList;
		assertEquals(docList.getSize(),0);
	}
	@Test
	public void testGetSizeEmptyDocumentList(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.allAnnotations = null;
		assertEquals(docList.getSize(),0);
	}
	@Test
	public void testSetCurrentAnnotation(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setCurrentAnnotation("Vote");
		assertNotNull(docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
	}
	@Test
	public void testSetCurrentAnnotationAlreadySet(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setCurrentAnnotation("Vote");
		String[] currentLabelArray = docList.labelArray;
		assertNotNull(docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
		docList.setCurrentAnnotation("Vote");
		assertEquals(currentLabelArray, docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
	}
	@Test
	public void testSetCurrAnnotationInvalid(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		try{
			docList.setCurrentAnnotation("Invalid");
			fail("Failed to catch exception");
		} catch(IllegalStateException e){
			assertEquals(e.getMessage(),"Can't find the label column named Invalid in provided file");
		}
	}
	@Test
	public void testSetCurrAnnotationDifferentAnn(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setCurrentAnnotation("Vote");
		String[] currentLabelArray = docList.labelArray;
		assertNotNull(docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
		docList.setCurrentAnnotation("State");
		assertNotSame(currentLabelArray, docList.labelArray);
		assertEquals("State", docList.currentAnnotation);
	}
	@Test
	public void testSetCurrAnnotationWithType(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setCurrentAnnotation("Vote", Feature.Type.NOMINAL);
		assertNotNull(docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
	}
	@Test
	public void testSetCurrentAnnotationWithTypeAlreadySet(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setCurrentAnnotation("Vote", Feature.Type.NOMINAL);
		String[] currentLabelArray = docList.labelArray;
		assertNotNull(docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
		docList.setCurrentAnnotation("Vote", Feature.Type.NOMINAL);
		assertEquals(currentLabelArray, docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
	}
	@Test
	public void testSetCurrAnnotationWithTypeInvalid(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		try{
			docList.setCurrentAnnotation("Invalid", Feature.Type.NOMINAL);
			fail("Failed to catch exception");
		} catch(IllegalStateException e){
			assertEquals(e.getMessage(),"Can't find the label column named Invalid in provided file");
		}
	}
	@Test
	public void testSetCurrAnnotationWithTypeDifferentAnn(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setCurrentAnnotation("Vote", Feature.Type.NOMINAL);
		String[] currentLabelArray = docList.labelArray;
		assertNotNull(docList.labelArray);
		assertEquals("Vote", docList.currentAnnotation);
		docList.setCurrentAnnotation("State", Feature.Type.NOMINAL);
		assertNotSame(currentLabelArray, docList.labelArray);
		assertEquals("State", docList.currentAnnotation);
	}
	@Test
	public void testSetTextColumns(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		Set<String> texts = new HashSet<String>();
		texts.add("text");
		docList.setTextColumns(texts);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
	}
	@Test
	public void testOverWriteTextColumns(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		Set<String> texts = new HashSet<String>();
		texts.add("text");
		docList.setTextColumns(texts);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
		texts.remove("text");
		texts.add("Vote");
		docList.setTextColumns(texts);
		assertNotNull(docList.textColumns.get("Vote"));
		assertNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
	}
	@Test
	public void testSetTextColumnIsText(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setTextColumn("text", true);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
	}
	@Test
	public void testSetTextColumnAlreadyThere(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setTextColumn("text", true);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
		docList.setTextColumn("text", true);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
	}
	@Test
	public void testSetTextColumnInvalidText(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		try{
			docList.setTextColumn("invalid", true);
			fail("Failed to catch exception");
		} catch(IllegalStateException e){
			
		}
	}
	@Test
	public void testRemoveTextColumn(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setTextColumn("text", true);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
		docList.setTextColumn("text", false);
		assertNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 0);
	}
	@Test
	public void testRemoveTextColumnNotThere(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setTextColumn("text", true);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
		docList.setTextColumn("notThere", false);
		assertNotNull(docList.textColumns.get("text"));
		assertEquals(docList.textColumns.size(), 1);
	}
	@Test
	public void testSetFileNames(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		fileNames.add("test1.csv");
		ArrayList<String> newFileList = new ArrayList<String>(fileNames);
		docList.setFilenames(newFileList);
		assertEquals(docList.getFilenameList().size(), 2);
		assertEquals(docList.getFilenameList(), newFileList);
		docList.setFilenames(null);
		assertNull(docList.getFilenameList());
	}
	@Test
	public void testEmptyAddInstances(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		int currentSize = docList.getSize();
		docList.addInstances(new ArrayList<Map<String,String>>(), new ArrayList<String>());
		assertEquals(currentSize, docList.getSize());
	}
	@Test
	public void testAddInstancesMultipleCases(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		ArrayList<Map<String,String>> rows = new ArrayList<Map<String,String>>();
		Map<String,String> firstAddition = new HashMap<String,String>();
		firstAddition.put("add1", "firstValue");
		firstAddition.put("add2", "SecondValue");
		Map<String,String> secondAddition = new HashMap<String,String>();
		secondAddition.put("add1", "firstValue");
		secondAddition.put("add2", "SecondValue");
		rows.add(firstAddition);
		rows.add(secondAddition);
		String[] newColumns = {"add1", "add2", "text"};
		List<String> columns = Arrays.asList(newColumns);
		int currentSize = docList.getSize();
		docList.addInstances(rows, columns);
		assertEquals(currentSize+rows.size(), docList.getSize());
	}
	@Test
	public void testAddInstancesTextColumns(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		ArrayList<Map<String,String>> rows = new ArrayList<Map<String,String>>();
		Map<String,String> firstAddition = new HashMap<String,String>();
		Map<String,String> secondAddition = new HashMap<String,String>();
		firstAddition.put("text", "sampletext1");
		firstAddition.put("text", "sampletext2");
		secondAddition.put("text", "sampletext1");
		secondAddition.put("text", "sampletext2");
		rows.add(firstAddition);
		rows.add(secondAddition);
		docList.setTextColumn("text", true);
		docList.setTextColumn("Vote", true);
		String[] newColumns = {"add1", "add2", "text"};
		List<String> columns = Arrays.asList(newColumns);
		int currentSize = docList.getSize();
		docList.addInstances(rows, columns);
		assertEquals(currentSize + rows.size(), docList.getSize());
	}
	@Test
	public void testGetAndSetEmptAnnotStr(){
		Set<String> fileNames = new TreeSet<String>();
		fileNames.add("testData/Gallup.csv");
		DocumentList docList = new DocumentList(fileNames);
		docList.setEmptyAnnotationString("test");
		assertEquals(docList.getEmptyAnnotationString(), "test");
	}
	/****************************************************/
	//Useless Tests Go Here
	@Test
	public void testGetAnnotationArrayNull(){
		DocumentList dList = new DocumentList("test");
		try{
			List<String> notes = dList.getAnnotationArray(null);
			assertNull(notes);
		}catch(NoSuchElementException e){
			;
		}
	}
	@Test
	public void testGetAnnotationArray(){
		List<String> instances = new ArrayList<String>();
		for(int i = 0; i < 5; i++){
			instances.add("i");
		}
		Map<String, List<String>> annMap = new TreeMap<String, List<String>>();
		annMap.put("value", instances);
		annMap.put("otherValue", instances);
		DocumentList dList = new DocumentList(instances, annMap);
		assertEquals(instances, dList.getAnnotationArray("value"));
	}
	
}