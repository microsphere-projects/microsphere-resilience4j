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
package io.microsphere.resilience4j.spring.bulkhead.annotation;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.microsphere.resilience4j.spring.common.annotation.EnableResilience4jExtension;
import io.microsphere.spring.beans.factory.annotation.EnableConfigurationBeanBinding;
import io.microsphere.spring.beans.factory.annotation.EnableConfigurationBeanBindings;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Resilience4j {@link Bulkhead}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(EnableBulkheadRegistrar.class)
@EnableResilience4jExtension
@EnableConfigurationBeanBindings({
        @EnableConfigurationBeanBinding(prefix = "microsphere.resilience4j.bulkhead", type = BulkheadConfigurationProperties.class),
        @EnableConfigurationBeanBinding(prefix = "microsphere.resilience4j.thread-pool-bulkhead", type = ThreadPoolBulkheadConfigurationProperties.class)
})
public @interface EnableBulkhead {

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
     * Which Web Environment to be supported
     *
     * @return empty as default
     */
    @AliasFor(annotation = EnableResilience4jExtension.class, attribute = "webEnvironment")
    EnableResilience4jExtension.WebEnvironment[] webEnvironment() default {};

    /**
     * The Data Access Environment
     *
     * @return empty as default
     */
    @AliasFor(annotation = EnableResilience4jExtension.class, attribute = "dataAccessEnvironment")
    EnableResilience4jExtension.DataAccessEnvironment[] dataAccessEnvironment() default {};

    /**
     * The RPC Environment
     *
     * @return empty as default
     */
    @AliasFor(annotation = EnableResilience4jExtension.class, attribute = "rpcEnvironment")
    EnableResilience4jExtension.RPCEnvironment[] rpcEnvironment() default {};
}
