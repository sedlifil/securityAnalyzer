package cz.cvut.fel.sedlifil.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import cz.cvut.fel.sedlifil.container.ComponentMethodNode;
import cz.cvut.fel.sedlifil.container.ControllerContainer;
import cz.cvut.fel.sedlifil.container.SecurityAnnotation;
import cz.cvut.fel.sedlifil.fileHandler.IFileHandler;
import cz.cvut.fel.sedlifil.visitor.Visitor;
import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.component.context.AnalysisContext;
import edu.baylor.ecs.jparser.component.impl.ClassComponent;
import edu.baylor.ecs.jparser.component.impl.FieldComponent;
import edu.baylor.ecs.jparser.component.impl.MethodInfoComponent;
import edu.baylor.ecs.jparser.factory.context.AnalysisContextFactory;
import edu.baylor.ecs.jparser.factory.directory.DirectoryFactory;
import edu.baylor.ecs.jparser.model.AnnotationValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static cz.cvut.fel.sedlifil.helper.Constants.*;
import static cz.cvut.fel.sedlifil.helper.Constants.CRITICAL_REQUEST_METHOD_SET;

public class Analyzer {
    private Logger logger = LoggerFactory.getLogger(Analyzer.class);

    private final IFileHandler fileHandler;
    private final String sourcePathOfApplication;
    private List<String> filesFromPathList;
    private List<ContainerClassCU> containerClassCUList;
    private final AnalysisContext analysisContext;
    private final Set<String> criticalAnnotationSet;

    private Set<ComponentMethodNode> componentMethodNodeOfTreeSet;

    private List<ControllerContainer> controllerContainerList;
    private Set<ComponentMethodNode> inconsistentComponentMethodMap;


    public Analyzer(IFileHandler fileHandler, String sourcePathOfApplication, String rootPathOfPackageApplication) throws FileNotFoundException {
        this.fileHandler = fileHandler;
        this.sourcePathOfApplication = sourcePathOfApplication;
        containerClassCUList = new ArrayList<>();
        criticalAnnotationSet = new HashSet<>();
        componentMethodNodeOfTreeSet = new HashSet<>();
        inconsistentComponentMethodMap = new HashSet<>();


        DirectoryFactory directoryFactory = new DirectoryFactory();
        AnalysisContextFactory analysisFactory = new AnalysisContextFactory();

        // Creates a directory graph from the DirectoryFactory from a string path to the directory in question.
        Component directoryGraph = directoryFactory.createDirectoryGraph(sourcePathOfApplication);


        analysisContext = analysisFactory.createAnalysisContextFromDirectoryGraph(directoryGraph);

        // set Java SymbolSolver
        try {
            TypeSolver myTypeSolver = new CombinedTypeSolver(
                    new ReflectionTypeSolver(), JarTypeSolver.getJarTypeSolver("/Users/filip/Dropbox/palmTourism/palmTourism/target/palmTourism-0.0.1-SNAPSHOT.jar"),
                    new JavaParserTypeSolver(new File(rootPathOfPackageApplication)));

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(myTypeSolver);
            StaticJavaParser
                    .getConfiguration()
                    .setSymbolResolver(symbolSolver);

        } catch (IOException e) {
            logger.error("Second parameter is incorrect.");
            System.exit(2);
        }

        // for each class or interface component set new compilation unit with Java SymbolSolver
        for (Component _component : analysisContext.getClassesAndInterfaces()) {
            if (_component.asInterfaceComponent() != null) {
                CompilationUnit cu = StaticJavaParser.parse(new File(_component.asInterfaceComponent().getPath()));
                _component.asInterfaceComponent().setCompilationUnit(cu);
            } else {
                CompilationUnit cu = StaticJavaParser.parse(new File(_component.asClassComponent().getPath()));
                _component.asClassComponent().setCompilationUnit(cu);
            }
        }

    }


    public void analyzeApplicationSecurity() {
        filesFromPathList = fileHandler.getAllFilesFromPAth(sourcePathOfApplication);

        controllerContainerList = findControllersByAnnotation();

        // change interface component field variables into their implementation (into class component)
        // class component field variables remain same
        controllerContainerList.forEach(this::setClassContainerFromField);

        // todo jen jedna trida -> pozdeji smazat
//        createGraphOfSubMethodsForClassComponent(controllerContainerList.get(0));

        // todo odkomentovat
        createGraphOfSubMethodsForControllers();

//        printAllInterfacesOrClassNamesWithMethods();


        traverseCriticalMethods();

        saveSuggestedAnnotationsToFile();

        printAllControllerClassNamesWithCriticalMethods();


    }


    /**
     * for all controllers create their sub methods graph
     */
    private void createGraphOfSubMethodsForControllers() {
        controllerContainerList.forEach(this::createGraphOfSubMethodsForClassComponent);
    }


    private void createGraphOfSubMethodsForClassComponent(ControllerContainer controllerContainer) {

        // evaluate critical methods
        evaluateCriticalMethods(controllerContainer);

        //for each critical method find sub methods
        for (ComponentMethodNode componentMethodNode : controllerContainer.getCriticalMethods()) {
            findAndSetSubMethodsForComponentMethodNode(componentMethodNode);
        }

        controllerContainer.getCriticalMethods().forEach(ctiricalMethod -> {
            ctiricalMethod.getSubMethods().forEach(subMethod -> {
                createGraphOfSubMethodsForComponentMethodNode(subMethod);
            });
        });
    }


    private void createGraphOfSubMethodsForComponentMethodNode(ComponentMethodNode componentMethodNode) {
        findAndSetSubMethodsForComponentMethodNode(componentMethodNode);

        componentMethodNode.getSubMethods().forEach(subComponentMethodNode -> {
            createGraphOfSubMethodsForComponentMethodNode(subComponentMethodNode);
        });

    }

    /**
     * get all critical methods (POST, PUT, DELETE) of Class classComponent
     */
    private void evaluateCriticalMethods(ControllerContainer controllerContainer) {
        List<ComponentMethodNode> criticalMethods = new ArrayList<>();

        // get all methods
        List<MethodDeclaration> methodDeclarationArrayList = new ArrayList<>();
        VoidVisitor<List<MethodDeclaration>> methodClassVisitor = new Visitor.MethodClassVisitor();
        methodClassVisitor.visit(controllerContainer.getClassComponent().asClassComponent().getCompilationUnit(), methodDeclarationArrayList);

        methodDeclarationArrayList.forEach(methodDeclaration -> {
            // get all annotations for methoDeclaration
            List<AnnotationExpr> annotationExprs = new ArrayList<>();
            VoidVisitor<List<AnnotationExpr>> annotationMethodVisitor = new Visitor.AnnotationMethodVisitor();
            annotationMethodVisitor.visit(methodDeclaration, annotationExprs);

            annotationLoop:
            for (AnnotationExpr annotationExpr : annotationExprs) {
                String annName = annotationExpr.getNameAsString();
                // for  specific methodDeclaration in annotation such as @PostMapping
                if (CRITICAL_MAPPING_SET.contains(annName)) {
                    criticalMethods.add(new ComponentMethodNode(methodDeclaration, methodDeclaration.getNameAsString(), controllerContainer.getClassComponent(), false));
                    break;
                }
//                 for @RequestMapping - specific methodDeclaration as parameter
                else if (annName.equals(REQUEST_MAPPING)) {
                    for (MemberValuePair pair : annotationExpr.toNormalAnnotationExpr().orElse(new NormalAnnotationExpr()).getPairs()) {
                        if (pair.getName().asString().equals(METHOD_STRING) && CRITICAL_REQUEST_METHOD_SET.contains(pair.getValue().toString())) {
                            criticalMethods.add(new ComponentMethodNode(methodDeclaration, methodDeclaration.getNameAsString(), controllerContainer.getClassComponent(), false));
                            break annotationLoop;
                        }
                    }
                }
            }
        });
        controllerContainer.setCriticalMethods(criticalMethods);
    }


    /**
     * for each critical method traverse through all parents and check if final endpoint has at least some role from set of roles of critical method
     * if not add comment to method that not contain proper security annotation role(s)
     */
    private void traverseCriticalMethods() {
        componentMethodNodeOfTreeSet.stream()
                .filter(ComponentMethodNode::isCriticalMethod)
                .filter(componentMethodNode -> !componentMethodNode.getParentSecurityAnnotationMap().isEmpty())
                .forEach(componentMethodNode -> componentMethodNode.getParentComponentMethodNodeList()
                        .forEach(parent -> findInconsistencyOfParentsMethod(componentMethodNode.getParentSecurityAnnotationMap(), parent)));
    }

    /**
     *
     */
    private void findInconsistencyOfParentsMethod(Map<String, SecurityAnnotation> securityAnnotationMap, ComponentMethodNode componentMethodNode) {
        for (Map.Entry<String, SecurityAnnotation> stringSecurityAnnotationEntry : componentMethodNode.getSecurityAnnotationMap().entrySet()) {
            // componentMethodNode contains at least one security annotation for this path
            if (securityAnnotationMap.containsKey(stringSecurityAnnotationEntry.getKey())) {
                return;
            }
        }

        // component method node does not have parent node and it is not used any security annotation
        // than add comment to this method declaration about security inconsistency
        // and suggest annotation that will fix it
        if (componentMethodNode.getParentComponentMethodNodeList().isEmpty()) {
            for (Map.Entry<String, SecurityAnnotation> entry : securityAnnotationMap.entrySet()) {
                componentMethodNode.addSecurityAnnotationSuggestion(entry.getValue());
            }
            inconsistentComponentMethodMap.add(componentMethodNode);
        }

        componentMethodNode.getParentComponentMethodNodeList().forEach(parent -> findInconsistencyOfParentsMethod(securityAnnotationMap, parent));
    }

    /**
     * for each inconsistent component method create comment and add to compilation unit
     * and for each compilation unit save it to file
     */
    private void saveSuggestedAnnotationsToFile() {
        inconsistentComponentMethodMap.forEach(componentMethodNode -> {

            StringBuilder comment = new StringBuilder();
            comment.append("\n")
                    .append("\tTODO - SecurityAnalyzer found potentially security inconsistency\n")
                    .append("\tSome suggestions to repair it:\n");
            for (Map.Entry<String, SecurityAnnotation> entry : componentMethodNode.getSecurityAnnotationSuggestionMap().entrySet()) {
                comment.append("\t")
                        .append(entry.getKey())
                        .append(" // from ")
                        .append(getComponentName(entry.getValue().getParentNode().getClassComponent()))
                        .append(".")
                        .append(entry.getValue().getParentNode().getMethodName())
                        .append("\n");
            }
            comment.append("\t");
            addCommentToCompilationUnit(componentMethodNode.getClassComponent(), componentMethodNode.getMethodName(), new BlockComment(comment.toString()));
        });


        inconsistentComponentMethodMap.stream().filter(distinctByKey(p -> getComponentName(p.getClassComponent()))).forEach(componentMethodNode -> {
            CompilationUnit compilationUnit;
            if (componentMethodNode.getClassComponent().asClassComponent() != null)
                compilationUnit = componentMethodNode.getClassComponent().asClassComponent().getCompilationUnit();
            else
                compilationUnit = componentMethodNode.getClassComponent().asInterfaceComponent().getCompilationUnit();
            try {
                saveCompilationUnitToFile(componentMethodNode.getClassComponent().getPath(), compilationUnit);
            } catch (IOException e) {

                e.printStackTrace();
            }
        });
    }


    /**
     * find all sub methods for one specific methodDeclaration of ComponentMethodNode
     * and set this list as subMethods list
     *
     * @param componentMethodNode
     */
    private void findAndSetSubMethodsForComponentMethodNode(ComponentMethodNode componentMethodNode) {
        List<ComponentMethodNode> subComponentMethodNodeList = new ArrayList<>();
        List<MethodCallExpr> list = getAllSubMethods(componentMethodNode.getMethodDeclaration());
        for (MethodCallExpr methodCallExpr : list) {
            // javaSymbolSolver is successful - method was found
            try {
                methodCallExpr.resolve();
                ComponentMethodNode componentSubMethodNode = findProperClassComponentFromSubMethod(componentMethodNode, methodCallExpr);
                if (componentSubMethodNode != null) {
                    subComponentMethodNodeList.add(componentSubMethodNode);
                }
            } catch (Exception e) {
                // JavaSymboler did not resolve this methodExpr - do it manually
                // interested only in critical methodsExpr - potentially repository critical method (such as save, delete...)
                if (CRITICAL_REPOSITORY_METHOD_SET.contains(methodCallExpr.getName().asString()) && componentMethodNode.getClassComponent().asClassComponent() != null && methodCallExpr.getScope().isPresent()) {
                    for (FieldComponent fieldComponent : componentMethodNode.getClassComponent().asClassComponent().getFieldComponents()) {
                        // methodExpr is method of this field variable
                        // find this classComponent from analysisContext and check that it has @Repository annotation
                        if (fieldComponent.getFieldName().equals(methodCallExpr.getScope().get().toString())) {
                            for (Component searchComponent : analysisContext.getClassesAndInterfaces()) {
                                if (searchComponent.asInterfaceComponent() != null) {
                                    // interfaceComponent is equal from field variable
                                    if (searchComponent.asInterfaceComponent().getContainerName().equals(fieldComponent.getType())) {
                                        // find out if interfaceComponent has class annotation @Repository
                                        for (Component annotation : searchComponent.asInterfaceComponent().getAnnotations()) {
                                            // interfaceComponent has annotation @Repository
                                            if (annotation.asAnnotationComponent().getAsString().equals(REPOSITORY_ANNOTATION)) {
                                                ComponentMethodNode componentSubMethodNode = createComponentMethodNode(null,
                                                        methodCallExpr.getName().asString(), searchComponent, componentMethodNode,
                                                        isMethodCritical(methodCallExpr.getName().asString(), searchComponent));
                                                subComponentMethodNodeList.add(componentSubMethodNode);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
        componentMethodNode.setSubMethods(subComponentMethodNodeList);
    }


    /**
     * find proper class componet and its proper method from sub method and wrap it to ComponentMethodNode
     *
     * @param methodCallExpr
     * @return
     */
    private ComponentMethodNode findProperClassComponentFromSubMethod(ComponentMethodNode parent, MethodCallExpr
            methodCallExpr) {
        for (Component component : analysisContext.getClassesAndInterfaces()) {
            CompilationUnit cu;
            if (component.asInterfaceComponent() != null) {
                cu = component.asInterfaceComponent().getCompilationUnit();
            } else {
                cu = component.asClassComponent().getCompilationUnit();
            }

            // get all methods
            List<MethodDeclaration> methodDeclarationArrayList = new ArrayList<>();
            VoidVisitor<List<MethodDeclaration>> methodClassVisitor = new Visitor.MethodClassVisitor();
            methodClassVisitor.visit(cu, methodDeclarationArrayList);

            // for each method try that match with methoCallExpr
            for (MethodDeclaration methodDeclaration : methodDeclarationArrayList) {
                try {

                    if (methodDeclaration.resolve().getQualifiedSignature().equals(methodCallExpr.resolve().getQualifiedSignature())) {
                        // if component is Interface component find its implementation
                        if (component.asInterfaceComponent() != null) {
                            return createComponentMethodNodeFomInterfaceComponent(methodDeclaration, component, parent);
                        }
                        return createComponentMethodNode(methodDeclaration, methodDeclaration.getNameAsString(), component, parent, isMethodCritical(methodDeclaration.getNameAsString(), component));
                    }
                } catch (UnsolvedSymbolException e) {
                    logger.warn("UnsolvedSymbolException " + methodDeclaration.getNameAsString() + " was not solved by JavaSymboler.");
                }
            }
        }
        return null;
    }


    /**
     * find all subMethods which are called in this method
     *
     * @param methodDeclaration
     * @return
     */
    private List<MethodCallExpr> getAllSubMethods(MethodDeclaration methodDeclaration) {
        List<MethodCallExpr> methodCallExprList = new ArrayList<>();
        if (methodDeclaration == null)
            return methodCallExprList;

        methodDeclaration.accept(new Visitor.MethodCallVisitor(), methodCallExprList);
        return methodCallExprList;
    }

    /**
     * return true is method component critical method (f.e.: annotation @PostRequest, method PUT, POST, DELETE)
     */
    private boolean isMethodCritical(MethodInfoComponent methodInfoComponent) {
        for (Component ann : methodInfoComponent.getAnnotations()) {
            String annName = ann.asAnnotationComponent().getAsString();
            // for  specific method in annotation such as @PostMapping
            if (CRITICAL_MAPPING_SET.contains(annName)) {
                return true;
            }
            // for @RequestMapping - specific method as parameter
            else if (annName.equals(REQUEST_MAPPING)) {
                for (AnnotationValuePair pair : ann.asAnnotationComponent().getAnnotationValuePairList()) {
                    if (pair.getKey().equals(METHOD_STRING) && CRITICAL_REQUEST_METHOD_SET.contains(pair.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * get all class components with annotation "@Controller" (REST_CONTROLLER, etc.)
     *
     * @return
     */
    private List<ControllerContainer> findControllersByAnnotation() {
        List<ControllerContainer> controllerList = new ArrayList<>();

        analysisContext.getClassNames().forEach(className -> {
            Component classComponent = analysisContext.getClassByName(className);

            // get all class annotations
            List<AnnotationExpr> annotationClassList = new ArrayList<>();
            VoidVisitor<List<AnnotationExpr>> annotationClassVisitor = new ContainerClassCU.AnnotationClassVisitor();
            annotationClassVisitor.visit(classComponent.asClassComponent().getCompilationUnit(), annotationClassList);

            for (AnnotationExpr ann : annotationClassList) {
                if (ann.getName().asString().contains("Controller")) {
                    controllerList.add(new ControllerContainer(classComponent));
                    break;
                }

            }
        });
        return controllerList;
    }


    /**
     * set list of field variables of class component
     * change interface component into class component of its implementation if exists
     * class component field variables remain same
     *
     * @param controllerContainer
     */
    private void setClassContainerFromField(ControllerContainer controllerContainer) {
        List<Component> classComponetsFromFieldList = new ArrayList<>();
        for (FieldComponent fieldComponent : controllerContainer.getClassComponent().asClassComponent().getFieldComponents()) {
            Component par = getClassComponentFromClassOrInterfaceByName(fieldComponent.getType());
            if (par != null) {
                classComponetsFromFieldList.add(par);
            }
        }
        controllerContainer.setClassComponentsFromFieldsList(classComponetsFromFieldList);
    }


    /**
     * add comment to specific method of component
     *
     * @param component
     * @param methodName
     * @param comment
     * @throws IOException
     */
    private void addCommentToCompilationUnit(Component component, String methodName, Comment comment) {
        // find all methodDeclarations for class component
        List<MethodDeclaration> methodDeclarationArrayList = new ArrayList<>();
        VoidVisitor<List<MethodDeclaration>> methodClassVisitor = new Visitor.MethodClassVisitor();
        methodClassVisitor.visit(component.asClassComponent().getCompilationUnit(), methodDeclarationArrayList);

        // find proper method to add comment
        methodDeclarationArrayList.forEach(method -> {
            if (method.getNameAsString().equals(methodName))
                method.setComment(comment);
        });
    }


    /**
     * save compilationUnit to file based on path
     *
     * @param filePath
     * @param compilationUnit
     * @throws IOException
     */
    private void saveCompilationUnitToFile(String filePath, CompilationUnit compilationUnit) throws IOException {
        fileHandler.saveCompilationUnitToFile(filePath, compilationUnit);
    }


    /**
     * get class component by name or class component which implements interface component by name
     *
     * @param name interface or class name
     * @return class component or null
     */
    private Component getClassComponentFromClassOrInterfaceByName(String name) {
        for (Component component : analysisContext.getClassesAndInterfaces()) {
            if (component.asInterfaceComponent() != null) {
                if (component.asInterfaceComponent().getContainerName().equalsIgnoreCase(name)) {
                    return findClassComponentImplementedInterface(component);
                }
            } else {
                if (component.asClassComponent().getClassName().equalsIgnoreCase(name)) {
                    return component;
                }
            }
        }
        return null;
    }

    /**
     * for interface component find its implementation and set properly methodDeclaration and class component
     * if its implementation is not found return interfaceComponent
     *
     * @param component
     * @return
     */
    private ComponentMethodNode createComponentMethodNodeFomInterfaceComponent(MethodDeclaration methodDeclaration,
                                                                               Component component, ComponentMethodNode parent) {
        Component classComponent = findClassComponentImplementedInterface(component);
        if (classComponent == null) {
            return createComponentMethodNode(methodDeclaration, methodDeclaration.getNameAsString(), component, parent, isMethodCritical(methodDeclaration.getNameAsString(), component));
        }

        List<MethodDeclaration> methodDeclarationArrayList = new ArrayList<>();
        VoidVisitor<List<MethodDeclaration>> methodClassVisitor = new Visitor.MethodClassVisitor();
        methodClassVisitor.visit(classComponent.asClassComponent().getCompilationUnit(), methodDeclarationArrayList);

        String className = classComponent.asClassComponent().getClassName();
        for (MethodDeclaration method : methodDeclarationArrayList) {
            String methodFullName = method.resolve().getQualifiedSignature();
            String methodNameWithParams = methodFullName.substring(methodFullName.indexOf(className) + className.length());
            if (methodDeclaration.resolve().getQualifiedSignature().endsWith(methodNameWithParams)) {
                return createComponentMethodNode(method, methodDeclaration.getNameAsString(), classComponent, parent, isMethodCritical(methodDeclaration.getNameAsString(), classComponent));
            }

        }
        return null;
    }

    /**
     * find class component which implements interface component or return null
     *
     * @param interfaceComponent
     * @return
     */
    private Component findClassComponentImplementedInterface(Component interfaceComponent) {
        List<String> implementsList = new ArrayList<>();

        for (ClassComponent classComponent : analysisContext.getClasses()) {
            VoidVisitor<List<String>> methodClassVisitor = new Visitor.ImplementsClassVisitor();
            methodClassVisitor.visit(classComponent.getCompilationUnit(), implementsList);

            for (String impl : implementsList) {
                if (impl.equals(interfaceComponent.asInterfaceComponent().getContainerName())) {
                    return classComponent;
                }
            }
            implementsList.clear();
        }
        return null;
    }

    /**
     * check if methodDeclaration has security annotation(s) and parse from that role names
     *
     * @param methodDeclaration
     * @return
     */
    private Map<String, SecurityAnnotation> getRolesFromSecurityAnnotationsMethodDeclaration(MethodDeclaration methodDeclaration, ComponentMethodNode componentMethodNode) {
        if (methodDeclaration == null)
            return new HashMap<>();

        Map<String, SecurityAnnotation> roles = new HashMap<>();
        List<AnnotationExpr> annotationExprList = new ArrayList<>();
        VoidVisitor<List<AnnotationExpr>> annotationClassVisitor = new Visitor.AnnotationMethodVisitor();
        annotationClassVisitor.visit(methodDeclaration, annotationExprList);
        for (AnnotationExpr annotationExpr : annotationExprList) {

            if (annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_SECURED) || annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_ROLES_ALLOWED)) {
                if (annotationExpr.isSingleMemberAnnotationExpr()) {
                    Set<String> rolesOfAnnotation = Arrays.stream(annotationExpr.asSingleMemberAnnotationExpr().getMemberValue().toString().split(","))
                            .map(chunk -> chunk.replaceAll("[^A-Za-z_]", "")).collect(Collectors.toSet());
                    roles.put(annotationExpr.toString(), new SecurityAnnotation(annotationExpr.toString(), rolesOfAnnotation, componentMethodNode));
                }
            } else if (annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_PREAUTHORIZE) || annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_POSTAUTHORIZE)) {
                Set<String> rolesOfAnnotation = Arrays.stream(annotationExpr.asSingleMemberAnnotationExpr().getMemberValue().toString().split("or|and"))
                        .filter(chunk -> chunk.contains(SECURITY_HAS_ROLE))
                        .map(chunk -> chunk.substring(chunk.indexOf(SECURITY_HAS_ROLE) + SECURITY_HAS_ROLE.length()))
                        .map(chunk -> chunk.replaceAll("[^A-Za-z_]", "")).collect(Collectors.toSet());
                roles.put(annotationExpr.toString(), new SecurityAnnotation(annotationExpr.toString(), rolesOfAnnotation, componentMethodNode));


            }
        }
        return roles;

    }


    /**
     * create new componentMethodNode or find already created one and add parent component
     * also find and set roles
     *
     * @param methodDeclaration
     * @param methodName
     * @param component
     * @param parentComponent
     * @param critical
     * @return
     */
    private ComponentMethodNode createComponentMethodNode(MethodDeclaration methodDeclaration, String methodName,
                                                          Component component, ComponentMethodNode parentComponent, boolean critical) {
        for (ComponentMethodNode componentMethodNode : componentMethodNodeOfTreeSet) {
            // already created componentMethodNode
            if (componentMethodNode.getMethodDeclaration() == methodDeclaration && componentMethodNode.getMethodName().equals(methodName)) {
                if (getComponentName(componentMethodNode.getClassComponent()).equals(getComponentName(component))) {
                    componentMethodNode.addParentClassComponent(parentComponent);
                    // add all roles of parent
                    if (parentComponent != null) {
                        parentComponent.getParentSecurityAnnotationMap().values().forEach(componentMethodNode::addParentSecurityAnnotation);
                    }
                    return componentMethodNode;
                }
            }
        }
        ComponentMethodNode componentMethodNode = new ComponentMethodNode(methodDeclaration, methodName, component, parentComponent, critical);

        // find all Security annotations
        Map<String, SecurityAnnotation> roles = getRolesFromSecurityAnnotationsMethodDeclaration(methodDeclaration, componentMethodNode);
        componentMethodNode.setParentSecurityAnnotationMap(roles);
        componentMethodNode.setSecurityAnnotationMap(roles);
        // add all roles of parent
        if (parentComponent != null) {
            parentComponent.getParentSecurityAnnotationMap().values().forEach(componentMethodNode::addParentSecurityAnnotation);
        }
        componentMethodNodeOfTreeSet.add(componentMethodNode);
        return componentMethodNode;
    }


    /**
     * method is critical if class/interface has repository annotation and method is critical (save or delete methods in db)
     *
     * @param methodName
     * @param component
     * @return
     */
    private boolean isMethodCritical(String methodName, Component component) {
        List<Component> annotations = new ArrayList<>();

        if (component.asInterfaceComponent() != null) {
            annotations = component.asInterfaceComponent().getAnnotations();
        } else if (component.asClassComponent() != null) {
            annotations = component.asClassComponent().getAnnotations();
        }

        boolean hasRepositoryAnnotation = false;
        for (Component ann : annotations) {
            if (ann.asAnnotationComponent().getAsString().equals(REPOSITORY_ANNOTATION)) {
                hasRepositoryAnnotation = true;
                break;
            }
        }
        // component does not have @Repository annotation
        if (!hasRepositoryAnnotation)
            return false;

        for (String criticalMethod : CRITICAL_REPOSITORY_METHOD_SET) {
            if (methodName.startsWith(criticalMethod)) {
                return true;
            }
        }
        return false;
    }

    /**
     * get name for interface or class component
     *
     * @param component
     * @return
     */
    private String getComponentName(Component component) {
        if (component.asInterfaceComponent() != null)
            return component.asInterfaceComponent().getContainerName();
        if (component.asClassComponent() != null)
            return component.asClassComponent().getClassName();
        return "";
    }



    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }


    private void printAllClassAnnotations() {
        containerClassCUList.forEach(cu -> {
            for (AnnotationExpr annotationExpr : cu.getClassAnnotations()) {
                System.out.println("\t" + annotationExpr.getName());
            }

        });
        System.out.println("}");
    }


    /**
     * print created graph of dependencies for controllerContainers
     */
    private void printAllControllerClassNamesWithCriticalMethods() {
        System.out.println("printAllClassNames(){");
        controllerContainerList.forEach(controllerContainer -> {
            System.out.println(controllerContainer.getClassComponent().asClassComponent().getClassName());
            controllerContainer.getCriticalMethods().forEach(method -> {
                printSubMethods(method, 1);
            });
        });

        System.out.println("}");
    }

    private void printSubMethods(ComponentMethodNode componentMethodNode, int i) {
        StringBuilder padding = new StringBuilder();
        for (int j = 0; j < i; j++) {
            padding.append("\t");
        }
        System.out.print(padding + "[");
        componentMethodNode.getParentComponentMethodNodeList().forEach(componentMethodNode1 -> {
            System.out.print(getComponentName(componentMethodNode1.getClassComponent()) + "." + componentMethodNode1.getMethodName() + ", ");
        });
        System.out.print("]");

        if (componentMethodNode.getMethodDeclaration() == null) {
            System.out.print(getComponentName(componentMethodNode.getClassComponent()) + "." + componentMethodNode.getMethodName());
            System.out.print("[" + componentMethodNode.isCriticalMethod() + "] ");
            System.out.print("{");
            componentMethodNode.getParentSecurityAnnotationMap().keySet().forEach(role -> System.out.print(role + ", "));
            System.out.println("}");
        } else {
            System.out.print(componentMethodNode.getMethodDeclaration().resolve().getQualifiedSignature());
            System.out.print("[" + componentMethodNode.isCriticalMethod() + "]");
            System.out.print("{");
            componentMethodNode.getParentSecurityAnnotationMap().keySet().forEach(role -> System.out.print(role + ", "));
            System.out.println("}");
        }
        for (ComponentMethodNode subComponentMethod : componentMethodNode.getSubMethods()) {
            printSubMethods(subComponentMethod, i + 1);
        }

    }


    private void printAllInterfacesOrClassNamesWithMethods() {
        System.out.println("printAllClassNames(){");

        analysisContext.getClassesAndInterfaces().forEach(component -> {
            if (component.asInterfaceComponent() != null) {
                System.out.println("\tINTERFACE NAME = " + component.asInterfaceComponent().getContainerName());
                System.out.println("\t\tMethods " + component.asInterfaceComponent().getMethods().size());
                component.asInterfaceComponent().getMethods().forEach(m -> {
                    System.out.println("\t" + m.asMethodInfoComponent().getMethodName());
                });
            }
            // component is class
            else {
                System.out.println("\tCLASS NAME = " + component.asClassComponent().getClassName());
                System.out.println("\t\tMethods");
                component.asClassComponent().getMethods().forEach(m -> {
                    System.out.println("\t" + m.asMethodInfoComponent().getMethodName());
                });
            }


        });

        System.out.println("}");
    }


}
