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
package io.microsphere.resilience4j.common;

import io.github.resilience4j.core.Registry;
import io.microsphere.lang.function.ThrowableSupplier;
import io.microsphere.logging.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static io.microsphere.lang.Prioritized.COMPARATOR;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.resilience4j.util.Resilience4jUtils.createTemplates;
import static java.util.Arrays.asList;

/**
 * {@link Resilience4jFacade} implementation delegates to the instances of {@link Resilience4jOperations}.
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jFacade
 * @see Resilience4jOperations
 * @see Resilience4jTemplate
 * @see Resilience4jModule
 * @see Registry
 * @since 1.0.0
 */
public class DelegatingResilience4jFacade implements Resilience4jFacade {

    private final static Logger logger = getLogger(DelegatingResilience4jFacade.class);

    private final List<Resilience4jTemplate> templates;

    private final int size;

    public DelegatingResilience4jFacade(Registry<?, ?>... registries) {
        this(asList(registries));
    }

    public DelegatingResilience4jFacade(Collection<Registry<?, ?>> registries) {
        this(createTemplates(registries));
    }

    public DelegatingResilience4jFacade(List<Resilience4jTemplate> templates) {
        int size = templates.size();
        List<Resilience4jTemplate> effectiveTemplates = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Resilience4jTemplate template = templates.get(i);
            if (!effectiveTemplates.contains(template)) {
                effectiveTemplates.add(template);
            }
        }
        effectiveTemplates.sort(COMPARATOR);

        this.templates = effectiveTemplates;
        this.size = effectiveTemplates.size();
        if (logger.isTraceEnabled()) {
            logger.trace("{} templates : {} -> effective {} templates : {}", size, templates, this.size, this.templates);
        }
    }

    @Override
    public <T> T execute(String name, Supplier<T> callback) {
        T result = null;
        for (int i = 0; i < size; i++) {
            Resilience4jTemplate template = templates.get(i);
            result = (T) template.execute(name, callback);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("execute(name = '{}' , callback = {}) operations of {} templates were executed, result : {}",
                    name, callback, size, result);
        }
        return result;
    }

    @Override
    public <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
        T result = null;
        for (int i = 0; i < size; i++) {
            Resilience4jTemplate template = templates.get(i);
            result = (T) template.call(name, callback);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("call(name = '{}' , callback = {}) operations of {} templates were executed, result : {}",
                    name, callback, size, result);
        }
        return result;
    }

    @Override
    public Resilience4jContext<Resilience4jContext[]> begin(String name) {
        Resilience4jContext[] subContexts = new Resilience4jContext[size];
        Resilience4jContext<Resilience4jContext[]> context = new Resilience4jContext<>(name, subContexts);
        for (int i = 0; i < size; i++) {
            Resilience4jTemplate template = templates.get(i);
            if (template.isBeginSupported()) {
                subContexts[i] = template.begin(name);
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("begin() operations of {} templates were executed -> {}", size, context);
        }
        return context;
    }

    @Override
    public void end(Resilience4jContext<Resilience4jContext[]> context) {
        Resilience4jContext[] subContexts = context.getEntry();
        for (int i = 0; i < size; i++) {
            Resilience4jTemplate template = templates.get(i);
            if (template.isEndSupported()) {
                template.end(subContexts[i]);
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("end() operations of {} templates were executed -> {}", context);
        }
    }

    /**
     * Get the size of delegates
     *
     * @return positive integer
     */
    public int getSize() {
        return size;
    }
}
