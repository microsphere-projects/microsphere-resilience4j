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
package io.microsphere.resilience4j.util;

import io.github.resilience4j.core.Registry;
import io.microsphere.reflect.MethodUtils;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.util.BaseUtils;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static io.microsphere.invoke.MethodHandleUtils.findVirtual;
import static io.microsphere.reflect.MethodUtils.findMethod;
import static io.microsphere.reflect.MethodUtils.invokeMethod;
import static io.microsphere.resilience4j.common.Resilience4jModule.valueOf;
import static java.beans.Introspector.decapitalize;
import static java.util.Collections.unmodifiableMap;

/**
 * The utility class for Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see BaseUtils
 * @since 1.0.0
 */
public abstract class Resilience4jUtils extends BaseUtils {


    private static final Map<Resilience4jModule, Method> getEntryMethodsCache;

    static {
        Resilience4jModule[] modules = Resilience4jModule.values();
        Map<Resilience4jModule, Method> methodsCache = new HashMap<>(modules.length);
        for (Resilience4jModule module : modules) {
            initGetEntryMethodHandlesCache(module, methodsCache);
        }
        getEntryMethodsCache = unmodifiableMap(methodsCache);
    }

    private static void initGetEntryMethodHandlesCache(Resilience4jModule module, Map<Resilience4jModule, Method> methodsCache) {
        Class<?> entryClass = module.getEntryClass();
        Class<?> configClass = module.getConfigurationClass();
        Class<?> registryClass = module.getRegistryClass();
        String methodName = decapitalize(entryClass.getSimpleName());
        Method method = findMethod(registryClass, methodName, String.class, configClass);
        methodsCache.put(module, method);
    }

    public static <E, C> E getEntry(Registry<E, C> registry, String name, C configuration) {
        Resilience4jModule module = valueOf(registry.getClass());
        Method method = getEntryMethodsCache.get(module);
        try {
            return invokeMethod(registry, method, name, configuration);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
