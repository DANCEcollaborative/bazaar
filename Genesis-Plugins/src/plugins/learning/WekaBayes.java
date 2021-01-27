package plugins.learning;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JCheckBox;

import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;

import weka.classifiers.bayes.NaiveBayes;

public class WekaBayes extends WekaCore{

	protected Boolean kernelEstimator = false;
	protected Boolean supervisedDiscretization = false;
	
	public WekaBayes(){
		super();
		nominal.removeAll();
		classifier = new NaiveBayes();
		JCheckBox kernelBox = new JCheckBox("Use Kernel Estimator");
		kernelBox.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				kernelEstimator = ((JCheckBox)arg0.getSource()).isSelected();
			}
			
		});

		JCheckBox discreteBox = new JCheckBox("Use Supervised Discretization");
		discreteBox.addItemListener(new ItemListener(){

			@Override
			public void itemStateChanged(ItemEvent arg0) {
				supervisedDiscretization = ((JCheckBox)arg0.getSource()).isSelected();
			}
			
		});
		nominal.add("left", kernelBox);
		nominal.add("br left", discreteBox);
	}
	
	@Override
	public String getOutputName() {
		return "bayes";
	}
	
	@Override
	public String toString(){
		return "Naive Bayes";
	}


	@Override
	public Map<String, String> generateConfigurationSettings() {
		Map<String, String> config = new TreeMap<String, String>();
		
		Map<String, String> parent = super.generateConfigurationSettings();
		config.putAll(parent);
		
		config.put("nb-kernel", kernelEstimator.toString());
		config.put("nb-discretize", supervisedDiscretization.toString());
		return config;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) {

		super.configureFromSettings(settings);	
		kernelEstimator = Boolean.TRUE.toString().equals(settings.get("nb-kernel"));
		supervisedDiscretization = Boolean.TRUE.toString().equals(settings.get("nb-discretize"));
		((NaiveBayes)classifier).setUseKernelEstimator(kernelEstimator);
		((NaiveBayes)classifier).setUseSupervisedDiscretization(supervisedDiscretization);
	}
	

	public boolean supportsClassType(Feature.Type type)
	{
		return type == Type.NOMINAL;
	}

}
