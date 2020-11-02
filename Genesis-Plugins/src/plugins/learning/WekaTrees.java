package plugins.learning;

import java.util.Map;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import edu.cmu.side.model.feature.Feature;

import weka.classifiers.trees.J48;
import weka.classifiers.trees.M5P;

public class WekaTrees extends WekaCore{

	JCheckBox splits = new JCheckBox("Binary Splits", false);
	JTextField confText = new JTextField(3);
	JCheckBox prune = new JCheckBox("Prune Tree", true);
	JTextField minObjText = new JTextField(3);
	
	public WekaTrees(){
		super();
		
		classifier = new J48();
		
		nominal.removeAll();
		nominal.add("left", new JLabel("Training with J48:"));
		nominal.add("br left", splits);
		confText.setText("0.25");
		nominal.add("br left", confText);
		nominal.add("left", new JLabel("Confidence Factor"));
		nominal.add("br left", prune);
		minObjText.setText("2");
		nominal.add("br left", minObjText);
		nominal.add("br left", new JLabel("Minimum Objects in Leaves"));
		
		numeric.removeAll();
		numeric.add("left", new JLabel("Training with M5P"));
		
		
	}
	
	@Override
	public String getOutputName(){
		return "trees";
	}
	
	@Override
	public String toString(){
		return "Decision Trees";
	}
	
	@Override
	public Map<String, String> generateConfigurationSettings() {
		Map<String, String> config = new TreeMap<String, String>();
		
		Map<String, String> parent = super.generateConfigurationSettings();
		config.putAll(parent);
		
		switch(visibleType){
		case NOMINAL:
		case BOOLEAN:
		case STRING:
			config.put("type", "j48");
			break;
		case NUMERIC:
			config.put("type", "m5p");
		}
		config.put("j48-split", ""+splits.isSelected());
		config.put("j48-conf", confText.getText());
		config.put("j48-prune", ""+prune.isSelected());
		config.put("j48-objects", minObjText.getText());
		
		return config;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings){
		super.configureFromSettings(settings);
		if(settings.get("type").equals("j48")){
			classifier = new J48();
			Boolean binSplits = settings.containsKey("j48-split") && settings.get("j48-split").equals(Boolean.TRUE.toString());
			((J48)classifier).setBinarySplits(binSplits);
			Double conf = 0.25;
			try{
				conf = Double.parseDouble(settings.get("j48-conf"));
			}catch(Exception e){}
			((J48)classifier).setConfidenceFactor(conf.floatValue());
			Boolean prune = settings.containsKey("j48-prune") && settings.get("j48-prune").equals(Boolean.TRUE.toString());
			((J48)classifier).setUnpruned(!prune);
			Integer minObj = 2;
			try{
				minObj = Integer.parseInt(settings.get("j48-objects"));
			}catch(Exception e){}
			((J48)classifier).setMinNumObj(minObj);			
		}else if(settings.get("type").equals("m5p")){
			classifier = new M5P();
		}

	}

	public boolean supportsClassType(Feature.Type type)
	{
		return true;
	}
}
