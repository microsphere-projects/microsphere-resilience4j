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


import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.microsphere.lang.function.ThrowableAction;
import io.microsphere.resilience4j.common.AbstractResilience4jTemplateTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static io.microsphere.lang.function.ThrowableAction.execute;
import static io.microsphere.resilience4j.bulkhead.ThreadPoolBulkheadTemplate.getRootCause;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link ThreadPoolBulkheadTemplate} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see ThreadPoolBulkheadTemplate
 * @since 1.0.0
 */
class ThreadPoolBulkheadTemplateTest extends AbstractResilience4jTemplateTest<ThreadPoolBulkhead, ThreadPoolBulkheadConfig,
        ThreadPoolBulkheadRegistry, ThreadPoolBulkheadTemplate> {

    @Test
    void testOnEvents() throws InterruptedException {
        String entryName = super.entryName;
        ThreadPoolBulkheadTemplate template = super.template;

        assertSame(template, template.onCallPermittedEvent(entryName, event -> {
            assertEquals(entryName, event.getBulkheadName());
        }));

        assertSame(template, template.onCallFinishedEvent(entryName, event -> {
            assertEquals(entryName, event.getBulkheadName());
        }));

        assertSame(template, template.onCallRejectedEvent(entryName, event -> {
            assertEquals(entryName, event.getBulkheadName());
        }));

        ExecutorService executorService = newFixedThreadPool(1);
        for (int i = 0; i < 100; i++) {
            executorService.execute(() -> {
                assertEquals(entryName, template.execute(entryName, () -> entryName));
            });
        }

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            executorService.awaitTermination(50, MILLISECONDS);
        }
    }

    @Test
    void testGetRootCause() {
        Throwable rootCause = new Throwable("Root Cause");
        Throwable throwable = new Throwable("Throwable", rootCause);
        assertSame(rootCause, getRootCause(throwable));

        ThrowableAction action = () -> {
            throw throwable;
        };

        execute(action, e -> {
            assertSame(rootCause, getRootCause(e));
        });

        assertThrows(NullPointerException.class, () -> {
            try {
                ThrowableAction a = () -> {
                    String s = null;
                    s.toString();
                };
                a.execute();
            } catch (Throwable e) {
                throw getRootCause(e);
            }
        });
    }
}