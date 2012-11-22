package org.apache.tomcat.util.xml.generator;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.servlet.ServletContext;

import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.file.Matcher;
import org.apache.tomcat.util.res.StringManager;

/**
 * The class will scan the web application archive for jar files that may
 * contain web fragments.
 */
class WarScanner implements JarScanner {

    private static final Log log = LogFactory.getLog(WarScanner.class);

    /**
     * The string resources for this package.
     */
    private static final StringManager sm = StringManager
            .getManager(Constants.Package);

    /**
     * The jar file that will be scanned.
     */
    private JarFile jarFile;

    /**
     * The temporary directory where the extracted jar entries will be stored.
     */
    private Path temporaryDir;

    /**
     * Initializes <code>WarScanner</code>
     * 
     * @param jarFile The jar file that will be scanned
     * @param temporaryDir The temporary directory where the extracted jar entries will
     *        be stored
     */
    WarScanner(JarFile jarFile, Path temp) {
        this.jarFile = jarFile;
        this.temporaryDir = temp;
    }

    @Override
    public void scan(ServletContext context, ClassLoader classloader,
            JarScannerCallback callback, Set<String> jarsToSkip) {

        if (log.isTraceEnabled()) {
            log.trace(sm.getString("warScanner.webinflibStart"));
        }

        Set<String[]> ignoredJarsTokens = new HashSet<>();
        for (String pattern : jarsToSkip) {
            ignoredJarsTokens.add(Matcher.tokenizePathAsArray(pattern));
        }

        Enumeration<JarEntry> jarEntries = this.jarFile.entries();
        while (jarEntries.hasMoreElements()) {
            JarEntry jarEntry = jarEntries.nextElement();
            String entryName = jarEntry.getName();
            if (entryName.startsWith(Constants.WEBINF_LIB_ENTRY_PREFIX)
                    && entryName.endsWith(Constants.JAR_SUFFIX)
                    && !Matcher
                            .matchPath(ignoredJarsTokens, entryName
                                    .substring(entryName.lastIndexOf('/') + 1))) {
                if (log.isDebugEnabled()) {
                    log.debug(sm.getString("warScanner.webinflibJarScan",
                            entryName));
                }
                URL url = null;
                try {
                    Path destinationFile = extractFile(this.jarFile, jarEntry,
                            this.temporaryDir);
                    if (destinationFile != null) {
                        url = new URL(Constants.JAR_SCHEMA
                                + destinationFile.toUri().toURL()
                                + Constants.JAR_TO_ENTRY_SEPARATOR);
                        URLConnection conn = url.openConnection();
                        callback.scan((JarURLConnection) conn);
                    }
                } catch (IOException e) {
                    log.warn(sm.getString("warScanner.webinflibFail", url), e);
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(sm.getString("warScanner.webinflibJarNoScan",
                            entryName));
                }
            }
        }
    }

    /**
     * Extracts the specified jar entry.
     * 
     * @param jarFile The jar file that contains the jar entry
     * @param jarEntry The jar entry that will be extracted
     * @param destinationDir The destination directory where the jar entry will be
     *        extracted
     * @return The resulting file
     * @throws IOException If exception occurs during extraction process
     */
    private Path extractFile(JarFile jarFile, JarEntry jarEntry,
            Path destinationDir) throws IOException {
        Path destinationFile = destinationDir.resolve(jarEntry.getName());

        if (Files.isDirectory(destinationFile)) {
            return null;
        }

        Files.createDirectories(destinationFile.getParent());

        Files.copy(jarFile.getInputStream(jarEntry), destinationFile);

        return destinationFile;
    }
}
