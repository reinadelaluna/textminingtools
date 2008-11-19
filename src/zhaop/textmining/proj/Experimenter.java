package zhaop.textmining.proj;

import java.util.Hashtable;

import weka.attributeSelection.BestFirst;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.rules.ZeroR;

import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.StringToWordVector;
import weka.core.SelectedTag;
import weka.core.Utils;

public class Experimenter {

  static Hashtable<String, Filter> ms_Filters = new Hashtable<String, Filter>();
  static Hashtable<String, Classifier> ms_Classifiers = new Hashtable<String, Classifier>();

  public Experimenter() {
    fill_Filters();
    fill_Classifiers();
  }

  private void fill_Filters() {
    ms_Filters
        .put("StringToWordVectorFilter", createStringToWordVectorFilter());
    ms_Filters
        .put("MyStringToVectorFilter", createMyStringToWordVectorFilter());
    ms_Filters.put("TFIDFWordVectorFilter",
        createTFIDFStringToWordVectorFilter());
    ms_Filters.put("TFWordVectorFilter", createTFIDFStringToWordVectorFilter());
    ms_Filters.put("WCWordVectorFilter",
        createWordCountStringToWordVectorFilter());
  }

  private void fill_Classifiers() {
    ms_Classifiers.put("NaiveBayes", new NaiveBayes());
    ms_Classifiers.put("SVM", new SMO());
    ms_Classifiers.put("ZeroR", new ZeroR());

  }

  private static Filter createStringToWordVectorFilter() {
    StringToWordVector filter = new StringToWordVector();

    filter.setTokenizer(new weka.core.tokenizers.WordTokenizer());
    filter.setLowerCaseTokens(true);

    // filter.setUseStoplist(false);
    filter.setStopwords(Util.STOPWORDS_FILE);
    filter.setStemmer(new weka.core.stemmers.SnowballStemmer());
    // System.out.println("Stop words: " + filter.getStopwords().getName());

    return filter;
  }

  private static Filter createMyStringToWordVectorFilter() {
    MyStringToWordVector filter = new MyStringToWordVector();
    filter.setTokenizer(new weka.core.tokenizers.WordTokenizer());
    filter.setLowerCaseTokens(true);
    // filter.setUseStoplist(false);
    filter.setStopwords(Util.STOPWORDS_FILE);
    filter.setStemmer(new weka.core.stemmers.SnowballStemmer());
    // System.out.println("Stop words: " + filter.getStopwords().getName());

    return filter;
  }

  private static Filter createWordCountStringToWordVectorFilter() {
    MyStringToWordVector filter = (MyStringToWordVector) createMyStringToWordVectorFilter();
    filter.setOutputWordCounts(true);
    // filter.setWordsToKeep(1000000);
    return filter;
  }

  private Filter createTFIDFStringToWordVectorFilter() {
    MyStringToWordVector filter = (MyStringToWordVector) createWordCountStringToWordVectorFilter();
    filter.setOutputWordCounts(true);
    filter.setIDFTransform(true);
    filter.setTFTransform(false);
    return filter;
  }

  private Filter createTFStringToWordVectorFilter() {
    MyStringToWordVector filter = (MyStringToWordVector) createWordCountStringToWordVectorFilter();
    filter.setOutputWordCounts(true);
    filter.setIDFTransform(false);
    filter.setTFTransform(true);
    return filter;
  }

  public static void main(String[] options) throws Exception {

    Experimenter exp = new Experimenter();
    exp.doMain(options);

  }

  private void doMain(String[] options) throws Exception {
    TextDirectoryClassifier tool = new TextDirectoryClassifier();

    // load Training data
    String dataDir = Utils.getOption('d', options);
    tool.loadTrainingDataFrom(dataDir);

    // load Test data
    String testDir = Utils.getOption('t', options);
    
    tool.loadTestDataFrom(testDir);

    // load ArffFolder
    String arffDir = Utils.getOption('a', options);
    tool.setARFFDirectory(arffDir);

    // load ResultFolder
    String resultDir = Utils.getOption('r', options);
    tool.setResultDir(resultDir);

    // load Classifier
    String classifierName = Utils.getOption('c', options);

    Classifier classifier = ms_Classifiers.get(classifierName);
    if (classifier == null) {
      System.err.println("No classifier specified. Use SVM instead");
      classifier = new SMO(); // default hehe.
    } else {
      System.out.println("Classifier: " + classifier.getClass().toString());
    }
    tool.configClassifier(classifier);

    // load Filter
    String filterName = Utils.getOption('f', options);
    Filter filter = ms_Filters.get(filterName);
    if (filter == null) {
      System.err
          .println("No filter specified. Use MyStringToWordVector instead");
      filter = createMyStringToWordVectorFilter();
    }
    tool.configFilter(filter);

    // load experiment name
    String expName = Utils.getOption('n', options);
    tool.setExpName(expName);

    // load Attribute Selection
    String asName = Utils.getOption('s', options);
    if (asName != null && asName.length() != 0) {
      System.out.println("Processing with attribute selection");
      Filter asf = getASFilter(asName);
      tool.addFilter(asf);
    }

    tool.process();
  }

  private Filter getASFilter(String asName) throws Exception {
    if (asName.equals("AttrSel")) {
      AttributeSelection as = new AttributeSelection();
      as.setEvaluator(new weka.attributeSelection.CfsSubsetEval());
      BestFirst bf = new BestFirst();
      bf.setLookupCacheSize(8);
      bf.setSearchTermination(5);
      as.setSearch(bf);
      return as;
    } else if (asName.equals("AttrSelBack")) {
      AttributeSelection as = new AttributeSelection();
      as.setEvaluator(new weka.attributeSelection.CfsSubsetEval());
      BestFirst bf = new BestFirst();
      bf.setLookupCacheSize(8);
      bf.setSearchTermination(5);
      // backward
      bf.setDirection(new SelectedTag(0, bf.TAGS_SELECTION));
      as.setSearch(bf);
      return as;
    } else {
      throw new Exception("No attribute filter selected!: "+asName);
      
    }
  }
}
