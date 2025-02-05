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
import io.microsphere.lang.function.ThrowableSupplier;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

import java.util.concurrent.ExecutorService;

import static io.microsphere.util.ExceptionUtils.create;
import static io.microsphere.util.ExceptionUtils.wrap;
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
    public TimeLimiter createEntry(String name) {
        TimeLimiterRegistry registry = super.getRegistry();
        return registry.timeLimiter(name, super.getConfiguration(name), registry.getTags());
    }

    @Override
    public void end(Resilience4jContext<TimeLimiter> context) {
        throw create(UnsupportedOperationException.class, "TimeLimiter does not support end operation");
    }

    @Override
    public <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
        TimeLimiter timeLimiter = getEntry(name);
        return timeLimiter.decorateFutureSupplier(() -> executorService.submit(() -> {
            try {
                return callback.get();
            } catch (Throwable t) {
                throw wrap(t, Exception.class);
            }
        })).call();
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
