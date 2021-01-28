package edu.cmu.side.view.util;

import javax.swing.DefaultListModel;


public class FastListModel extends DefaultListModel{
	private boolean listenersEnabled = true;
	public boolean getListenersEnabled(){ return listenersEnabled;}
	public void setListenersEnabled(boolean enabled){ listenersEnabled = enabled; }
	@Override
	public void fireContentsChanged(Object source, int index0, int index1){
		if(getListenersEnabled()){
			super.fireContentsChanged(source, index0, index1);
		}
	}
	@Override
	public void fireIntervalAdded(Object source, int index0, int index1){
		if(getListenersEnabled()){
			super.fireIntervalAdded(source, index0, index1);
		}
	}
	@Override
	public void fireIntervalRemoved(Object source, int index0, int index1){
		if(getListenersEnabled()){
			super.fireIntervalRemoved(source, index0, index1);
		}
	}
	public void addAll(Object[] a){
		setListenersEnabled(false);
		for(int i = 0; i < a.length; i++){
			super.add(this.getSize(), a[i]);
		}
		setListenersEnabled(true);
		super.fireIntervalAdded(this, this.getSize()-a.length, this.getSize()-1);
	}
}
