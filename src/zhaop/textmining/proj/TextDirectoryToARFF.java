package zhaop.textmining.proj;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.rules.ZeroR;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;

import weka.filters.unsupervised.attribute.Reorder;
import weka.filters.unsupervised.attribute.StringToWordVector;

import zhaop.textmining.proj.MultiFilter;
import zhaop.textmining.proj.Util;

import java.io.*;
import java.util.Hashtable;



public class TextDirectoryToARFF implements Serializable {

  private static final long serialVersionUID = 1L;
  

  private String m_ARFFFolder = "arff";
  private String m_DataFolder = "data/training";
  
  private Instances m_Data = null;
  private Instances m_FilteredData = null;


  /* The filter used to generate the word counts. */
  private Filter m_Filter = null;
  /* The actual classifier. */
  private Classifier m_Classifier = null;

  private Hashtable<String, String> m_ClassValues = new Hashtable<String, String>();

  public TextDirectoryToARFF() throws Exception {
    configFilter();
    configClassifier();
  }
  
  private void configFilter() throws Exception{
    StringToWordVector filter = new StringToWordVector();
    filter.setTokenizer(new weka.core.tokenizers.WordTokenizer());
    filter.setLowerCaseTokens(true);
    // filter.setUseStoplist(false);
    filter.setWordsToKeep(800);
    filter.setStopwords(Util.STOPWORDS_FILE);
    filter.setStemmer(new weka.core.stemmers.SnowballStemmer());
    // System.out.println("Stop words: " + filter.getStopwords().getName());
    
    configFilter(filter);
  }

  private void configClassifier() {
    m_Classifier = new ZeroR();
    // m_Classifier = new NaiveBayes();
  }

  public void configFilter(Filter f) throws Exception {
    Filter[] filters = new Filter[2];
    filters[0] = f;
    Reorder ro = new Reorder();
    String[] options = new String[2];
    options[0] = "-R";
    options[1] = "2-last,first";
    ro.setOptions(options);
    filters[1] = ro;

    MultiFilter mf = new MultiFilter();
    mf.setFilters(filters);
    m_Filter = mf;
    System.out.println("we have filters: "
        + ((MultiFilter) m_Filter).getFilters().length);
  }

  public void configClassifier(Classifier f) {

    m_Classifier = f;
  }



  private void initDataSet(String dirName) throws Exception{
    String nameOfDataset = "TextCategorization";

    // Create vector of attributes.
    FastVector attributes = new FastVector(2);

    // Add attribute for holding messages.
    attributes.addElement(new Attribute(Util.attrNameText, (FastVector) null));

    // Add class attribute.
    FastVector classValues = Util.getFastVector(Util.pathConcat(dirName, "-classlist"));
    attributes.addElement(new Attribute(Util.attrNameClass, classValues));

    // Create dataset with initial capacity of 100, and set index of class.
    m_Data = new Instances(nameOfDataset, attributes, 100);
    m_Data.setClassIndex(m_Data.numAttributes() - 1);

    m_FilteredData = null;

  }

  /**
   * Updates model using the given training message.
   */
  public void addTrainingData(String message, String classValue)
      throws Exception {
    addData(message, classValue, m_Data);
  }


  private void addData(String message, String classValue, Instances dataset) {
    // Make message into instance.
    Instance instance = makeInstance(message, dataset);
    // Set class value for instance.
    instance.setClassValue(classValue);

    // Add instance to training data.
    dataset.add(instance);
  }

  /**
   * Method that converts a text message into an instance.
   */
  private Instance makeInstance(String text, Instances data) {

    // Create instance of length two.
    Instance instance = new Instance(2);

    // Set value for message attribute
    Attribute messageAtt = data.attribute(Util.attrNameText);
    instance.setValue(messageAtt, messageAtt.addStringValue(text));

    // Give instance access to attribute information from the dataset.
    instance.setDataset(data);
    return instance;
  }

 

  public void doTrain() throws Exception {
    System.out.println("Training Classifier...");

    prepareInput();

    // Rebuild classifier.
//    m_Classifier.buildClassifier(m_FilteredData);
  }

  private void prepareInput() throws Exception {
    // Initialize filter and tell it about the input format.
    m_Filter.setInputFormat(m_Data);

    // Generate word counts from the training data.
    m_FilteredData = Filter.useFilter(m_Data, m_Filter);
  }

  public void doDummyClassify() throws Exception {
    for (int i = 0; i < m_FilteredData.numInstances(); i++) {
      Instance instance = m_FilteredData.instance(i);
      double pred = m_Classifier.classifyInstance(instance);

    }

  }



  private String getKlassName(double predicted) {
    return m_Data.classAttribute().value((int) predicted);
  }

  /**
   * Main method.
   */
  public static void main(String[] options) throws Exception{
    TextDirectoryToARFF tool = new TextDirectoryToARFF();
    tool.doMain(options);
  }

  private void doMain(String[] options) {
    try {

      // deal with options
      doOptions(options);

      process();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void process() throws Exception {
    // main exec
    this.doTrain();
    // this.doDummyClassify();
    // have to rerun the classification because of saving results

    this.saveTrainingData();
    this.saveFilteredData();

    System.out.println("OOOPS! ARFF fin.");
  }

  private void doOptions(String[] options) throws Exception {
    // Get Arff output dir.
    String arffdir = Utils.getOption('a', options);
    setM_ARFFFolder(arffdir);

    // Get training data dir.
    String dirName = Utils.getOption('d', options);
    this.m_DataFolder = dirName;
    loadTrainingDataFrom(dirName);

  }


 
  public String getM_DataFolder() {
    return m_DataFolder;
  }

  public String getM_ARFFFolder() {
    return m_ARFFFolder;
  }

  public void setM_ARFFFolder(String folder) {
    m_ARFFFolder = folder;
  }

  public void setM_DataFolder(String dataFolder) {
    m_DataFolder = dataFolder;
  }

  public void loadTrainingDataFrom(String dirName) throws Exception {
    m_DataFolder = dirName;
    if (!Util.isDirectory(dirName)) {
      throw new Exception("Must provide dir name of training text files.");
    }
    System.out.println(dirName);
    // Init dataset
    initDataSet(dirName);
    File dir = new File(dirName);
    File[] files = dir.listFiles();

    // Get class file data
    m_ClassValues = getClassValues(dirName);
//    Util.printTable(m_ClassValues);
    // Process document.
    for (File f : files) {
      if(f.isDirectory()){
        System.out.println("Dir found: " + f.getName());
        continue;
      }
      String filePath = f.getAbsolutePath();
      System.out.println(filePath);
      String fileName = f.getName();
      String text = readText(filePath);
      String classValue = "";
      classValue = m_ClassValues.get(fileName);
      System.out.println(classValue);
      if (classValue == null || classValue.length() == 0) {
        throw new Exception("No classValue found for the data");
      }
      this.addTrainingData(text, classValue);
    }
  }

  private String readText(String filePath) {
    try {
      FileReader m = new FileReader(filePath);

      StringBuffer text = new StringBuffer();
      int l;
      while ((l = m.read()) != -1) {
        text.append((char) l);
      }
      m.close();
      return text.toString();
    } catch (IOException e) {
      e.printStackTrace();
      return "";
    }
  }

  public void saveTrainingData() throws Exception {
    this.saveTrainingDataAs("training.arff");
  }

  public void saveFilteredData() throws Exception {
    this.saveFilteredDataAs("filtered.arff");
  }

  public void saveTrainingDataAs(String filepath) throws Exception {
    this.saveDataAs(m_Data, filepath);
  }


  public void saveFilteredDataAs(String filepath) throws Exception {
    this.saveDataAs(m_FilteredData, filepath);
  }

  private void saveDataAs(Instances data, String file) throws Exception {
    String arffpath = Util.pathJoin(m_ARFFFolder, file);
    ArffSaver saver = new ArffSaver();
    saver.setInstances(data);
    saver.setFile(new File(arffpath));
    saver.writeBatch();
  }

  private Hashtable<String, String> getClassValues(String dirName) {
    try {
      String classValuesFile = Util.pathConcat(dirName, "-class");
      if (!(new File(classValuesFile).exists())) {
        return new Hashtable<String, String>();
      } else {

        BufferedReader is;
        is = new BufferedReader(new FileReader(classValuesFile));
        String line;
        String[] items = new String[2];
        Hashtable<String, String> ret = new Hashtable<String, String>();
        while ((line = is.readLine()) != null) {
          items = line.split("\t");
          if (items.length != 2) {
            System.err.println("Class Value file format error!");
          }
          ret.put(items[0], items[1]);
        }
        // printTable(ret);
        return ret;
      }

    } catch (Exception e) {
      System.err.println("failed to load class values: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }





 
  public void addFilter(Filter asf) {
    if (this.m_Filter.getClass().getSimpleName().equals("MultiFilter")) {
      //create a new multifilter chain.
      MultiFilter mf = new MultiFilter();
      Filter[] oldFilters = ((MultiFilter) m_Filter).getFilters();
      int lenOld = oldFilters.length;
      Filter[] newFilters = new Filter[lenOld + 1];
      for (int i = 0; i < oldFilters.length; i++) {
        newFilters[i] = oldFilters[i];
        // System.out.println("old filter: " +
        // oldFilters[i].getClass().getSimpleName() );
      }
      
      //add new filter in to the filter chain.
      newFilters[newFilters.length - 1] = asf;
      mf.setFilters(newFilters);
      
      //save the new filter chain.
      m_Filter = mf;
    } else {
      System.out.println("Error in adding filter to :"
          + this.m_Filter.getClass().getSimpleName());
    }
  }


}
