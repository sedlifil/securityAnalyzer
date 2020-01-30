package cz.cvut.fel.sedlifil.visitor;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.List;

public class Visitor {

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
     * Annotation method visitor
     * visitor fill in into list of AnnotationExpr all annotation of given methodDeclaration
     */
    public static class AnnotationMethodVisitor extends VoidVisitorAdapter<List<AnnotationExpr>> {
        @Override
        public void visit(MethodDeclaration n, List<AnnotationExpr> collector) {
            super.visit(n, collector);
            collector.addAll(n.getAnnotations());
        }
    }

    /**
     * method class visitor
     * visitor fill in into list of methodDeclaration all methods of given container class
     */
    public static class MethodClassVisitor extends VoidVisitorAdapter<List<MethodDeclaration>> {
        @Override
        public void visit(MethodDeclaration md, List<MethodDeclaration> collector) {
            super.visit(md, collector);
            collector.add(md);
        }
    }

    /**
     * Implements class visitor
     * visitor fill in into list of string all implemented class of given container class
     */
    public static class ImplementsClassVisitor extends VoidVisitorAdapter<List<String>> {
        @Override
        public void visit(ClassOrInterfaceDeclaration md, List<String> collector) {
            super.visit(md, collector);
            md.getImplementedTypes().forEach(y -> collector.add(y.getNameAsString()));
        }
    }


    public static class MethodCallVisitor extends VoidVisitorAdapter<List<MethodCallExpr>> {
        @Override
        public void visit(MethodCallExpr md, List<MethodCallExpr> collector) {
            super.visit(md, collector);
            collector.add(md);
        }
    }


}
