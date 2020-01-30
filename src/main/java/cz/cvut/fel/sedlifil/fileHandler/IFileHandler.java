package cz.cvut.fel.sedlifil.fileHandler;

import com.github.javaparser.ast.CompilationUnit;

import java.io.IOException;
import java.util.List;

public interface IFileHandler {
    List<String> getAllFilesFromPAth(String path);


    void saveCompilationUnitToFile(String absolutePath, CompilationUnit compilationUnit) throws IOException;

    }
