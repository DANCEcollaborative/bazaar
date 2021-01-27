package plugins.learning;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JPanel;

import edu.cmu.side.Workbench;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.feature.Feature;

import se.datadosen.component.RiverLayout;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;

public class WekaGeneric extends WekaCore
{

	GenericObjectEditor genericObjectEditor;
	PropertyPanel propertyPanel;
	JPanel panel = new JPanel(new RiverLayout());

	public WekaGeneric()
	{
		genericObjectEditor = new GenericObjectEditor();
		genericObjectEditor.setClassType(weka.classifiers.Classifier.class);
		genericObjectEditor.setValue(new weka.classifiers.bayes.NaiveBayes());
		propertyPanel = new PropertyPanel(genericObjectEditor);
		panel.add("hfill", propertyPanel);
		panel.setSize(100, 100);
		
		genericObjectEditor.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent arg0)
			{
//				System.out.println("property change!");
//				System.out.println(panel.getParent().getClass().getSimpleName());
//				System.out.println(panel.getParent().getParent().getClass().getSimpleName());
				Workbench.update(panel.getParent());
			}
		});
	}

	@Override
	public String toString()
	{
		return "Weka (All)";
	}

	@Override
	public String getOutputName()
	{
		return "weka";
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		return super.generateConfigurationSettings();
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		super.configureFromSettings(settings);
		//FIXME: doesn't store settings right with Parallel Learning Plugin
		loadClassifierFromSettings(settings);
		genericObjectEditor.setValue(classifier);
	}

	public boolean supportsClassType(Feature.Type type)
	{
		classifier = (Classifier) genericObjectEditor.getValue();
		return super.supportsClassType(type);
	}
	
	@Override
	protected Component getConfigurationUIForSubclass() 
	{
		Recipe table = BuildModelControl.getHighlightedFeatureTableRecipe();
		if(table != null)
		{
			Feature.Type type = table.getTrainingTable().getClassValueType();
			Capabilities capabilities = new Capabilities(null);
			capabilities.disableAllClasses();
			
			switch(type)
			{
				case NUMERIC:
					capabilities.enable(Capability.NUMERIC_CLASS);
					break;
				case NOMINAL:
					capabilities.enable(Capability.NOMINAL_CLASS);
					break;
				case BOOLEAN:
					 capabilities.enable(Capability.BINARY_CLASS);
					 break;
				default:
					capabilities.enableAllClasses();
			}
			genericObjectEditor.setCapabilitiesFilter(capabilities);
		}
		
		return panel;
	}

}
