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

import feign.Feign;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.DelegatingResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.feign.api.User;
import io.microsphere.resilience4j.feign.api.UserService;
import io.microsphere.resilience4j.feign.server.SimpleUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Resilience4jCapability} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jCapability
 * @since 1.0.0
 */
public abstract class Resilience4jCapabilityTest {

    private final boolean isDelegatedClient;

    private Resilience4jFacade facade;

    private Resilience4jCapability capability;

    private Feign feign;

    private UserService userService;

    public Resilience4jCapabilityTest(boolean isDelegatedClient) {
        this.isDelegatedClient = isDelegatedClient;
    }

    @BeforeEach
    public void init() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        this.facade = new DelegatingResilience4jFacade(bulkheadRegistry, rateLimiterRegistry,
                retryRegistry, timeLimiterRegistry, circuitBreakerRegistry);

        Encoder encoder = new GsonEncoder();
        Decoder decoder = new GsonDecoder();

        this.capability = new Resilience4jCapability(facade, isDelegatedClient);

        this.feign = Feign.builder()
                .client(new DelegatingClient<>(encoder, new SimpleUserService()))
                .decoder(decoder)
                .encoder(encoder)
                .addCapability(new DelegatingCapability())
                .addCapability(this.capability)
                .build();

        this.userService = feign.newInstance(new Target.HardCodedTarget<>(UserService.class, "http://localhost:8080"));
    }

    @Test
    public void testProxy() {
        InvocationHandler handler = getInvocationHandler(this.userService);

        final InvocationHandler delegate;

        assertEquals(this.isDelegatedClient, this.capability.isDelegatedClient());

        if (this.capability.isDelegatedClient()) {
            delegate = handler;
        } else {
            assertTrue(handler instanceof Resilience4jInvocationHandler);

            Resilience4jInvocationHandler resilience4jInvocationHandler = (Resilience4jInvocationHandler) handler;
            delegate = resilience4jInvocationHandler.getDelegate();
            assertTrue(delegate instanceof DelegatingInvocationHandler);
            assertEquals(this.facade, resilience4jInvocationHandler.getFacade());
        }

        DelegatingInvocationHandler delegateInvocationHandler = (DelegatingInvocationHandler) delegate;
        assertEquals("feign.ReflectiveFeign$FeignInvocationHandler", delegateInvocationHandler.getDelegate().getClass().getName());

    }

    @Test
    public void testCreateUser() {
        String userName = "test-user";
        User user = this.userService.createUser(userName);
        assertEquals(userName, user.getName());
    }
}
