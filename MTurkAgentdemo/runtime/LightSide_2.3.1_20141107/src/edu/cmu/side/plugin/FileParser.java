package edu.cmu.side.plugin;

import java.awt.Component;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.cmu.side.model.data.DocumentList;

public abstract class FileParser extends SIDEPlugin {
	public static String type = "file_parser";
	public abstract DocumentList parseDocumentList(Set<String> filenames, Charset encoding) throws IOException;
	public abstract boolean canHandle(String filename);
	@Override
	public String getType(){
		return FileParser.type;
	}
	@Override
	public Map<String, String> generateConfigurationSettings() {
		return new HashMap<String,String>();
	}
	@Override
	public void configureFromSettings(Map<String, String> settings) {
		//TODO:Is there anything meaningful to do here?
	}
	@Override
	public String getOutputName() {
		//TODO: Does this even make sense?
		return "FileParser";
	}
	@Override
	protected Component getConfigurationUIForSubclass() {
		// TODO Auto-generated method stub
		return null;
	}
}
