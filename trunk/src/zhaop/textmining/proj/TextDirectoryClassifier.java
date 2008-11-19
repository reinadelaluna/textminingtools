/**
 * Java program for classifying short text messages into two classes.
 */
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

//import weka.filters.MultiFilter;
import zhaop.textmining.proj.MessageClassifier;
import zhaop.textmining.proj.MultiFilter;
import zhaop.textmining.proj.MyStringToWordVector;
import zhaop.textmining.proj.Util;

import java.io.*;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;


public class TextDirectoryClassifier implements Serializable {

  private boolean mb_HasKlassEvalData = false;
  private String m_ExpName = "DefaultExperiment";
  /* The training data gathered so far. */
  private Instances m_Data = null;

  private Instances m_FilteredData = null;

  /* The testing data. */
  private Instances m_TestData = null;

  private Instances m_FilteredTestData = null;

  /* The filter used to generate the word counts. */
  private Filter m_Filter = null;
  /* The actual classifier. */
  private Classifier m_Classifier = null;

  /* Whether the model is up to date. */
  private boolean m_UpToDate;

  private String m_ARFFPath = "data/";

  private StringBuilder m_ResultText = new StringBuilder();

  private File m_ResultDir;

  private Hashtable<String, String> m_ClassValues = new Hashtable<String, String>();
  private Hashtable<String, String> m_TestClassValues = new Hashtable<String, String>();

  private String m_ARFFFolder;

  private String m_DataFolder;

  private String m_TestDataFolder;

  private Hashtable<String, String> m_ResultTable;

  public TextDirectoryClassifier() {
//    init();
    configFilter();
    configClassifier();
  }

  private void configFilter() {
    StringToWordVector filter = new StringToWordVector();
    filter.setTokenizer(new weka.core.tokenizers.WordTokenizer());
    filter.setLowerCaseTokens(true);
    // filter.setUseStoplist(false);
    filter.setStopwords(Util.STOPWORDS_FILE);
    filter.setStemmer(new weka.core.stemmers.SnowballStemmer());
    // System.out.println("Stop words: " + filter.getStopwords().getName());
    m_Filter = filter;
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

  public TextDirectoryClassifier(Instances data, StringToWordVector filter,
      Classifier classifier) {
    super();
    m_Filter = filter;
    m_Classifier = classifier;
  }

  private void initDataSet(String dirName) throws Exception{
    String nameOfDataset = "TextCategorization";

    // Create vector of attributes.
    FastVector attributes = new FastVector(2);

    // Add attribute for holding messages.
    attributes.addElement(new Attribute("Text", (FastVector) null));

    // Add class attribute.
    FastVector classValues = Util.getFastVector(Util.pathConcat(dirName, "-classlist"));
    attributes.addElement(new Attribute("Class", classValues));

    // Create dataset with initial capacity of 100, and set index of class.
    m_Data = new Instances(nameOfDataset, attributes, 100);
    m_Data.setClassIndex(m_Data.numAttributes() - 1);

    m_TestData = new Instances(nameOfDataset, attributes, 100);
    m_TestData.setClassIndex(m_TestData.numAttributes() - 1);

    m_FilteredData = null;
    m_FilteredTestData = null;
  }

  /**
   * Updates model using the given training message.
   */
  public void addTrainingData(String message, String classValue)
      throws Exception {

    addData(message, classValue, m_Data);
    m_UpToDate = false;
  }

  public void addTestData(String message, String classValue) throws Exception {

    addData(message, classValue, m_TestData);
  }

  public void addData(String message, String classValue, Instances dataset) {
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
    Attribute messageAtt = data.attribute("NewsArticle");
    instance.setValue(messageAtt, messageAtt.addStringValue(text));

    // Give instance access to attribute information from the dataset.
    instance.setDataset(data);
    return instance;
  }

  // public Instances filter(Instances data) throws Exception {
  // // Initialize filter and tell it about the input format.
  // m_Filter.setInputFormat(data);
  //
  // // Generate word counts from the training data.
  // Instances filtered = Filter.useFilter(data, m_Filter);
  //
  // return filtered;
  // }

  public void doTrain() throws Exception {
    System.out.println("Training Classifier...");

    // Initialize filter and tell it about the input format.
    m_Filter.setInputFormat(m_Data);

    // Generate word counts from the training data.
    m_FilteredData = Filter.useFilter(m_Data, m_Filter);

    // Rebuild classifier.
    m_Classifier.buildClassifier(m_FilteredData);
    m_UpToDate = true;
  }

  public void doDummyClassify() throws Exception {
    for (int i = 0; i < m_FilteredData.numInstances(); i++) {
      Instance instance = m_FilteredData.instance(i);
      double pred = m_Classifier.classifyInstance(instance);

      // Util.printIfInteresting("Dummy classified as","<-",
      // getKlassName(pred),
      // getKlassName(instance.classValue())
      // );
    }

  }

  public void doClassify() throws Exception {
    System.out.println("Classifying documents....");

    // m_Filter.setInputFormat(m_TestData);
    // Generate word counts from the training data.
    m_FilteredTestData = Filter.useFilter(m_TestData, m_Filter);

    // Check whether classifier and filter are up to date.
    if (!m_UpToDate) {
      this.doTrain();
    }

    for (int i = 0; i < m_FilteredTestData.numInstances(); i++) {
      Instance curTestInstance = m_FilteredTestData.instance(i);
      double predicted = m_Classifier.classifyInstance(curTestInstance);
      String predictedKlass = getKlassName(predicted);

      // Util.printIfInteresting("Document classified as","<-",
      // predictedKlass,
      // getKlassName(curTestInstance.classValue())
      // );
    }
  }

  public void doEvaluation() throws Exception {
    Evaluation eval = new Evaluation(m_FilteredData);
    eval.evaluateModel(m_Classifier, m_FilteredTestData);
    System.out.println(eval.toSummaryString("\nResults\n=======\n", false));
    System.out.println(eval.toClassDetailsString("Detailed"));

    StringBuilder sb = new StringBuilder();
    sb.append("=-=-=-=-=-= My stats =-=-=-=-=-=\n");
    sb.append(String.format("%-10s%-10s%-10s%-10s\n", "Accuracy", "Precision",
        "Recall", "F-score"));
    sb.append(String.format("%-10.3f%-10.3f%-10.3f%-10.3f\n",
        eval.pctCorrect() / 100.0, eval.precision(1), eval.recall(1), eval
            .fMeasure(1)));

    System.out.println(sb.toString());

  }

  /**
   * Classifies a given document.
   */
  public String classify(String document) throws Exception {

    // Check whether classifier has been built.
    if (m_Data.numInstances() == 0) {
      throw new Exception("No classifier available.");
    }

    // Check whether classifier and filter are up to date.
    if (!m_UpToDate) {
      this.doTrain();

    }

    // Make separate little test set so that document
    // does not get added to string attribute in m_Data.
    Instances testset = m_Data.stringFreeStructure();

    // Make document into test instance.
    Instance instance = makeInstance(document, testset);

    // Filter instance.
    m_Filter.input(instance);
    Instance filteredInstance = m_Filter.output();

    // Get index of predicted class value.
    double predicted = m_Classifier.classifyInstance(filteredInstance);
    String predictedKlass = getKlassName(predicted);
    // Output class value.

    // Save result
    // filteredInstance.insertAttributeAt(filteredInstance.numAttributes()-1);

    return predictedKlass;
  }

  private String getKlassName(double predicted) {
    return m_Data.classAttribute().value((int) predicted);
  }

  /**
   * Main method.
   */
  public static void main(String[] options) {
    TextDirectoryClassifier tool = new TextDirectoryClassifier();
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
    this.doClassify();
    // this.doDummyClassify();
    if (mb_HasKlassEvalData) {
      
      this.doEvaluation();
    }
    // have to rerun the classification because of saving results
    this.doClassifyWithResultOutput();

    this.saveTrainingData();
    this.saveFilteredData();
    this.saveTestData();
    this.saveFilteredTestData();

    System.out.println("OOOPS! Experiment fin.");
  }

  private void doOptions(String[] options) throws Exception {
    // Get training arff file
    String arffdir = Utils.getOption('a', options);
    this.m_ARFFFolder = arffdir;
    setARFFDirectory(arffdir);

    // Get files in dir
    String dirName = Utils.getOption('d', options);
    this.m_DataFolder = dirName;
    loadTrainingDataFrom(dirName);

    // Get test dir
    String testDirName = Utils.getOption('t', options);
    this.m_TestDataFolder = testDirName;
    loadTestDataFrom(testDirName);

    // Get the filter
    // String filterName = Utils.getOption('f', options);
    // if(filterName.length()==0){
    // this.configFilter();
    // }else{
    // try{
    // Filter f = (Filter)Class.forName(filterName);
    // }catch(Exception e){
    // System.err.println("Filter class Name is wrong!");
    // }
    // }

  }

  public void setARFFDirectory(String arffdir) {
    this.m_ARFFPath = arffdir;
  }

  public void doClassifyWithResultOutput() throws Exception {
    m_TestClassValues = getClassValues(m_TestDataFolder);
    m_ResultTable = new Hashtable<String, String>();
    File testDir = new File(m_TestDataFolder);
    File[] testFiles = testDir.listFiles();

    // Process test
    // execTest(testFiles);
    for (File f : testFiles) {
      String filePath = f.getAbsolutePath();
      String fileName = f.getName();
      String text = readText(filePath);
      String classValue = "";
      if(mb_HasKlassEvalData){
        classValue = m_TestClassValues.get(fileName);
        if (classValue == null || classValue.length() == 0) {
          throw new Exception("No testclassValue found for the data");
        }
      }
      String klassResult = this.classify(text);
      m_ResultTable.put(fileName, klassResult);
      
    }
    this.saveResult();

  }

  private void saveResult() throws Exception {
    if (m_ResultDir == null) {
      System.out.println("No result folder specified, use data/result instead");
      m_ResultDir = Util.loadDir("data/result");
    }
    File resultFile = new File(m_ResultDir.getAbsolutePath() + "/" + m_ExpName);
    if (!resultFile.exists()) {
      System.out.println(resultFile.getAbsolutePath());
      resultFile.createNewFile();
    }
    BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile));
    printTable(m_ResultTable, bw);
    bw.close();

  }

  public void loadTestDataFrom(String testDirName) throws Exception {
    m_TestDataFolder = testDirName;
    m_TestClassValues = getClassValues(testDirName);

    File testDir = new File(testDirName);
    File[] testFiles = testDir.listFiles();

    for (File f : testFiles) {
      String filePath = f.getAbsolutePath();
      String fileName = f.getName();
      String text = readText(filePath);
      String classValue = "true";
      if (mb_HasKlassEvalData) {
        classValue = m_TestClassValues.get(fileName);
        if (classValue == null || classValue.length() == 0) {
          throw new Exception("No testclassValue found for the data");
        }
      }
      // System.out.println("Add test data: " + fileName + "," + classValue);
      this.addTestData(text, classValue);
    }
  }

  public void loadTrainingDataFrom(String dirName) throws Exception {
    m_DataFolder = dirName;
    if (dirName.length() == 0) {
      throw new Exception("Must provide dir name of training text files.");
    }
    
    // Init dataset
    initDataSet(dirName);
    File dir = new File(dirName);
    File[] files = dir.listFiles();

    // Get class file data
    m_ClassValues = getClassValues(dirName);
    
    // Process document.
    for (File f : files) {
      String filePath = f.getAbsolutePath();
      String fileName = f.getName();
      String text = readText(filePath);
      String classValue = "";
      classValue = m_ClassValues.get(fileName);
      if (classValue == null || classValue.length() == 0) {
        throw new Exception("No classValue found for the data");
      }
      this.addTrainingData(text, classValue);
    }
  }

  private void execTest(File[] testFiles) throws Exception, IOException {
    Hashtable<String, String> tableResult = new Hashtable<String, String>();

    for (File f : testFiles) {
      String filePath = f.getAbsolutePath();
      String fileName = f.getName();
      String text = readText(filePath);
      String classResult = this.classify(text);
      tableResult.put(fileName, classResult);
      BufferedWriter bw = new BufferedWriter(
          new FileWriter("data/result-class"));

      printTable(tableResult, bw);
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

  public void saveFilteredTestData() throws Exception {
    this.saveDataAs(m_FilteredTestData, "filteredTest.arff");
  }

  private void saveTestData() throws Exception {
    this.saveDataAs(m_TestData, "test.arff");

  }

  public void saveTrainingDataAs(String filepath) throws Exception {
    this.saveDataAs(m_Data, filepath);
  }

  public void saveTestDataAs(String filepath) throws Exception {
    this.saveDataAs(m_TestData, filepath);
  }

  public void saveFilteredDataAs(String filepath) throws Exception {
    this.saveDataAs(m_FilteredData, filepath);
  }

  public void saveDataAs(Instances data, String file) throws Exception {
    String arffpath = Util.pathJoin(m_ARFFPath, file);
    ArffSaver saver = new ArffSaver();
    saver.setInstances(data);
    saver.setFile(new File(arffpath));
    saver.writeBatch();
  }

  private Hashtable<String, String> getClassValues(String dirName) {
    try {
      String classValuesFile = Util.pathConcat(dirName, "-class");
      if (!(new File(classValuesFile).exists())) {
        this.mb_HasKlassEvalData = false;
        return null;
      } else {
        this.mb_HasKlassEvalData = true;

        BufferedReader is;
        is = new BufferedReader(new FileReader(classValuesFile));
        String line;
        String[] items = new String[2];
        Hashtable<String, String> ret = new Hashtable<String, String>();
        while ((line = is.readLine()) != null) {
          items = line.split(" ");
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

  private static void printTable(Hashtable<String, String> ret) {

    Set<String> keys = ret.keySet();
    Iterator<String> iter = keys.iterator();
    while (iter.hasNext()) {
      Object key = iter.next();
      System.out.println(key + " " + ret.get(key) + "\n");
    }
  }

  private static void printTable(Hashtable<String, String> ret,
      BufferedWriter out) throws Exception {
    Vector<String> v = new Vector<String>(ret.keySet());
    Collections.sort(v);
    Iterator<String> it = v.iterator();
    while (it.hasNext()) {
      String element = it.next();
      out.write(element + " " + (String) ret.get(element) + "\n");
    }
    out.close();
  }

  public void setResultDir(String resultDir) throws Exception {
    this.m_ResultDir = Util.loadDir(resultDir);
  }

  public void setExpName(String expName) {
    if (expName.length() == 0) {
      System.out.println("No name specified for the experiment, generate one");
      generateName();
    } else {
      this.m_ExpName = expName.replace(' ', '-');
    }

  }

  private void generateName() {
    this.m_ExpName = "exp-" + this.m_Classifier.getClass().getSimpleName()
        + "-" + this.m_Filter.getClass().getSimpleName() + "-result";

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
