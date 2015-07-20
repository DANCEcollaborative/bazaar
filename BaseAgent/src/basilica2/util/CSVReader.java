package basilica2.util;
import java.util.*;
import java.io.*;

public class CSVReader
{
	private static final String COMMENT = "#";

	public static List<Map<String, String>> readCSV(File csvf)
	{
		ArrayList<Map<String, String>> rows = new ArrayList<Map<String,String>>();
		try
		{
			BufferedReader fin = new BufferedReader( new FileReader(csvf));
			
			String line = "";
			String[] fields = null;
			while((line = fin.readLine()) != null)
			{
				//System.out.println(line);
				
				if(!line.startsWith(COMMENT))
				{
					if(fields == null)
					{
						fields = line.split(",");
					}
					else
					{
						Map<String, String> row = new HashMap<String,String>();
						for(int i = 0; i < fields.length; i++)
						{
							int endIndex=0;

							//System.out.println(line);
							
							
							if(line.startsWith("\""))
							{
								boolean quoting = true;
								while(quoting)
								{
									endIndex = line.indexOf("\"", endIndex+1);
									if(endIndex < 1 || endIndex > line.length()-1)  //presume a line starting with a quote that doesn't end with one has a builtin line break
									{
										String line2 = fin.readLine();
										if(line2 != null)
										{
											line += " \n " + line2;
											continue;
										}
										else
											endIndex = line.length()-1;
									}
									

									
									if(line.substring(endIndex-1, endIndex+1).equals("\\\"") )
									{
										quoting = true;
									}
									else if( (endIndex + 2 <= line.length() 
											&& line.substring(endIndex, endIndex+2).equals("\"\"")))
									{
										endIndex ++;
										quoting = true;
									}
									else
										quoting = false;
								}

								//System.out.println("'"+line.substring(endIndex-1, endIndex+1)+"'");
								
								endIndex = Math.min(line.length(), endIndex+1); //+1 to account for the quote
							}
							
							
							/*if(quoteIndex == 0)
							{
								endIndex = line.substring(1).indexOf("\",");
								
								if(endIndex != -1)
									endIndex += 2;
							}*/
							
							else
								endIndex = line.indexOf(",");
							
							if(endIndex == -1)
								endIndex = line.length();
							
							if(line.isEmpty())
							{
								row.put(fields[i], "");
							}
							else
							{
								String column = line.substring(0, endIndex);
								//System.out.println(fields[i]+":\t"+column);
								if(column.startsWith("\""))
								{
									column = column.substring(1);
									if(column.endsWith("\""))
										column = column.substring(0, column.length()-1);
									column = column.replaceAll("\\\\\"", "\"");
									column = column.replaceAll("\"\"", "\"");
								}
								
								line = endIndex>=line.length()?"":line.substring(endIndex+1);
								
								row.put(fields[i], column);
							}
						}
						rows.add(row);
					}
					
				}
					
			}
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return rows;
	}

	
	public static void main(String[] args)
	{
		File test = new File("/Users/dadamson/foo.csv");
		System.out.println(CSVReader.readCSV(test));
	}


	public static List<String> readComments(File myLog)
	{
		ArrayList<String> comments = new ArrayList<String>();
		try
		{
			BufferedReader fin = new BufferedReader( new FileReader(myLog));
			
			String line = "";
			while((line = fin.readLine()) != null)
			{
				if(line.startsWith(COMMENT))
				{
					comments.add(line.substring(COMMENT.length()));
				}
			}
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return comments;
	}
}
