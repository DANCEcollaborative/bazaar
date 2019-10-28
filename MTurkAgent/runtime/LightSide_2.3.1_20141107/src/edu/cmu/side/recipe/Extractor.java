package edu.cmu.side.recipe;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.plugin.control.ImportController;
import edu.cmu.side.view.util.RecipeExporter;

/**
 * loads a model trained using lightSIDE uses it to label new instances.
 * 
 * @author dadamson
 */
public class Extractor extends Chef
{
	static
	{
		System.setProperty("java.awt.headless", "true");
		//System.out.println(java.awt.GraphicsEnvironment.isHeadless() ? "Running in headless mode." : "Not actually headless");
	}

	public static void main(String[] args) throws Exception
	{
		List<String> exportOptions = Arrays.asList("arff", "csv", "xml");
		String recipePath, outPath;
		if (args.length < 5 || !exportOptions.contains(args[0]))
		{
			printUsage();
			System.exit(0);
		}

		try
		{
			recipePath = args[2];
			outPath = args[3];
			
			Charset encoding = Charset.forName(args[1]);
			Set<String> corpusFiles = new HashSet<String>();

			for (int i = 4; i < args.length; i++)
			{
				corpusFiles.add(args[i]);
			}

			System.out.println("Loading template recipe from " + recipePath);
			Recipe recipe = loadRecipe(recipePath);

			System.out.println("Loading documents: " + corpusFiles);
			DocumentList newDocs = ImportController.makeDocumentList(corpusFiles, encoding);
			
			if(!newDocs.allAnnotations().containsKey(recipe.getAnnotation()) || !newDocs.allAnnotations().keySet().containsAll(recipe.getDocumentList().getTextColumns()))
			{
				System.out.println("\n****");
				System.out.println("Your new data set "+corpusFiles);
				System.out.println("doesn't have the columns needed by the template file "+recipePath);
				System.out.println("It needs a class column ["+recipe.getAnnotation() +"], and text columns "+recipe.getDocumentList().getTextColumns()+".");
				System.out.println("Instead, its columns are "+newDocs.allAnnotations().keySet()+".\n");
				System.exit(1);
			}
			
			Recipe result = followRecipe(recipe, newDocs, Stage.MODIFIED_TABLE, recipe.getFeatureTable().getThreshold());
			
			File firstDoc = new File(args[4]);

			if(!outPath.toLowerCase().endsWith(args[0]))
			{
				outPath = outPath.replaceAll("\\.(arff|xml|csv|ARFF|XML|CSV)$", "");
				outPath += "."+args[0];
			}
			
			System.out.println("Saving finished feature table to " + outPath);

			File outFile = new File(outPath);
			FeatureTable trainingTable = result.getTrainingTable();
			String tableName = firstDoc.getName()+" "+recipe.getFeatureTable().getName();
			result.setRecipeName(tableName);
			trainingTable.setName(tableName);
			
			System.out.println("Saving in "+args[0]+" format.");
			if(args[0].equals("arff"))
			{
				RecipeExporter.exportToARFF(trainingTable, outFile);
			}
			else if(args[0].equals("csv"))
			{
				RecipeExporter.exportToCSV(trainingTable, outFile);
			}
			else //(args[3].equals("xml"))
			{
				RecipeExporter.exportToXML(result, outFile);
			}

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
		System.out.println();
		System.out.println("Usage: scripts/extract.sh {arff|csv|xml} {data-encoding} path/to/template.xml path/to/output/table path/to/data.csv...");
		System.out.println();
		System.out.println("Extracts a new feature table with the same extraction settings as template.xml (any saved LightSide feature table or model)");
		System.out.println("Feature tables can be saved in ARFF, CSV, or LightSide XML formats.");
		System.out.println("Common data encodings are UTF-8, windows-1252, and MacRoman.");
		System.out.println();
		System.out.println("(Make sure that the template's class column, text columns, and any columns used as features,");
		System.out.println(" all exist with the same names in the new data.)");
		System.out.println();
	}

}
