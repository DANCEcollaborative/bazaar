package edu.cmu.side.view.explore;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.plugin.EvaluateOneModelPlugin;
import edu.cmu.side.view.generic.ActionBar;

public class ExploreActionBar extends ActionBar{
	
	public ExploreActionBar(StatusUpdater update){
		super(update);
		removeAll();
		setLayout(new RiverLayout());
		setBackground(Color.white);
		combo = new JComboBox();
		combo.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ExploreResultsControl.setHighlightedModelAnalysisPlugin((EvaluateOneModelPlugin)combo.getSelectedItem());
				Workbench.update(ExploreActionBar.this);
			}
		});
		Workbench.reloadComboBoxContent(combo, ExploreResultsControl.getModelAnalysisPlugins().keySet(), null);
		if(combo.getItemCount()>0 && combo.getSelectedIndex()==-1){
			combo.setSelectedIndex(0);
		}
		
		settings.removeAll();
		
		add("left", new JLabel("Exploration Plugin:"));
		add("left", combo);
		add("hfill", settings);
		add("right", progressBar);
		add("right", this.updaters);
	}

	@Override
	public void startedTask()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endedTask()
	{
		// TODO Auto-generated method stub
		
	}
	
}
