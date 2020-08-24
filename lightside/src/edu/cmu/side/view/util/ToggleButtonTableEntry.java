package edu.cmu.side.view.util;

import javax.swing.JToggleButton;

public class ToggleButtonTableEntry extends JToggleButton{


	  private Object value = null;

	  public ToggleButtonTableEntry(Object itemValue, boolean selected) {
	    super(itemValue == null ? "" : "" + itemValue, selected);
	    setValue(itemValue);
	  }
	  public Object getValue() {
	    return value;
	  }

	  public void setValue(Object value) {
	    this.value = value;
	  }
	  
}
