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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.microsphere.logging.Logger;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static io.microsphere.resilience4j.util.Resilience4jUtils.getEventProcessor;
import static io.microsphere.util.Assert.assertNotNull;

/**
 * The abstract template class for Resilience4j supports the common operations:
 * <ul>
 *     <li>One-Time Operation :
 *      <ul>
 *          <li>{@link #execute(Supplier, CheckedFunction0)} or {@link #execute(String, CheckedFunction0)} : execution with result</li>
 *          <li>{@link #execute(Supplier, CheckedRunnable)} or {@link #execute(String, CheckedRunnable)} : execution without result</li>
 *      </ul>
 *     </li>
 *     <li>Two-Phase Operation (unsupported in those cases : {@link Retry} and {@link TimeLimiter}) :
 *        <li>{@link #begin(Supplier)} or {@link #begin(String)} : the first phase</li>
 *        <li>{@link #end(Resilience4jContext)} :  the second phase</li>
 *     </li>
 * </ul>
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
public abstract class Resilience4jTemplate<E, C, R extends Registry<E, C>> {

    protected final Logger logger = getLogger(getClass());

    protected final R registry;

    protected final Resilience4jModule module;

    protected final EventProcessor registryEventProcessor;

    /**
     * Local Cache using {@link HashMap} with better performance,
     * it's no thread-safe and can be thread-safe if and only if it's initialized by
     * {@link #initLocalEntriesCache(String)} at the initialization phase.
     */
    protected final Map<String, E> localEntriesCache;

    public Resilience4jTemplate(R registry) {
        assertNotNull(registry, "The registry must not be null");
        this.registry = registry;
        this.registryEventProcessor = getEventProcessor(registry);
        this.module = valueOf(registry.getClass());
        this.localEntriesCache = createLocalEntriesCache();
    }

    /**
     * Get the Resilience4j Registry
     *
     * @return non-null
     */
    @NonNull
    public final R getRegistry() {
        return registry;
    }

    /**
     * Get the {@link Resilience4jModule Resilience4j's module}
     *
     * @return non-null
     */
    @NonNull
    public final Resilience4jModule getModule() {
        return module;
    }

    /**
     * Get the default {@link C configuration}
     *
     * @return non-null
     */
    @NonNull
    public final C getDefaultConfig() {
        return registry.getDefaultConfig();
    }

    /**
     * Get the class of Resilience4j's entry
     *
     * @return non-null
     */
    @NonNull
    public final Class<E> getEntryClass() {
        return (Class<E>) this.module.getEntryClass();
    }

    /**
     * Get the class of Resilience4j's configuration
     *
     * @return non-null
     */
    @NonNull
    public final Class<C> getConfigClass() {
        return (Class<C>) this.module.getConfigClass();
    }

    /**
     * Initialize the local entries cache
     *
     * @param entryNames the names of entries
     */
    public final Resilience4jTemplate<E, C, R> initLocalEntriesCache(Iterable<String> entryNames) {
        for (String entryName : entryNames) {
            initLocalEntriesCache(entryName);
        }
        return this;
    }

    /**
     * Initialize the local entries cache
     *
     * @param entryName the name of entry
     */
    public final Resilience4jTemplate<E, C, R> initLocalEntriesCache(String entryName) {
        if (isLocalEntriesCachePresent()) {
            E entry = getEntry(entryName);
            localEntriesCache.put(entryName, entry);
        } else {
            logger.warn("The local entries cache is not required, please review the #createLocalEntriesCache() method implementation.");
            return this;
        }
        return this;
    }

    /**
     * Execute the target callback
     *
     * @param entryNameGenerator the generator of entry name
     * @param callback           the callback to be executed
     */
    public final void execute(Supplier<String> entryNameGenerator, CheckedRunnable callback) {
        execute(entryNameGenerator.get(), callback);
    }

    /**
     * Execute the target callback
     *
     * @param entryName the entry name
     * @param callback  the callback to be executed
     */
    public final void execute(String entryName, CheckedRunnable callback) {
        execute(entryName, () -> {
            callback.run();
            return null;
        });
    }

    /**
     * Execute the target callback
     *
     * @param entryNameGenerator the generator of entry name
     * @param callback           the callback to be executed
     * @param <V>                the type of result
     * @return {@link CheckedFunction0#apply()}
     */
    public final <V> V execute(Supplier<String> entryNameGenerator, CheckedFunction0<V> callback) {
        return execute(entryNameGenerator.get(), callback);
    }

    /**
     * Execute the target callback
     *
     * @param entryName the entry name
     * @param callback  the callback to be executed
     * @param <V>       the type of result
     * @return {@link CheckedFunction0#apply()}
     */
    public final <V> V execute(String entryName, CheckedFunction0<V> callback) {
        Resilience4jContext<E> context = beforeExecute(entryName);
        V result = null;
        try {
            result = execute(context, callback);
            context.result = result;
        } catch (Throwable e) {
            if (context != null) {
                context.failure = e;
            }
            if (logger.isDebugEnabled()) {
                logger.debug("It's failed to execute callback", e);
            }
        } finally {
            if (context != null) {
                afterExecute(context);
            }
        }
        return result;
    }

    /**
     * Begin the execution as the first phase.
     *
     * @param entryNameGenerator the generator of entry name
     * @return {@link Resilience4jContext} with the entry and its name
     */
    public final Resilience4jContext<E> begin(Supplier<String> entryNameGenerator) {
        return begin(entryNameGenerator.get());
    }

    /**
     * Begin the execution as the first phase.
     *
     * @param entryName the entry name
     * @return {@link Resilience4jContext} with the entry and its name
     */
    public Resilience4jContext<E> begin(String entryName) {
        return beforeExecute(entryName);
    }

    /**
     * End the execution as the second phase.
     *
     * @param context {@link Resilience4jContext}
     */
    public void end(Resilience4jContext<E> context) {
        afterExecute(context);
    }

    /**
     * Callback before {@link #execute(Resilience4jContext, CheckedFunction0) execution}.
     *
     * @param entryName the entry name
     * @return {@link Resilience4jContext} with the entry and its name
     * @see #beforeExecute(Resilience4jContext)
     */
    protected final Resilience4jContext<E> beforeExecute(String entryName) {
        E entry = getEntry(entryName);
        Resilience4jContext<E> context = new Resilience4jContext(entryName, entry);
        beforeExecute(context);
        return context;
    }

    /**
     * Callback before {@link #execute(Resilience4jContext, CheckedFunction0) execution}.
     *
     * @param context {@link Resilience4jContext}
     */
    protected void beforeExecute(Resilience4jContext<E> context) {
    }

    /**
     * Call the target callback, for instance, the result maybe be wrapped.
     *
     * @param <V>      the type of result
     * @param context  {@link Resilience4jContext}
     * @param callback {@link CheckedFunction0}
     * @return {@link CheckedFunction0#apply()}
     * @throws Throwable if {@link CheckedFunction0#apply()} throws an exception
     */
    protected <V> V execute(Resilience4jContext<E> context, CheckedFunction0<V> callback) throws Throwable {
        return callback.apply();
    }

    /**
     * Callback after {@link #execute(Resilience4jContext, CheckedFunction0) execution}
     *
     * @param context {@link Resilience4jContext}
     * @return {@link CheckedFunction0#apply()}
     */
    protected void afterExecute(Resilience4jContext<E> context) {
    }

    /**
     * Execute the specified {@link E Resilience4j's entry} by name
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryConsumer the {@link Consumer} for the Resilience4j's entry
     * @return
     */
    public final Resilience4jTemplate<E, C, R> executeEntry(String name, Consumer<E> entryConsumer) {
        E entry = getEntry(name);
        entryConsumer.accept(entry);
        return this;
    }

    /**
     * Get the Resilience4j's entry by the specified name
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @NonNull
    protected final E getEntry(String name) {
        E entry = getEntryFromCache(name);
        if (entry != null) {
            return entry;
        }
        Optional<E> optionalEntry = registry.find(name);
        return optionalEntry.orElseGet(() -> createEntry(name));
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
    protected boolean isLocalEntriesCachePresent() {
        return this.localEntriesCache != null;
    }

    /**
     * Get the Resilience4j's entry from cache by the specified name.
     *
     * @param name the name of the Resilience4j's entry
     * @return <code>null</code> if can't be found or the local cache is not required
     */
    protected final E getEntryFromCache(String name) {
        return isLocalEntriesCachePresent() ? localEntriesCache.get(name) : null;
    }

    /**
     * Create the Resilience4j's entry.
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @NonNull
    protected abstract E createEntry(String name);

    /**
     * Remove the Resilience4j's entry.
     *
     * @param name the name of the Resilience4j's entry
     * @return <code>null</code> if can't be found by <code>name</code>
     */
    @Nullable
    protected E removeEntry(String name) {
        Optional<E> optionalEntry = this.registry.remove(name);
        return optionalEntry.orElse(null);
    }

    /**
     * Replace the Resilience4j's entry.
     *
     * @param name     the name of the Resilience4j's entry
     * @param newEntry the new Resilience4j's entry
     * @return the old Resilience4j's entry if replaced, otherwise <code>null</code>
     */
    protected E replaceEntry(String name, E newEntry) {
        Optional<E> optionalEntry = this.registry.replace(name, newEntry);
        return optionalEntry.orElse(null);
    }

    /**
     * Adds a configuration to the registry
     *
     * @param configName    the configuration name
     * @param configuration the added configuration
     * @return {@link Resilience4jTemplate}
     */
    public Resilience4jTemplate<E, C, R> configuration(String configName, C configuration) {
        registry.addConfiguration(configName, configuration);
        return this;
    }

    /**
     * Get the {@link C configuration} by the specified name
     *
     * @param configName the specified configuration name
     * @return if the {@link C configuration} can't be found by the specified configuration name,
     * {@link #getDefaultConfig()} will be used as default
     */
    @NonNull
    protected final C getConfiguration(String configName) {
        return registry.getConfiguration(configName).orElse(getDefaultConfig());
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
        eventProcessor.registerConsumer(eventType.getSimpleName(), eventConsumer);
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
            localEntriesCache.clear();
        }
    }
}
