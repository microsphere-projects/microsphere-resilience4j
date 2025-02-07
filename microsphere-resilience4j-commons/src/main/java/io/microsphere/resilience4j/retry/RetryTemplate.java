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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnIgnoredErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import io.github.resilience4j.retry.event.RetryOnSuccessEvent;
import io.microsphere.lang.function.ThrowableSupplier;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

import static io.github.resilience4j.retry.Retry.decorateCheckedSupplier;

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

    public RetryTemplate(RetryRegistry registry) {
        super(registry);
    }

    @Override
    public Retry createEntry(String name) {
        RetryRegistry registry = super.getRegistry();
        return registry.retry(name, super.getConfiguration(name));
    }

    @Override
    public <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
        Retry retry = getEntry(name);
        return decorateCheckedSupplier(retry, callback::get).apply();
    }

    @Override
    public boolean isBeginSupported() {
        return false;
    }

    @Override
    public boolean isEndSupported() {
        return false;
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnErrorEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnErrorEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onErrorEvent(String entryName, EventConsumer<RetryOnErrorEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, RetryOnErrorEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnIgnoredErrorEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnIgnoredErrorEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onIgnoredErrorEvent(String entryName, EventConsumer<RetryOnIgnoredErrorEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, RetryOnIgnoredErrorEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnRetryEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnRetryEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onRetryEvent(String entryName, EventConsumer<RetryOnRetryEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, RetryOnRetryEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link RetryOnSuccessEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link RetryOnSuccessEvent}
     * @return {@link RetryTemplate}
     */
    public RetryTemplate onSuccessEvent(String entryName, EventConsumer<RetryOnSuccessEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, RetryOnSuccessEvent.class, eventConsumer);
        return this;
    }
}
