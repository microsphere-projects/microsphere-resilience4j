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
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.microsphere.lang.function.ThrowableAction;
import io.microsphere.lang.function.ThrowableConsumer;
import io.microsphere.lang.function.ThrowableFunction;
import io.microsphere.lang.function.ThrowableSupplier;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.microsphere.reflect.MethodUtils.findMethod;
import static io.microsphere.reflect.MethodUtils.invokeMethod;
import static io.microsphere.util.ExceptionUtils.wrap;
import static java.util.Collections.emptyMap;

/**
 * The common operations for the single {@link Resilience4jModule Resilience4j module}:
 * <ul>
 *     <li>One-Time Operation :
 *      <ul>
 *          <li>{@link #execute(String, ThrowableSupplier)} or {@link #call(String, Supplier)} : execution with result</li>
 *          <li>{@link #execute(String, ThrowableAction)} or {@link #call(String, Runnable)} : execution without result</li>
 *      </ul>
 *     </li>
 *     <li>Two-Phase Operation (unsupported in those cases : {@link Retry} and {@link TimeLimiter}) :
 *        <li>{@link #begin(String)} : the first phase</li>
 *        <li>{@link #end(Resilience4jContext)} :  the second phase</li>
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
public interface Resilience4jOperations<E, C, R extends Registry<E, C>> {

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
     * Get the tags of Resilience4j's registry
     *
     * @return non-null
     */
    @NonNull
    default Map<String, String> getTags() {
        Method method = findMethod(Registry.class, "getTags");
        if (method != null) {
            return invokeMethod(getRegistry(), method);
        }
        return emptyMap();
    }

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
        return getRegistry().getConfiguration(configName).orElseGet(() -> getDefaultConfig());
    }

    /**
     * Get the default {@link C configuration}
     *
     * @return non-null
     */
    @NonNull
    default C getDefaultConfig() {
        return getRegistry().getDefaultConfig();
    }

    /**
     * Adds a configuration to the registry
     *
     * @param configName    the configuration name
     * @param configuration the added configuration
     * @return {@link Resilience4jTemplate}
     */
    default Resilience4jOperations<E, C, R> addConfiguration(String configName, C configuration) {
        getRegistry().addConfiguration(configName, configuration);
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
        Optional<E> optionalEntry = getRegistry().find(name);
        return optionalEntry.orElseGet(() -> createEntry(name));
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
        Optional<E> optionalEntry = getRegistry().remove(name);
        return optionalEntry.orElse(null);
    }

    /**
     * Replace the Resilience4j's entry.
     *
     * @param name     the name of the Resilience4j's entry
     * @param newEntry the new Resilience4j's entry
     * @return the old Resilience4j's entry if replaced, otherwise <code>null</code>
     */
    default E replaceEntry(String name, E newEntry) {
        Optional<E> optionalEntry = getRegistry().replace(name, newEntry);
        return optionalEntry.orElse(null);
    }

    // The One-Time Operations

    /**
     * Execute the callback with result
     *
     * @param name     the name of the Resilience4j's entry
     * @param callback the callback to be executed
     */
    default void execute(String name, Runnable callback) {
        execute(name, () -> {
            callback.run();
            return null;
        });
    }

    /**
     * Execute the callback with result
     *
     * @param name     the name of the Resilience4j's entry
     * @param callback the callback to be executed
     */
    default <T> T execute(String name, Supplier<T> callback) {
        try {
            return call(name, callback::get);
        } catch (Throwable t) {
            throw wrap(t, RuntimeException.class);
        }
    }

    /**
     * Execute the {@link Consumer consumer} of the Resilience4j's entry without result
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryConsumer the {@link Consumer consumer} of the Resilience4j's entry
     */
    default Resilience4jOperations<E, C, R> execute(String name, Consumer<E> entryConsumer) {
        E entry = getEntry(name);
        entryConsumer.accept(entry);
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
        E entry = getEntry(name);
        return entryFunction.apply(entry);
    }

    /**
     * Execute the callback without result, and may throw any error
     *
     * @param name     the name of the Resilience4j's entry
     * @param callback the callback to be executed
     * @throws Throwable any error caused by the execution of the callback
     */
    default void call(String name, ThrowableAction callback) throws Throwable {
        call(name, () -> {
            callback.execute();
            return null;
        });
    }

    /**
     * Execute the callback with result, and may throw any error
     *
     * @param name     the name of the Resilience4j's entry
     * @param callback the callback to be executed
     * @param <T>      the type of result
     * @throws Throwable any error caused by the execution of the callback
     */
    default <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
        Resilience4jContext context = begin(name);
        try {
            return callback.get();
        } finally {
            end(context);
        }
    }

    /**
     * Execute the {@link Consumer consumer} of the Resilience4j's entry without result
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryConsumer the {@link Consumer consumer} of the Resilience4j's entry
     * @throws Throwable any error caused by the execution of the <code>entryConsumer</code>
     */
    default Resilience4jOperations<E, C, R> call(String name, ThrowableConsumer<E> entryConsumer) throws Throwable {
        E entry = getEntry(name);
        entryConsumer.accept(entry);
        return this;
    }

    /**
     * Execute the {@link Function function} of the Resilience4j's entry with result
     *
     * @param name          the name of the Resilience4j's entry
     * @param entryFunction the {@link Function function} of the Resilience4j's entry
     * @param <T>           the type of result
     * @return the result of the <code>entryFunction</code>
     * @throws Throwable any error caused by the execution of the <code>entryFunction</code>
     */
    default <T> T call(String name, ThrowableFunction<E, T> entryFunction) throws Throwable {
        E entry = getEntry(name);
        return entryFunction.apply(entry);
    }

    // The Two-Phase Operations

    /**
     * Begin the execution in the first phase.
     *
     * @param name the name of the Resilience4j's entry
     * @return {@link Resilience4jContext} with the entry and its name
     */
    Resilience4jContext<E> begin(String name);

    /**
     * End the execution in the second phase.
     *
     * @param context {@link Resilience4jContext}
     */
    void end(Resilience4jContext<E> context);

}
