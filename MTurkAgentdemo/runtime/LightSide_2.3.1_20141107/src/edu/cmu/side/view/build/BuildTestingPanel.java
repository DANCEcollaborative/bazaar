package edu.cmu.side.view.build;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.view.util.AbstractListPanel;

public class BuildTestingPanel extends AbstractListPanel {

	static final int SOFT_MINIMUM = 20;
	JLabel titleLabel = new JLabel("Evaluation Options:");
	ButtonGroup highOptions = new ButtonGroup();
	JRadioButton radioCV = new JRadioButton("Cross-Validation");
	JRadioButton radioTestSet = new JRadioButton("Supplied Test Set");
	JRadioButton radioNone = new JRadioButton("No Evaluation");
	
	JCheckBox checkCustomCV = new JCheckBox("Custom");
	
	ButtonGroup cvOptions = new ButtonGroup();
	JRadioButton radioRandom = new JRadioButton("Random");
	JRadioButton radioByAnnotation = new JRadioButton("By Annotation:");
	JRadioButton radioByFile = new JRadioButton("By File");

	
	JSlider numFoldSlider = new JSlider();
	JLabel numFoldsLabel = new JLabel("10");
	ButtonGroup foldNums = new ButtonGroup();
	JRadioButton radioAuto = new JRadioButton("Auto");
	JRadioButton radioManual = new JRadioButton("Manual:");
	
	JComboBox<String> annotations = new JComboBox<String>();
	TestSetLoadPanel testSetLoadPanel = new TestSetLoadPanel("Test Set (CSV):");
	
	JPanel cvControlPanel = new JPanel(new RiverLayout(0, 3));
	
	JPanel controlPanel = new JPanel(new BorderLayout(0,0));
	JPanel selectPanel = new JPanel(new RiverLayout(10, 3));
	
	Map<JRadioButton, Component> configPanels = new HashMap<JRadioButton, Component>();
	private int maxFolds = 10;
	private Dictionary<Integer, JLabel> sliderLabels;
	
	static BuildModelControl.ValidationButtonListener numFoldsListener = new BuildModelControl.ValidationButtonListener("numFolds","10");

	public BuildTestingPanel()
	{
		highOptions.add(radioCV);
		highOptions.add(radioTestSet);
		highOptions.add(radioNone);
		radioCV.addActionListener(new BuildModelControl.ValidationButtonListener("type", "CV"));
		radioCV.addActionListener(new BuildModelControl.ValidationButtonListener("test", Boolean.TRUE.toString()));
		radioTestSet.addActionListener(new BuildModelControl.ValidationButtonListener("type", "SUPPLY"));
		radioTestSet.addActionListener(new BuildModelControl.ValidationButtonListener("test", Boolean.TRUE.toString()));
		radioNone.addActionListener(new BuildModelControl.ValidationButtonListener("test", Boolean.FALSE.toString()));
		radioCV.setSelected(true);
		

		BuildModelControl.updateValidationSetting("test", Boolean.TRUE.toString());
		BuildModelControl.updateValidationSetting("type", "CV");
		BuildModelControl.updateValidationSetting("source", "RANDOM");
		BuildModelControl.updateValidationSetting("numFolds", "10");
		BuildModelControl.updateValidationSetting("foldMethod", "AUTO");
		

		addConfigPanelRadioListeners();
		
		setLayout(new BorderLayout(10, 0));
		this.setBorder(new EmptyBorder(0,0,0,0));
		selectPanel.setBorder(new EmptyBorder(0,0,0,0));
		controlPanel.setBorder(new EmptyBorder(0,0,0,0));
		selectPanel.add("vtop", titleLabel);
		selectPanel.add("br vtop", radioCV);
		selectPanel.add("br vtop", radioTestSet);
		selectPanel.add("br vtop", radioNone);
		
		this.add(selectPanel, BorderLayout.WEST);

		buildCVControlPanel();

		controlPanel.add(cvControlPanel);
		GenesisControl.addListenerToMap(testSetLoadPanel, this);
		this.add(controlPanel, BorderLayout.CENTER);
	}

	/**
	 * attach listeners to show appropriate sub-config panel.
	 */
	protected void addConfigPanelRadioListeners()
	{
		configPanels.put(radioCV, cvControlPanel);
		configPanels.put(radioTestSet, testSetLoadPanel);
		configPanels.put(radioNone, new JPanel());
		ActionListener testRadioListener = new ActionListener()
		{

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				for (Entry<JRadioButton, Component> e : configPanels.entrySet())
				{
					if (e.getKey().isSelected())
					{
						Component config = e.getValue();
						controlPanel.removeAll();
						controlPanel.add(config, BorderLayout.CENTER);
						revalidate();
						repaint();
						return;
					}
				}
				BuildTestingPanel.this.add(new JPanel());

			}
			
		};
		for(JRadioButton radio : configPanels.keySet())
		{
			radio.addActionListener(testRadioListener);
		}
	}

	/**
	 * 
	 */
	protected void buildCVControlPanel()
	{
		cvOptions.add(radioRandom);
		cvOptions.add(radioByAnnotation);
		cvOptions.add(radioByFile);
		radioRandom.addActionListener(new BuildModelControl.ValidationButtonListener("source", "RANDOM"));
		radioByAnnotation.addActionListener(new BuildModelControl.ValidationButtonListener("source", "ANNOTATIONS"));
		radioByFile.addActionListener(new BuildModelControl.ValidationButtonListener("source", "FILES"));
		radioRandom.setSelected(true);
		radioAuto.addActionListener(new BuildModelControl.ValidationButtonListener("foldMethod", "AUTO"));
		radioManual.addActionListener(new BuildModelControl.ValidationButtonListener("foldMethod", "MANUAL"));
		
		foldNums.add(radioAuto);
		foldNums.add(radioManual);
		radioAuto.setSelected(true);
	
		numFoldSlider.setMinorTickSpacing(1);
		numFoldSlider.setMinimum(2);
		numFoldSlider.setMaximum(15);
		numFoldSlider.setValue(10);
		numFoldSlider.setEnabled(false);
		numFoldSlider.setPaintLabels(true);
		numFoldSlider.setPaintTicks(false);
		numFoldSlider.setSnapToTicks(true);
		numFoldsListener.actionPerformed(null);
		
		
		final ActionListener cvRadioActionListener = new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {

				numFoldSlider.setEnabled(radioManual.isSelected());

				refreshPanel();
				
//				if(!radioManual.isSelected())
//					numFoldsListener.setValue("10");
				numFoldsListener.actionPerformed(null);
				
				
			}
		};
		
		numFoldSlider.addChangeListener(new ChangeListener(){

			@Override
			public void stateChanged(ChangeEvent arg0)
			{
				updateNumFolds();
			}});
		
		radioByAnnotation.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e)
			{
				
				annotations.setEnabled(radioByAnnotation.isSelected());	
				refreshPanel();
			}});
		annotations.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				String annotation = (String) annotations.getSelectedItem();
				BuildModelControl.updateValidationSetting("annotation", annotation);
				updateSlider(BuildModelControl.getHighlightedFeatureTableRecipe());
			}});
		
		annotations.setEnabled(false);
		radioManual.addActionListener(cvRadioActionListener);
		radioAuto.addActionListener(cvRadioActionListener);
		radioByAnnotation.addActionListener(cvRadioActionListener);
		radioByFile.addActionListener(cvRadioActionListener);
		radioRandom.addActionListener(cvRadioActionListener);
		
		cvControlPanel.setBorder(new EmptyBorder(0,0,0,0));
		annotations.setBorder(new EmptyBorder(0,20,0,20));

		JLabel howToFoldLabel = new JLabel("Fold Assignment:");
		cvControlPanel.add("br left", howToFoldLabel);
		cvControlPanel.add("br left", radioRandom);
		cvControlPanel.add("br left", radioByAnnotation);
		cvControlPanel.add("br hfill",annotations);
		cvControlPanel.add("br left", radioByFile);

		
		JLabel foldLabel = new JLabel("Number of Folds:");
		cvControlPanel.add("br left", foldLabel);
		cvControlPanel.add("br left", radioAuto);
		cvControlPanel.add("br left", radioManual);
		cvControlPanel.add("left", numFoldsLabel);
		cvControlPanel.add("br hfill", numFoldSlider);
	}
	
	@Override
	public void refreshPanel()
	{
		Recipe recipe = BuildModelControl.getHighlightedFeatureTableRecipe();
		updateCVByAnnotationSettings(recipe);
		updateCVByFileSettings(recipe);
		updateSlider(recipe);
		testSetLoadPanel.refreshPanel();
		
//		System.out.println("Validation Settings Refreshed:\n"+BuildModelControl.getValidationSettings());
	}

	/**
	 * @param recipe
	 */
	protected void updateCVByFileSettings(Recipe recipe)
	{
		if(recipe != null)
		{
			if(recipe.getDocumentList().getFilenames().size() < 2)
			{
				radioByFile.setEnabled(false);
				if(radioByFile.isSelected())
				{
					radioRandom.setSelected(true);
					BuildModelControl.updateValidationSetting("foldMethod", "AUTO");
					BuildModelControl.updateValidationSetting("source", "RANDOM");
				}
			}
			else
			{
				radioByFile.setEnabled(true);
			}
		}
	}

	protected void updateCVByAnnotationSettings(Recipe recipe)
	{
//		int i = annotations.getSelectedIndex();
	
		if(recipe != null)
		{
			String selectedColumn = annotations.getSelectedItem()+"";
			
			DocumentList documentList = recipe.getDocumentList();
			String[] annotationNames = documentList.getAnnotationNames();
			Arrays.sort(annotationNames);
			DefaultComboBoxModel<String> model = new DefaultComboBoxModel<String>(annotationNames);
			model.removeElement(recipe.getTrainingTable().getAnnotation());
			annotations.setModel(model);
			int items = annotations.getItemCount();
			if(items > 0)
			{
				annotations.setSelectedIndex(Math.max(model.getIndexOf(selectedColumn), 0));

				radioByAnnotation.setEnabled(true);
				annotations.setEnabled(true);
			}	
			else
			{
				radioByAnnotation.setEnabled(false);
				annotations.setEnabled(false);
				if(radioByAnnotation.isSelected())
				{
					radioRandom.setSelected(true);
					radioAuto.setSelected(true);
					BuildModelControl.updateValidationSetting("source", "RANDOM");
					BuildModelControl.updateValidationSetting("foldMethod", "AUTO");
				}
			}
		}
		else
		{
			radioByAnnotation.setEnabled(false);
			annotations.setEnabled(false);
			radioRandom.setSelected(true);
			radioAuto.setSelected(true);
		}
		
	}

	protected void updateSlider(Recipe recipe)
	{

		//System.out.println("BTP 282: updating slider for "+recipe);
		if(recipe != null)
		{
			DocumentList documentList = recipe.getDocumentList();
			if(radioRandom.isSelected())
			{
				maxFolds = documentList.getSize();
			}
			else if(radioByAnnotation.isSelected() && annotations.getSelectedItem() != null)
			{
				maxFolds = documentList.getPossibleAnn(annotations.getSelectedItem().toString()).size();
			}
			else if(radioByFile.isSelected())
			{
				maxFolds = documentList.getFilenames().size();
			}
		}

		if(maxFolds < SOFT_MINIMUM)
		{
			numFoldSlider.setMaximum(maxFolds);
			numFoldSlider.setMinimum(2);
			numFoldSlider.setLabelTable(numFoldSlider.createStandardLabels(Math.max(1, maxFolds/4)));
		}
		else
		{

			sliderLabels = new Hashtable<Integer, JLabel>();
			sliderLabels.put(2, new JLabel(""+2));
			sliderLabels.put(5, new JLabel(""+5));
			sliderLabels.put(10, new JLabel(""+10));
//			sliderLabels.put(11, new JLabel(""+(SOFT_MINIMUM-5)));
//			sliderLabels.put(12,new JLabel(""+SOFT_MINIMUM));
//			sliderLabels.put(13,new JLabel(""+maxFolds/2));
			sliderLabels.put(15,new JLabel("Max"));
			numFoldSlider.setLabelTable(sliderLabels);
			numFoldSlider.setMinimum(2);
			numFoldSlider.setMaximum(15);
		}
		int numFolds = updateNumFolds();
		BuildModelControl.updateValidationSetting("numFolds", numFolds);
		
	}
	
	protected int updateNumFolds()
	{
		final int max = numFoldSlider.getMaximum();
		int value = numFoldSlider.getValue();
		
		if(maxFolds >= SOFT_MINIMUM)
		{
			if(value == max)
				value = maxFolds;
			else if(value == max - 1)
				value = maxFolds/2;
			else if(value == max - 2)
				value = maxFolds/10;
			else if(value == max - 3)
				value = SOFT_MINIMUM;
			else if(value == max - 4)
				value = SOFT_MINIMUM - 5;
		}
		
		if(value > maxFolds)
			value = maxFolds;
		
		//hate to lose this divisibility hack
//		value = Math.max(2, value);
//		int max = numFoldSlider.getMaximum();
//		//for(value=Math.min(value,max); max % value != 0; value++); //increment until divisible (or == max)

//		if(value > SOFT_MINIMUM)
//		{
//			for (int i = 0; i + value <= max && value - i >= SOFT_MINIMUM; i++)
//			{
//				int remainderUp = max % (i + value);
//				int remainderDown = max % (value - i);
//				if (remainderUp == 0)// < maxFolds / 100)
//				{
//					value = i + value;
//					break;
//				}
//				else if (remainderDown == 0 || value - i == SOFT_MINIMUM)// < maxFolds / 100)
//				{
//					value = value - i;
//					break;
//				}
//
//			}
//		}
		

		numFoldsLabel.setText(value+"");
		
		if(!numFoldSlider.getValueIsAdjusting())
		{	

			String text;
			String itemName = radioRandom.isSelected()? "instance" : radioByAnnotation.isSelected() ? "annotation" : "file";
			//System.out.println("BTP 405: updating fold value to "+value);
			if(maxFolds % value != 0)
			{
				text = "Leave out approximately "+(maxFolds/value)+" "+itemName+"s per fold ("+(maxFolds % value)+" folds will have an extra "+itemName+")";
			}
			else
			{
				text = "Leave out "+(maxFolds/value)+" "+itemName+(maxFolds == value?"":"s")+" per fold.";
			}

			numFoldSlider.setToolTipText(text);
			
			numFoldsListener.setValue(value+"");
			numFoldsListener.actionPerformed(null);
		}
		return value;
		
	}

}
