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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.CommonProperties;
import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.common.ratelimiter.configuration.CommonRateLimiterConfigurationProperties;
import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.CommonTimeLimiterConfigurationProperties;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;

import static io.microsphere.resilience4j.common.Resilience4jConstants.BULKHEAD_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.CIRCUIT_BREAKER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RATE_LIMITER_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.RETRY_MODULE_NAME;
import static io.microsphere.resilience4j.common.Resilience4jConstants.TIME_LIMITER_MODULE_NAME;
import static io.microsphere.util.ClassUtils.isAssignableFrom;

/**
 * The Resilience4j Module enumeration
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public enum Resilience4jModule {

    /**
     * {@link Retry} module
     */
    RETRY(RETRY_MODULE_NAME, Retry.class, RetryConfig.class, CommonRetryConfigurationProperties.class, RetryEvent.class, RetryRegistry.class, 0),

    /**
     * {@link CircuitBreaker} module
     */
    CIRCUIT_BREAKER(CIRCUIT_BREAKER_MODULE_NAME, CircuitBreaker.class, CircuitBreakerConfig.class, CommonCircuitBreakerConfigurationProperties.class, CircuitBreakerEvent.class, CircuitBreakerRegistry.class, 1),

    /**
     * {@link RateLimiter} Module
     */
    RATE_LIMITER(RATE_LIMITER_MODULE_NAME, RateLimiter.class, RateLimiterConfig.class, CommonRateLimiterConfigurationProperties.class, RateLimiterEvent.class, RateLimiterRegistry.class, 2),

    /**
     * {@link TimeLimiter} module
     */
    TIME_LIMITER(TIME_LIMITER_MODULE_NAME, TimeLimiter.class, TimeLimiterConfig.class, CommonTimeLimiterConfigurationProperties.class, TimeLimiterEvent.class, TimeLimiterRegistry.class, 3),

    /**
     * {@link Bulkhead} module
     */
    BULKHEAD(BULKHEAD_MODULE_NAME, Bulkhead.class, BulkheadConfig.class, CommonBulkheadConfigurationProperties.class, BulkheadEvent.class, BulkheadRegistry.class, 4);

    private final String name;

    private final Class<?> entryClass;

    private final Class<?> configClass;

    private final Class<? extends CommonProperties> configurationPropertiesClass;

    private final Class<?> eventClass;

    private final Class<? extends Registry> registryClass;

    /**
     * @see <a href="https://resilience4j.readme.io/docs/getting-started-3#aspect-order">Resilience4j Aspect order</a>
     */
    private final int defaultAspectOrder;

    Resilience4jModule(String name, Class<?> entryClass, Class<?> configClass,
                       Class<? extends CommonProperties> configurationPropertiesClass,
                       Class<?> eventClass, Class<? extends Registry> registryClass, int defaultAspectOrder) {
        this.name = name;
        this.entryClass = entryClass;
        this.registryClass = registryClass;
        this.configClass = configClass;
        this.configurationPropertiesClass = configurationPropertiesClass;
        this.eventClass = eventClass;
        this.defaultAspectOrder = defaultAspectOrder;
    }

    /**
     * Get the name of Resilience4j's module
     *
     * @return non-null
     */
    public String getName() {
        return name;
    }

    /**
     * Get the class of Resilience4j's entry
     *
     * @return non-null
     */
    public Class<?> getEntryClass() {
        return entryClass;
    }

    /**
     * Get the class of Resilience4j's config
     *
     * @return non-null
     */
    public Class<?> getConfigClass() {
        return configClass;
    }

    /**
     * Get the class of Resilience4j's config properties
     *
     * @return non-null
     */
    public Class<? extends CommonProperties> getConfigurationPropertiesClass() {
        return configurationPropertiesClass;
    }

    /**
     * Get the class of Resilience4j's event
     *
     * @return non-null
     */
    public Class<?> getEventClass() {
        return eventClass;
    }

    /**
     * Get the class of Resilience4j's {@link Registry registry}
     *
     * @return non-null
     */
    public Class<? extends Registry> getRegistryClass() {
        return registryClass;
    }

    /**
     * Get the default order of Resilience4j's entry as an aspect
     *
     * @return non-null
     * @see <a href="https://resilience4j.readme.io/docs/getting-started-3#aspect-order">Resilience4j Aspect order</a>
     */
    public int getDefaultAspectOrder() {
        return defaultAspectOrder;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Resilience4jModule{");
        sb.append("name = ").append(name);
        sb.append(", entryClass = ").append(entryClass);
        sb.append(", configClass = ").append(configClass);
        sb.append(", configurationPropertiesClass = ").append(configurationPropertiesClass);
        sb.append(", eventClass = ").append(eventClass);
        sb.append(", registryClass = ").append(registryClass);
        sb.append(", defaultAspectOrder = ").append(defaultAspectOrder);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Search the {@link Resilience4jModule} by the specified type
     *
     * @param type the type to search, may be one of the following:
     *             <ul>
     *                 <li>{@link #getEntryClass() the entry class}</li>
     *                 <li>{@link #getConfigClass() the configuration class}</li>
     *                 <li>{@link #getConfigurationPropertiesClass() the configuration properties class}</li>
     *                 <li>{@link #getEventClass() the event class}</li>
     *                 <li>{@link #getRegistryClass() the entry registry class}</li>
     *             </ul>
     * @return the {@link Resilience4jModule} member if found
     */
    public static Resilience4jModule valueOf(Class<?> type) {
        Resilience4jModule module = null;
        for (Resilience4jModule m : values()) {
            if (isAssignableFrom(m.getEntryClass(), type)
                    || isAssignableFrom(m.getConfigClass(), type)
                    || isAssignableFrom(m.getConfigurationPropertiesClass(), type)
                    || isAssignableFrom(m.getEventClass(), type)
                    || isAssignableFrom(m.getRegistryClass(), type)
            ) {
                module = m;
                break;
            }
        }
        if (module == null) {
            throw new IllegalArgumentException("The 'type' can't be found in Resilience4jModule : " + type.getName());
        }
        return module;
    }

}
