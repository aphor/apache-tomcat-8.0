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

package org.apache.coyote.ajp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ObjectName;

import org.apache.coyote.RequestGroupInfo;
import org.apache.coyote.RequestInfo;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.ExceptionUtils;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.net.AprEndpoint;
import org.apache.tomcat.util.net.SocketStatus;
import org.apache.tomcat.util.net.SocketWrapper;
import org.apache.tomcat.util.net.AprEndpoint.Handler;


/**
 * Abstract the protocol implementation, including threading, etc.
 * Processor is single threaded and specific to stream-based protocols,
 * will not fit Jk protocols like JNI.
 *
 * @author Remy Maucherat
 * @author Costin Manolache
 */
public class AjpAprProtocol extends AbstractAjpProtocol {
    
    
    private static final Log log = LogFactory.getLog(AjpAprProtocol.class);

    @Override
    protected Log getLog() { return log; }

    // ------------------------------------------------------------ Constructor


    public AjpAprProtocol() {
        endpoint = new AprEndpoint();
        cHandler = new AjpConnectionHandler(this);
        setSoLinger(Constants.DEFAULT_CONNECTION_LINGER);
        setSoTimeout(Constants.DEFAULT_CONNECTION_TIMEOUT);
        //setServerSoTimeout(Constants.DEFAULT_SERVER_SOCKET_TIMEOUT);
        setTcpNoDelay(Constants.DEFAULT_TCP_NO_DELAY);
    }

    
    // ----------------------------------------------------- Instance Variables


    /**
     * Connection handler for AJP.
     */
    private AjpConnectionHandler cHandler;


    // --------------------------------------------------------- Public Methods


    /** Start the protocol
     */
    @Override
    public void init() throws Exception {
        endpoint.setName(getName());
        ((AprEndpoint)endpoint).setHandler(cHandler);
        ((AprEndpoint)endpoint).setUseSendfile(false);

        try {
            endpoint.init();
        } catch (Exception ex) {
            log.error(sm.getString("ajpprotocol.endpoint.initerror"), ex);
            throw ex;
        }
        if (log.isInfoEnabled()) {
            log.info(sm.getString("ajpprotocol.init", getName()));
        }
    }


    @Override
    public void start() throws Exception {
        if (this.domain != null ) {
            try {
                tpOname = new ObjectName
                    (domain + ":" + "type=ThreadPool,name=" + getName());
                Registry.getRegistry(null, null)
                    .registerComponent(endpoint, tpOname, null );
            } catch (Exception e) {
                log.error("Can't register threadpool" );
            }
            rgOname = new ObjectName
                (domain + ":type=GlobalRequestProcessor,name=" + getName());
            Registry.getRegistry(null, null).registerComponent
                (cHandler.global, rgOname, null);
        }

        try {
            endpoint.start();
        } catch (Exception ex) {
            log.error(sm.getString("ajpprotocol.endpoint.starterror"), ex);
            throw ex;
        }
        if (log.isInfoEnabled())
            log.info(sm.getString("ajpprotocol.start", getName()));
    }


    public boolean getUseSendfile() { return endpoint.getUseSendfile(); }
    public void setUseSendfile(@SuppressWarnings("unused") boolean useSendfile) {
        /* No sendfile for AJP */
    }

    public int getPollTime() { return ((AprEndpoint)endpoint).getPollTime(); }
    public void setPollTime(int pollTime) { ((AprEndpoint)endpoint).setPollTime(pollTime); }

    public void setPollerSize(int pollerSize) { ((AprEndpoint)endpoint).setPollerSize(pollerSize); }
    public int getPollerSize() { return ((AprEndpoint)endpoint).getPollerSize(); }

    // --------------------------------------  AjpConnectionHandler Inner Class


    protected static class AjpConnectionHandler implements Handler {

        protected AjpAprProtocol proto;
        protected AtomicLong registerCount = new AtomicLong(0);
        protected RequestGroupInfo global = new RequestGroupInfo();

        protected ConcurrentHashMap<SocketWrapper<Long>, AjpAprProcessor> connections =
            new ConcurrentHashMap<SocketWrapper<Long>, AjpAprProcessor>();

        protected ConcurrentLinkedQueue<AjpAprProcessor> recycledProcessors = 
            new ConcurrentLinkedQueue<AjpAprProcessor>() {
            private static final long serialVersionUID = 1L;
            protected AtomicInteger size = new AtomicInteger(0);
            @Override
            public boolean offer(AjpAprProcessor processor) {
                boolean offer = (proto.processorCache == -1) ? true : (size.get() < proto.processorCache);
                //avoid over growing our cache or add after we have stopped
                boolean result = false;
                if ( offer ) {
                    result = super.offer(processor);
                    if ( result ) {
                        size.incrementAndGet();
                    }
                }
                if (!result) unregister(processor);
                return result;
            }
            
            @Override
            public AjpAprProcessor poll() {
                AjpAprProcessor result = super.poll();
                if ( result != null ) {
                    size.decrementAndGet();
                }
                return result;
            }
            
            @Override
            public void clear() {
                AjpAprProcessor next = poll();
                while ( next != null ) {
                    unregister(next);
                    next = poll();
                }
                super.clear();
                size.set(0);
            }
        };

        public AjpConnectionHandler(AjpAprProtocol proto) {
            this.proto = proto;
        }

        // FIXME: Support for this could be added in AJP as well
        @Override
        public SocketState event(SocketWrapper<Long> socket, SocketStatus status) {
            return SocketState.CLOSED;
        }
        
        @Override
        public SocketState process(SocketWrapper<Long> socket) {
            AjpAprProcessor processor = recycledProcessors.poll();
            try {
                if (processor == null) {
                    processor = createProcessor();
                }

                SocketState state = processor.process(socket);
                if (state == SocketState.LONG) {
                    // Check if the post processing is going to change the state
                    state = processor.asyncPostProcess();
                }
                if (state == SocketState.LONG || state == SocketState.ASYNC_END) {
                    // Need to make socket available for next processing cycle
                    // but no need for the poller
                    connections.put(socket, processor);
                } else {
                    if (state == SocketState.OPEN) {
                        connections.put(socket, processor);
                    }
                    recycledProcessors.offer(processor);
                }
                return state;

            } catch(java.net.SocketException e) {
                // SocketExceptions are normal
                log.debug(sm.getString(
                        "ajpprotocol.proto.socketexception.debug"), e);
            } catch (java.io.IOException e) {
                // IOExceptions are normal
                log.debug(sm.getString(
                        "ajpprotocol.proto.ioexception.debug"), e);
            }
            // Future developers: if you discover any other
            // rare-but-nonfatal exceptions, catch them here, and log as
            // above.
            catch (Throwable e) {
                ExceptionUtils.handleThrowable(e);
                // any other exception or error is odd. Here we log it
                // with "ERROR" level, so it will show up even on
                // less-than-verbose logs.
                log.error(sm.getString("ajpprotocol.proto.error"), e);
            }
            recycledProcessors.offer(processor);
            return SocketState.CLOSED;
        }

        @Override
        public SocketState asyncDispatch(SocketWrapper<Long> socket, SocketStatus status) {

            AjpAprProcessor result = connections.get(socket);
            
            SocketState state = SocketState.CLOSED; 
            if (result != null) {
                // Call the appropriate event
                try {
                    state = result.asyncDispatch(socket, status);
                }
                // Future developers: if you discover any other
                // rare-but-nonfatal exceptions, catch them here, and log as
                // debug.
                catch (Throwable e) {
                    ExceptionUtils.handleThrowable(e);
                    // any other exception or error is odd. Here we log it
                    // with "ERROR" level, so it will show up even on
                    // less-than-verbose logs.
                    AjpAprProtocol.log.error
                        (sm.getString("ajpprotocol.proto.error"), e);
                } finally {
                    if (state == SocketState.LONG && result.isAsync()) {
                        state = result.asyncPostProcess();
                    }
                    if (state != SocketState.LONG && state != SocketState.ASYNC_END) {
                        connections.remove(socket);
                        recycledProcessors.offer(result);
                        if (state == SocketState.OPEN) {
                            ((AprEndpoint)proto.endpoint).getPoller().add(socket.getSocket().longValue());
                        }
                    }
                }
            }
            return state;
        }
        
        protected AjpAprProcessor createProcessor() {
            AjpAprProcessor processor = new AjpAprProcessor(proto.packetSize, (AprEndpoint)proto.endpoint);
            processor.setAdapter(proto.adapter);
            processor.setTomcatAuthentication(proto.tomcatAuthentication);
            processor.setRequiredSecret(proto.requiredSecret);
            register(processor);
            return processor;
        }
        
        protected void register(AjpAprProcessor processor) {
            if (proto.getDomain() != null) {
                synchronized (this) {
                    try {
                        long count = registerCount.incrementAndGet();
                        RequestInfo rp = processor.getRequest().getRequestProcessor();
                        rp.setGlobalProcessor(global);
                        ObjectName rpName = new ObjectName
                            (proto.getDomain() + ":type=RequestProcessor,worker="
                                + proto.getName() + ",name=AjpRequest" + count);
                        if (log.isDebugEnabled()) {
                            log.debug("Register " + rpName);
                        }
                        Registry.getRegistry(null, null).registerComponent(rp, rpName, null);
                        rp.setRpName(rpName);
                    } catch (Exception e) {
                        log.warn("Error registering request");
                    }
                }
            }
        }

        protected void unregister(AjpAprProcessor processor) {
            if (proto.getDomain() != null) {
                synchronized (this) {
                    try {
                        RequestInfo rp = processor.getRequest().getRequestProcessor();
                        rp.setGlobalProcessor(null);
                        ObjectName rpName = rp.getRpName();
                        if (log.isDebugEnabled()) {
                            log.debug("Unregister " + rpName);
                        }
                        Registry.getRegistry(null, null).unregisterComponent(rpName);
                        rp.setRpName(null);
                    } catch (Exception e) {
                        log.warn("Error unregistering request", e);
                    }
                }
            }
        }

    }

}
