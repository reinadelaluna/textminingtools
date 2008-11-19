This is the 684 project source and data folder.

Author: zhaop (Puming Zhao 314134)

1.  All data are in ./data folder

2.  ./lib folder should contain weka.jar and snowball.jar 
    (as my makefile script directly usese them, 
     if you set the enviroment variables in the makefiles, 
     then it will be ok to run them)

3.  makefile-dev is the script using data/development/ as testing folder.
4.  makefile-test use data/test/ as  testing folder. 
*.  Each make tag is a test script. 
    So e.g. `make svm-wc` in makefile-test
    will run the testing of SVM with WordCount.
    The results of this run will be in file: result-test/SVM-WC
    
*.  Result `name-class` files are generated to result-test/ folder. 
    I put them out into the same folder with ReadMe.txt,
    as you may have noticed.
    
*.  Data input folders are set at makefile-test:
    e.g.  `-d` is data dir, `-t` is test dir, `-r` is where to store results.
    options_dir = -d data/training -t data/test  -r .

5.  Important java classes are 
        zhaop.s684.proj.Experimenter 
        (which runs the experiments)
    and zhaop.s684.proj.TextDirectoryClassifier 
        (which is a holder for one experiment, sorry for the bad naming) 
    
6.  As I noticed the demand for scripts very late, 
    not all runs are written as script. 
    I ran many of the experiments by hacking the java code in Experimenter.

7.  results are evaluated by eval.prl

8.  zhaop_SVMWC-AS.dev is the last classifier I used, 
    with SVM by WordCount, using BestFirst to Select Feature.

9.  Thank you very much.
  
