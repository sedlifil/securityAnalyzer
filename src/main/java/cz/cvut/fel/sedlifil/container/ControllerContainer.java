package cz.cvut.fel.sedlifil.container;


import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitor;
import cz.cvut.fel.sedlifil.visitor.Visitor;
import edu.baylor.ecs.jparser.component.Component;
import edu.baylor.ecs.jparser.model.AnnotationValuePair;
import javassist.bytecode.annotation.MemberValue;

import java.util.ArrayList;
import java.util.List;

import static cz.cvut.fel.sedlifil.helper.Constants.*;
import static cz.cvut.fel.sedlifil.helper.Constants.CRITICAL_REQUEST_METHOD_SET;

public class ControllerContainer {
    private final Component classComponent;
    private List<ComponentMethodNode> criticalMethods;
    private List<Component> classComponentsFromFieldsList;

    public ControllerContainer(Component classComponent) {
        this.classComponent = classComponent;
        criticalMethods = new ArrayList<>();
        classComponentsFromFieldsList = new ArrayList<>();
    }

    public Component getClassComponent() {
        return classComponent;
    }

    public List<ComponentMethodNode> getCriticalMethods() {
        return criticalMethods;
    }

    public void setCriticalMethods(List<ComponentMethodNode> criticalMethods) {
        this.criticalMethods = criticalMethods;
    }

    public List<Component> getClassComponentsFromFieldsList() {
        return classComponentsFromFieldsList;
    }

    public void setClassComponentsFromFieldsList(List<Component> classComponentsFromFieldsList) {
        this.classComponentsFromFieldsList = classComponentsFromFieldsList;
    }




//      TODO prepare for methodInfoComponent -> because of lack of proper type parameters, I can not use it
//    /**
//     * get all critical methods (POST, PUT, DELETE) of Class classComponent
//     *
//     */
//    private void evaluateCriticalMethods() {
//        criticalMethods = new ArrayList<>();
//
////        System.out.println(controllerContainer.getMethodDeclaration().asClassComponent().getClassName());
////        System.out.println("Methods:");
//        classComponent.asClassComponent().getMethods().forEach(method -> {
////            System.out.println("\t" + method.asMethodInfoComponent().getMethodName());
////            System.out.println("\t\tAnnotations:");
//
//            annotationLoop:
//            for (Component ann : method.asMethodInfoComponent().getAnnotations()) {
//
//
//                String annName = ann.asAnnotationComponent().getAsString();
////                System.out.println("\t\t\t" + annName);
////                ann.asAnnotationComponent().getAnnotationValuePairList().forEach(pair -> {
////                    System.out.println("\t\t\t\t" + pair.getKey() + " -> " + pair.getValue());
////                });
//
//                // for  specific method in annotation such as @PostMapping
//                if (CRITICAL_MAPPING_SET.contains(annName)) {
////                    System.out.println("\t\tCRITICAL ANN");
//                    criticalMethods.add(new ComponentMethodNode(method, classComponent));
//                    break;
//                }
//                // for @RequestMapping - specific method as parameter
//                else if (annName.equals(REQUEST_MAPPING)) {
////                    System.out.print("\t\tREQUEST MAP ANN = ");
//                    for (AnnotationValuePair pair : ann.asAnnotationComponent().getAnnotationValuePairList()) {
//                        if (pair.getKey().equals(METHOD_STRING) && CRITICAL_REQUEST_METHOD_SET.contains(pair.getValue())) {
//                            criticalMethods.add(new ComponentMethodNode(method, classComponent));
//                            break annotationLoop;
//                        }
//                    }
//
//                }
//            }
//        });
//    }


}
