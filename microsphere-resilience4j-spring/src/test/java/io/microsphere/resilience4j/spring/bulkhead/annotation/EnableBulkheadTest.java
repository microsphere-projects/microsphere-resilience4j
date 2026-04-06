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
package io.microsphere.resilience4j.spring.bulkhead.annotation;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.BulkheadConfigurationProperties;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
import io.microsphere.spring.core.convert.annotation.EnableSpringConverterAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Duration;

import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_PERMITTED;
import static io.microsphere.resilience4j.spring.LoggingResilience4jPlugin.NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link EnableBulkhead} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@SpringJUnitConfig(classes = EnableBulkheadTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.bulkhead.instances[test].maxConcurrentCalls=10",
        "microsphere.resilience4j.bulkhead.instances[test].eventConsumerBufferSize=100",
        "microsphere.resilience4j.bulkhead.instances[test].maxWaitDuration=PT30S"
})
@EnableBulkhead(publishEvents = true, consumeEvents = true, plugins = NAME)
@EnableSpringConverterAdapter
class EnableBulkheadTest {

    @Autowired
    private BulkheadRegistry registry;

    @Autowired
    private BulkheadConfigurationProperties properties;

    @Test
    void test() {
        String entryName = "test";
        Bulkhead circuitBreaker = registry.bulkhead(entryName);
        circuitBreaker.acquirePermission();

        CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(Integer.valueOf(10), instanceProperties.getMaxConcurrentCalls());
        assertEquals(Integer.valueOf(100), instanceProperties.getEventConsumerBufferSize());
        assertEquals(Duration.ofSeconds(30), instanceProperties.getMaxWaitDuration());

        // replace entry
        registry.replace(entryName, circuitBreaker);

        // remove entry
        registry.remove(entryName);
    }

    @EventListener(BulkheadOnCallPermittedEvent.class)
    void onBulkheadOnCallPermittedEvent(BulkheadOnCallPermittedEvent event) {
        assertEquals("test", event.getBulkheadName());
        assertEquals(CALL_PERMITTED, event.getEventType());
    }
}
