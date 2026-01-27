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

package io.microsphere.resilience4j.spring;


import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.spring.bulkhead.annotation.EnableBulkhead;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link LazyResilience4jFacade} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see LazyResilience4jFacade
 * @since 1.0.0
 */
@SpringJUnitConfig(LazyResilience4jFacadeTest.class)
@EnableBulkhead
class LazyResilience4jFacadeTest {

    private static final String TEST_ENTRY_NAME = "test";

    @Autowired
    private LazyResilience4jFacade lazyResilience4jFacade;

    @Test
    void testExecute() {
        LazyResilience4jFacade facade = this.lazyResilience4jFacade;
        facade.execute(TEST_ENTRY_NAME, () -> assertNotNull(facade));
        assertSame(facade, facade.execute(TEST_ENTRY_NAME, () -> facade));
    }

    @Test
    void testCall() throws Throwable {
        LazyResilience4jFacade facade = this.lazyResilience4jFacade;
        facade.call(TEST_ENTRY_NAME, () -> assertNotNull(facade));
        facade.call(TEST_ENTRY_NAME, () -> assertNotNull(facade), Exception.class);
        assertSame(facade, facade.call(TEST_ENTRY_NAME, () -> facade));
        assertSame(facade, facade.call(TEST_ENTRY_NAME, () -> facade, Exception.class));
    }

    @Test
    void testIsBeginSupported() {
        assertTrue(this.lazyResilience4jFacade.isBeginSupported());
    }

    @Test
    void testBegin() {
        Resilience4jContext<Resilience4jContext[]> context = this.lazyResilience4jFacade.begin(TEST_ENTRY_NAME);
        assertEquals(1, context.getEntry().length);
    }

    @Test
    void testIsEndSupported() {
        assertTrue(this.lazyResilience4jFacade.isEndSupported());
    }

    @Test
    void testEnd() {
        Resilience4jContext<Resilience4jContext[]> context = this.lazyResilience4jFacade.begin(TEST_ENTRY_NAME);
        this.lazyResilience4jFacade.end(context);
        assertEquals(1, context.getEntry().length);
    }

    @Test
    void testGetDelegate() {
        assertEquals(this.lazyResilience4jFacade.unwrap(Resilience4jFacade.class), this.lazyResilience4jFacade.getDelegate());
    }
}