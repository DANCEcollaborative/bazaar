package plugins.learning;

import java.util.Enumeration;
import java.util.Map;


import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;

import weka.classifiers.functions.LibLINEAR;
import weka.core.SelectedTag;

public class WekaLogit extends WekaCore{

	ButtonGroup regularization = new ButtonGroup();
	JRadioButton radioL2 = new JRadioButton("L2 Regularization");
	JRadioButton radioL1 = new JRadioButton("L1 Regularization");
	JRadioButton radioL2dual = new JRadioButton("L2 Regularization (Dual)");

	public WekaLogit(String type){
		this();
		if("l2".equals(type)){
			radioL2.setSelected(true);
		}else if("l1".equals(type)){
			radioL1.setSelected(true);
		}else if("l2d".equals(type)){
			radioL2dual.setSelected(true);
		}
	}
	public WekaLogit(){
		nominal.removeAll();
		classifier = new LibLINEAR();
		regularization.add(radioL2);
		regularization.add(radioL1);
		regularization.add(radioL2dual);
		radioL2.setSelected(true);
		nominal.add("left", radioL2);
		nominal.add("br left", radioL1);
		nominal.add("br left", radioL2dual);
		((LibLINEAR)classifier).setEps(0.0001);
	}

	@Override
	public String getOutputName() {
		return "logit";
	}

	@Override
	public String toString(){
		return "Logistic Regression";
	}

	@Override
	public Map<String, String> generateConfigurationSettings() {
		Map<String, String> config = new TreeMap<String, String>();
		String reg = "";
		if(radioL2.isSelected()){
			reg = "l2";
		}else if(radioL1.isSelected()){
			reg = "l1";
		}else if(radioL2dual.isSelected()){
			reg = "l2d";
		}
		config.put("reg", reg);

		Map<String, String> parent = super.generateConfigurationSettings();
		config.putAll(parent);

		return config;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) 
	{
		super.configureFromSettings(settings);

		String reg = settings.get("reg");
		LibLINEAR linear = (LibLINEAR) classifier;
		try {
			linear.setOptions(new String[] {"convertNominalToBinary"});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//linear.setConvertNominalToBinary(true);
		if (reg.equals("l2"))
		{
			linear.setSVMType(new SelectedTag(0, LibLINEAR.TAGS_SVMTYPE));
			linear.setProbabilityEstimates(true);
			radioL2.setSelected(true);
		}
		else if (reg.equals("l1"))
		{
			linear.setSVMType(new SelectedTag(6, LibLINEAR.TAGS_SVMTYPE));
			linear.setProbabilityEstimates(false);
			radioL1.setSelected(true);
		}
		else if (reg.equals("l2d"))
		{
			linear.setSVMType(new SelectedTag(7, LibLINEAR.TAGS_SVMTYPE));
			linear.setProbabilityEstimates(false);
			radioL2dual.setSelected(true);
		}
	}
	

	public boolean supportsClassType(Feature.Type type)
	{
		return type == Type.NOMINAL;
	}

}