package plugins.features;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.cmu.side.model.data.DocumentList;
import edu.cmu.side.model.feature.Feature;
import edu.cmu.side.model.feature.FeatureHit;
import edu.cmu.side.model.feature.LocalFeatureHit;
import edu.cmu.side.plugin.ParallelFeaturePlugin;
import edu.cmu.side.util.TokenizingToolLanguage;
import edu.cmu.side.util.TokenizingTools;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasOffset;
import edu.stanford.nlp.ling.HasTag;
import edu.stanford.nlp.ling.HasWord;

/**
 * This class handles all of the most common features that we see people asking for, including features based around n-grams and parts of speech.
 * It is fairly rare, actually, that anyone needs an extractor other than this one.
 * 
 * Note that this is also some of the more complex code that we have, simply because of how many different things this class is aggregating.
 * Each of the selections in the UI are attached to both a tag and a checkbox, as well as a map of selections for which ones to use at any given time.
 * 
 * New instances of BasicFeatures are set to binary unigrams, and can be modified from there.
 * 
 * @author lightsidelabs
 *
 */
public class BasicFeatures extends ParallelFeaturePlugin
{

	/**
	 * Tags for XML.
	 */
	public static final String TAG_UNIGRAM = "Unigrams";
	public static final String TAG_BIGRAM = "Bigrams";
	public static final String TAG_TRIGRAM = "Trigrams";
	public static final String TAG_POS_BIGRAM = "POS Bigrams";
	public static final String TAG_POS_TRIGRAM = "POS Trigrams";
	public static final String TAG_WORD_POS_PAIR = "Word/POS Pairs";
	public static final String TAG_NUMERIC = "Count Occurences";
	public static final String TAG_PUNCT = "Include Punctuation";
	public static final String TAG_LINE_LENGTH = "Line Length";
	public static final String TAG_CONTAINS_NON_STOP = "Contains Non-Stopwords";
	public static final String TAG_REMOVE_STOP = "Ignore All-stopword N-Grams";
	public static final String TAG_STEM = "Stem N-Grams";
	public static final String TRACK_LOCAL = "Track Feature Hit Location";
	public static final String SKIP_STOPWORDS = "Skip Stopwords in N-Grams";
	public static final String TAG_NORMALIZED = "Normalize N-Gram Counts";
	// public static final String TAG_DIFFERENTIATE =
	// "Differentiate text columns?";

	public static final String SEPARATOR = "line_separator";

	protected static enum NGramType {TOKENS, POS_TAGS, WORD_POS_PAIRS};

	protected BasicFeaturesPanel panel;

	protected Map<String, Boolean> selections = new TreeMap<String, Boolean>();
	protected Map<String, String> punctuationMap = new TreeMap<String, String>();
	protected Map<String, String> reversePunctuationMap = new TreeMap<String, String>();
	protected Set<String> stopWordsSet = new TreeSet<String>();

	protected String stopwordsFilename = null;//"toolkits/english.stp";
	protected String punctuationFilename = null; //"toolkits/punctuation.stp";
	/**
	 * for memory management, it may be desirable to save a few bytes by leaving out the location information
	 */

	public BasicFeatures()
	{
		List<String> checkboxes = new ArrayList<String>();
		checkboxes.add(TAG_UNIGRAM);
		selections.put(TAG_UNIGRAM, true);
		checkboxes.add(TAG_BIGRAM);
		selections.put(TAG_BIGRAM, false);
		checkboxes.add(TAG_TRIGRAM);
		selections.put(TAG_TRIGRAM, false);
		checkboxes.add(TAG_POS_BIGRAM);
		selections.put(TAG_POS_BIGRAM, false);
		checkboxes.add(TAG_POS_TRIGRAM);
		selections.put(TAG_POS_TRIGRAM, false);
		checkboxes.add(TAG_WORD_POS_PAIR);
		selections.put(TAG_WORD_POS_PAIR, false);
		checkboxes.add(TAG_LINE_LENGTH);
		selections.put(TAG_LINE_LENGTH, false);
		checkboxes.add(SEPARATOR);
		checkboxes.add(TAG_NUMERIC);
		selections.put(TAG_NUMERIC, false);
		checkboxes.add(TAG_NORMALIZED);
		selections.put(TAG_NORMALIZED, false);
		checkboxes.add(TAG_PUNCT);
		selections.put(TAG_PUNCT, true);
		checkboxes.add(TAG_STEM);
		selections.put(TAG_STEM, false);
		checkboxes.add(SKIP_STOPWORDS);
		selections.put(SKIP_STOPWORDS, false);
		checkboxes.add(TAG_REMOVE_STOP);
		selections.put(TAG_REMOVE_STOP, false);
		checkboxes.add(TAG_CONTAINS_NON_STOP);
		selections.put(TAG_CONTAINS_NON_STOP, false);
		checkboxes.add(TRACK_LOCAL);
		selections.put(TRACK_LOCAL, true);

		loadStopWords(TokenizingTools.getPunctuationFilename(), TokenizingTools.getStopwordsFilename());
		panel = new BasicFeaturesPanel(this, checkboxes);
	}
	
	public void loadStopWords(String punctuationFilename, String stopwordsFilename) {
		if (this.stopwordsFilename != null && this.stopwordsFilename.equals(stopwordsFilename)) {
			return;
		}
		this.stopwordsFilename = stopwordsFilename;
		this.punctuationFilename = punctuationFilename;
		try
		{
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(punctuationFilename), Charset.forName("UTF-8")));
			String line;
			punctuationMap.clear();
			reversePunctuationMap.clear();
			while ((line = in.readLine()) != null)
			{
				String[] split = line.split("\\s+");
				if (split.length > 1)
				{
					punctuationMap.put(split[0], split[1]);
					reversePunctuationMap.put(split[1], split[0]);
				}
			}
			in.close();
			in = new BufferedReader(new InputStreamReader(new FileInputStream(stopwordsFilename), Charset.forName("UTF-8")));
			stopWordsSet.clear();
			while ((line = in.readLine()) != null)
			{
				stopWordsSet.add(line);
			}
			in.close();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
	}

	public boolean getOption(String tag)
	{
		return selections.containsKey(tag) ? selections.get(tag) : false;
	}

	public void setOption(String tag, boolean value)
	{
		if (selections.containsKey(tag))
		{
			selections.put(tag, value);
		}
	}

	public void setAllOptionsFalse()
	{
		for (String key : selections.keySet())
		{
			setOption(key, false);
		}
	}


	/*@Override
	public Collection<FeatureHit> extractFeatureHitsForSubclass(DocumentList documents, StatusUpdater update)
	{

		update.update("Extracting Basic Features", 1, documents.getSize());
		Map<String, List<String>> text = documents.getCoveredTextList();
		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();

		int size = documents.getSize();
		for (int i = 0; i < size; i++)
		{

			if (halt)
			{
				break;
			}
			if((i+1)%50 == 0 || i == size-1)
				update.update("Extracting Basic Features", i+1, documents.getSize());
			
			localHitCache.clear();
			for (String col : text.keySet())
			{
				extractFeaturesOnce(hits, col, text.get(col).get(i), i, documents);
			}
		}
		update.update("Finishing Basic Feature Extraction...");
		return hits;
	}*/

	public <T  extends HasWord & HasTag & HasOffset> void extractFeaturesOnce(Collection<FeatureHit> hits, String column, String text, int docID, DocumentList documentList, Map<Feature, FeatureHit> localHitCache)
	{

		boolean binary = !selections.get(TAG_NUMERIC);
		boolean stem = selections.get(TAG_STEM);
		boolean removeStopwords = selections.get(TAG_REMOVE_STOP);
		boolean skipStopwords = selections.get(SKIP_STOPWORDS);
		boolean punct = selections.get(TAG_PUNCT);
		boolean local = selections.get(TRACK_LOCAL);

		loadStopWords(TokenizingTools.getPunctuationFilename(), TokenizingTools.getStopwordsFilename());
		List<T> tokens = tokenize(text, selections.get(TAG_POS_BIGRAM) || selections.get(TAG_POS_TRIGRAM) || selections.get(TAG_WORD_POS_PAIR));
		
		
		if(tokens.isEmpty())
		{
			logger.warning("Document "+(docID+1)+" has a blank text field in column "+column);
			return ;
		}

		
		if (selections.get(TAG_LINE_LENGTH))
		{
			Feature f = Feature.fetchFeature(getOutputName(), column + "NUM_TOKENS", Feature.Type.NUMERIC, this);
			hits.add(new FeatureHit(f, new Double(tokens.size()), docID));
		}
		
		
		if (selections.get(TAG_UNIGRAM) || selections.get(TAG_CONTAINS_NON_STOP))
		{
			Collection<FeatureHit> localHits = aggregateNGrams(tokens, column, binary, 1, removeStopwords,  skipStopwords, stem, punct, local, NGramType.TOKENS, docID, documentList, localHitCache);

			if (selections.get(TAG_CONTAINS_NON_STOP))
			{
				Feature f = Feature.fetchFeature(getOutputName(), documentList.getTextFeatureName("CONTAINS_NONSTOP", column), Feature.Type.BOOLEAN, this);
				boolean contains = false;
				for (FeatureHit hit : localHits)
				{
					String name = hit.getFeature().getFeatureName();
					if (!stopWordsSet.contains(name) && !reversePunctuationMap.containsKey(name))
					{
						contains = true;
						break;
					}
				}
				if (contains)
				{
					hits.add(new FeatureHit(f, Boolean.TRUE, docID));
				}
			}

			if (selections.get(TAG_UNIGRAM))
			{
				hits.addAll(localHits);
			}
		}

		if (selections.get(TAG_BIGRAM))
		{
			Collection<FeatureHit> localHits = aggregateNGrams(tokens, column, binary, 2, removeStopwords,  skipStopwords, stem, punct, local, NGramType.TOKENS, docID, documentList,localHitCache);
			hits.addAll(localHits);
		}
		if (selections.get(TAG_TRIGRAM))
		{
			Collection<FeatureHit> localHits = aggregateNGrams(tokens, column, binary, 3, removeStopwords, skipStopwords, stem, punct, local, NGramType.TOKENS, docID, documentList, localHitCache);
			hits.addAll(localHits);
		}
		if (selections.get(TAG_POS_BIGRAM))
		{
			hits.addAll(aggregateNGrams(tokens, column, binary, local, NGramType.POS_TAGS, 2, docID, documentList, localHitCache));
		}
		if (selections.get(TAG_POS_TRIGRAM))
		{
			hits.addAll(aggregateNGrams(tokens, column, binary, local, NGramType.POS_TAGS, 3, docID, documentList, localHitCache));
		}
		if (selections.get(TAG_WORD_POS_PAIR))
		{
			hits.addAll(aggregateNGrams(tokens, column, binary, 1, removeStopwords,  skipStopwords, stem, punct, local, NGramType.WORD_POS_PAIRS, docID, documentList, localHitCache));
		}

	}

	protected  <T  extends HasWord & HasTag & HasOffset> List<T> tokenize(String text, boolean includePOS)
	{
		List<CoreLabel> tokens;
		tokens = TokenizingTools.tokenizeInvertible(text);
		
		if (includePOS)
		{
			tokens = TokenizingTools.tagInvertible(tokens);
		}
		
		return (List<T>) tokens;
	}

	public <T  extends HasWord & HasTag & HasOffset> Collection<FeatureHit> aggregateNGrams(List<T> tokens, String column, boolean binary, boolean local, NGramType type, int n, int docID, DocumentList documentList,Map<Feature, FeatureHit> localHitCache)
	{
		return aggregateNGrams(tokens, column, binary, n, false, false, false, true, local, type, docID, documentList,localHitCache);
	}

	public <T  extends HasWord & HasTag & HasOffset> Collection<FeatureHit> aggregateNGrams(List<T> tokens, String column, boolean binary, int n, boolean ignoreTotallyStopwordSequences, boolean skipStopwords, boolean stem, boolean includePunctuation, boolean local, NGramType type,
			int docID, DocumentList documentList,Map<Feature, FeatureHit> localHitCache)
	{
		
		Collection<FeatureHit> localHits = new ArrayList<FeatureHit>(tokens.size());
		StringBuilder ngramStringBuilder = new StringBuilder();

		boolean[] isPunct = new boolean[n]; //isPunct[i] is true if the ith part of the current n-gram is punctuation
		int added = 0; //the current length of the current n-gram.
		int tokensConsumed = 0; //number of tokens "eaten" altogether for this n-gram. 
		boolean notAllStop = false; //true if some of the tokens are not stopwords.
		String tokenString; //the string representation of a single unit (tag or token or pair) within an n-gram.
		
		double numericValue = selections.get(TAG_NORMALIZED) ? 1.0/tokens.size() :1.0;
		for (int i = -1; i < tokens.size(); i++)
		{
			//reset for new ngram starting at i
			added = 0;
			tokensConsumed = 0; 
			notAllStop = false;
			ngramStringBuilder.setLength(0);
			
			if(i < 0) //prefix n-grams with beginning-of-line.
			{
				if(n > 1)
					ngramStringBuilder.append("BOL");
				
				tokensConsumed++;
				added++;
			}
			
			while (added < n && (i + tokensConsumed) < tokens.size())
			{
				//get the next unseen token for this n-gram
				T token = tokens.get(i + tokensConsumed);

				String word = token.word().toLowerCase();
				
				if(punctuationMap.containsKey(word))
				{
					word = punctuationMap.get(word);
					isPunct[added] = true;
				}
				else
				{
					isPunct[added] = false;
				}
				
				// TODO: Introduce stemmers for other languages; for now just ignore this option
				if(stem && !isPunct[added] && type== NGramType.TOKENS && TokenizingTools.getLanguage() == TokenizingToolLanguage.ENGLISH)
				{
					word = EnglishStemmer.stem(word);
				}
				
				switch(type)
				{
					case POS_TAGS:
						tokenString = token.tag();

						if(word.equals("newturn") || word.equals("otherturn") || word.equals("tutorturn"))
							tokenString = "|EOT|";
						
						break;
					case WORD_POS_PAIRS:
						tokenString = word+" / "+token.tag();
						break;
					default:
						tokenString = word;
				}
				
				if (includePunctuation || !isPunct[added])
				{
					
					if(!skipStopwords || !stopWordsSet.contains(word))
					{
						//add this token to the n-gram.
						if(added > 0 && added < n)
							ngramStringBuilder.append("_");
						ngramStringBuilder.append(tokenString);
						added++;
					}
					
					if (ignoreTotallyStopwordSequences && !stopWordsSet.contains(word)) //catch n-grams containing stopwords
					{
						notAllStop = true; 
					}
				}
				else if(added == 0) //ignore this starting point if we didn't add anything that starts here.
				{
					break;
				}
				tokensConsumed++;
			}
			
			if(added < n && i+tokensConsumed >= tokens.size()) //tag EOL on to fill the gap
			{
				ngramStringBuilder.append("_EOL");
				added++;
			}
			
			if (added == n && (!ignoreTotallyStopwordSequences || notAllStop)) //turn the ngram into a feature hit.
			{
				String key = ngramStringBuilder.toString();
				if (key.length() > 0)
				{

					Feature.Type featureType = binary? Feature.Type.BOOLEAN : Feature.Type.NUMERIC;
					Feature f = Feature.fetchFeature(getOutputName(), documentList.getTextFeatureName(key, column), featureType, this);
		

					int beginPosition = tokens.get(i<0?0:i).beginPosition();
					int endPosition = tokens.get(i + tokensConsumed - 1).endPosition();
					
					
					
					if (!localHitCache.containsKey(f)) //brand-new feature for this document
					{
						FeatureHit hit;
						if(local)
							hit = new LocalFeatureHit(f, binary?true:numericValue, docID, column, beginPosition, endPosition);
						else
							hit = new FeatureHit(f, binary?true:numericValue, docID);
						localHits.add(hit);
						localHitCache.put(f, hit);
					}
					else //already occurred in this document.
					{
						if(local) //record the location of this hit.
						{
							((LocalFeatureHit) localHitCache.get(f)).addHit(column, beginPosition, endPosition);
						}
						
						if(!binary) //increment the numeric feature value
						{
							localHitCache.get(f).setValue(((Double)localHitCache.get(f).getValue())+numericValue);
						}
					}
				}
			}
		}
		//integrated numericization.
		return localHits;
		//return finalizeHits(localHits, binary, docID);
	}

	public Collection<FeatureHit> finalizeHits(Collection<FeatureHit> local, boolean binary, int docID)
	{
		if (binary)
		{
			return local;
		}
		else //TODO: make this more efficient than a full copy
		{
			Collection<FeatureHit> result = new HashSet<FeatureHit>();
			for (FeatureHit h : local)
			{
				LocalFeatureHit hit = (LocalFeatureHit) h;
				Feature f = hit.getFeature();
				Feature newF = Feature.fetchFeature(f.getExtractorPrefix(), f.getFeatureName(), Feature.Type.NUMERIC, this);

				FeatureHit newHit = new LocalFeatureHit(newF, new Double(hit.getHits().size()), docID, hit.getHits());
				result.add(newHit);
			}

			return result;
		}
	}

	//used to generate a proposed feature table name from the current settings - should be fairly short but descriptive of major settings
	@Override
	public String getDefaultName()
	{
		String name = "";

		if(selections.get(TAG_UNIGRAM))
				name += "1";
		if(selections.get(TAG_BIGRAM))
				name += "2";
		if(selections.get(TAG_TRIGRAM))
			name += "3";
		if(selections.get(TAG_POS_BIGRAM) || selections.get(TAG_POS_TRIGRAM))
				name += "POS";
		if(!name.isEmpty())
			name+="grams";
		
		if(selections.get(TAG_WORD_POS_PAIR))
		{
			if(name.isEmpty())
				name = "pairs";
			else
				name += "_pairs";
		}
		if(selections.get(TAG_LINE_LENGTH))
		{
			if(name.isEmpty())
				name = "length";
			else
				name += "_length";
		}
		
		if(selections.get(TAG_NUMERIC))
		{
			if(selections.get(TAG_NORMALIZED))
				name += "_norm";
			else
				name += "_count";
		}
		if(selections.get(TAG_REMOVE_STOP))
		{
				name += "_nostop";
		}
		else if(selections.get(TAG_REMOVE_STOP))
		{
				name += "_skip";
		}
		if(!selections.get(TAG_PUNCT))
		{
				name += "_nopunct";
		}
		
		if(name.isEmpty())
		{
			name = "basic";
		}
		
		
		return name;
	}
	
	//used in various places in the workbench as a key to this plugin.
	@Override
	public String getOutputName()
	{
		return "basic";
	}

	//displayed to the user as the name of this plugin.
	@Override
	public String toString()
	{
		return "Basic Features";
	}

	@Override
	protected Component getConfigurationUIForSubclass()
	{
		return panel;
	}

	@Override
	public Map<String, String> generateConfigurationSettings()
	{
		Map<String, String> settings = new TreeMap<String, String>();
		for (String key : selections.keySet())
		{
			settings.put(key, selections.get(key).toString());
		}
		return settings;
	}

	@Override
	public void configureFromSettings(Map<String, String> settings)
	{
		setAllOptionsFalse();
		for (String key : settings.keySet())
		{
			boolean selected = settings.get(key).equals(Boolean.TRUE.toString());
			setOption(key, selected);
			// if(key.contains("POS") && selected){
			// try{
			// tagger = new
			// MaxentTagger("toolkits/maxent/left3words-wsj-0-18.tagger");
			// }catch(Exception e){
			// e.printStackTrace();
			// }
			// }
		}
		
	}

	@Override
	public boolean isTokenized(Feature f)
	{
		return false;
	}

	@Override
	public Collection<FeatureHit> extractFeatureHitsFromDocument(DocumentList documents, int i)
	{
		Map<String, List<String>> text = documents.getCoveredTextList();
		Collection<FeatureHit> hits = new TreeSet<FeatureHit>();
		loadStopWords(TokenizingTools.getPunctuationFilename(), TokenizingTools.getStopwordsFilename());
		

		Map<Feature, FeatureHit> localHitCache = new HashMap<Feature, FeatureHit>();
		for (String col : text.keySet())
		{
			extractFeaturesOnce(hits, col, text.get(col).get(i), i, documents, localHitCache);
		}
		
		return hits;
		
	}

	// @Override
	// public List<String> tokenize(Feature f, String s)
	// {
	// return TokenizingTools.tokenize(s);
	// }

}