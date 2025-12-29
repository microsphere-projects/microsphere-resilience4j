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
import io.microsphere.lang.function.ThrowableAction;
import io.microsphere.lang.function.ThrowableSupplier;
import io.microsphere.logging.Logger;
import io.microsphere.util.ValueHolder;
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
import static java.lang.System.nanoTime;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    protected final static String TEST_DATA = "test-data";

    protected final static Runnable NO_ACTION = () -> {
    };

    protected final static ThrowableAction NO_THROWABLE_ACTION = () -> {
    };

    protected final static ThrowableAction NPE_ACTION = () -> {
        throw new NullPointerException();
    };

    protected final static Supplier<String> TEST_DATA_SUPPLIER = () -> TEST_DATA;

    protected final static ThrowableSupplier<String> NO_THROWABLE_TEST_DATA_SUPPLIER = () -> TEST_DATA;

    protected final static ThrowableSupplier<String> NPE_SUPPLIER = () -> {
        throw new NullPointerException();
    };

    protected final static Class<NullPointerException> NULL_POINTER_EXCEPTION_CLASS = NullPointerException.class;

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
        logger.trace("Resolved the entryClass : '{}', entryConfigClass : '{}', registryClass : '{}', templateClass : '{}'",
                entryClass.getName(), configClass.getName(), registryClass.getName(), templateClass.getName());
    }

    @BeforeEach
    void init() throws Throwable {
        this.registry = createRegistry();
        this.entryConfig = createEntryConfig();
        this.template = createTemplate(registry);
        this.template.addConfiguration(this.entryName, entryConfig);
        logger.trace("The instance of Registry(class : '{}') was created.", this.registry.getClass().getName());
        logger.trace("The instance of Resilience4jTemplate(class : '{}') was created.", this.template.getClass().getName());
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
    void testExecuteWithRunnable() {
        this.template.execute(this.entryName, NO_ACTION);
    }

    @Test
    void testExecuteWithSupplier() {
        assertSame(TEST_DATA, this.template.execute(this.entryName, TEST_DATA_SUPPLIER));
    }

    @Test
    void testCallWithThrowableAction() throws Throwable {
        this.template.call(this.entryName, NO_THROWABLE_ACTION);
    }

    @Test
    void testCallWithThrowableActionAndThrowableClass() {
        this.template.call(this.entryName, NO_THROWABLE_ACTION, NULL_POINTER_EXCEPTION_CLASS);
        assertThrows(NULL_POINTER_EXCEPTION_CLASS, () -> this.template.call(this.entryName, NPE_ACTION, NULL_POINTER_EXCEPTION_CLASS));
    }

    @Test
    void testCallWithThrowableSupplier() throws Throwable {
        assertSame(TEST_DATA, this.template.call(this.entryName, NO_THROWABLE_TEST_DATA_SUPPLIER));
    }

    @Test
    void testCallWithThrowableSupplierAndThrowableClass() {
        assertSame(TEST_DATA, this.template.call(this.entryName, NO_THROWABLE_TEST_DATA_SUPPLIER, NULL_POINTER_EXCEPTION_CLASS));
        assertThrows(NULL_POINTER_EXCEPTION_CLASS, () -> this.template.call(this.entryName, NPE_SUPPLIER, NULL_POINTER_EXCEPTION_CLASS));
    }

    @Test
    void testIsBeginSupported() {
        Resilience4jModule module = template.getModule();
        switch (module) {
            case RETRY:
            case TIME_LIMITER:
                assertFalse(this.template.isBeginSupported());
                break;
            default:
                assertTrue(this.template.isBeginSupported());
        }
    }

    @Test
    void testBegin() {
        RT template = this.template;
        String entryName = this.entryName;
        Resilience4jModule module = template.getModule();
        ValueHolder<Object> resultHolder = new ValueHolder<>();
        try {
            Resilience4jContext<E> context = template.begin(entryName);
            resultHolder.setValue(context.getEntry());
        } catch (Throwable e) {
            resultHolder.setValue(e);
        }

        switch (module) {
            case RETRY:
                ;
            case TIME_LIMITER:
                assertTrue(resultHolder.getValue() instanceof Throwable);
                break;
            default:
                assertTrue(template.getEntryClass().isInstance(resultHolder.getValue()));
        }
    }

    @Test
    void testIsEndSupported() {
        assertEquals(this.template.isBeginSupported(), this.template.isEndSupported());
    }

    @Test
    void testEnd() {
        String entryName = this.entryName;
        RT template = this.template;
        Resilience4jModule module = template.getModule();
        ValueHolder<Object> resultHolder = new ValueHolder<>();

        try {
            Resilience4jContext<E> context = new Resilience4jContext<>(entryName, template.getEntry(entryName));
            context.setStartTime(nanoTime());
            template.end(context);
            resultHolder.setValue(context.getEntry());
        } catch (Throwable e) {
            resultHolder.setValue(e);
        }

        switch (module) {
            case RETRY:
                ;
            case TIME_LIMITER:
                assertTrue(resultHolder.getValue() instanceof Throwable);
                break;
            default:
                assertTrue(template.getEntryClass().isInstance(resultHolder.getValue()));
        }
    }

    @Test
    void testReadMethods() {
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
    void testLocalEntriesCache() {
        String entryName = "test-1";
        if (this.template.isLocalEntriesCachePresent()) {
            this.template.initLocalEntriesCache(asList(entryName));
            E entry = this.template.getEntryFromCache(entryName);
            assertNotNull(entry);
        }
    }

    @Test
    void testEntriesAndEvents() {
        RT template = this.template;
        String entryName = this.entryName;

        // TEST ADDED
        template.onEntryAddedEvent(event -> {
            logger.trace("The event of registry : '{}' was received.", event);
            assertNotNull(event.getAddedEntry());
            assertEquals(ADDED, event.getEventType());
        });

        template.initLocalEntriesCache(entryName);
        E entry = template.getEntry(entryName);
        assertNotNull(entry);
        E foundEntry = template.getEntry(entryName);
        assertSame(entry, foundEntry);

        // Test REPLACED
        template.onEntryReplacedEvent(event -> {
            logger.trace("The event of registry : '{}' was received.", event);
            assertSame(entry, event.getOldEntry());
            assertNotSame(entry, event.getNewEntry());
            assertEquals(REPLACED, event.getEventType());
        });
        String newEntryName = "test-entry-2";
        E newEntry = template.createEntry(newEntryName);
        E oldEntry = template.replaceEntry(entryName, newEntry);
        assertNotNull(newEntry);
        assertNotNull(oldEntry);

        String newEntryName2 = "test-entry-3";

        oldEntry = template.replaceEntry(newEntryName2, newEntry);
        assertNull(oldEntry);

        // Test REMOVED
        template.onEntryRemovedEvent(event -> {
            logger.trace("The event of registry : '{}' was received.", event);
            assertEquals(newEntry, event.getRemovedEntry());
            assertEquals(REMOVED, event.getEventType());
        });
        E removedEntry = template.removeEntry(newEntryName);
        assertNotNull(removedEntry);

        removedEntry = template.removeEntry(newEntryName2);
        assertNull(removedEntry);
    }

    @Test
    void testExecuteWithConsumer() {
        assertSame(this.template, this.template.execute(this.entryName, entry -> {
            logger.trace("{}.execute('{}', entry : {})", this.getClass().getName(), entryName, entry);
        }));
    }

    @Test
    void testExecuteWithFunction() {
        E entry = this.template.execute(this.entryName, e -> e);
        assertSame(entry, this.template.getEntry(this.entryName));
    }

    @Test
    void testCallWithThrowableConsumer() throws Throwable {
        assertSame(this.template, this.template.call(this.entryName, entry -> {
            logger.trace("{}.execute('{}', entry : {})", this.getClass().getName(), entryName, entry);
        }));
    }

    @Test
    void testCallWithThrowableConsumerAndThrowableClass() {
        assertSame(this.template, this.template.call(this.entryName, entry -> {
            logger.trace("{}.execute('{}', entry : {})", this.getClass().getName(), entryName, entry);
        }, NULL_POINTER_EXCEPTION_CLASS));

        assertThrows(NULL_POINTER_EXCEPTION_CLASS, () -> this.template.call(this.entryName, entry -> {
            String name = null;
            name.toUpperCase();
        }, NULL_POINTER_EXCEPTION_CLASS));
    }

    @Test
    void testCallWithThrowableFunction() throws Throwable {
        assertSame(this.template.getEntry(this.entryName), this.template.call(this.entryName, e -> e));
    }

    @Test
    void testCallWithThrowableFunctionAndThrowableClass() {
        assertSame(this.template.getEntry(this.entryName), this.template.call(this.entryName, e -> e, NULL_POINTER_EXCEPTION_CLASS));

        assertThrows(NULL_POINTER_EXCEPTION_CLASS, () -> this.template.call(this.entryName, entry -> {
            String name = null;
            return name.toUpperCase();
        }, NULL_POINTER_EXCEPTION_CLASS));
    }

    @Test
    void testPriority() {
        RT template = this.template;
        assertEquals(template.getPriority(), template.getModule().getDefaultAspectOrder());

        int priority = 1;
        assertEquals(priority, template.setPriority(priority).getPriority());
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
        logger.trace("the event of {}({}) was received.", entryClass.getSimpleName(), event);
    }
}