package plugins.fileParsing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

import plugins.input.CSVParser;
import edu.cmu.side.model.data.DocumentList;

public class CSVParserTest{
	CSVParser parser;
	HashSet<String> files;
	@Before
	public void setUp(){
		parser = new CSVParser();
		files = new HashSet<String>();
	}
	@Test
	public void testCanHandleTrue(){
		String correctFileType = "correct.csv";
		assertTrue(parser.canHandle(correctFileType));
	}
	@Test
	public void testCanHandleFalseIncorrectSuffix(){
		String incorrectFileType = "incorrect.rb";
		assertFalse(parser.canHandle(incorrectFileType));
	}
	@Test
	public void testCanHandleFalseNoSuffix(){
		String incorrectNoSuffix = "incorrect";
		assertFalse(parser.canHandle(incorrectNoSuffix));
	}
	@Test
	public void testParseDocumentEmptyList(){
		DocumentList parsed;
		try {
			parsed = parser.parseDocumentList(files, Charset.forName("UTF-8"));
			assertEquals(parsed.getSize(), 0);
		} catch (IOException e) {
			fail();
			e.printStackTrace();
		}
	}
	@Test
	public void testParseDocumentSingleFile(){
		files.add("../lightside/testData/Gallup.csv");
		try{
			DocumentList parsed = parser.parseDocumentList(files, Charset.forName("UTF-8"));
			assertEquals(parsed.getSize(), 942);
		} catch(IOException e){
			fail();
			e.printStackTrace();
		}
	}
	@Test
	public void testParseDocumentNullList(){
		try{
			DocumentList parsed = parser.parseDocumentList(null, Charset.forName("UTF-8"));
			assertEquals(parsed.getSize(),0);
		} catch(IOException e){
			fail();
			e.printStackTrace();
		}
	}
}