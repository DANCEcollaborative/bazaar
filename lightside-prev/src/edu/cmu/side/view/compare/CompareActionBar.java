package edu.cmu.side.view.compare;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.CompareModelsControl;
import edu.cmu.side.model.StatusUpdater;
import edu.cmu.side.plugin.EvaluateTwoModelPlugin;
import edu.cmu.side.view.generic.ActionBar;

public class CompareActionBar extends ActionBar {

	public CompareActionBar(StatusUpdater update){
		super(update);
		removeAll();
		setLayout(new RiverLayout());
		setBackground(Color.white);
		combo = new JComboBox();
		EvaluateTwoModelPlugin plug = (CompareModelsControl.getModelComparisonPlugins().keySet().size()>0?
				CompareModelsControl.getModelComparisonPlugins().keySet().toArray(new EvaluateTwoModelPlugin[0])[0]:null);
		combo.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				CompareModelsControl.setHighlightedModelComparisonPlugin((EvaluateTwoModelPlugin)combo.getSelectedItem());
				Workbench.update(CompareActionBar.this);
			}
		});
		Workbench.reloadComboBoxContent(combo, CompareModelsControl.getModelComparisonPlugins().keySet(), plug);
		add("left", new JLabel("Comparison Plugin:"));
		add("hfill", combo);
		
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
