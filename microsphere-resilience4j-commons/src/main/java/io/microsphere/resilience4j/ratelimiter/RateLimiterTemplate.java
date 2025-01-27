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
package io.microsphere.resilience4j.ratelimiter;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnDrainedEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnFailureEvent;
import io.github.resilience4j.ratelimiter.event.RateLimiterOnSuccessEvent;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

/**
 * {@link Resilience4jTemplate} for {@link RateLimiter}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RateLimiter
 * @see RateLimiterConfig
 * @see RateLimiterRegistry
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public class RateLimiterTemplate extends Resilience4jTemplate<RateLimiter, RateLimiterConfig, RateLimiterRegistry> {

    public RateLimiterTemplate(RateLimiterRegistry registry) {
        super(registry);
    }

    /**
     * Create the {@link RateLimiter}
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @Override
    protected RateLimiter createEntry(String name) {
        RateLimiterRegistry registry = super.getRegistry();
        return registry.rateLimiter(name, super.getConfiguration(name), registry.getTags());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeExecute(Resilience4jContext<RateLimiter> context) {
        RateLimiter rateLimiter = context.getEntry();
        rateLimiter.acquirePermission();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterExecute(Resilience4jContext<RateLimiter> context) {
        RateLimiter rateLimiter = context.getEntry();
        Throwable failure = context.getFailure();
        if (failure == null) {
            Object result = context.getResult();
            if (result == null) {
                rateLimiter.onSuccess();
            } else {
                rateLimiter.onResult(result);
            }
        } else {
            rateLimiter.onError(failure);
        }
    }

    /**
     * Register the {@link EventConsumer} of {@link RateLimiterOnSuccessEvent}
     *
     * @param entryName     the name of entry
     * @param eventConsumer the {@link EventConsumer} of {@link RateLimiterOnSuccessEvent}
     * @return {@link RateLimiterTemplate}
     */
    public RateLimiterTemplate onSuccessEvent(String entryName, EventConsumer<RateLimiterOnSuccessEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, RateLimiterOnSuccessEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} of {@link RateLimiterOnFailureEvent}
     *
     * @param entryName     the name of entry
     * @param eventConsumer the {@link EventConsumer} of {@link RateLimiterOnFailureEvent}
     * @return {@link RateLimiterTemplate}
     */
    public RateLimiterTemplate onFailureEvent(String entryName, EventConsumer<RateLimiterOnFailureEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, RateLimiterOnFailureEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} of {@link RateLimiterOnDrainedEvent}
     *
     * @param entryName     the name of entry
     * @param eventConsumer the {@link EventConsumer} of {@link RateLimiterOnFailureEvent}
     * @return {@link RateLimiterTemplate}
     */
    public RateLimiterTemplate onDrainedEvent(String entryName, EventConsumer<RateLimiterOnDrainedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, RateLimiterOnDrainedEvent.class, eventConsumer);
        return this;
    }
}
