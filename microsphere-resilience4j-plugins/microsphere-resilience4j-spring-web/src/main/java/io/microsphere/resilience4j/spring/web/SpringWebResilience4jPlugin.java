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

package io.microsphere.resilience4j.spring.web;

import io.microsphere.resilience4j.spring.common.Resilience4jPlugin;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.ConfigurableEnvironment;

import static io.microsphere.resilience4j.spring.web.Resilience4jHandlerMethodInterceptor.BEAN_NAME;
import static io.microsphere.spring.beans.factory.support.BeanRegistrar.registerBeanDefinition;

/**
 * {@link Resilience4jPlugin} for Spring Web
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Resilience4jPlugin
 * @see Resilience4jHandlerMethodInterceptor
 * @since 1.0.0
 */
public class SpringWebResilience4jPlugin implements Resilience4jPlugin {

    /**
     * The plugin name of {@link SpringWebResilience4jPlugin}
     */
    public static final String PLUGIN_NAME = "spring-web";

    @Override
    public void plugin(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry, ConfigurableEnvironment environment) {
        registerResilience4jHandlerMethodInterceptor(registry);
    }

    private void registerResilience4jHandlerMethodInterceptor(BeanDefinitionRegistry registry) {
        registerBeanDefinition(registry, BEAN_NAME, Resilience4jHandlerMethodInterceptor.class);
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }
}