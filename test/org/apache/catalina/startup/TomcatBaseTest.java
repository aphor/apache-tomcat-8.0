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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.AprLifecycleListener;

import org.apache.tomcat.util.buf.ByteChunk;

import junit.framework.TestCase;

/**
 * Base test case that provides a Tomcat instance for each test - mainly so we
 * don't have to keep writing the cleanup code.
 */
public abstract class TomcatBaseTest extends TestCase {
    private Tomcat tomcat;
    private File tempDir;
    private static int port = 8000;

    /**
     * Make Tomcat instance accessible to sub-classes.
     */
    public Tomcat getTomcatInstance() {
        return tomcat;
    }

    /**
     * Sub-classes need to know port so they can connect
     */
    public int getPort() {
        return port;
    }
    
    /**
     * Sub-classes may want to add connectors on a new port
     */
    public int getNextPort() {
        port++;
        return getPort();
    }

    /**
     * Helper method that returns the path of the temporary directory used by
     * the test runs. The directory is configured during {@link #setUp()} and is
     * deleted at {@link #tearDown()}.
     */
    public File getTemporaryDirectory() {
        return tempDir;
    }

    /**
     * Helper method that returns the directory where Tomcat build resides. It
     * is used to access resources that are part of default Tomcat deployment.
     * E.g. the examples webapp.
     */
    public File getBuildDirectory() {
        return new File(System.getProperty("tomcat.test.tomcatbuild",
                "output/build"));
    }

    @Override
    public void setUp() throws Exception {
        // Need to use JULI so log messages from the tests are visible
        System.setProperty("java.util.logging.manager",
                "org.apache.juli.ClassLoaderLogManager");

        tempDir = new File(System.getProperty("tomcat.test.temp", "output/tmp"));
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            fail("Unable to create temporary directory for test");
        }
        
        System.setProperty("catalina.base", tempDir.getAbsolutePath());
        
        File appBase = new File(tempDir, "webapps");
        if (!appBase.exists() && !appBase.mkdir()) {
            fail("Unable to create appBase for test");
        }
        
        tomcat = new Tomcat();

        // Add AprLifecycleListener
        StandardServer server = (StandardServer) tomcat.getServer();
        AprLifecycleListener listener = new AprLifecycleListener();
        server.addLifecycleListener(listener);
        
        tomcat.setBaseDir(tempDir.getAbsolutePath());
        tomcat.getHost().setAppBase(appBase.getAbsolutePath());
          
        // If each test is running on same port - they
        // may interfere with each other (on unix at least)
        tomcat.setPort(getNextPort());
    }
    
    @Override
    public void tearDown() throws Exception {
        // Some tests may call tomcat.destroy(), some tests may just call
        // tomcat.stop(), some not call either method. Make sure that stop() &
        // destroy() are called as necessary.
        if (tomcat.server != null &&
                tomcat.server.getState() != LifecycleState.DESTROYED) {
            if (tomcat.server.getState() != LifecycleState.STOPPED) {
                tomcat.stop();
            }
            tomcat.destroy();
        }
        ExpandWar.delete(tempDir);
    }
    
    /**
     * Simple Hello World servlet for use by test cases
     */
    public static final class HelloWorldServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            PrintWriter out = resp.getWriter();
            out.print("<html><body><p>Hello World</p></body></html>");
        }
    }
    

    /**
     *  Wrapper for getting the response.
     */
    public static ByteChunk getUrl(String path) throws IOException {
        ByteChunk out = new ByteChunk();
        getUrl(path, out, null);
        return out;
    }

    public static int getUrl(String path, ByteChunk out,
            Map<String, List<String>> resHead) throws IOException {

        URL url = new URL(path);
        HttpURLConnection connection = 
            (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(1000000);
        connection.connect();
        int rc = connection.getResponseCode();
        if (resHead != null) {
            Map<String, List<String>> head = connection.getHeaderFields();
            resHead.putAll(head);
        }
        InputStream is = connection.getInputStream();
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(is);
            byte[] buf = new byte[2048];
            int rd = 0;
            while((rd = bis.read(buf)) > 0) {
                out.append(buf, 0, rd);
            }
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return rc;
    }
}
