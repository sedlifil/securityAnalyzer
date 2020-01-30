package cz.cvut.fel.sedlifil.main;

import cz.cvut.fel.sedlifil.analyzer.Analyzer;
import cz.cvut.fel.sedlifil.fileHandler.FileHandler;
import cz.cvut.fel.sedlifil.fileHandler.IFileHandler;
import org.apache.log4j.BasicConfigurator;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) {
        String filePath = "";

        if(args.length == 0) {
            System.out.println("One parameter needed - path to directory src of analyzing application.");
            System.exit(1);
        }

        if (args.length == 1) {
            filePath = args[0];
        }


        BasicConfigurator.configure();
        IFileHandler fileHandler = new FileHandler(filePath);

        Analyzer analyzer = null;
        try {
            analyzer = new Analyzer(fileHandler, filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        analyzer.analyzeApplicationSecurity();
    }
}
