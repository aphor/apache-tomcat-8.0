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
package org.apache.tomcat.util.http.fileupload;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Default implementation of the {@link FileItemHeaders} interface.
 *
 * @author Michael C. Macaluso
 * @since 1.3
 */
public class FileItemHeadersImpl implements FileItemHeaders, Serializable {
    private static final long serialVersionUID = -4455695752627032559L;

    /**
     * Map of <code>String</code> keys to a <code>List</code> of
     * <code>String</code> instances.
     */
    private final Map<String,List<String>> headerNameToValueListMap =
        new HashMap<String,List<String>>();

    /**
     * List to preserve order of headers as added.  This would not be
     * needed if a <code>LinkedHashMap</code> could be used, but don't
     * want to depend on 1.4.
     */
    private final List<String> headerNameList = new ArrayList<String>();

    public String getHeader(String name) {
        String nameLower = name.toLowerCase();
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);
        if (null == headerValueList) {
            return null;
        }
        return headerValueList.get(0);
    }

    public Iterator<String> getHeaderNames() {
        return headerNameList.iterator();
    }

    public Iterator<String> getHeaders(String name) {
        String nameLower = name.toLowerCase();
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);
        if (null == headerValueList) {
            return Collections.<String>emptyList().iterator();
        }
        return headerValueList.iterator();
    }

    /**
     * Method to add header values to this instance.
     *
     * @param name name of this header
     * @param value value of this header
     */
    public synchronized void addHeader(String name, String value) {
        String nameLower = name.toLowerCase();
        List<String> headerValueList = headerNameToValueListMap.get(nameLower);
        if (null == headerValueList) {
            headerValueList = new ArrayList<String>();
            headerNameToValueListMap.put(nameLower, headerValueList);
            headerNameList.add(nameLower);
        }
        headerValueList.add(value);
    }
}
