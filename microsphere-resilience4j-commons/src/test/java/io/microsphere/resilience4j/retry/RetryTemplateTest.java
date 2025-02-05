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
package io.microsphere.resilience4j.retry;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.common.AbstractResilience4jTemplateTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.resilience4j.retry.event.RetryEvent.Type.ERROR;
import static io.github.resilience4j.retry.event.RetryEvent.Type.IGNORED_ERROR;
import static io.github.resilience4j.retry.event.RetryEvent.Type.RETRY;
import static io.github.resilience4j.retry.event.RetryEvent.Type.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link RetryTemplate} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RetryTemplate
 * @since 1.0.0
 */
public class RetryTemplateTest extends AbstractResilience4jTemplateTest<Retry, RetryConfig, RetryRegistry, RetryTemplate> {

    private final int maxAttempts = 3;

    @Override
    protected RetryConfig createEntryConfig() {
        return RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .ignoreExceptions(IllegalStateException.class)
                .retryExceptions(TimeoutException.class)
                .build();
    }

    @Test
    public void testExecute() throws Throwable {
        String entryName = super.entryName;
        RetryTemplate template = super.template;
        String result = "OK";

        AtomicInteger attempts = new AtomicInteger(maxAttempts);

        template.onSuccessEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getName());
            assertSame(SUCCESS, event.getEventType());
            assertEquals(maxAttempts - 1, event.getNumberOfRetryAttempts());
        });

        template.onRetryEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getName());
            assertSame(RETRY, event.getEventType());
            assertEquals(maxAttempts - attempts.get(), event.getNumberOfRetryAttempts());
        });

        template.onErrorEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getName());
            assertSame(ERROR, event.getEventType());
            assertEquals(maxAttempts - attempts.get(), event.getNumberOfRetryAttempts());
        });

        assertEquals(result, template.call(entryName, () -> {
            if (attempts.decrementAndGet() == 0) {
                return result;
            } else {
                throw new TimeoutException("For testing");
            }
        }));
    }

    @Test
    public void testExecuteOnIgnoredException() throws Throwable {
        String entryName = super.entryName;
        RetryTemplate template = super.template;

        template.onIgnoredErrorEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getName());
            assertSame(IGNORED_ERROR, event.getEventType());
        });

        assertThrows(IllegalStateException.class, () -> template.call(entryName, () -> {
            throw new IllegalStateException("For testing");
        }));

    }

}
