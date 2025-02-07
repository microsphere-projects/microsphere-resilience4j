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
import io.github.resilience4j.core.lang.Nullable;
import io.microsphere.lang.Prioritized;
import io.microsphere.lang.function.ThrowableConsumer;
import io.microsphere.lang.function.ThrowableFunction;
import io.microsphere.logging.Logger;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The Resilience4j advanced operations extend {@link Resilience4jOperations} includes:
 * <ul>
 *     <li>The Operations for Getter :
 *      <ul>
 *          <li>{@link #getRegistry()}</li>
 *          <li>{@link #getModule()}</li>
 *          <li>{@link #getEntryClass()}</li>
 *          <li>{@link #getConfigClass()}</li>
 *          <li>{@link #getLogger()}</li>
 *      </ul>
 *     </li>
 *     <li>The Operations for Resilience4j's Configuration :
 *     <ul>
 *        <li>{@link #getConfiguration(String)}</li>
 *        <li>{@link #getDefaultConfig()}</li>
 *        <li>{@link #addConfiguration(String, C)}</li>
 *     </ul>
 *     </li>
 *     <li>The Operations for Resilience4j's Entry
 *     <ul>
 *        <li>{@link #getEntry(String)}</li>
 *        <li>{@link #createEntry(String)}</li>
 *        <li>{@link #removeEntry(String)}</li>
 *        <li>{@link #replaceEntry(String, C)}</li>
 *     </ul>
 *     <li>The extended One-Time Operations:
 *     <ul>
 *         <li>{@link #execute(String, Consumer)}</li>
 *         <li>{@link #execute(String, Function)}</li>
 *         <li>{@link #call(String, ThrowableConsumer)}</li>
 *         <li>{@link #call(String, ThrowableFunction)}</li>
 *     </ul>
 *     </li>
 * </ul>
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's entry configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the type of Resilience4j's entry registry, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jModule
 * @since 1.0.0
 */
public interface AdvancedResilience4jOperations<E, C, R extends Registry<E, C>> extends Resilience4jOperations<E>, Prioritized {

    // The Operations for Getter

    /**
     * Get the Resilience4j Registry
     *
     * @return non-null
     */
    @NonNull
    R getRegistry();

    /**
     * Get the {@link Resilience4jModule Resilience4j's module}
     *
     * @return non-null
     */
    @NonNull
    Resilience4jModule getModule();

    /**
     * Get the class of Resilience4j's entry
     *
     * @return non-null
     */
    @NonNull
    default Class<E> getEntryClass() {
        return (Class<E>) getModule().getEntryClass();
    }

    /**
     * Get the class of Resilience4j's configuration
     *
     * @return non-null
     */
    @NonNull
    default Class<C> getConfigClass() {
        return (Class<C>) getModule().getConfigClass();
    }

    /**
     * Get the {@link Logger}
     *
     * @return non-null
     */
    @NonNull
    Logger getLogger();


    // The Operations for Resilience4j's Configuration

    /**
     * Get the {@link C configuration} by the specified name
     *
     * @param configName the specified configuration name
     * @return if the {@link C configuration} can't be found by the specified configuration name,
     * {@link #getDefaultConfig()} will be used as default
     */
    @NonNull
    default C getConfiguration(String configName) {
        Logger logger = getLogger();
        R registry = getRegistry();
        Optional<C> optionalConfiguration = registry.getConfiguration(configName);
        final C configuration;
        if (optionalConfiguration.isPresent()) {
            configuration = optionalConfiguration.get();
            if (logger.isTraceEnabled()) {
                logger.trace("The configuration[name : '{}'] was found in the registry[{}] : {}", configName, registry, configuration);
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace("The configuration[name : '{}'] was not found in the registry[{}]", configName, registry);
            }
            configuration = getDefaultConfig();
        }
        return configuration;
    }

    /**
     * Get the default {@link C configuration}
     *
     * @return non-null
     */
    @NonNull
    default C getDefaultConfig() {
        Logger logger = getLogger();
        R registry = getRegistry();
        final C configuration = registry.getDefaultConfig();
        if (logger.isTraceEnabled()) {
            logger.trace("The default configuration was retrieved in the registry[{}] : {}", registry, configuration);
        }
        return configuration;
    }

    /**
     * Adds a configuration to the registry
     *
     * @param configName    the configuration name
     * @param configuration the added configuration
     * @return {@link Resilience4jTemplate}
     */
    default AdvancedResilience4jOperations<E, C, R> addConfiguration(String configName, C configuration) {
        Logger logger = getLogger();
        R registry = getRegistry();
        registry.addConfiguration(configName, configuration);
        if (logger.isTraceEnabled()) {
            logger.trace("The configuration[name : '{}'] was added to the registry[{}] : {}", configName, registry, configuration);
        }
        return this;
    }

    // The Operations for Resilience4j's Entry

    /**
     * Get the Resilience4j's entry by the specified name
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @NonNull
    default E getEntry(String name) {
        Logger logger = getLogger();
        R registry = getRegistry();
        Optional<E> optionalEntry = registry.find(name);
        final E entry;
        if (optionalEntry.isPresent()) {
            entry = optionalEntry.get();
            if (logger.isTraceEnabled()) {
                logger.trace("The entry[name : '{}'] was found in the registry[{}] : {}", name, registry, entry);
            }
        } else {
            entry = createEntry(name);
            if (logger.isTraceEnabled()) {
                logger.trace("The entry[name : '{}'] was not found in the registry[{}] and created : {}", name, registry, entry);
            }
        }
        return entry;
    }

    /**
     * Create the Resilience4j's entry.
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @NonNull
    E createEntry(String name);

    /**
     * Remove the Resilience4j's entry.
     *
     * @param name the name of the Resilience4j's entry
     * @return <code>null</code> if can't be found by <code>name</code>
     */
    @Nullable
    default E removeEntry(String name) {
        Logger logger = getLogger();
        R registry = getRegistry();
        Optional<E> optionalEntry = registry.remove(name);
        final E entry;
        if (optionalEntry.isPresent()) {
            entry = optionalEntry.get();
            if (logger.isTraceEnabled()) {
                logger.trace("The entry[name : '{}'] was removed in the registry[{}] : {}", name, registry, entry);
            }
        } else {
            entry = null;
            if (logger.isTraceEnabled()) {
                logger.trace("The entry[name : '{}'] was not found in the registry[{}]", name, registry);
            }
        }
        return entry;
    }

    /**
     * Replace the Resilience4j's entry.
     *
     * @param name     the name of the Resilience4j's entry
     * @param newEntry the new Resilience4j's entry
     * @return the old Resilience4j's entry if replaced, otherwise <code>null</code>
     */
    default E replaceEntry(String name, E newEntry) {
        Logger logger = getLogger();
        R registry = getRegistry();
        Optional<E> optionalEntry = registry.replace(name, newEntry);
        final E oldEntry;
        if (optionalEntry.isPresent()) {
            oldEntry = optionalEntry.get();
            if (logger.isTraceEnabled()) {
                logger.trace("The entry[name : '{}' , old : {}] was replaced in the registry[{}] : ", name, oldEntry, registry, newEntry);
            }
        } else {
            oldEntry = null;
            if (logger.isTraceEnabled()) {
                logger.trace("The old entry[name : '{}'] was not found in the registry[{}], the new one will be add : {}", name, registry, newEntry);
            }
        }
        return oldEntry;
    }

    /**
     * Execute the {@link Consumer consumer} of the Resilience4j's entry without result
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryConsumer the {@link Consumer consumer} of the Resilience4j's entry
     */
    default AdvancedResilience4jOperations<E, C, R> execute(String name, Consumer<E> entryConsumer) {
        Logger logger = getLogger();
        E entry = getEntry(name);
        entryConsumer.accept(entry);
        if (logger.isTraceEnabled()) {
            logger.trace("The entry[name : '{}' , consumer : {}] was executed : {}", name, entryConsumer, entry);
        }
        return this;
    }

    /**
     * Execute the {@link Function function} of the Resilience4j's entry with result
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryFunction the {@link Function function} of the Resilience4j's entry
     * @param <T>           the type of result
     * @return the result of the <code>entryFunction</code>
     */
    default <T> T execute(String name, Function<E, T> entryFunction) {
        Logger logger = getLogger();
        E entry = getEntry(name);
        T result = entryFunction.apply(entry);
        if (logger.isTraceEnabled()) {
            logger.trace("The entry[name : '{}' , function : {}] was executed , result : {}", name, entryFunction, result);
        }
        return result;
    }

    /**
     * Call the {@link Consumer consumer} of the Resilience4j's entry without result
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryConsumer the {@link Consumer consumer} of the Resilience4j's entry
     * @throws Throwable any error caused by the execution of the <code>entryConsumer</code>
     */
    default AdvancedResilience4jOperations<E, C, R> call(String name, ThrowableConsumer<E> entryConsumer) throws Throwable {
        Logger logger = getLogger();
        E entry = getEntry(name);
        entryConsumer.accept(entry);
        if (logger.isTraceEnabled()) {
            logger.trace("The entry[name : '{}' , consumer : {}] was called : {}", name, entryConsumer, entry);
        }
        return this;
    }

    /**
     * Call the {@link Function function} of the Resilience4j's entry with result
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryFunction the {@link Function function} of the Resilience4j's entry
     * @param <T>           the type of result
     * @return the result of the <code>entryFunction</code>
     * @throws Throwable any error caused by the execution of the <code>entryFunction</code>
     */
    default <T> T call(String name, ThrowableFunction<E, T> entryFunction) throws Throwable {
        Logger logger = getLogger();
        E entry = getEntry(name);
        T result = entryFunction.apply(entry);
        if (logger.isTraceEnabled()) {
            logger.trace("The entry[name : '{}' , function : {}] was called , result : {}", name, entryFunction, result);
        }
        return result;
    }

    @Override
    default int getPriority() {
        return this.getModule().getDefaultAspectOrder();
    }
}
