package edu.cmu.side.view.extract;


import edu.cmu.side.Workbench;
import edu.cmu.side.control.ExtractFeaturesControl;
import edu.cmu.side.control.GenesisControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.view.generic.GenericLoadCSVPanel;

public class ExtractLoadPanel extends GenericLoadCSVPanel{

	public ExtractLoadPanel(String s){
		super(s);		
	}

	@Override
	public void setHighlight(Recipe r) {
		ExtractFeaturesControl.setHighlightedDocumentListRecipe(r);
		Workbench.update(this);
	}

	@Override
	public Recipe getHighlight() {
		return ExtractFeaturesControl.getHighlightedDocumentListRecipe();
	}

	@Override
	public void refreshPanel() {		
		refreshPanel(GenesisControl.getDocumentLists());
	}

	@Override
	public void deleteHighlight()
	{
		ExtractFeaturesControl.setHighlightedDocumentListRecipe(null);
		
	}
	
}
