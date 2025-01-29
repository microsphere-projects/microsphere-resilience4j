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
package io.microsphere.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.common.AbstractResilience4jTemplateTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_FINISHED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_PERMITTED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_REJECTED;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link BulkheadTemplate} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see BulkheadTemplate
 * @since 1.0.0
 */
public class BulkheadTemplateTest extends AbstractResilience4jTemplateTest<Bulkhead, BulkheadConfig, BulkheadRegistry, BulkheadTemplate> {

    private int maxConcurrentCalls = 1;

    private Duration maxWaitDuration = ofMillis(100);

    @Override
    protected BulkheadConfig createEntryConfig() {
        return BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                .maxWaitDuration(maxWaitDuration)
                .build();
    }

    @Test
    public void testExecute() {
        String entryName = this.entryName;
        BulkheadTemplate template = this.template;

        template.onCallPermittedEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getBulkheadName());
            assertEquals(CALL_PERMITTED, event.getEventType());
        });

        template.onCallFinishedEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getBulkheadName());
            assertEquals(CALL_FINISHED, event.getEventType());
        });

        Object result = template.execute(() -> entryName, () -> null);
        assertNull(result);
    }

    @Test
    public void testExecuteOnCallRejected() throws InterruptedException {
        String entryName = this.entryName;
        BulkheadTemplate template = this.template;

        template.onCallRejectedEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getBulkheadName());
            assertEquals(CALL_REJECTED, event.getEventType());
        });

        int times = 3;
        ExecutorService executorService = newFixedThreadPool(times);

        for (int i = 0; i < times; i++) {
            executorService.execute(() -> {
                await(maxWaitDuration, () -> template.execute(() -> entryName, () -> {
                }));
            });
        }

        executorService.shutdown();

        while (!executorService.awaitTermination(100, TimeUnit.MILLISECONDS)) {

        }
    }
}
