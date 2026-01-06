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

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties;
import io.microsphere.spring.core.convert.annotation.EnableSpringConverterAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_PERMITTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link EnableBulkhead} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@SpringJUnitConfig(classes = EnableThreadPoolBulkheadTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.thread-pool-bulkhead.instances[test].maxThreadPoolSize=1",
        "microsphere.resilience4j.thread-pool-bulkhead.instances[test].coreThreadPoolSize=1",
        "microsphere.resilience4j.thread-pool-bulkhead.instances[test].queueCapacity=1",
        "microsphere.resilience4j.bulkhead.instances[test].maxConcurrentCalls=10",
        "microsphere.resilience4j.bulkhead.instances[test].eventConsumerBufferSize=100",
        "microsphere.resilience4j.bulkhead.instances[test].maxWaitDuration=PT30S"
})
@EnableBulkhead
@EnableThreadPoolBulkhead(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
public class EnableThreadPoolBulkheadTest {

    @Autowired
    private ThreadPoolBulkheadRegistry registry;

    @Autowired
    private CommonThreadPoolBulkheadConfigurationProperties properties;

    @Test
    void test() throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead threadPoolBulkhead = registry.bulkhead("test");
        CompletionStage<String> completionStage = threadPoolBulkhead.executeCallable(() -> "Hello World");
        assertEquals("Hello World", completionStage.toCompletableFuture().get());

        InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(1, instanceProperties.getMaxThreadPoolSize());
        assertEquals(1, instanceProperties.getCoreThreadPoolSize());
        assertEquals(1, instanceProperties.getQueueCapacity());
    }

    @EventListener(BulkheadOnCallPermittedEvent.class)
    void onBulkheadOnCallPermittedEvent(BulkheadOnCallPermittedEvent event) {
        assertEquals("test", event.getBulkheadName());
        assertEquals(CALL_PERMITTED, event.getEventType());
    }
}
