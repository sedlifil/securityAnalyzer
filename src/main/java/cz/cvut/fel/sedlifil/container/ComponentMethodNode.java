package cz.cvut.fel.sedlifil.container;

import com.github.javaparser.ast.body.MethodDeclaration;
import edu.baylor.ecs.jparser.component.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ComponentMethodNode {

    private final MethodDeclaration methodDeclaration;
    private final String methodName;
    private final Component classComponent; // class component of this method component
    private Set<ComponentMethodNode> parentComponentMethodNodeList;
    private List<ComponentMethodNode> subMethods;
    private final boolean criticalMethod;
    private Set<String> roles;


    public ComponentMethodNode(MethodDeclaration methodDeclaration, String methodName, Component classComponent, Set<ComponentMethodNode> parentComponentMethodNodeList, boolean criticalMethod) {
        this.methodDeclaration = methodDeclaration;
        this.methodName = methodName;
        this.classComponent = classComponent;
        this.parentComponentMethodNodeList = parentComponentMethodNodeList;
        this.criticalMethod = criticalMethod;
        subMethods = new ArrayList<>();
        roles = new HashSet<>();
    }

    public ComponentMethodNode(MethodDeclaration methodDeclaration, String methodName, Component classComponent, ComponentMethodNode componentMethodNode, boolean criticalMethod) {
        this.methodDeclaration = methodDeclaration;
        this.methodName = methodName;
        this.classComponent = classComponent;
        this.parentComponentMethodNodeList = new HashSet<>();
        this.parentComponentMethodNodeList.add(componentMethodNode);
        this.criticalMethod = criticalMethod;
        subMethods = new ArrayList<>();
        roles = new HashSet<>();
    }

    public ComponentMethodNode(MethodDeclaration methodDeclaration, String methodName, Component classComponent, boolean criticalMethod) {
        this.methodDeclaration = methodDeclaration;
        this.methodName = methodName;
        this.classComponent = classComponent;
        this.parentComponentMethodNodeList = new HashSet<>();
        this.criticalMethod = criticalMethod;
        subMethods = new ArrayList<>();
        roles = new HashSet<>();
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

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public void addRole(String role) {
        roles.add(role);
    }
}
