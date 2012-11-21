package org.apache.tomcat.util.xml.processor;

import org.apache.tomcat.util.bcel.classfile.JavaClass;

public interface IHandlesTypesProcessor {

    void checkHandlesTypes(JavaClass clazz);

}
