package edu.cmu.side.util;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.AbstractTokenizer;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.LexedTokenFactory;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.sequences.SeqClassifierFlags;
import edu.stanford.nlp.ie.crf.CRFClassifier;

public class ChineseTokenizer<T extends HasWord> extends AbstractTokenizer<T>  {


	  private static final Properties segmenterOptions = new Properties();
	  private static final CRFClassifier<CoreLabel> segmenter;
	  
	  private final LexedTokenFactory<T> theTokenFactory;
	  private static SeqClassifierFlags flags;
	    
	  static {
	          segmenterOptions.setProperty("sighanCorporaDict", "toolkits/segmentation");
		  segmenterOptions.setProperty("serDictionary", "toolkits/segmentation/dict-chris6.ser.gz");
			    
		  segmenterOptions.setProperty("inputEncoding", "UTF-8");
		  segmenterOptions.setProperty("sighanPostProcessing", "true");
		  segmenterOptions.setProperty("kBest", "0");
		  flags = new SeqClassifierFlags(segmenterOptions);
		  segmenter = new CRFClassifier<CoreLabel>(flags);
		  segmenter.loadClassifierNoExceptions("toolkits/segmentation/ctb.gz", segmenterOptions);
	  	
	  }

	  public static ChineseTokenizer<CoreLabel> newChineseTokenizer(Reader r, LexedTokenFactory<CoreLabel> tokenFactory) {
	    return new ChineseTokenizer<>(r, tokenFactory);
	  }

	  
	  public ChineseTokenizer(Reader r, LexedTokenFactory<T> tokenFactory) {
		  theTokenFactory = tokenFactory;
		  objectBank = segmenter.makeObjectBankFromReader(new BufferedReader(r), segmenter.defaultReaderAndWriter()).iterator();
	  }


	  
	  /*
	   * Iterators for getNext to step through raw input Reader and output Chinese words
	   */
	  private Iterator<List<CoreLabel>> objectBank = null;
	  private Iterator<T> currentLine = new ArrayList<T>().iterator();
	  
	  /*
	   * (non-Javadoc)
	   * Get the next word in the line if there are more words
	   * Otherwise, get the next line, and clump the list of annotated characters into words (and output the first word)
	   * @see edu.stanford.nlp.process.AbstractTokenizer#getNext()
	   */
	  @Override
	  @SuppressWarnings("unchecked")
	  protected T getNext() {
		  T nextToken = null;
		  if (currentLine.hasNext()) {
			  nextToken = (T) currentLine.next();
		  } else {
			  if (objectBank.hasNext()) {
				  List<CoreLabel> classified = segmenter.classify(objectBank.next());
				  currentLine = makeSegmentTokens(classified).iterator();
				  nextToken = (T) currentLine.next();
			  }
		  } 
		  return nextToken;
	  }
	  
	 	    
	  /* 
	   * makeSegmentTokens(): 
	   * Turn a List<CoreLabel> of annotated Chinese characters into
	   * a List<CoreLabel> of Chinese words
	   */
	  private List<T> makeSegmentTokens(List<CoreLabel> characters) {
		  String word = "";
		  int wordstart = 0;
		  ArrayList<T> result = new ArrayList<T>();
		  for (CoreLabel cchar : characters) {
			  if (cchar.get(edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation.class).equals("1")) {
				  if (word.length() > 0) {
					  result.add(theTokenFactory.makeToken(word, wordstart, word.length()));
				  }
				  word = "";
				  wordstart = Integer.parseInt(cchar.get(edu.stanford.nlp.ling.CoreAnnotations.PositionAnnotation.class));
			  } 
		      word += cchar.get(edu.stanford.nlp.ling.CoreAnnotations.OriginalCharAnnotation.class);
		  }
		  if (word.length() > 0) {
			 result.add(theTokenFactory.makeToken(word, wordstart, word.length()));
		  }
		  return result;
	  }
	    
	  public static class ChineseTokenizerFactory<T extends HasWord> implements TokenizerFactory<T>, Serializable  {

	 
		private static final long serialVersionUID = 7931635565721110964L;
		protected final LexedTokenFactory<T> factory;
	    protected Properties lexerProperties = new Properties();

	    
	    public static TokenizerFactory<CoreLabel> newTokenizerFactory() {
	      return new ChineseTokenizerFactory<>(new CoreLabelTokenFactory(true));
	    }

	    ChineseTokenizerFactory(LexedTokenFactory<T> factory) {
	      this.factory = factory;
	    }

	    @Override
	    public Iterator<T> getIterator(Reader r) {
	      return getTokenizer(r);
	    }

	    @Override
	    public Tokenizer<T> getTokenizer(Reader r) {
	      return new ChineseTokenizer<>(r, factory);
	    }

	    /**
	     * options: A comma-separated list of options
	     */
	    @Override
	    public void setOptions(String options) {
	      String[] optionList = options.split(",");
	      for (String option : optionList) {
	        lexerProperties.put(option, "true");
	      }
	    }

	    @Override
	    public Tokenizer<T> getTokenizer(Reader r, String extraOptions) {
	      setOptions(extraOptions);
	      return getTokenizer(r);
	    }

	  } // end static class ChineseTokenizerFactory

	 

	  public static TokenizerFactory<CoreLabel> factory() {
	    TokenizerFactory<CoreLabel> tf = ChineseTokenizerFactory.newTokenizerFactory();
	    for (String option : segmenterOptions.stringPropertyNames()) {
	      tf.setOptions(option);
	    }
	    return tf;
	  }
}
