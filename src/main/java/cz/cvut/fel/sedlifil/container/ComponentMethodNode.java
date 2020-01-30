package cz.cvut.fel.sedlifil.container;

import com.github.javaparser.ast.body.MethodDeclaration;
import edu.baylor.ecs.jparser.component.Component;

import java.util.*;

public class ComponentMethodNode {

    private final MethodDeclaration methodDeclaration;
    private final String methodName;
    private final Component classComponent; // class component of this method component
    private Set<ComponentMethodNode> parentComponentMethodNodeList;
    private List<ComponentMethodNode> subMethods;
    private final boolean criticalMethod;
    private Map<String, SecurityAnnotation> securityAnnotationMap;
    private Map<String, SecurityAnnotation> parentSecurityAnnotationMap;
    private Map<String, SecurityAnnotation> securityAnnotationSuggestionMap;


    public ComponentMethodNode(MethodDeclaration methodDeclaration, String methodName, Component classComponent, Set<ComponentMethodNode> parentComponentMethodNodeList, boolean criticalMethod) {
        this.methodDeclaration = methodDeclaration;
        this.methodName = methodName;
        this.classComponent = classComponent;
        this.parentComponentMethodNodeList = parentComponentMethodNodeList;
        this.criticalMethod = criticalMethod;
        this.subMethods = new ArrayList<>();
        securityAnnotationMap = new HashMap<>();
        parentSecurityAnnotationMap = new HashMap<>();
        securityAnnotationSuggestionMap = new HashMap<>();
    }

    public ComponentMethodNode(MethodDeclaration methodDeclaration, String methodName, Component classComponent, ComponentMethodNode componentMethodNode, boolean criticalMethod) {
        this.methodDeclaration = methodDeclaration;
        this.methodName = methodName;
        this.classComponent = classComponent;
        this.parentComponentMethodNodeList = new HashSet<>();
        this.parentComponentMethodNodeList.add(componentMethodNode);
        this.criticalMethod = criticalMethod;
        subMethods = new ArrayList<>();
        securityAnnotationMap = new HashMap<>();
        parentSecurityAnnotationMap = new HashMap<>();
        securityAnnotationSuggestionMap = new HashMap<>();
    }

    public ComponentMethodNode(MethodDeclaration methodDeclaration, String methodName, Component classComponent, boolean criticalMethod) {
        this.methodDeclaration = methodDeclaration;
        this.methodName = methodName;
        this.classComponent = classComponent;
        this.parentComponentMethodNodeList = new HashSet<>();
        this.criticalMethod = criticalMethod;
        subMethods = new ArrayList<>();
        securityAnnotationMap = new HashMap<>();
        parentSecurityAnnotationMap = new HashMap<>();
        securityAnnotationSuggestionMap = new HashMap<>();
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public Component getClassComponent() {
        return classComponent;
    }

    public List<ComponentMethodNode> getSubMethods() {
        return subMethods;
    }

    public void setSubMethods(List<ComponentMethodNode> subMethods) {
        this.subMethods = subMethods;
    }

    public String getMethodName() {
        return methodName;
    }

    public boolean isCriticalMethod() {
        return criticalMethod;
    }

    public Set<ComponentMethodNode> getParentComponentMethodNodeList() {
        return parentComponentMethodNodeList;
    }

    public void setParentComponentMethodNodeList(Set<ComponentMethodNode> parentComponentMethodNodeList) {
        this.parentComponentMethodNodeList = parentComponentMethodNodeList;
    }

    public void addParentClassComponent(ComponentMethodNode classComponent) {
        parentComponentMethodNodeList.add(classComponent);
    }

    public Map<String, SecurityAnnotation> getSecurityAnnotationMap() {
        return securityAnnotationMap;
    }

    public void setSecurityAnnotationMap(Map<String, SecurityAnnotation> securityAnnotationMap) {
        this.securityAnnotationMap = securityAnnotationMap;
    }

    public Map<String, SecurityAnnotation> getParentSecurityAnnotationMap() {
        return parentSecurityAnnotationMap;
    }

    public void setParentSecurityAnnotationMap(Map<String, SecurityAnnotation> parentSecurityAnnotationMap) {
        this.parentSecurityAnnotationMap = parentSecurityAnnotationMap;
    }

    public void addSecurityAnnotation(SecurityAnnotation securityAnnotation){
        securityAnnotationMap.put(securityAnnotation.getAnnotation(), securityAnnotation);
    }
    public void addParentSecurityAnnotation(SecurityAnnotation securityAnnotation){
        if(!parentSecurityAnnotationMap.containsKey(securityAnnotation.getAnnotation()))
            parentSecurityAnnotationMap.put(securityAnnotation.getAnnotation(), securityAnnotation);
    }

    public Map<String, SecurityAnnotation> getSecurityAnnotationSuggestionMap() {
        return securityAnnotationSuggestionMap;
    }

    public void addSecurityAnnotationSuggestion(SecurityAnnotation securityAnnotationSuggestion) {
        this.securityAnnotationSuggestionMap.put(securityAnnotationSuggestion.getAnnotation(), securityAnnotationSuggestion);
    }

}
