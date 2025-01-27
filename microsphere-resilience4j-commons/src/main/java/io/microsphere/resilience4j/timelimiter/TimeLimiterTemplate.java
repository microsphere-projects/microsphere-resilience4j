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
package io.microsphere.resilience4j.timelimiter;

import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnErrorEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnTimeoutEvent;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.vavr.CheckedFunction0;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.concurrent.ForkJoinPool.commonPool;

/**
 * {@link Resilience4jTemplate} for {@link TimeLimiter}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see TimeLimiter
 * @see TimeLimiterConfig
 * @see TimeLimiterRegistry
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public class TimeLimiterTemplate extends Resilience4jTemplate<TimeLimiter, TimeLimiterConfig, TimeLimiterRegistry> {

    private final ExecutorService executorService;

    public TimeLimiterTemplate(TimeLimiterRegistry registry) {
        this(registry, commonPool());
    }

    public TimeLimiterTemplate(TimeLimiterRegistry registry, ExecutorService executorService) {
        super(registry);
        this.executorService = executorService;
    }

    /**
     * Create the {@link TimeLimiter}
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @Override
    protected TimeLimiter createEntry(String name) {
        TimeLimiterRegistry registry = super.getRegistry();
        return registry.timeLimiter(name, super.getConfiguration(name), registry.getTags());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeExecute(Resilience4jContext<TimeLimiter> context) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <V> V execute(Resilience4jContext<TimeLimiter> context, CheckedFunction0<V> callback) throws Throwable {
        TimeLimiter timeLimiter = context.getEntry();
        Callable<V> docoratedCallable = timeLimiter.decorateFutureSupplier(() -> executorService.submit(() -> {
            try {
                return callback.apply();
            } catch (Throwable t) {
                throw new Exception(t);
            }
        }));
        return docoratedCallable.call();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterExecute(Resilience4jContext<TimeLimiter> context) {
    }

    /**
     * Register the {@link EventConsumer} for {@link TimeLimiterOnSuccessEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link TimeLimiterOnSuccessEvent}
     * @return {@link TimeLimiterTemplate}
     */
    public TimeLimiterTemplate onSuccessEvent(String entryName, EventConsumer<TimeLimiterOnSuccessEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, TimeLimiterOnSuccessEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link TimeLimiterOnErrorEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link TimeLimiterOnErrorEvent}
     * @return {@link TimeLimiterTemplate}
     */
    public TimeLimiterTemplate onErrorEvent(String entryName, EventConsumer<TimeLimiterOnErrorEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, TimeLimiterOnErrorEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} for {@link TimeLimiterOnTimeoutEvent}.
     *
     * @param entryName     the name of the entry
     * @param eventConsumer the {@link EventConsumer} for {@link TimeLimiterOnTimeoutEvent}
     * @return {@link TimeLimiterTemplate}
     */
    public TimeLimiterTemplate onTimeoutEvent(String entryName, EventConsumer<TimeLimiterOnTimeoutEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, TimeLimiterOnTimeoutEvent.class, eventConsumer);
        return this;
    }


}
