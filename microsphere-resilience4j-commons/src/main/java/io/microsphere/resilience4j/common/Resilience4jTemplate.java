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
package io.microsphere.resilience4j.common;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.microsphere.logging.Logger;
import io.microsphere.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static io.microsphere.resilience4j.util.Resilience4jUtils.getEventProcessor;
import static io.microsphere.text.FormatUtils.format;
import static io.microsphere.util.Assert.assertNotNull;
import static io.microsphere.util.ExceptionUtils.create;

/**
 * The abstract template class for {@link AdvancedResilience4jOperations Resilience4j operations}.
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's entry configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the type of Resilience4j's entry registry, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jModule
 * @see Resilience4jContext
 * @see Registry
 * @see RegistryEventConsumer
 * @since 1.0.0
 */
public abstract class Resilience4jTemplate<E, C, R extends Registry<E, C>> implements AdvancedResilience4jOperations<E, C, R> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final R registry;

    protected final Resilience4jModule module;

    protected final EventProcessor registryEventProcessor;

    /**
     * Local Cache using {@link HashMap} with better performance,
     * it's no thread-safe and can be thread-safe if and only if it's initialized by
     * {@link #initLocalEntriesCache(String)} at the initialization phase.
     */
    protected final Map<String, E> localEntriesCache;

    private int priority;

    public Resilience4jTemplate(R registry) {
        assertNotNull(registry, "The registry must not be null");
        this.registry = registry;
        this.registryEventProcessor = getEventProcessor(registry);
        this.module = valueOf(registry.getClass());
        this.localEntriesCache = createLocalEntriesCache();
        this.priority = this.module.getDefaultAspectOrder();
        if (logger.isTraceEnabled()) {
            logger.trace("Resilience4jTemplate was created : {}", toString());
        }
    }

    /**
     * Get the Resilience4j Registry
     *
     * @return non-null
     */
    @NonNull
    @Override
    public final R getRegistry() {
        return registry;
    }

    /**
     * Get the {@link Resilience4jModule Resilience4j's module}
     *
     * @return non-null
     */
    @NonNull
    @Override
    public final Resilience4jModule getModule() {
        return module;
    }

    /**
     * Get the default {@link C configuration}
     *
     * @return non-null
     */
    @NonNull
    @Override
    public final C getDefaultConfig() {
        return AdvancedResilience4jOperations.super.getDefaultConfig();
    }

    /**
     * Get the class of Resilience4j's entry
     *
     * @return non-null
     */
    @NonNull
    @Override
    public final Class<E> getEntryClass() {
        return AdvancedResilience4jOperations.super.getEntryClass();
    }

    /**
     * Get the class of Resilience4j's configuration
     *
     * @return non-null
     */
    @NonNull
    @Override
    public final Class<C> getConfigClass() {
        return AdvancedResilience4jOperations.super.getConfigClass();
    }

    @Override
    public final Logger getLogger() {
        return this.logger;
    }

    /**
     * Initialize the local entries cache
     *
     * @param names the names of entries
     */
    public final Resilience4jTemplate<E, C, R> initLocalEntriesCache(Iterable<String> names) {
        for (String entryName : names) {
            initLocalEntriesCache(entryName);
        }
        return this;
    }

    /**
     * Initialize the local entries cache
     *
     * @param name the name of entry
     */
    public final Resilience4jTemplate<E, C, R> initLocalEntriesCache(String name) {
        if (isLocalEntriesCachePresent()) {
            E entry = getEntry(name);
            localEntriesCache.put(name, entry);
            if (logger.isTraceEnabled()) {
                logger.info("The local entries cache for entry[name : '{}'] was initialized : {}", name, entry);
            }
        } else {
            logger.warn("The local entries cache is not required, please review the #createLocalEntriesCache() method implementation.");
            return this;
        }
        return this;
    }

    /**
     * Get the Resilience4j's entry by the specified name
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @NonNull
    @Override
    public final E getEntry(String name) {
        E entry = getEntryFromCache(name);
        if (entry != null) {
            return entry;
        }
        return AdvancedResilience4jOperations.super.getEntry(name);
    }

    /**
     * Default, an instance of {@link HashMap} will be created as a local cache to enhance better performance ,
     * it's no thread-safe and can be thread-safe if and only if it's initialized by
     * {@link #initLocalEntriesCache(String)} at the initialization phase.
     *
     * @return {@link HashMap} as default, <code>null</code> means the local cache is not required.
     */
    @Nullable
    protected Map<String, E> createLocalEntriesCache() {
        return new HashMap<>();
    }

    /**
     * Is local entries caches present or not?
     *
     * @return <code>true</code> if present, otherwise <code>false</code>
     */
    protected final boolean isLocalEntriesCachePresent() {
        return this.localEntriesCache != null;
    }


    /**
     * Clear the local entries cache
     */
    protected final void clearLocalEntriesCache() {
        localEntriesCache.clear();
        if (logger.isTraceEnabled()) {
            logger.trace("The local entries cache was cleared!");
        }
    }

    /**
     * Get the Resilience4j's entry from cache by the specified name.
     *
     * @param name the name of the Resilience4j's entry
     * @return <code>null</code> if can't be found or the local cache is not required
     */
    protected final E getEntryFromCache(String name) {
        E entry = null;
        if (isLocalEntriesCachePresent()) {
            entry = localEntriesCache.get(name);
            if (entry == null) {
                if (logger.isTraceEnabled()) {
                    logger.trace("The local entries cache for entry[name : '{}'] was not found.", name);
                }
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("The local entries cache for entry[name : '{}'] was found : {}", name, entry);
                }
            }
        }
        return entry;
    }

    /**
     * Adds a configuration to the registry
     *
     * @param configName    the configuration name
     * @param configuration the added configuration
     * @return {@link Resilience4jTemplate}
     */
    @Override
    public final AdvancedResilience4jOperations<E, C, R> addConfiguration(String configName, C configuration) {
        return AdvancedResilience4jOperations.super.addConfiguration(configName, configuration);
    }

    /**
     * Get the {@link C configuration} by the specified name
     *
     * @param configName the specified configuration name
     * @return if the {@link C configuration} can't be found by the specified configuration name,
     * {@link #getDefaultConfig()} will be used as default
     */
    @NonNull
    @Override
    public final C getConfiguration(String configName) {
        return AdvancedResilience4jOperations.super.getConfiguration(configName);
    }

    @Override
    public final Resilience4jContext<E> begin(String name) {
        E entry = getEntry(name);
        Resilience4jContext<E> context = new Resilience4jContext(name, entry);
        doBegin(context);
        if (logger.isTraceEnabled()) {
            logger.trace("begin() operation was executed -> ", context);
        }
        return context;
    }

    protected void doBegin(Resilience4jContext<E> context) {
        throw create(UnsupportedOperationException.class, format("{} does not support begin operation", this.getClass()));
    }

    @Override
    public final void end(Resilience4jContext<E> context) {
        doEnd(context);
        if (logger.isTraceEnabled()) {
            logger.trace("end() operation was executed -> {}", context);
        }
    }

    protected void doEnd(Resilience4jContext<E> context) {
        throw create(UnsupportedOperationException.class, format("{} does not support end operation", this.getClass()));
    }

    /**
     * Register the {@link EventConsumer} of {@link EntryAddedEvent}
     *
     * @param entryAddedEventEventConsumer the {@link EventConsumer} of {@link EntryAddedEvent}
     * @return {@link Resilience4jTemplate}
     */
    public final Resilience4jTemplate<E, C, R> onEntryAddedEvent(EventConsumer<EntryAddedEvent<E>> entryAddedEventEventConsumer) {
        return registerRegistryEventConsumer(EntryAddedEvent.class, entryAddedEventEventConsumer);
    }

    /**
     * Register the {@link EventConsumer} of {@link EntryRemovedEvent}
     *
     * @param entryRemovedEventEventConsumer the {@link EventConsumer} of {@link EntryAddedEvent}
     * @return {@link Resilience4jTemplate}
     */
    public final Resilience4jTemplate<E, C, R> onEntryRemovedEvent(EventConsumer<EntryRemovedEvent<E>> entryRemovedEventEventConsumer) {
        return registerRegistryEventConsumer(EntryRemovedEvent.class, entryRemovedEventEventConsumer);
    }

    /**
     * Register the {@link EventConsumer} of {@link EntryReplacedEvent}
     *
     * @param entryReplacedEventEventConsumer the {@link EventConsumer} of {@link EntryAddedEvent}
     * @return {@link Resilience4jTemplate}
     */
    public final Resilience4jTemplate<E, C, R> onEntryReplacedEvent(EventConsumer<EntryReplacedEvent<E>> entryReplacedEventEventConsumer) {
        return registerRegistryEventConsumer(EntryReplacedEvent.class, entryReplacedEventEventConsumer);
    }

    /**
     * Register the {@link EventConsumer} for RegistryEvent
     *
     * @param eventType     the type of Resilience4j event
     * @param eventConsumer EventConsumer
     * @param <T>           the type of Resilience4j event
     * @return {@link Resilience4jTemplate}
     */
    protected final <T extends RegistryEvent> Resilience4jTemplate<E, C, R> registerRegistryEventConsumer(
            Class<? super T> eventType, EventConsumer<T> eventConsumer) {
        return registerEventConsumer(registryEventProcessor, eventType, eventConsumer);
    }

    /**
     * Register the {@link EventConsumer} for {@link E Resilience4j's entry, e.g., {@link CircuitBreaker}}
     *
     * @param entryName     the name of Resilience4j's entry
     * @param eventType     the event type of Resilience4j's entry
     * @param eventConsumer the {@link EventConsumer event consumer} of Resilience4j's entry
     * @param <T>           the event type of Resilience4j's entry
     * @return {@link Resilience4jTemplate}
     */
    protected final <T> Resilience4jTemplate<E, C, R> registerEntryEventConsumer(String entryName,
                                                                                 Class<? super T> eventType, EventConsumer<T> eventConsumer) {
        E entry = getEntry(entryName);
        EventProcessor entryEventProcessor = getEventProcessor(entry);
        return registerEventConsumer(entryEventProcessor, eventType, eventConsumer);
    }

    private <T> Resilience4jTemplate<E, C, R> registerEventConsumer(EventProcessor eventProcessor,
                                                                    Class<? super T> eventType, EventConsumer<T> eventConsumer) {
        String name = eventType.getSimpleName();
        eventProcessor.registerConsumer(name, eventConsumer);
        if (logger.isTraceEnabled()) {
            logger.trace("The event[type : '{}'] consumer[name : '{}'] was registered : {}", eventType, name, eventConsumer);
        }
        return this;
    }

    @Override
    public final int getPriority() {
        return priority;
    }

    /**
     * Set the priority
     *
     * @param priority
     * @return {@link Resilience4jTemplate}
     */
    public final Resilience4jTemplate<E, C, R> setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Destroy :
     * <ul>
     *     <li>clear the local entries cache if required</li>
     * </ul>
     */
    public void destroy() {
        if (isLocalEntriesCachePresent()) {
            clearLocalEntriesCache();
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" +
                "registry = " + registry +
                ", module = " + module +
                ", registryEventProcessor = " + registryEventProcessor +
                ", localEntriesCache = " + localEntriesCache +
                ", priority = " + priority +
                '}';
    }
}
