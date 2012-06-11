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
package org.apache.catalina.valves;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Test;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.TomcatBaseTest;
import org.apache.tomcat.util.buf.ByteChunk;

public class TestErrorReportValve extends TomcatBaseTest {

    @Test
    public void testBug53071() throws Exception {
        Tomcat tomcat = getTomcatInstance();

        // Must have a real docBase - just use temp
        Context ctx =
            tomcat.addContext("", System.getProperty("java.io.tmpdir"));

        Tomcat.addServlet(ctx, "errorServlet", new ErrorServlet());
        ctx.addServletMapping("/", "errorServlet");

        tomcat.start();

        ByteChunk res = getUrl("http://localhost:" + getPort());

        Assert.assertTrue(res.toString().contains(ErrorServlet.ERROR_TEXT));
    }


    private static final class ErrorServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;
        private static final String ERROR_TEXT = "The wheels fell off.";
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp)
                throws ServletException, IOException {
            req.setAttribute(RequestDispatcher.ERROR_EXCEPTION,
                    new Throwable(ERROR_TEXT));
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
