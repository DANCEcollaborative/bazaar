package edu.cmu.side.plugin.control;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import com.yerihyo.yeritools.debug.YeriDebug;
import com.yerihyo.yeritools.xml.XMLToolkit;
import com.yerihyo.yeritools.xml.XMLable;

import edu.cmu.side.plugin.SIDEPlugin;

public class PluginWrapper implements Comparable<PluginWrapper>, XMLable {
	private Map<String, String> configMap = new HashMap<String, String>();
	private File rootFolder;
	
	public static final String NAME = "name";
	public static final String AUTHOR = "author";
	public static final String VERSION = "version";
	public static final String JARFILE = "jarfile";
	public static final String CLASSNAME = "classname";
	public static final String CONFIG = "config";
	
//	public static class PluginException extends Exception{
//		private static final long serialVersionUID = 1L;
//	}
	
	private transient File jarFile;
	public File getJarFile(){
		if(jarFile!=null){ return jarFile; }
		
		String jarFileName = configMap.get(JARFILE);
		if(jarFileName==null || jarFileName.length()==0){
			return null;
		}
		jarFile = new File(rootFolder, jarFileName);
		return jarFile;
	}
	
	private transient SIDEPlugin sidePlugin = null;
	public SIDEPlugin getSIDEPlugin()
	// return a new copy of the PluginWrapper object
	{
		if(sidePlugin!=null){ return sidePlugin; }
		
		try {
			String jarFilePath = this.getJarFile().getAbsolutePath();
			String className = configMap.get(CLASSNAME);
			PluginLoader pl = new PluginLoader(jarFilePath, className);
			sidePlugin = pl.Plugin();
		} catch (Exception e) {
			System.err.println("The '" + this.configMap.get(NAME)
					+ "' pluginWrapper could not be loaded:");
			YeriDebug.ASSERT(e);
		}

		return sidePlugin;
	}

	public String getType(){
		SIDEPlugin sidePlugin = this.getSIDEPlugin();
		return sidePlugin==null?"N/A":sidePlugin.getType();
	}

	public String getConfiguration(){
		SIDEPlugin sidePlugin = this.getSIDEPlugin();
		String config = sidePlugin.getAboutMap().get(CONFIG);
		return config==null?"N/A":config;
	}

	/** TODO: change here for multiple jars **/
	public void WriteToDisk() {
		// Create the "config.xml" xmiFile and write the definition of the
		// pluginWrapper into it
		File xmlFile = new File(this.getJarFile().getParent(), "config.xml");
		try {
			// boolean okToGo = true;
			if (!xmlFile.exists()) {
				if (!xmlFile.createNewFile()) {
					throw new Exception(
							"The 'config.xml' xmiFile could not be created for the "
									+ this.configMap.get(NAME) + " PluginWrapper");
				}
			}
			// If we got to this point, the xmiFile exists
			FileWriter fw = new FileWriter(xmlFile);
			fw.write(XMLToolkit.XMLVersionString("1.0", "UTF-8"));
			fw.write(this.toXML());
			fw.close();

			// Once the xmiFile has be written, reload it
			// Do we need this?
			// LoadPluginXMLFile(this);
		} catch (Exception e) {

		}
	}

	public Map<String,String> getConfigMap(){ return this.configMap; }
	

	public PluginWrapper(File rootFolder, Element xmlElement) {
		this.rootFolder = rootFolder;
		this.configMap.putAll(XMLToolkit.xmlSimpleTypeToMap(xmlElement, "plugin"));
	}

	@Override
	public void fromXML(Element root) throws Exception {
//		NodeList children = xDoc.getChildNodes();
		for (Element element : XMLToolkit.getChildElements(root)) {
			this.configMap.put(element.getTagName(), element.getTextContent().trim());
		}
	}

	@Override
	public String toString() {
		return this.configMap.get(NAME) + " v " + this.configMap.get(VERSION);
	}

	@Override
	public String toXML(){
		return XMLToolkit.mapToXMLSimpleTypeString("plugin", configMap);
	}

	@Override
	public int compareTo(PluginWrapper arg0) {
		int result = 0;
		try {
			result = this.configMap.get(NAME).compareTo(arg0.configMap.get(configMap));
		} catch (Exception e) {
			System.out.println("Error in PluginWrapper.CompareTo: " + e.getMessage());
		}
		return result;
	}
}

