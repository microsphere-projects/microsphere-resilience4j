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
package io.microsphere.resilience4j.spring.circuitbreaker.annotation;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.microsphere.resilience4j.spring.common.Resilience4jPlugin;
import io.microsphere.resilience4j.spring.common.annotation.EnableResilience4jExtension;
import io.microsphere.spring.beans.factory.annotation.EnableConfigurationBeanBinding;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static io.microsphere.resilience4j.common.Resilience4jConstants.CIRCUIT_BREAKER_PREFIX;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enable Resilience4j {@link CircuitBreaker}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@Target(TYPE)
@Retention(RUNTIME)
@Documented
@Inherited
@Import(EnableCircuitBreakerRegistrar.class)
@EnableResilience4jExtension
@EnableConfigurationBeanBinding(prefix = CIRCUIT_BREAKER_PREFIX, type = CircuitBreakerConfigurationProperties.class)
public @interface EnableCircuitBreaker {

    /**
     * Whether to publish Resilience4j's events
     *
     * @return <code>true</code> as default
     */
    @AliasFor(annotation = EnableResilience4jExtension.class, attribute = "publishEvents")
    boolean publishEvents() default false;

    /**
     * Whether to consume Resilience4j's events
     *
     * @return <code>true</code> as default
     */
    @AliasFor(annotation = EnableResilience4jExtension.class, attribute = "consumeEvents")
    boolean consumeEvents() default false;

    /**
     * The Spring Bean names of the {@link Resilience4jPlugin Resilience4j plugins}
     *
     * @return the Spring Bean names of the Resilience4j plugins
     * @see Resilience4jPlugin
     */
    @AliasFor(annotation = EnableResilience4jExtension.class, attribute = "plugins")
    String[] plugins() default {};
}
