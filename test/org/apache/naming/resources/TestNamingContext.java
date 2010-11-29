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
package org.apache.naming.resources;

import java.io.IOException;
import java.io.PrintWriter;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.ContextResource;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.TomcatBaseTest;
import org.apache.tomcat.util.buf.ByteChunk;

public class TestNamingContext extends TomcatBaseTest {

    public void testLookupSingletonResource() throws Exception {
        doTestLookup(true);
    }
    
    public void testLookupNonSingletonResource() throws Exception {
        doTestLookup(false);
    }
    
    public void doTestLookup(boolean useSingletonResource) throws Exception {
        Tomcat tomcat = getTomcatInstance();
        tomcat.enableNaming();
        
        // Must have a real docBase - just use temp
        StandardContext ctx = (StandardContext)
            tomcat.addContext("", System.getProperty("java.io.tmpdir"));
        
        // Create the resource
        ContextResource cr = new ContextResource();
        cr.setName("list/foo");
        cr.setType("org.apache.naming.resources.TesterObject");
        cr.setProperty("factory", "org.apache.naming.resources.TesterFactory");
        cr.setSingleton(useSingletonResource);
        ctx.getNamingResources().addResource(cr);
        
        // Map the test Servlet
        Bug49994Servlet bug49994Servlet = new Bug49994Servlet();
        Tomcat.addServlet(ctx, "bug49994Servlet", bug49994Servlet);
        ctx.addServletMapping("/", "bug49994Servlet");

        tomcat.start();

        ByteChunk bc = getUrl("http://localhost:" + getPort() + "/");
        
        String expected;
        if (useSingletonResource) {
            expected = "EQUAL";
        } else {
            expected = "NOTEQUAL";
        }
        assertEquals(expected, bc.toString());

    }

    public final class Bug49994Servlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            resp.setContentType("text/plain;UTF-8");
            PrintWriter out = resp.getWriter();

            try {
                Context ctx = new InitialContext();
                Object obj1 = ctx.lookup("java:comp/env/list/foo");
                Object obj2 = ctx.lookup("java:comp/env/list/foo");
                if (obj1 == obj2) {
                    out.print("EQUAL");
                } else {
                    out.print("NOTEQUAL");
                }
            } catch (NamingException ne) {
                ne.printStackTrace(out);
            }
        }
    }

    public void testListBindings() throws Exception {
        Tomcat tomcat = getTomcatInstance();
        tomcat.enableNaming();
        
        // Must have a real docBase - just use temp
        StandardContext ctx = (StandardContext)
            tomcat.addContext("", System.getProperty("java.io.tmpdir"));
        
        // Create the resource
        ContextResource cr = new ContextResource();
        cr.setName("list/foo");
        cr.setType("org.apache.naming.resources.TesterObject");
        cr.setProperty("factory", "org.apache.naming.resources.TesterFactory");
        ctx.getNamingResources().addResource(cr);
        
        // Map the test Servlet
        Bug23950Servlet bug23950Servlet = new Bug23950Servlet();
        Tomcat.addServlet(ctx, "bug23950Servlet", bug23950Servlet);
        ctx.addServletMapping("/", "bug23950Servlet");

        tomcat.start();

        ByteChunk bc = getUrl("http://localhost:" + getPort() + "/");
        assertEquals("org.apache.naming.resources.TesterObject", bc.toString());
    }
    
    public final class Bug23950Servlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            resp.setContentType("text/plain;UTF-8");
            PrintWriter out = resp.getWriter();

            try {
                Context ctx = new InitialContext();
                NamingEnumeration<Binding> enm =
                    ctx.listBindings("java:comp/env/list");
                while (enm.hasMore()) {
                    Binding b = enm.next();
                    out.print(b.getObject().getClass().getName());
                }
            } catch (NamingException ne) {
                ne.printStackTrace(out);
            }
        }
    }
    
    public void testBeanFactory() throws Exception {
        Tomcat tomcat = getTomcatInstance();
        tomcat.enableNaming();
        
        // Must have a real docBase - just use temp
        StandardContext ctx = (StandardContext)
            tomcat.addContext("", System.getProperty("java.io.tmpdir"));
        
        // Create the resource
        ContextResource cr = new ContextResource();
        cr.setName("bug50351");
        cr.setType("org.apache.naming.resources.TesterObject");
        cr.setProperty("factory", "org.apache.naming.factory.BeanFactory");
        cr.setProperty("foo", "value");
        ctx.getNamingResources().addResource(cr);
        
        // Map the test Servlet
        Bug50351Servlet bug50351Servlet = new Bug50351Servlet();
        Tomcat.addServlet(ctx, "bug50351Servlet", bug50351Servlet);
        ctx.addServletMapping("/", "bug50351Servlet");

        tomcat.start();

        ByteChunk bc = getUrl("http://localhost:" + getPort() + "/");
        assertEquals("value", bc.toString());
    }

    public final class Bug50351Servlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {

            resp.setContentType("text/plain;UTF-8");
            PrintWriter out = resp.getWriter();

            try {
                Context ctx = new InitialContext();
                Object obj = ctx.lookup("java:comp/env/bug50351");
                TesterObject to = (TesterObject) obj;
                out.print(to.getFoo());
            } catch (NamingException ne) {
                ne.printStackTrace(out);
            }
        }
    }
    

}
