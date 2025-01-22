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
package io.microsphere.resilience4j.spring.bulkhead.web;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.spring.common.web.Resilience4jHandlerMethodInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link HandlerInterceptor} based on Resilience4j {@link Bulkhead}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see HandlerInterceptor
 * @see Bulkhead
 * @since 1.0.0
 */
public class BulkheadHandlerMethodInterceptor extends Resilience4jHandlerMethodInterceptor<Bulkhead, BulkheadConfig, BulkheadRegistry> {

    public BulkheadHandlerMethodInterceptor(BulkheadRegistry registry) {
        super(registry);
    }

    @Override
    protected void beforeExecute(Bulkhead entry) {
        entry.acquirePermission();
    }

    @Override
    protected void afterExecute(Bulkhead entry, Object result, Throwable failure) {
        entry.onComplete();
    }

    @Override
    protected Bulkhead createEntry(String name) {
        BulkheadRegistry registry = super.getRegistry();
        return registry.bulkhead(name, super.getConfiguration(name), registry.getTags());
    }

}
