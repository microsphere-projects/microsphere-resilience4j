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
package io.microsphere.resilience4j.retry;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.vavr.CheckedFunction0;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.ThreadLocal.withInitial;

/**
 * {@link Resilience4jTemplate} for {@link Retry}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Retry
 * @see RetryConfig
 * @see RetryRegistry
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public class RetryTemplate extends Resilience4jTemplate<Retry, RetryConfig, RetryRegistry> {

    /**
     * Local caching for {@link EventProcessor} of {@link Retry}
     */
    private final ConcurrentMap<String, EventProcessor<RetryEvent>> namedEntryProcessorsMap = new ConcurrentHashMap<>();

    public RetryTemplate(RetryRegistry registry) {
        super(registry);
    }

    @Override
    protected Map<String, Retry> createLocalEntriesCache() {
        return null;
    }

    @Override
    protected Retry createEntry(String name) {
        RetryRegistry registry = super.getRegistry();
        return Retry.of(name, super.getConfiguration(name), registry.getTags());
    }

    /**
     * Get the {@link EventProcessor} of {@link Retry} by the specified name
     *
     * @param name the specified name
     * @return non-null
     */
    protected final EventProcessor<RetryEvent> getEventProcessor(String name) {
        return namedEntryProcessorsMap.computeIfAbsent(name, n -> new EventProcessor<>());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <V> V execute(Resilience4jContext<Retry> context, CheckedFunction0<V> callback) throws Throwable {
        Retry retry = context.getEntry();
        String name = retry.getName();
        EventProcessor<RetryEvent> eventProcessor = getEventProcessor(name);
        if (eventProcessor.hasConsumers()) {
            Retry.EventPublisher eventPublisher = retry.getEventPublisher();
            eventPublisher.onEvent(event -> {
                eventProcessor.processEvent(event);
            });
        }
        Retry.Context<V> ctx = retry.context();
        do {
            try {
                V result = callback.apply();
                final boolean validationOfResult = ctx.onResult(result);
                if (!validationOfResult) {
                    ctx.onComplete();
                    return result;
                }
            } catch (Exception exception) {
                ctx.onError(exception);
            }
        } while (true);
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnErrorEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnErrorEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onErrorEvent(String entryName, EventConsumer<RetryOnErrorEvent> eventConsumer) {
        return doRegisterEntryEventConsumer(entryName, RetryOnErrorEvent.class, eventConsumer);
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnIgnoredErrorEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnIgnoredErrorEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onIgnoredErrorEvent(String entryName, EventConsumer<RetryOnIgnoredErrorEvent> eventConsumer) {
        return doRegisterEntryEventConsumer(entryName, RetryOnIgnoredErrorEvent.class, eventConsumer);
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnRetryEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnRetryEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onRetryEvent(String entryName, EventConsumer<RetryOnRetryEvent> eventConsumer) {
        return doRegisterEntryEventConsumer(entryName, RetryOnRetryEvent.class, eventConsumer);
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnSuccessEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnSuccessEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onSuccessEvent(String entryName, EventConsumer<RetryOnSuccessEvent> eventConsumer) {
        return doRegisterEntryEventConsumer(entryName, RetryOnSuccessEvent.class, eventConsumer);
    }

    protected RetryTemplate doRegisterEntryEventConsumer(String entryName, Class<? extends RetryEvent> eventType, EventConsumer<? extends RetryEvent> eventConsumer) {
        EventProcessor<RetryEvent> eventProcessor = getEventProcessor(entryName);
        eventProcessor.registerConsumer(eventType.getSimpleName(), eventConsumer);
        return this;
    }

    @Override
    public void destroy() {
        super.destroy();
        this.namedEntryProcessorsMap.clear();
    }
}
