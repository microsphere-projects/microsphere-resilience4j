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

package io.microsphere.resilience4j.hibernate;

import io.microsphere.annotation.Nonnull;
import io.microsphere.hibernate.entity.EntityCallback;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import org.hibernate.type.Type;

import static io.microsphere.resilience4j.common.Resilience4jContext.doInContext;
import static io.microsphere.util.Assert.assertNotNull;

/**
 * Resilience4j x Hibernate Entity Callback
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EntityCallback
 * @since 1.0.0
 */
public class Resilience4jHibernateEntityCallback implements EntityCallback {

    /**
     * The default entry name prefix :  "hibernate@"
     */
    public static final String DEFAULT_ENTRY_NAME_PREFIX = "hibernate@";

    @Nonnull
    private Resilience4jFacade facade;

    @Nonnull
    private String entryNamePrefix = DEFAULT_ENTRY_NAME_PREFIX;

    public void setFacade(@Nonnull Resilience4jFacade facade) {
        assertNotNull(facade, () -> "'Resilience4jFacade' argument must not be null!");
        this.facade = facade;
    }

    public void setEntryNamePrefix(@Nonnull String entryNamePrefix) {
        assertNotNull(entryNamePrefix, () -> "'entryNamePrefix' argument must not be null!");
        this.entryNamePrefix = entryNamePrefix;
    }

    @Override
    public void onPreLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        String entryName = buildEntryName(entity, "LOAD");
        Resilience4jContext<Resilience4jContext[]> begin = this.facade.begin(entryName);
        begin.withinContext();
    }

    @Override
    public void onPostLoad(Object entity, Object id, String[] propertyNames, Type[] types) {
        doInContext(contextt -> this.facade.end(contextt), true);
    }

    @Override
    public void onPreDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        String entryName = buildEntryName(entity, "DELETE");
        Resilience4jContext<Resilience4jContext[]> begin = this.facade.begin(entryName);
        begin.withinContext();
    }

    @Override
    public void onPostDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        doInContext(contextt -> this.facade.end(contextt), true);
    }

    @Override
    public void onPreInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {
        String entryName = buildEntryName(entity, "INSERT");
        Resilience4jContext<Resilience4jContext[]> begin = this.facade.begin(entryName);
        begin.withinContext();
    }

    @Override
    public void onPostInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {
        doInContext(contextt -> this.facade.end(contextt), true);
    }

    @Override
    public void onPreUpdate(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {
        String entryName = buildEntryName(entity, "UPDATE");
        Resilience4jContext<Resilience4jContext[]> begin = this.facade.begin(entryName);
        begin.withinContext();
    }

    @Override
    public void onPostUpdate(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes) {
        doInContext(contextt -> this.facade.end(contextt), true);
    }

    protected String buildEntryName(Object entity, String action) {
        Class<?> entityClass = entity.getClass();
        return entryNamePrefix + entityClass.getName() + '@' + action;
    }
}