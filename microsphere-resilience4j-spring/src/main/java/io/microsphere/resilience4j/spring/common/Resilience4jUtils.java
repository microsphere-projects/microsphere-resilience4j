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

package io.microsphere.resilience4j.spring.common;

import io.github.resilience4j.fallback.configure.FallbackConfiguration;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import static io.microsphere.spring.beans.factory.support.BeanRegistrar.registerBeanDefinition;
import static io.microsphere.util.ClassLoaderUtils.resolveClass;

/**
 * The utilities class for Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration
 * @since 1.0.0
 */
public abstract class Resilience4jUtils {

    /**
     * The {@link Class} name of {@link io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration} since
     * Resilience4j 1.5
     *
     * @see io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration
     * @since Resilience4j 1.5
     */
    public static final String SPEL_RESOLVER_CONFIGURATION_CLASS_NAME = "io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration";

    /**
     * Register the common configurations.
     * Note :
     * Resilience4j Spring 2.0+ Entry Configuration class imports FallbackConfiguration and
     * SpelResolverConfiguration default
     *
     * @param registry {@link BeanDefinitionRegistry}
     */
    public static void registerCommonConfigurations(BeanDefinitionRegistry registry) {
        registerFallbackConfiguration(registry);
        registerSpelResolverConfiguration(registry);
    }

    /**
     * Register {@link FallbackConfiguration}
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @see FallbackConfiguration
     */
    public static void registerFallbackConfiguration(BeanDefinitionRegistry registry) {
        registerBeanDefinition(registry, FallbackConfiguration.class);
    }

    /**
     * Register {@link io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration} if present in class-path.
     *
     * @param registry {@link BeanDefinitionRegistry}
     * @see io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration
     */
    public static void registerSpelResolverConfiguration(BeanDefinitionRegistry registry) {
        Class<?> spelResolverConfigurationClass = resolveClass(SPEL_RESOLVER_CONFIGURATION_CLASS_NAME);
        if (spelResolverConfigurationClass != null) {
            registerBeanDefinition(registry, spelResolverConfigurationClass);
        }
    }

    private Resilience4jUtils() {
    }
}