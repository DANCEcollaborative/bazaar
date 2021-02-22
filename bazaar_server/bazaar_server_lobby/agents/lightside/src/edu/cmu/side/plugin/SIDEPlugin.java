package edu.cmu.side.plugin;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import edu.cmu.side.plugin.control.PluginManager;

public abstract class SIDEPlugin implements Cloneable, Comparable<SIDEPlugin>
{
	private transient File rootFolder;

	protected boolean halt;
	private static boolean useSharedPluginsInDeserializedRecipes = true;
	
	protected final static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private Map<String, String> aboutMap = new HashMap<String, String>();

	public Map<String, String> getAboutMap()
	{
		return aboutMap;
	}

	public abstract String getOutputName();

	public static String classnameXMLKey = "classname";

	protected StringBuilder wrapSIDEPluginOption(CharSequence cs)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("<").append(this.getType()).append(" " + classnameXMLKey + "=\"").append(this.getClass().getName()).append("\">");
		builder.append(cs);
		builder.append("</").append(this.getType()).append(">");
		return builder;
	}

	public SIDEPlugin()
	{
	}

	public SIDEPlugin(File rootFolder)
	{
		this.rootFolder = rootFolder;
	}

	public void setRootFolder(File rootFolder)
	{
		this.rootFolder = rootFolder;
	}

	public File PluginFolder()
	{
		return this.rootFolder;
	}

	/**
	 * return a string which indicates the type of functionality provided by the
	 * pluginWrapper; pluginManager are grouped by type for example:
	 * "segmenter", "summarization", etc.
	 * 
	 * @return
	 */
	public abstract String getType();

	/**
	 * LightSIDE Genesis will remind the user, at many places, what options they
	 * selected. This method will be called to get that string.
	 * 
	 * @return
	 */
	public String getDescription()
	{
		return "No description available.";
	}

	/**
	 * This method is provided to give you a way to make sure that the
	 * pluginWrapper has been installed properly. The default implementation
	 * prints the name and version of the pluginWrapper and a 'success' message.
	 * If your implementation requires more sophisitcated error checking, you
	 * should overload this method. For example, if there are any external
	 * resources which you need access to, this is the time to check and make
	 * sure you can get to them. If you encounter any error or 'warning'
	 * conditions, append the relevant messages to the 'msg' buffer which was
	 * passed in.
	 */
	public boolean validatePlugin(StringBuffer msg)

	{
		boolean result = true;
		msg.append(this.aboutMap.get("title") + " " + this.aboutMap.get("version") + "\n");
		result = doValidation(msg);
		return result;
	}

	public void stopWhenPossible()
	{
		halt = true;
	}

	/*--------------------------------------------------------------------------------- */
	/*--------------------------------- OVERLOADABLE METHODS -------------------------- */
	/*--------------------------------------------------------------------------------- */

	public boolean doValidation(StringBuffer msg)
	{
		return true;
	}

	// Perform whatever pluginWrapper-specific validation is necessary
	// to ensure that the pluginWrapper can run

	public boolean isConfigurable()
	// return TRUE if the user can customize the
	// behavior of the pluginWrapper, FALSE otherwise
	{
		return false;
	}

	public Component getConfigurationUI()
	{
		return this.getConfigurationUIForSubclass();
	}

	protected abstract Component getConfigurationUIForSubclass();

	public abstract Map<String, String> generateConfigurationSettings();

	public abstract void configureFromSettings(Map<String, String> settings);

	public static SIDEPlugin fromSerializable(Serializable pug) throws InstantiationException, IllegalAccessException, ClassNotFoundException
	{
		SIDEPlugin plugin;
	
		logger.fine(new Date()+"\tSidePlugin: loading plugin "+pug+"...");
		
		if(pug == null) 
		{
			return null;
		}
		
		String classname = pug.toString();
		if(!useSharedPluginsInDeserializedRecipes)
		{
			plugin = (SIDEPlugin) Class.forName(classname).newInstance();
		}
		else
		{
			plugin = PluginManager.getPluginByClassname(classname);
		}
		return plugin;
	}

	public Serializable toSerializable() throws IOException
	{
		String classname = this.getClass().getName();
		return classname;
	}

	public boolean isStopped()
	{
		return halt;
	}

	@Override
	public int compareTo(SIDEPlugin p)
	{
		return this.getOutputName().compareTo(p.getOutputName());
	}

	public static boolean useSharedPluginsInDeserializedRecipes()
	{
		return useSharedPluginsInDeserializedRecipes;
	}

	public static void setUseSharedPluginsWhenDeserializing(boolean singletons)
	{
		SIDEPlugin.useSharedPluginsInDeserializedRecipes = singletons;
	}
	
	public boolean settingsMatch(Map<String, String> settingsA, Map<String, String> settingsB)
	{
		return settingsA.equals(settingsB);
	}
}
