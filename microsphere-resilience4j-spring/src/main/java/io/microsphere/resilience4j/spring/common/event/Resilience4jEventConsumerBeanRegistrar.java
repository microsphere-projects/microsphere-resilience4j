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
package io.microsphere.resilience4j.spring.common.event;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.EventConsumer;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.microsphere.logging.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Method;

import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.resilience4j.spring.common.event.EventConsumerMethodFilter.INSTANCE;
import static org.springframework.core.ResolvableType.forMethodParameter;
import static org.springframework.util.ReflectionUtils.doWithMethods;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;

/**
 * Resilience4j {@link EventConsumer Event consumer beans} register
 *
 * @param <E> the type of Resilience4j entry, e.g : {@link CircuitBreaker}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public abstract class Resilience4jEventConsumerBeanRegistrar<E> implements RegistryEventConsumer<E>, BeanFactoryAware {

    protected final Logger logger = getLogger(getClass());

    private BeanFactory beanFactory;

    @Override
    public void onEntryAddedEvent(EntryAddedEvent<E> entryAddedEvent) {
        E entry = entryAddedEvent.getAddedEntry();
        logger.trace("A new entry was added : {} , event-type : {} , creation-time : {}", entry,
                entryAddedEvent.getEventType(), entryAddedEvent.getCreationTime());
        register(entry);
    }

    @Override
    public void onEntryRemovedEvent(EntryRemovedEvent<E> entryRemoveEvent) {
        logger.trace("The entry was removed : {} , event-type : {} , creation-time : {}", entryRemoveEvent.getRemovedEntry(),
                entryRemoveEvent.getEventType(), entryRemoveEvent.getCreationTime());
    }

    @Override
    public void onEntryReplacedEvent(EntryReplacedEvent<E> entryReplacedEvent) {
        E newEntry = entryReplacedEvent.getNewEntry();
        logger.trace("The entry was replaced[old : {} , new : {}], event-type : {} , creation-time : {}",
                entryReplacedEvent.getOldEntry(), newEntry,
                entryReplacedEvent.getEventType(), entryReplacedEvent.getCreationTime());
        register(newEntry);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private void register(E entry) {
        Class<?> entryClass = entry.getClass();
        Method eventPublisherMethod = findMethod(entryClass, "getEventPublisher");
        Class<?> eventPublisherClass = eventPublisherMethod.getReturnType();
        Object eventPublisher = invokeMethod(eventPublisherMethod, entry);
        doWithMethods(eventPublisherClass, method -> {
            ResolvableType type = forMethodParameter(method, 0);
            ObjectProvider objectProvider = beanFactory.getBeanProvider(type);
            objectProvider.forEach(eventConsumerBean -> invokeMethod(method, eventPublisher, eventConsumerBean));
        }, INSTANCE);
    }
}