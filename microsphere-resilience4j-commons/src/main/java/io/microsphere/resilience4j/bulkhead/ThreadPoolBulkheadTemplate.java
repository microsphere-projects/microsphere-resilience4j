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
package io.microsphere.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.core.EventConsumer;
import io.microsphere.lang.function.ThrowableSupplier;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * {@link Resilience4jTemplate} for {@link ThreadPoolBulkhead}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see ThreadPoolBulkhead
 * @see ThreadPoolBulkheadConfig
 * @see ThreadPoolBulkheadRegistry
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public class ThreadPoolBulkheadTemplate extends Resilience4jTemplate<ThreadPoolBulkhead, ThreadPoolBulkheadConfig, ThreadPoolBulkheadRegistry> {

    public ThreadPoolBulkheadTemplate(ThreadPoolBulkheadRegistry registry) {
        super(registry);
    }

    /**
     * Create the {@link ThreadPoolBulkhead}
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @Override
    public ThreadPoolBulkhead createEntry(String name) {
        ThreadPoolBulkheadRegistry registry = super.getRegistry();
        return registry.bulkhead(name, super.getConfiguration(name), registry.getTags());
    }

    @Override
    public boolean isBeginSupported() {
        return false;
    }

    @Override
    public boolean isEndSupported() {
        return false;
    }

    @Override
    public <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
        ThreadPoolBulkhead threadPoolBulkhead = getEntry(name);
        CompletionStage<T> completionStage = threadPoolBulkhead.executeCallable(() -> ThrowableSupplier.execute(callback));
        CompletableFuture<T> future = completionStage.toCompletableFuture();
        try {
            return future.get();
        } catch (Throwable e) {
            throw e.getCause().getCause();
        }
    }

    /**
     * Register the {@link EventConsumer} of {@link BulkheadOnCallPermittedEvent}.
     *
     * @param entryName     the name of Resilience4j's entry
     * @param eventConsumer the {@link EventConsumer} of {@link BulkheadOnCallPermittedEvent}
     * @return {@link ThreadPoolBulkheadTemplate}
     */
    public ThreadPoolBulkheadTemplate onCallPermittedEvent(String entryName, EventConsumer<BulkheadOnCallPermittedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, BulkheadOnCallPermittedEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} of {@link BulkheadOnCallRejectedEvent}.
     *
     * @param entryName     the name of Resilience4j's entry
     * @param eventConsumer the {@link EventConsumer} of {@link BulkheadOnCallRejectedEvent}
     * @return {@link ThreadPoolBulkheadTemplate}
     */
    public ThreadPoolBulkheadTemplate onCallRejectedEvent(String entryName, EventConsumer<BulkheadOnCallRejectedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, BulkheadOnCallRejectedEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} of {@link BulkheadOnCallFinishedEvent}.
     *
     * @param entryName     the name of Resilience4j's entry
     * @param eventConsumer the {@link EventConsumer} of {@link BulkheadOnCallFinishedEvent}
     * @return {@link ThreadPoolBulkheadTemplate}
     */
    public ThreadPoolBulkheadTemplate onCallFinishedEvent(String entryName, EventConsumer<BulkheadOnCallFinishedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, BulkheadOnCallFinishedEvent.class, eventConsumer);
        return this;
    }
}