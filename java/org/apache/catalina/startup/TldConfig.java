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


package org.apache.catalina.startup;


import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.digester.Digester;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Startup event listener for a <b>Context</b> that configures application
 * listeners configured in any TLD files.
 *
 * @author Craig R. McClanahan
 * @author Jean-Francois Arcand
 * @author Costin Manolache
 */
public final class TldConfig  implements LifecycleListener {

    private static final String JAR_EXT = ".jar";
    private static final String TLD_EXT = ".tld";
    private static final String WEB_INF = "/WEB-INF/";
    private static final String WEB_INF_LIB = "/WEB-INF/lib/";
    
    // Names of JARs that are known not to contain any TLDs
    private static HashSet<String> noTldJars;

    private static org.apache.juli.logging.Log log=
        org.apache.juli.logging.LogFactory.getLog( TldConfig.class );

    /**
     * The string resources for this package.
     */
    private static final StringManager sm =
        StringManager.getManager(Constants.Package);

    /**
     * The <code>Digester</code>s available to process tld files.
     */
    private static Digester[] tldDigesters = new Digester[4];

    /*
     * Initializes the set of JARs that are known not to contain any TLDs
     */
    static {
        noTldJars = new HashSet<String>();
        // Bootstrap JARs
        noTldJars.add("bootstrap.jar");
        noTldJars.add("commons-daemon.jar");
        noTldJars.add("tomcat-juli.jar");
        // Main JARs
        noTldJars.add("annotations-api.jar");
        noTldJars.add("catalina.jar");
        noTldJars.add("catalina-ant.jar");
        noTldJars.add("catalina-ha.jar");
        noTldJars.add("catalina-tribes.jar");
        noTldJars.add("el-api.jar");
        noTldJars.add("jasper.jar");
        noTldJars.add("jasper-el.jar");
        noTldJars.add("jasper-jdt.jar");
        noTldJars.add("jsp-api.jar");
        noTldJars.add("servlet-api.jar");
        noTldJars.add("tomcat-api.jar");
        noTldJars.add("tomcat-coyote.jar");
        noTldJars.add("tomcat-dbcp.jar");
        // i18n JARs
        noTldJars.add("tomcat-i18n-en.jar");
        noTldJars.add("tomcat-i18n-es.jar");
        noTldJars.add("tomcat-i18n-fr.jar");
        noTldJars.add("tomcat-i18n-ja.jar");
        // Misc JARs not included with Tomcat
        noTldJars.add("ant.jar");
        noTldJars.add("commons-dbcp.jar");
        noTldJars.add("commons-beanutils.jar");
        noTldJars.add("commons-fileupload-1.0.jar");
        noTldJars.add("commons-pool.jar");
        noTldJars.add("commons-digester.jar");
        noTldJars.add("commons-logging.jar");
        noTldJars.add("commons-collections.jar");
        noTldJars.add("jmx.jar");
        noTldJars.add("jmx-tools.jar");
        noTldJars.add("xercesImpl.jar");
        noTldJars.add("xmlParserAPIs.jar");
        noTldJars.add("xml-apis.jar");
        // JARs from J2SE runtime
        noTldJars.add("sunjce_provider.jar");
        noTldJars.add("ldapsec.jar");
        noTldJars.add("localedata.jar");
        noTldJars.add("dnsns.jar");
        noTldJars.add("tools.jar");
        noTldJars.add("sunpkcs11.jar");
    }

    /**
     * Create (if necessary) and return a Digester configured to process the
     * tld.
     */
    private static Digester createTldDigester(boolean namespaceAware,
            boolean validation) {
        
        Digester digester = null;
        if (!namespaceAware && !validation) {
            if (tldDigesters[0] == null) {
                tldDigesters[0] = DigesterFactory.newDigester(validation,
                        namespaceAware, new TldRuleSet());
            }
            digester = tldDigesters[0];
        } else if (!namespaceAware && validation) {
            if (tldDigesters[1] == null) {
                tldDigesters[1] = DigesterFactory.newDigester(validation,
                        namespaceAware, new TldRuleSet());
            }
            digester = tldDigesters[1];
        } else if (namespaceAware && !validation) {
            if (tldDigesters[2] == null) {
                tldDigesters[2] = DigesterFactory.newDigester(validation,
                        namespaceAware, new TldRuleSet());
            }
            digester = tldDigesters[2];
        } else {
            if (tldDigesters[3] == null) {
                tldDigesters[3] = DigesterFactory.newDigester(validation,
                        namespaceAware, new TldRuleSet());
            }
            digester = tldDigesters[3];
        }
        return digester;
    }


    // ----------------------------------------------------- Instance Variables

    /**
     * The Context we are associated with.
     */
    private Context context = null;


    /**
     * The <code>Digester</code> we will use to process tag library
     * descriptor files.
     */
    private Digester tldDigester = null;


    /**
     * Attribute value used to turn on/off TLD validation
     */
    private boolean tldValidation = false;


    /**
     * Attribute value used to turn on/off TLD  namespace awarenes.
     */
    private boolean tldNamespaceAware = false;

    private boolean rescan=true;

    /**
     * Set of URIs discovered for the associated context. Used to enforce the
     * correct processing priority. Only the TLD associated with the first
     * instance of any URI will be processed.
     */
    private Set<String> taglibUris = new HashSet<String>();
    
    private ArrayList<String> listeners = new ArrayList<String>();

    // --------------------------------------------------------- Public Methods

    /**
     * Adds a taglib URI to the list of known URIs.
     */
    public void addTaglibUri(String uri) {
        taglibUris.add(uri);
    }

    /**
     * Determines if the provided URI is a known taglib URI.
     */
    public boolean isKnownTaglibUri(String uri) {
        return taglibUris.contains(uri);
    }

    /**
     * Sets the list of JARs that are known not to contain any TLDs.
     *
     * @param jarNames List of comma-separated names of JAR files that are 
     * known not to contain any TLDs 
     */
    public static void setNoTldJars(String jarNames) {
        if (jarNames != null) {
            noTldJars.clear();
            StringTokenizer tokenizer = new StringTokenizer(jarNames, ",");
            while (tokenizer.hasMoreElements()) {
                noTldJars.add(tokenizer.nextToken());
            }
        }
    }

    /**
     * Set the validation feature of the XML parser used when
     * parsing xml instances.
     * @param tldValidation true to enable xml instance validation
     */
    public void setTldValidation(boolean tldValidation){
        this.tldValidation = tldValidation;
    }

    /**
     * Get the server.xml &lt;host&gt; attribute's xmlValidation.
     * @return true if validation is enabled.
     *
     */
    public boolean getTldValidation(){
        return this.tldValidation;
    }

    /**
     * Get the server.xml &lt;host&gt; attribute's xmlNamespaceAware.
     * @return true if namespace awarenes is enabled.
     *
     */
    public boolean getTldNamespaceAware(){
        return this.tldNamespaceAware;
    }


    /**
     * Set the namespace aware feature of the XML parser used when
     * parsing xml instances.
     * @param tldNamespaceAware true to enable namespace awareness
     */
    public void setTldNamespaceAware(boolean tldNamespaceAware){
        this.tldNamespaceAware = tldNamespaceAware;
    }    


    public boolean isRescan() {
        return rescan;
    }

    public void setRescan(boolean rescan) {
        this.rescan = rescan;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void addApplicationListener( String s ) {
        //if(log.isDebugEnabled())
            log.debug( "Add tld listener " + s);
        listeners.add(s);
    }

    public String[] getTldListeners() {
        String result[]=new String[listeners.size()];
        listeners.toArray(result);
        return result;
    }


    /**
     * Scan for and configure all tag library descriptors found in this
     * web application.
     *
     * @exception Exception if a fatal input/output or parsing error occurs
     */
    public void execute() throws Exception {
        long t1=System.currentTimeMillis();

        /*
         * Priority order of URIs required by spec is:
         * 1. J2EE platform taglibs - Tomcat doesn't provide these
         * 2. web.xml entries
         * 3. JARS in WEB-INF/lib & TLDs under WEB-INF (equal priority)
         * 4. Additional entries from the container
         * 
         * Keep processing order in sync with o.a.j.compiler.TldLocationsCache
         */
        
        // Stage 2 - web.xml entries
        tldScanWebXml();
        
        // Stage 3a - TLDs under WEB-INF (not lib or classes)
        tldScanResourcePaths(WEB_INF);

        // Stage 3b - .jar files in WEB-INF/lib/
        tldScanWebInfLib();
        
        // Stage 4 - Additional entries from the container
        tldScanClassloaders();

        // Now add all the listeners we found to the listeners for this context
        String list[] = getTldListeners();

        if( log.isDebugEnabled() )
            log.debug(sm.getString("tldConfig.addListeners",
                    Integer.valueOf(list.length)));

        for( int i=0; list!=null && i<list.length; i++ ) {
            context.addApplicationListener(list[i]);
        }

        long t2=System.currentTimeMillis();
        if( context instanceof StandardContext ) {
            ((StandardContext)context).setTldScanTime(t2-t1);
        }

    }

    // -------------------------------------------------------- Private Methods


    /**
     * Get the taglib entries from web.xml and add them to the map.
     * 
     * This is not kept in sync with o.a.j.compiler.TldLocationsCache as this
     * code needs to scan the TLDs listed in web.xml whereas Jasper only needs
     * the URI to TLD mappings.
     */
    private void tldScanWebXml() {
        
        if (log.isTraceEnabled()) {
            log.trace(sm.getString("tldConfig.webxmlStart"));
        }
     
        String taglibs[] = context.findTaglibs();
        for (int i = 0; i < taglibs.length; i++) {
            String resourcePath = context.findTaglib(taglibs[i]);
            // Note: Whilst the Servlet 2.4 DTD implies that the location must
            // be a context-relative path starting with '/', JSP.7.3.6.1 states
            // explicitly how paths that do not start with '/' should be
            // handled.
            if (!resourcePath.startsWith("/")) {
                resourcePath = WEB_INF + resourcePath;
            }
            if (taglibUris.contains(taglibs[i])) {
                log.warn(sm.getString("tldConfig.webxmlSkip", resourcePath,
                        taglibs[i]));
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(sm.getString("tldConfig.webxmlAdd", resourcePath,
                            taglibs[i]));
                }
                try {
                    InputStream stream = context.getServletContext(
                            ).getResourceAsStream(resourcePath);
                    tldScanStream(stream);
                    taglibUris.add(taglibs[i]);
                } catch (IOException ioe) {
                    log.warn(sm.getString("tldConfig.webxmlFail", resourcePath,
                            taglibs[i]), ioe);
                }
            }
        }
    }
    
    /*
     * Scans the web application's sub-directory identified by startPath,
     * along with its sub-directories, for TLDs.
     *
     * Initially, rootPath equals /WEB-INF/. The /WEB-INF/classes and
     * /WEB-INF/lib sub-directories are excluded from the search, as per the
     * JSP 2.0 spec.
     * 
     * Keep in sync with o.a.j.comiler.TldLocationsCache
     */
    private void tldScanResourcePaths(String startPath) {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("tldConfig.webinfScan", startPath));
        }

        ServletContext ctxt = context.getServletContext();

        Set<String> dirList = ctxt.getResourcePaths(startPath);
        if (dirList != null) {
            Iterator<String> it = dirList.iterator();
            while (it.hasNext()) {
                String path = it.next();
                if (!path.endsWith(TLD_EXT)
                        && (path.startsWith(WEB_INF_LIB)
                                || path.startsWith("/WEB-INF/classes/"))) {
                    continue;
                }
                if (path.endsWith(TLD_EXT)) {
                    if (path.startsWith("/WEB-INF/tags/") &&
                            !path.endsWith("implicit.tld")) {
                        continue;
                    }
                    InputStream stream = ctxt.getResourceAsStream(path);
                    try {
                        tldScanStream(stream);
                    } catch (IOException ioe) {
                        log.warn(sm.getString("tldConfig.webinfFail", path),
                                ioe);
                    } finally {
                        if (stream != null) {
                            try {
                                stream.close();
                            } catch (Throwable t) {
                                // do nothing
                            }
                        }
                    }
                } else {
                    tldScanResourcePaths(path);
                }
            }
        }
    }
    
    /*
     * Scan the JARs in the WEB-INF/lib directory. Skip the JARs known not to
     * have any TLDs in them.
     * 
     * Keep in sync with o.a.j.comiler.TldLocationsCache
     */
    private void tldScanWebInfLib() {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("tldConfig.webinflibStart"));
        }
        ServletContext ctxt = context.getServletContext();
        
        Set<String> dirList = ctxt.getResourcePaths(WEB_INF_LIB);
        if (dirList != null) {
            Iterator<String> it = dirList.iterator();
            while (it.hasNext()) {
                String path = it.next();
                if (path.endsWith(JAR_EXT) &&
                        !noTldJars.contains(
                                path.substring(path.lastIndexOf('/')))) {
                    // Need to scan this JAR for TLDs
                    URL url = null;
                    try {
                        url = ctxt.getResource(path);
                        tldScanJar(url);
                    } catch (IOException e) {
                        log.warn(sm.getString("tldConfig.webinflibJarFail"), e);
                    }
                }
            }
        }
    }

    /*
     * Scan the classloader hierarchy for JARs and, optionally, for JARs where
     * the name doesn't end in .jar and directories that represent exploded
     * JARs. The JARs under WEB-INF/lib will be skipped as they have been
     * scanned previously.
     *
     * This represents a Tomcat-specific extension to the TLD search
     * order defined in the JSP spec. It allows tag libraries packaged as JAR
     * files to be shared by web applications by simply dropping them in a 
     * location that all web applications have access to (e.g.,
     * <CATALINA_HOME>/lib). It also supports some of the weird and
     * wonderful arrangements present when Tomcat gets embedded.
     *
     * The set of shared JARs to be scanned for TLDs is narrowed down by
     * the <tt>noTldJars</tt> class variable, which contains the names of JARs
     * that are known not to contain any TLDs.
     * 
     * Keep in sync with o.a.j.comiler.TldLocationsCache
     */
    private void tldScanClassloaders() {

        ClassLoader loader = 
            Thread.currentThread().getContextClassLoader();
        
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) loader).getURLs();
                for (int i=0; i<urls.length; i++) {
                    // Extract the jarName if there is one to be found
                    String jarName = getJarName(urls[i]);
                    
                    // Skip JARs with known not to contain TLDs and JARs in
                    // WEB-INF/lib we have already scanned
                    if (!(noTldJars.contains(jarName) ||
                            urls[i].toString().contains(
                                    WEB_INF_LIB + jarName))) {
                        try {
                            tldScanJar(urls[i]);
                        } catch (IOException ioe) {
                            log.warn(sm.getString(
                                    "tldConfig.classloaderFail",urls[i]), ioe);
                        }
                    }
                }
            }
            loader = loader.getParent();
        }
    }

    /*
     * Keep in sync with o.a.j.comiler.TldLocationsCache
     */
    private void tldScanJar(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        if (conn instanceof JarURLConnection) {
            tldScanJar((JarURLConnection) conn);
        } else {
            String urlStr = url.toString();
            if (urlStr.startsWith("file:")
                    && urlStr.endsWith(JAR_EXT)) {
                URL jarURL = new URL("jar:" + urlStr + "!/");
                tldScanJar((JarURLConnection) jarURL.openConnection());
            }
        }
    }

    /*
     * Scans the given JarURLConnection for TLD files located in META-INF
     * (or a sub-directory of it).
     *
     * @param conn The JarURLConnection to the JAR file to scan
     * 
     * Keep in sync with o.a.j.comiler.TldLocationsCache
     */
    private void tldScanJar(JarURLConnection conn) throws IOException {

        JarFile jarFile = null;
        try {
            conn.setUseCaches(false);
            jarFile = conn.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("META-INF/")) continue;
                if (!name.endsWith(".tld")) continue;
                InputStream stream = jarFile.getInputStream(entry);
                tldScanStream(stream);
            }
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
    }


    /*
     * Extract the JAR name, if present, from a URL
     * 
     * Keep in sync with o.a.j.comiler.TldLocationsCache
     */
    private String getJarName(URL url) {
        
        String name = null;
        
        String path = url.getPath();
        int end = path.indexOf(JAR_EXT);
        if (end != -1) {
            int start = path.lastIndexOf('/', end);
            name = path.substring(start + 1, end + 4);
        }
        
        return name;
    }

    /*
     * Scan the TLD contents in the specified input stream, and register
     * any application event listeners found there.  <b>NOTE</b> - This 
     * method ensure that the InputStream is correctly closed.
     *
     * @param resourceStream InputStream containing a tag library descriptor
     *
     * @throws IOException  If the file cannot be read
     */
    private void tldScanStream(InputStream resourceStream) throws IOException {
        
        InputSource source = new InputSource(resourceStream);
        
        synchronized (tldDigester) {
            try {
                tldDigester.push(this);
                tldDigester.parse(source);
            } catch (SAXException s) {
                // Hack - makes exception handling simpler
                throw new IOException(s);
            } finally {
                tldDigester.reset();
                if (resourceStream != null) {
                    try {
                        resourceStream.close();
                    } catch (Throwable t) {
                        // do nothing
                    }
                }
            }
        }
    }

    public void lifecycleEvent(LifecycleEvent event) {
        // Identify the context we are associated with
        try {
            context = (Context) event.getLifecycle();
        } catch (ClassCastException e) {
            log.error(sm.getString("tldConfig.cce", event.getLifecycle()), e);
            return;
        }
        
        if (event.getType().equals(Lifecycle.INIT_EVENT)) {
            init();
        } else if (event.getType().equals(Lifecycle.START_EVENT)) {
            try {
                execute();
            } catch (Exception e) {
                log.error(sm.getString(
                        "tldConfig.execute", context.getPath()), e);
            }
        } // Ignore the other event types - nothing to do 
    }
    
    private void init() {
        if (tldDigester == null){
            // (1)  check if the attribute has been defined
            //      on the context element.
            setTldValidation(context.getTldValidation());
            setTldNamespaceAware(context.getTldNamespaceAware());
    
            // (2) if the attribute wasn't defined on the context
            //     and override is not set on the context try the host.
            if (!context.getOverride()) {
                if (!tldValidation) {
                    setTldValidation(
                            ((StandardHost) context.getParent()).getXmlValidation());
                }
    
                if (!tldNamespaceAware) {
                    setTldNamespaceAware(
                      ((StandardHost) context.getParent()).getXmlNamespaceAware());
                }
            }
            tldDigester = createTldDigester(tldNamespaceAware, tldValidation);
        }
    }
}
