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
package io.microsphere.resilience4j.spring.timelimiter.jdbc.druid;

import com.alibaba.druid.filter.Filter;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.microsphere.resilience4j.spring.common.jdbc.druid.Resilience4jDruidFilter;
import io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate;

/**
 * {@link TimeLimiter} x Druid {@link Filter}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class TimeLimiterDruidFilter extends Resilience4jDruidFilter<TimeLimiter, TimeLimiterConfig, TimeLimiterRegistry> {

    public TimeLimiterDruidFilter(TimeLimiterRegistry registry) {
        super(registry);
    }

    @Override
    protected Resilience4jTemplate<TimeLimiter, TimeLimiterConfig, TimeLimiterRegistry> createTemplate(TimeLimiterRegistry registry) {
        return new TimeLimiterTemplate(registry);
    }
}
