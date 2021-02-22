package edu.cmu.side.recipe;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.yerihyo.yeritools.csv.CSVWriter;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.PredictionResult;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.plugin.ModelMetricPlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.plugin.control.ImportController;
import edu.cmu.side.plugin.control.PluginManager;

/**
 * loads a model trained using lightSIDE uses it to label new instances.
 * 
 * @author dadamson
 */
public class Tester extends Chef
{
	static
	{
		System.setProperty("java.awt.headless", "true");
		//System.out.println(java.awt.GraphicsEnvironment.isHeadless() ? "Running in headless mode." : "Not actually headless");
	}

	public static void main(String[] args) throws Exception
	{
		String recipePath, outPath;
		if (args.length < 4)
		{
			printUsage();
			System.exit(1);
		}

		try
		{

			recipePath = args[1];
			outPath = args[2];
			Charset encoding = Charset.forName(args[0]);

			Set<String> corpusFiles = new HashSet<String>();

			for (int i = 3; i < args.length; i++)
			{
				corpusFiles.add(args[i]);
			}

			if (!quiet) logger.info("Loading " + recipePath);
			Recipe recipe = loadRecipe(recipePath);

			if (!quiet) logger.info("Loading documents: " + corpusFiles);
			DocumentList newDocs = ImportController.makeDocumentList(corpusFiles, encoding);
			Recipe extracted = followRecipe(recipe, newDocs, Stage.MODIFIED_TABLE, 0);
			FeatureTable newFeatures = extracted.getTrainingTable();
			//System.out.println("Original "+recipe.getTrainingTable().getFeatureSet().size());
			//System.out.println("Extracted "+newFeatures.getFeatureSet().size());
			//newFeatures.reconcileFeatures(recipe.getFeatureTable().getFeatureSet());
			//System.out.println("Reconciled "+newFeatures.getFeatureSet().size());

			
			Predictor p = new Predictor(recipe, recipe.getAnnotation()+"_predicted");
			PredictionResult preds = p.predictFromTable(newFeatures);

			TrainingResult trainingResult = new TrainingResult(recipe.getTrainingTable(), extracted.getTrainingTable(), preds);
			
			printEvaluations(trainingResult);
			
			System.out.println("Writing predictions to output file "+outPath);
			Predictor.addPredictionsToDocumentList(p.predictionAnnotation, false, false, preds, newDocs);
			FileOutputStream out = new FileOutputStream(outPath);
			OutputStreamWriter writer = new OutputStreamWriter(out, encoding);
			CSVWriter csvWriter = new CSVWriter(writer);
			
			Map<String, List<String>> allAnnotations = newDocs.allAnnotations();
			Set<String> nonTextColumns = allAnnotations.keySet();
			Set<String> textColumns = newDocs.getTextColumns();
			
			List<String> allColumns = new ArrayList<String>(nonTextColumns);
			allColumns.addAll(textColumns);
			
			String[] dummy = new String[allColumns.size()];
			ArrayList<String> albert = new ArrayList<String>();
			csvWriter.writeNext(allColumns.toArray(dummy));
			Map<String, List<String>> coveredTextList = newDocs.getCoveredTextList();
			for(int i = 0; i < newDocs.getSize(); i++)
			{
				albert.clear();
				for(String key : nonTextColumns)
				{
					albert.add(allAnnotations.get(key).get(i));
				}
				for(String key : textColumns)
				{
					albert.add(coveredTextList.get(key).get(i));
				}
				csvWriter.writeNext(albert.toArray(dummy));
			}
			csvWriter.close();

			System.exit(0);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("\n****");
			printUsage();
			System.exit(1);
		}
	}

	

	public static void printUsage()
	{
		System.err.println("Usage: scripts/test.sh {data-encoding} saved/template.model.xml path/for/output.csv data/labeled-test-data.csv...");
		System.out.println("Applies a trained model to a new (labeled) data set, and evaluates performance.");
		System.out.println("Common data encodings are UTF-8, windows-1252, and MacRoman.");
		System.out.println("(Make sure that the text columns, class column, and any columns used as features have the same names in the new data as they did for the template.)");
	}

}
