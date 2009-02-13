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
package org.apache.tomcat.jdbc.pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.tomcat.jdbc.pool.PoolProperties.InterceptorProperty;

/**
 * Abstract class that is to be extended for implementations of interceptors.
 * 
 * @author Filip Hanik
 * @version 1.0
 */
public abstract class JdbcInterceptor implements InvocationHandler {
    /**
     * java.sql.Connection.close()
     */
    public static final String CLOSE_VAL = "close";
    /**
     * java.sql.Connection.toString()
     */
    public static final String TOSTRING_VAL = "toString";
    /**
     * java.sql.Connection.isClosed()
     */
    public static final String ISCLOSED_VAL = "isClosed";
    /**
     * javax.sql.DataSource.getConnection()
     */
    public static final String GETCONNECTION_VAL = "getConnection";
    
    /**
     * Properties for this interceptor
     */
    protected Map<String,InterceptorProperty> properties = null; 
    
    /**
     * The next interceptor in the chain
     */
    private JdbcInterceptor next = null;
    /**
     * Property that decides how we do string comparison, default is reference (==)
     */
    private boolean useEquals = false;

    /**
     * Public constructor for instantation through reflection
     */
    public JdbcInterceptor() {
    }

    /**
     * Gets invoked each time an operation on java.sql.Connection is invoked.
     * {@inheritDoc}
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (getNext()!=null) return getNext().invoke(this,method,args);
        else throw new NullPointerException();
    }

    /**
     * Returns the next interceptor in the chain
     * @return
     */
    public JdbcInterceptor getNext() {
        return next;
    }

    /**
     * configures the next interceptor in the chain
     * @param next
     */
    public void setNext(JdbcInterceptor next) {
        this.next = next;
    }
    
    /**
     * Performs a string comparison, using references unless the useEquals property is set to true.
     * @param name1
     * @param name2
     * @return
     */
    public boolean compare(String name1, String name2) {
        if (isUseEquals()) {
            return name1.equals(name2);
        } else {
            return name1==name2;
        }
    }
    
    /**
     * Compares a method name (String) to a method (Method)
     * {@link compare(String,String)}
     * Uses reference comparison unless the useEquals property is set to true
     * @param methodName
     * @param method
     * @return true if the name matches
     */
    public boolean compare(String methodName, Method method) {
        return compare(methodName, method.getName());
    }
    
    /**
     * Gets called each time the connection is borrowed from the pool
     * This means that if an interceptor holds a reference to the connection
     * the interceptor can be reused for another connection.
     * @param parent - the connection pool owning the connection
     * @param con - the pooled connection
     */
    public abstract void reset(ConnectionPool parent, PooledConnection con);
    
    /**
     * 
     * @return the configured properties for this interceptor
     */
    public Map<String,InterceptorProperty> getProperties() {
        return properties;
    }

    /**
     * Called during the creation of an interceptor
     * The properties can be set during the configuration of an interceptor
     * @param properties
     */
    public void setProperties(Map<String,InterceptorProperty> properties) {
        this.properties = properties;
        final String useEquals = "useEquals";
        InterceptorProperty p = properties.get(useEquals);
        if (p!=null) {
            setUseEquals(Boolean.parseBoolean(p.getValue()));
        }
    }
    
    /**
     * @return true if the compare method uses the Object.equals(Object) method
     *         false if comparison is done on a reference level
     */
    public boolean isUseEquals() {
        return useEquals;
    }
    
    /**
     * Set to true if string comparisons (for the {@link compare} method) should use the Object.equals(Object) method
     * The default is false
     * @param useEquals
     */
    public void setUseEquals(boolean useEquals) {
        this.useEquals = useEquals;
    }
    
    /**
     * This method is invoked by a connection pool when the pool is closed.
     * Interceptor classes can override this method if they keep static
     * variables or other tracking means around.
     * <b>This method is only invoked on a single instance of the interceptor, and not on every instance created.</b>
     * @param pool - the pool that is being closed.
     */
    public void poolClosed(ConnectionPool pool) {
    }

    /**
     * This method is invoked by a connection pool when the pool is first started up, usually when the first connection is requested.
     * Interceptor classes can override this method if they keep static
     * variables or other tracking means around.
     * <b>This method is only invoked on a single instance of the interceptor, and not on every instance created.</b>
     * @param pool - the pool that is being closed.
     */
    public void poolStarted(ConnectionPool pool) {
    }

}
