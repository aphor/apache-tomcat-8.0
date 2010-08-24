/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.catalina.filters;

import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.core.DummyResponse;
import org.apache.catalina.startup.TomcatBaseTest;

public class TestCsrfPreventionFilter extends TomcatBaseTest {

    private static final String RESULT_NONCE =
        Constants.CSRF_NONCE_SESSION_ATTR_NAME + "=TESTNONCE";

    private final HttpServletResponse wrapper =
        new CsrfPreventionFilter.CsrfResponseWrapper(
                new NonEncodingResponse(), "TESTNONCE");

    public void testAddNonceNoQueryNoAnchor() throws Exception {
        assertEquals("/test?" + RESULT_NONCE ,
                wrapper.encodeRedirectURL("/test"));
    }
    
    public void testAddNonceQueryNoAnchor() throws Exception {
        assertEquals("/test?a=b&" + RESULT_NONCE ,
                wrapper.encodeRedirectURL("/test?a=b"));
    }
    
    public void testAddNonceNoQueryAnchor() throws Exception {
        assertEquals("/test?" + RESULT_NONCE + "#c",
                wrapper.encodeRedirectURL("/test#c"));
    }
    
    public void testAddNonceQueryAnchor() throws Exception {
        assertEquals("/test?a=b&" + RESULT_NONCE + "#c",
                wrapper.encodeRedirectURL("/test?a=b#c"));
    }
    
    private static class NonEncodingResponse extends DummyResponse {

        @Override
        public String encodeRedirectURL(String url) {
            return url;
        }

        @Override
        public String encodeRedirectUrl(String url) {
            return url;
        }

        @Override
        public String encodeURL(String url) {
            return url;
        }

        @Override
        public String encodeUrl(String url) {
            return url;
        }
    }
}
