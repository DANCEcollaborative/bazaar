package plugins.features;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.plugin.ParallelFeaturePlugin;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.view.util.FastListModel;
import edu.cmu.side.view.util.WarningButton;

public class RegexFeatures extends ParallelFeaturePlugin
{

	private static final String REGEX_FEATURE_SUFFIX = "__rx";
	Component configPanel;

	public RegexFeatures()
	{
		super();
		configPanel = (JComponent) createUI();
	}

	JLabel regexLabel = new JLabel("Regex:");
	JTextField input = new JTextField("good|great|awesome");
	JButton add = new JButton("");
	JButton delete = new JButton("");
	JButton help = new JButton("Regex Cheat Sheet");
	JList patternList = new JList();
	DefaultListModel model = new FastListModel();
	JCheckBox numericCheckBox = new JCheckBox("Count Occurences");
	JButton loadButton = new JButton(new ImageIcon("toolkits/icons/folder_page_white.png"));
	JTextArea testArea = new JTextArea("Do your awesome regular expressions match against this sample text?", 3, 10);
	WarningButton warn = new WarningButton();

	boolean numericFeatures = false;

	List<Pattern> regexes = new ArrayList<Pattern>();

	SIDEPlugin[] extractors;

	// TODO: add file chooser and don't supply LIWC.

	private void configureFromUI()
	{

	}

	@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
	{

		long start = System.currentTimeMillis();
		Collection<FeatureHit> allHits = new TreeSet<FeatureHit>();

		int size = documents.getSize();
		for (int i = 0; i < size; i++)
		{

			if ((i+1) % 50 == 0 || i == size-1) 
				update.update("Extracting Regex Features", i+1, size);

			Collection<FeatureHit> hits = extractFeatureHitsFromDocument(documents, i);
			
			allHits.addAll(hits);
		}
		System.out.println("Regex complete in "+(System.currentTimeMillis()-start)/1000+" seconds.");

		return allHits;
	}

	@Override
	public Collection<FeatureHit> extractFeatureHitsFromDocument(DocumentList documents, int i)
	{
		Map<String, List<String>> coveredTextList = documents.getCoveredTextList();
		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();
		Map<Feature, LocalFeatureHit> locals = new HashMap<Feature, LocalFeatureHit>();
//			locals.clear();
		for (String column : coveredTextList.keySet())
		{
			List<String> textField = coveredTextList.get(column);
			for (Pattern pattern : regexes)
			{
				System.out.println(pattern);
				Matcher matcher = pattern.matcher(textField.get(i));
				while (matcher.find())
				{
					// TODO: ensure that ARFF/etc handle funky name
					// characters acceptably.
					Feature feature;
					if (numericFeatures)
						feature = Feature.fetchFeature("regex", documents.getTextFeatureName( pattern.pattern()+REGEX_FEATURE_SUFFIX, column), Type.NUMERIC, this);
					else
						feature = Feature.fetchFeature("regex", documents.getTextFeatureName( pattern.pattern()+REGEX_FEATURE_SUFFIX, column), Type.BOOLEAN, this);

					if (locals.containsKey(feature))
					{
						LocalFeatureHit localFeatureHit = locals.get(feature);

						if (numericFeatures) localFeatureHit.setValue((Double) localFeatureHit.getValue() + 1);

						localFeatureHit.addHit(column, matcher.start(), matcher.end());
					}
					else
					{
						LocalFeatureHit newHit;
						if (numericFeatures)
							newHit = new LocalFeatureHit(feature, 1.0, i, column, matcher.start(), matcher.end());
						else
							newHit = new LocalFeatureHit(feature, true, i, column, matcher.start(), matcher.end());
						hits.add(newHit);
						locals.put(feature, newHit);
					}
				}
			}
		}
		return hits;
	}

	@Override
	public String getOutputName()
	{
		return "regex";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		return configPanel;
	}
	
	/**
	 * @return
	 */
	protected Component createUI()
	{
		add.setIcon(new ImageIcon("toolkits/icons/add.png"));
		delete.setIcon(new ImageIcon("toolkits/icons/delete.png"));
		help.setIcon(new ImageIcon("toolkits/icons/help.png"));

		add.setToolTipText("Add Regular Expression");
		delete.setToolTipText("Remove Selected Expression");
		delete.setEnabled(false);

		input.setMinimumSize(new Dimension(0, 100));
		
		final JPanel panel = new JPanel(new RiverLayout());
		panel.add("br hfill", input);
		panel.add("right", warn);
		panel.add("right", add);
		panel.add("right", loadButton);
		
		testArea.setForeground(new Color(64, 64, 64));
		testArea.setLineWrap(true);
		testArea.setWrapStyleWord(true);
		testArea.getDocument().addDocumentListener(new DocumentListener()
		{
			
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				patternList.repaint();
			}
			
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				patternList.repaint();				
			}
			
			@Override
			public void changedUpdate(DocumentEvent e)
			{
				patternList.repaint();
			}
		});
		
		patternList.addListSelectionListener(new ListSelectionListener()
		{
			Highlighter highlight = testArea.getHighlighter();
			HighlightPainter marker = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 224, 128));
			
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				Pattern p = (Pattern) patternList.getSelectedValue();
				highlight.removeAllHighlights();
				
				if(p != null)
				{
					Matcher m = p.matcher(testArea.getText());
					while(m.find())
					{
						 try
						{
							highlight.addHighlight(m.start(), m.end(), marker);
							testArea.repaint();
						}
						catch (BadLocationException e1)
						{
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
		});

		patternList.setCellRenderer(new DefaultListCellRenderer()
		{
			private Color matchColorSelected = new Color(192, 224, 255);
			private Color matchBackgroundColorSelected = new Color(32, 64, 192);
			private Color matchColor = new Color(32, 64, 192);
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				
				Matcher m = ((Pattern)value).matcher(testArea.getText());
				if(m.find())
				{
					label.setText("✓ "+label.getText());
					label.setFont(label.getFont().deriveFont(Font.BOLD));
					label.setForeground(isSelected?matchColorSelected:matchColor);
					
					if(isSelected)
						label.setBackground(matchBackgroundColorSelected);
				}
				else
				{
					label.setText("   "+label.getText());
				}
				
				return label;
			}
		});
		
		JScrollPane patternScroll = new JScrollPane();
		patternScroll.setViewportView(patternList);
		patternScroll.setPreferredSize(new Dimension(0, 150));
		panel.add("br hfill", patternScroll);
		panel.add("br left", help);
		panel.add("hfill", new JPanel());
		panel.add("right", delete);
		panel.add("br hfill", new JScrollPane(testArea));
		panel.add("br left", numericCheckBox);

		loadButton.addActionListener(new ActionListener()
		{
			JFileChooser regexChooser;

			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (regexChooser == null)
				{
					regexChooser = new JFileChooser();

					regexChooser.setDialogTitle("Load Regular Expressions");

				}
				int status = regexChooser.showOpenDialog(panel);
				if (status == JFileChooser.APPROVE_OPTION)
				{
					loadPatternsFromFile(regexChooser.getSelectedFile());
				}
			}
		});

		help.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Desktop.getDesktop().browse(new URI("http://www.asiteaboutnothing.net/regex/regex-quickstart.html"));
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				catch (URISyntaxException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});

		ActionListener addExpressionActionListener = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				String patternText = input.getText();
				try
				{
					Pattern p = Pattern.compile(patternText, Pattern.CASE_INSENSITIVE);
					
					boolean alreadyExists = false;
					for(Pattern pete : regexes)
					{
						if(pete.pattern().equals(p.pattern()))
						{
							alreadyExists = true;
							break;
						}
					}
					
					if (!alreadyExists)
					{
						regexes.add(0, p);
						model.add(0, p);
						patternList.setModel(model);
						patternList.setSelectedIndex(0);
						configPanel.revalidate();
					}
					warn.clearWarning();
				}
				catch (Exception ex)
				{
					System.err.println(ex.getMessage());
					// TODO: display warning flag
					warn.setWarning("<html>Couldn't compile regular expression.<br><pre>"+ex.getLocalizedMessage()+"");
				}
				
			}
		};
		add.addActionListener(addExpressionActionListener);
		input.addActionListener(addExpressionActionListener);

		delete.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					for(int index : patternList.getSelectedIndices())
					{
						model.remove(index);
						regexes.remove(index);
					}
					patternList.setModel(model);
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}
			}
		});

		model.addListDataListener(new ListDataListener()
		{
			@Override
			public void intervalRemoved(ListDataEvent arg0)
			{
				delete.setEnabled(!model.isEmpty());
			}

			@Override
			public void intervalAdded(ListDataEvent arg0)
			{
				delete.setEnabled(!model.isEmpty());

			}

			@Override
			public void contentsChanged(ListDataEvent arg0)
			{
				delete.setEnabled(!model.isEmpty());

			}
		});

		numericCheckBox.addActionListener(new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				numericFeatures = numericCheckBox.isSelected();
			}
		});

		return panel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		configureFromUI();
		Map<String, String> settings = new TreeMap<String, String>();
		for (Pattern p : regexes)
		{
			String patternString = p.pattern();
			settings.put("pattern_" + model.indexOf(p), patternString);
		}
		settings.put("num_patterns", "" + regexes.size());
		settings.put("Count Regex Occurrences", "" + numericFeatures);

		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		regexes.clear();
		model.clear();

		numericFeatures = settings.get("Count Regex Occurrences").equals("true");
		numericCheckBox.setSelected(numericFeatures);

		int numPatterns = Integer.parseInt(settings.get("num_patterns"));

		// model.setSize(numPatterns);

		String warnString = "";
		
		for (int i = 0; i < numPatterns; i++)
		{
			String patternKey = "pattern_" + i;
			if (settings.containsKey(patternKey))
			{
				String patternString = settings.get(patternKey);

				try
				{
					Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
					regexes.add(pattern);
					model.add(i, pattern);
				}
				catch(Exception e)
				{
					warnString += "Stored pattern /"+patternString+"/ could not be compiled.\n";
					System.err.println("Stored pattern /"+patternString+"/ could not be compiled");
				}
			}

		}
		
		if(!warnString.isEmpty())
		{
			warn.setWarning(warnString);
		}

		patternList.setModel(model);
		patternList.setSelectedIndex(0);

	}

	public void loadPatternsFromFile(File file)
	{
		try
		{
			Scanner scan = new Scanner(file);

			while (scan.hasNextLine())
			{
				String line = scan.nextLine();
				Pattern pattern = Pattern.compile(line, Pattern.CASE_INSENSITIVE);
				regexes.add(pattern);
				model.add(model.size(), pattern);
			}

			patternList.setModel(model);
			patternList.setSelectedIndex(model.size());
		}
		catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public String toString()
	{
		return "Regular Expressions";
	}

	public static void main(String[] args)
	{
		JFrame freddy = new JFrame("Test!");
		freddy.setContentPane((JPanel) new RegexFeatures().configPanel);
		freddy.setPreferredSize(new Dimension(400, 400));
		freddy.setVisible(true);
		freddy.pack();
		freddy.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
