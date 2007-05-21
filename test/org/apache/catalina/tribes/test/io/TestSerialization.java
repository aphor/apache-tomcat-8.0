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
package org.apache.catalina.tribes.test.io;

import org.apache.catalina.tribes.io.XByteBuffer;
import junit.framework.TestCase;

public class TestSerialization extends TestCase {
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    public void testEmptyArray() throws Exception {
        
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public static void main(String[] args) throws Exception {
        //XByteBuffer.deserialize(new byte[0]);
        XByteBuffer.deserialize(new byte[] {-84, -19, 0, 5, 115, 114, 0, 17, 106});
    }

}
