package edu.cmu.side.view.util;

import javax.swing.JRadioButton;

public class RadioButtonListEntry extends JRadioButton implements Comparable
{
	  private Object value = null;

	  public RadioButtonListEntry(Object itemValue, boolean selected) {
	    super(itemValue == null ? "" : "" + itemValue, selected);
	    setValue(itemValue);
	    setBackground(null);
	  }

	  public Object getValue() {
	    return value;
	  }

	  public void setValue(Object value) {
	    this.value = value;
	  }

	  @Override
	public String toString()
	  {
		  if(value == null) return "NULL";
		  return value.toString();
	  }

	@Override
	public int compareTo(Object o)
	{
		return this.toString().compareTo(o.toString());
	}
}
