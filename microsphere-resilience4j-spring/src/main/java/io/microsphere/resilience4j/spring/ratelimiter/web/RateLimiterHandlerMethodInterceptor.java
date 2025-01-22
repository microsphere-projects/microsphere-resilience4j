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
package io.microsphere.resilience4j.spring.ratelimiter.web;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.microsphere.resilience4j.spring.common.web.Resilience4jHandlerMethodInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@link HandlerInterceptor} based on Resilience4j {@link RateLimiter}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see HandlerInterceptor
 * @see RateLimiter
 * @since 1.0.0
 */
public class RateLimiterHandlerMethodInterceptor extends Resilience4jHandlerMethodInterceptor<RateLimiter, RateLimiterConfig, RateLimiterRegistry> {

    public RateLimiterHandlerMethodInterceptor(RateLimiterRegistry registry) {
        super(registry);
    }

    @Override
    protected void beforeExecute(RateLimiter rateLimiter) {
        rateLimiter.acquirePermission();
    }

    @Override
    protected void afterExecute(RateLimiter rateLimiter, Object result, Throwable failure) {
        if (failure == null) {
            rateLimiter.onResult(args);
        } else {
            rateLimiter.onError(failure);
        }
    }

    @Override
    protected RateLimiter createEntry(String name) {
        RateLimiterRegistry registry = super.getRegistry();
        return registry.rateLimiter(name, super.getConfiguration(name), registry.getTags());
    }
}
