package plugins.analysis.one.display;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.data.TrainingResult;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit.HitLocation;

public class OriginalSingleDocumentHighlight
{
	Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
	HighlightPainter painter = new DefaultHighlightPainter(new Color(255, 224, 128));

	TrainingResult trainingResult;
	Integer index;

	private Map<String, Integer> indexTranslations = new TreeMap<String, Integer>();

	JTextArea textArea = new JTextArea(10, 2);
	JPanel panel = new JPanel();

	public OriginalSingleDocumentHighlight(TrainingResult tr, int docID)
	{
		trainingResult = tr;
		;
		index = docID;
	}

	public Component getUI()
	{
		panel = new JPanel(new RiverLayout());
		JLabel label = new JLabel("Instance " + index + " (Predicted " + trainingResult.getPredictions().get(index).toString() + ", Actual "
				+ trainingResult.getEvaluationTable().getAnnotations().get(index) + ")");
		label.setFont(font);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		this.highlightFeature(ExploreResultsControl.getHighlightedFeature());
		
		panel.add("left", label);
		panel.add("br hfill vfill", new JScrollPane(textArea));

		
		return panel;
	}

	public void highlightFeature(Feature feature)
	{
		
		FeatureTable featureTable = trainingResult.getEvaluationTable();
		DocumentList docs = featureTable.getDocumentList();
		StringBuilder text = new StringBuilder();//docs.getPrintableTextAt(index);
		
		
		Map<String, List<String>> textColumns = docs.getCoveredTextList();
		
		
		Map<String, Integer> offsets = new HashMap<String, Integer>();
		for(String column : textColumns.keySet())
		{
			String columnText = textColumns.get(column).get(index);
			
			if(textColumns.size() > 1)
				text.append(column+":\n");
			
			offsets.put(column, text.length());
			
			if(feature != null && feature.isTokenized())
			{
				text.append(prepareTokenizedText(columnText, column, feature));
			}
			
			else 
			{
				text.append(columnText);
			}

			if(textColumns.size() > 1)
				text.append("\n\n");
			
		}
			
		textArea.setText(text.toString());
		
		
		Highlighter highLightSIDE = textArea.getHighlighter();
		Collection<FeatureHit> hits = featureTable.getHitsForDocument(index);
				
		
		if(feature != null)
		for(FeatureHit h : hits)
		{
			if(h instanceof LocalFeatureHit && h.getFeature().equals(feature))
			{
				LocalFeatureHit local = (LocalFeatureHit) h;
				
				//System.out.println("SDH 107 Highlighting local feature hit "+h);
				
				
				for(HitLocation spot : local.getHits())
				{
					try
					{
						String column = spot.getColumn();
						int start = spot.getStart();
						int end = spot.getEnd();
						
						if(feature.isTokenized())
						{
							start = indexTranslations.get(column+":"+start);
							end = indexTranslations.get(column+":"+end)-1;
						}
						
						int columnOffset = offsets.get(column);
						start += columnOffset;
						end += columnOffset;
						
						highLightSIDE.addHighlight(start, end, painter);
					}
					catch (BadLocationException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					catch (NullPointerException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	


//	static
//	{
//		try
//		{
//			tagger = new MaxentTagger("toolkits/maxent/left3words-wsj-0-18.tagger");
//			factory = PTBTokenizerFactory.newTokenizerFactory();
//			// check if we are to use a custom stoplist
//			//
//			// this should be only a file name with the file being present in
//			// the etc/ directory of TagHelperTools2
//		}
//		catch (Exception e)
//		{
//			e.printStackTrace();
//			JOptionPane.showMessageDialog(null, "Could not find MaxentTagger files", "ERROR", JOptionPane.ERROR_MESSAGE);
//			System.exit(0);
//		}
//	}

	StringBuilder prepareTokenizedText(String instance, String columnName, Feature feature)
	{
		StringBuilder tokenString = new StringBuilder();

		List<String> tokens = feature.getExtractor().tokenize(feature, instance);
		
		int i;
		for(i = 0; i < tokens.size(); i++)
		{
			indexTranslations.put(columnName+":"+i, tokenString.length());
			String token = tokens.get(i);
			tokenString.append(token);
			tokenString.append(" ");
		}
		indexTranslations.put(columnName+":"+i, tokenString.length());

		return tokenString;
	}
	
}
