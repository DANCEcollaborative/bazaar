package edu.cmu.side.util;

public enum TokenizingToolLanguage
{
	ENGLISH(EnglishTokenizingTool.class), 
	//GERMAN(GermanTokenizingTool.class), 
	//FRENCH(FrenchTokenizingTool.class), 
	//ARABIC(ArabicTokenizingTool.class), 
	CHINESE(ChineseTokenizingTool.class);
	//JAPANESE(JapaneseTokenizingTool.class);

	private AbstractTokenizingTool tool;
	private TokenizingToolLanguage(Class<? extends AbstractTokenizingTool> clazz)
	{
		try
		{
			tool = clazz.newInstance();
		}
		catch (InstantiationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public AbstractTokenizingTool getTool()
	{
		return tool;
	}
}
