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

package org.apache.catalina.filters;

import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.juli.logging.Log;
import org.apache.tomcat.util.IntrospectionUtils;
import org.apache.tomcat.util.res.StringManager;

/**
 * Base class for filters that provide some utility methods. 
 * 
 * @author xxd
 *
 */
public abstract class FilterBase implements Filter {
    
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);

    public void init(FilterConfig filterConfig) throws ServletException {
        Enumeration<String> paramNames = filterConfig.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (!IntrospectionUtils.setProperty(this, paramName,
                    filterConfig.getInitParameter(paramName))) {
                getLogger().warn(sm.getString("filterbase.noSuchProperty",
                        paramName, this.getClass().getName()));
            }
        }    
    }

    /**
     * Whether the request object is an HttpServletRequest or not.
     * 
     * @param request
     * @return
     */
    protected boolean isHttpServletRequest(ServletRequest request) {
        return request instanceof HttpServletRequest;
    }

    /**
     * Whether the response object is an HttpServletResponse or not.
     * 
     * @param response
     * @return
     */
    protected boolean isHttpServletResponse(ServletResponse response) {
        return response instanceof HttpServletResponse;
    }

    /**
     * Whether the corresponding Servlet is an HttpServlet or not.
     * 
     * @param request
     * @param response
     * @return
     */
    protected boolean isHttpServlet(ServletRequest request,
            ServletResponse response) {
        return isHttpServletRequest(request) && isHttpServletResponse(response);
    }

    protected abstract Log getLogger();
    
}
