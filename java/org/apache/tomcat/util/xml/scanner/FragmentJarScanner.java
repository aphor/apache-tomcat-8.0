package org.apache.tomcat.util.xml.scanner;

import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;

import org.apache.tomcat.JarScanner;
import org.apache.tomcat.util.xml.WebXml;
import org.apache.tomcat.util.xml.parser.WebXmlParser;

/**
 * Scans <code>/WEB-INF/lib</code> for JARs and for each one searches for
 * <code>/META-INF/web-fragment.xml</code>
 */
public class FragmentJarScanner {

    /**
     * Scan <code>/WEB-INF/lib</code> for JARs and for each one found add it and
     * any <code>/META-INF/web-fragment.xml</code> to the resulting Map.
     * <code>web-fragment.xml</code> files will be parsed before being added to
     * the map. Every JAR will be added and <code>NULL</code> will be used if no
     * <code>web-fragment.xml</code> was found. Any JARs known not contain
     * fragments will be skipped.
     * 
     * @param jarScanner The <code>JarScanner</code> that will be used
     * @param webXmlParser The parser that will be used for processing
     *        <code>web-fragment.xml</code>
     * @param servletContext The <code>ServletContext</code> that can be used by the
     *        <code>JarScanner</code>
     * @param classLoader The <code>ClassLoader</code> that can be used by the
     *        <code>JarScanner</code>
     * @param pluggabilityJarsToSkip List of the JARs to be skipped by the scanner
     * @param parseOperationSuccessful Status of the parse operation for all
     *        <code>web-fragment.xml</code> that are found
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
