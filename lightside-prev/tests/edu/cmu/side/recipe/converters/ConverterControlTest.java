package edu.cmu.side.recipe.converters;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.side.model.Recipe;

public class ConverterControlTest
{	
	//TODO: parameterize this so there's no near-duplicate tests
	private static final File wholeTestXMLOutputFile = new File("testData/test_whole_foobarblah.xml");
	private static final File predictionTestXMLOutputFile = new File("testData/test_prediction_foobarblah.xml");
	private static final File predictionTestZipOutputFile = new File("testData/test_prediction_foobarblah.xml.zip");
	private static final File wholeTestZipOutputFile = new File("testData/test_whole_foobarblah.xml.zip");
	private static final File[] tempFiles = {wholeTestXMLOutputFile, predictionTestXMLOutputFile, predictionTestZipOutputFile, wholeTestZipOutputFile};
	Recipe predictRecipe;
	Recipe wholeRecipe;
	String delim = File.separator;
	File xmlPredictFile = new File("testData/test.predict.xml");
	File xmlWholeModelFile = new File("testData/test.model.side.xml");
	
	@Before
	public void setUp()
	{
		
		File serializedWholeModelFile = new File("testData"+delim+"test.model.side.ser");
		File serializedPredictFile = new File("testData"+delim+"test.predict.ser");
		
		
		try {
			
			
			FileInputStream in = new FileInputStream(serializedWholeModelFile);
			ObjectInputStream stream = new ObjectInputStream(in);
			
			wholeRecipe = (Recipe)stream.readObject();
			stream.close();
			
			in = new FileInputStream(serializedPredictFile);
			stream = new ObjectInputStream(in);
			predictRecipe = (Recipe)stream.readObject();
			stream.close();
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//This is an abnormally long test. We can speed it up by switching it to writing to a file.
	
	
	@After
	public void tearDown()
	{
		//comment out to leave the temp files around for human inspection.
		for(File temp : tempFiles)
		{
			if(temp.exists())
			{
				temp.delete();
			}
		}
	}
	
	@Test
	public void testPredictionRecipeXML() throws IOException
	{
		ConverterControl.writeToXML(predictionTestXMLOutputFile, predictRecipe);
		Recipe r = ConverterControl.readFromXML(predictionTestXMLOutputFile);
		assertTrue(r.equals(predictRecipe));
	}
	@Test
	public void testWholeRecipeXML() throws IOException
	{
		ConverterControl.writeToXML(wholeTestXMLOutputFile, wholeRecipe);
		Recipe r = ConverterControl.readFromXML(wholeTestXMLOutputFile);
		assertTrue(r.equals(wholeRecipe));
	}
	
	@Test
	public void testPredictionRecipeZippedXMLFile() throws IOException
	{
		ConverterControl.writeToZippedXML(predictionTestZipOutputFile, predictRecipe);
		Recipe r = ConverterControl.readFromZippedXML(predictionTestZipOutputFile);
		assertTrue(r.equals(predictRecipe));
	}
	@Test
	public void testWholeRecipeZippedXMLFile() throws IOException
	{
		ConverterControl.writeToZippedXML(wholeTestZipOutputFile, wholeRecipe);
		Recipe r = ConverterControl.readFromZippedXML(wholeTestZipOutputFile);
		assertTrue(r.equals(wholeRecipe));
	}
	
	@Test @Ignore
	public void testPredictionRecipeZippedXMLString() throws IOException
	{
		String zippedXML = ConverterControl.getZippedXMLString(predictRecipe);
		Recipe r = ConverterControl.getRecipeFromZippedXMLString(zippedXML);
		assertTrue(r.equals(predictRecipe));
	}
	@Test @Ignore
	public void testWholeRecipeZippedXMLString() throws IOException
	{
		String zippedXML = ConverterControl.getZippedXMLString(wholeRecipe);
		Recipe r = ConverterControl.getRecipeFromZippedXMLString(zippedXML);
		assertTrue(r.equals(wholeRecipe));
	}
	
}