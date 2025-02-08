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
package io.microsphere.resilience4j.feign;

import io.microsphere.resilience4j.common.Resilience4jFacade;
import org.junit.jupiter.api.Test;

import static io.microsphere.resilience4j.feign.Resilience4jCapability.DEFAULT_DECORATED_POINT;
import static io.microsphere.resilience4j.feign.Resilience4jCapability.DEFAULT_ENTRY_NAME_PREFIX;
import static io.microsphere.resilience4j.feign.Resilience4jCapability.DecoratedPoint.CLIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link Resilience4jCapability} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jCapability
 * @since 1.0.0
 */
public class Resilience4jCapabilityTest extends AbstractResilience4jFeignTest {

    @Override
    protected Resilience4jCapability createResilience4jCapability(Resilience4jFacade facade) {
        return new Resilience4jCapability(facade);
    }

    @Test
    public void testConstants() {
        assertEquals("feign@", DEFAULT_ENTRY_NAME_PREFIX);
        assertEquals(CLIENT, DEFAULT_DECORATED_POINT);
    }
}
