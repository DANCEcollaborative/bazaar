package plugins.analysis.two.matrix;

import java.awt.Dimension;

import javax.swing.JTabbedPane;

import edu.cmu.side.control.GenesisControl;

public class MatrixExplorePanel extends JTabbedPane{

	DifferenceFeaturePanel byFeature = new DifferenceFeaturePanel();
	DifferenceInstancePanel byInstance = new DifferenceInstancePanel();
	
	public MatrixExplorePanel(){
		addTab("Differences By Instance", byInstance);
//		addTab("Differences By Feature", byFeature);
		setPreferredSize(new Dimension(375,400));
		byFeature.setPreferredSize(new Dimension(350,350));
		byInstance.setPreferredSize(new Dimension(350,350));
		
		GenesisControl.addListenerToMap(byFeature, byFeature);
		GenesisControl.addListenerToMap(byInstance, byInstance);
		

	}
	
	public void refreshPanel(){
		byFeature.refreshPanel();
		byInstance.refreshPanel();
	}
	
	public DifferenceFeaturePanel getFeaturePanel(){
		return byFeature;
	}
	
	public DifferenceInstancePanel getInstancePanel(){
		return byInstance;
	}
}
