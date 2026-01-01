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

import io.microsphere.resilience4j.spring.common.annotation.EnableResilience4jExtension;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * The plug-in SPI of Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableResilience4jExtension
 * @since 1.0.0
 */
public interface Resilience4jPlugin {

    /**
     * Plugin into {@link ConfigurableListableBeanFactory Spring BeanFactory}
     *
     * @param beanFactory {@link ConfigurableListableBeanFactory}
     * @param registry    {@link BeanDefinitionRegistry}
     * @param environment {@link ConfigurableEnvironment}
     */
    void plugin(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry, ConfigurableEnvironment environment);

    /**
     * Get the name of this plug-in
     *
     * @return non-null
     */
    String getName();
}