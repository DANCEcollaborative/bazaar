package edu.cmu.side.recipe.converters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.thoughtworks.xstream.XStream;

import edu.cmu.side.model.Recipe;

//TODO: explicitly enable reading/writing of UTF-8 recipes. 
public class ConverterControl
{
	private static XStream xStream;
	
	public enum RecipeFileFormat {XML, ZIPPED_XML, SERIALIZED};
	
	private ConverterControl()
	{}
 
	public static void writeRecipeToFile(String recipeOutFile, Recipe recipe, RecipeFileFormat exportFormat) throws IOException
	{

		if(exportFormat == RecipeFileFormat.XML)
		{
			ConverterControl.writeToXML(recipeOutFile, recipe);
		}
		else if(exportFormat == RecipeFileFormat.ZIPPED_XML)
		{
			ConverterControl.writeToZippedXML(new File(recipeOutFile), recipe);
		}
		else
		{
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(recipeOutFile));
			oos.writeObject(recipe);
			oos.close();
		}
	}
	
	public static Recipe loadRecipe(String recipePath) throws IOException, FileNotFoundException
	{

		File recipeFile = new File(recipePath);
		if (!recipeFile.exists())
		{
			throw new FileNotFoundException("No model file at " + recipeFile.getPath());
		}
		
		IOException ex = null;
		for(RecipeFileFormat format : RecipeFileFormat.values())
		{
			try
			{
				Recipe loadedRecipe = ConverterControl.loadRecipe(recipePath, format);
				//System.out.println("ConverterControl: "+recipePath+" is "+format);
				return loadedRecipe;
			}
			catch(IOException e)
			{
				ex = new IOException("Couldn't load recipe in any format: "+recipePath, e);
				e.printStackTrace();
			}	
		}
		throw ex;
	}
	
	public static Recipe loadRecipe(String recipePath, RecipeFileFormat format) throws IOException, FileNotFoundException
	{
		File recipeFile = new File(recipePath);
		if (!recipeFile.exists())
		{
			throw new FileNotFoundException("No model file at " + recipeFile.getPath());
		}
		else
		{
			try
			{

				Recipe recipe;
				if (format == RecipeFileFormat.XML)
				{
					recipe = ConverterControl.readFromXML(recipeFile);
				}
				else if (format == RecipeFileFormat.ZIPPED_XML)
				{
					recipe = ConverterControl.readFromZippedXML(recipeFile);
				}
				else
				{
					ObjectInputStream ois = new ObjectInputStream(new FileInputStream(recipeFile));
					recipe = (Recipe) ois.readObject();
					ois.close();
				}

				return recipe;

			}
			catch (Exception e)
			{
				throw new IOException("Failed to read recipe as "+format+" : " + recipePath, e);
			}
		}
	}

	public static Recipe readFromXML(String fileName) throws IOException
	{
		File file = createFile(fileName);

		return readFromXML(file);
	}

	public static Recipe readFromXML(File file) throws IOException
	{
		XStream stream = getXStream();
		Recipe r =(Recipe) stream.fromXML(new InputStreamReader(new FileInputStream(file), Charset.forName("UTF-8")));
        
		return r;
	}
	
	public static void writeToXML(String fileName, Recipe recipe) throws IOException
	{
		File file = createFile(fileName);
		writeToXML(file, recipe);
	}

	public static void writeToXML(File file, Recipe recipe) throws IOException
	{
		XStream stream = getXStream();
		FileOutputStream fileOut = new FileOutputStream(file);
		OutputStreamWriter writer = new OutputStreamWriter(fileOut, Charset.forName("UTF-8"));
		stream.toXML(recipe, writer);
		writer.close();
		System.out.println("Wrote XML recipe for "+recipe.getRecipeName()+" to "+file.getPath());
	}

	public static String getXMLString(Recipe recipe)
	{
		XStream stream = getXStream();

		return stream.toXML(recipe);
	}

	public static Recipe getRecipeFromXMLString(String recipeXML)
	{
		XStream stream = getXStream();

		return (Recipe) stream.fromXML(recipeXML);

	}

	public static Recipe getRecipeFromZippedXMLString(String zippedRecipeXML) throws IOException
	{
		InputStream stringIn = new ByteArrayInputStream(zippedRecipeXML.getBytes());
		
		Recipe recipe = streamInZippedXML(stringIn);
		
		stringIn.close();
		return recipe;
	}

	public static Recipe readFromZippedXML(File file) throws IOException
	{
		InputStream fileIn = new FileInputStream(file);
		
		Recipe recipe = streamInZippedXML(fileIn);
		
		fileIn.close();
		return recipe;
	}	
	
	public static void writeToZippedXML(File file, Recipe recipe) throws IOException
	{
		FileOutputStream fileOut = new FileOutputStream(file);
		
		streamOutZippedXML(recipe, fileOut);
		
		fileOut.close();
		System.out.println("Wrote zipped XML recipe for "+recipe.getRecipeName()+" to "+file.getPath());
	}

	public static String getZippedXMLString(Recipe recipe) throws IOException
	{
		ByteArrayOutputStream stringOut = new ByteArrayOutputStream();
		
		streamOutZippedXML(recipe, stringOut);
		String zippedString = stringOut.toString();
		
		stringOut.close();
		return zippedString;
	}


	private static File createFile(String name) throws IOException
	{
		File file = new File(name);
		if (!file.exists())
		{
			file.createNewFile();
		}
		return file;
	}
	
	protected static void streamOutZippedXML(Recipe r, OutputStream out) throws IOException
	{
		ZipOutputStream zipper = new ZipOutputStream(out);
		OutputStreamWriter zipperWriter = new OutputStreamWriter(zipper, Charset.forName("UTF-8"));
		zipper.putNextEntry(new ZipEntry(r.getRecipeName()));
		XStream stream = getXStream();
		stream.toXML(r, zipperWriter);
		zipper.closeEntry();
		zipper.close();
	}
	
	protected static Recipe streamInZippedXML(InputStream in) throws IOException
	{
		ZipInputStream unzipper = new ZipInputStream(in);
		ZipEntry entry = unzipper.getNextEntry();
		System.out.println("Getting Zipped "+entry.getName());
		

		InputStreamReader unzipperReader = new InputStreamReader(unzipper, Charset.forName("UTF-8"));
		
		XStream stream = getXStream();
		Recipe r = (Recipe) stream.fromXML(unzipperReader);
		unzipper.close();
		
		return r;
	}


	protected static XStream getXStream()
	{
		if(xStream == null)
		{
			xStream = new XStream();
			xStream.registerConverter(new FeatureTableConverter());
		}
		return xStream;
	}


	public static void main(String[] args) throws IOException, ClassNotFoundException
	{
		long start, end;
//		start = System.currentTimeMillis();
//		Recipe rs = (Recipe) new ObjectInputStream(new FileInputStream(new File("saved/essay_source.ser"))).readObject();
//		
//		end = System.currentTimeMillis();
//		System.out.println("loaded super large serialized recipe in "+(end - start)+"ms.");
		
		start = System.currentTimeMillis();
		Recipe rx = ConverterControl.readFromXML(new File("saved/movies.side.xml"));
		
		end = System.currentTimeMillis();
		System.out.println("loaded medium XML recipe in "+(end - start)+"ms.");
//		
//		start = System.currentTimeMillis();
//		Recipe rx = ConverterControl.readFromXML(new File("saved/essay_source.side.xml"));
//		
//		end = System.currentTimeMillis();
//		System.out.println("loaded super large plain XML in "+(end - start)+"ms.");
//		
//		start = System.currentTimeMillis();
//		Recipe rz = ConverterControl.readFromZippedXML(new File("saved/essay_source.side.xml.zip"));
//		
//		end = System.currentTimeMillis();
//		System.out.println("loaded super large zipped XML in "+(end - start)+"ms.");
//		System.out.println("recipes match? "+(rx.equals(rz)));
	}
}
