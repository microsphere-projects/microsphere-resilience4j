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

package io.microsphere.resilience4j.spring.common.event;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.springframework.util.ReflectionUtils.MethodFilter;

import java.lang.reflect.Method;

/**
 * The {@link MethodFilter} for The {@link EventPublisher}s' {@link EventConsumer}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see MethodFilter
 * @see EventConsumer
 * @see EventPublisher
 * @see Bulkhead.EventPublisher
 * @see CircuitBreaker.EventPublisher
 * @see RateLimiter.EventPublisher
 * @see Registry.EventPublisher
 * @see Retry.EventPublisher
 * @see TimeLimiter.EventPublisher
 * @see Timer.EventPublisher
 * @since 1.0.0
 */
public class EventConsumerMethodFilter implements MethodFilter {

    public static EventConsumerMethodFilter INSTANCE = new EventConsumerMethodFilter();

    @Override
    public boolean matches(Method method) {
        return method.getParameterCount() == 1 && EventConsumer.class.equals(method.getParameterTypes()[0]);
    }
}