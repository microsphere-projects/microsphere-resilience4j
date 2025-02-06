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

import org.junit.jupiter.api.Test;

import static io.microsphere.resilience4j.common.Resilience4jConstants.BULKHEAD_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.BULKHEAD_PREFIX;
import static io.microsphere.resilience4j.common.Resilience4jConstants.CIRCUIT_BREAKER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.CIRCUIT_BREAKER_PREFIX;
import static io.microsphere.resilience4j.common.Resilience4jConstants.PREFIX;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RATE_LIMITER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RATE_LIMITER_PREFIX;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RETRY_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RETRY_PREFIX;
import static io.microsphere.resilience4j.common.Resilience4jConstants.TIME_LIMITER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.TIME_LIMITER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Resilience4jConstants} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jConstants
 * @since 1.0.0
 */
public class Resilience4jConstantsTest {

    @Test
    public void testConstants() {

        assertEquals("microsphere.resilience4j.", PREFIX);

        // Retry
        assertEquals("retry", RETRY_MODULE_NAME);
        assertEquals("microsphere.resilience4j.retry.", RETRY_PREFIX);

        // Circuit Breaker
        assertEquals("circuit-breaker", CIRCUIT_BREAKER_MODULE_NAME);
        assertEquals("microsphere.resilience4j.circuit-breaker.", CIRCUIT_BREAKER_PREFIX);

        // RateLimiter
        assertEquals("rate-limiter", RATE_LIMITER_MODULE_NAME);
        assertEquals("microsphere.resilience4j.rate-limiter.", RATE_LIMITER_PREFIX);

        // TimeLimiter
        assertEquals("time-limiter", TIME_LIMITER_MODULE_NAME);
        assertEquals("microsphere.resilience4j.time-limiter.", TIME_LIMITER_PREFIX);

        // Bulkhead
        assertEquals("bulkhead", BULKHEAD_MODULE_NAME);
        assertEquals("microsphere.resilience4j.bulkhead.", BULKHEAD_PREFIX);

    }
}
