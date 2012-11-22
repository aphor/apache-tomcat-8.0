package org.apache.tomcat.util.xml.generator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.bcel.classfile.ClassFormatException;
import org.apache.tomcat.util.res.StringManager;
import org.apache.tomcat.util.xml.WebXml;
import org.apache.tomcat.util.xml.parser.WebXmlParser;
import org.apache.tomcat.util.xml.processor.AnnotationsProcessor;
import org.apache.tomcat.util.xml.scanner.FragmentJarScanner;
import org.xml.sax.InputSource;

/**
 * Offline generator for effective web.xml
 */
public class EffectiveWebXmlGenerator {

    private static final Log log = LogFactory
            .getLog(EffectiveWebXmlGenerator.class);

    /**
     * The string resources for this package.
     */
    private static final StringManager sm = StringManager
            .getManager(Constants.Package);

    /**
     * The path to the war file that will be analyzed
     */
    private static String warFile = null;

    /**
     * XML namespace validation
     */
    private static boolean xmlNamespaceAware = false;

    /**
     * XML validation
     */
    private static boolean xmlValidation = false;

    /**
     * The list of JARs that will be skipped when scanning a web application for
     * JARs. This means the JAR will not be scanned for web fragments, SCIs,
     * annotations or classes that match @HandlesTypes.
     */
    private static final Set<String> pluggabilityJarsToSkip = new HashSet<>();

    /**
     * @param args
     *            The args must contain one argument - a properties file. The
     *            file contains information for: the path to the war file, xml
     *            namespace validation, xml validation and the jar files that
     *            have to be skiped when searching for web fragments.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args == null || args.length != 1) {
            return;
        }

        initProperties(args[0]);

        if (warFile != null) {
            boolean ok = true;

            WebXmlParser webXmlParser = new WebXmlParser(xmlNamespaceAware,
                    xmlValidation);

            WebXml webXml = new WebXml();

            Path war = Paths.get(warFile).normalize();
            Path temporaryDir = war.getParent().resolve("temp");

            JarFile jarFile = null;
            try {
                jarFile = new JarFile(war.toFile());

                // Parse context level web.xml
                InputSource contextWebXml = getContextWebXmlSource(jarFile, war);
                if (!webXmlParser.parseWebXml(contextWebXml, webXml, false)) {
                    ok = false;
                }

                // Identify all the JARs packaged with the application
                // If the JARs have a web-fragment.xml it will be parsed at this
                // point.
                Boolean parseOperationSuccessful = new Boolean(true);
                Map<String, WebXml> fragments = FragmentJarScanner
                        .processJarsForWebFragments(new WarScanner(jarFile,
                                temporaryDir), webXmlParser, null, null,
                                pluggabilityJarsToSkip,
                                parseOperationSuccessful);

                if (!parseOperationSuccessful) {
                    ok = false;
                }

                // Order the fragments.
                Set<WebXml> orderedFragments = null;
                orderedFragments = WebXml.orderWebFragments(webXml, fragments);

                if (!webXml.isMetadataComplete()) {
                    // Process /WEB-INF/classes for annotations
                    if (ok) {
                        processAnnotations(jarFile, webXml,
                                webXml.isMetadataComplete());
                    }

                    // Process JARs for annotations - only need to process
                    // those fragments we are going to use
                    if (ok) {
                        AnnotationsProcessor.processAnnotations(
                                orderedFragments, null,
                                webXml.isMetadataComplete());
                    }
                    
                    if (ok) {
                        // Merge web-fragment.xml files into the main web.xml file.
                        webXml.merge(orderedFragments);
                    }
                }
                
                String mergedWebXml = webXml.toXml();
                Files.write(war.getParent().resolve("effective-web.xml"), mergedWebXml.getBytes());
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
                try {
                    recursiveDelete(temporaryDir);
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static void initProperties(String properties) throws IOException {
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(properties);
            props.load(fis);
            warFile = props.getProperty("warFile");
            xmlNamespaceAware = Boolean.parseBoolean(props.getProperty(
                    "xmlNamespaceAware", "false"));
            xmlValidation = Boolean.parseBoolean(props.getProperty(
                    "xmlValidation", "false"));
            addJarsToSkip(props.getProperty("jarsToSkip"));
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static void addJarsToSkip(String jarsToSkip) {
        if (jarsToSkip != null) {
            StringTokenizer tokenizer = new StringTokenizer(jarsToSkip, ",");
            while (tokenizer.hasMoreElements()) {
                pluggabilityJarsToSkip.add(tokenizer.nextToken());
            }
        }
    }

    /**
     * Identify the application web.xml to be used and obtain an input source
     * for it.
     */
    private static InputSource getContextWebXmlSource(JarFile jarFile, Path war) {
        InputStream stream = null;
        InputSource source = null;
        URL url = null;

        JarEntry entry = jarFile.getJarEntry(Constants.APPLICATION_WEB_XML);
        if (entry != null) {
            try {
                stream = jarFile.getInputStream(entry);
                url = new URL(Constants.JAR_SCHEMA + war.toUri().toURL()
                        + Constants.JAR_TO_ENTRY_SEPARATOR
                        + Constants.APPLICATION_WEB_XML);
            } catch (IOException e) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            sm.getString("effectiveWebXmlGenerator.applicationMissing"),
                            e);
                }
            }
        }

        if (stream == null || url == null) {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        } else {
            source = new InputSource(url.toExternalForm());
            source.setByteStream(stream);
        }

        return source;
    }

    private static void processAnnotations(JarFile jarFile, WebXml fragment,
            boolean handlesTypesOnly) {
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String entryName = jarEntry.getName();
            if (entryName.startsWith(Constants.WEBINF_CLASSES_ENTRY_PREFIX)
                    && entryName.endsWith(Constants.CLASS_SUFFIX)) {
                InputStream is = null;
                try {
                    is = jarFile.getInputStream(jarEntry);
                    AnnotationsProcessor.processAnnotationsStream(is, fragment,
                            null, handlesTypesOnly);
                } catch (IOException | ClassFormatException e) {
                    log.error(sm.getString(
                            "effectiveWebXmlGenerator.inputStreamWebResource",
                            entryName), e);
                }
            }
        }
    }

    private static void recursiveDelete(Path temporaryDir) throws IOException {
        Files.walkFileTree(temporaryDir, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                    throws IOException {
                if (exc == null) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
    }
}
