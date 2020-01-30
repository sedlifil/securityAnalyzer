package cz.cvut.fel.sedlifil.main;

import cz.cvut.fel.sedlifil.analyzer.Analyzer;
import cz.cvut.fel.sedlifil.fileHandler.FileHandler;
import cz.cvut.fel.sedlifil.fileHandler.IFileHandler;
import org.apache.log4j.BasicConfigurator;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) {
        String filePath = "";
        String rootPackagePath = "";

        if(args.length < 2) {
            System.out.println("Two parameters needed:");
            System.out.println("First - path to directory src of analyzing application.");
            System.out.println("Second - parent directory path of package name.");
            System.exit(1);
        }

        if (args.length == 2) {
            filePath = args[0];
            rootPackagePath = args[1];
        }


        BasicConfigurator.configure();
        IFileHandler fileHandler = new FileHandler(filePath);

        Analyzer analyzer = null;
        try {
            analyzer = new Analyzer(fileHandler, filePath, rootPackagePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        analyzer.analyzeApplicationSecurity();
    }
}
