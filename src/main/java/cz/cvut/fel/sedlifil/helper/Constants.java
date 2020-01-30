package cz.cvut.fel.sedlifil.helper;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Constants {

    public static final int ERROR_CODE = 1;
    public static final String PomXML = "pom.xml";
    public static final String FILE_DELIMITER = File.separator;
    public static final String JAVA_SOURCE = FILE_DELIMITER + "src";

    public static final String JAVA_TARGET = "target";
    public static final String JAVA_TEST = "test";
    public static final String JAVA_IMPORT_ALL_PACKAGE = "*";
    public static final String JAVA_SUFFIX = ".java";

    public static final String REPOSITORY_ANNOTATION = "@Repository";
    public static final String REQUEST_MAPPING = "RequestMapping";
    public static final String METHOD_STRING = "method";
    public static final Set<String> CRITICAL_MAPPING_SET = new HashSet<>(Arrays.asList("PutMapping", "PostMapping", "DeleteMapping"));
    public static final Set<String> CRITICAL_REQUEST_METHOD_SET = new HashSet<>(Arrays.asList("RequestMethod.DELETE", "RequestMethod.PUT", "RequestMethod.POST"));
    public static final Set<String> CRITICAL_REPOSITORY_METHOD_SET = new HashSet<>(Arrays.asList("save", "delete"));

    public static final String SECURITY_ANNOTATION_PREAUTHORIZE = "PreAuthorize";
    public static final String SECURITY_ANNOTATION_POSTAUTHORIZE = "PostAuthorize";
    public static final String SECURITY_ANNOTATION_SECURED = "Secured";
    public static final String SECURITY_ANNOTATION_ROLES_ALLOWED = "RolesAllowed";
    public static final String SECURITY_HAS_ROLE = "hasRole";



}
