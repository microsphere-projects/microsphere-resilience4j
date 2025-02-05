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
package io.microsphere.resilience4j.feign;

import feign.InvocationHandlerFactory;
import feign.Target;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.Registry;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

import static io.microsphere.reflect.MethodUtils.getSignature;

/**
 * Resilience4j {@link InvocationHandler} class
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's entry configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the type of Resilience4j's entry registry, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see io.github.resilience4j.feign.DecoratorInvocationHandler
 * @since 1.0.0
 */
public class Resilience4jInvocationHandler<E, C, R extends Registry<E, C>> implements InvocationHandler {

    private final Target<?> target;

    private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

    private final Resilience4jTemplate<E, C, R> template;

    private final Resilience4jModule module;

    public Resilience4jInvocationHandler(Target<?> target,
                                         Map<Method, InvocationHandlerFactory.MethodHandler> dispatch,
                                         Resilience4jTemplate<E, C, R> template) {
        this.target = target;
        this.dispatch = dispatch;
        this.template = template;
        this.module = template.getModule();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "equals":
                return equals(args.length > 0 ? args[0] : null);

            case "hashCode":
                return hashCode();

            case "toString":
                return toString();

            default:
                break;
        }

        return this.template.call(getEntryName(method), () -> dispatch.get(method).invoke(args));
    }

    protected String getEntryName(Method method) {
        return "feign:" + module.name() + "@" + getSignature(method);
    }
}
