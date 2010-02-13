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
package org.apache.el.lang;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.el.ELException;

import junit.framework.TestCase;

public class TestELSupport extends TestCase {
    public void testBigDecimal() {
        testIsSame(new BigDecimal(
                "0.123456789012345678901234567890123456789012345678901234567890123456789"));
    }

    public void testBigInteger() {
        testIsSame(new BigInteger(
                "1234567890123456789012345678901234567890123456789012345678901234567890"));
    }

    public void testLong() {
        testIsSame(Long.valueOf(0x0102030405060708L));
    }

    public void testInteger() {
        testIsSame(Integer.valueOf(0x01020304));
    }

    public void testShort() {
        testIsSame(Short.valueOf((short) 0x0102));
    }

    public void testByte() {
        testIsSame(Byte.valueOf((byte) 0xEF));
    }

    public void testDouble() {
        testIsSame(Double.valueOf(0.123456789012345678901234));
    }

    public void testFloat() {
        testIsSame(Float.valueOf(0.123456F));
    }

    public void testCoerceIntegerToNumber() {
        Integer input = Integer.valueOf(4390241);
        Object output = ELSupport.coerceToType(input, Number.class);
        assertEquals(input, output);
    }

    public void testCoerceNullToNumber() {
        Object output = ELSupport.coerceToType(null, Number.class);
        assertEquals(Long.valueOf(0), output);
    }
    
    public void testCoerceEnumAToEnumA() {
        Object output = null;
        try {
            output = ELSupport.coerceToEnum(TestEnumA.VALA1, TestEnumA.class);
        } finally {
            assertEquals(TestEnumA.VALA1, output);
        }
    }
    
    public void testCoerceEnumAToEnumB() {
        Object output = null;
        try {
            output = ELSupport.coerceToEnum(TestEnumA.VALA1, TestEnumB.class);
        } catch (ELException ele) {
            // Ignore
        }
        assertNull(output);
    }

    public void testCoerceEnumAToEnumC() {
        Object output = null;
        try {
            output = ELSupport.coerceToEnum(TestEnumA.VALA1, TestEnumC.class);
        } catch (ELException ele) {
            // Ignore
        }
        assertNull(output);
    }

    private static void testIsSame(Object value) {
        assertEquals(value, ELSupport.coerceToNumber(value, value.getClass()));
    }
    
    private static enum TestEnumA {
        VALA1,
        VALA2
    }
    private static enum TestEnumB {
        VALB1,
        VALB2
    }
    private static enum TestEnumC {
        VALA1,
        VALA2,
        VALB1,
        VALB2
    }
}
