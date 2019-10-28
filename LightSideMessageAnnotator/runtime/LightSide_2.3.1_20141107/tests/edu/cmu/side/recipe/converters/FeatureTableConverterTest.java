package edu.cmu.side.recipe.converters;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.junit.Before;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.FeatureTable;

public class FeatureTableConverterTest {
	Recipe predictRecipe;
	Recipe wholeRecipe;
	String delim = File.separator;
	File xmlPredictFile = new File("testData/test.predict.xml");
	File xmlWholeModelFile = new File("testData/test.model.side.xml");
	@Before
	public void setUp(){
		
		File serializedWholeModelFile = new File("testData/test.model.side.ser");
		File serializedPredictFile = new File("testData/test.predict.ser");
		
		
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
	

	@Test
	public void testConvertExistantPredict(){
		FeatureTable ftBefore = predictRecipe.getFeatureTable();
		XStream streamer = getXStream();
		String xml = streamer.toXML(ftBefore);
		Object afterObj = streamer.fromXML(xml);
		FeatureTable ftAfter = (FeatureTable) afterObj;
		assertTrue(ftBefore.equals(ftAfter));
	}
	
	@Test
	public void testConvertExistantModel(){
		FeatureTable ftBefore = wholeRecipe.getFeatureTable();
		XStream streamer = getXStream();
		String xml = streamer.toXML(ftBefore);
		Object afterObj = streamer.fromXML(xml);
		FeatureTable ftAfter = (FeatureTable) afterObj;
		assertTrue(ftBefore.equals(ftAfter));
	}
	@Test
	public void testConvertNull(){
		FeatureTable ftBefore = null;
		XStream streamer = getXStream();
		String XML = streamer.toXML(ftBefore);
		Object afterObj = streamer.fromXML(XML);
		assertNull(afterObj);
	}

	
	static XStream xStream = null;
	protected static XStream getXStream()
	{
		if(xStream == null)
		{
			xStream = new XStream();
			xStream.registerConverter(new FeatureTableConverter());
		}
		return xStream;
	}
}
