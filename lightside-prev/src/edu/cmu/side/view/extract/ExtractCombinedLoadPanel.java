package edu.cmu.side.view.extract;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.RecipeManager;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.CheckBoxListEntry;
import edu.cmu.side.view.util.FastListModel;
import edu.cmu.side.view.util.SelectPluginList;
import edu.cmu.side.view.util.WarningButton;

public class ExtractCombinedLoadPanel extends AbstractListPanel
{

	ExtractLoadPanel files = new ExtractLoadPanel("CSV Files:");

	JComboBox annotationFieldCombo = new JComboBox();
	JComboBox classTypeCombo = new JComboBox();
	SelectPluginList textColumnsList = new SelectPluginList();
	JScrollPane textColumnsScroll = new JScrollPane(textColumnsList);
	JCheckBox differentiateBox = new JCheckBox("Differentiate Text Fields");
	WarningButton warn = new WarningButton();

	public ExtractCombinedLoadPanel(String s)
	{	
		setLayout(new BorderLayout());
		add(BorderLayout.CENTER, files);
		JPanel pan = new JPanel(new RiverLayout());
		ExtractFeaturesControl.AnnotationComboListener comboListener = new ExtractFeaturesControl.AnnotationComboListener(this);
		annotationFieldCombo.addActionListener(comboListener);
		classTypeCombo.addActionListener(comboListener);
		// annotationFieldCombo.setRenderer(new AbbreviatedComboBoxCellRenderer(30));

		textColumnsScroll.setPreferredSize(new Dimension(0, 100));
		
		pan.add("left", new JLabel("Class:"));
		pan.add("hfill", annotationFieldCombo);
		pan.add("br left", new JLabel("Type: "));
		pan.add("hfill", classTypeCombo);
		pan.add("br left", new JLabel("Text Fields:"));
		pan.add("hfill", new JPanel());
		pan.add("left", warn);
		pan.add("br hfill", textColumnsScroll);
		pan.add("br left", differentiateBox);
		add(BorderLayout.SOUTH, pan);

		// GenesisControl.addListenerToMap(files, files); //you should never
		// have to listen to yourself.
		// GenesisControl.addListenerToMap(this, files); //document lists update
		// when "this" changes now
		GenesisControl.addListenerToMap(files, this);
		GenesisControl.addListenerToMap(RecipeManager.Stage.DOCUMENT_LIST, files);
		
		ImageIcon iconLoad = new ImageIcon("toolkits/icons/csv_note.png");
		load.setIcon(iconLoad);
		
		classTypeCombo.setModel(new DefaultComboBoxModel(new Type[]{Type.NOMINAL, Type.NUMERIC}));
		classTypeCombo.setSelectedItem(Type.NOMINAL);
		
		differentiateBox.addActionListener(ExtractFeaturesControl.differentiateTextColumnsListener);
		differentiateBox.setToolTipText("When checked, extractors will create features that are unique to each text field.");
	}

	@Override
	public void refreshPanel()
	{
		if (files.getHighlight() != null)
		{
			DocumentList sdl = ExtractFeaturesControl.getHighlightedDocumentListRecipe().getDocumentList();
			Workbench.reloadComboBoxContent(annotationFieldCombo, sdl.allAnnotations().keySet(), ExtractFeaturesControl.getSelectedClassAnnotation());
			Map<String, Boolean> columns = new TreeMap<String, Boolean>();
			
			if(ExtractFeaturesControl.getSelectedClassAnnotation() == null || !sdl.allAnnotations().containsKey(ExtractFeaturesControl.getSelectedClassAnnotation()))
			{
				ExtractFeaturesControl.setSelectedClassAnnotation(sdl.guessTextAndAnnotationColumns());
			}
			
			if(sdl.guessValueType(ExtractFeaturesControl.getSelectedClassAnnotation()) == Type.NOMINAL)
			{
				classTypeCombo.setModel(new DefaultComboBoxModel(new Object[]{Type.NOMINAL}));
			}
			else
			{
				Type oldType = ExtractFeaturesControl.getSelectedClassType();
				classTypeCombo.setModel(new DefaultComboBoxModel(new Object[]{Type.NUMERIC, Type.NOMINAL}));
				ExtractFeaturesControl.setSelectedClassType(oldType);
			}
			
			
			for (String s : sdl.allAnnotations().keySet())
			{
				if (ExtractFeaturesControl.getSelectedClassAnnotation() == null || !ExtractFeaturesControl.getSelectedClassAnnotation().equals(s)) columns.put(s, false);
			}
			for (String s : sdl.getTextColumns())
			{
				columns.put(s, true);
			}
			reloadCheckBoxList(columns);
		}
		else
		{
			Workbench.reloadComboBoxContent(annotationFieldCombo, new ArrayList<Object>(), null);
			reloadCheckBoxList(new TreeMap<String, Boolean>());
		}
		annotationFieldCombo.setEnabled(ExtractFeaturesControl.hasHighlightedDocumentList());
		classTypeCombo.setEnabled(ExtractFeaturesControl.hasHighlightedDocumentList());

	}

	public void reloadCheckBoxList(Map<String, Boolean> labels)
	{
		FastListModel model = new FastListModel();
		CheckBoxListEntry[] array = new CheckBoxListEntry[labels.size()];
		int i = 0;
		for (String key : labels.keySet())
		{
			array[i] = new CheckBoxListEntry(key, labels.get(key));
			array[i].addItemListener(new ItemListener()
			{
				@Override
				public void itemStateChanged(ItemEvent ie)
				{
					// because this modifies a recipe, should it notify on the
					// recipemanager? or on the individual recipe?
					DocumentList sdl = ExtractFeaturesControl.getHighlightedDocumentListRecipe().getDocumentList();
					sdl.setTextColumn(((CheckBoxListEntry) ie.getItem()).getValue().toString(), ie.getStateChange() == ItemEvent.SELECTED);
					Workbench.reloadComboBoxContent(annotationFieldCombo, sdl.allAnnotations().keySet(), ExtractFeaturesControl.getSelectedClassAnnotation());
					Workbench.update(RecipeManager.Stage.DOCUMENT_LIST);
					Workbench.update(ExtractCombinedLoadPanel.this);
				}
			});
			i++;
		}
		model.addAll(array);
		textColumnsList.setModel(model);
	}


	public void setWarning(String warnText)
	{
		warn.setWarning(warnText);;
	}

	public void clearWarning()
	{
		warn.clearWarning();
	}
	
	public JComboBox getAnnotationFieldCombo()
	{
		return annotationFieldCombo;
	}
	

	public JComboBox getClassTypeCombo()
	{
		return classTypeCombo;
	}
}
