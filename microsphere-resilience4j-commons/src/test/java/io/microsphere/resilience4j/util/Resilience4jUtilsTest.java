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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.microsphere.resilience4j.ratelimiter.RateLimiterTemplate;
import io.microsphere.resilience4j.retry.RetryTemplate;
import io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.github.resilience4j.bulkhead.BulkheadRegistry.ofDefaults;
import static io.microsphere.resilience4j.common.Resilience4jModule.BULKHEAD;
import static io.microsphere.resilience4j.common.Resilience4jModule.CIRCUIT_BREAKER;
import static io.microsphere.resilience4j.common.Resilience4jModule.RATE_LIMITER;
import static io.microsphere.resilience4j.common.Resilience4jModule.RETRY;
import static io.microsphere.resilience4j.common.Resilience4jModule.TIME_LIMITER;
import static io.microsphere.resilience4j.util.Resilience4jUtils.getEntry;
import static io.microsphere.resilience4j.util.Resilience4jUtils.getEventProcessor;
import static io.microsphere.resilience4j.util.Resilience4jUtils.loadDefaultTemplates;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.copyOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link Resilience4jUtils} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jUtils
 * @since 1.0.0
 */
public class Resilience4jUtilsTest {

    @Test
    public void testLoadDefaultTemplates() {
        Map<Resilience4jModule, Class<? extends Resilience4jTemplate>> defaultTemplates = loadDefaultTemplates();
        assertEquals(allOf(Resilience4jModule.class), copyOf(defaultTemplates.keySet()));
        assertEquals(defaultTemplates.get(RETRY), RetryTemplate.class);
        assertEquals(defaultTemplates.get(CIRCUIT_BREAKER), CircuitBreakerTemplate.class);
        assertEquals(defaultTemplates.get(RATE_LIMITER), RateLimiterTemplate.class);
        assertEquals(defaultTemplates.get(TIME_LIMITER), TimeLimiterTemplate.class);
        assertEquals(defaultTemplates.get(BULKHEAD), BulkheadTemplate.class);
    }

    @Test
    public void testGetEntry() {
        BulkheadRegistry bulkheadRegistry = ofDefaults();
        Bulkhead bulkhead = getEntry(bulkheadRegistry, "test");
        assertNotNull(bulkhead);
    }

    @Test
    public void testGetEventPublisher() {
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        assertEquals(retryRegistry.getEventPublisher(), getEventProcessor(retryRegistry));

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        assertEquals(timeLimiterRegistry.getEventPublisher(), getEventProcessor(timeLimiterRegistry));

        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        assertEquals(rateLimiterRegistry.getEventPublisher(), getEventProcessor(rateLimiterRegistry));

        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        assertEquals(bulkheadRegistry.getEventPublisher(), getEventProcessor(bulkheadRegistry));

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        assertEquals(circuitBreakerRegistry.getEventPublisher(), getEventProcessor(circuitBreakerRegistry));

        assertThrows(UnsupportedOperationException.class, () -> getEventProcessor(new Object()));
        assertThrows(UnsupportedOperationException.class, () -> getEventProcessor((EventPublisher) onEventConsumer -> {
        }));

        String name = "test";


        Retry retry = retryRegistry.retry(name);
        Object entry = retry;
        assertEquals(retry.getEventPublisher(), getEventProcessor(retry));
        assertEquals(retry.getEventPublisher(), getEventProcessor(entry));

        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(name);
        entry = timeLimiter;
        assertEquals(timeLimiter.getEventPublisher(), getEventProcessor(timeLimiter));
        assertEquals(timeLimiter.getEventPublisher(), getEventProcessor(entry));

        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(name);
        entry = rateLimiter;
        assertEquals(rateLimiter.getEventPublisher(), getEventProcessor(rateLimiter));
        assertEquals(rateLimiter.getEventPublisher(), getEventProcessor(entry));

        Bulkhead bulkhead = bulkheadRegistry.bulkhead(name);
        entry = bulkhead;
        assertEquals(bulkhead.getEventPublisher(), getEventProcessor(bulkhead));
        assertEquals(bulkhead.getEventPublisher(), getEventProcessor(entry));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        entry = circuitBreaker;
        assertEquals(circuitBreaker.getEventPublisher(), getEventProcessor(circuitBreaker));
        assertEquals(circuitBreaker.getEventPublisher(), getEventProcessor(entry));
    }
}
