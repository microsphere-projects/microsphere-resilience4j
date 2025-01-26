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
package io.microsphere.resilience4j.util;


import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.bulkhead.BulkheadRegistry.ofDefaults;
import static io.microsphere.resilience4j.util.Resilience4jUtils.getEntry;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link Resilience4jUtils} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jUtils
 * @since 1.0.0
 */
public class Resilience4jUtilsTest {

    @Test
    public void testGetEntry() {
        BulkheadRegistry bulkheadRegistry = ofDefaults();
        Bulkhead bulkhead = getEntry(bulkheadRegistry, "test");
        assertNotNull(bulkhead);
    }

    @Test
    public void testGetEventPublisher() {

    }
}
