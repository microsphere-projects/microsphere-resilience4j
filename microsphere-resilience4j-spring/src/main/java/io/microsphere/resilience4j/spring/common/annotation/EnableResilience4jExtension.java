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

import io.microsphere.resilience4j.spring.common.event.Resilience4jEventApplicationEventPublisher;
import io.microsphere.resilience4j.spring.common.event.Resilience4jEventConsumerBeanRegistrar;
import io.microsphere.resilience4j.spring.common.web.Resilience4jHandlerMethodInterceptor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static io.microsphere.util.ClassLoaderUtils.resolveClass;

/**
 * {@link EnableResilience4jExtension @Enable Resilience4j Extension} annotation
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableResilience4jRegistrar
 * @since 1.0.0
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
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
     * Which Web Environment to be supported
     *
     * @return empty as default
     */
    WebEnvironment[] webEnvironment() default {};

    /**
     * The Data Access Environment
     *
     * @return empty as default
     */
    DataAccessEnvironment[] dataAccessEnvironment() default {};

    /**
     * The RPC Environment
     *
     * @return empty as default
     */
    RPCEnvironment[] rpcEnvironment() default {};

    enum WebEnvironment {

        /**
         * Servlet Environment
         */
        SERVLET("javax.servlet.Servlet"),

        /**
         * Spring WebMVC Environment
         */
        SPRING_WEBMVC("org.springframework.web.servlet.DispatcherServlet",
                Resilience4jHandlerMethodInterceptor.class),

        /**
         * Spring WebFlux Environment
         */
        SPRING_WEBFLUX("org.springframework.web.reactive.DispatcherHandler"),

        ;

        private final String candidateClassName;

        private final Class<?> componentClass;

        private final boolean supported;

        // TODO Remove this Constructor
        WebEnvironment(String candidateClassName) {
            this(candidateClassName, null);
        }

        WebEnvironment(String candidateClassName, Class<?> componentClass) {
            this.candidateClassName = candidateClassName;
            this.supported = resolveClass(candidateClassName) != null;
            this.componentClass = componentClass;
        }

        /**
         * Whether supports in the runtime
         *
         * @return <code>true</code> if supported, <code>false</code> otherwise
         */
        public boolean supports() {
            return supported;
        }

        /**
         * Get the candidate class name
         *
         * @return non-null
         */
        public String getCandidateClassName() {
            return candidateClassName;
        }

        /**
         * Get the component class
         *
         * @return non-null
         */
        public Class<?> getComponentClass() {
            return componentClass;
        }
    }

    /**
     * The Data Access Environment
     */
    enum DataAccessEnvironment {

        JDBC,

        JPA,

        HIBERNATE,

        MYBATIS,

        REDIS,

        ;

    }

    enum RPCEnvironment {


        DUBBO,

        FEIGN,

        ;
    }
}
