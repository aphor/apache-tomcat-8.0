package org.apache.tomcat.util.xml.scanner;

import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.xml.WebXml;
import org.apache.tomcat.util.xml.parser.WebXmlParser;

public class FragmentJarScanner {

    /**
     * Scan /WEB-INF/lib for JARs and for each one found add it and any
     * /META-INF/web-fragment.xml to the resulting Map. web-fragment.xml files
     * will be parsed before being added to the map. Every JAR will be added and
     * <code>null</code> will be used if no web-fragment.xml was found. Any JARs
     * known not contain fragments will be skipped.
     *
     * @return A map of JAR name to processed web fragment (if any)
     */
    public static Map<String, WebXml> processJarsForWebFragments(
            JarScanner jarScanner, WebXmlParser webXmlParser,
            ServletContext servletContext, ClassLoader classLoader,
            Set<String> pluggabilityJarsToSkip, Boolean parseOperationSuccessful) {

        FragmentJarScannerCallback callback = new FragmentJarScannerCallback(
                webXmlParser);

        jarScanner.scan(servletContext, classLoader, callback,
                pluggabilityJarsToSkip);

        parseOperationSuccessful = callback.isParseOperationSuccessful();
        
        return callback.getFragments();
    }

}
