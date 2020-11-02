package plugins.analysis.one.display;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
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

public class SingleDocumentHighlight
{
	final static private Font HEADER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 14);
	HighlightPainter painter = new DefaultHighlightPainter(new Color(255, 224, 128));
	HighlightPainter scorePainter = new DefaultHighlightPainter(new Color(224, 128, 255, 128));


	JLabel detail = new JLabel();
	JScrollPane scroller;
	
	TrainingResult trainingResult;
	Integer index;

	private Map<String, Integer> indexTranslations = new TreeMap<String, Integer>(); // for
																						// tokenization
	Map<String, Integer> offsets = new HashMap<String, Integer>();
	// for columns

	JTextArea textArea = new JTextArea();
	JPanel panel = new JPanel();

	Feature highlightFeature;

	public SingleDocumentHighlight(TrainingResult tr, int docID)
	{
		trainingResult = tr;
		index = docID;
	}

	public SingleDocumentHighlight(TrainingResult result, int docIndex, Feature highlightFeature)
	{
		trainingResult = result;
		index = docIndex;
		this.highlightFeature = highlightFeature;
	}

	public Component getUI(Component parent)
	{
		panel = new JPanel(new RiverLayout());

		panel.addComponentListener(new ComponentAdapter()
		{
			Timer resizeTimer;
			boolean adapting = false;
			int lineHeight = textArea.getFont().getSize()+4;

			@Override
			public void componentResized(ComponentEvent e)
			{
				if (resizeTimer != null && !adapting)
				{
					resizeTimer.restart();
				}
				else 
					if (!adapting)
				{
					resizeTimer = new Timer(500, new ActionListener()
					{

						@Override
						public void actionPerformed(ActionEvent e)
						{

							adapting = true; 
							Dimension d = scroller.getSize();
							int scale = textArea.getLineCount() + textArea.getText().length() / ((scroller.getWidth()-10) / 10);
							int h = Math.min(150, scale * lineHeight);
							h = Math.max(40, h);
							d.height = h;
							d.width = 0;
							resizeTimer.stop();
							resizeTimer = null;
//							System.out.println(d);
							
							scroller.setPreferredSize(d);
//							panel.revalidate();

							adapting = false;
						}
					});
					resizeTimer.start();

				}

			}

		});

		String predictedLabel = trainingResult.getPredictions().get(index).toString();
		DocumentList docs = trainingResult.getEvaluationTable().getDocumentList();
		String actualLabel = trainingResult.getEvaluationTable().getAnnotations().get(index);
		JLabel label = new JLabel("Instance " + index + " (Predicted " + predictedLabel + ", Actual " + actualLabel + ")");
		label.setFont(HEADER_FONT);

		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setColumns(10);
		textArea.setRows(0);

		setTextFromDoc(docs);

		scroller = new JScrollPane(textArea);


		if (highlightFeature != null)
		{
			this.highlightFeature(ExploreResultsControl.getHighlightedFeature());
			detail.setText("Highlighting " + highlightFeature.getFeatureName() + " feature hits");
		}
		
		int scale = textArea.getLineCount() + textArea.getText().length() / ((parent.getWidth()-20)/10);
		int h = Math.min(150,scale * textArea.getFont().getSize()+4);
		h = Math.max(40, h);
		scroller.setPreferredSize(new Dimension(0, h));

		panel.add("left", label);
		panel.add("br left", detail);
		panel.add("br hfill vfill", scroller);
		
		java.util.Timer timmy = new java.util.Timer();
		timmy.schedule(new TimerTask()
		{
			
			@Override
			public void run()
			{
				textArea.scrollRectToVisible(new Rectangle(0,0));
			}
		}, 100);
		
		return panel;
	}

	protected Map<String, List<String>> setTextFromDoc(DocumentList docs)
	{
		Map<String, List<String>> textColumns = docs.getCoveredTextList();

		StringBuilder text = new StringBuilder();// docs.getPrintableTextAt(index);
		for (String column : textColumns.keySet())
		{
			String columnText = textColumns.get(column).get(index);

			if (textColumns.size() > 1) text.append(column + ":\n");

			offsets.put(column, text.length());

			text.append(columnText);

			if (textColumns.size() > 1) text.append("\n\n");

		}

		textArea.setText(text.toString());
		return textColumns;
	}

	public void highlightFeature(Feature feature)
	{

		FeatureTable featureTable = trainingResult.getEvaluationTable();
		DocumentList docs = featureTable.getDocumentList();

		Highlighter highLightSIDE = textArea.getHighlighter();
		Collection<FeatureHit> hits = featureTable.getHitsForDocument(index);

		if (feature != null) for (FeatureHit h : hits)
		{
			if (h instanceof LocalFeatureHit && h.getFeature().equals(feature))
			{
				LocalFeatureHit local = (LocalFeatureHit) h;

				// System.out.println("SDH 107 Highlighting local feature hit "+h);

			for (HitLocation spot : local.getHits())
			{
				try
				{
					String column = spot.getColumn();
					int start = spot.getStart();
					int end = spot.getEnd();

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

	StringBuilder prepareTokenizedText(String instance, String columnName, Feature feature)
	{
		StringBuilder tokenString = new StringBuilder();

		List<String> tokens = feature.getExtractor().tokenize(feature, instance);

		int i;
		for (i = 0; i < tokens.size(); i++)
		{
			indexTranslations.put(columnName + ":" + i, tokenString.length());
			String token = tokens.get(i);
			tokenString.append(token);
			tokenString.append(" ");
		}
		indexTranslations.put(columnName + ":" + i, tokenString.length());

		return tokenString;
	}

}
