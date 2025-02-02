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
package io.microsphere.resilience4j.retry;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.vavr.CheckedFunction0;

/**
 * {@link Resilience4jTemplate} for {@link Retry}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Retry
 * @see RetryConfig
 * @see RetryRegistry
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public class RetryTemplate extends Resilience4jTemplate<Retry, RetryConfig, RetryRegistry> {

    public RetryTemplate(RetryRegistry registry) {
        super(registry);
    }

    @Override
    protected Retry createEntry(String name) {
        RetryRegistry registry = super.getRegistry();
        return Retry.of(name, super.getConfiguration(name), registry.getTags());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <V> V execute(Resilience4jContext<Retry> context, CheckedFunction0<V> callback) throws Throwable {
        Retry retry = context.getEntry();
        Retry.Context<V> ctx = retry.context();
        do {
            try {
                V result = callback.apply();
                final boolean validationOfResult = ctx.onResult(result);
                if (!validationOfResult) {
                    ctx.onComplete();
                    return result;
                }
            } catch (Exception exception) {
                ctx.onError(exception);
            }
        } while (true);
    }

}
