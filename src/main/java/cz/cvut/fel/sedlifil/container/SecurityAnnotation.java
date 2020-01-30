package cz.cvut.fel.sedlifil.container;

import java.util.Set;

public class SecurityAnnotation {
    private final String annotation;
    private final Set<String> roles;
    private final ComponentMethodNode parentNode;

    public SecurityAnnotation(String annotation, Set<String> roles, ComponentMethodNode parentNode) {
        this.annotation = annotation;
        this.roles = roles;
        this.parentNode = parentNode;
    }

    public String getAnnotation() {
        return annotation;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public ComponentMethodNode getParentNode() {
        return parentNode;
    }
}
