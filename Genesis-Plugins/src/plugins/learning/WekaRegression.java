package plugins.learning;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;

import weka.classifiers.functions.LinearRegression;
import weka.core.SelectedTag;

public class WekaRegression extends WekaCore{

	ButtonGroup attSel = new ButtonGroup();
	JRadioButton m5Att = new JRadioButton("M5 Method", true);
	JRadioButton greedyAtt = new JRadioButton("Greedy", false);
	JRadioButton noneAtt = new JRadioButton("Use All", false);
	
	JCheckBox elimColinear = new JCheckBox("Eliminate Colinear Attributes");
	
	Boolean elimBool = true;
	String attString = "m5";
	
	public WekaRegression(){
		super();
		numeric.removeAll();
		classifier = new LinearRegression();
		
		numeric.add("br left", new JLabel("Attribute Selection:"));
		numeric.add("br left", m5Att);
		numeric.add("br left", greedyAtt);
		numeric.add("br left", noneAtt);
		
		m5Att.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if(((JRadioButton)arg0.getSource()).isSelected()){
					attString = "m5";
				}
			}
		});
		
		greedyAtt.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				if(((JRadioButton)arg0.getSource()).isSelected()){
					attString = "greedy";
				}
			}
		});

		noneAtt.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				attString = "none";
			}
		});
		
		elimColinear.addItemListener(new ItemListener(){
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				elimBool = elimColinear.isSelected();
			}
		});

		elimColinear.setSelected(true);
		attSel.add(m5Att);
		attSel.add(greedyAtt);
		attSel.add(noneAtt);
		numeric.add("br br left", elimColinear);
		
		numeric.add("br left", new JLabel(new ImageIcon("toolkits/icons/error.png")));
		numeric.add("left", new JLabel("Warning:"));
		numeric.add("br left", new JLabel("Regressions scale poorly to many features."));

	}
	

	
	@Override
	public String getOutputName() {
		return "regression";
	}
	
	@Override
	public String toString(){
		return "Linear Regression";
	}
	
	@Override
	public Map<String, String> generateConfigurationSettings(){
		Map<String, String> config = new TreeMap<String, String>();
		
		Map<String, String> parent = super.generateConfigurationSettings();
		config.putAll(parent);

		config.put("attribute-selection", attString);
		config.put("colinear", ""+elimBool);
		return config;
	}
	
	@Override
	public void configureFromSettings(Map<String, String> settings){
		super.configureFromSettings(settings);
		elimBool = Boolean.TRUE.toString().equals(settings.get("colinear"));
		attString = settings.get("attribute-selection");
		classifier = new LinearRegression();
		LinearRegression linear = (LinearRegression)classifier;
		linear.setEliminateColinearAttributes(elimBool);
		if("m5".equals(attString)){
			linear.setAttributeSelectionMethod(new SelectedTag(LinearRegression.SELECTION_M5, LinearRegression.TAGS_SELECTION));
		}else if("greedy".equals(attString)){
			linear.setAttributeSelectionMethod(new SelectedTag(LinearRegression.SELECTION_GREEDY, LinearRegression.TAGS_SELECTION));			
		}else if("none".equals(attString)){
			linear.setAttributeSelectionMethod(new SelectedTag(LinearRegression.SELECTION_NONE, LinearRegression.TAGS_SELECTION));					
		}
	}
	

	public boolean supportsClassType(Feature.Type type)
	{
		return type == Type.NUMERIC;
	}
}
