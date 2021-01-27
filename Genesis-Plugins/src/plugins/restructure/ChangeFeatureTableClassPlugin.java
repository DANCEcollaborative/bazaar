package plugins.restructure;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.model.data.FeatureTable;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.plugin.RestructurePlugin;
import edu.cmu.side.view.util.Refreshable;

public class ChangeFeatureTableClassPlugin extends RestructurePlugin
{
	String newClass = "Clarity";
	Type newType = Type.NOMINAL;
	JComboBox<String> classChooser = new JComboBox<String>();
	JComboBox<Type> typeChooser = new JComboBox<Type>(new Type[]{Type.NOMINAL, Type.NUMERIC});
	JPanel panel = null;
	
	@Override
	protected FeatureTable restructureWithMaskForSubclass(FeatureTable original, boolean[] mask, int threshold, StatusUpdater progressIndicator)
	{
		progressIndicator.update("Cloning original table...");
		FeatureTable too = original.clone();
		too.setAnnotation(newClass);
		too.setClassValueType(newType);
		progressIndicator.update("Generating class values...");
		too.generateConvertedClassValues();
		logger.log(Level.FINE, too.getAnnotation()+":"+too.getClassValueType());
		return too;
	}

	@Override
	protected FeatureTable restructureTestSetForSubclass(FeatureTable original, FeatureTable test, int threshold, StatusUpdater progressIndicator)
	{
		return restructureWithMaskForSubclass(original, null, threshold, progressIndicator);
	}

	@Override
	public String getOutputName()
	{
		return "classchange";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		if(panel == null)
			panel = (JPanel) makePanel();
		return panel;
	}

	public Component makePanel()
	{
		panel = new JPanel();
		panel.add(classChooser);
		panel.add(typeChooser);
		classChooser.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				newClass = (String) classChooser.getSelectedItem();
			}
		});
		typeChooser.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				newType = (Type) typeChooser.getSelectedItem();
			}
		});
		
		Refreshable updater = new Refreshable()
		{
			@Override
			public void refreshPanel()
			{
				Recipe featureTableRecipe = RestructureTablesControl.getHighlightedFeatureTableRecipe();
				if(featureTableRecipe != null)
				classChooser.setModel(new DefaultComboBoxModel<String>(featureTableRecipe.getTrainingTable().getDocumentList().allAnnotations().keySet().toArray(new String[0])));
			}
		};
		GenesisControl.addListenerToMap(Stage.FEATURE_TABLE, updater);
		GenesisControl.addListenerToMap(Stage.MODIFIED_TABLE, updater);
		updater.refreshPanel();
		
		return panel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new HashMap<String, String>();
		settings.put("newClass", newClass);
		settings.put("newType", newType.toString());
		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		newClass = settings.get("newClass");
		newType = Type.valueOf(settings.get("newType"));
	}
	
	public String toString()
	{
		return "Target Class";
	}

}
