package plugins.wrappers;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import plugins.wrappers.FeatureSelection;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.recipe.converters.ConverterControl;
import edu.cmu.side.view.util.DefaultMap;

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

public class FeatureSelectionPluginTest
{
	static FeatureSelection selector = new FeatureSelection();
	static Map<String, String> settings = new HashMap<String, String>();
	static int selectionSize = 10;
	
	static StatusUpdater updater = new StatusUpdater()
	{
		
		@Override
		public void update(String update)
		{
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void update(String updateSlot, int slot1, int slot2)
		{
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void reset()
		{
			// TODO Auto-generated method stub
			
		}
	};
	
	@BeforeClass
	public static void setUp()
	{
		settings.put("num", ""+selectionSize);
		settings.put("active", "true");
	}

	@Test
	public void testFeatureSelectionNominal() throws Exception
	{
		 Recipe wholeRecipe = ConverterControl.readFromXML("testData/test.model.side.xml");
		 
		 selector.configureFromSettings(settings);
		 selector.learnFromTrainingData(wholeRecipe.getTrainingTable(), 0, new DefaultMap<Integer, Integer>(1), updater);
		 assert(selector.getSelectedFeatures().size() == selectionSize);
		 assert(wholeRecipe.getTrainingTable().getFeatureSet().containsAll(selector.getSelectedFeatures()));
		 
	}
	
	@Test
	public void testFeatureSelectionNumeric() throws Exception
	{
		 Recipe wholeRecipe = ConverterControl.readFromXML("testData/test.numeric.model.side.xml");
		 
		 selector.configureFromSettings(settings);
		 selector.learnFromTrainingData(wholeRecipe.getTrainingTable(), 0, new DefaultMap<Integer, Integer>(1), updater);
		 assert(selector.getSelectedFeatures().size() == selectionSize);
		 assert(wholeRecipe.getTrainingTable().getFeatureSet().containsAll(selector.getSelectedFeatures()));
	}

}
