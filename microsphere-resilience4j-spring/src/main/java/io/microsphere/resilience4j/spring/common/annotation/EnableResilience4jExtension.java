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
package io.microsphere.resilience4j.spring.common.annotation;

import io.microsphere.annotation.Nonnull;
import io.microsphere.resilience4j.spring.common.Resilience4jPlugin;
import io.microsphere.resilience4j.spring.common.event.Resilience4jEventApplicationEventPublisher;
import io.microsphere.resilience4j.spring.common.event.Resilience4jEventConsumerBeanRegistrar;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link EnableResilience4jExtension @Enable Resilience4j Extension} annotation
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableResilience4jRegistrar
 * @since 1.0.0
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Inherited
@Documented
public @interface EnableResilience4jExtension {

    /**
     * Whether to publish Resilience4j's events
     *
     * @return <code>true</code> as default
     * @see Resilience4jEventApplicationEventPublisher
     */
    boolean publishEvents() default false;

    /**
     * Whether to consume Resilience4j's events
     *
     * @return <code>true</code> as default
     * @see Resilience4jEventConsumerBeanRegistrar
     */
    boolean consumeEvents() default false;

    /**
     * The {@link Resilience4jPlugin#getName() names} of the {@link Resilience4jPlugin Resilience4j plugins}
     *
     * @return the {@link Resilience4jPlugin#getName() names} of the {@link Resilience4jPlugin Resilience4j plugins}
     * @see Resilience4jPlugin
     */
    @Nonnull
    String[] plugins() default {};
}
