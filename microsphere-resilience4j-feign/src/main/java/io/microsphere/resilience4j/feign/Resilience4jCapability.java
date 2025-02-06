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
import io.microsphere.resilience4j.common.Resilience4jFacade;

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
     * The default delegated client
     */
    public static final boolean DEFAULT_IS_DELEGATED_CLIENT = true;

    private final Resilience4jFacade facade;

    private final String entryNamePrefix;

    private final boolean isDelegatedClient;

    public Resilience4jCapability(Resilience4jFacade facade) {
        this(facade, DEFAULT_IS_DELEGATED_CLIENT);
    }

    public Resilience4jCapability(Resilience4jFacade facade, boolean isDelegatedClient) {
        this(facade, DEFAULT_ENTRY_NAME_PREFIX, isDelegatedClient);
    }

    public Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix) {
        this(facade, entryNamePrefix, DEFAULT_IS_DELEGATED_CLIENT);
    }

    public Resilience4jCapability(Resilience4jFacade facade, String entryNamePrefix, boolean isDelegatedClient) {
        this.facade = facade;
        this.entryNamePrefix = entryNamePrefix;
        this.isDelegatedClient = isDelegatedClient;
    }

    @Override
    public Client enrich(Client client) {
        return isDelegatedClient ? new Resilience4jClient(client, facade, entryNamePrefix) : client;
    }

    @Override
    public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory) {
        return isDelegatedClient ? invocationHandlerFactory : new Resilience4jInvocationHandlerFactory(invocationHandlerFactory, facade, entryNamePrefix);
    }

    /**
     * Whether {@link Resilience4jClient delegated client} or not.
     *
     * @return <code>true</code> indicates {@link Resilience4jClient delegated client} , otherwise
     * {@link Resilience4jInvocationHandlerFactory} will be used
     */
    public boolean isDelegatedClient() {
        return isDelegatedClient;
    }
}
