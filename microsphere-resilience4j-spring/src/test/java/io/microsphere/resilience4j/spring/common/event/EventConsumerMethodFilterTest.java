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

package io.microsphere.resilience4j.spring.common.event;


import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.junit.jupiter.api.Test;

import static io.microsphere.resilience4j.spring.common.event.EventConsumerMethodFilter.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.util.ReflectionUtils.doWithLocalMethods;

/**
 * {@link EventConsumerMethodFilter} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EventConsumerMethodFilter
 * @since 1.0.0
 */
class EventConsumerMethodFilterTest {

    @Test
    void testMatches() {
        assertMethods(EventPublisher.class, true);
        assertMethods(Retry.EventPublisher.class, true);
        assertMethods(CircuitBreaker.EventPublisher.class, true);
        assertMethods(RateLimiter.EventPublisher.class, true);
        assertMethods(TimeLimiter.EventPublisher.class, true);
        assertMethods(Bulkhead.EventPublisher.class, true);
        assertMethods(EventConsumerMethodFilterTest.class, false);
    }

    void assertMethods(Class<?> type, boolean matched) {
        doWithLocalMethods(type, method -> {
            assertEquals(matched, INSTANCE.matches(method));
        });
    }
}