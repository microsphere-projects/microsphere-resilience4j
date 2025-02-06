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


import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;

/**
 * The constants for Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface Resilience4jConstants {

    /**
     * The prefix of Resilience4j
     */
    String PREFIX = "microsphere.resilience4j.";

    /**
     * The module name of {@link Retry}
     */
    String RETRY_MODULE_NAME = "retry";

    /**
     * The prefix of {@link Retry}
     */
    String RETRY_PREFIX = PREFIX + RETRY_MODULE_NAME + ".";

    /**
     * The module name of {@link CircuitBreaker}
     */
    String CIRCUIT_BREAKER_MODULE_NAME = "circuit-breaker";

    /**
     * The prefix of {@link CircuitBreaker}
     */
    String CIRCUIT_BREAKER_PREFIX = PREFIX + CIRCUIT_BREAKER_MODULE_NAME + ".";

    /**
     * The module name of {@link RateLimiter}
     */
    String RATE_LIMITER_MODULE_NAME = "rate-limiter";

    /**
     * The prefix of {@link RateLimiter}
     */
    String RATE_LIMITER_PREFIX = PREFIX + RATE_LIMITER_MODULE_NAME + ".";

    /**
     * The module name of {@link TimeLimiter}
     */
    String TIME_LIMITER_MODULE_NAME = "time-limiter";

    /**
     * The prefix of {@link TimeLimiter}
     */
    String TIME_LIMITER_PREFIX = PREFIX + TIME_LIMITER_MODULE_NAME + ".";

    /**
     * The module name of {@link Bulkhead}
     */
    String BULKHEAD_MODULE_NAME = "bulkhead";

    /**
     * The prefix of {@link Bulkhead}
     */
    String BULKHEAD_PREFIX = PREFIX + BULKHEAD_MODULE_NAME + ".";
}
