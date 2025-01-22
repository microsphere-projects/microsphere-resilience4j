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
package io.microsphere.resilience4j.spring.retry.jdbc.druid;

import com.alibaba.druid.filter.Filter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.spring.common.jdbc.druid.Resilience4jDruidFilter;


/**
 * {@link Retry} x Druid {@link Filter}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class RetryDruidFilter extends Resilience4jDruidFilter<Retry, RetryConfig, RetryRegistry> {

    private static ThreadLocal<Retry.Context<Object>> contextThreadLocal = new ThreadLocal<>();

    public RetryDruidFilter(RetryRegistry registry) {
        super(registry);
    }

    @Override
    protected Retry createEntry(String name) {
        RetryRegistry registry = super.getRegistry();
        return registry.retry(name, super.getConfiguration(name), registry.getTags());
    }

    @Override
    protected void beforeExecute(Retry retry) {
        Retry.Context<Object> context = retry.context();
        contextThreadLocal.set(context);
    }

    @Override
    protected void afterExecute(Retry retry, long duration, Object result, Throwable failure) {
        try {
            Retry.Context<Object> context = contextThreadLocal.get();
            if (failure == null) {
                if (result != null) {
                    // Success with result
                    context.onResult(result);
                } else {
                    // Success without result
                    context.onSuccess();
                }
            } else {
                // On error
                if (failure instanceof RuntimeException) {
                    context.onRuntimeError((RuntimeException) failure);
                } else if (failure instanceof Exception) {
                    context.onError((Exception) failure);
                } else {
                    context.onRuntimeError(new RuntimeException(failure));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            contextThreadLocal.remove();
        }

    }
}

