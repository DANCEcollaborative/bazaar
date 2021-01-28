package plugins.learning;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.SMOreg;
import weka.classifiers.functions.supportVector.PolyKernel;
import weka.core.SelectedTag;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.Feature.Type;

public class WekaSVM extends WekaCore{

	ButtonGroup nominalModels = new ButtonGroup();
	JRadioButton radioLibLin = new JRadioButton("LibLINEAR");
	JRadioButton radioSMO = new JRadioButton("Sequential Minimal Optimization");
	ButtonGroup numericModels = new ButtonGroup();
	JRadioButton radioRegression = new JRadioButton("Sequential Minimal Optimization (Regression)");
	JTextField smoExponent = new JTextField(2);
	JCheckBox normalizeBox = new JCheckBox("Normalize");
	String type = "liblinear";
	boolean normalize = true;
	
	public WekaSVM(){
		super();

		classifier = new LibLINEAR();
		((LibLINEAR) classifier).setSVMType(new SelectedTag(1, LibLINEAR.TAGS_SVMTYPE));
		
		normalizeBox.setToolTipText("Normalize the inputs to the SVM, for comparable feature weights");
		normalizeBox.setSelected(true);
		
		nominal.removeAll();
		panel = new JPanel(new BorderLayout());
		nominalModels.add(radioLibLin);
		nominalModels.add(radioSMO);
		nominal.add("left", new JLabel("Settings for Nominal Class Values:"));
		nominal.add("br left", normalizeBox);
		nominal.add("br left", radioLibLin);
		nominal.add("br left", radioSMO);
		JLabel poly = new JLabel("Exponent:");
		poly.setBorder(new EmptyBorder(0,30,0,0));
		nominal.add("br left", poly);
		smoExponent.setText("1");
		nominal.add("left", smoExponent);
		radioLibLin.setSelected(true);
		
		numeric.removeAll();
		radioRegression.setSelected(true);
		numeric.add("left", new JLabel("Settings for Numeric Class Values:"));
		numeric.add("br left", radioRegression);
		
		
		normalizeBox.addActionListener(new ActionListener()
		{
			
			@Override
			public void actionPerformed(ActionEvent e)
			{
				normalize = normalizeBox.isSelected();
			}
		});
		
		ActionListener checkSVMTypeListener = new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				setSVMTypeFromUI();
			}
		};
		
		radioLibLin.addActionListener(checkSVMTypeListener);
		radioSMO.addActionListener(checkSVMTypeListener);
		radioRegression.addActionListener(checkSVMTypeListener);
		
	}
	
	@Override
	public String getOutputName(){
		return "svm";
	}
	
	@Override
	public String toString(){
		return "Support Vector Machines";
	}

	@Override
	protected Component getConfigurationUIForSubclass() 
	{
		Component c = super.getConfigurationUIForSubclass();
		setSVMTypeFromUI();
		return c;
	}

	protected void setSVMTypeFromUI()
	{
		if(visibleType == null)
			visibleType = Type.NOMINAL;
		switch(visibleType)
		{
			case NOMINAL:
			case BOOLEAN:
			case STRING:
				if(radioLibLin.isSelected()){
					type = "liblinear";
				}else if(radioSMO.isSelected()){
					type = "smo";
				}
				break;
			case NUMERIC:
				type = "smoreg";
				break;
			
		}
	}
	
	@Override
	public Map<String, String> generateConfigurationSettings() {
		Map<String, String> config = new TreeMap<String, String>();
		if(type == "smo")
				config.put("exponent", smoExponent.getText());
		config.put("type",type);
		
		if(normalize)
			config.put("normalize", "true");
		
		Map<String, String> parent = super.generateConfigurationSettings();
		config.putAll(parent);
		
		return config;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings) {
		
		super.configureFromSettings(settings);
		
		type = settings.get("type");
		if (type.equals("liblinear"))
		{
			classifier = new LibLINEAR();
			LibLINEAR linear = (LibLINEAR) classifier;
			linear.setSVMType(new SelectedTag(1, LibLINEAR.TAGS_SVMTYPE));
			if(settings.containsKey("normalize"))
			{
				linear.setNormalize(true);
			}
		}
		else if (type.equals("smo"))
		{
			classifier = new SMO();
			SMO smo = (SMO) classifier;
			double exp = 1.0;
			try{
				exp = Double.parseDouble(settings.get("exponent"));
			}catch(Exception e){}
			((PolyKernel)(smo.getKernel())).setExponent(exp);
			
			if(settings.containsKey("normalize"))
			{
				smo.setFilterType(new SelectedTag(SMO.FILTER_NORMALIZE, SMO.TAGS_FILTER));
			}
			else
			{
				smo.setFilterType(new SelectedTag(SMO.FILTER_NONE, SMO.TAGS_FILTER));
			}
		}
		else if(type.equals("smoreg")){
			classifier = new SMOreg();
			SMOreg reg = (SMOreg)classifier;
			if(settings.containsKey("normalize"))
			{
				reg.setFilterType(new SelectedTag(SMOreg.FILTER_NORMALIZE, SMOreg.TAGS_FILTER));
			}
			else
			{
				reg.setFilterType(new SelectedTag(SMOreg.FILTER_NONE, SMOreg.TAGS_FILTER));
			}
		}
		
		
	}


	public boolean supportsClassType(Feature.Type type)
	{
		return true;
	}

}
