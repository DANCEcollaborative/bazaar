package edu.cmu.side.plugin.control;

/*
 * This class loads a pluginWrapper located within a Jar xmiFile.
 * 
 * Pass in the path to the Jar xmiFile and the className, and it will
 * try to load and instantiate a SIDEPlugin object.  If it succeeds,
 * the object will retain a pointer to the pluginWrapper.  If it fails, it
 * will throw an exception
 */

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/*import com.yerihyo.yeritools.RTTIToolkit;*/
import com.yerihyo.yeritools.debug.YeriDebug;

import edu.cmu.side.plugin.SIDEPlugin;


public class PluginLoader {
	private SIDEPlugin plugin = null;
	private File jarFile = null;
	private String className = "";

	public String ClassName() {
		return this.className;
	}

	public File JarFile() {
		return this.jarFile;
	}

	public SIDEPlugin Plugin() {
		return this.plugin;
	}

	public static Class<?> getClass(File jarFile, String name) throws Exception {
		URLClassLoader clazzLoader;
		Class<?> clazz;
		String filePath = jarFile.getAbsolutePath();
		filePath = "file:///" + filePath + "";
		URL url = new URL(filePath);
		clazzLoader = new URLClassLoader(new URL[] { url });
		clazz = clazzLoader.loadClass(name);
		return clazz;
	}
	
	/*public static Class<?> getClassNameList(File jarFile) throws Exception {
		URLClassLoader clazzLoader;
		Class<?> clazz;
		String filePath = jarFile.getAbsolutePath();
		filePath = "file:///" + filePath + "";
		URL url = new URL(filePath);
		clazzLoader = new URLClassLoader(new URL[] { url });
		clazzLoader.
		clazz = clazzLoader.loadClass(name);
		return clazz;
	}*/
	
	public PluginLoader(String jarFilePath, String className) throws Exception {
		this.jarFile = new File(jarFilePath);
		if (!jarFile.exists()) {
			throw new Exception("Could not read Jar xmiFile at "
					+ jarFile.getAbsolutePath());
		}

		this.className = className;

		try 
		{
			Class<?> clazz = getClass(this.jarFile, className);
			Constructor<?> constructor = clazz.getConstructor();
			this.plugin = (SIDEPlugin) constructor.newInstance();
			this.plugin.setRootFolder(new File(""));
		} catch (Exception e) {
			System.out.println("error with class '"+className+"'");
			YeriDebug.ASSERT(e);
		}

	}
	
	
	public static SIDEPlugin extractSIDEPlugin(String jarFilePath, String className) throws Exception {
		SIDEPlugin sidePlugin = null;
		File jarFile = new File(jarFilePath);
		if (!jarFile.exists()) {
			throw new Exception("Could not read Jar xmiFile at "
					+ jarFile.getAbsolutePath());
		}

		/*
		 * RTTIToolkit no longer works as of Java 9.  I don't understand why this code
		 * searches through class names like this rather than just letting getClass
		 * fail.  Commenting out for now.
		 * 
		 * Chris Bogart   Nov 2018
		 * 
		boolean foundIt = false;
		List<String> classNames = RTTIToolkit.getClassNameList(jarFile);
		for (String cn : classNames) {
			// Remove the ".class" at the back
			String name = cn.substring(0, cn.length() - 6);
			if (name.equalsIgnoreCase(className)) {
				foundIt = true;
				break;
			}
		}
		if (!foundIt) {
			throw new Exception("The JAR xmiFile " + jarFile.getAbsolutePath()
					+ " does not contain the class '" + className + "'.");
		}
		*/

		try {
			Class<?> clazz = getClass(jarFile, className);
			sidePlugin = (SIDEPlugin) clazz.newInstance();
			sidePlugin.setRootFolder(new File(""));
		} catch (Exception e) {
			YeriDebug.die(e);
		}
		
		return sidePlugin;
	}
}
