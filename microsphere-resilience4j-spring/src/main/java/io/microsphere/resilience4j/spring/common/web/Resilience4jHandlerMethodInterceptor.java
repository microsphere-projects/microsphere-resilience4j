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
package io.microsphere.resilience4j.spring.common.web;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.Registry;
import io.microsphere.logging.Logger;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.spring.web.event.WebEndpointMappingsReadyEvent;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import io.microsphere.spring.web.method.support.HandlerMethodInterceptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.reflect.MethodUtils.getSignature;
import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static org.springframework.core.ResolvableType.forType;

/**
 * The abstract template class for Resilience4j's {@link HandlerMethodInterceptor}
 *
 * @param <E> the type of Resilience4j's entity, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the registry of Resilience4j's entity, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see HandlerMethodInterceptor
 * @see Resilience4jModule
 * @since 1.0.0
 */
public abstract class Resilience4jHandlerMethodInterceptor<E, C, R extends Registry<E, C>> implements HandlerMethodInterceptor,
        ApplicationListener<WebEndpointMappingsReadyEvent>, DisposableBean, Ordered {

    protected final static int ENTRY_CLASS_GENERIC_INDEX = 0;

    protected final static int CONFIGURATION_CLASS_GENERIC_INDEX = 1;

    protected final Logger logger = getLogger(getClass());

    protected final R registry;

    /**
     * Local Cache using {@link HashMap} with better performance
     */
    protected final Map<String, E> entryCaches;

    private final Class<E> entryClass;

    private final Class<C> configClass;

    private final Resilience4jModule module;

    public Resilience4jHandlerMethodInterceptor(R registry) {
        // always keep self being a delegate
        Assert.notNull(registry, "The 'registry' argument can't be null");
        this.registry = registry;
        this.entryCaches = new HashMap<>();
        ResolvableType currentType = forType(getClass());
        ResolvableType superType = currentType.as(Resilience4jHandlerMethodInterceptor.class);
        this.module = valueOf(this.registry.getClass());
        this.entryClass = (Class<E>) module.getEntryClass();
        this.configClass = (Class<C>) module.getConfigClass();
    }

    @Override
    public void beforeExecute(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request) throws Exception {
        E entry = getEntry(handlerMethod);
        beforeExecute(entry, handlerMethod, args, request);
    }

    @Override
    public void afterExecute(HandlerMethod handlerMethod, Object[] args, Object returnValue, Throwable error, NativeWebRequest request) throws Exception {
        E entry = getEntry(handlerMethod);
        afterExecute(entry, handlerMethod, args, returnValue, error, request);
    }

    protected abstract void beforeExecute(E entry, HandlerMethod handlerMethod, Object[] args, NativeWebRequest request) throws Exception;

    protected abstract void afterExecute(E entry, HandlerMethod handlerMethod, Object[] args, Object returnValue, Throwable error, NativeWebRequest request) throws Exception;

    @Override
    public void onApplicationEvent(WebEndpointMappingsReadyEvent event) {
        initEntryCache(event);
    }

    @Override
    public void destroy() throws Exception {
        this.entryCaches.clear();
    }

    /**
     * Get the Resilience4j Registry
     *
     * @return non-null
     */
    public final R getRegistry() {
        return registry;
    }

    /**
     * Get the order of current interceptor bean
     *
     * @return {@link #getModule() current module}'s {@link Resilience4jModule#getDefaultAspectOrder() aspect order} as default
     * @see <a href="https://resilience4j.readme.io/docs/getting-started-3#aspect-order">Resilience4j Aspect order</a>
     */
    public int getOrder() {
        return this.module.getDefaultAspectOrder();
    }

    protected void initEntryCache(WebEndpointMappingsReadyEvent event) {
        Collection<WebEndpointMapping> webEndpointMappings = event.getMappings();
        int size = webEndpointMappings.size();
        Map<String, E> entryCaches = new HashMap<>(size);

        Iterator<WebEndpointMapping> iterator = webEndpointMappings.iterator();
        while (iterator.hasNext()) {
            WebEndpointMapping webEndpointMapping = iterator.next();
            Object endpoint = webEndpointMapping.getEndpoint();
            if (endpoint instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) endpoint;
                String entryName = getEntryName(handlerMethod);
                E entry = createEntry(entryName);
                entryCaches.put(entryName, entry);
                logger.debug("A new entry[name : '{}' , type : '{}'] was added into cache", entryName, entry.getClass().getName());
            }
        }
        this.entryCaches.putAll(entryCaches);
    }

    protected final E getEntry(HandlerMethod handlerMethod) {
        String name = getEntryName(handlerMethod);
        return getEntry(name);
    }

    protected final E getEntry(String name) {
        Optional<E> optionalEntry = registry.find(name);
        return optionalEntry.orElseGet(() -> createEntry(name));
    }

    protected abstract E createEntry(String name);

    /**
     * Get the name of fault-tolerance entry
     *
     * @param handlerMethod Spring MVC {@link HandlerMethod Handler Method}
     * @return non-null
     */
    protected String getEntryName(HandlerMethod handlerMethod) {
        String moduleName = getModule().name();
        Method method = handlerMethod.getMethod();
        String signature = getSignature(method);
        return "spring:webmvc:" + moduleName + "@" + signature;
    }

    /**
     * Get the {@link C configuration} by the specified name
     *
     * @param configName the specified configuration name
     * @return if the {@link C configuration} can't be found by the specified configuration name,
     * {@link #getDefaultConfiguration()} will be used as default
     */
    protected C getConfiguration(String configName) {
        return registry.getConfiguration(configName).orElse(getDefaultConfiguration());
    }

    /**
     * Get the default {@link C configuration}
     *
     * @return non-null
     */
    public final C getDefaultConfiguration() {
        return registry.getDefaultConfig();
    }

    /**
     * Get the class of Resilience4j's entry
     *
     * @return non-null
     */
    public final Class<E> getEntryClass() {
        return this.entryClass;
    }

    /**
     * Get the class of Resilience4j's configuration
     *
     * @return non-null
     */
    public final Class<C> getConfigClass() {
        return this.configClass;
    }

    /**
     * Get the {@link Resilience4jModule Resilience4j's module}
     *
     * @return non-null
     */
    public final Resilience4jModule getModule() {
        return module;
    }

}
