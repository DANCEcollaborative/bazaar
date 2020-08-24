package edu.cmu.side.view.generic;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.Refreshable;

public class GenericTripleFrame extends JPanel{

	JSplitPane bigSplit = new JSplitPane();
	JSplitPane smallSplit = new JSplitPane();
	JScrollPane scroll;
	ArrayList<AbstractListPanel> panels = new ArrayList<AbstractListPanel>();

	public GenericTripleFrame(AbstractListPanel chooseData, AbstractListPanel choosePlugin, AbstractListPanel chooseSettings, boolean scrollable){
		bigSplit.setLeftComponent(chooseData);
		
		smallSplit.setLeftComponent(choosePlugin);
		
		if (scrollable)
		{
			scroll = new JScrollPane(chooseSettings);
			smallSplit.setRightComponent(scroll);
		}
		else
		{
			smallSplit.setRightComponent(chooseSettings);
		}

		bigSplit.setRightComponent(smallSplit);
		
		scroll.setBorder(BorderFactory.createEmptyBorder());
		bigSplit.setBorder(BorderFactory.createEmptyBorder());
		smallSplit.setBorder(BorderFactory.createEmptyBorder());
		
		chooseData.setPreferredSize(new Dimension(275, 450));
		smallSplit.setPreferredSize(new Dimension(650, 450));
		choosePlugin.setPreferredSize(new Dimension(300, 450));
		scroll.setPreferredSize(new Dimension(325,450));

		Dimension minimumSize = new Dimension(50, 200);
		chooseData.setMinimumSize(minimumSize);
		choosePlugin.setMinimumSize(minimumSize);
		chooseSettings.setMinimumSize(minimumSize);
		
		panels.add(chooseData);
		panels.add(choosePlugin);
		panels.add(chooseSettings);
		setLayout(new BorderLayout());
		
		add(BorderLayout.CENTER, bigSplit);
	}
	public GenericTripleFrame(AbstractListPanel chooseData, AbstractListPanel choosePlugin, AbstractListPanel chooseSettings){
		this(chooseData, choosePlugin, chooseSettings, true);
	}
	
	public void setBigSplitPosition(int bigPosition)
	{
		bigSplit.setDividerLocation(bigPosition);
	}
	public void setSmallSplitPosition(int smallPosition)
	{
		smallSplit.setDividerLocation(smallPosition);
	}
	
	public void refreshPanel(){
		for(Refreshable panel : panels){
			panel.refreshPanel();
		}
		scroll.repaint();
	}
}
