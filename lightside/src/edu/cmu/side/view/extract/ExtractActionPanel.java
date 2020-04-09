package edu.cmu.side.view.extract;

import java.util.ArrayList;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.view.generic.ActionBar;
import edu.stanford.nlp.util.StringUtils;

public class ExtractActionPanel extends ActionBar{

	static JTextField threshold = new JTextField(2);

	public ExtractActionPanel(StatusUpdater update){
		super("features", Stage.FEATURE_TABLE, update);
		name.setText(getDefaultName());
		actionButton.setText("Extract");
		actionButton.setIcon(new ImageIcon("toolkits/icons/application_view_columns.png"));
		//Doesn't update the backend when the threshold changes!!
		threshold.setText("5");
		settings.add("left", new JLabel("Rare Threshold:"));
		settings.add("left", threshold);
		actionButton.addActionListener(new ExtractFeaturesControl.BuildTableListener(this, threshold, name));
	}
	
	@Override
	public void refreshPanel()
	{
		super.refreshPanel();
		String annotation = ExtractFeaturesControl.getSelectedClassAnnotation();
		actionButton.setEnabled(ExtractFeaturesControl.hasHighlightedDocumentList() && annotation != null);
		ArrayList<String> names = new ArrayList<String>();
		Map<FeaturePlugin, Boolean> plugins = ExtractFeaturesControl.getFeaturePlugins();
		for(FeaturePlugin p : plugins.keySet())
		{
			if(plugins.get(p))
				names.add(p.getDefaultName());
		}
		if(names.isEmpty())
			names.add("features");
		setDefaultName(StringUtils.join(names, "_"));
	}

	@Override
	public void startedTask()
	{
	}

	@Override
	public void endedTask()
	{
	}
}
