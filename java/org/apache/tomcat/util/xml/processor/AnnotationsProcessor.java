package org.apache.tomcat.util.xml.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.annotation.HandlesTypes;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.bcel.classfile.AnnotationElementValue;
import org.apache.tomcat.util.bcel.classfile.AnnotationEntry;
import org.apache.tomcat.util.bcel.classfile.ArrayElementValue;
import org.apache.tomcat.util.bcel.classfile.ClassFormatException;
import org.apache.tomcat.util.bcel.classfile.ClassParser;
import org.apache.tomcat.util.bcel.classfile.ElementValue;
import org.apache.tomcat.util.bcel.classfile.ElementValuePair;
import org.apache.tomcat.util.bcel.classfile.JavaClass;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.scan.Jar;
import org.apache.tomcat.util.scan.JarFactory;
import org.apache.tomcat.util.xml.FilterDef;
import org.apache.tomcat.util.xml.FilterMap;
import org.apache.tomcat.util.xml.ServletDef;
import org.apache.tomcat.util.xml.WebXml;

/** 
 * Processes annotations in the web application
 */
public class AnnotationsProcessor {

    private static final String CLASS_EXTENSION = ".class";

    private static final Log log = LogFactory.getLog( AnnotationsProcessor.class );

    /**
     * The string resources for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);

    /**
     * Processes the given <code>InputStream</code> for annotations and stores
     * the result in the given <code>WebXml</code>. During this operation
     * <code>IHandlesTypesProcessor</code> will be invoked in order to check for
     * {@link HandlesTypes}.
     * 
     * @param is <code>InputStream</code> that will be processed for
     *        annotations
     * @param fragment Representation of web application deployment descriptor where
     *        the annotations data will be stored
     * @param handlesTypesProcessor Processor that will check for {@link HandlesTypes}
     * @param handlesTypesOnly Specifies whether the metadata is complete
     * @throws ClassFormatException
     * @throws IOException
     */
    public static void processAnnotationsStream(InputStream is, WebXml fragment,
            IHandlesTypesProcessor handlesTypesProcessor, boolean handlesTypesOnly)
            throws ClassFormatException, IOException {

        ClassParser parser = new ClassParser(is, null);
        JavaClass clazz = parser.parse();

        if (handlesTypesProcessor != null) {
            handlesTypesProcessor.checkHandlesTypes(clazz);
        }
        if (handlesTypesOnly) {
            return;
        }

        String className = clazz.getClassName();

        AnnotationEntry[] annotationsEntries = clazz.getAnnotationEntries();

        for (AnnotationEntry ae : annotationsEntries) {
            String type = ae.getAnnotationType();
            if ("Ljavax/servlet/annotation/WebServlet;".equals(type)) {
                processAnnotationWebServlet(className, ae, fragment);
            }else if ("Ljavax/servlet/annotation/WebFilter;".equals(type)) {
                processAnnotationWebFilter(className, ae, fragment);
            }else if ("Ljavax/servlet/annotation/WebListener;".equals(type)) {
                fragment.addListener(className);
            } else {
                // Unknown annotation - ignore
            }
        }
    }

    /**
     * Process filter annotation and merge with existing one! FIXME: refactoring
     * method too long and has redundant subroutines with
     * processAnnotationWebServlet!
     * 
     * @param className The class name
     * @param ae Representation of the annotation
     * @param fragment Representation of web application deployment descriptor where
     *        the annotations data will be stored
     */
    protected static void processAnnotationWebFilter(String className,
            AnnotationEntry ae, WebXml fragment) {
        String filterName = null;
        // must search for name s. Spec Servlet API 3.0 - 8.2.3.3.n.ii page 81
        ElementValuePair[] evps = ae.getElementValuePairs();
        for (ElementValuePair evp : evps) {
            String name = evp.getNameString();
            if ("filterName".equals(name)) {
                filterName = evp.getValue().stringifyValue();
                break;
            }
        }
        if (filterName == null) {
            // classname is default filterName as annotation has no name!
            filterName = className;
        }
        FilterDef filterDef = fragment.getFilters().get(filterName);
        FilterMap filterMap = new FilterMap();

        boolean isWebXMLfilterDef;
        if (filterDef == null) {
            filterDef = new FilterDef();
            filterDef.setFilterName(filterName);
            filterDef.setFilterClass(className);
            isWebXMLfilterDef = false;
        } else {
            isWebXMLfilterDef = true;
        }

        boolean urlPatternsSet = false;
        boolean servletNamesSet = false;
        boolean dispatchTypesSet = false;
        String[] urlPatterns = null;

        for (ElementValuePair evp : evps) {
            String name = evp.getNameString();
            if ("value".equals(name) || "urlPatterns".equals(name)) {
                if (urlPatternsSet) {
                    throw new IllegalArgumentException(sm.getString(
                            "annotationsProcessor.urlPatternValue", className));
                }
                urlPatterns = processAnnotationsStringArray(evp.getValue());
                urlPatternsSet = urlPatterns.length > 0;
                for (String urlPattern : urlPatterns) {
                    filterMap.addURLPattern(urlPattern);
                }
            } else if ("servletNames".equals(name)) {
                String[] servletNames = processAnnotationsStringArray(evp
                        .getValue());
                servletNamesSet = servletNames.length > 0;
                for (String servletName : servletNames) {
                    filterMap.addServletName(servletName);
                }
            } else if ("dispatcherTypes".equals(name)) {
                String[] dispatcherTypes = processAnnotationsStringArray(evp
                        .getValue());
                dispatchTypesSet = dispatcherTypes.length > 0;
                for (String dispatcherType : dispatcherTypes) {
                    filterMap.setDispatcher(dispatcherType);
                }
            } else if ("description".equals(name)) {
                if (filterDef.getDescription() == null) {
                    filterDef.setDescription(evp.getValue().stringifyValue());
                }
            } else if ("displayName".equals(name)) {
                if (filterDef.getDisplayName() == null) {
                    filterDef.setDisplayName(evp.getValue().stringifyValue());
                }
            } else if ("largeIcon".equals(name)) {
                if (filterDef.getLargeIcon() == null) {
                    filterDef.setLargeIcon(evp.getValue().stringifyValue());
                }
            } else if ("smallIcon".equals(name)) {
                if (filterDef.getSmallIcon() == null) {
                    filterDef.setSmallIcon(evp.getValue().stringifyValue());
                }
            } else if ("asyncSupported".equals(name)) {
                if (filterDef.getAsyncSupported() == null) {
                    filterDef
                            .setAsyncSupported(evp.getValue().stringifyValue());
                }
            } else if ("initParams".equals(name)) {
                Map<String, String> initParams = processAnnotationWebInitParams(evp
                        .getValue());
                if (isWebXMLfilterDef) {
                    Map<String, String> webXMLInitParams = filterDef
                            .getParameterMap();
                    for (Map.Entry<String, String> entry : initParams
                            .entrySet()) {
                        if (webXMLInitParams.get(entry.getKey()) == null) {
                            filterDef.addInitParameter(entry.getKey(), entry
                                    .getValue());
                        }
                    }
                } else {
                    for (Map.Entry<String, String> entry : initParams
                            .entrySet()) {
                        filterDef.addInitParameter(entry.getKey(), entry
                                .getValue());
                    }
                }

            }
        }
        if (!isWebXMLfilterDef) {
            fragment.addFilter(filterDef);
            if (urlPatternsSet || servletNamesSet) {
                filterMap.setFilterName(filterName);
                fragment.addFilterMapping(filterMap);
            }
        }
        if (urlPatternsSet || dispatchTypesSet) {
            Set<FilterMap> fmap = fragment.getFilterMappings();
            FilterMap descMap = null;
            for (FilterMap map : fmap) {
                if (filterName.equals(map.getFilterName())) {
                    descMap = map;
                    break;
                }
            }
            if (descMap != null) {
                String[] urlsPatterns = descMap.getURLPatterns();
                if (urlPatternsSet
                        && (urlsPatterns == null || urlsPatterns.length == 0)) {
                    for (String urlPattern : filterMap.getURLPatterns()) {
                        descMap.addURLPattern(urlPattern);
                    }
                }
                String[] dispatcherNames = descMap.getDispatcherNames();
                if (dispatchTypesSet
                        && (dispatcherNames == null || dispatcherNames.length == 0)) {
                    for (String dis : filterMap.getDispatcherNames()) {
                        descMap.setDispatcher(dis);
                    }
                }
            }
        }

    }

    /**
     * Process servlet annotation and merge with existing one! FIXME:
     * refactoring method too long and has redundant subroutines with
     * processAnnotationWebFilter!
     * 
     * @param className The class name
     * @param ae Representation of the annotation
     * @param fragment Representation of web application deployment descriptor where
     *        the annotations data will be stored
     */
    protected static void processAnnotationWebServlet(String className,
            AnnotationEntry ae, WebXml fragment) {
        String servletName = null;
        // must search for name s. Spec Servlet API 3.0 - 8.2.3.3.n.ii page 81
        ElementValuePair[] evps = ae.getElementValuePairs();
        for (ElementValuePair evp : evps) {
            String name = evp.getNameString();
            if ("name".equals(name)) {
                servletName = evp.getValue().stringifyValue();
                break;
            }
        }
        if (servletName == null) {
            // classname is default servletName as annotation has no name!
            servletName = className;
        }
        ServletDef servletDef = fragment.getServlets().get(servletName);

        boolean isWebXMLservletDef;
        if (servletDef == null) {
            servletDef = new ServletDef();
            servletDef.setServletName(servletName);
            servletDef.setServletClass(className);
            isWebXMLservletDef = false;
        } else {
            isWebXMLservletDef = true;
        }

        boolean urlPatternsSet = false;
        String[] urlPatterns = null;

        // ElementValuePair[] evps = ae.getElementValuePairs();
        for (ElementValuePair evp : evps) {
            String name = evp.getNameString();
            if ("value".equals(name) || "urlPatterns".equals(name)) {
                if (urlPatternsSet) {
                    throw new IllegalArgumentException(sm.getString(
                            "annotationsProcessor.urlPatternValue", className));
                }
                urlPatternsSet = true;
                urlPatterns = processAnnotationsStringArray(evp.getValue());
            } else if ("description".equals(name)) {
                if (servletDef.getDescription() == null) {
                    servletDef.setDescription(evp.getValue().stringifyValue());
                }
            } else if ("displayName".equals(name)) {
                if (servletDef.getDisplayName() == null) {
                    servletDef.setDisplayName(evp.getValue().stringifyValue());
                }
            } else if ("largeIcon".equals(name)) {
                if (servletDef.getLargeIcon() == null) {
                    servletDef.setLargeIcon(evp.getValue().stringifyValue());
                }
            } else if ("smallIcon".equals(name)) {
                if (servletDef.getSmallIcon() == null) {
                    servletDef.setSmallIcon(evp.getValue().stringifyValue());
                }
            } else if ("asyncSupported".equals(name)) {
                if (servletDef.getAsyncSupported() == null) {
                    servletDef.setAsyncSupported(evp.getValue()
                            .stringifyValue());
                }
            } else if ("loadOnStartup".equals(name)) {
                if (servletDef.getLoadOnStartup() == null) {
                    servletDef
                            .setLoadOnStartup(evp.getValue().stringifyValue());
                }
            } else if ("initParams".equals(name)) {
                Map<String, String> initParams = processAnnotationWebInitParams(evp
                        .getValue());
                if (isWebXMLservletDef) {
                    Map<String, String> webXMLInitParams = servletDef
                            .getParameterMap();
                    for (Map.Entry<String, String> entry : initParams
                            .entrySet()) {
                        if (webXMLInitParams.get(entry.getKey()) == null) {
                            servletDef.addInitParameter(entry.getKey(), entry
                                    .getValue());
                        }
                    }
                } else {
                    for (Map.Entry<String, String> entry : initParams
                            .entrySet()) {
                        servletDef.addInitParameter(entry.getKey(), entry
                                .getValue());
                    }
                }
            }
        }
        if (!isWebXMLservletDef && urlPatterns != null) {
            fragment.addServlet(servletDef);
        }
        if (urlPatterns != null) {
            if (!fragment.getServletMappings().containsValue(servletName)) {
                for (String urlPattern : urlPatterns) {
                    fragment.addServletMapping(urlPattern, servletName);
                }
            }
        }

    }

    /**
     * Process the init params.
     * 
     * @param ev
     * @return Map that contains the init param name and value pairs
     */
    protected static Map<String,String> processAnnotationWebInitParams(
            ElementValue ev) {
        Map<String, String> result = new HashMap<>();
        if (ev instanceof ArrayElementValue) {
            ElementValue[] arrayValues =
                ((ArrayElementValue) ev).getElementValuesArray();
            for (ElementValue value : arrayValues) {
                if (value instanceof AnnotationElementValue) {
                    ElementValuePair[] evps = ((AnnotationElementValue)
                            value).getAnnotationEntry().getElementValuePairs();
                    String initParamName = null;
                    String initParamValue = null;
                    for (ElementValuePair evp : evps) {
                        if ("name".equals(evp.getNameString())) {
                            initParamName = evp.getValue().stringifyValue();
                        } else if ("value".equals(evp.getNameString())) {
                            initParamValue = evp.getValue().stringifyValue();
                        } else {
                            // Ignore
                        }
                    }
                    result.put(initParamName, initParamValue);
                }
            }
        }
        return result;
    }

    protected static String[] processAnnotationsStringArray(ElementValue ev) {
        ArrayList<String> values = new ArrayList<>();
        if (ev instanceof ArrayElementValue) {
            ElementValue[] arrayValues =
                ((ArrayElementValue) ev).getElementValuesArray();
            for (ElementValue value : arrayValues) {
                values.add(value.stringifyValue());
            }
        } else {
            values.add(ev.stringifyValue());
        }
        String[] result = new String[values.size()];
        return values.toArray(result);
    }

    /**
     * Processes the given <code>Set</code> with fragments for annotations and
     * stores the result in the corresponding <code>WebXml</code>. During this
     * operation <code>IHandlesTypesProcessor</code> will be invoked in order to
     * check for {@link HandlesTypes}.
     * 
     * @param fragment Representation of web application deployment descriptor where
     *        the annotations data will be stored
     * @param handlesTypesProcessor Processor that will check for {@link HandlesTypes}
     * @param handlesTypesOnly Specifies whether the metadata is complete
     */
    public static void processAnnotations(Set<WebXml> fragments,
            IHandlesTypesProcessor handlesTypesProcessor, boolean handlesTypesOnly) {
        for(WebXml fragment : fragments) {
            WebXml annotations = new WebXml();
            // no impact on distributable
            annotations.setDistributable(true);
            URL url = fragment.getURL();
            processAnnotationsUrl(url, annotations, handlesTypesProcessor,
                    (handlesTypesOnly || fragment.isMetadataComplete()));
            Set<WebXml> set = new HashSet<>();
            set.add(annotations);
            // Merge annotations into fragment - fragment takes priority
            fragment.merge(set);
        }
    }


    /**
     * Processes the given <code>URL</code> for annotations and stores the
     * result in the corresponding <code>WebXml</code>. During this operation
     * <code>IHandlesTypesProcessor</code> will be invoked in order to check for
     * {@link HandlesTypes}.
     * 
     * @param url <code>URL</code> that will be processed for annotations
     * @param fragment Representation of web application deployment descriptor where
     *        the annotations data will be stored
     * @param handlesTypesProcessor Processor that will check for {@link HandlesTypes}
     * @param handlesTypesOnly Specifies whether the metadata is complete
     */
    protected static void processAnnotationsUrl(URL url, WebXml fragment,
            IHandlesTypesProcessor handlesTypesProcessor, boolean handlesTypesOnly) {
        if (url == null) {
            // Nothing to do.
            return;
        } else if ("jar".equals(url.getProtocol())) {
            processAnnotationsJar(url, fragment, handlesTypesProcessor, handlesTypesOnly);
        } else if ("file".equals(url.getProtocol())) {
            try {
                processAnnotationsFile(
                        new File(url.toURI()), fragment, handlesTypesProcessor, handlesTypesOnly);
            } catch (URISyntaxException e) {
                log.error(sm.getString("annotationsProcessor.fileUrl", url), e);
            }
        } else {
            log.error(sm.getString("annotationsProcessor.unknownUrlProtocol",
                    url.getProtocol(), url));
        }

    }


    /**
     * Processes the given <code>URL</code> for annotations and stores the
     * result in the corresponding <code>WebXml</code>. During this operation
     * <code>IHandlesTypesProcessor</code> will be invoked in order to check for
     * {@link HandlesTypes}.
     * 
     * @param url <code>URL</code> that will be processed for annotations
     * @param fragment Representation of web application deployment descriptor where
     *        the annotations data will be stored
     * @param handlesTypesProcessor Processor that will check for {@link HandlesTypes}
     * @param handlesTypesOnly Specifies whether the metadata is complete
     */
    protected static void processAnnotationsJar(URL url, WebXml fragment,
            IHandlesTypesProcessor handlesTypesProcessor, boolean handlesTypesOnly) {

        Jar jar = null;
        InputStream is;

        try {
            jar = JarFactory.newInstance(url);

            jar.nextEntry();
            String entryName = jar.getEntryName();
            while (entryName != null) {
                if (entryName.endsWith(CLASS_EXTENSION)) {
                    is = null;
                    try {
                        is = jar.getEntryInputStream();
                        processAnnotationsStream(
                                is, fragment, handlesTypesProcessor, handlesTypesOnly);
                    } catch (IOException e) {
                        log.error(sm.getString("annotationsProcessor.inputStreamJar",
                                entryName, url),e);
                    } catch (ClassFormatException e) {
                        log.error(sm.getString("annotationsProcessor.inputStreamJar",
                                entryName, url),e);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException ioe) {
                                // Ignore
                            }
                        }
                    }
                }
                jar.nextEntry();
                entryName = jar.getEntryName();
            }
        } catch (IOException e) {
            log.error(sm.getString("annotationsProcessor.jarFile", url), e);
        } finally {
            if (jar != null) {
                jar.close();
            }
        }
    }


    /**
     * Processes the given <code>File</code> for annotations and stores the
     * result in the corresponding <code>WebXml</code>. During this operation
     * <code>IHandlesTypesProcessor</code> will be invoked in order to check for
     * {@link HandlesTypes}.
     * 
     * @param file <code>File</code> that will be processed for annotations
     * @param fragment Representation of web application deployment descriptor where
     *        the annotations data will be stored
     * @param handlesTypesProcessor Processor that will check for {@link HandlesTypes}
     * @param handlesTypesOnly Specifies whether the metadata is complete
     */
    public static void processAnnotationsFile(File file, WebXml fragment,
            IHandlesTypesProcessor handlesTypesProcessor, boolean handlesTypesOnly) {

        if (file.isDirectory()) {
            String[] dirs = file.list();
            for (String dir : dirs) {
                processAnnotationsFile(
                        new File(file,dir), fragment, handlesTypesProcessor, handlesTypesOnly);
            }
        } else if (file.canRead() && file.getName().endsWith(CLASS_EXTENSION)) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                processAnnotationsStream(fis, fragment, handlesTypesProcessor, handlesTypesOnly);
            } catch (IOException e) {
                log.error(sm.getString("annotationsProcessor.inputStreamFile",
                        file.getAbsolutePath()),e);
            } catch (ClassFormatException e) {
                log.error(sm.getString("annotationsProcessor.inputStreamFile",
                        file.getAbsolutePath()),e);
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (Throwable t) {
                        ExceptionUtils.handleThrowable(t);
                    }
                }
            }
        }
    }

}
