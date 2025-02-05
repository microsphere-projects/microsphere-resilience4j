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
package io.microsphere.resilience4j.timelimiter;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.AbstractResilience4jTemplateTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static io.github.resilience4j.timelimiter.event.TimeLimiterEvent.Type.ERROR;
import static io.github.resilience4j.timelimiter.event.TimeLimiterEvent.Type.SUCCESS;
import static io.github.resilience4j.timelimiter.event.TimeLimiterEvent.Type.TIMEOUT;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TimeLimiterTemplate} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see TimeLimiterTemplate
 * @since 1.0.0
 */
public class TimeLimiterTemplateTest extends AbstractResilience4jTemplateTest<TimeLimiter, TimeLimiterConfig, TimeLimiterRegistry, TimeLimiterTemplate> {

    private final long timeoutDurationInMills = 50;

    private final Duration timeoutDuration = ofMillis(timeoutDurationInMills);

    private ExecutorService executorService;

    @Override
    protected TimeLimiterTemplate createTemplate(TimeLimiterRegistry registry) throws Throwable {
        executorService = newFixedThreadPool(1);
        return new TimeLimiterTemplate(registry, executorService);
    }

    /**
     * Create an instance of {@link TimeLimiterConfig} for testing
     *
     * @return non-null
     */
    @Override
    protected TimeLimiterConfig createEntryConfig() {
        return TimeLimiterConfig.custom()
                .cancelRunningFuture(true)
                .timeoutDuration(timeoutDuration)
                .build();
    }

    @Override
    protected void preDestroy() {
        executorService.shutdown();
    }

    @Test
    public void testExecute() {
        String entryName = super.entryName;
        TimeLimiterTemplate template = this.template;

        template.onSuccessEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getTimeLimiterName());
            assertSame(SUCCESS, event.getEventType());
        });

        template.execute(entryName, () -> {
        });
    }

    @Test
    public void testExecuteOnFailed() {
        String entryName = super.entryName;
        TimeLimiterTemplate template = this.template;

        template.onErrorEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getTimeLimiterName());
            assertSame(ERROR, event.getEventType());
            assertTrue(event.getThrowable() instanceof RuntimeException);
        });

        assertThrows(RuntimeException.class, () -> {
            template.call(entryName, () -> {
                throw new RuntimeException("For testing");
            });
        });
    }

    @Test
    public void testExecuteOnTimeout() {
        String entryName = super.entryName;
        TimeLimiterTemplate template = this.template;

        template.onTimeoutEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getTimeLimiterName());
            assertSame(TIMEOUT, event.getEventType());
        });

        assertThrows(TimeoutException.class, () -> {
            template.call(entryName, () -> {
                await(timeoutDurationInMills * 3);
            });
        });
    }
}
