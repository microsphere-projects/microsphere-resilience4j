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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import io.github.resilience4j.core.EventConsumer;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

/**
 * {@link Resilience4jTemplate} for {@link Bulkhead}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Bulkhead
 * @see BulkheadConfig
 * @see BulkheadRegistry
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public class BulkheadTemplate extends Resilience4jTemplate<Bulkhead, BulkheadConfig, BulkheadRegistry> {

    public BulkheadTemplate(BulkheadRegistry registry) {
        super(registry);
    }

    /**
     * Create the {@link Bulkhead}
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @Override
    public Bulkhead createEntry(String name) {
        BulkheadRegistry registry = super.getRegistry();
        return registry.bulkhead(name, super.getConfiguration(name), registry.getTags());
    }

    @Override
    protected void doBegin(Resilience4jContext<Bulkhead> context) {
        Bulkhead bulkhead = context.getEntry();
        bulkhead.acquirePermission();
    }

    @Override
    protected void doEnd(Resilience4jContext<Bulkhead> context) {
        Bulkhead bulkhead = context.getEntry();
        bulkhead.onComplete();
    }

    /**
     * Register the {@link EventConsumer} of {@link BulkheadOnCallPermittedEvent}.
     *
     * @param entryName     the name of Resilience4j's entry
     * @param eventConsumer the {@link EventConsumer} of {@link BulkheadOnCallPermittedEvent}
     * @return {@link BulkheadTemplate}
     */
    public BulkheadTemplate onCallPermittedEvent(String entryName, EventConsumer<BulkheadOnCallPermittedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, BulkheadOnCallPermittedEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} of {@link BulkheadOnCallRejectedEvent}.
     *
     * @param entryName     the name of Resilience4j's entry
     * @param eventConsumer the {@link EventConsumer} of {@link BulkheadOnCallRejectedEvent}
     * @return {@link BulkheadTemplate}
     */
    public BulkheadTemplate onCallRejectedEvent(String entryName, EventConsumer<BulkheadOnCallRejectedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, BulkheadOnCallRejectedEvent.class, eventConsumer);
        return this;
    }

    /**
     * Register the {@link EventConsumer} of {@link BulkheadOnCallFinishedEvent}.
     *
     * @param entryName     the name of Resilience4j's entry
     * @param eventConsumer the {@link EventConsumer} of {@link BulkheadOnCallFinishedEvent}
     * @return {@link BulkheadTemplate}
     */
    public BulkheadTemplate onCallFinishedEvent(String entryName, EventConsumer<BulkheadOnCallFinishedEvent> eventConsumer) {
        registerEntryEventConsumer(entryName, BulkheadOnCallFinishedEvent.class, eventConsumer);
        return this;
    }
}
