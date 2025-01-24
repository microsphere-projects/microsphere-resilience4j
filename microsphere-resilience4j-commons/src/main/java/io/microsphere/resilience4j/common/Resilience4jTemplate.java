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
import io.vavr.CheckedRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static io.microsphere.util.Assert.assertNotNull;

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

    protected final Logger logger = getLogger(getClass());

    protected final R registry;

    protected final Resilience4jModule module;

    /**
     * Local Cache using {@link HashMap} with better performance,
     * it's no thread-safe and can be thread-safe if and only if it's initialized by
     * {@link #initLocalEntriesCache(String)} at the initialization phase.
     */
    protected final Map<String, E> localEntriesCache;

    public Resilience4jTemplate(R registry) {
        assertNotNull(registry, "The registry must not be null");
        this.registry = registry;
        this.module = valueOf(registry.getClass());
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
     * Adds a configuration to the registry
     *
     * @param configName    the configuration name
     * @param configuration the added configuration
     * @return {@link Resilience4jTemplate}
     */
    public Resilience4jTemplate<E, C, R> addConfiguration(String configName, C configuration) {
        registry.addConfiguration(configName, configuration);
        return this;
    }

    /**
     * Initialize the local entries cache
     *
     * @param entryNames the names of entries
     */
    public Resilience4jTemplate<E, C, R> initLocalEntriesCache(Iterable<String> entryNames) {
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
    public Resilience4jTemplate<E, C, R> initLocalEntriesCache(String entryName) {
        E entry = getEntry(entryName);
        localEntriesCache.put(entryName, entry);
        return this;
    }

    /**
     * Execute the target callback
     *
     * @param entryNameGenerator the generator of entry name
     * @param callback           the callback to be executed
     */
    public final void execute(Supplier<String> entryNameGenerator, CheckedRunnable callback) {
        execute(entryNameGenerator, () -> {
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
        Resilience4jContext<E> context = begin(entryNameGenerator);
        V result = null;
        try {
            result = execute(context, callback);
        } catch (Throwable e) {
            context.failure = e;
            if (logger.isDebugEnabled()) {
                logger.debug("It's failed to execute callback", e);
            }
        } finally {
            end(context);
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
        String entryName = entryNameGenerator.get();
        E entry = getEntry(entryName);
        Resilience4jContext<E> context = new Resilience4jContext(entryName, entry);
        beforeExecute(context);
        return context;
    }

    /**
     * End the execution as the final phase.
     *
     * @param context {@link Resilience4jContext}
     */
    public final void end(Resilience4jContext<E> context) {
        afterExecute(context);
    }

    /**
     * Destroy :
     * <ul>
     *     <li>clear the local entries cache</li>
     * </ul>
     */
    public void destroy() {
        localEntriesCache.clear();
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
     * @param context {@link Resilience4jContext}
     */
    protected abstract void beforeExecute(Resilience4jContext<E> context);

    /**
     * Callback after execution
     *
     * @param context {@link Resilience4jContext}
     * @return {@link CheckedFunction0#apply()}
     */
    protected abstract void afterExecute(Resilience4jContext<E> context);

}
