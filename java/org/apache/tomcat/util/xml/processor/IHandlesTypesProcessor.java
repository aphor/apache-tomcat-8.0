package org.apache.tomcat.util.xml.processor;

import javax.servlet.annotation.HandlesTypes;

import org.apache.tomcat.util.bcel.classfile.JavaClass;

/**
 * Processor that will check for {@link HandlesTypes}
 */
public interface IHandlesTypesProcessor {

    /**
     * For classes packaged with the web application, the class and each super
     * class needs to be checked for a match with {@link HandlesTypes} or for an
     * annotation that matches {@link HandlesTypes}.
     * 
     * @param javaClass <code>JavaClass</code> that will be checked
     */
    void checkHandlesTypes(JavaClass clazz);

}
