package edu.cmu.side.view.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.recipe.converters.ConverterControl;

public class RecipeExporter
{

	static JFileChooser tableChooser;
	static JFileChooser modelChooser;
	static JFileChooser predictChooser;
	
	//TODO: multi-extension names aren't detected correctly by FileNameExtensionFilter
	public final static FileFilter csvFilter = new EndsWithFileFilter("CSV", "features.csv", "csv", "CSV");
//	public final static FileFilter csvFilterMac = new EndsWithFileFilter("CSV (Mac Excel, MacRoman)", "csv", "CSV");
//	public final static FileFilter csvFilterWindows = new EndsWithFileFilter("CSV (Windows Excel, CP1252)", "csv", "CSV");
	public final static FileFilter arffFilter = new EndsWithFileFilter("ARFF (Weka)", "features.arff", "arff", "ARFF");
	public final static FileFilter xmlTableFilter = new EndsWithFileFilter("LightSide Feature Table XML", "features.xml");
	public final static FileFilter xmlModelFilter = new EndsWithFileFilter("LightSide Trained Model XML", "model.xml");
	public final static FileFilter xmlPredictFilter = new EndsWithFileFilter("Predict-Only XML", "predict.xml");
	public final static FileFilter xmlGenericFilter = new EndsWithFileFilter("LightSide XML", "xml");
	public final static FileFilter serializedTableFilter = new EndsWithFileFilter("LightSide Feature Table", "features.side");
	public final static FileFilter serializedModelFilter = new EndsWithFileFilter("LightSide Trained Model", "model.side");
	public final static FileFilter serializedGenericFilter = new EndsWithFileFilter("LightSide", "side");
	public final static FileFilter serializedPredictFilter = new EndsWithFileFilter("Predict-Only Serialized", "predict.side");

	protected final static Pattern extensionPattern = Pattern.compile("(\\.(?:features|model|predict)?\\.[a-zA-Z]+)$");

	protected static boolean useXML = true;
	protected static boolean useSerialized = false;
	
	public static JFileChooser setUpChooser(JFileChooser chooser, FileFilter... filters)
	{
		//System.out.println("REx: setting up chooser for filters "+Arrays.toString(filters));
		if (chooser == null)
		{
			System.out.println("REx: making new chooser ");
			chooser = new JFileChooser(new File("saved"))
			{
				String lastName = null;

				@Override
				public void setFileFilter(FileFilter filter)
				{
					//System.out.println("REx: setting file filter within special subclass: Filter="+filter);
				
					if(filter == null)
						return;

					//System.out.println("REx: calling super.setFileFilter");
					super.setFileFilter(filter);
					
					File f = this.getSelectedFile();
					if (f == null && lastName != null)
					{
						f = new File(lastName);
					}
					if (f != null && filter instanceof EndsWithFileFilter)
					{
						String name = f.getName();

						List<String> extensions = Arrays.asList(((EndsWithFileFilter) filter).getExtensions());

						boolean validExtension = false;
						for (String ext : extensions)
						{
							//System.out.println("REx: looping over file extensions: "+ext);
							if (name.endsWith("."+ext))
							{
								//System.out.println("REx: valid extension ."+ext);
								validExtension = true;
								break;
							}
						}

						if (!validExtension)
						{
							Matcher match = extensionPattern.matcher(name);
							if(match.find())
							{
								name = match.replaceFirst("." + extensions.get(0));
							}
							else
								name = name + "." + extensions.get(0);

							//System.out.println("REx: I would like to rename this file to "+name);
							this.setSelectedFile(new File(name));
							
						}
							
					}

				}

				@Override
				public void setSelectedFile(File f)
				{
					//System.out.println("REx: calling setSelectedFile: "+f);
					if (f != null) lastName = f.getName();
					super.setSelectedFile(f);
				}
			};

			
			for (FileFilter filter : filters)
			{
				if (filter == arffFilter)
				{
					try
					{
						Class.forName("plugins.learning.WekaTools");
						chooser.addChoosableFileFilter(filter);
					}
					catch (ClassNotFoundException cnf)
					{
						System.err.println("WekaTools not found - disabling ARFF exporter");
					}
				}

				else
					chooser.addChoosableFileFilter(filter);
			}
			
			chooser.setAcceptAllFileFilterUsed(true);
			chooser.setFileFilter(filters[0]);
		}
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setFileHidingEnabled(false);

		System.out.println("REx: returning chooser for filter "+Arrays.toString(filters)+"\n"+Arrays.toString(chooser.getChoosableFileFilters()));
		
		return chooser;
	}

	public static void exportFeatures(Recipe tableRecipe)
	{
		if(useXML && useSerialized)
			tableChooser = setUpChooser(tableChooser, xmlTableFilter, serializedTableFilter, csvFilter, arffFilter);
		else if(!useXML)
			tableChooser = setUpChooser(tableChooser, serializedTableFilter, csvFilter, arffFilter);
		else
			tableChooser = setUpChooser(tableChooser, xmlTableFilter, csvFilter, arffFilter);
		
		FeatureTable table = tableRecipe.getTrainingTable();
		try
		{
			tableChooser.setSelectedFile(new File(table.getName() + "." + ((EndsWithFileFilter) tableChooser.getFileFilter()).getExtensions()[0]));

			System.out.println("REx: selected file is "+tableChooser.getSelectedFile());
			

			Thread.sleep(100);
			System.out.println("REx: showing dialog");
			int state = tableChooser.showDialog(null, "Save Feature Table");
			System.out.println("REx: final selected file is "+tableChooser.getSelectedFile());
			if (state == JFileChooser.APPROVE_OPTION)
			{
				File f = tableChooser.getSelectedFile();
				if (f.exists())
				{
					int confirm = JOptionPane.showConfirmDialog(null, f.getName() + " already exists. Do you want to overwrite it?");
					if (confirm != JOptionPane.YES_OPTION) return;
				}

				if (tableChooser.getFileFilter() == csvFilter) exportToCSV(table, f);
				else if (tableChooser.getFileFilter() == arffFilter) 
				{
					exportToARFF(table, f);
				}
				else if (tableChooser.getFileFilter() == xmlTableFilter) exportToXML(tableRecipe, f);
				else if (tableChooser.getFileFilter() == serializedTableFilter) exportToSerialized(tableRecipe, f);
			}
		}
		catch (Exception e)
		{
			String message = e.getMessage();
			if (table == null)
				message = "Feature Table is null.";
			else if (message == null || message.isEmpty()) message = "Couldn't save feature table.";
			JOptionPane.showMessageDialog(null, message);
			e.printStackTrace();
		}

	}

	public static void exportToARFF(FeatureTable table, File f) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
			InvocationTargetException
	{
		try
		{
			Class wekaToolsClass = ClassLoader.getSystemClassLoader().loadClass("plugins.learning.WekaTools");
			Method exportMethod = wekaToolsClass.getMethod("exportToARFF", FeatureTable.class, File.class);
			exportMethod.invoke(null, table, f);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Couldn't export to ARFF - is the genesis.jar plugin package in place?\n"+e.getMessage(), "Export Error", JOptionPane.WARNING_MESSAGE);
		}
	}

	public static void exportToXML(Recipe recipe, File target) throws IOException
	{

		ConverterControl.writeToXML(target, recipe);
	}
	
	@Deprecated
	public static void exportToSerialized(Recipe recipe, File target) throws IOException
	{
		//TODO: write out here
		FileOutputStream fout = new FileOutputStream(target);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(recipe);
	}
	
	public static void exportToCSV(FeatureTable ft, File file) throws IOException
	{

		if (!file.getName().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");
		FileWriter outf = new FileWriter(file);
		outf.write("Instance");
		DocumentList localDocuments = ft.getDocumentList();
		outf.write("," + ft.getAnnotation());

		for (Feature f : ft.getFeatureSet())
			outf.write("," + f.getFeatureName().replaceAll(",", "_"));
		outf.write("\n");

		List<String> annotations = ft.getAnnotations();

		for (int i = 0; i < localDocuments.getSize(); i++)
		{
			outf.write(("" + (i + 1)));

			outf.write("," + annotations.get(i));

			Collection<FeatureHit> hits = ft.getHitsForDocument(i);
			for (Feature f : ft.getFeatureSet())
			{
				boolean didHit = false;
				for (FeatureHit h : hits)
				{
					if (h.getFeature().equals(f))
					{
						outf.write("," + h.getValue());
						didHit = true;
						break;
					}

				}

				if (!didHit)
				{
					if (f.getFeatureType() == Type.NUMERIC)
					{
						outf.write(",");
					}
					else if (f.getFeatureType() == Type.BOOLEAN)
					{
						outf.write("," + Boolean.FALSE);
					}
					else
						outf.write(",");
				}

			}
			outf.write("\n");
		}
		outf.close();

	}

	public static void exportTrainedModel(Recipe modelRecipe)
	{
		JFileChooser chooser;
		if(modelRecipe.getStage() == Stage.TRAINED_MODEL)
		{
			if(useXML && useSerialized)
				chooser = modelChooser = setUpChooser(modelChooser, xmlModelFilter, xmlPredictFilter, serializedModelFilter, serializedPredictFilter);
				
			else if(useXML)
				chooser = modelChooser = setUpChooser(modelChooser, xmlModelFilter, xmlPredictFilter);
			else
				chooser = modelChooser = setUpChooser(modelChooser, serializedModelFilter, serializedPredictFilter);
				
		}
		else
		{
			if(useXML && useSerialized)
				chooser = predictChooser = setUpChooser(predictChooser, xmlPredictFilter, serializedPredictFilter);
			else if(useXML)
				chooser = predictChooser = setUpChooser(predictChooser, xmlPredictFilter);
			else
				chooser = modelChooser = setUpChooser(predictChooser, serializedPredictFilter);
		}
		System.out.println("REx: chooser has "+Arrays.toString(chooser.getChoosableFileFilters()));
		
		System.out.println("REx: getting trained model...");
			
		TrainingResult result = modelRecipe.getTrainingResult();
		try
		{
			String name = result.getName() != null ? result.getName() : modelRecipe.getRecipeName();
			String pathname = name + "." + ((EndsWithFileFilter) modelChooser.getFileFilter()).getExtensions()[0];
			System.out.println("REx: setting default name '"+pathname+"' from selected filter...");
			chooser.setSelectedFile(new File(pathname));

			System.out.println("REx: selected file is '"+chooser.getSelectedFile()+"'");
			
			System.out.println("REx: showing dialogue...");
			
			Thread.sleep(100);
			
			int state = chooser.showDialog(null, "Save Trained Model");
			if (state == JFileChooser.APPROVE_OPTION)
			{
				File f = chooser.getSelectedFile();
				if (f.exists())
				{
					int confirm = JOptionPane.showConfirmDialog(null, f.getName() + " already exists. Do you want to overwrite it?");
					if (confirm != JOptionPane.YES_OPTION) return;
				}

				if (chooser.getFileFilter() == xmlPredictFilter) exportToXMLForPrediction(modelRecipe, f);
				else if (chooser.getFileFilter() == xmlModelFilter) exportToXML(modelRecipe, f);
				else if (chooser.getFileFilter() == serializedModelFilter) exportToSerialized(modelRecipe, f);
				else if (chooser.getFileFilter() == serializedPredictFilter) exportToSerializedForPrediction(modelRecipe, f);
			}
		}
		catch (Exception e)
		{
			String message = e.getMessage();
			if (result == null)
				message = "Training Result is null.";
			else if (message == null || message.isEmpty()) message = "Couldn't save trained model.";
			JOptionPane.showMessageDialog(null, message);
			e.printStackTrace();
		}

	}

	@Deprecated
	public static void exportToSerializedForPrediction(Recipe recipe, File target) throws IOException
	{
		Recipe dupe = Recipe.copyPredictionRecipe(recipe);
		FileOutputStream fout = new FileOutputStream(target);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(dupe);
	}
	
	public static void exportToXMLForPrediction(Recipe recipe, File target) throws IOException
	{
		//TODO: Setup Rewrite Here
		Recipe dupe = Recipe.copyPredictionRecipe(recipe);
		ConverterControl.writeToXML(target, dupe);
	}

	public static FileFilter getTrainedModelFilter()
	{
		if(useXML())
		{
			return xmlModelFilter;
		}
		else
		{
			return serializedModelFilter;
		}
	}
	
	public static FileFilter getPredictModelFilter()
	{

		if(useXML())
		{
			return xmlPredictFilter;
		}
		else
		{
			return serializedPredictFilter;
		}
	}
	
	public static FileFilter getFeatureTableFilter()
	{

		if(useXML())
		{
			return xmlTableFilter;
		}
		else
		{
			return serializedTableFilter;
		}
	}
	
	public static FileFilter getGenericFilter()
	{
		if(useXML())
		{
			return xmlGenericFilter;
		}
		else
		{
			return serializedGenericFilter;
		}
	}
	
	public static FileFilter getARFFFilter()
	{
		return arffFilter;
	}
	
	public static FileFilter getCSVFilter()
	{
		return csvFilter;
	}
	
	public static boolean useXML()
	{
		return useXML;
	}

	public static void setUseXML(boolean useXML)
	{
		RecipeExporter.useXML = useXML;
	}
}
