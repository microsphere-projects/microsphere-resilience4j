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

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link ChainableResilience4jFacade} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jFacade
 * @see ChainableResilience4jFacade
 * @since 1.0.0
 */
public class ChainableResilience4jFacadeTest {

    private final String entryName = "test-entry";

    private final int size = 5;

    private ChainableResilience4jFacade facade;

    @BeforeEach
    public void init() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        this.facade = new ChainableResilience4jFacade(
                bulkheadRegistry,
                bulkheadRegistry,
                rateLimiterRegistry,
                rateLimiterRegistry,
                timeLimiterRegistry,
                timeLimiterRegistry,
                retryRegistry,
                circuitBreakerRegistry,
                circuitBreakerRegistry
        );
    }

    @Test
    public void testExecute() {
        this.facade.execute(entryName, () -> {
        });
    }

    @Test
    public void testCall() throws Throwable {
        this.facade.call(entryName, () -> {
        });
    }

    @Test
    public void testBeginAndEnd() {
        Resilience4jContext<Resilience4jContext[]> context = this.facade.begin(entryName);
        Resilience4jContext[] subContexts = context.getEntry();
        assertEquals(this.size, subContexts.length);
        this.facade.end(context);
    }

    @Test
    public void testGetSize() {
        int size = this.facade.getSize();
        assertEquals(this.size, size);
    }

    @AfterEach
    public void destroy() {
        this.facade.destroy();
    }
}
