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


import io.github.resilience4j.bulkhead.internal.InMemoryBulkheadRegistry;
import io.github.resilience4j.retry.internal.InMemoryRetryRegistry;
import io.microsphere.lang.function.ThrowableSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.microsphere.collection.Lists.ofList;
import static io.microsphere.resilience4j.util.Resilience4jUtils.createTemplates;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CallbackChain} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see CallbackChain
 * @since 1.0.0
 */
class CallbackChainTest {

    private static final String TEST_NAME = "test";

    private static final ThrowableSupplier<String> TEST_SUPPILER = () -> "Hello,World";

    private CallbackChain<String> emptyCallbackChain;
    private CallbackChain<String> callbackChain;

    @BeforeEach
    void setUp() {
        this.emptyCallbackChain = new CallbackChain<>(TEST_NAME, TEST_SUPPILER, emptyList());
        this.callbackChain = new CallbackChain<>(TEST_NAME, TEST_SUPPILER, createTemplates(ofList(new InMemoryBulkheadRegistry(), new InMemoryRetryRegistry())));
    }

    @Test
    void testGet() throws Throwable {
        assertEquals(TEST_SUPPILER.get(), this.emptyCallbackChain.get());
        assertEquals(TEST_SUPPILER.get(), this.callbackChain.get());
    }
}