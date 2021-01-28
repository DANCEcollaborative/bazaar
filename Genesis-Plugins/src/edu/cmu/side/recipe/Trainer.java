package edu.cmu.side.recipe;


import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import plugins.features.BasicFeatures;
import plugins.features.CharacterNGrams;
import plugins.learning.WekaBayes;
import plugins.learning.WekaLogit;
import plugins.wrappers.FeatureSelection;
import weka.classifiers.functions.LibLINEAR;
import edu.cmu.side.model.Recipe;
import edu.cmu.side.model.RecipeManager.Stage;
import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature.Type;
import edu.cmu.side.plugin.FeaturePlugin;
import edu.cmu.side.plugin.LearningPlugin;
import edu.cmu.side.plugin.WrapperPlugin;
import edu.cmu.side.plugin.control.ImportController;
import edu.cmu.side.recipe.converters.ConverterControl;

public class Trainer extends Chef
{
	public static List<FeaturePlugin> getExtractors(Collection<String> featureKeys)
	{
		System.out.println("Feature Set: "+featureKeys);
		List<FeaturePlugin> featureExtractors = new ArrayList<FeaturePlugin>();
		
		BasicFeatures basics = new BasicFeatures();
		basics.setOption(BasicFeatures.TRACK_LOCAL, false);

		basics.setOption(BasicFeatures.TAG_UNIGRAM, featureKeys.contains("uni"));
		basics.setOption(BasicFeatures.TAG_BIGRAM, featureKeys.contains("bi"));
		basics.setOption(BasicFeatures.TAG_TRIGRAM, featureKeys.contains("tri"));
		
		if(featureKeys.contains("pos"))
		{
			basics.setOption(BasicFeatures.TAG_POS_BIGRAM, true);
			basics.setOption(BasicFeatures.TAG_POS_TRIGRAM, true);
		}
		else
		{
			basics.setOption(BasicFeatures.TAG_POS_BIGRAM, featureKeys.contains("pos2"));
			basics.setOption(BasicFeatures.TAG_POS_TRIGRAM, featureKeys.contains("pos3"));
		}

		featureExtractors.add(basics);

		if (featureKeys.contains("char"))
		{
			CharacterNGrams acrossGrams = new CharacterNGrams(4, 4, true, false);
			acrossGrams.setTrackHitLocation(false);
			featureExtractors.add(acrossGrams);
		}
		return featureExtractors;
	}

	public static LearningPlugin getLearner(String learnerKey)
	{
		Pattern logitPattern = Pattern.compile("logit(?:_(l[12]d?))?(?:_c(\\d+[.]?\\d+))?", Pattern.CASE_INSENSITIVE);
		Matcher logitMatcher = logitPattern.matcher(learnerKey);
		if(learnerKey.equals("bayes"))
		{
			return new WekaBayes();
		}
		else if(logitMatcher.find())
		{
			WekaLogit logit = new WekaLogit();
			Map<String, String> settings = new HashMap<String, String>();
			if(logitMatcher.group(1) != null)
				settings.put("reg", logitMatcher.group(1));

			logit.configureFromSettings(settings);
			
			if(logitMatcher.group(2) != null)
			{
				double cost = Double.parseDouble(logitMatcher.group(2));
				((LibLINEAR)logit.getClassifier()).setCost(cost);
				((LibLINEAR)logit.getClassifier()).setEps(0.0001);;
			}
			
			
			return logit;
		}
		else throw new IllegalArgumentException("Expected 'bayes' or 'logit_[l1|l2|l2d]_cN]', got "+learnerKey);
	}
	
	public static WrapperPlugin getFeatureSelectionWrapper(int threshold)
	{
		Map<String, String> settings = new HashMap<String, String>();
		settings.put("num", ""+threshold);
		settings.put("active", "true");
		
		FeatureSelection selector = new FeatureSelection();
		selector.configureFromSettings(settings);
		return selector;
	}
	
	public static Map<String, Serializable> getValidationSettings(DocumentList testDocs)
	{
		HashMap<String, Serializable> settings = new HashMap<String, Serializable>();

		settings.put("test", "true");
		settings.put("type", "CV");
		settings.put("source", "RANDOM");
		settings.put("foldMethod", "AUTO");
		settings.put("numFolds", "10");
		settings.put("testSet", testDocs);
		
		return settings;
	}

	public static void main(String[] args) throws Exception
	{
		System.out.println("./scripts/yoda.sh FEATURE_SET  LEARNER  FEATURE_SELECTION  LABEL_COLUMN  TEXT_COLUMN  ENCODING  path/to/training/data.csv path/to/saved/model.side.xml");
		System.out.println("Builds and evaluates a trained model");
		System.out.println("Examples:");
		System.out.println("./scripts/yoda.sh uni,bi,pos   logit_l2_c1.0  5000  score  essay_text   UTF-8  path/to/training/data.csv path/to/saved/model.side.xml");
		System.out.println("./scripts/yoda.sh tri,char     bayes          2000  label  text  windows-1252  path/to/training/data.csv path/to/saved/model.side.xml");

		String featureString = args[0];
		String learnerString = args[1];
		int featureThreshold = Integer.parseInt(args[2]);
		String labelString = args[3];
		String textColumnString = args[4];
		Charset charset = Charset.forName(args[5]);
		String csvPath = args[6];
		String outputPath = args[7];
		
		//setQuiet(false);
		
		HashSet<String> fileNames = new HashSet<String>();
		fileNames.add(csvPath);
		DocumentList docs = ImportController.makeDocumentList(fileNames, charset);
		
		docs.setCurrentAnnotation(labelString);
		
		HashSet<String> textColumns = new HashSet<String>();
		textColumns.add(textColumnString);
		docs.setTextColumns(textColumns);
		
		List<FeaturePlugin> features = getExtractors(Arrays.asList(featureString.split(",")));
		LearningPlugin learner = getLearner(learnerString);
		
		WrapperPlugin wrapper = getFeatureSelectionWrapper(featureThreshold);
		
		Recipe rootLeafSoup = Recipe.fetchRecipe();
		rootLeafSoup.setDocumentList(docs);
		for(FeaturePlugin extractor : features)
		{
			rootLeafSoup.addExtractor(extractor, extractor.generateConfigurationSettings());
		}
		rootLeafSoup.addWrapper(wrapper, wrapper.generateConfigurationSettings());
		rootLeafSoup.setLearner(learner);
		rootLeafSoup.setLearnerSettings(learner.generateConfigurationSettings());
		rootLeafSoup.setValidationSettings(getValidationSettings(docs));
		
		Trainer.simmerFeatures(rootLeafSoup, 5, labelString, Type.NOMINAL, Stage.TRAINED_MODEL);
		System.out.println(rootLeafSoup.getTrainingTable().getFeatureSet().size()+" features for text columns "+rootLeafSoup.getTextColumns()+" label '"+rootLeafSoup.getAnnotation()+"'");
		
		Trainer.broilModel(rootLeafSoup);
		Trainer.displayTrainingResults(rootLeafSoup);
		
		ConverterControl.writeToXML(outputPath, rootLeafSoup);	
		System.exit(0);
	}

}
