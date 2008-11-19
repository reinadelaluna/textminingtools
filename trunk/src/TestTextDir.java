import java.util.Enumeration;

import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.converters.TextDirectoryLoader;

public class TestTextDir {

  public static void main(String[] args) {
    if (args.length > 0) {
      try {
        TextDirectoryLoader loader = new TextDirectoryLoader();
        loader.setOptions(args);
        System.out.println(loader.getDataSet());
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.err.println("\nUsage:\n" + "\tTextDirectoryLoader [options]\n"
          + "\n" + "Options:\n");

      Enumeration enm = ((OptionHandler) new TextDirectoryLoader())
          .listOptions();
      while (enm.hasMoreElements()) {
        Option option = (Option) enm.nextElement();
        System.err.println(option.synopsis());
        System.err.println(option.description());
      }

      System.err.println();
    }
  }
}
