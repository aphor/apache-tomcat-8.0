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

package org.apache.catalina.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.imageio.ImageIO;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.res.StringManager;

/**
 * Provide a workaround for known places where the Java Runtime environment can
 * cause a memory leak or lock files.
 * <p>
 * Memory leaks occur when JRE code uses
 * the context class loader to load a singleton as this will cause a memory leak
 * if a web application class loader happens to be the context class loader at
 * the time. The work-around is to initialise these singletons when Tomcat's
 * common class loader is the context class loader.
 * <p>
 * Locked usually files occur when a resource inside a JAR is accessed without
 * first disabling Jar URL connection caching. The workaround is to disable this
 * caching by default. 
 */
public class JreMemoryLeakPreventionListener implements LifecycleListener {

    protected static final Log log =
        LogFactory.getLog(JreMemoryLeakPreventionListener.class);
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);
    
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        // Initialise these classes when Tomcat starts
        if (Lifecycle.INIT_EVENT.equals(event.getType())) {
            /*
             * Several components end up calling:
             * sun.awt.AppContext.getAppContext()
             * 
             * Those libraries / components known to trigger memory leaks due to
             * eventual calls to getAppContext() are:
             * - Google Web Toolkit via its use of javax.imageio
             * - Tomcat via its use of java.beans.Introspector.flushCaches() in
             *   1.6.0_15 onwards
             * - others TBD
             */
            
            // Trigger a call to sun.awt.AppContext.getAppContext(). This will
            // pin the common class loader in memory but that shouldn't be an
            // issue.
            ImageIO.getCacheDirectory();
            
            /*
             * Several components end up opening JarURLConnections without first
             * disabling chaching. This effectively locks the file. Whilst more
             * noticeable and harder to ignore on Windows, it affects all
             * operating systems.
             * 
             * Those libraries/components known to trigger this issue include:
             * - log4j versions 1.2.15 and earlier
             * - javax.xml.bind.JAXBContext.newInstance()
             */
            
            // Set the default JAR URL caching policy to not to cache
            try {
                // Doesn't matter that this JAR doesn't exist - just as long as
                // the URL is well-formed
                URL url = new URL("jar:file://dummy.jar!/dummy.txt");
                URLConnection uConn = url.openConnection();
                uConn.setDefaultUseCaches(false);
            } catch (MalformedURLException e) {
                log.error(sm.getString(
                        "jreLeakListener.jarUrlConnCacheFail"), e);
            } catch (IOException e) {
                log.error(sm.getString(
                "jreLeakListener.jarUrlConnCacheFail"), e);
            }
        }
    }

}
