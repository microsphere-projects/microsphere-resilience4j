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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static io.github.resilience4j.core.registry.RegistryEvent.Type.ADDED;
import static io.github.resilience4j.core.registry.RegistryEvent.Type.REMOVED;
import static io.github.resilience4j.core.registry.RegistryEvent.Type.REPLACED;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.reflect.MethodUtils.invokeStaticMethod;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    protected C entryConfig;

    protected RT template;

    protected String entryName = "test-entry";

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
    public final void init() throws Throwable {
        this.registry = createRegistry();
        this.entryConfig = createEntryConfig();
        this.template = createTemplate(registry);
        this.template.configuration(this.entryName, entryConfig);
        logger.debug("The instance of Registry(class : '{}') was created.", this.registry.getClass().getName());
        logger.debug("The instance of Resilience4jTemplate(class : '{}') was created.", this.template.getClass().getName());
        postInit();
    }

    /**
     * Create an instance of {@link C Entry Configure} for testing
     *
     * @return non-null
     */
    protected C createEntryConfig() {
        return registry.getDefaultConfig();
    }

    /**
     * Post-initialization callback method
     */
    protected void postInit() {
        // DO NOTHING, The subclass can override it
    }

    protected Supplier<String> getEntryNameGenerator() {
        return () -> entryName;
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
        Class<E> entryClass = this.entryClass;
        RT template = this.template;
        R registry = this.registry;
        Class<C> configClass = this.configClass;

        assertNotNull(template.getModule());

        assertEquals(entryClass, template.getEntryClass());
        assertEquals(configClass, template.getConfigClass());
        assertTrue(registryClass.isAssignableFrom(template.getRegistry().getClass()));
        assertEquals(templateClass, template.getClass());

        assertEquals(registry, template.getRegistry());
        assertEquals(registry.getDefaultConfig(), template.getDefaultConfig());
        String configName = "default";
        assertEquals(registry.getConfiguration(configName).get(), template.getConfiguration(configName));
        assertEquals(registry.getDefaultConfig(), template.getConfiguration("not-exists"));

    }

    @Test
    public final void testLocalEntriesCache() {
        String entryName = "test-1";
        this.template.initLocalEntriesCache(asList(entryName));
        E entry = this.template.getEntryFromCache(entryName);
        assertNotNull(entry);
    }

    @Test
    public final void testEntriesAndEvents() {
        RT template = this.template;
        String entryName = this.entryName;

        // TEST ADDED
        template.onEntryAddedEvent(event -> {
            logger.debug("The event of registry : '{}' was received.", event);
            assertNotNull(event.getAddedEntry());
            assertEquals(ADDED, event.getEventType());
        });
        E entry = template.createEntry(entryName);
        assertNotNull(entry);
        E foundEntry = template.getEntry(entryName);
        assertSame(entry, foundEntry);

        // Test REPLACED
        template.onEntryReplacedEvent(event -> {
            logger.debug("The event of registry : '{}' was received.", event);
            assertSame(entry, event.getOldEntry());
            assertNotSame(entry, event.getNewEntry());
            assertEquals(REPLACED, event.getEventType());
        });
        String newEntryName = "test-entry-2";
        E newEntry = template.createEntry(newEntryName);
        E oldEntry = template.replaceEntry(entryName, newEntry);
        assertNotNull(newEntry);
        assertNotNull(oldEntry);

        // Test REMOVED
        template.onEntryRemovedEvent(event -> {
            logger.debug("The event of registry : '{}' was received.", event);
            assertEquals(newEntry, event.getRemovedEntry());
            assertEquals(REMOVED, event.getEventType());
        });
        E removedEntry = template.removeEntry(newEntryName);
        assertNotNull(removedEntry);

    }

    @AfterEach
    public void destroy() {
        preDestroy();
        this.template.destroy();
    }

    /**
     * Pre-destroy callback method
     */
    protected void preDestroy() {
        // DO NOTHING, The subclass can override it
    }

    protected void await(Duration waitDuration, Runnable runnable) {
        this.await(waitDuration.toMillis(), runnable);
    }

    protected void await(long waitTimeInMillis, Runnable runnable) {
        await(waitTimeInMillis);
        runnable.run();
    }

    protected void await(long waitTimeInMillis) {
        try {
            TimeUnit.MILLISECONDS.sleep(waitTimeInMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    protected void logEvent(Object event) {
        logger.debug("the event of {}({}) was received.", entryClass.getSimpleName(), event);
    }
}
