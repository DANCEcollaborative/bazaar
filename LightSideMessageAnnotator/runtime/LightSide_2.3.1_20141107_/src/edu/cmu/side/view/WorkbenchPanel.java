package edu.cmu.side.view;

import java.awt.Color;


import javax.swing.BorderFactory;
import javax.swing.JTabbedPane;

import edu.cmu.side.view.build.BuildModelPane;
import edu.cmu.side.view.compare.CompareModelsPane;
import edu.cmu.side.view.explore.ExploreResultsPane;
import edu.cmu.side.view.extract.ExtractFeaturesPane;
import edu.cmu.side.view.predict.PredictLabelsPane;
import edu.cmu.side.view.restructure.RestructureFeaturesPane;

public class WorkbenchPanel extends JTabbedPane{
	
	ExtractFeaturesPane extractFeatures = new ExtractFeaturesPane();
	RestructureFeaturesPane modifyFeatures = new RestructureFeaturesPane();
	BuildModelPane buildModel = new BuildModelPane();
	ExploreResultsPane exploreResults = new ExploreResultsPane();
	CompareModelsPane compareModels = new CompareModelsPane();
	PredictLabelsPane predictLabels = new PredictLabelsPane();

	boolean updating = false;
	public WorkbenchPanel(){
		this.setBorder(BorderFactory.createEmptyBorder());
		setBackground(new Color(246,246,246));
		addTab("Extract Features", extractFeatures);
		addTab("Restructure Data", modifyFeatures);
		addTab("Build Models", buildModel);
		addTab("Explore Results", exploreResults);
		addTab("Compare Models", compareModels);
		addTab("Predict Labels", predictLabels);
	}
}
