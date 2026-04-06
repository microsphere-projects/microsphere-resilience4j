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

import io.microsphere.annotation.Nonnull;
import io.microsphere.logging.Logger;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.microsphere.resilience4j.spring.common.Resilience4jPlugin;
import io.microsphere.spring.web.event.WebEndpointMappingsReadyEvent;
import io.microsphere.spring.web.metadata.WebEndpointMapping;
import io.microsphere.spring.web.method.support.HandlerMethodInterceptor;
import io.microsphere.spring.webmvc.annotation.EnableWebMvcExtension;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static io.microsphere.collection.MapUtils.newFixedHashMap;
import static io.microsphere.lang.Wrapper.tryUnwrap;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.resilience4j.spring.web.SpringWebResilience4jPlugin.PLUGIN_NAME;
import static io.microsphere.spring.web.util.WebScope.REQUEST;
import static io.microsphere.util.Assert.assertNotNull;

/**
 * The abstract template class for Resilience4j's {@link HandlerMethodInterceptor}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Resilience4jPlugin
 * @see HandlerMethodInterceptor
 * @see EnableWebMvcExtension
 * @since 1.0.0
 */
public class Resilience4jHandlerMethodInterceptor implements HandlerMethodInterceptor,
        ApplicationListener<WebEndpointMappingsReadyEvent>, Ordered {

    public static final String BEAN_NAME = "resilience4jHandlerMethodInterceptor";

    private static final String RESILIENCE4J_CONTEXT_ATTRIBUTE_NAME = Resilience4jContext.class.getName();

    private static final Logger logger = getLogger(Resilience4jHandlerMethodInterceptor.class);

    private final Resilience4jFacade resilience4jFacade;

    private int order;

    private Map<Method, String> methodEntryNamesCache;

    public Resilience4jHandlerMethodInterceptor(Resilience4jFacade resilience4jFacade) {
        // always keep self being a delegate
        assertNotNull(resilience4jFacade, () -> "The 'resilience4jFacade' argument can't be null");
        this.resilience4jFacade = resilience4jFacade;
    }

    @Override
    public void beforeExecute(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request) {
        Resilience4jContext<Resilience4jContext[]> context = resilience4jFacade.begin(getEntryName(handlerMethod));
        REQUEST.setAttribute(request, RESILIENCE4J_CONTEXT_ATTRIBUTE_NAME, context);
    }

    @Override
    public void afterExecute(HandlerMethod handlerMethod, Object[] args, Object returnValue, Throwable error, NativeWebRequest request) {
        Resilience4jContext<Resilience4jContext[]> context = (Resilience4jContext<Resilience4jContext[]>) REQUEST.removeAttribute(request, RESILIENCE4J_CONTEXT_ATTRIBUTE_NAME);
        if (context == null) {
            logger.trace("The 'context' is null , please check whether the 'resilience4jFacade' is a 'ChainableResilience4jFacade'", error);
            return;
        }
        context.setResult(returnValue)
                .setFailure(error);
        this.resilience4jFacade.end(context);
    }

    @Override
    public void onApplicationEvent(WebEndpointMappingsReadyEvent event) {
        initEntryCache(event);
    }

    /**
     * Get the {@link Resilience4jTemplate}
     *
     * @return non-null
     */
    @Nonnull
    public final Resilience4jFacade getResilience4jFacade() {
        return this.resilience4jFacade;
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    protected void initEntryCache(WebEndpointMappingsReadyEvent event) {
        Collection<WebEndpointMapping> webEndpointMappings = event.getMappings();
        int size = webEndpointMappings.size();


        Map<Method, String> methodEntryNamesCache = newFixedHashMap(size);
        this.methodEntryNamesCache = methodEntryNamesCache;

        Iterator<WebEndpointMapping> iterator = webEndpointMappings.iterator();
        while (iterator.hasNext()) {
            WebEndpointMapping webEndpointMapping = iterator.next();
            Object endpoint = webEndpointMapping.getEndpoint();
            if (endpoint instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) endpoint;
                String entryName = getEntryName(handlerMethod);
                logger.trace("Create the entryName : '{}' for HandlerMethod : {}", entryName, handlerMethod);
            }
        }

        ChainableResilience4jFacade facade = tryUnwrap(this.resilience4jFacade, ChainableResilience4jFacade.class);
        if (facade != null) {
            List<Resilience4jTemplate> templates = facade.getTemplates();
            for (Resilience4jTemplate template : templates) {
                template.initLocalEntriesCache(methodEntryNamesCache.values());
            }
        }
    }

    /**
     * Get the name of fault-tolerance entry
     *
     * @param handlerMethod Spring MVC {@link HandlerMethod Handler Method}
     * @return non-null
     */
    public String getEntryName(HandlerMethod handlerMethod) {
        Method method = handlerMethod.getMethod();
        return this.methodEntryNamesCache.computeIfAbsent(method, m -> PLUGIN_NAME + "@" + handlerMethod);
    }
}