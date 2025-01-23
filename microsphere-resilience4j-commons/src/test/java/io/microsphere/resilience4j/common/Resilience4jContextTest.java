/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.resilience4j.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Resilience4jContext} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jContext
 * @since 1.0.0
 */
public class Resilience4jContextTest {

    private Resilience4jContext context;

    @BeforeEach
    public void init() {
        context = new Resilience4jContext();
    }

    @Test
    public void testAttributes() {
        assertNotNull(context.getAttributes());
        assertTrue(context.getAttributes().isEmpty());
    }

    @Test
    public void testAttribute() {
        String name = "test-name";
        String value = "test-value";
        String defaultValue = "test-value-2";

        assertSame(context, context.setAttribute(name, value));
        assertTrue(context.hasAttribute(name));
        assertSame(value, context.getAttribute(name));
        assertSame(value, context.removeAttribute(name));
        assertFalse(context.hasAttribute(name));
        assertSame(defaultValue, context.getAttribute(name, defaultValue));

        assertSame(context, context.setAttribute(name, defaultValue));
        assertSame(context, context.removeAttributes());
        assertFalse(context.hasAttribute(name));
    }
}
