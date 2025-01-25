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

import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_FINISHED;
import static io.github.resilience4j.bulkhead.event.BulkheadEvent.Type.CALL_PERMITTED;
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

    @Test
    public void testExecute() {
        String entryName = this.entryName;
        BulkheadTemplate template = this.template;

        template.onCallPermittedEvent(entryName, event -> {
            logger.debug("the event of Bulkhead {} was received.", event);
            assertEquals(entryName, event.getBulkheadName());
            assertEquals(CALL_PERMITTED, event.getEventType());
        });

        template.onCallFinishedEvent(entryName, event -> {
            logger.debug("the event of Bulkhead {} was received.", event);
            assertEquals(entryName, event.getBulkheadName());
            assertEquals(CALL_FINISHED, event.getEventType());
        });

        Object result = template.execute(() -> entryName, () -> null);
        assertNull(result);
    }
}
