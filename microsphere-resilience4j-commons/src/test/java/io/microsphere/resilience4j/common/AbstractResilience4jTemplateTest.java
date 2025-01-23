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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.Registry;
import io.microsphere.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.reflect.MethodUtils.invokeStaticMethod;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Abstract test class for {@link Resilience4jTemplate}
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's entry configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the type of Resilience4j's entry registry, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jTemplate
 * @since 1.0.0
 */
public abstract class AbstractResilience4jTemplateTest<E, C, R extends Registry<E, C>, RT extends Resilience4jTemplate<E, C, R>> {

    protected final Logger logger = getLogger(getClass());

    protected final Class<E> entryClass;

    protected final Class<C> configClass;

    protected final Class<R> registryClass;

    protected final Class<RT> templateClass;

    protected R registry;

    protected RT template;

    public AbstractResilience4jTemplateTest() {
        ParameterizedType superType = (ParameterizedType) this.getClass().getGenericSuperclass();
        Type[] actualTypeArguments = superType.getActualTypeArguments();
        entryClass = (Class<E>) actualTypeArguments[0];
        configClass = (Class<C>) actualTypeArguments[1];
        registryClass = (Class<R>) actualTypeArguments[2];
        templateClass = (Class<RT>) actualTypeArguments[3];
        logger.debug("Resolved the entryClass : '{}', entryConfigClass : '{}', registryClass : '{}', templateClass : '{}'",
                entryClass.getName(), configClass.getName(), registryClass.getName(), templateClass.getName());
    }

    @BeforeEach
    public void init() throws Throwable {
        this.registry = createRegistry();
        this.template = createTemplate(registry);
        logger.debug("The instance of Registry(class : '{}') was created.", this.registry.getClass().getName());
        logger.debug("The instance of Resilience4jTemplate(class : '{}') was created.", this.template.getClass().getName());
    }

    protected RT createTemplate(R registry) throws Throwable {
        Constructor constructor = templateClass.getConstructor(registryClass);
        return (RT) constructor.newInstance(registry);
    }

    /**
     * Create an instance of {@link Registry}
     *
     * @return non-null
     */
    protected R createRegistry() {
        return invokeStaticMethod(registryClass, "ofDefaults");
    }

    @Test
    public final void testReadMethods() {

        assertNotNull(this.template.getModule());

        assertEquals(this.entryClass, this.template.getEntryClass());
        assertEquals(this.configClass, this.template.getConfigClass());
        assertTrue(this.registryClass.isAssignableFrom(this.template.getRegistry().getClass()));
        assertEquals(this.templateClass, this.template.getClass());

        assertEquals(this.registry, this.template.getRegistry());
        assertEquals(this.registry.getDefaultConfig(), this.template.getDefaultConfig());
        String configName = "default";
        assertEquals(this.registry.getConfiguration(configName).get(), this.template.getConfiguration(configName));
        assertEquals(this.registry.getDefaultConfig(), this.template.getConfiguration("not-exists"));

    }

    @Test
    public final void testLocalEntriesCache() {
        String entryName = "test-1";
        this.template.initLocalEntriesCache(asList(entryName));
        E entry = this.template.getEntryFromCache(entryName);
        assertNotNull(entry);
    }
}
