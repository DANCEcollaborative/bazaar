package edu.cmu.side.view.util;

public interface Refreshable
{

	/** What needs to be updated in this panel when something changes in the backend model? */
	public abstract void refreshPanel();

}