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

import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.mybatis.test.AbstractMyBatisTest;
import io.microsphere.mybatis.test.entity.User;
import io.microsphere.mybatis.test.mapper.UserMapper;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransaction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;

import static io.microsphere.reflect.MethodUtils.findMethod;
import static io.microsphere.resilience4j.mybatis.plugin.Resilience4jExecutorInterceptor.DEFAULT_ENTRY_NAME_PREFIX;
import static io.microsphere.util.ArrayUtils.ofArray;
import static java.lang.Boolean.FALSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link Resilience4jExecutorInterceptor} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jExecutorInterceptor
 * @since 1.0.0
 */
class Resilience4jExecutorInterceptorTest extends AbstractMyBatisTest {

    private Resilience4jFacade facade;

    private Resilience4jExecutorInterceptor interceptor;

    @Override
    protected void customize(Configuration configuration) {
        this.facade = createResilience4jFacade();
        this.interceptor = createResilience4jExecutorInterceptor(this.facade);
        configuration.addInterceptor(this.interceptor);
    }

    @Test
    void testMapper() throws Throwable {
        doInMapper(UserMapper.class, userMapper -> {
            int id = 1;
            String name = "Mercy";
            User user = new User(id, name);
            userMapper.saveUser(user);
            User foundUser = userMapper.getUserById(1);
            assertEquals(foundUser, user);
        });
    }

    @Test
    void testSqlSession() throws Throwable {
        doInSqlSession(sqlSession -> {
            Cursor<User> cursor = sqlSession.selectCursor("io.microsphere.mybatis.test.mapper.UserMapper.getUserById", 1);
            assertNotNull(cursor);
            assertNotNull(sqlSession.flushStatements());
            sqlSession.commit();
            sqlSession.commit(true);
            sqlSession.rollback();
            sqlSession.rollback(true);
            sqlSession.clearCache();
        });
    }

    @Test
    void testConstants() {
        assertEquals("mybatis@", DEFAULT_ENTRY_NAME_PREFIX);
    }

    @Test
    void testGetter() {
        assertSame(facade, interceptor.getFacade());
        assertEquals(DEFAULT_ENTRY_NAME_PREFIX, interceptor.getEntryNamePrefix());
    }

    @Test
    void testInvoke() throws Throwable {
        doInSqlSession(sqlSession -> {
            Configuration configuration = sqlSession.getConfiguration();
            Connection connection = sqlSession.getConnection();
            Transaction transaction = new JdbcTransaction(connection);
            Executor executor = new SimpleExecutor(configuration, transaction);

            Method method = findMethod(Executor.class, "isClosed");
            Invocation invocation = new Invocation(executor, method, ofArray());
            assertEquals(FALSE, interceptor.intercept(invocation));

            transaction.close();
            connection.close();
        });
    }

    private Resilience4jFacade createResilience4jFacade() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        this.facade = new ChainableResilience4jFacade(bulkheadRegistry, rateLimiterRegistry,
                retryRegistry, timeLimiterRegistry, circuitBreakerRegistry);
        return new ChainableResilience4jFacade(bulkheadRegistry, rateLimiterRegistry,
                retryRegistry, timeLimiterRegistry, circuitBreakerRegistry);
    }

    private Resilience4jExecutorInterceptor createResilience4jExecutorInterceptor(Resilience4jFacade facade) {
        return new Resilience4jExecutorInterceptor(facade);
    }
}