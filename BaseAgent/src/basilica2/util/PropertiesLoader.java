package basilica2.util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import edu.cmu.cs.lti.project911.utils.log.Logger;

public class PropertiesLoader
{
	public static Properties loadProperties(String path)
	{
		 Properties properties = new Properties();
	        if ((path != null) && (path.trim().length() != 0)) 
	        {
	        	File propertiesFile = new File(path);
				if(!propertiesFile.exists())
				{
					propertiesFile = new File("properties"+File.separator+path);
					if(!propertiesFile.exists())
					{
						Logger.commonLog("PropertiesLoader",Logger.LOG_ERROR,"no properties file at "+path);
						return properties;
					}
				}
	            try 
	            {
	                properties.load(new FileReader(propertiesFile));
	            } 
	            catch (IOException ex) 
	            {
	                ex.printStackTrace();
	            }
	        }

            return properties;
	}
}
