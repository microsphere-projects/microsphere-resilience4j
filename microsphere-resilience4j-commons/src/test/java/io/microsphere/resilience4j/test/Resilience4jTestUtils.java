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

package io.microsphere.resilience4j.test;

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.annotation.Nonnull;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;

/**
 * The utilities class of Resilience4j Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Resilience4jFacade
 * @since 1.0.0
 */
public class Resilience4jTestUtils {

    /**
     * Create {@link ChainableResilience4jFacade} instance.
     *
     * @return non-null
     * @see ChainableResilience4jFacade
     */
    @Nonnull
    public static ChainableResilience4jFacade createChainableResilience4jFacade() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        return new ChainableResilience4jFacade(
                bulkheadRegistry,
                bulkheadRegistry,
                rateLimiterRegistry,
                rateLimiterRegistry,
                timeLimiterRegistry,
                timeLimiterRegistry,
                retryRegistry,
                circuitBreakerRegistry,
                circuitBreakerRegistry);
    }
}
