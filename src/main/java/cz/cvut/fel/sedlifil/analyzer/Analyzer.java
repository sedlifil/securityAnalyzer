package cz.cvut.fel.sedlifil.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
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
import cz.cvut.fel.sedlifil.fileHandler.IFileHandler;
import cz.cvut.fel.sedlifil.visitor.Visitor;
import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.component.context.AnalysisContext;
import edu.baylor.ecs.jparser.component.impl.ClassComponent;
import edu.baylor.ecs.jparser.component.impl.FieldComponent;
import edu.baylor.ecs.jparser.component.impl.MethodInfoComponent;
import edu.baylor.ecs.jparser.component.impl.ModuleComponent;
import edu.baylor.ecs.jparser.factory.container.impl.ModuleComponentFactory;
import edu.baylor.ecs.jparser.factory.context.AnalysisContextFactory;
import edu.baylor.ecs.jparser.factory.directory.DirectoryFactory;
import edu.baylor.ecs.jparser.model.AnnotationValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
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


    public Analyzer(IFileHandler fileHandler, String sourcePathOfApplication) throws FileNotFoundException {
        this.fileHandler = fileHandler;
        this.sourcePathOfApplication = sourcePathOfApplication;
        containerClassCUList = new ArrayList<>();
        criticalAnnotationSet = new HashSet<>();
        componentMethodNodeOfTreeSet = new HashSet<>();


        DirectoryFactory directoryFactory = new DirectoryFactory();
        AnalysisContextFactory analysisFactory = new AnalysisContextFactory();

// Creates a directory graph from the DirectoryFactory from a string path to the directory in question.
        Component directoryGraph = directoryFactory.createDirectoryGraph("/Users/filip/Dropbox/palmTourism/palmTourism/src");

        directoryGraph.getSubComponents().forEach(sub -> {
            System.out.println(sub.getInstanceName());
        });

//
        ModuleComponentFactory moduleFactory = ModuleComponentFactory.getInstance();
        ModuleComponent moduleGraph = moduleFactory.createComponent(null, directoryGraph);


//
//        // Lastly, create the full AnalysisContext object as such:
        analysisContext = analysisFactory.createAnalysisContextFromDirectoryGraph(directoryGraph);
//        analysisContext.getMethods().forEach(m -> {
//            System.out.println(m.asMethodInfoComponent().getId() + " -> " + m.asMethodInfoComponent().getMethodName() + " - " + m.asMethodInfoComponent().getParent().asClassComponent().getClassName());
//            m.asMethodInfoComponent().getSubMethods().forEach(submet -> {
//                System.out.println("\t" + submet.getId() + " -> " + submet);
//            });
//        });


        // set Java SymbolSolver
        try {
            TypeSolver myTypeSolver = new CombinedTypeSolver(
                    new ReflectionTypeSolver(), JarTypeSolver.getJarTypeSolver("/Users/filip/Dropbox/palmTourism/palmTourism/target/palmTourism-0.0.1-SNAPSHOT.jar"),
                    new JavaParserTypeSolver(new File("/Users/filip/Dropbox/palmTourism/palmTourism/src/main/java")));

            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(myTypeSolver);
            StaticJavaParser
                    .getConfiguration()
                    .setSymbolResolver(symbolSolver);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // for each class or interface component set new compilation unit with Java SymbolSolver
        System.out.println("CLASS -> METHOD -> PARAMETERS");
        for (Component _component : analysisContext.getClassesAndInterfaces()) {
            System.out.println();
            if (_component.asInterfaceComponent() != null) {
                CompilationUnit cu = StaticJavaParser.parse(new File(_component.asInterfaceComponent().getPath()));
                _component.asInterfaceComponent().setCompilationUnit(cu);
//                _component.asInterfaceComponent().getMethods().forEach(method -> {
//                    System.out.println(_component.asInterfaceComponent().getContainerName() + " x-> " + method.asMethodInfoComponent().getMethodName() + " - " + method.asMethodInfoComponent().getMethodParams());
//                });
            } else {
                CompilationUnit cu = StaticJavaParser.parse(new File(_component.asClassComponent().getPath()));
                _component.asClassComponent().setCompilationUnit(cu);

//                _component.asClassComponent().getMethods().forEach(method -> {
//                    System.out.println(_component.asClassComponent().getClassName() + " -> " + method.asMethodInfoComponent().getMethodName() + " - " + method.asMethodInfoComponent().getMethodParams());
//                });
            }


        }

    }


    public void analyzeApplicationSecurity() {
        filesFromPathList = fileHandler.getAllFilesFromPAth(sourcePathOfApplication);

//        loadAllJavaClassesFromFiles();
//
//        printAllClassAnnotations();

//        jparserDirectory();

//        getAllClasses();


        controllerContainerList = findControllersByAnnotation();

        // fill in controller container with critical methods
//        controllerContainerList.forEach(this::getAllCriticalMethod); // todo pouzit nekde critical methods

        // change interface component field variables into their implementation (into class component)
        // class component field variables remain same
        controllerContainerList.forEach(this::setClassContainerFromField);


        // todo jen jedna trida -> pozdeji smazat
//        createTreeOfSubMethodsForClassComponent(controllerContainerList.get(0));

        // todo odkomentovat
        createTreeOfSubMethodsForControllers();

//        printAllInterfacesOrClassNamesWithMethods();

//        try {
//            addComment(controllerList.get(0), "getAllCarPlates", new BlockComment("TODO ANALYZER"));
//        } catch (IOException e) {
//            // TODO DODELAT NEJAKY EXCEPTION
//            e.printStackTrace();
//        }

        printAllControllerClassNamesWithCriticalMethods();


    }


    private void loadAllJavaClassesFromFiles() {
        filesFromPathList.forEach(path -> {
            try {
                ContainerClassCU containerClassCU = new ContainerClassCU(path);
                containerClassCUList.add(containerClassCU);
            } catch (FileNotFoundException e) {
                logger.error("Error: can not open file " + path);
                logger.info("JavaParser ends with error status.");
                System.exit(13);
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }


    private void jparserDirectory() {
        // Create the directory factory, uses default constructor and sets the language type to "Java" by default

//        analysisContext.getClassNames().forEach(className -> {
//            Component component = analysisContext.getClassByName(className);
//            System.out.println("CLASS = " + component.getInstanceName());
//            System.out.print("parent :");
//            if (component.getParent() != null) {
//                System.out.println(component.getParent());
//            }

//            component.asClassComponent().getMethods().forEach(method -> {
//                System.out.println("\t" + method.asMethodInfoComponent().getMethodName());
//                System.out.print("ANNOTATIONS: ");
//                method.asMethodInfoComponent().getAnnotations().forEach(ann -> System.out.print("\t" + ann.asAnnotationComponent().getAsString() + ", "));
//                System.out.println();
//            });
//        });
    }

    // todo jen pro zkouseni
    private void getAllClasses() {
        System.out.println(" ALL CLASSES");
        analysisContext.getClassesAndInterfaces().forEach(component -> {

            System.out.print("CLASS = " + component.getInstanceType() + "-" + component.getInstanceName());
            System.out.print(", parent :");
            if (component.getParent() != null) {
                System.out.println(component.getParent().getInstanceName());
            } else
                System.out.println();

        });
    }

    /**
     * for all found Controllers create their sub methods tree
     */
    private void createTreeOfSubMethodsForControllers() {
        // TODO ZMENIT
        controllerContainerList.forEach(controllerContainer -> {
//            if(controllerContainer.getClassComponent().asClassComponent().getClassName().equals("FDocumentController"))
            createTreeOfSubMethodsForClassComponent(controllerContainer);
        });
//        createTreeOfSubMethodsForClassComponent(controllerContainerList.get(0));
    }


    private void createTreeOfSubMethodsForClassComponent(ControllerContainer controllerContainer) {
        System.out.println("\n\n\n");
        System.out.println("createTreeOfSubMethodsForClassComponent");
        System.out.println(controllerContainer.getClassComponent().asClassComponent().getClassName());
//        methodComponent.getMethodParams().forEach(methodParamComponent -> System.out.println(methodParamComponent.getParameterName() + " -> " + methodParamComponent.getParameterType()));
//        System.out.println(methodComponent);

//        System.out.println(methodComponent.getRawSource());
//        System.out.println(methodComponent.getRawSourceStripped().size());
//        methodComponent.getRawSourceStripped().forEach(strip -> System.out.println(strip));

//        for (MethodInfoComponent subMethod : methodComponent.getSubMethods()) {
//            System.out.println("\t\t" + subMethod.getId() + " -> " + subMethod.getMethodName() + subMethod);
//        }


//        List<MethodCallExpr> methodCallExprList2 = new ArrayList<>();
//
//        controllerContainer.getClassComponent().asClassComponent().getCompilationUnit().findAll(MethodDeclaration.class).forEach(ae -> {
////                ResolvedType resolvedType = ae.calculateResolvedType();
//            System.out.println(ae.toString() + " is a: " + ae.getParameters().size());
//            ae.getParameters().forEach(argument -> {
//                System.out.println("\t" + argument.getType());
//            });
//
//            ae.accept(new Visitor.MethodCallVisitor(), methodCallExprList2);
//            methodCallExprList2.forEach(methodCallExpr -> {
//                System.out.println(methodCallExpr.getScope() + "->" + methodCallExpr.getName() + methodCallExpr.getArguments().size());
//                if (!methodCallExpr.getScope().get().toString().contains("LOGGER")) {
//                    methodCallExpr.resolve();
//
//                }
//
//            });
//            methodCallExprList2.clear();
//        });

//            cu.findAll(MethodCallExpr.class).forEach(ae -> {
////                ResolvedType resolvedType = ae.calculateResolvedType();
//                System.out.println(ae.toString() + " is a: " + ae.getParentNode().get());
//                ae.getArguments().forEach(argument -> {
//                    System.out.println("\t" + argument);
//                });
//                if (!ae.getScope().get().toString().contains("Logger") && !ae.getScope().get().toString().contains("LOGGER")) {
//                    System.out.println("AE = " + ae.resolve().getQualifiedSignature());
//                }
//            });

//        controllerContainer.getCriticalMethods().forEach(method -> {
//            System.out.println(method.getMethodDeclaration().getNameAsString());
//        });


        // evaluate critical methods

        evaluateCriticalMethods(controllerContainer);
        //for each critical method find sub methods
        for (ComponentMethodNode componentMethodNode : controllerContainer.getCriticalMethods()) {
            findAndSetSubMethodsForComponentMethodNode(componentMethodNode);
        }

        controllerContainer.getCriticalMethods().forEach(ctiricalMethod -> {
            ctiricalMethod.getSubMethods().forEach(subMethod -> {
                createTreeOfSubMethodsForComponentMethodNode(subMethod);
            });
        });

//        System.out.println("=============================");
//        controllerContainer.getCriticalMethods().forEach(criticalMethod -> {
//            System.out.println(criticalMethod.getMethodDeclaration().getNameAsString());
//            criticalMethod.getSubMethods().forEach(subMethod -> {
//                System.out.println("\t" + subMethod.getMethodDeclaration().getNameAsString());
//            });
//        });
//        System.out.println("=============================");


        // for each critical method find subMethods  -> OLD IMPLEMENTATION WHEN controller container had critical method as methodComponent
//        for (MethodDeclaration method : controllerContainer.getClassComponent().asClassComponent().getCompilationUnit().findAll(MethodDeclaration.class)) {
//            if (controllerContainer.getCriticalMethods()
//                    .stream()
//                    .map(critM -> critM.getMethodDeclaration().asMethodInfoComponent().getMethodName()).collect(Collectors.toList()).contains(method.getNameAsString())) {
//                System.out.print("Method = " + method.resolve().getQualifiedSignature() + ", params = ");
//                for (int i = 0; i < method.getParameters().size(); i++) {
//                    System.out.print(method.getParameters().get(i).getName() + " ");
//                    System.out.print(method.getParameters().get(i).getType() + " ");
//
//                }
//                System.out.println();
//                System.out.println("    " + method.resolve().getQualifiedSignature());
//                List<MethodCallExpr> list = getAllSubMethods(method);
//                for (MethodCallExpr methodCallExpr : list) {
//                    System.out.println("                " + methodCallExpr.getScope() + "->" + methodCallExpr.getName() + methodCallExpr.getArguments().size());
//                    System.out.println("                " + methodCallExpr.resolve().getQualifiedSignature());
//                    System.out.println("                    "+ methodCallExpr.resolve().getName());
//                    System.out.println("                    "+ methodCallExpr.resolve().getClassName());
//                    for (int i = 0; i < methodCallExpr.resolve().getNumberOfParams(); i++) {
//                        System.out.println("                        "+ methodCallExpr.resolve().getParam(i).getName());
//                        System.out.println("                        "+ methodCallExpr.resolve().getParam(i).getType());
//                    }
//                }
//            }
//        }
    }


    private void createTreeOfSubMethodsForComponentMethodNode(ComponentMethodNode componentMethodNode) {

        System.out.println("createTreeOfSubMethodsForComponentMethodNode = " + componentMethodNode.getMethodName());

        if (componentMethodNode.getClassComponent().asInterfaceComponent() != null)
            System.out.println(componentMethodNode.getClassComponent().asInterfaceComponent().getContainerName() + " - " + componentMethodNode.getMethodName());
        else
            System.out.println(componentMethodNode.getClassComponent().asClassComponent().getClassName() + " - " + componentMethodNode.getMethodName());

        findAndSetSubMethodsForComponentMethodNode(componentMethodNode);
        componentMethodNode.getSubMethods().forEach(sub -> {
            if (sub.getClassComponent().asInterfaceComponent() != null) {
                System.out.println("ISUB = " + sub.getClassComponent().asInterfaceComponent().getContainerName() + " -> " + sub.getMethodName());
            } else {
                System.out.println("SUB = " + sub.getClassComponent().asClassComponent().getClassName() + " -> " + sub.getMethodName());
            }
        });
        componentMethodNode.getSubMethods().forEach(subComponentMethodNode -> {
            createTreeOfSubMethodsForComponentMethodNode(subComponentMethodNode);
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
//            System.out.println("\t" + methodDeclaration.asMethodInfoComponent().getMethodName());
//            System.out.println("\t\tAnnotations:");

            // get all annotations for methoDeclaration
            List<AnnotationExpr> annotationExprs = new ArrayList<>();
            VoidVisitor<List<AnnotationExpr>> annotationMethodVisitor = new Visitor.AnnotationMethodVisitor();
            annotationMethodVisitor.visit(methodDeclaration, annotationExprs);

            annotationLoop:
            for (AnnotationExpr annotationExpr: annotationExprs){
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
                            criticalMethods.add(new ComponentMethodNode(methodDeclaration, methodDeclaration.getNameAsString(),controllerContainer.getClassComponent(), false));
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
    private void traverseCriticalMethods(){

        componentMethodNodeOfTreeSet.stream()
                .filter(ComponentMethodNode::isCriticalMethod)
                .forEach(componentMethodNode -> {
                    componentMethodNode.getParentComponentMethodNodeList().forEach(parent -> findInconsistencyOfParentsMethod(componentMethodNode.getRoles(), parent));

                });
    }

    /**
     *
     * @param roles
     * @param componentParentMethodNode
     */
    private void findInconsistencyOfParentsMethod(Set<String> roles, ComponentMethodNode componentParentMethodNode){
        Set<String> parentRoles = getRolesFromSecurityAnnotationsMethodDeclaration(componentParentMethodNode.getMethodDeclaration());


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
        System.out.println("LIST = " + list.size());
        for (MethodCallExpr methodCallExpr : list) {
            System.out.println(methodCallExpr.getName());
            // javaSymbolSolver is successful - method was found
            try {
                methodCallExpr.resolve();
            } catch (Exception e) {
                // todo zkusit sparovat repository field variable s metodou expr

                // this methodExpr is has to be critical
                if (CRITICAL_REPOSITORY_METHOD_SET.contains(methodCallExpr.getName().asString()) && componentMethodNode.getClassComponent().asClassComponent() != null && methodCallExpr.getScope().isPresent()) {
                    for (FieldComponent fieldComponent : componentMethodNode.getClassComponent().asClassComponent().getFieldComponents()) {
//                        System.out.println(fieldComponent.getFieldName() + " - " + fieldComponent.getType());
                        // metoda pochazi z interface z teto field variable
                        // najit tuto tridu z analysisContext a zkontrolovat ze ma anotaci @Repository
                        if (fieldComponent.getFieldName().equals(methodCallExpr.getScope().get().toString())) {
                            for (Component searchComponent : analysisContext.getClassesAndInterfaces()) {
                                if (searchComponent.asInterfaceComponent() != null) {
                                    // interface z tridni promenne se shoduji -> hledane interface je nalezene
//                                    System.out.println(searchComponent.asInterfaceComponent().getContainerName() + " vs. " +fieldComponent.getType());
                                    if (searchComponent.asInterfaceComponent().getContainerName().equals(fieldComponent.getType())) {
                                        // zjistit jestli nalezene interface ma tridni anotaci @repository
                                        System.out.println("QQQQQQ " + searchComponent.asInterfaceComponent().getContainerName());
                                        for (Component annotation : searchComponent.asInterfaceComponent().getAnnotations()) {
                                            System.out.println(annotation.asAnnotationComponent().getAsString());
                                            // this interface component has annotation @Repository
                                            if (annotation.asAnnotationComponent().getAsString().equals(REPOSITORY_ANNOTATION)) {
                                                ComponentMethodNode componentSubMethodNode = createComponentMethodNode(null, methodCallExpr.getName().asString(), searchComponent, componentMethodNode, isMethodCritical(methodCallExpr.getName().asString(), searchComponent));
                                                subComponentMethodNodeList.add(componentSubMethodNode);

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                System.out.print("EXCEPTION FOR METHOD: " + methodCallExpr.getNameAsString() + " - ");
                if (methodCallExpr.getScope().isPresent()) {
                    System.out.println(" - " + methodCallExpr.getScope().get());
                } else {
                    System.out.println(" - no scope");
                }
                //
                continue;
            }
            ComponentMethodNode componentSubMethodNode = findProperClassComponentFromSubMethod(componentMethodNode, methodCallExpr);
            if (componentSubMethodNode != null) {
                subComponentMethodNodeList.add(componentSubMethodNode);
            } else {
                System.out.println(" NULLLLLLLLLLLLLLL");
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
        System.out.println("findProperClassComponentFromSubMethod = " + methodCallExpr.getNameAsString());
        for (Component component : analysisContext.getClassesAndInterfaces()) {
            CompilationUnit cu;
            if (component.asInterfaceComponent() != null) {
                cu = component.asInterfaceComponent().getCompilationUnit();
//                System.out.println("INTERFACE = " + component.asInterfaceComponent().getContainerName());
            } else {
                cu = component.asClassComponent().getCompilationUnit();
//                System.out.println("CLASS = " + component.asClassComponent().getContainerName());
            }


            // get all methods
            List<MethodDeclaration> methodDeclarationArrayList = new ArrayList<>();
            VoidVisitor<List<MethodDeclaration>> methodClassVisitor = new Visitor.MethodClassVisitor();
            methodClassVisitor.visit(cu, methodDeclarationArrayList);

            // for each method try that match with methoCallExpr
            for (MethodDeclaration methodDeclaration : methodDeclarationArrayList) {
                try {
//                    System.out.println(methodDeclaration.resolve().getQualifiedSignature() + " vs. " + methodCallExpr.resolve().getQualifiedSignature());

                    if (methodDeclaration.resolve().getQualifiedSignature().equals(methodCallExpr.resolve().getQualifiedSignature())) {
                        System.out.println("FOUND");
                        // if component is Interface component find its implementation
                        if (component.asInterfaceComponent() != null) {
                            System.out.print("INTERFACE");
                            ComponentMethodNode componentMethodNode = createComponentMethodNodeFomInterfaceComponent(methodDeclaration, component, parent);
                            System.out.println(componentMethodNode);
                            return componentMethodNode;
                        }
                        System.out.println("CLASSSSS");
                        return createComponentMethodNode(methodDeclaration, methodDeclaration.getNameAsString(), component, parent, isMethodCritical(methodDeclaration.getNameAsString(), component));
                    }
                } catch (UnsolvedSymbolException e) {
//                    System.out.println(e.toString());
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
//        List<MethodCallExpr> methodCallExprListResult = new ArrayList<>();
//
        if (methodDeclaration == null)
            return methodCallExprList;

        methodDeclaration.accept(new Visitor.MethodCallVisitor(), methodCallExprList);
//        // for each sub method find its proper class component and called method component
//        for (MethodCallExpr methodCallExpr : methodCallExprList) {
//            if (!methodCallExpr.getScope().get().toString().contains("LOGGER")) {
//                methodCallExprListResult.add(methodCallExpr);
//            }
//        }
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
//                    System.out.println("\t\tCRITICAL ANN");
                return true;
            }
            // for @RequestMapping - specific method as parameter
            else if (annName.equals(REQUEST_MAPPING)) {
//                    System.out.print("\t\tREQUEST MAP ANN = ");
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
//            System.out.println(classComponent.asClassComponent().getClassName());
            List<AnnotationExpr> annotationClassList = new ArrayList<>();
            VoidVisitor<List<AnnotationExpr>> annotationClassVisitor = new ContainerClassCU.AnnotationClassVisitor();
            annotationClassVisitor.visit(classComponent.asClassComponent().getCompilationUnit(), annotationClassList);

            for (AnnotationExpr ann : annotationClassList) {
//                System.out.println("\t" + ann.getName());
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
     * change interace component into class component of its implemetation
     * class component field variables remain same
     *
     * @param controllerContainer
     */
    private void setClassContainerFromField(ControllerContainer controllerContainer) {

//        analysisContext.getClasses().forEach(cl -> {
//            System.out.println(cl.getClassName());
//        });
//        System.out.println();
//        System.out.println();
//        System.out.println();
//
//        System.out.println("TRY SUBMETHODS");
        System.out.println("setClassContainerFromField");
        System.out.println(controllerContainer.getClassComponent().asClassComponent().getClassName());
        System.out.println("    FIELDCOMPONENTS");

        List<Component> classComponetsFromFieldList = new ArrayList<>();
        for (FieldComponent fieldComponent : controllerContainer.getClassComponent().asClassComponent().getFieldComponents()) {
            System.out.println("\t" + fieldComponent);
            // get class component either as class component or class which implements interface name
            Component par = getClassComponentFromClassOrInterfaceByName(fieldComponent.getType());
            if (par != null) {
                classComponetsFromFieldList.add(par);
//                par.asClassComponent().getMethods().forEach(m -> {
//                    System.out.println("\t\t\t" + m.asMethodInfoComponent().getId() + m.asMethodInfoComponent().getMethodName());
//                });
            }
        }
        controllerContainer.setClassComponentsFromFieldsList(classComponetsFromFieldList);
        System.out.println("SETED field components");
        controllerContainer.getClassComponentsFromFieldsList().forEach(component -> {
            System.out.println("    " + component.asClassComponent().getClassName());
        });

    }


    /**
     * add comment to specific method of Class component
     *
     * @param component
     * @param methodName
     * @param comment
     * @throws IOException
     */
    private void addComment(Component component, String methodName, Comment comment) throws IOException {
        // find all methodDeclarations for class component
        List<MethodDeclaration> methodDeclarationArrayList = new ArrayList<>();
        VoidVisitor<List<MethodDeclaration>> methodClassVisitor = new Visitor.MethodClassVisitor();
        methodClassVisitor.visit(component.asClassComponent().getCompilationUnit(), methodDeclarationArrayList);

        // find proper method to add comment
        methodDeclarationArrayList.forEach(method -> {
            System.out.println(method.getNameAsString());
            if (method.getNameAsString().equals(methodName))
                method.setComment(comment);

        });

        fileHandler.saveCompilationUnitToFile(component.getPath(), component.asClassComponent().getCompilationUnit());
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
     *
     * @param component
     * @return
     */
    private ComponentMethodNode createComponentMethodNodeFomInterfaceComponent(MethodDeclaration methodDeclaration,
                                                                               Component component, ComponentMethodNode parent) {
        System.out.println("createComponentMethodNodeFomInterfaceComponent");
        Component classComponent = findClassComponentImplementedInterface(component);

        System.out.println("COKOLIV");
        // when implementation for interface component not found -> return this interface component
        if (classComponent == null) {
            System.out.println("classs is null");
            return createComponentMethodNode(methodDeclaration, methodDeclaration.getNameAsString(), component, parent, isMethodCritical(methodDeclaration.getNameAsString(), component));
        }

        System.out.println(classComponent.asClassComponent().getClassName());

        List<MethodDeclaration> methodDeclarationArrayList = new ArrayList<>();
        VoidVisitor<List<MethodDeclaration>> methodClassVisitor = new Visitor.MethodClassVisitor();
        methodClassVisitor.visit(classComponent.asClassComponent().getCompilationUnit(), methodDeclarationArrayList);

        System.out.println("list of size " + methodDeclarationArrayList.size());
        String className = classComponent.asClassComponent().getClassName();
        // find proper method to add comment
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

//            System.out.println("IMPLEMENTS FOUND OF CLASS " + classComponent.getClassName());
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
     * @param methodDeclaration
     * @return
     */
    private Set<String> getRolesFromSecurityAnnotationsMethodDeclaration(MethodDeclaration methodDeclaration) {
        if (methodDeclaration == null)
            return new HashSet<>();

//        System.out.println("METHOD DECLARATION = " + methodDeclaration.getNameAsString());

        Set<String> roles = new HashSet<>();
        List<AnnotationExpr> annotationExprList = new ArrayList<>();
        VoidVisitor<List<AnnotationExpr>> annotationClassVisitor = new Visitor.AnnotationMethodVisitor();
        annotationClassVisitor.visit(methodDeclaration, annotationExprList);
        for (AnnotationExpr annotationExpr : annotationExprList) {
//            System.out.println("ANN = " + annotationExpr.getNameAsString());

            if (annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_SECURED) || annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_ROLES_ALLOWED)) {
                if (annotationExpr.isSingleMemberAnnotationExpr()) {
//                    System.out.println("Single = " + annotationExpr.asSingleMemberAnnotationExpr().getName() + " - " + annotationExpr.asSingleMemberAnnotationExpr().getMemberValue());
//                    String[] list = annotationExpr.asSingleMemberAnnotationExpr().getMemberValue().toString().split(",");
//                    for (int i = 0; i < list.length; i++) {
//                        System.out.println(list[i]);
//                        System.out.println(list[i].substring(list[i].indexOf("\"") + 1, list[i].lastIndexOf("\"")));
//                        System.out.println(list[i].replaceAll("[^A-Za-z_]", ""));
//                    }

                    Set<String> rolesOfAnnotation = Arrays.stream(annotationExpr.asSingleMemberAnnotationExpr().getMemberValue().toString().split(","))
                            .map(chunk -> chunk.replaceAll("[^A-Za-z_]", "")).collect(Collectors.toSet());
                    roles.addAll(rolesOfAnnotation);
//                    roles.forEach(role -> System.out.println(role));
                }
            } else if (annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_PREAUTHORIZE) || annotationExpr.getNameAsString().equals(SECURITY_ANNOTATION_POSTAUTHORIZE)) {
//                System.out.println("PRE OR POST AUTHORIZE");
                Set<String> rolesOfAnnotation = Arrays.stream(annotationExpr.asSingleMemberAnnotationExpr().getMemberValue().toString().split("or|and"))
                        .filter(chunk -> chunk.contains(SECURITY_HAS_ROLE))
                        .map(chunk -> chunk.substring(chunk.indexOf(SECURITY_HAS_ROLE) + SECURITY_HAS_ROLE.length()))
                        .map(chunk -> chunk.replaceAll("[^A-Za-z_]", "")).collect(Collectors.toSet());
//                roles.forEach(role -> System.out.println(role));
                roles.addAll(rolesOfAnnotation);


            }

//            if (annotationExpr.isMarkerAnnotationExpr()) {
//                System.out.println("Marker = " + annotationExpr.asMarkerAnnotationExpr().getName());
//            } else if (annotationExpr.isSingleMemberAnnotationExpr()) {
//                System.out.println("Single = " + annotationExpr.asSingleMemberAnnotationExpr().getName() + " - " + annotationExpr.asSingleMemberAnnotationExpr().getMemberValue());
//            } else if (annotationExpr.isNormalAnnotationExpr()) {
//                System.out.print("Normal = " + annotationExpr.asNormalAnnotationExpr().getName());
//                annotationExpr.asNormalAnnotationExpr().getPairs().forEach(pair -> {
//                    System.out.print(pair.getName() + "->" + pair.getValue().toString() + ", ");
//                });
//                System.out.println();
//            } else {
//                System.out.print("???? = " + annotationExpr.isAnnotationExpr());
//
//            }
        }
        return roles;

    }


    /**
     * create new componentMethodNode or find already created one and add parent component
     * also find and set roles
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
                        parentComponent.getRoles().forEach(componentMethodNode::addRole);
                    }
                    return componentMethodNode;
                }
            }
        }
        ComponentMethodNode componentMethodNode = new ComponentMethodNode(methodDeclaration, methodName, component, parentComponent, critical);

        // find all Security annotations
        Set<String> roles = getRolesFromSecurityAnnotationsMethodDeclaration(methodDeclaration);
        componentMethodNode.setRoles(roles);
        // add all roles of parent
        if (parentComponent != null) {
            parentComponent.getRoles().forEach(componentMethodNode::addRole);
        }

        componentMethodNodeOfTreeSet.add(componentMethodNode);

        return componentMethodNode;
    }


    /**
     * method is critical if class/interface has repository annotation and method is critical (save or delete methods in db)
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


    private void printAllClassAnnotations() {
        System.out.println("printAllClassAnnotations(){");
        containerClassCUList.forEach(cu -> {
            for (AnnotationExpr annotationExpr : cu.getClassAnnotations()) {
                System.out.println("\t" + annotationExpr.getName());
//                if (annotationExpr.getName().asString().contains("Controller")){
//                    System.out.println(cu.getNameClass() + " = ");
//                    cu.getMethodDeclarations().forEach(method -> {
//                        System.out.println("\t" + method.getNameAsString());
//                    });
//
//                    break;
//                }
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
            componentMethodNode.getRoles().forEach(role -> System.out.print(role + ", "));
            System.out.println("}");
        } else {
            System.out.print(componentMethodNode.getMethodDeclaration().resolve().getQualifiedSignature());
            System.out.print("[" + componentMethodNode.isCriticalMethod() + "]");
            System.out.print("{");
            componentMethodNode.getRoles().forEach(role -> System.out.print(role + ", "));
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
