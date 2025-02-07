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
package io.microsphere.resilience4j.feign;

import io.github.resilience4j.core.lang.NonNull;
import io.microsphere.resilience4j.common.Resilience4jFacade;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static io.microsphere.reflect.MethodUtils.getSignature;
import static java.util.Objects.requireNonNull;

/**
 * Resilience4j {@link InvocationHandler} class
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see io.github.resilience4j.feign.DecoratorInvocationHandler
 * @since 1.0.0
 */
public class Resilience4jInvocationHandler implements InvocationHandler {

    private final InvocationHandler delegate;

    private final Resilience4jFacade facade;

    private final String entryNamePrefix;

    public Resilience4jInvocationHandler(InvocationHandler delegate,
                                         Resilience4jFacade facade, String entryNamePrefix) {
        this.delegate = requireNonNull(delegate, "The 'delegate' argument must not be null!");
        this.facade = requireNonNull(facade, "The 'facade' argument must not be null!");
        this.entryNamePrefix = requireNonNull(entryNamePrefix, "The 'entryNamePrefix' argument must not be null!");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String entryName = buildEntryName(method);
        return this.facade.call(entryName, () -> delegate.invoke(proxy, method, args));
    }

    /**
     * Get the delegate {@link InvocationHandler}
     *
     * @return non-null
     */
    @NonNull
    public InvocationHandler getDelegate() {
        return delegate;
    }

    /**
     * Get the {@link Resilience4jFacade}
     *
     * @return non-null
     */
    @NonNull
    public Resilience4jFacade getFacade() {
        return facade;
    }

    private String buildEntryName(Method method) {
        return entryNamePrefix + getSignature(method);
    }
}
