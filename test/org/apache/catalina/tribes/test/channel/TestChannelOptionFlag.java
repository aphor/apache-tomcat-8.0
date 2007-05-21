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
package org.apache.catalina.tribes.test.channel;

import junit.framework.*;
import org.apache.catalina.tribes.group.*;
import org.apache.catalina.tribes.ChannelInterceptor;
import org.apache.catalina.tribes.ChannelException;

/**
 * <p>Title: </p> 
 * 
 * <p>Description: </p> 
 * 
 * <p>Company: </p>
 * 
 * @author not attributable
 * @version 1.0
 */
public class TestChannelOptionFlag extends TestCase {
    GroupChannel channel = null;
    protected void setUp() throws Exception {
        super.setUp();
        channel = new GroupChannel();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if ( channel != null ) try {channel.stop(channel.DEFAULT);}catch ( Exception ignore) {}
        channel = null;
    }
    
    
    public void testOptionConflict() throws Exception {
        boolean error = false;
        channel.setOptionCheck(true);
        ChannelInterceptor i = new TestInterceptor();
        i.setOptionFlag(128);
        channel.addInterceptor(i);
        i = new TestInterceptor();
        i.setOptionFlag(128);
        channel.addInterceptor(i);
        try {
            channel.start(channel.DEFAULT);
        }catch ( ChannelException x ) {
            if ( x.getMessage().indexOf("option flag conflict") >= 0 ) error = true;
        }
        assertEquals(true,error);
    }

    public void testOptionNoConflict() throws Exception {
        boolean error = false;
        channel.setOptionCheck(true);
        ChannelInterceptor i = new TestInterceptor();
        i.setOptionFlag(128);
        channel.addInterceptor(i);
        i = new TestInterceptor();
        i.setOptionFlag(64);
        channel.addInterceptor(i);
        i = new TestInterceptor();
        i.setOptionFlag(256);
        channel.addInterceptor(i);
        try {
            channel.start(channel.DEFAULT);
        }catch ( ChannelException x ) {
            if ( x.getMessage().indexOf("option flag conflict") >= 0 ) error = true;
        }
        assertEquals(false,error);
    }
    
    public static class TestInterceptor extends ChannelInterceptorBase {
        
    }


}
