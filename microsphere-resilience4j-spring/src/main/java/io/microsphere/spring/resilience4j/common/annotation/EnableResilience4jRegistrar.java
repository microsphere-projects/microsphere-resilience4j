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
package io.microsphere.spring.resilience4j.common.annotation;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.microsphere.spring.context.annotation.BeanCapableImportCandidate;
import io.microsphere.spring.core.annotation.ResolvablePlaceholderAnnotationAttributes;
import io.microsphere.spring.resilience4j.circuitbreaker.annotation.EnableCircuitBreaker;
import io.microsphere.spring.resilience4j.common.event.Resilience4jEventApplicationEventPublisher;
import io.microsphere.spring.resilience4j.common.event.Resilience4jEventConsumerBeanRegistrar;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.microsphere.spring.core.annotation.ResolvablePlaceholderAnnotationAttributes.of;
import static io.microsphere.spring.util.BeanRegistrar.registerBeanDefinition;
import static java.util.Collections.unmodifiableMap;
import static org.springframework.core.io.support.SpringFactoriesLoader.loadFactoryNames;
import static org.springframework.util.ClassUtils.resolveClassName;

/**
 * The abstract {@link ImportBeanDefinitionRegistrar} class to {@link EnableResilience4jExtension enable
 * Resilience4j's features extension}
 *
 * @param <A>  the type of Enable Annotation , e.g : {@link EnableCircuitBreaker}
 * @param <E>  the type of Resilience4j entry, e.g : {@link CircuitBreaker}
 * @param <EC> the type of Resilience4j entry's configuration, e.g : {@link CircuitBreakerConfiguration}
 * @param <ET> the type o Resilience4j entry's event : {@link CircuitBreakerEvent}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableResilience4jExtension
 * @see EnableCircuitBreaker
 * @since 1.0.0
 */
public abstract class EnableResilience4jRegistrar<A extends Annotation, E, EC> extends BeanCapableImportCandidate
        implements ImportBeanDefinitionRegistrar {

    private static final Map<String, Class<?>> attributedEventComponentClassesMap;

    static {
        Map<String, Class<?>> classesMap = new HashMap<>(2);
        classesMap.put("publishEvents", Resilience4jEventApplicationEventPublisher.class);
        classesMap.put("consumeEvents", Resilience4jEventConsumerBeanRegistrar.class);
        attributedEventComponentClassesMap = unmodifiableMap(classesMap);
    }

    private final ResolvableType superType;

    private final Class<A> annotationType;

    private final Class<E> entryType;

    private final Class<EC> entryConfigurationType;

    protected EnableResilience4jRegistrar() {
        this.superType = resolveSuperType();
        this.annotationType = resolveGeneric(0);
        this.entryType = resolveGeneric(1);
        this.entryConfigurationType = resolveGeneric(2);
    }

    /**
     * {@inheritDoc}
     */
    public final void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        String annotationClassName = annotationType.getName();
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotationClassName);
        ResolvablePlaceholderAnnotationAttributes attributes = of(annotationAttributes, annotationType, getEnvironment());

        // Register Entry Configuration Bean
        registerEntryConfiguration(registry);

        // Register Event Component Beans
        registerEventComponentBeans(attributes, registry);

        // Register Web Environment Component Beans
        registerWebEnvironmentComponentBeans(attributes, registry);
    }

    protected final Class<EC> getEntryConfigurationType() {
        return this.entryConfigurationType;
    }

    private void registerEntryConfiguration(BeanDefinitionRegistry registry) {
        registerBeanDefinition(registry, getEntryConfigurationType());
    }

    private void registerEventComponentBeans(ResolvablePlaceholderAnnotationAttributes attributes, BeanDefinitionRegistry registry) {
        for (Map.Entry<String, Class<?>> entry : attributedEventComponentClassesMap.entrySet()) {
            String attributeName = entry.getKey();
            boolean supported = attributes.getBoolean(attributeName);
            if (supported) {
                Class<?> eventComponentClass = entry.getValue();
                registerComponentBean(eventComponentClass, registry);
            }
        }
    }

    private void registerBean(ResolvablePlaceholderAnnotationAttributes attributes,
                              String attributeName,
                              Class<?> beanType,
                              BeanDefinitionRegistry registry) {
        boolean supported = attributes.getBoolean(attributeName);
        if (supported) {
            registerBeanDefinition(registry, beanType);
        }
    }

    private void registerWebEnvironmentComponentBeans(ResolvablePlaceholderAnnotationAttributes attributes,
                                                      BeanDefinitionRegistry registry) {
        ClassLoader classLoader = this.getClassLoader();
        EnableResilience4jExtension.WebEnvironment[] webEnvironmentArray = (EnableResilience4jExtension.WebEnvironment[]) attributes.get("webEnvironment");
        for (EnableResilience4jExtension.WebEnvironment webEnvironment : webEnvironmentArray) {
            if (webEnvironment.supports()) {
                Class<?> webComponentClass = webEnvironment.getComponentClass();
                registerComponentBean(webComponentClass, registry);
            }
        }
    }

    private void registerComponentBean(Class<?> componentClass, BeanDefinitionRegistry registry) {
        ClassLoader classLoader = this.getClassLoader();
        List<String> actualComponentClassNames = loadFactoryNames(componentClass, classLoader);
        for (String actualComponentClassName : actualComponentClassNames) {
            Class<?> actualComponentClass = resolveClassName(actualComponentClassName, classLoader);
            Class<E> entryTypeFromComponent = resolveEntryType(actualComponentClass, componentClass);
            if (Objects.equals(entryTypeFromComponent, this.entryType)) {
                registerBeanDefinition(registry, actualComponentClass);
            }
        }
    }


    private ResolvableType resolveSuperType() {
        return resolveSuperType(this.getClass(), EnableResilience4jRegistrar.class);
    }

    private <T> Class<T> resolveGeneric(int index) {
        return (Class<T>) this.superType.resolveGeneric(index);
    }

    private static ResolvableType resolveSuperType(Class<?> targetClass, Class<?> superClass) {
        ResolvableType type = ResolvableType.forType(targetClass);
        return type.as(superClass);
    }

    private static <E> Class<E> resolveEntryType(Class<?> targetClass, Class<?> superClass) {
        ResolvableType superType = resolveSuperType(targetClass, superClass);
        return (Class<E>) superType.resolveGeneric(0);
    }

}
