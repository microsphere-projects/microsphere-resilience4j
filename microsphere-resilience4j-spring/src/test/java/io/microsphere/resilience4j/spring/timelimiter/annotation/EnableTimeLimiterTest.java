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
package io.microsphere.resilience4j.spring.timelimiter.annotation;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.configure.TimeLimiterConfigurationProperties;
import io.github.resilience4j.timelimiter.event.TimeLimiterOnSuccessEvent;
import io.microsphere.spring.core.convert.annotation.EnableSpringConverterAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static java.lang.Boolean.TRUE;
import static java.lang.Integer.valueOf;
import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link EnableTimeLimiter} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@SpringJUnitConfig(classes = EnableTimeLimiterTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.time-limiter.instances[test].timeoutDuration=PT10S",
        "microsphere.resilience4j.time-limiter.instances[test].cancelRunningFuture=true",
        "microsphere.resilience4j.time-limiter.instances[test].eventConsumerBufferSize=200"
})
@EnableTimeLimiter(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
public class EnableTimeLimiterTest {

    @Autowired
    private TimeLimiterRegistry registry;

    @Autowired
    private TimeLimiterConfigurationProperties properties;

    @Autowired
    private ConfigurableBeanFactory beanFactory;

    @Test
    void test() {
        TimeLimiter timeLimiter = registry.timeLimiter("test");

        TimeLimiterConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(ofSeconds(10), instanceProperties.getTimeoutDuration());
        assertEquals(TRUE, instanceProperties.getCancelRunningFuture());
        assertEquals(valueOf(200), instanceProperties.getEventConsumerBufferSize());

        timeLimiter.onSuccess();
    }

    @EventListener(TimeLimiterOnSuccessEvent.class)
    void onTimeLimiterOnSuccessEvent(TimeLimiterOnSuccessEvent event) {
        assertEquals("test", event.getTimeLimiterName());
    }
}