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
package io.microsphere.resilience4j.circuitbreaker;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.common.AbstractResilience4jTemplateTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Random;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.FAILURE_RATE_EXCEEDED;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.NOT_PERMITTED;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.STATE_TRANSITION;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.SUCCESS;
import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link CircuitBreakerTemplate} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see CircuitBreakerTemplate
 * @since 1.0.0
 */
public class CircuitBreakerTemplateTest extends AbstractResilience4jTemplateTest<CircuitBreaker, CircuitBreakerConfig,
        CircuitBreakerRegistry, CircuitBreakerTemplate> {

    private static final Random random = new Random();

    private final Class<? extends Throwable> exceptionClass = RuntimeException.class;

    private final int rateThreshold = 20;

    private final Duration duration = ofMillis(50);

    private final int permitted = 2;

    /**
     * Create an instance of {@link CircuitBreakerConfig} for testing
     *
     * @return non-null
     */
    @Override
    protected CircuitBreakerConfig createEntryConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindow(1, 1, COUNT_BASED)
                .failureRateThreshold(rateThreshold)
                .maxWaitDurationInHalfOpenState(duration)
                .recordExceptions(exceptionClass)
                .slowCallDurationThreshold(duration)
                .permittedNumberOfCallsInHalfOpenState(permitted)
                .slowCallRateThreshold(rateThreshold)
                .writableStackTraceEnabled(false)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Test
    public void testExecute() throws Throwable {
        String entryName = this.entryName;
        CircuitBreakerTemplate template = this.template;

        template.onSuccessEvent(entryName, event -> {
            log(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(SUCCESS, event.getEventType());
        });
        String result = template.execute(getEntryNameGenerator(), () -> entryName);
        assertEquals(entryName, result);
    }

    @Test
    public void testExecuteOverFailureRate() {
        String entryName = this.entryName;
        CircuitBreakerTemplate template = this.template;

        template.onFailureRateExceededEvent(entryName, event -> {
            log(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(FAILURE_RATE_EXCEEDED, event.getEventType());
        });

        template.onStateTransitionEvent(entryName, event -> {
            log(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(STATE_TRANSITION, event.getEventType());
        });

        template.onCallNotPermittedEvent(entryName, event -> {
            log(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(NOT_PERMITTED, event.getEventType());
        });

        template.execute(getEntryNameGenerator(), () -> {
            executeRandomly(() -> {
                throw new RuntimeException("testing");
            }, rateThreshold);
        });

        template.execute(getEntryNameGenerator(), () -> {
        });

        // wait
        await(duration);

        for (int i = 0; i < permitted; i++) {
            template.execute(getEntryNameGenerator(), () -> {
            });
        }

        template.execute(getEntryNameGenerator(), () -> {
        });

    }

    private void executeRandomly(Runnable runnable, int rateThreshold) {
        if (random.nextInt(100) >= rateThreshold) {
            runnable.run();
        }
    }

    private void log(Object event) {
        logger.debug("the event of CircuitBreaker ({}) was received.", event);
    }
}
