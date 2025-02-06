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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Delegating {@link InvocationHandler}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see InvocationHandler
 * @since 1.0.0
 */
public class DelegatingInvocationHandler implements InvocationHandler {

    private static final ThreadLocal<Object[]> argumentsHolder = new ThreadLocal<Object[]>();

    private final InvocationHandler delegate;

    public DelegatingInvocationHandler(InvocationHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        argumentsHolder.set(args);
        return delegate.invoke(proxy, method, args);
    }

    public static Object[] getArguments() {
        return argumentsHolder.get();
    }

    public InvocationHandler getDelegate() {
        return delegate;
    }
}
