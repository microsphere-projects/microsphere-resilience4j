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
package io.microsphere.resilience4j.alibaba.druid.filter;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.lang.function.ThrowableConsumer;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static io.microsphere.resilience4j.alibaba.druid.filter.Resilience4jDruidFilter.DEFAULT_ENTRY_NAME_PREFIX;
import static java.sql.ResultSet.CONCUR_UPDATABLE;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static java.sql.Statement.NO_GENERATED_KEYS;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link Resilience4jDruidFilter} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Resilience4jDruidFilter
 * @since 1.0.0
 */
public class Resilience4jDruidFilterTest {

    private Resilience4jFacade facade;

    private Resilience4jDruidFilter filter;

    private DruidDataSource dataSource;

    @BeforeEach
    public void init() throws Throwable {
        this.facade = createResilience4jFacade();
        this.filter = createResilience4jDruidFilter(this.facade);
        this.dataSource = createDruidDataSource(this.filter);
        initData();
    }

    protected DruidDataSource createDruidDataSource(Filter filter) throws Throwable {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:mem:test_mem");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("sa");
        dataSource.setProxyFilters(asList(filter));
        dataSource.init();
        return dataSource;
    }

    protected Resilience4jFacade createResilience4jFacade() {
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

    protected Resilience4jDruidFilter createResilience4jDruidFilter(Resilience4jFacade facade) {
        return new Resilience4jDruidFilter(facade);
    }

    private void initData() throws Throwable {
        executeStatement(statement -> {
            statement.execute("CREATE TABLE users (id INT, name VARCHAR(50))", NO_GENERATED_KEYS);
        });
    }

    @Test
    public void testExecuteStatement() throws Throwable {
        executeStatement(statement -> {
            assertEquals(1, statement.executeUpdate("INSERT INTO users (id, name) VALUES (1, 'Mercy')"));
            assertEquals(1, statement.executeUpdate("INSERT INTO users (id, name) VALUES (2, 'Blitz')", new String[0]));
            assertEquals(1, statement.executeUpdate("INSERT INTO users (id, name) VALUES (3, 'Ma')", new int[0]));
            assertEquals(1, statement.executeUpdate("INSERT INTO users (id, name) VALUES (4, 'M')", NO_GENERATED_KEYS));
            statement.addBatch("INSERT INTO users (id, name) VALUES (5, 'Z')");
            statement.addBatch("UPDATE users set name = 'z' WHERE id = 5");
            assertArrayEquals(new int[]{1, 1}, statement.executeBatch());
            ResultSet resultSet = statement.executeQuery("SELECT id,name FROM users");
            assertNotNull(resultSet);
            statement.execute("DELETE FROM users WHERE id = 1");
            statement.execute("DELETE FROM users WHERE id = 2", new String[0]);
            statement.execute("DELETE FROM users WHERE id = 3", new int[0]);
            statement.execute("DELETE FROM users WHERE id = 4", NO_GENERATED_KEYS);
            statement.execute("DELETE FROM users");
        });
    }

    @Test
    public void testExecutePreparedStatement() throws Throwable {

        executePreparedStatement("INSERT INTO users (id, name) VALUES (?, ?)", preparedStatement -> {
            preparedStatement.setInt(1, 1);
            preparedStatement.setString(2, "Mercy");
            assertEquals(1, preparedStatement.executeUpdate());
        });

        executeConnection(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT id,name FROM users WHERE id=?", TYPE_FORWARD_ONLY);
            preparedStatement.setInt(1, 1);
            assertNotNull(preparedStatement.executeQuery());

            preparedStatement = connection.prepareStatement("SELECT id,name FROM users WHERE id=?", TYPE_FORWARD_ONLY, CONCUR_UPDATABLE);
            preparedStatement.setInt(1, 1);
            assertNotNull(preparedStatement.execute());

        });
    }

    @Test
    public void testGetter() {
        assertSame(facade, filter.getFacade());
        assertEquals(DEFAULT_ENTRY_NAME_PREFIX, filter.getEntryNamePrefix());
    }

    private void executePreparedStatement(String sql, ThrowableConsumer<PreparedStatement> consumer) throws Throwable {
        executeConnection(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            try {
                consumer.accept(preparedStatement);
            } finally {
                preparedStatement.close();
            }
        });
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
        Connection connection = dataSource.getConnection();
        try {
            consumer.accept(connection);
        } finally {
            connection.close();
        }
    }


    @AfterEach
    public void destroy() throws Throwable {
        destroyData();
        dataSource.close();
    }

    private void destroyData() throws Throwable {
        executeStatement(statement -> {
            statement.execute("DROP TABLE users");
        });
    }
}
