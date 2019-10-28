package edu.cmu.side.view.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;


public class CSVExporter
{

	static FileNameExtensionFilter csvFilter = new FileNameExtensionFilter("CSV (Excel)", "csv", "CSV");
	static JFileChooser chooser = new JFileChooser(new File("./data/"));
	

	public static void exportToCSV(TableModel model)
	{
		exportToCSV(model, "export.csv");
	}
	
	public static void exportToCSV(TableModel model, String filename)
	{
		if(!filename.endsWith(".csv"))
			filename += ".csv";
		
		chooser.setFileFilter(csvFilter);
		chooser.setSelectedFile(new File(filename));
		
		int state = chooser.showDialog(null, "Export to CSV");
		if(state == JFileChooser.APPROVE_OPTION)
		{
			File f = chooser.getSelectedFile();
			if(f.exists())
			{
				int confirm = JOptionPane.showConfirmDialog(null, f.getName()+" already exists. Overwrite?");
				if(confirm != JOptionPane.YES_OPTION)
					return;
			}
			
			try
			{
				exportToCSV(model, f);
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(null, e.getMessage());
			}
		}
		
	}
	
	public static void exportToCSV(TableModel model, File file) throws IOException
	{
		if (file != null)
		{
				//BufferedWriter bufferedWriter = new BufferedWriter(new FileWriterWithEncoding(file, Charset.forName("UTF-8")));
				OutputStreamWriter outWriter = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
				PrintWriter fileWriter = new PrintWriter(outWriter);
				
				System.out.println(model.getColumnCount());
				for(int i = 0; i < model.getColumnCount(); i++)
				{
					System.out.println(model.getColumnName(i));
				}

				int cols = model.getColumnCount();
				for (int j = 0; j < cols; ++j)
				{
					String s = model.getColumnName(j);
					fileWriter.print(s);
					if (j < cols - 1) fileWriter.print(",");
				}
				fileWriter.println("");

				for (int i = 0; i < model.getRowCount(); ++i)
				{
					for (int j = 0; j < cols; ++j)
					{
						String s;
						Object obj = model.getValueAt(i, j);
						if (obj != null)
						{
							s = obj.toString();
//							s = s.replaceAll("[\n\r]+", " ");
							if (s.contains(",") || s.contains("\"") || s.contains("�") || s.contains("�") || s.contains("\n") || s.contains("\r"))
							{
								s = s.replaceAll("\"", "\"\"");
								s = s.replaceAll("�", "\"\"");
								s = s.replaceAll("�", "\"\"");
								s = "\"" + s + "\"";
							}
						}
						else
						{
							s = "";
						}
						fileWriter.print(s);
						if (j < cols - 1) fileWriter.print(",");
					}
					fileWriter.println("");
				}
				fileWriter.close();
			
		}
	}
	


//	public static void exportToCSV(TableModel model, File file)
//	{
//		try
//		{
//			if (!file.getName().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");
//
//			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
//			PrintWriter fileWriter = new PrintWriter(bufferedWriter);
//
//			int cols = model.getColumnCount();
//			for (int j = 0; j < cols; ++j)
//			{
//				String s = model.getColumnName(j).replaceAll(",", "_");
//				fileWriter.print(s);
//				if (j < cols - 1) fileWriter.print(",");
//			}
//			fileWriter.println("");
//
//			for (int i = 0; i < model.getRowCount(); ++i)
//			{
//				for (int j = 0; j < cols; ++j)
//				{
//					String s;
//					Object obj = model.getValueAt(i, j);
//					if (obj != null)
//						s = obj.toString().replaceAll(",", "_");
//					else
//						s = "";
//					fileWriter.print(s);
//					if (j < cols - 1) fileWriter.print(",");
//				}
//				fileWriter.println("");
//			}
//			fileWriter.close();
//		}
//		catch (Exception x)
//		{
//			JOptionPane.showMessageDialog(null, x.getMessage());
//		}
//	}

}