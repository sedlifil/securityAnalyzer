package cz.cvut.fel.sedlifil.fileHandler;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cz.cvut.fel.sedlifil.helper.Constants.*;

public class FileHandler implements IFileHandler {
    private final String sourcePathOfApplication;
    private Logger logger = LoggerFactory.getLogger(FileHandler.class);
    private List<String> filesFromPAthList;


    public FileHandler(String sourcePathOfApplication) {
        this.sourcePathOfApplication = sourcePathOfApplication;
        filesFromPAthList = new ArrayList<>();
    }


    public List<String> getAllFilesFromPAth(String path) {
        // fill all files from given path/directory to filesFromPAthList
        getAllFilesFromDirectory(path);

        return filesFromPAthList;
    }

    @Override
    public void saveCompilationUnitToFile(String absolutePath, CompilationUnit compilationUnit) throws IOException {
        File file = new File(absolutePath);
        LexicalPreservingPrinter.setup(compilationUnit);
        Files.write(file.toPath(), Collections.singletonList(compilationUnit.toString()), StandardCharsets.UTF_8);
    }


    private void getAllFilesFromDirectory(String path) {

        System.out.println();
        Path absPath = Paths.get(path).toAbsolutePath();
        File[] files = new File(absPath.toString()).listFiles();
        List<String> directories = new ArrayList<>();

        if (files == null) {
            logger.error("There is no directory or file in this path.");
            logger.error("JavaParser exits!");
            System.exit(ERROR_CODE);
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(JAVA_SUFFIX)) {
                if (!file.getName().startsWith("._")) {
//                    System.out.println("file = " + file);
                    filesFromPAthList.add(file.getAbsolutePath());
                }
            } else if (file.isDirectory() && !file.getName().equals(JAVA_TEST) && !file.getName().startsWith(".")) {
                directories.add(file.getName());
            }
//            else if (file.getName().equals(PomXML)) {
//                filesToAllBlocks.add(absPath.toString().concat(FILE_DELIMITER + file.getName()));
//            }
//            else {
//                for (String aFilesName : filesName) {
//                    if (file.getName().equals(aFilesName)) {
//                        filesToAllBlocks.add(absPath.toString().concat(FILE_DELIMITER + file.getName()));
//                    }
//                }
//            }
        }

        /* recursion for found directories */
        for (String s : directories) {
            getAllFilesFromDirectory(path.concat(FILE_DELIMITER + s));
        }
    }
}
