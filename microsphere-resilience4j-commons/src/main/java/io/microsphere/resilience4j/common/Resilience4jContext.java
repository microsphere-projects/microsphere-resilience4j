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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;

/**
 * The context of Resilience4j
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jModule
 * @since 1.0.0
 */
public class Resilience4jContext {

    private Map<String, Object> attributes;

    /**
     * Set the attribute name and value.
     *
     * @param name  the attribute name
     * @param value the attribute value
     * @return {@link Resilience4jContext}
     */
    public Resilience4jContext setAttribute(String name, Object value) {
        Map<String, Object> attributes = getOrCreateAttributes();
        attributes.put(name, value);
        return this;
    }

    /**
     * Check whether the attribute exists by name.
     *
     * @param name the attribute name
     * @return <code>true</code> if exists, otherwise <code>false</code>
     */
    public boolean hasAttribute(String name) {
        Map<String, Object> attributes = getOrCreateAttributes();
        return attributes.containsKey(name);
    }

    /**
     * Get the attribute value by name.
     *
     * @param name the attribute name
     * @return the attribute value if found, otherwise <code>null</code>
     */
    public <T> T getAttribute(String name) {
        Map<String, Object> attributes = getOrCreateAttributes();
        return (T) attributes.get(name);
    }

    /**
     * Get the attribute value by name.
     *
     * @param name         the attribute name
     * @param defaultValue the default value of attribute
     * @return the attribute value if found, otherwise <code>defaultValue</code>
     */
    public <T> T getAttribute(String name, T defaultValue) {
        Map<String, Object> attributes = getOrCreateAttributes();
        return (T) attributes.getOrDefault(name, defaultValue);
    }

    /**
     * Remove the attribute by name.
     *
     * @param name the attribute name
     * @return the attribute value if removed, otherwise <code>null</code>
     */
    public <T> T removeAttribute(String name) {
        Map<String, Object> attributes = getOrCreateAttributes();
        return (T) attributes.remove(name);
    }

    /**
     * Remove all attributes
     *
     * @return {@link Resilience4jContext}
     */
    public Resilience4jContext removeAttributes() {
        Map<String, Object> attributes = getOrCreateAttributes();
        attributes.clear();
        return this;
    }

    /**
     * Get the attributes
     *
     * @return the read-only attributes
     */
    public Map<String, Object> getAttributes() {
        Map<String, Object> attributes = this.attributes;
        if (attributes == null) {
            return emptyMap();
        }
        return unmodifiableMap(attributes);
    }

    protected Map<String, Object> getOrCreateAttributes() {
        Map<String, Object> attributes = this.attributes;
        if (attributes == null) {
            attributes = new HashMap<>();
            this.attributes = attributes;
        }
        return attributes;
    }
}
