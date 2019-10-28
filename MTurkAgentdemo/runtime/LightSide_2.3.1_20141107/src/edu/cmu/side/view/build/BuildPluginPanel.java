package edu.cmu.side.view.build;

import java.awt.BorderLayout;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.Workbench;
import edu.cmu.side.control.BuildModelControl;
import edu.cmu.side.plugin.LearningPlugin;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.FastListModel;
import edu.cmu.side.view.util.RadioButtonListEntry;
import edu.cmu.side.view.util.SelectPluginList;

public class BuildPluginPanel extends AbstractListPanel {

	public JPanel panel = new JPanel(new RiverLayout());
	public JPanel middle = new JPanel(new BorderLayout());
	public BuildTestingPanel test = new BuildTestingPanel();
	
	FastListModel pluginsModel = new FastListModel();
	SelectPluginList pluginsList = new SelectPluginList();
	JScrollPane pluginsScroll = new JScrollPane(pluginsList);
	BuildActionPanel.NameListener listener;
	public BuildPluginPanel(BuildActionPanel.NameListener list){
		setLayout(new BorderLayout());
		listener = list;
		ArrayList<RadioButtonListEntry> pluginsToPass = new ArrayList<RadioButtonListEntry>();
		Map<LearningPlugin, Boolean> learningPlugins = BuildModelControl.getLearningPlugins();
		ButtonGroup bg = new ButtonGroup();
		for(LearningPlugin plug : learningPlugins.keySet()){
			RadioButtonListEntry entry = new RadioButtonListEntry(plug, learningPlugins.get(plug));
			pluginsToPass.add(entry);
			bg.add(entry);
		}
		Collections.sort(pluginsToPass, new Comparator<RadioButtonListEntry>(){

			@Override
			public int compare(RadioButtonListEntry arg0, RadioButtonListEntry arg1)
			{
				LearningPlugin one = (LearningPlugin) arg0.getValue();
				LearningPlugin other = (LearningPlugin) arg1.getValue();
				return one.getOutputName().compareTo(other.getOutputName());
			}});
		
		pluginsModel.addAll(pluginsToPass.toArray(new RadioButtonListEntry[0]));
		pluginsList.setModel(pluginsModel);
		pluginsList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				RadioButtonListEntry rb = ((RadioButtonListEntry)pluginsModel.get(pluginsList.getSelectedIndex()));
				highlight(rb);
				Workbench.update(BuildPluginPanel.this);
			}
		});
		if(pluginsToPass.size()>0){
			pluginsToPass.get(0).setSelected(true);	
			highlight(pluginsToPass.get(0));
		}
		panel.add("left", new JLabel("Learning Plugin:"));
		panel.add("br hfill vfill", pluginsScroll);
		add(BorderLayout.NORTH, panel);
		add(BorderLayout.CENTER, test);
	}
	
	public void highlight(RadioButtonListEntry rb){
		LearningPlugin r = (LearningPlugin)rb.getValue();
		BuildModelControl.setHighlightedLearningPlugin(r);
		Component c = r.getConfigurationUI();
		listener.actionPerformed(new ActionEvent(rb, -1, ""));
	}
	
	@Override
	public void refreshPanel(){

		repaint();
		test.refreshPanel();
	}
}
