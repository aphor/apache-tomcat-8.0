/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.servlets.jsp;

import java.util.Vector;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.tomcat.addons.UserTemplateClassMapper;

/** 
 * Extracted from Jasper - maps a template file ( foo/my.jsp ) to a classname
 * that is generated by the UserTemplateCompiler.
 * 
 * TODO: transform this to an interface, paired with UserTemplateCompiler.
 * 
 * @author Costin Manolache
 */
public class SimpleTemplateClassMapper implements UserTemplateClassMapper {

    /** 
     * Load the proxied jsp, if any.
     * @param config 
     * @throws ServletException 
     */
    public Servlet loadProxy(String jspFile, 
                             ServletContext ctx, 
                             ServletConfig config) throws ServletException {
        String mangledClass = getClassName( jspFile );

        HttpServlet jsp = null;
        Class jspC = null;
        
        // Already created
        if( jspC == null ) {
            try {
                jspC=Class.forName( mangledClass );
            } catch( Throwable t ) {
                // Not found - first try 
            }
        }
        
        if (jspC == null) {
            // Class not found - needs to be compiled
            return compileAndInitPage(ctx, jspFile, config);
        }
        
        try {
            jsp=(HttpServlet)jspC.newInstance();
        } catch( Throwable t ) {
            t.printStackTrace();
        }
        jsp.init(config);
        return jsp;
    }
    
    public boolean needsReload(String jspFile, Servlet s) {
        return false;
    }
    
    protected Servlet compileAndInitPage(ServletContext ctx, 
                                      String jspUri, 
                                      ServletConfig cfg) 
        throws ServletException {
        throw new ServletException("Pre-compiled page not found, please " +
        		"add a compiler addon to compile at runtime");
    }
    
    /** Convert an identifier to a class name, using jasper conventions
     * 
     * @param jspUri a relative JSP file
     * @return class name that would be generated by jasper
     */
    public String getClassName( String jspUri ) {
        int iSep = jspUri.lastIndexOf('/') + 1;
        String className = makeJavaIdentifier(jspUri.substring(iSep));
        String basePackageName = JSP_PACKAGE_NAME;

        iSep--;
        String derivedPackageName = (iSep > 0) ?
                makeJavaPackage(jspUri.substring(1,iSep)) : "";
        
        if (derivedPackageName.length() == 0) {
            return basePackageName + "." + className;
        }
        return basePackageName + '.' + derivedPackageName + "." + className;
    }

    // ------------- Copied from jasper ---------------------------

    private static final String JSP_PACKAGE_NAME = "org.apache.jsp";

    private static final String makeJavaIdentifier(String identifier) {
        StringBuilder modifiedIdentifier = 
            new StringBuilder(identifier.length());
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            modifiedIdentifier.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch) && ch != '_') {
                modifiedIdentifier.append(ch);
            } else if (ch == '.') {
                modifiedIdentifier.append('_');
            } else {
                modifiedIdentifier.append(mangleChar(ch));
            }
        }
        if (isJavaKeyword(modifiedIdentifier.toString())) {
            modifiedIdentifier.append('_');
        }
        return modifiedIdentifier.toString();
    }

    private static final String javaKeywords[] = {
        "abstract", "assert", "boolean", "break", "byte", "case",
        "catch", "char", "class", "const", "continue",
        "default", "do", "double", "else", "enum", "extends",
        "final", "finally", "float", "for", "goto",
        "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short",
        "static", "strictfp", "super", "switch", "synchronized",
        "this", "throws", "transient", "try", "void",
        "volatile", "while" };

    private static final String makeJavaPackage(String path) {
        String classNameComponents[] = split(path,"/");
        StringBuilder legalClassNames = new StringBuilder();
        for (int i = 0; i < classNameComponents.length; i++) {
            legalClassNames.append(makeJavaIdentifier(classNameComponents[i]));
            if (i < classNameComponents.length - 1) {
                legalClassNames.append('.');
            }
        }
        return legalClassNames.toString();
    }

    private static final String [] split(String path, String pat) {
        Vector comps = new Vector();
        int pos = path.indexOf(pat);
        int start = 0;
        while( pos >= 0 ) {
            if(pos > start ) {
                String comp = path.substring(start,pos);
                comps.add(comp);
            }
            start = pos + pat.length();
            pos = path.indexOf(pat,start);
        }
        if( start < path.length()) {
            comps.add(path.substring(start));
        }
        String [] result = new String[comps.size()];
        for(int i=0; i < comps.size(); i++) {
            result[i] = (String)comps.elementAt(i);
        }
        return result;
    }
            

    /**
     * Test whether the argument is a Java keyword
     */
    private static boolean isJavaKeyword(String key) {
        int i = 0;
        int j = javaKeywords.length;
        while (i < j) {
            int k = (i+j)/2;
            int result = javaKeywords[k].compareTo(key);
            if (result == 0) {
                return true;
            }
            if (result < 0) {
                i = k+1;
            } else {
                j = k;
            }
        }
        return false;
    }

    /**
     * Mangle the specified character to create a legal Java class name.
     */
    private static final String mangleChar(char ch) {
        char[] result = new char[5];
        result[0] = '_';
        result[1] = Character.forDigit((ch >> 12) & 0xf, 16);
        result[2] = Character.forDigit((ch >> 8) & 0xf, 16);
        result[3] = Character.forDigit((ch >> 4) & 0xf, 16);
        result[4] = Character.forDigit(ch & 0xf, 16);
        return new String(result);
    }
    
}
