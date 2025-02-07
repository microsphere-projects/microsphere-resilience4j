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

import feign.Capability;
import feign.Client;
import feign.InvocationHandlerFactory;
import io.github.resilience4j.core.lang.NonNull;
import io.microsphere.resilience4j.common.Resilience4jFacade;

import static io.microsphere.resilience4j.feign.Resilience4jCapability.DecoratedPoint.CLIENT;
import static io.microsphere.resilience4j.feign.Resilience4jCapability.DecoratedPoint.INVOCATION_HANDLER_FACTORY;

/**
 * {@link Capability} by Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Resilience4jClient
 * @see Resilience4jInvocationHandlerFactory
 * @see Capability
 * @since 1.0.0
 */
public class Resilience4jCapability implements Capability {

    /**
     * The default entry name prefix
     */
    public static final String DEFAULT_ENTRY_NAME_PREFIX = "microsphere-resilience4j-feign@";

    /**
     * The default {@link DecoratedPoint decorated type}
     */
    public static final DecoratedPoint DEFAULT_DECORATED_POINT = CLIENT;

    private final Resilience4jFacade facade;

    private final String entryNamePrefix;

    private final DecoratedPoint decoratedPoint;

    public Resilience4jCapability(Resilience4jFacade facade) {
        this(facade, DEFAULT_ENTRY_NAME_PREFIX);
    }

    public Resilience4jCapability(Resilience4jFacade facade, DecoratedPoint decoratedPoint) {
        this(facade, DEFAULT_ENTRY_NAME_PREFIX, decoratedPoint);
    }

    public Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix) {
        this(facade, entryNamePrefix, DEFAULT_DECORATED_POINT);
    }

    public Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix, DecoratedPoint decoratedPoint) {
        this.facade = facade;
        this.entryNamePrefix = entryNamePrefix;
        this.decoratedPoint = decoratedPoint;
    }

    @Override
    public Client enrich(Client client) {
        return isDecoratedClient() ? new Resilience4jClient(client, facade, entryNamePrefix) : client;
    }

    @Override
    public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory) {
        return isDecoratedInvocationHandlerFactory() ? new Resilience4jInvocationHandlerFactory(invocationHandlerFactory, facade, entryNamePrefix) : invocationHandlerFactory;
    }

    /**
     * Get {@link DecoratedPoint decorated type}
     *
     * @return non-null
     */
    @NonNull
    public DecoratedPoint getDecoratedPoint() {
        return decoratedPoint;
    }

    /**
     * Whether {@link Client} will be decorated by {@link Resilience4jClient}
     *
     * @return <code>true</code> if {@link Client} will be decorated by {@link Resilience4jClient},
     * otherwise <code>false</code>
     */
    public boolean isDecoratedClient() {
        return CLIENT.equals(getDecoratedPoint());
    }

    /**
     * Whether {@link InvocationHandlerFactory} will be decorated by {@link Resilience4jInvocationHandlerFactory}
     *
     * @return <code>true</code> if {@link InvocationHandlerFactory} will be decorated by {@link Resilience4jInvocationHandlerFactory},
     * otherwise <code>false</code>
     */
    public boolean isDecoratedInvocationHandlerFactory() {
        return INVOCATION_HANDLER_FACTORY.equals(getDecoratedPoint());
    }

    /**
     * Decorated Point
     */
    public static enum DecoratedPoint {

        /**
         * {@link Client} will be decorated by {@link Resilience4jClient}
         */
        CLIENT,

        /**
         * {@link InvocationHandlerFactory} will be decorated by {@link Resilience4jInvocationHandlerFactory}
         */
        INVOCATION_HANDLER_FACTORY
    }
}
