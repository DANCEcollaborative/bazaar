package edu.cmu.side.plugin;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import junit.framework.TestCase;

import org.junit.BeforeClass;
import org.junit.Test;

import plugins.features.BasicFeatures;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.plugin.control.ImportController;
import edu.cmu.side.plugin.control.PluginManager;
import edu.cmu.side.recipe.Chef;
import edu.cmu.side.recipe.converters.ConverterControl;

/* Things to test:
 * training models
 * 	with no features
 *  with boolean features
 *  with numeric features
 *  numeric class labels
 *  nominal class labels
 *  
 *  with all validation schemes
 *  	cross validation by annotation
 *  	cross validation by annotation - no annotation present
 *  	cross validation by file - multiple files
 *  	cross validation by file - one file
 *  	cross validation randomly
 *  	supplied test set - matches original data format
 *  	supplied test set - incompatible columns
 *  with each learning plugin? or subclasses of this test case, per plugin?
 *  
 * evaluating models - might be a separate test case, per eval plugin
 *  
 * predicting on unlabeled data from saved model - PredictorTest
 * */

public class FeaturePluginTest extends TestCase
{

	static StatusUpdater dummyUpdater = null;
	
	@BeforeClass @Override
	public void setUp()
	{
		 dummyUpdater = new StatusUpdater(){
			public void update(String updateSlot, int slot1, int slot2) {}
			public void update(String update) {}
			public void reset() {}};
	}

	@Test
	public void testBasicFeatures() throws Exception
	{
		DocumentList testDocs = ImportController.makeDocumentList(new HashSet<String>(Arrays.asList("testData/animal.csv")), Charset.forName("UTF-8")); 
		testDocs.setTextColumn("text", true);
		testDocs.setCurrentAnnotation("class");
		
		BasicFeatures plug = (BasicFeatures)PluginManager.getPluginByClassname("plugins.features.BasicFeatures");
		
		plug.setOption(BasicFeatures.TAG_POS_BIGRAM, true);
		plug.setOption(BasicFeatures.TAG_UNIGRAM, true);
		
		Collection<FeatureHit> hits= plug.extractFeatureHitsForSubclass(testDocs, new StatusUpdater(){
			public void update(String updateSlot, int slot1, int slot2) {}
			public void update(String update) {}
			public void reset() {}});
		System.out.println(hits.size() + " features extracted.");
		assertTrue(hits.size() == 1062);
	}

}
