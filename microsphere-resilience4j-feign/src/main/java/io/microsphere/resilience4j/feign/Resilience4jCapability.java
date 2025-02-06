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
import feign.InvocationHandlerFactory;
import io.microsphere.resilience4j.common.Resilience4jFacade;

/**
 * {@link Capability} by Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Capability
 * @since 1.0.0
 */
public class Resilience4jCapability implements Capability {

    private final Resilience4jFacade facade;

    public Resilience4jCapability(Resilience4jFacade facade) {
        this.facade = facade;
    }

    @Override
    public InvocationHandlerFactory enrich(InvocationHandlerFactory invocationHandlerFactory) {
        return new Resilience4jInvocationHandlerFactory(invocationHandlerFactory, facade);
    }
}
