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

import io.microsphere.lang.function.ThrowableSupplier;

import java.util.List;

/**
 * Resilience4j {@link ThrowableSupplier callback} Chain
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see ThrowableSupplier
 * @since 1.0.0
 */
class CallbackChain<T> implements ThrowableSupplier<T> {

    private final String entryName;

    private final ThrowableSupplier<T> delegate;

    private final List<Resilience4jTemplate> templates;

    private final int size;

    private int pos; // position

    public CallbackChain(String entryName, ThrowableSupplier<T> delegate, List<Resilience4jTemplate> templates) {
        this.entryName = entryName;
        this.delegate = delegate;
        this.templates = templates;
        this.size = templates.size();
        this.pos = 0;
    }

    @Override
    public T get() throws Throwable {
        if (pos < size) {
            Resilience4jTemplate template = templates.get(pos++);
            return (T) template.call(this.entryName, CallbackChain.this::get);
        }
        return delegate.get();
    }
}
