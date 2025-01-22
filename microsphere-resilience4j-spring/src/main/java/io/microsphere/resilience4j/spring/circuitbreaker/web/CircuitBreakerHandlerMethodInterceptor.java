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
package io.microsphere.resilience4j.spring.circuitbreaker.web;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.spring.common.web.Resilience4jHandlerMethodInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * {@link HandlerInterceptor} based on Resilience4j {@link CircuitBreaker}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see HandlerInterceptor
 * @see CircuitBreaker
 * @since 1.0.0
 */
public class CircuitBreakerHandlerMethodInterceptor extends Resilience4jHandlerMethodInterceptor<CircuitBreaker, CircuitBreakerConfig, CircuitBreakerRegistry> {

    private static final String START_TIME_ATTRIBUTE_NAME = "microsphere.resilience4j.circuitBreaker.startTime";

    public CircuitBreakerHandlerMethodInterceptor(CircuitBreakerRegistry registry) {
        super(registry);
    }

    @Override
    protected void beforeExecute(CircuitBreaker circuitBreaker) {
        circuitBreaker.acquirePermission();
        long startTime = circuitBreaker.getCurrentTimestamp();
        request.setAttribute(START_TIME_ATTRIBUTE_NAME, startTime, SCOPE_REQUEST);
    }

    @Override
    protected void afterExecute(CircuitBreaker circuitBreaker, Object result, Throwable failure) {
        long starTime = (long) request.getAttribute(START_TIME_ATTRIBUTE_NAME, SCOPE_REQUEST);
        long duration = circuitBreaker.getCurrentTimestamp() - starTime;
        if (failure == null) {
            circuitBreaker.onResult(duration, TimeUnit.NANOSECONDS, args);
        } else {
            circuitBreaker.onError(duration, TimeUnit.NANOSECONDS, failure);
        }
    }

    @Override
    protected CircuitBreaker createEntry(String name) {
        CircuitBreakerRegistry registry = super.getRegistry();
        return registry.circuitBreaker(name, super.getConfiguration(name), registry.getTags());
    }

}
