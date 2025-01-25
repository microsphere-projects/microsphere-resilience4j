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
package io.microsphere.resilience4j.spring.circuitbreaker.web;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.SUCCESS;
import static io.microsphere.reflect.MethodUtils.findMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link CircuitBreakerHandlerMethodInterceptor} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class CircuitBreakerHandlerMethodInterceptorTest {

    private static final CircuitBreakerRegistry registry = ofDefaults();

    private CircuitBreakerHandlerMethodInterceptor interceptor;

    private CircuitBreakerTemplate template;

    @BeforeEach
    public void init() {
        this.interceptor = new CircuitBreakerHandlerMethodInterceptor(registry);
        this.template = this.interceptor.getTemplate();
    }

    @Test
    public void testProperties() {
        assertNotNull(interceptor.getTemplate());
        assertEquals(interceptor.getTemplate().getModule().getDefaultAspectOrder(),
                interceptor.getOrder());
    }

    @Test
    public void testExecute() throws Exception {
        Method method = findMethod(CircuitBreakerHandlerMethodInterceptorTest.class, "testExecute");
        HandlerMethod handlerMethod = new HandlerMethod(this, method);
        Object[] args = null;
        MockHttpServletRequest request = new MockHttpServletRequest();
        NativeWebRequest nativeWebRequest = new ServletWebRequest(request);
        Object returnValue = null;
        Throwable error = null;

        String entryName = interceptor.getEntryName(handlerMethod);

        template.onSuccessEvent(entryName, event -> {
            assertSame(SUCCESS, event.getEventType());
        });

        interceptor.beforeExecute(handlerMethod, args, nativeWebRequest);
        interceptor.afterExecute(handlerMethod, args, returnValue, error, nativeWebRequest);

    }
}
