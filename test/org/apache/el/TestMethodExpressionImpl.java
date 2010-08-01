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

package org.apache.el;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;

import org.apache.jasper.el.ELContextImpl;

import junit.framework.TestCase;

public class TestMethodExpressionImpl extends TestCase {

    private ExpressionFactory factory;
    ELContext context;
    
    @Override
    public void setUp() {
        factory = ExpressionFactory.newInstance();
        context = new ELContextImpl();

        TesterBeanA beanA = new TesterBeanA();
        beanA.setName("A");
        context.getVariableMapper().setVariable("beanA",
                factory.createValueExpression(beanA, TesterBeanA.class));
        
        TesterBeanAA beanAA = new TesterBeanAA();
        beanAA.setName("AA");
        context.getVariableMapper().setVariable("beanAA",
                factory.createValueExpression(beanAA, TesterBeanAA.class));
        
        TesterBeanAAA beanAAA = new TesterBeanAAA();
        beanAAA.setName("AAA");
        context.getVariableMapper().setVariable("beanAAA",
                factory.createValueExpression(beanAAA, TesterBeanAAA.class));
        
        TesterBeanB beanB = new TesterBeanB();        
        beanB.setName("B");
        context.getVariableMapper().setVariable("beanB",
                factory.createValueExpression(beanB, TesterBeanB.class));
        
        TesterBeanBB beanBB = new TesterBeanBB();        
        beanBB.setName("BB");
        context.getVariableMapper().setVariable("beanBB",
                factory.createValueExpression(beanBB, TesterBeanBB.class));
        
        TesterBeanBBB beanBBB = new TesterBeanBBB();        
        beanBBB.setName("BBB");
        context.getVariableMapper().setVariable("beanBBB",
                factory.createValueExpression(beanBBB, TesterBeanBBB.class));
        
        TesterBeanC beanC = new TesterBeanC();
        context.getVariableMapper().setVariable("beanC",
                factory.createValueExpression(beanC, TesterBeanC.class));
    }
    
    public void testIsParametersProvided() {
        TesterBeanB beanB = new TesterBeanB();
        beanB.setName("Tomcat");
        ValueExpression var =
            factory.createValueExpression(beanB, TesterBeanB.class);
        context.getVariableMapper().setVariable("beanB", var);

        MethodExpression me1 = factory.createMethodExpression(
                context, "${beanB.getName}", String.class, new Class<?>[] {});
        MethodExpression me2 = factory.createMethodExpression(
                context, "${beanB.sayHello('JUnit')}", String.class,
                new Class<?>[] { String.class });

        assertFalse(me1.isParmetersProvided());
        assertTrue(me2.isParmetersProvided());
    }

    public void testInvoke() {
        TesterBeanB beanB = new TesterBeanB();
        beanB.setName("B");

        context.getVariableMapper().setVariable("beanB",
                factory.createValueExpression(beanB, TesterBeanB.class));

        MethodExpression me1 = factory.createMethodExpression(
                context, "${beanB.getName}", String.class, new Class<?>[] {});
        MethodExpression me2 = factory.createMethodExpression(
                context, "${beanB.sayHello('JUnit')}", String.class,
                new Class<?>[] { String.class });
        MethodExpression me3 = factory.createMethodExpression(
                context, "${beanB.sayHello}", String.class,
                new Class<?>[] { String.class });

        assertEquals("B", me1.invoke(context, null));
        assertEquals("Hello JUnit from B", me2.invoke(context, null));
        assertEquals("Hello JUnit from B",
                me2.invoke(context, new Object[] { "JUnit2" }));
        assertEquals("Hello JUnit2 from B",
                me3.invoke(context, new Object[] { "JUnit2" }));
        assertEquals("Hello JUnit from B",
                me2.invoke(context, new Object[] { null }));
        assertEquals("Hello null from B",
                me3.invoke(context, new Object[] { null }));
    }

    public void testInvokeWithSuper() {
        MethodExpression me = factory.createMethodExpression(context,
                "${beanA.setBean(beanBB)}", null ,
                new Class<?>[] { TesterBeanB.class });
        me.invoke(context, null);
        ValueExpression ve = factory.createValueExpression(context,
                "${beanA.bean.name}", String.class);
        Object r = ve.getValue(context);
        assertEquals("BB", r);
    }
    
    public void testInvokeWithSuperABNoReturnTypeNoParamTypes() {
        MethodExpression me2 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanB)}", null , null);
        Object r2 = me2.invoke(context, null);
        assertEquals("AB: Hello A from B", r2.toString());
    }
    
    public void testInvokeWithSuperABReturnTypeNoParamTypes() {
        MethodExpression me3 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanB)}", String.class , null);
        Object r3 = me3.invoke(context, null);
        assertEquals("AB: Hello A from B", r3.toString());
    }
    
    public void testInvokeWithSuperABNoReturnTypeParamTypes() {
        MethodExpression me4 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanB)}", null ,
                new Class<?>[] {TesterBeanA.class, TesterBeanB.class});
        Object r4 = me4.invoke(context, null);
        assertEquals("AB: Hello A from B", r4.toString());
    }
    
    public void testInvokeWithSuperABReturnTypeParamTypes() {
        MethodExpression me5 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanB)}", String.class ,
                new Class<?>[] {TesterBeanA.class, TesterBeanB.class});
        Object r5 = me5.invoke(context, null);
        assertEquals("AB: Hello A from B", r5.toString());
    }
    
    public void testInvokeWithSuperABB() {
        MethodExpression me6 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanBB)}", null , null);
        Object r6 = me6.invoke(context, null);
        assertEquals("ABB: Hello A from BB", r6.toString());
    }
    
    public void testInvokeWithSuperABBB() {
        MethodExpression me7 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanBBB)}", null , null);
        Object r7 = me7.invoke(context, null);
        assertEquals("ABB: Hello A from BBB", r7.toString());
    }
    
    public void testInvokeWithSuperAAB() {
        MethodExpression me8 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAA,beanB)}", null , null);
        Object r8 = me8.invoke(context, null);
        assertEquals("AAB: Hello AA from B", r8.toString());
    }
    
    public void testInvokeWithSuperAABB() {
        MethodExpression me9 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAA,beanBB)}", null , null);
        Exception e = null;
        try {
            me9.invoke(context, null);
        } catch (Exception e1) {
            e = e1;
        }
        // Expected to fail
        assertNotNull(e);
    }
    
    public void testInvokeWithSuperAABBB() {
        // The Java compiler reports this as ambiguous. Using the parameter that
        // matches exactly seems reasonable to limit the scope of the method
        // search so the EL will find a match.
        MethodExpression me10 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAA,beanBBB)}", null , null);
        Object r10 = me10.invoke(context, null);
        assertEquals("AAB: Hello AA from BBB", r10.toString());
    }
    
    public void testInvokeWithSuperAAAB() {
        MethodExpression me11 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAAA,beanB)}", null , null);
        Object r11 = me11.invoke(context, null);
        assertEquals("AAB: Hello AAA from B", r11.toString());
    }
    
    public void testInvokeWithSuperAAABB() {
        // The Java compiler reports this as ambiguous. Using the parameter that
        // matches exactly seems reasonable to limit the scope of the method
        // search so the EL will find a match.
        MethodExpression me12 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAAA,beanBB)}", null , null);
        Object r12 = me12.invoke(context, null);
        assertEquals("ABB: Hello AAA from BB", r12.toString());
    }
    
    public void testInvokeWithSuperAAABBB() {
        MethodExpression me13 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAAA,beanBBB)}", null , null);
        Exception e = null;
        try {
            me13.invoke(context, null);
        } catch (Exception e1) {
            e = e1;
        }
        // Expected to fail
        assertNotNull(e);
    }
    
    public void testInvokeWithVarArgsAB() throws Exception {
        MethodExpression me1 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanB,beanB)}", null , null);
        Exception e = null;
        try {
            me1.invoke(context, null);
        } catch (Exception e1) {
            e = e1;
        }
        // Expected to fail
        assertNotNull(e);
    }
    
    public void testInvokeWithVarArgsABB() throws Exception {
        MethodExpression me2 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanBB,beanBB)}", null , null);
        Object r2 = me2.invoke(context, null);
        assertEquals("ABB[]: Hello A from BB, BB", r2.toString());
    }
    
    public void testInvokeWithVarArgsABBB() throws Exception {
        MethodExpression me3 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanA,beanBBB,beanBBB)}", null , null);
        Object r3 = me3.invoke(context, null);
        assertEquals("ABB[]: Hello A from BBB, BBB", r3.toString());
    }
    
    public void testInvokeWithVarArgsAAB() throws Exception {
        MethodExpression me4 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAA,beanB,beanB)}", null , null);
        Exception e = null;
        try {
            me4.invoke(context, null);
        } catch (Exception e1) {
            e = e1;
        }
        // Expected to fail
        assertNotNull(e);
    }
    
    public void testInvokeWithVarArgsAABB() throws Exception {
        MethodExpression me5 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAA,beanBB,beanBB)}", null , null);
        Object r5 = me5.invoke(context, null);
        assertEquals("ABB[]: Hello AA from BB, BB", r5.toString());
    }
    
    public void testInvokeWithVarArgsAABBB() throws Exception {
        MethodExpression me6 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAA,beanBBB,beanBBB)}", null , null);
        Object r6 = me6.invoke(context, null);
        assertEquals("ABB[]: Hello AA from BBB, BBB", r6.toString());
    }
    
    public void testInvokeWithVarArgsAAAB() throws Exception {
        MethodExpression me7 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAAA,beanB,beanB)}", null , null);
        Exception e = null;
        try {
            me7.invoke(context, null);
        } catch (Exception e1) {
            e = e1;
        }
        // Expected to fail
        assertNotNull(e);
    }
    
    public void testInvokeWithVarArgsAAABB() throws Exception {
        MethodExpression me8 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAAA,beanBB,beanBB)}", null , null);
        Object r8 = me8.invoke(context, null);
        assertEquals("ABB[]: Hello AAA from BB, BB", r8.toString());
    }
    
    public void testInvokeWithVarArgsAAABBB() throws Exception {
        MethodExpression me9 = factory.createMethodExpression(context,
                "${beanC.sayHello(beanAAA,beanBBB,beanBBB)}", null , null);
        Object r9 = me9.invoke(context, null);
        assertEquals("ABB[]: Hello AAA from BBB, BBB", r9.toString());
    }
    
    /*
     * This is also tested implicitly in numerous places elsewhere in this
     * class.
     */
    public void testBug49655() throws Exception {
        // This is the call the failed
        MethodExpression me = factory.createMethodExpression(context,
                "#{beanA.setName('New value')}", null, null);
        // The rest is to check it worked correctly
        me.invoke(context, null);
        ValueExpression ve = factory.createValueExpression(context,
                "#{beanA.name}", java.lang.String.class);
        assertEquals("New value", ve.getValue(context));
    }
}
