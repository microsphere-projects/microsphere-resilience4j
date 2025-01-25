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
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.microsphere.spring.web.event.WebEndpointMappingsReadyEvent;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import io.microsphere.spring.web.method.support.HandlerMethodInterceptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.reflect.MethodUtils.getSignature;
import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;

/**
 * The abstract template class for Resilience4j's {@link HandlerMethodInterceptor}
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's entry configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the type of Resilience4j's entry registry, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see HandlerMethodInterceptor
 * @see Resilience4jModule
 * @since 1.0.0
 */
public abstract class Resilience4jHandlerMethodInterceptor<E, C, R extends Registry<E, C>> implements HandlerMethodInterceptor,
        ApplicationListener<WebEndpointMappingsReadyEvent>, DisposableBean, Ordered {

    private static final String RESILIENCE4J_CONTEXT_ATTRIBUTE_NAME = Resilience4jContext.class.getName();

    protected final Logger logger = getLogger(getClass());

    protected final Resilience4jTemplate<E, C, R> template;

    public Resilience4jHandlerMethodInterceptor(R registry) {
        // always keep self being a delegate
        Assert.notNull(registry, "The 'registry' argument can't be null");
        this.template = createTemplate(registry);
    }

    protected abstract Resilience4jTemplate<E, C, R> createTemplate(R registry);

    @Override
    public void beforeExecute(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request) throws Exception {
        Resilience4jContext<E> context = template.begin(() -> getEntryName(handlerMethod));
        request.setAttribute(RESILIENCE4J_CONTEXT_ATTRIBUTE_NAME, context, SCOPE_REQUEST);
    }

    @Override
    public void afterExecute(HandlerMethod handlerMethod, Object[] args, Object returnValue, Throwable error, NativeWebRequest request) throws Exception {
        Resilience4jContext<E> context = (Resilience4jContext<E>) request.getAttribute(RESILIENCE4J_CONTEXT_ATTRIBUTE_NAME, SCOPE_REQUEST);
        this.template.end(context);
    }

    @Override
    public void onApplicationEvent(WebEndpointMappingsReadyEvent event) {
        initEntryCache(event);
    }

    @Override
    public void destroy() throws Exception {
        this.template.destroy();
    }

    /**
     * Get the {@link Resilience4jTemplate}
     *
     * @return non-null
     */
    public final <T extends Resilience4jTemplate<E, C, R>> T getTemplate() {
        return (T) this.template;
    }

    /**
     * Get the order of current interceptor bean
     *
     * @return {@link Resilience4jTemplate current module}'s {@link Resilience4jModule#getDefaultAspectOrder() aspect order} as default
     * @see <a href="https://resilience4j.readme.io/docs/getting-started-3#aspect-order">Resilience4j Aspect order</a>
     */
    public int getOrder() {
        return this.template.getModule().getDefaultAspectOrder();
    }

    protected void initEntryCache(WebEndpointMappingsReadyEvent event) {
        Collection<WebEndpointMapping> webEndpointMappings = event.getMappings();
        int size = webEndpointMappings.size();

        List<String> entryNames = new ArrayList<>(size);

        Iterator<WebEndpointMapping> iterator = webEndpointMappings.iterator();
        while (iterator.hasNext()) {
            WebEndpointMapping webEndpointMapping = iterator.next();
            Object endpoint = webEndpointMapping.getEndpoint();
            if (endpoint instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) endpoint;
                String entryName = getEntryName(handlerMethod);
                entryNames.add(entryName);
            }
        }
        this.template.initLocalEntriesCache(entryNames);
    }

    /**
     * Get the name of fault-tolerance entry
     *
     * @param handlerMethod Spring MVC {@link HandlerMethod Handler Method}
     * @return non-null
     */
    public String getEntryName(HandlerMethod handlerMethod) {
        String moduleName = this.template.getModule().name();
        Method method = handlerMethod.getMethod();
        String signature = getSignature(method);
        return "spring:webmvc:" + moduleName + "@" + signature;
    }
}
