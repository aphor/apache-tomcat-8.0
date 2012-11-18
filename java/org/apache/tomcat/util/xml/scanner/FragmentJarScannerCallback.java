package org.apache.tomcat.util.xml.scanner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.scan.Jar;
import org.apache.tomcat.util.scan.JarFactory;
import org.apache.tomcat.util.xml.WebXml;
import org.apache.tomcat.util.xml.parser.WebXmlParser;
import org.xml.sax.InputSource;

class FragmentJarScannerCallback implements JarScannerCallback {

    private static final String FRAGMENT_LOCATION =
        "META-INF/web-fragment.xml";
    
    private final Map<String,WebXml> fragments = new HashMap<>();
    
    private WebXmlParser webXmlParser;
    
    private boolean parseOperationSuccessful = true;

    FragmentJarScannerCallback(WebXmlParser webXmlParser) {
        this.webXmlParser = webXmlParser;
    }
    
    @Override
    public void scan(JarURLConnection jarConn) throws IOException {

        URL url = jarConn.getURL();
        URL resourceURL = jarConn.getJarFileURL();
        Jar jar = null;
        InputStream is = null;
        WebXml fragment = new WebXml();

        try {
            jar = JarFactory.newInstance(url);
            is = jar.getInputStream(FRAGMENT_LOCATION);

            if (is == null) {
                // If there is no web.xml, normal JAR no impact on
                // distributable
                fragment.setDistributable(true);
            } else {
                InputSource source = new InputSource(
                        resourceURL.toString() + "!/" + FRAGMENT_LOCATION);
                source.setByteStream(is);
                if (!this.webXmlParser.parseWebXml(source, fragment, true)) {
                    this.parseOperationSuccessful = false;
                }
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    // Ignore
                }
            }
            if (jar != null) {
                jar.close();
            }
            fragment.setURL(url);
            if (fragment.getName() == null) {
                fragment.setName(fragment.getURL().toString());
            }
            this.fragments.put(fragment.getName(), fragment);
        }
    }

    @Override
    public void scan(File file) throws IOException {

        InputStream stream = null;
        WebXml fragment = new WebXml();

        try {
            File fragmentFile = new File(file, FRAGMENT_LOCATION);
            if (fragmentFile.isFile()) {
                stream = new FileInputStream(fragmentFile);
                InputSource source =
                    new InputSource(fragmentFile.toURI().toURL().toString());
                source.setByteStream(stream);
                if (!this.webXmlParser.parseWebXml(source, fragment, true)) {
                    this.parseOperationSuccessful = false;
                }
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Throwable t) {
                    ExceptionUtils.handleThrowable(t);
                }
            }
            fragment.setURL(file.toURI().toURL());
            if (fragment.getName() == null) {
                fragment.setName(fragment.getURL().toString());
            }
            this.fragments.put(fragment.getName(), fragment);
        }
    }


    @Override
    public void scanWebInfClasses() {
        // NO-OP. Fragments unpacked in WEB-INF classes are not handled,
        // mainly because if there are multiple fragments there is no way to
        // handle multiple web-fragment.xml files.
    }

    Map<String,WebXml> getFragments() {
        return this.fragments;
    }
    
    boolean isParseOperationSuccessful() {
        return this.parseOperationSuccessful;
    }
}
