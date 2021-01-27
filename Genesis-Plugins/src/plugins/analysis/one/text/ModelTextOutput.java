package plugins.analysis.one.text;

import java.awt.Component;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import se.datadosen.component.RiverLayout;
import edu.cmu.side.control.ExploreResultsControl;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.plugin.EvaluateOneModelPlugin;

public class ModelTextOutput extends EvaluateOneModelPlugin{

	JPanel panel = new JPanel(new RiverLayout());

	protected JTextArea description = new JTextArea();
	protected JScrollPane describeScroll;
	
	public ModelTextOutput(){
		panel.add("left", new JLabel("Text Description:"));

//		describeScroll = new JScrollPane(description);
//		panel.add("br hfill vfill", describeScroll);

		panel.add("br hfill vfill", description);
	}
	
	@Override
	public String toString(){
		return "Model Output (Text)";
	}
	
	@Override
	public String getOutputName() {
		return "text";
	}

	@Override
	protected Component getConfigurationUIForSubclass() {
		if(ExploreResultsControl.hasHighlightedTrainedModelRecipe()){
			Recipe rec = ExploreResultsControl.getHighlightedTrainedModelRecipe();
			description.setText(rec.getTrainingResult().getLongDescriptionString());
		}else{
			description.setText("");
		}
		return panel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) {
		// TODO Auto-generated method stub
		
	}

}
