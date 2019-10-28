package edu.cmu.side.view.util;

import javax.swing.JCheckBox;

/**
 * 
 * @author gtoffoli
 */
public class CheckBoxListEntry extends JCheckBox implements Comparable<CheckBoxListEntry>
{

  private Object value = null;

  public CheckBoxListEntry(Object itemValue, boolean selected) {
    super(itemValue == null ? "" : "" + itemValue, selected);
    setValue(itemValue);
    setBackground(null);
  }

  @Override
public boolean isSelected() {
    return super.isSelected();
  }

  @Override
public void setSelected(boolean selected) {
    super.setSelected(selected);
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
public int compareTo(CheckBoxListEntry box)
{
//	if(this.isSelected() ^ box.isSelected())
//	{
//		return this.isSelected() ? -1 : 1;
//	}
//	else
	{
		return this.getValue().toString().compareTo(box.getValue().toString());
	}
}

}