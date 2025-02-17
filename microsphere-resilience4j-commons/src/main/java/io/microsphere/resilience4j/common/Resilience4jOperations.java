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
import io.microsphere.lang.function.ThrowableAction;
import io.microsphere.lang.function.ThrowableSupplier;

import java.util.function.Supplier;

import static io.microsphere.util.ExceptionUtils.wrap;

/**
 * The common operations for the single {@link Resilience4jModule Resilience4j module}:
 * <ul>
 *     <li>One-Time Operations :
 *      <ul>
 *          <li>execution with result : {@link #call(String, ThrowableSupplier)} or {@link #execute(String, Supplier)}</li>
 *          <li>execution without result : {@link #call(String, ThrowableAction)} or {@link #execute(String, Runnable)}</li>
 *      </ul>
 *     </li>
 *     <li>Two-Phase Operations :
 *        <li>{@link #begin(String) the first phase} if {@link #isBeginSupported() supported}</li>
 *        <li>{@link #end(Resilience4jContext) the second phase} if {@link #isEndSupported() supported}</li>
 *     </li>
 * </ul>
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @since 1.0.0
 */
public interface Resilience4jOperations<E> {

    // The One-Time Operations

    /**
     * Execute the callback without result
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
     * Call the callback without result, and may throw any error
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
     * Call the callback with result, and may throw any error
     *
     * @param name           the name of the Resilience4j's entry
     * @param callback       the callback to be executed
     * @param throwableClass the sub-class of the {@link Throwable}
     * @param <TR>           the sub-class of the {@link Throwable}
     * @throws Throwable any error caused by the execution of the callback
     */
    default <TR extends Throwable> void call(String name, ThrowableAction callback, Class<TR> throwableClass) throws TR {
        try {
            call(name, callback);
        } catch (Throwable t) {
            throw wrap(t, throwableClass);
        }
    }

    /**
     * Call the callback with result, and may throw any error
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
     * Call the callback with result, and may throw any error
     *
     * @param name           the name of the Resilience4j's entry
     * @param callback       the callback to be executed
     * @param throwableClass the sub-class of the {@link Throwable}
     * @param <T>            the type of result
     * @param <TR>           the sub-class of the {@link Throwable}
     * @throws Throwable any error caused by the execution of the callback
     */
    default <T, TR extends Throwable> T call(String name, ThrowableSupplier<T> callback, Class<TR> throwableClass) throws TR {
        try {
            return call(name, callback);
        } catch (Throwable t) {
            throw wrap(t, throwableClass);
        }
    }

    // The Two-Phase Operations

    /**
     * Whether {@link #begin(String) the first phase} is supported
     *
     * @return <code>true</code> if supported, otherwise <code>false</code>
     */
    default boolean isBeginSupported() {
        return true;
    }

    /**
     * Begin the execution in the first phase.
     *
     * @param name the name of the Resilience4j's entry
     * @return {@link Resilience4jContext} with the entry and its name
     */
    Resilience4jContext<E> begin(String name);

    /**
     * Whether {@link #end(Resilience4jContext) the second phase} is supported
     *
     * @return <code>true</code> if supported, otherwise <code>false</code>
     */
    default boolean isEndSupported() {
        return true;
    }

    /**
     * End the execution in the second phase.
     *
     * @param context {@link Resilience4jContext}
     */
    void end(Resilience4jContext<E> context);
}
