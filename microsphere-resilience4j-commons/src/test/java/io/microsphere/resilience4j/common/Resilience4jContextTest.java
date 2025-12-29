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

import static io.microsphere.collection.MapUtils.ofMap;
import static java.lang.System.nanoTime;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private String entryName = "test-entry";

    private String entry = "test";

    private Resilience4jContext<String> context;

    @BeforeEach
    void setUp() {
        this.context = new Resilience4jContext<>(entryName, entry);
    }

    @Test
    void testProperties() {
        assertEquals(entryName, this.context.getEntryName());
        assertEquals(entry, this.context.getEntry());

        Long startTime = nanoTime();
        assertSame(startTime, this.context.setStartTime(startTime).getStartTime());

        assertSame(entryName, this.context.setResult(entryName).getResult());

        Exception exception = new Exception();
        assertSame(exception, this.context.setFailure(exception).getFailure());
    }

    @Test
    void testAttributes() {
        assertNotNull(this.context.getAttributes());
        assertNotNull(this.context.getAttributes());
        assertTrue(this.context.getAttributes().isEmpty());
    }

    @Test
    void testAttribute() {
        String name = "test-name";
        String value = "test-value";
        String defaultValue = "test-value-2";

        assertSame(this.context, this.context.setAttribute(name, value));
        assertTrue(this.context.hasAttribute(name));
        assertSame(value, this.context.getAttribute(name));
        assertSame(value, this.context.removeAttribute(name));
        assertFalse(this.context.hasAttribute(name));
        assertSame(defaultValue, this.context.getAttribute(name, defaultValue));

        assertSame(this.context, this.context.setAttribute(name, defaultValue));
        assertSame(this.context, this.context.removeAttributes());
        assertFalse(this.context.hasAttribute(name));
    }

    @Test
    void testRemoveAttributes() {
        assertSame(this.context, this.context.removeAttributes());
        assertSame(this.context, this.context.setAttribute("test", "value"));
        assertSame(this.context, this.context.removeAttributes());
    }

    @Test
    void testGetAttributes() {
        assertSame(emptyMap(), this.context.getAttributes());
        assertSame(this.context, this.context.setAttribute("test", "value"));
        assertEquals(ofMap("test", "value"), this.context.getAttributes());
    }

    @Test
    void testToString() {
        assertNotNull(this.context.toString());
    }

}
