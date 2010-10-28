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
package org.apache.tomcat.util.http.mapper;

import junit.framework.TestCase;

import org.apache.tomcat.util.buf.MessageBytes;

public class TestMapper extends TestCase {

    private Mapper mapper;

    @Override
    protected void setUp() throws Exception {
        mapper = new Mapper();

        mapper.addHost("sjbjdvwsbvhrb", new String[0], "blah1");
        mapper.addHost("sjbjdvwsbvhr/", new String[0], "blah1");
        mapper.addHost("wekhfewuifweuibf", new String[0], "blah2");
        mapper.addHost("ylwrehirkuewh", new String[0], "blah3");
        mapper.addHost("iohgeoihro", new String[0], "blah4");
        mapper.addHost("fwehoihoihwfeo", new String[0], "blah5");
        mapper.addHost("owefojiwefoi", new String[0], "blah6");
        mapper.addHost("iowejoiejfoiew", new String[0], "blah7");
        mapper.addHost("iowejoiejfoiew", new String[0], "blah17");
        mapper.addHost("ohewoihfewoih", new String[0], "blah8");
        mapper.addHost("fewohfoweoih", new String[0], "blah9");
        mapper.addHost("ttthtiuhwoih", new String[0], "blah10");
        mapper.addHost("lkwefjwojweffewoih", new String[0], "blah11");
        mapper.addHost("zzzuyopjvewpovewjhfewoih", new String[0], "blah12");
        mapper.addHost("xxxxgqwiwoih", new String[0], "blah13");
        mapper.addHost("qwigqwiwoih", new String[0], "blah14");

        mapper.setDefaultHostName("ylwrehirkuewh");

        String[] welcomes = new String[2];
        welcomes[0] = "boo/baba";
        welcomes[1] = "bobou";
        
        mapper.addContext("iowejoiejfoiew", "blah7", "",
                "context0", new String[0], null);
        mapper.addContext("iowejoiejfoiew", "blah7", "/foo",
                "context1", new String[0], null);
        mapper.addContext("iowejoiejfoiew", "blah7", "/foo/bar",
                "context2", welcomes, null);
        mapper.addContext("iowejoiejfoiew", "blah7", "/foo/bar/bla",
                "context3", new String[0], null);

        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/fo/*",
                "wrapper0", false, false);
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/",
                "wrapper1", false, false);
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/blh",
                "wrapper2", false, false);
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "*.jsp",
                "wrapper3", false, false);
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/blah/bou/*",
                "wrapper4", false, false);
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "/blah/bobou/*",
                "wrapper5", false, false);
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar", "*.htm",
                "wrapper6", false, false);
        mapper.addWrapper("iowejoiejfoiew", "/foo/bar/bla", "/bobou/*",
                "wrapper7", false, false);
    }
    

    public void testAddHost() throws Exception {
        // Check we have the right number (add 16 but one is a duplicate)
        assertEquals(15, mapper.hosts.length);
        
        // Make sure adding a duplicate *does not* overwrite
        assertEquals("blah7", mapper.hosts[3].object);
        
        // Check for alphabetical order of host names
        String previous;
        String current = mapper.hosts[0].name;
        for (int i = 1; i < mapper.hosts.length; i++) {
            previous = current;
            current = mapper.hosts[i].name;
            assertTrue(previous.compareTo(current) < 0);
        }
    }
    
    
    public void testMap() throws Exception {
        MappingData mappingData = new MappingData();
        MessageBytes host = MessageBytes.newInstance();
        host.setString("iowejoiejfoiew");
        MessageBytes uri = MessageBytes.newInstance();
        uri.setString("/foo/bar/blah/bobou/foo");
        uri.toChars();
        uri.getCharChunk().setLimit(-1);

        mapper.map(host, uri, mappingData);
        assertEquals("blah7", mappingData.host);
        assertEquals("context2", mappingData.context);
        assertEquals("wrapper5", mappingData.wrapper);
        assertEquals("/foo/bar", mappingData.contextPath.toString());
        assertEquals("/blah/bobou", mappingData.wrapperPath.toString());
        assertEquals("/foo", mappingData.pathInfo.toString());
        assertTrue(mappingData.redirectPath.isNull());

        mappingData.recycle();
        uri.recycle();
        uri.setString("/foo/bar/bla/bobou/foo");
        uri.toChars();
        uri.getCharChunk().setLimit(-1);
        mapper.map(host, uri, mappingData);
        assertEquals("blah7", mappingData.host);
        assertEquals("context3", mappingData.context);
        assertEquals("wrapper7", mappingData.wrapper);
        assertEquals("/foo/bar/bla", mappingData.contextPath.toString());
        assertEquals("/bobou", mappingData.wrapperPath.toString());
        assertEquals("/foo", mappingData.pathInfo.toString());
        assertTrue(mappingData.redirectPath.isNull());
    }
    
    
    public void testPerformance() throws Exception {
        MappingData mappingData = new MappingData();
        MessageBytes host = MessageBytes.newInstance();
        host.setString("iowejoiejfoiew");
        MessageBytes uri = MessageBytes.newInstance();
        uri.setString("/foo/bar/blah/bobou/foo");
        uri.toChars();
        uri.getCharChunk().setLimit(-1);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            mappingData.recycle();
            mapper.map(host, uri, mappingData);
        }
        long time = System.currentTimeMillis() - start;
        
        // Takes ~1s on markt's laptop. If this takes more than 3s something
        // probably needs looking at. If this fails repeatedly then we may need
        // to increase this limit.
        assertTrue(time < 3000);
    }
}
