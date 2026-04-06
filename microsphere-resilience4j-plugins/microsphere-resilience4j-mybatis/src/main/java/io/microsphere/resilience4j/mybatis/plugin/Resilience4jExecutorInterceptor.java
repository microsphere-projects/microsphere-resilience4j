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
package io.microsphere.resilience4j.mybatis.plugin;

import io.github.resilience4j.core.lang.NonNull;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.mybatis.executor.Resilience4jExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;

/**
 * Resilience4j x MyBatis {@link Executor}'s {@link Interceptor}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jFacade
 * @see Interceptor
 * @see Executor
 * @since 1.0.0
 */
public class Resilience4jExecutorInterceptor implements Interceptor {

    /**
     * The default entry name prefix
     */
    public static final String DEFAULT_ENTRY_NAME_PREFIX = "mybatis@";

    private final Resilience4jFacade facade;

    private final String entryNamePrefix;

    public Resilience4jExecutorInterceptor(Resilience4jFacade facade) {
        this(facade, DEFAULT_ENTRY_NAME_PREFIX);
    }

    public Resilience4jExecutorInterceptor(Resilience4jFacade facade, String entryNamePrefix) {
        this.facade = facade;
        this.entryNamePrefix = entryNamePrefix;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // Current method will be not executed
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return new Resilience4jExecutor((Executor) target, facade, entryNamePrefix);
        }
        return target;
    }

    /**
     * Get the {@link Resilience4jFacade}
     *
     * @return non-null
     */
    @NonNull
    public Resilience4jFacade getFacade() {
        return facade;
    }

    /**
     * Get the entry name prefix
     *
     * @return non-null
     */
    @NonNull
    public String getEntryNamePrefix() {
        return entryNamePrefix;
    }
}
