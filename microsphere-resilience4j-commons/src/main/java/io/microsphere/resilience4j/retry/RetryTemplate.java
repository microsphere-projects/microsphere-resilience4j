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

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

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
@Deprecated
public class RetryTemplate extends Resilience4jTemplate<Retry, RetryConfig, RetryRegistry> {

    public static final String RETRY_CONTEXT_ATTRIBUTE_NAME = "Retry.Context";

    public RetryTemplate(RetryRegistry registry) {
        super(registry);
    }

    /**
     * Create the {@link Retry}
     *
     * @param name the name of the Resilience4j's entry
     * @return non-null
     */
    @Override
    protected Retry createEntry(String name) {
        RetryRegistry registry = super.getRegistry();
        return registry.retry(name, super.getConfiguration(name), registry.getTags());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void beforeExecute(Resilience4jContext<Retry> context) {
        Retry retry = context.getEntry();
        Retry.Context retryContext = retry.context();
        context.setAttribute(RETRY_CONTEXT_ATTRIBUTE_NAME, retryContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterExecute(Resilience4jContext<Retry> context) {
        Retry.Context retryContext = context.getAttribute(RETRY_CONTEXT_ATTRIBUTE_NAME);
        Throwable failure = context.getFailure();
        if (failure == null) {
            Object result = context.getResult();
            if (result == null) {
                retryContext.onSuccess();
            } else {
                retryContext.onResult(result);
            }
        } else {
            if (failure instanceof RuntimeException) {
                retryContext.onRuntimeError((RuntimeException) failure);
            } else if (failure instanceof Exception) {
                try {
                    retryContext.onError((Exception) failure);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
