import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import basilica2.agents.data.PromptTable;


public class PostHocPromptMatcher extends PromptTable
{
	protected Map<Pattern, String> promptPatterns;
	

	public PostHocPromptMatcher(String file)
	{
		super(file);
	}

	protected void loadPrompts(String filename)
	{
		promptPatterns = new HashMap<Pattern, String>();
		super.loadPrompts(filename);
		for(String key : prompts.keySet())
		{
			for(String prompt : prompts.get(key))
			{
				prompt = prompt.replaceAll("\\[[^\\]]+\\]", ".+");
				Pattern pat = Pattern.compile(prompt);
				promptPatterns.put(pat, key);
			}
		}
	}
	
	protected String match(String input)
	{
		for(Pattern p : promptPatterns.keySet())
		{
			if(p.matcher(input).find())
				return promptPatterns.get(p);
		}
		return null;
	}
}
