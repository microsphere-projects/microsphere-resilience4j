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

/**
 * {@link HandlerInterceptor} based on Resilience4j {@link CircuitBreaker}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see HandlerInterceptor
 * @see CircuitBreaker
 * @since 1.0.0
 */
public class CircuitBreakerHandlerMethodInterceptor extends Resilience4jHandlerMethodInterceptor<CircuitBreaker, CircuitBreakerConfig, CircuitBreakerRegistry> {

    public CircuitBreakerHandlerMethodInterceptor(CircuitBreakerRegistry registry) {
        super(registry);
    }

}
