package edu.cmu.side.view.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.TrainingResult;

public class TrainedModelExporter
{

	static JFileChooser chooser;

	static FileNameExtensionFilter sideFilter = new FileNameExtensionFilter("LightSide Trained Model", "model.side", "side");
	static FileNameExtensionFilter predictFilter = new FileNameExtensionFilter("Predict-Only Model", "predict", "model.predict");

	public static void setUpChooser()
	{
		if (chooser == null)
		{
			chooser = new JFileChooser(new File("saved"))
			{
				String lastName = null;

				@Override
				public void setFileFilter(FileFilter filter)
				{
					File f = this.getSelectedFile();
					if (f == null && lastName != null)
					{
						f = new File(lastName);
					}
					if (f != null)
					{
						String name = f.getName();
						String extension = "";
						if (name.contains("."))
						{
							extension = name.substring(name.lastIndexOf(".") + 1);
							name = name.substring(0, name.lastIndexOf("."));
						}
						List<String> extensions = Arrays.asList(((FileNameExtensionFilter) filter).getExtensions());

						if (!extensions.contains(extension)) this.setSelectedFile(new File(name + "." + extensions.get(0)));
					}
					super.setFileFilter(filter);
				}

				@Override
				public void setSelectedFile(File f)
				{
					if (f != null) lastName = f.getName();
					super.setSelectedFile(f);
				}
			};
			chooser.addChoosableFileFilter(sideFilter);
			chooser.addChoosableFileFilter(predictFilter);
			
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.setFileFilter(sideFilter);
		}
	}

	public static void exportTrainedModel(Recipe tableRecipe)
	{
		setUpChooser();
		TrainingResult result = tableRecipe.getTrainingResult();
		try
		{
			chooser.setSelectedFile(new File(result.getName() + "." + ((FileNameExtensionFilter) chooser.getFileFilter()).getExtensions()[0]));

			int state = chooser.showDialog(null, "Save Trained Model");
			if (state == JFileChooser.APPROVE_OPTION)
			{
				File f = chooser.getSelectedFile();
				if (f.exists())
				{
					int confirm = JOptionPane.showConfirmDialog(null, f.getName() + " already exists. Do you want to overwrite it?");
					if (confirm != JOptionPane.YES_OPTION) return;
				}

				if (chooser.getFileFilter() == predictFilter)
					exportForPrediction(tableRecipe, f);
				else if (chooser.getFileFilter() == sideFilter) exportToSerialized(tableRecipe, f);
			}
		}
		catch (Exception e)
		{
			String message = e.getMessage();
			if (result == null)
				message = "Training Result is null.";
			else if (message == null || message.isEmpty()) message = "Couldn't save feature table.";
			JOptionPane.showMessageDialog(null, message);
			e.printStackTrace();
		}

	}

	public static void exportToSerialized(Recipe recipe, File target) throws IOException
	{
		recipe.setRecipeName(target.getName());
		FileOutputStream fout = new FileOutputStream(target);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(recipe);
	}

	public static void exportForPrediction(Recipe recipe, File target) throws IOException
	{
		Recipe dupe = Recipe.copyPredictionRecipe(recipe);
		FileOutputStream fout = new FileOutputStream(target);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(dupe);
	}
	
}
