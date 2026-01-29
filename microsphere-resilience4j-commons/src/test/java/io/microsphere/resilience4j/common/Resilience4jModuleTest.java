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
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.CommonTimeLimiterConfigurationProperties;
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

import static io.microsphere.resilience4j.common.Resilience4jConstants.BULKHEAD_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.CIRCUIT_BREAKER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RATE_LIMITER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RETRY_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.THREAD_POOL_BULKHEAD_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.TIME_LIMITER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jModule.BULKHEAD;
import static io.microsphere.resilience4j.common.Resilience4jModule.CIRCUIT_BREAKER;
import static io.microsphere.resilience4j.common.Resilience4jModule.RATE_LIMITER;
import static io.microsphere.resilience4j.common.Resilience4jModule.RETRY;
import static io.microsphere.resilience4j.common.Resilience4jModule.THREAD_POOL_BULKHEAD;
import static io.microsphere.resilience4j.common.Resilience4jModule.TIME_LIMITER;
import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static io.microsphere.resilience4j.common.Resilience4jModule.values;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link Resilience4jModule} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jModule
 * @since 1.0.0
 */
class Resilience4jModuleTest {

    @Test
    void test() {
        assertModule(RETRY, RETRY_MODULE_NAME, Retry.class, RetryConfig.class, CommonRetryConfigurationProperties.class, RetryEvent.class, RetryRegistry.class, 0);
        assertModule(CIRCUIT_BREAKER, CIRCUIT_BREAKER_MODULE_NAME, CircuitBreaker.class, CircuitBreakerConfig.class, CommonCircuitBreakerConfigurationProperties.class, CircuitBreakerEvent.class, CircuitBreakerRegistry.class, 1);
        assertModule(RATE_LIMITER, RATE_LIMITER_MODULE_NAME, RateLimiter.class, RateLimiterConfig.class, CommonRateLimiterConfigurationProperties.class, RateLimiterEvent.class, RateLimiterRegistry.class, 2);
        assertModule(TIME_LIMITER, TIME_LIMITER_MODULE_NAME, TimeLimiter.class, TimeLimiterConfig.class, CommonTimeLimiterConfigurationProperties.class, TimeLimiterEvent.class, TimeLimiterRegistry.class, 3);
        assertModule(BULKHEAD, BULKHEAD_MODULE_NAME, Bulkhead.class, BulkheadConfig.class, CommonBulkheadConfigurationProperties.class, BulkheadEvent.class, BulkheadRegistry.class, 4);
        assertModule(THREAD_POOL_BULKHEAD, THREAD_POOL_BULKHEAD_MODULE_NAME, ThreadPoolBulkhead.class, ThreadPoolBulkheadConfig.class, CommonThreadPoolBulkheadConfigurationProperties.class, BulkheadEvent.class, ThreadPoolBulkheadRegistry.class, 4);
    }

    @Test
    void testValueOfOnIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> valueOf(Resilience4jModule.class));
    }

    @Test
    void testToString() {
        for (Resilience4jModule module : values()) {
            assertNotNull(module.toString());
        }
    }

    private void assertModule(Resilience4jModule module, String moduleName, Class<?> entryClass, Class<?> configClass,
                              Class<?> configurationPropertiesClass, Class<?> eventClass,
                              Class<? extends Registry> registryClass, int defaultAspectOrder) {
        assertEquals(moduleName, module.getName());
        assertEquals(entryClass, module.getEntryClass());
        assertEquals(configClass, module.getConfigClass());
        assertEquals(configurationPropertiesClass, module.getConfigurationPropertiesClass());
        assertEquals(eventClass, module.getEventClass());
        assertEquals(registryClass, module.getRegistryClass());
        assertEquals(defaultAspectOrder, module.getDefaultAspectOrder());

        assertSame(module, valueOf(entryClass));
        assertSame(module, valueOf(configClass));
        assertSame(module, valueOf(configurationPropertiesClass));
        assertSame(module.getEventClass(), eventClass);
        assertSame(module, valueOf(registryClass));
    }
}