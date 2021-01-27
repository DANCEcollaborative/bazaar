package plugins.restructure.multi.view;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import plugins.restructure.multi.model.StructuredLevel;
import edu.cmu.side.view.util.RadioButtonListEntry;

public abstract class LevelButtonListener implements ItemListener{
	
	@Override
	public void itemStateChanged(ItemEvent arg0) {
		if(arg0.getSource() instanceof RadioButtonListEntry && ((RadioButtonListEntry)arg0.getSource()).isSelected()){
			setSelected((StructuredLevel)((RadioButtonListEntry)arg0.getSource()).getValue());
		}
	}
	
	public abstract void setSelected(StructuredLevel level);
	
}
