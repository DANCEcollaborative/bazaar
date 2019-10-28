package edu.cmu.side.view.util;

import java.io.File;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

public class EndsWithFileFilter extends FileFilter
{
	String[] extensions;
	String description;
	
	
	public EndsWithFileFilter(String description, String... ext)
	{
		super();
		this.extensions = ext;
		this.description = description;
	}
	
	public String[] getExtensions()
	{
		return extensions;
	}

	@Override
	public boolean accept(File file)
	{
		if(file.isDirectory())
			return true;
		
		String fileName = file.getName();
		for(String ext : extensions)
		{
			if(fileName.toLowerCase().endsWith(ext.toLowerCase()))
				return true;
		}
		return false;
	}

	@Override
	public String getDescription()
	{
		return description;
	}
	
	public String toString()
	{
		return getDescription()+": "+Arrays.toString(extensions);
	}
	
}