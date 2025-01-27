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
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnCallNotPermittedEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnFailureRateExceededEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnIgnoredErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnResetEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSlowCallRateExceededEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.EventConsumer;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

import java.util.concurrent.TimeUnit;

/**
 * {@link Resilience4jTemplate} for {@link CircuitBreaker}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see CircuitBreaker
 * @see CircuitBreakerConfig
 * @see CircuitBreakerRegistry
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public class CircuitBreakerTemplate extends Resilience4jTemplate<CircuitBreaker, CircuitBreakerConfig, CircuitBreakerRegistry> {

    public CircuitBreakerTemplate(CircuitBreakerRegistry registry) {
        super(registry);
    }

    /**
     * Create the {@link CircuitBreaker}
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @Override
    protected CircuitBreaker createEntry(String name) {
        CircuitBreakerRegistry registry = super.getRegistry();
        return registry.circuitBreaker(name, super.getConfiguration(name), registry.getTags());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeExecute(Resilience4jContext<CircuitBreaker> context) {
        CircuitBreaker circuitBreaker = context.getEntry();
        circuitBreaker.acquirePermission();
        context.setStartTime(circuitBreaker.getCurrentTimestamp());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterExecute(Resilience4jContext<CircuitBreaker> context) {
        CircuitBreaker circuitBreaker = context.getEntry();
        Throwable failure = context.getFailure();
        long startTime = context.getStartTime();
        long duration = circuitBreaker.getCurrentTimestamp() - startTime;
        TimeUnit timeUnit = circuitBreaker.getTimestampUnit();
        if (failure == null) {
            Object result = context.getResult();
            if (result == null) {
                circuitBreaker.onSuccess(duration, timeUnit);
            } else {
                circuitBreaker.onResult(duration, timeUnit, result);
            }
        } else {
            circuitBreaker.onError(duration, timeUnit, failure);
        }
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnCallNotPermittedEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnCallNotPermittedEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onCallNotPermittedEvent(String entryName, EventConsumer<CircuitBreakerOnCallNotPermittedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnCallNotPermittedEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnErrorEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnErrorEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onErrorEvent(String entryName, EventConsumer<CircuitBreakerOnErrorEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnErrorEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnFailureRateExceededEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnFailureRateExceededEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onFailureRateExceededEvent(String entryName, EventConsumer<CircuitBreakerOnFailureRateExceededEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnFailureRateExceededEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnIgnoredErrorEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnIgnoredErrorEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onIgnoredErrorEvent(String entryName, EventConsumer<CircuitBreakerOnIgnoredErrorEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnIgnoredErrorEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnResetEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnResetEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onResetEvent(String entryName, EventConsumer<CircuitBreakerOnResetEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnResetEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnSlowCallRateExceededEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnSlowCallRateExceededEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onSlowCallRateExceededEvent(String entryName, EventConsumer<CircuitBreakerOnSlowCallRateExceededEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnSlowCallRateExceededEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnSlowCallRateExceededEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnSlowCallRateExceededEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onStateTransitionEvent(String entryName, EventConsumer<CircuitBreakerOnStateTransitionEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnStateTransitionEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link CircuitBreakerOnSuccessEvent}.
     *
     * @param entryName     the name of {@link CircuitBreaker} instance
     * @param eventConsumer the {@link EventConsumer} for {@link CircuitBreakerOnSuccessEvent}
     * @return {@link CircuitBreakerTemplate}
     */
    public CircuitBreakerTemplate onSuccessEvent(String entryName, EventConsumer<CircuitBreakerOnSuccessEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, CircuitBreakerOnSuccessEvent.class, eventConsumer);
        return this;
    }
}
