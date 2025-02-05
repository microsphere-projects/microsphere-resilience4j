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
package io.microsphere.resilience4j.ratelimiter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.microsphere.resilience4j.common.AbstractResilience4jTemplateTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.ratelimiter.event.RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link RateLimiterTemplate} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see RateLimiterTemplate
 * @since 1.0.0
 */
public class RateLimiterTemplateTest extends AbstractResilience4jTemplateTest<RateLimiter, RateLimiterConfig, RateLimiterRegistry, RateLimiterTemplate> {

    private final long durationInMills = 50;

    private final Duration duration = ofMillis(durationInMills);

    @Override
    protected RateLimiterConfig createEntryConfig() {
        return RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(duration)
                .timeoutDuration(duration)
                .build();
    }

    @Test
    public void execute() {
        String entryName = super.entryName;
        RateLimiterTemplate template = this.template;

        template.onSuccessEvent(entryName, event -> {
            logEvent(event);
            assertEquals(entryName, event.getRateLimiterName());
            assertSame(SUCCESSFUL_ACQUIRE, event.getEventType());
        });

        for (int i = 0; i < 5; i++) {
            template.execute(entryName, () -> {
            });
        }

        for (int i = 0; i < 5; i++) {
            template.execute(entryName, () -> "For testing");
        }

        template.execute(entryName, () -> new RuntimeException("For testing"));
    }

    @Test
    public void executeOnFailed() throws InterruptedException {

        String entryName = super.entryName;
        RateLimiterTemplate template = this.template;

        template.onSuccessEvent(entryName, event -> {
            logEvent(event);
        });

        template.onFailureEvent(entryName, event -> {
            logEvent(event);
        });

        template.onDrainedEvent(entryName, event -> {
            logEvent(event);
        });

        ExecutorService executorService = newFixedThreadPool(2);

        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                template.execute(entryName, () -> {
                });
            });
        }

        executorService.shutdown();

        executorService.awaitTermination(3, TimeUnit.SECONDS);

    }

}
