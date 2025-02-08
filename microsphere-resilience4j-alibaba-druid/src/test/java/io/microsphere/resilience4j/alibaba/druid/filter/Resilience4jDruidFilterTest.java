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
import io.microsphere.lang.function.ThrowableAction;
import io.microsphere.lang.function.ThrowableConsumer;
import io.microsphere.lang.function.ThrowableFunction;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static io.microsphere.reflect.MethodUtils.findMethod;
import static io.microsphere.reflect.MethodUtils.invokeMethod;
import static java.sql.DriverManager.getConnection;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        execute(statement -> {
            statement.execute("CREATE TABLE users (id INT, name VARCHAR(50))");
        });
    }

    @Test
    public void test() throws Throwable {
        execute(statement -> {
            assertEquals(1, statement.executeUpdate("INSERT INTO users (id, name) VALUES (1, 'Mercy')"));
            assertEquals(1, statement.executeUpdate("INSERT INTO users (id, name) VALUES (2, 'Ma')"));
            ResultSet resultSet = statement.executeQuery("SELECT id,name FROM users");
            assertNotNull(resultSet);
        });
    }

    private void execute(ThrowableConsumer<Statement> consumer) throws Throwable {
        execute(statement -> {
            consumer.accept(statement);
            return null;
        });
    }

    private <R> R execute(ThrowableFunction<Statement, R> function) throws Throwable {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        try {
            return function.apply(statement);
        } finally {
            statement.close();
            connection.close();
        }
    }


    @AfterEach
    public void destroy() throws Throwable {
        dataSource.close();
    }
}
