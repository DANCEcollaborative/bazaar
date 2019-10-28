package edu.cmu.side.view.restructure;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;

import edu.cmu.side.control.RestructureTablesControl;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.view.generic.ActionBar;

public class RestructureActionPanel extends ActionBar{

	static JTextField threshold = new JTextField(2);

	public RestructureActionPanel(StatusUpdater update){
		super("restructure",Stage.MODIFIED_TABLE, update);
		actionButton.setText("Restructure");
		actionButton.setIcon(new ImageIcon("toolkits/icons/application_side_expand.png"));
		actionButton.setIconTextGap(10);
		actionButton.addActionListener(new RestructureTablesControl.FilterTableListener(this, name));

		threshold.setText("5");
		settings.add("left", new JLabel("Rare Threshold:"));
		settings.add("left", threshold);
		name.setText(getDefaultName());
	}
	
	public int getThreshold()
	{
		try
		{
			return Integer.parseInt(threshold.getText());
		}
		catch(NumberFormatException e)
		{
			return 0;
		}
	}

	@Override
	public void refreshPanel(){
		super.refreshPanel();
		actionButton.setEnabled(RestructureTablesControl.getFilterPlugins().values().contains(Boolean.TRUE)
								&& RestructureTablesControl.getHighlightedFeatureTableRecipe() != null);
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
