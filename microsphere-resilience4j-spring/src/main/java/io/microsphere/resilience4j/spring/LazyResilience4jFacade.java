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

package io.microsphere.resilience4j.spring;

import io.github.resilience4j.core.Registry;
import io.microsphere.lang.DelegatingWrapper;
import io.microsphere.lang.function.ThrowableAction;
import io.microsphere.lang.function.ThrowableSupplier;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jModule;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static io.microsphere.collection.ListUtils.newArrayList;

/**
 * Delegating {@link Resilience4jFacade} based on the lazy-initialized {@link Registry} beans
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Resilience4jFacade
 * @see Registry
 * @since 1.0.0
 */
public class LazyResilience4jFacade implements Resilience4jFacade, ApplicationListener<ContextRefreshedEvent>, DelegatingWrapper {

    public static final String BEAN_NAME = "lazyResilience4jFacade";

    private Resilience4jFacade delegate;

    private Set<Resilience4jModule> modules;

    @Override
    public void execute(String name, Runnable callback) {
        delegate.execute(name, callback);
    }

    @Override
    public <T> T execute(String name, Supplier<T> callback) {
        return delegate.execute(name, callback);
    }

    @Override
    public void call(String name, ThrowableAction callback) throws Throwable {
        delegate.call(name, callback);
    }

    @Override
    public <TR extends Throwable> void call(String name, ThrowableAction callback, Class<TR> throwableClass) throws TR {
        delegate.call(name, callback, throwableClass);
    }

    @Override
    public <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable {
        return delegate.call(name, callback);
    }

    @Override
    public <T, TR extends Throwable> T call(String name, ThrowableSupplier<T> callback, Class<TR> throwableClass) throws TR {
        return delegate.call(name, callback, throwableClass);
    }

    @Override
    public boolean isBeginSupported() {
        return delegate.isBeginSupported();
    }

    @Override
    public Resilience4jContext<Resilience4jContext[]> begin(String name) {
        return delegate.begin(name);
    }

    @Override
    public boolean isEndSupported() {
        return delegate.isEndSupported();
    }

    @Override
    public void end(Resilience4jContext<Resilience4jContext[]> context) {
        delegate.end(context);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        initDelegate(event.getApplicationContext());
    }

    public void setModules(Set<Resilience4jModule> modules) {
        this.modules = modules;
    }

    public Set<Resilience4jModule> getModules() {
        return this.modules;
    }

    private void initDelegate(ApplicationContext context) {
        Set<Resilience4jModule> modules = getModules();
        List<Registry> registries = newArrayList(modules.size());
        for (Resilience4jModule module : modules) {
            Class<? extends Registry> registryClass = module.getRegistryClass();
            Registry registry = context.getBean(registryClass);
            registries.add(registry);
        }
        this.delegate = new ChainableResilience4jFacade(registries);
    }

    @Override
    public Object getDelegate() {
        return this.delegate;
    }
}