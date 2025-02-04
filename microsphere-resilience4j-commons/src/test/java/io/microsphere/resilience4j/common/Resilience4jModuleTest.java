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
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.CommonProperties;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.junit.jupiter.api.Test;

import static io.microsphere.resilience4j.common.Resilience4jModule.BULKHEAD;
import static io.microsphere.resilience4j.common.Resilience4jModule.CIRCUIT_BREAKER;
import static io.microsphere.resilience4j.common.Resilience4jModule.RATE_LIMITER;
import static io.microsphere.resilience4j.common.Resilience4jModule.RETRY;
import static io.microsphere.resilience4j.common.Resilience4jModule.TIME_LIMITER;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Resilience4jModule} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jModule
 * @since 1.0.0
 */
public class Resilience4jModuleTest {

    @Test
    public void test() {
        assertModule(RETRY, Retry.class, RetryConfig.class, RetryConfigurationProperties.class, RetryEvent.class, RetryRegistry.class, 0);
        assertModule(CIRCUIT_BREAKER, CircuitBreaker.class, CircuitBreakerConfig.class, CircuitBreakerConfigurationProperties.class, CircuitBreakerEvent.class, CircuitBreakerRegistry.class, 1);
        assertModule(RATE_LIMITER, RateLimiter.class, RateLimiterConfig.class, RateLimiterConfigurationProperties.class, RateLimiterEvent.class, RateLimiterRegistry.class, 2);
        assertModule(TIME_LIMITER, TimeLimiter.class, TimeLimiterConfig.class, TimeLimiterConfigurationProperties.class, TimeLimiterEvent.class, TimeLimiterRegistry.class, 3);
        assertModule(BULKHEAD, Bulkhead.class, BulkheadConfig.class, BulkheadConfigurationProperties.class, BulkheadEvent.class, BulkheadRegistry.class, 4);
    }

    private void assertModule(Resilience4jModule module, Class<?> entryClass, Class<?> configClass,
                              Class<?> configurationPropertiesClass, Class<?> eventClass,
                              Class<? extends Registry> registryClass, int defaultAspectOrder) {
        assertEquals(entryClass, module.getEntryClass());
        assertEquals(configClass, module.getConfigClass());
        assertEquals(configurationPropertiesClass, module.getConfigurationPropertiesClass());
        assertEquals(eventClass, module.getEventClass());
        assertEquals(registryClass, module.getRegistryClass());
        assertEquals(defaultAspectOrder, module.getDefaultAspectOrder());
    }
}
