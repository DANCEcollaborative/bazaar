package edu.cmu.side.view.generic;

import java.awt.Component;
import java.awt.Font;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JLabel;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.plugin.SIDEPlugin;
import edu.cmu.side.view.util.AbstractListPanel;
import edu.cmu.side.view.util.Refreshable;

public abstract class GenericPluginConfigPanel<E extends SIDEPlugin> extends AbstractListPanel {

	Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
	protected Set<E> visiblePlugins = new TreeSet<E>();

	protected boolean showLabels;

	public GenericPluginConfigPanel(){
		this(true);
	}
	public GenericPluginConfigPanel(boolean label)
	{
		setLayout(new RiverLayout());
		showLabels = label;
	}
	

	public void refreshPanel(Map<E, Boolean> plugins){
		this.removeAll();
		TreeSet<E> pluginSet = new TreeSet<E>(plugins.keySet()); //sort by plugin key
		for(E plugin : pluginSet){
			if(plugins.get(plugin))
			{
				if(showLabels)
				{
					JLabel label = new JLabel("Configure " + plugin.toString());
					label.setFont(font);
					this.add("br left", label);					
				}
				Component configurationUI = plugin.getConfigurationUI();
				this.add("br hfill vfill", configurationUI);	
				
				if(configurationUI instanceof Refreshable)
				{
					((Refreshable)(configurationUI)).refreshPanel();
				}
			}
		}
		this.revalidate();
		this.repaint();
	}
}
