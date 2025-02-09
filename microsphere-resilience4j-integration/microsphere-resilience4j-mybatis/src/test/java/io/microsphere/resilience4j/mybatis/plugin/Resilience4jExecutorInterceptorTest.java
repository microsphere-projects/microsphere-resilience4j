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
import io.microsphere.lang.function.ThrowableConsumer;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.mybatis.entity.User;
import io.microsphere.resilience4j.mybatis.mapper.UserMapper;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;

import static io.microsphere.resilience4j.mybatis.plugin.Resilience4jExecutorInterceptor.DEFAULT_ENTRY_NAME_PREFIX;
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
public class Resilience4jExecutorInterceptorTest {

    private Resilience4jFacade facade;

    private Resilience4jExecutorInterceptor interceptor;

    private SqlSessionFactory sqlSessionFactory;


    @BeforeEach
    public void init() throws Throwable {
        this.facade = createResilience4jFacade();
        this.interceptor = createResilience4jExecutorInterceptor(this.facade);
        this.sqlSessionFactory = buildSqlSessionFactory();
        initData();
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

    private SqlSessionFactory buildSqlSessionFactory() throws IOException {
        String resource = "META-INF/mybatis/config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory factory = builder.build(inputStream);
        factory.getConfiguration().addInterceptor(this.interceptor);
        return factory;
    }

    private SqlSession openSqlSession() {
        return this.sqlSessionFactory.openSession();
    }

    private UserMapper getUserMapper(SqlSession sqlSession) throws Throwable {
        return sqlSession.getMapper(UserMapper.class);
    }

    private void initData() throws Throwable {
        executeStatement(statement -> {
            statement.execute("CREATE TABLE users (id INT, name VARCHAR(50))");
        });
    }

    @Test
    public void testMapper() throws Throwable {
        SqlSession sqlSession = openSqlSession();
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        int id = 1;
        String name = "Mercy";
        User user = new User(id, name);
        userMapper.saveUser(user);
        User foundUser = userMapper.getUserById(1);
        assertEquals(foundUser, user);
        sqlSession.close();
    }

    @Test
    public void testSqlSession() throws Throwable {
        SqlSession sqlSession = openSqlSession();
        Cursor<User> cursor = sqlSession.selectCursor("io.microsphere.resilience4j.mybatis.mapper.UserMapper.getUserById", 1);
        assertNotNull(cursor);
        assertNotNull(sqlSession.flushStatements());
        sqlSession.commit();
        sqlSession.commit(true);
        sqlSession.rollback();
        sqlSession.rollback(true);
        sqlSession.clearCache();
        sqlSession.close();
    }

    @Test
    public void testConstants() {
        assertEquals("mybatis@", DEFAULT_ENTRY_NAME_PREFIX);
    }

    @Test
    public void testGetter() {
        assertSame(facade, interceptor.getFacade());
        assertEquals(DEFAULT_ENTRY_NAME_PREFIX, interceptor.getEntryNamePrefix());
    }

    private void executeStatement(ThrowableConsumer<Statement> consumer) throws Throwable {
        executeConnection(connection -> {
            Statement statement = connection.createStatement();
            try {
                consumer.accept(statement);
            } finally {
                statement.close();
            }
        });
    }

    private void executeConnection(ThrowableConsumer<Connection> consumer) throws Throwable {
        Connection connection = openSqlSession().getConnection();
        try {
            consumer.accept(connection);
        } finally {
            connection.close();
        }
    }


    @AfterEach
    public void destroy() throws Throwable {
        destroyData();
    }

    private void destroyData() throws Throwable {
        executeStatement(statement -> {
            statement.execute("DROP TABLE users");
        });
    }

}
