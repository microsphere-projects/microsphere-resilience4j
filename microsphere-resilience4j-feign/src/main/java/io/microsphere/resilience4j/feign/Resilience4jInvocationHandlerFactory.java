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
import io.microsphere.resilience4j.common.Resilience4jFacade;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link InvocationHandlerFactory} for Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jInvocationHandler
 * @see InvocationHandlerFactory
 * @since 1.0.0
 */
public class Resilience4jInvocationHandlerFactory implements InvocationHandlerFactory {

    private final InvocationHandlerFactory delegate;

    private final Resilience4jFacade facade;

    public Resilience4jInvocationHandlerFactory(InvocationHandlerFactory delegate, Resilience4jFacade facade) {
        this.delegate = delegate;
        this.facade = facade;
    }

    @Override
    public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
        InvocationHandler delegate = this.delegate.create(target, dispatch);
        return new Resilience4jInvocationHandler(delegate, facade);
    }
}
