package plugins.features;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import se.datadosen.component.RiverLayout;

public class BasicFeaturesPanel extends JPanel{


	private BasicFeatures control;
	private Map<String, JCheckBox> checkboxes = new TreeMap<String, JCheckBox>();

	public BasicFeaturesPanel(BasicFeatures plugin, List<String> basics){		
		this.control = plugin;
		setLayout(new RiverLayout());
		for(String basic : basics){
			if(basic.equals(BasicFeatures.SEPARATOR)){
				add("br left", new JSeparator(SwingConstants.HORIZONTAL));
			}else{
				addCheckbox(plugin, basic);
			}
		}
	}


	public JCheckBox removeCheckbox(String option)
	{
		JCheckBox check = checkboxes.remove(option);
		
		if(check!=null)
		remove(check);
		
		return check;
	}
	
	public void addCheckbox(BasicFeatures plugin, String option)
	{
		JCheckBox box = new JCheckBox(option);
		box.setSelected(plugin.getOption(option));
		box.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent ie) {
				JCheckBox source = ((JCheckBox)ie.getSource());
				control.setOption(source.getText(), source.isSelected());
			}
		});
		checkboxes.put(option, box);
		add("br left", box);
	}
	
}
