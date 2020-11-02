package edu.cmu.side.model;


public interface StatusUpdater {

	void update(String updateSlot, int slot1, int slot2);
	void update(String update);
	void reset();
}

