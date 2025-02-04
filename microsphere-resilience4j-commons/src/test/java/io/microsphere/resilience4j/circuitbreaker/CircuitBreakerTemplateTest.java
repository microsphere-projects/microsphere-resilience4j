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
import java.util.function.Supplier;

import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.ERROR;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.IGNORED_ERROR;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.NOT_PERMITTED;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.RESET;
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
public class CircuitBreakerTemplateTest extends AbstractResilience4jTemplateTest<CircuitBreaker, CircuitBreakerConfig, CircuitBreakerRegistry, CircuitBreakerTemplate> {

    private final int rateThreshold = 10;

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
                .failureRateThreshold(rateThreshold)
                .ignoreExceptions(RuntimeException.class)
                .recordExceptions(Exception.class)
                .slowCallDurationThreshold(duration)
                .permittedNumberOfCallsInHalfOpenState(permitted)
                .slowCallRateThreshold(rateThreshold)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    @Test
    public void testExecute() throws Throwable {
        String entryName = this.entryName;
        CircuitBreakerTemplate template = this.template;

        template.onSuccessEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(SUCCESS, event.getEventType());
        });
        String result = template.execute(entryName, () -> entryName);
        assertEquals(entryName, result);
    }

    @Test
    public void testExecuteOnCallFailure() {
        String entryName = this.entryName;
        CircuitBreakerTemplate template = this.template;

        template.onFailureRateExceededEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
        });

        template.onStateTransitionEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(STATE_TRANSITION, event.getEventType());
        });

        template.onCallNotPermittedEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(NOT_PERMITTED, event.getEventType());
        });

        template.onErrorEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(ERROR, event.getEventType());
        });

        template.onIgnoredErrorEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(IGNORED_ERROR, event.getEventType());
        });

        template.onResetEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(RESET, event.getEventType());
        });

        for (int i = 0; i < 10; i++) {
            executeNothing();
        }

        executeThrowing(RuntimeException::new);

        template.execute(entryName, CircuitBreaker::reset);

        executeThrowing(Exception::new);
        executeThrowing(RuntimeException::new);
        executeThrowing(RuntimeException::new);

    }

    @Test
    public void testExecuteOnSlowCall() {
        String entryName = this.entryName;
        CircuitBreakerTemplate template = this.template;

        template.onSlowCallRateExceededEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
        });

        template.onCallNotPermittedEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getCircuitBreakerName());
            assertSame(NOT_PERMITTED, event.getEventType());
        });

        for (int i = 0; i < 10; i++) {
            executeNothing();
        }

        await(duration, this::executeNothing);
        await(duration.toMillis() * 2, this::executeNothing);
        await(duration.toMillis() * 2, this::executeNothing);
        await(duration.toMillis() * 2, this::executeNothing);
    }

    private void executeNothing() {
        template.execute(entryName, () -> {
        });
    }

    private void executeThrowing(Supplier<? extends Throwable> throwableSupplier) {
        try {
            template.call(entryName, () -> {
                throw throwableSupplier.get();
            });
        } catch (Throwable e) {

        }
    }
}
