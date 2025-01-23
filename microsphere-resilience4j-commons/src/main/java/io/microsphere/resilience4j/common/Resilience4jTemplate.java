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
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.core.lang.NonNull;
import io.microsphere.logging.Logger;
import io.vavr.CheckedFunction0;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static io.microsphere.util.Assert.assertNotNull;
import static java.lang.System.nanoTime;

/**
 * The abstract template class for Resilience4j
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's entry configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the type of Resilience4j's entry registry, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jModule
 * @since 1.0.0
 */
public abstract class Resilience4jTemplate<E, C, R extends Registry<E, C>> {

    public static final long UNKNOWN_DURATION = -1L;

    protected final Logger logger = getLogger(getClass());

    protected final R registry;

    protected final Resilience4jModule module;

    protected final boolean durationRecorded;

    /**
     * Local Cache using {@link HashMap} with better performance
     */
    protected final Map<String, E> localEntriesCache;

    public Resilience4jTemplate(R registry) {
        this(registry, false);
    }

    public Resilience4jTemplate(R registry, boolean durationRecorded) {
        assertNotNull(registry, "The registry must not be null");
        this.registry = registry;
        this.module = valueOf(registry.getClass());
        this.durationRecorded = durationRecorded;
        this.localEntriesCache = new HashMap<>();
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
     * Whether the duration is recorded
     *
     * @return <code>true</code> if the duration is recorded, otherwise <code>false</code>
     */
    public final boolean isDurationRecorded() {
        return durationRecorded;
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
    public void initLocalEntriesCache(Iterable<String> entryNames) {
        for (String entryName : entryNames) {
            initLocalEntriesCache(entryName);
        }
    }

    /**
     * Initialize the local entries cache
     *
     * @param entryName the name of entry
     */
    public void initLocalEntriesCache(String entryName) {
        E entry = getEntry(entryName);
        localEntriesCache.put(entryName, entry);
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
        String entryName = entryNameGenerator.get();
        return execute(entryName, callback);
    }

    /**
     * Execute the target callback
     *
     * @param instance           the injected instance that will be used to get the entry name
     * @param entryNameGenerator the generator of entry name
     * @param callback           the callback to be executed
     * @param <I>                the type of instance
     * @param <V>                the type of result
     * @return {@link CheckedFunction0#apply()}
     */
    public final <I, V> V execute(I instance, Function<I, String> entryNameGenerator, CheckedFunction0<V> callback) {
        String entryName = entryNameGenerator.apply(instance);
        return execute(entryName, callback);
    }

    protected <V> V execute(String entryName, CheckedFunction0<V> callback) {
        Long duration = UNKNOWN_DURATION;
        E entry = getEntry(entryName);
        V result = null;
        Throwable failure = null;
        Long startTime = null;
        Resilience4jContext context = new Resilience4jContext();
        beforeExecute(entry, context);
        try {
            startTime = isDurationRecorded() ? nanoTime() : null;
            result = call(entry, callback);
        } catch (Throwable e) {
            failure = e;
            if (logger.isDebugEnabled()) {
                logger.debug("It's failed to execute callback", e);
            }
        } finally {
            if (startTime != null) {
                duration = nanoTime() - startTime;
            }
            afterExecute(entry, duration, result, failure, context);
        }
        return result;
    }

    /**
     * Call the target callback, for instance, the result maybe be wrapped.
     *
     * @param entry    Resilience4j's entry, e.g., {@link CircuitBreaker}
     * @param callback {@link CheckedFunction0}
     * @param <V>      the type of result
     * @return {@link CheckedFunction0#apply()}
     * @throws Throwable if {@link CheckedFunction0#apply()} throws an exception
     */
    protected <V> V call(E entry, CheckedFunction0<V> callback) throws Throwable {
        return callback.apply();
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
     * Get the Resilience4j's entry from cache by the specified name
     *
     * @param name the name of the Resilience4j's entry
     * @return <code>null</code> if can't be found
     */
    protected final E getEntryFromCache(String name) {
        return localEntriesCache.get(name);
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
     * Create the Resilience4j's entry
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @NonNull
    protected abstract E createEntry(String name);

    /**
     * Callback before execution
     *
     * @param entry   Resilience4j's entry, e.g., {@link CircuitBreaker}
     * @param context {@link Resilience4jContext}
     */
    protected abstract void beforeExecute(E entry, Resilience4jContext context);

    /**
     * Callback after execution
     *
     * @param <V>      the type of execution result
     * @param entry    Resilience4j's entry, e.g., {@link CircuitBreaker}
     * @param duration duration in nana seconds if {@link #isDurationRecorded()} is <code>true</code>, or
     *                 <code>duration</code> will be assigned to be {@link #UNKNOWN_DURATION}(value is <code>-1</code>)
     * @param result   the execution result
     * @param failure  optional {@link Throwable} instance, if <code>null</code>, it means the execution is successful
     * @param context  {@link Resilience4jContext}
     * @return {@link CheckedFunction0#apply()}
     */
    protected abstract <V> void afterExecute(E entry, Long duration, V result, Throwable failure, Resilience4jContext context);

}
