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
package io.microsphere.resilience4j.spring.circuitbreaker.annotation;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.microsphere.spring.core.convert.annotation.EnableSpringConverterAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.microsphere.resilience4j.spring.common.annotation.EnableResilience4jExtension.WebEnvironment.SPRING_WEBMVC;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * {@link EnableCircuitBreaker} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EnableCircuitBreakerTest.class})
@TestPropertySource(properties = {
        "microsphere.resilience4j.circuitbreaker.instances[test].waitDurationInOpenState=PT30S",
        "microsphere.resilience4j.circuitbreaker.instances[test].slidingWindowSize=100",
        "microsphere.resilience4j.circuitbreaker.instances[test].slowCallRateThreshold=0.7"})
@EnableCircuitBreaker(
        publishEvents = true,
        consumeEvents = true,
        webEnvironment = {SPRING_WEBMVC})
@EnableSpringConverterAdapter
public class EnableCircuitBreakerTest {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Autowired
    private CircuitBreakerConfigurationProperties properties;

    @Test
    public void test() {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("test");
        circuitBreaker.acquirePermission();
        circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(Float.valueOf(0.7f), instanceProperties.getSlowCallRateThreshold());
        assertEquals(Integer.valueOf(100), instanceProperties.getSlidingWindowSize());
        assertEquals(Duration.ofSeconds(30), instanceProperties.getWaitDurationInOpenState());
    }

    @EventListener(CircuitBreakerOnSuccessEvent.class)
    public void onCircuitBreakerOnSuccessEvent(CircuitBreakerOnSuccessEvent event) {
        assertEquals("test", event.getCircuitBreakerName());
        assertEquals(Duration.ofMillis(100), event.getElapsedDuration());
    }
}
