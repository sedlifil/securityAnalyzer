package cz.cvut.fel.sedlifil.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static cz.cvut.fel.sedlifil.helper.Constants.FILE_DELIMITER;

public class ContainerClassCU {
    private String nameClass;
    private String nameClassWithAbsPath;
    private String belongToBlocks;
    private List<String> importsAsClassNameList;
    private List<String> implementsClassList;
    private List<String> importsImplementsExtendedList;
    private CompilationUnit compilationUnit;
    private List<VariableDeclarator> fieldsFromClassList;
    private List<AnnotationExpr> annotationClassList;

    public ContainerClassCU(String nameClassWithAbsPath) throws IOException {
        this.nameClassWithAbsPath = nameClassWithAbsPath;
        JavaParser javaParser = new JavaParser();
        compilationUnit = javaParser.parseResource(nameClassWithAbsPath).getResult().orElseThrow(() -> new IOException());
        setUpName();
    }

    private void setUpName() {
        String reg = FILE_DELIMITER;
        if(reg.equals("\\")){
            reg = "\\\\";
        }
        String list[] = nameClassWithAbsPath.split(reg);
        nameClass = list[list.length - 1];
    }

    /**
     * @return name of class
     */
    public String getNameClass() {
        return nameClass;
    }

    /**
     * @return name with absolute path of class
     */
    public String getNameClassWithAbsPath() {
        return nameClassWithAbsPath;
    }

    /**
     * @return compilation unit of class
     */
    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    /**
     * method to found all field variable of the class
     *
     * @return list of variables
     */
    public List<VariableDeclarator> getFieldsFromClassList() {
        if (fieldsFromClassList == null) {
            fieldsFromClassList = new ArrayList<>();
            VoidVisitor<List<VariableDeclarator>> variableClassVisitor = new FieldClassVisitor();
            variableClassVisitor.visit(compilationUnit, fieldsFromClassList);
        }
        return fieldsFromClassList;
    }

    /**
     * method to get all imported,implemented and extended class names with path from given compilationUnit
     *
     * @return list of imports, extended and implemented class of container class
     */
    public List<String> getImportsImplementsExtendedFromClass() {
        if (importsImplementsExtendedList == null) {
            List<ImportDeclaration> imports = compilationUnit.getImports();
            importsImplementsExtendedList = addImportsAsClassName(imports);
            List<String> implementsClassList = new ArrayList<>();

            VoidVisitor<List<String>> implementsClassVisitor = new ImplementsClassVisitor();
            implementsClassVisitor.visit(compilationUnit, implementsClassList);
            importsImplementsExtendedList.addAll(implementsClassList);

            List<String> extendedClassList = new ArrayList<>();
            VoidVisitor<List<String>> extendedClassVisitor = new ExtendedClassVisitor();
            extendedClassVisitor.visit(compilationUnit, extendedClassList);
            importsImplementsExtendedList.addAll(extendedClassList);
        }
        return importsImplementsExtendedList;
    }

    /**
     * method to get all implemented class names with path from given compilationUnit
     *
     * @return list of implements path
     */
    public List<String> getImplementsFromClass() {
        if (implementsClassList == null) {
            implementsClassList = new ArrayList<>();
            VoidVisitor<List<String>> implementsClassVisitor = new ImplementsClassVisitor();
            implementsClassVisitor.visit(compilationUnit, implementsClassList);
        }
        return implementsClassList;
    }

    /**
     * method to get all implemented and extended class name with path from given compilationUnit
     *
     * @return list of extended and implemented class of container class
     */
    public List<String> getImplementsExtendedFromClass() {
        List<String> implementsExtendedList = new ArrayList<>();
        VoidVisitor<List<String>> implementsClassVisitor = new ImplementsClassVisitor();
        implementsClassVisitor.visit(compilationUnit, implementsExtendedList);

        List<String> extendedClassList = new ArrayList<>();
        VoidVisitor<List<String>> extendedClassVisitor = new ExtendedClassVisitor();
        extendedClassVisitor.visit(compilationUnit, extendedClassList);
        implementsExtendedList.addAll(extendedClassList);
        return implementsExtendedList;
    }

    /**
     * method to get all imported class name with path from given compilationUnit
     *
     * @return list of imports of container class
     */
    public List<String> getImportsFromClass() {
        if (importsAsClassNameList == null) {
            List<ImportDeclaration> importsList = compilationUnit.getImports();
            importsAsClassNameList = addImportsAsClassName(importsList);
        }
        return importsAsClassNameList;
    }

    /**
     * @return list of methods of container class
     */
    public List<MethodDeclaration> getMethodDeclarations() {
        List<MethodDeclaration> methodDeclarationList = new ArrayList<>();
        VoidVisitor<List<MethodDeclaration>> methodVisitor = new MethodClassVisitor();
        methodVisitor.visit(compilationUnit, methodDeclarationList);
        return methodDeclarationList;
    }

    /**
     * @return list of annotationExpr of container class, but only from class declaration
     */
    public List<AnnotationExpr> getClassAnnotations() {
        if (annotationClassList == null) {
            annotationClassList = new ArrayList<>();
            VoidVisitor<List<AnnotationExpr>> annotationClassVisitor = new AnnotationClassVisitor();
            annotationClassVisitor.visit(compilationUnit, annotationClassList);
        }
        return annotationClassList;
    }


    /**
     * method to change raw line with imports to look like ordinary class name with path
     * using delimiter - JAVA_DELIMITER
     *
     * @param imports list of imports of container class
     * @return list of imports which was converted into class name
     */
    private List<String> addImportsAsClassName(List<ImportDeclaration> imports) {
        /* edit import to look like class name with absolute path
         * remove ; and word "import" in front of it
         * replace "." to "/"
         * */
        return imports.stream().map(y -> {
            String import_ = y.toString();
            return import_.substring(import_.indexOf(" ") + 1, import_.lastIndexOf(";"))
                    .replace(".", FILE_DELIMITER);

        }).collect(Collectors.toList());
    }


    /**
     * Annotation class visitor
     * visitor fill in into list of AnnotationExpr all annotation of given container class declaration
     */
    public static class AnnotationClassVisitor extends VoidVisitorAdapter<List<AnnotationExpr>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration n, List<AnnotationExpr> collector) {
            super.visit(n, collector);
            collector.addAll(n.getAnnotations());
        }
    }

    /**
     * Extended class visitor
     * visitor fill in into list of string all extended class of given container class
     */
    private static class ExtendedClassVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration md, List<String> collector) {
            super.visit(md, collector);
            md.getExtendedTypes().forEach(y -> collector.add(y.getNameAsString()));
        }
    }

    /**
     * Implements class visitor
     * visitor fill in into list of string all implemented class of given container class
     */
    private static class ImplementsClassVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration md, List<String> collector) {
            super.visit(md, collector);
            md.getImplementedTypes().forEach(y -> collector.add(y.getNameAsString()));
        }
    }

    /**
     * Field class visitor
     * visitor fill in into list of variableDeclaration of all fields from given container class
     */
    private static class FieldClassVisitor extends VoidVisitorAdapter<List<VariableDeclarator>> {
        @Override
        public void visit(FieldDeclaration md, List<VariableDeclarator> collector) {
            super.visit(md, collector);
            collector.addAll(md.getVariables());
        }
    }

    /**
     * method class visitor
     * visitor fill in into list of methodDeclaration all methods of given container class
     */
    private static class MethodClassVisitor extends VoidVisitorAdapter<List<MethodDeclaration>> {
        @Override
        public void visit(MethodDeclaration md, List<MethodDeclaration> collector) {
            super.visit(md, collector);
            collector.add(md);
        }
    }
}
