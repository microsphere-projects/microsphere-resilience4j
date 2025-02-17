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
package io.microsphere.resilience4j.util;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.EventProcessor;
import io.github.resilience4j.core.EventPublisher;
import io.github.resilience4j.core.Registry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.microsphere.util.BaseUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static io.microsphere.collection.CollectionUtils.size;
import static io.microsphere.io.IOUtils.close;
import static io.microsphere.lang.Prioritized.COMPARATOR;
import static io.microsphere.reflect.MethodUtils.findMethod;
import static io.microsphere.reflect.MethodUtils.invokeMethod;
import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static io.microsphere.text.FormatUtils.format;
import static io.microsphere.util.ClassLoaderUtils.getClassLoader;
import static io.microsphere.util.ClassLoaderUtils.getResource;
import static io.microsphere.util.ClassLoaderUtils.loadClass;
import static io.microsphere.util.ClassUtils.newInstance;
import static java.beans.Introspector.decapitalize;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

/**
 * The utility class for Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see BaseUtils
 * @since 1.0.0
 */
public abstract class Resilience4jUtils extends BaseUtils {

    /**
     * The resource-path for default templates
     */
    private static final String DEFAULT_TEMPLATES_RESOURCE_PATH = "META-INF/default/templates.properties";

    private static final Map<Resilience4jModule, Method> getEntryMethodsCache;

    private static final Map<Resilience4jModule, Class<? extends Resilience4jTemplate>> defaultTemplates = loadDefaultTemplates();

    static {
        Resilience4jModule[] modules = Resilience4jModule.values();
        Map<Resilience4jModule, Method> methodsCache = new HashMap<>(modules.length);
        for (Resilience4jModule module : modules) {
            initGetEntryMethodsCache(module, methodsCache);
        }
        getEntryMethodsCache = unmodifiableMap(methodsCache);
    }

    private static void initGetEntryMethodsCache(Resilience4jModule module, Map<Resilience4jModule, Method> methodsCache) {
        Class<?> entryClass = module.getEntryClass();
        Class<?> configClass = module.getConfigClass();
        Class<?> registryClass = module.getRegistryClass();
        String methodName = decapitalize(entryClass.getSimpleName());
        Method method = findMethod(registryClass, methodName, String.class, configClass);
        methodsCache.put(module, method);
    }

    public static <E, C> E getEntry(Registry<E, C> registry, String name) {
        return getEntry(registry, name, registry.getDefaultConfig());
    }

    public static <E, C> E getEntry(Registry<E, C> registry, String name, C configuration) {
        Resilience4jModule module = valueOf(registry.getClass());
        Method method = getEntryMethodsCache.get(module);
        try {
            return invokeMethod(registry, method, name, configuration);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static <E> EventProcessor<E> getEventProcessor(E entry) {
        if (entry instanceof CircuitBreaker) {
            return getEventProcessor((CircuitBreaker) entry);
        } else if (entry instanceof Bulkhead) {
            return getEventProcessor((Bulkhead) entry);
        } else if (entry instanceof RateLimiter) {
            return getEventProcessor((RateLimiter) entry);
        } else if (entry instanceof TimeLimiter) {
            return getEventProcessor((TimeLimiter) entry);
        } else if (entry instanceof Retry) {
            return getEventProcessor((Retry) entry);
        }
        String errorMessage = format("The entry only supports these modules :  {}", Arrays.toString(Resilience4jModule.values()));
        throw new UnsupportedOperationException(errorMessage);
    }

    public static <E> EventProcessor<E> getEventProcessor(Retry retry) {
        return asEventProcessor(retry.getEventPublisher());
    }

    public static <E> EventProcessor<E> getEventProcessor(TimeLimiter timeLimiter) {
        return asEventProcessor(timeLimiter.getEventPublisher());
    }

    public static <E> EventProcessor<E> getEventProcessor(RateLimiter rateLimiter) {
        return asEventProcessor(rateLimiter.getEventPublisher());
    }

    public static <E> EventProcessor<E> getEventProcessor(Bulkhead bulkhead) {
        return asEventProcessor(bulkhead.getEventPublisher());
    }

    public static <E> EventProcessor<E> getEventProcessor(CircuitBreaker circuitBreaker) {
        return asEventProcessor(circuitBreaker.getEventPublisher());
    }

    public static <T> EventProcessor<T> getEventProcessor(Registry registry) {
        Registry.EventPublisher eventPublisher = registry.getEventPublisher();
        return asEventProcessor(eventPublisher);
    }

    private static EventProcessor asEventProcessor(EventPublisher eventPublisher) {
        if (eventPublisher instanceof EventProcessor) {
            return (EventProcessor) eventPublisher;
        }
        String errorMessage = format("The eventPublisher should be an instance of EventProcessor, actual : {}", eventPublisher.getClass());
        throw new UnsupportedOperationException(errorMessage);
    }

    /**
     * Create an instance of {@link Resilience4jTemplate} from the specified {@link Registry}
     *
     * @param registry {@link Registry}
     * @param <E>      the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
     * @param <C>      the type of Resilience4j's entry configuration, e.g., {@link CircuitBreakerConfig}
     * @param <R>      the type of Resilience4j's entry registry, e.g., {@link CircuitBreakerRegistry}
     * @param <T>      the sub-type of {@link Resilience4jTemplate}
     * @return an instance of {@link Resilience4jTemplate}
     */
    public static <E, C, R extends Registry<E, C>, T extends Resilience4jTemplate<E, C, R>> T createTemplate(R registry) {
        Resilience4jModule module = valueOf(registry.getClass());
        Class<? extends Resilience4jTemplate> templateClass = defaultTemplates.get(module);
        return (T) newInstance(templateClass, registry);
    }

    /**
     * Create a {@link List} of {@link Resilience4jTemplate} from the specified {@link Registry registries}
     *
     * @param registries {@link Registry registries}
     * @return non-null read-only {@link List}
     */
    public static List<Resilience4jTemplate> createTemplates(Collection<Registry<?, ?>> registries) {
        int size = size(registries);
        if (size < 1) {
            return emptyList();
        }
        Map<Registry<?, ?>, Resilience4jTemplate> templatesMap = new HashMap<>(size);
        for (Registry<?, ?> registry : registries) {
            templatesMap.computeIfAbsent(registry, Resilience4jUtils::createTemplate);
        }
        List<Resilience4jTemplate> templates = new ArrayList<>(templatesMap.size());
        templates.addAll(templatesMap.values());
        // Sort
        templates.sort(COMPARATOR);
        return unmodifiableList(templates);
    }

    static Map<Resilience4jModule, Class<? extends Resilience4jTemplate>> loadDefaultTemplates() {
        Resilience4jModule[] modules = Resilience4jModule.values();
        int size = modules.length;
        Map<Resilience4jModule, Class<? extends Resilience4jTemplate>> defaultTemplates = new HashMap<>(size);

        ClassLoader classLoader = getClassLoader(Resilience4jTemplate.class);
        Properties properties = loadDefaultTemplatesProperties(classLoader);
        new EnumMap<>(Resilience4jModule.class);
        for (Resilience4jModule module : Resilience4jModule.values()) {
            String moduleName = module.getName();
            String templateClassName = properties.getProperty(moduleName);
            Class<? extends Resilience4jTemplate> templateClass =
                    (Class<? extends Resilience4jTemplate>) loadClass(templateClassName, classLoader);
            defaultTemplates.put(module, templateClass);
        }

        return defaultTemplates;
    }

    private static Properties loadDefaultTemplatesProperties(ClassLoader classLoader) {
        Properties properties = new Properties();
        URL url = getResource(classLoader, DEFAULT_TEMPLATES_RESOURCE_PATH);
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            close(inputStream);
        }
        return properties;
    }
}
