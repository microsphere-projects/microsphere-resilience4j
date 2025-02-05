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
package io.microsphere.resilience4j.spring.common.jdbc.druid;

import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.filter.FilterAdapter;
import com.alibaba.druid.filter.FilterChain;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.proxy.jdbc.DataSourceProxy;
import com.alibaba.druid.proxy.jdbc.PreparedStatementProxy;
import com.alibaba.druid.proxy.jdbc.ResultSetProxy;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.Registry;
import io.microsphere.logging.Logger;
import io.microsphere.resilience4j.common.Resilience4jTemplate;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;

import static com.alibaba.druid.sql.SQLUtils.parseStatements;
import static io.microsphere.logging.LoggerFactory.getLogger;
import static io.microsphere.resilience4j.util.Resilience4jUtils.createTemplate;

/**
 * Resilience4j x Druid {@link Filter}
 *
 * @param <E> the type of Resilience4j's entry, e.g., {@link CircuitBreaker}
 * @param <C> the type of Resilience4j's configuration, e.g., {@link CircuitBreakerConfig}
 * @param <R> the registry of Resilience4j's entity, e.g., {@link CircuitBreakerRegistry}
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy<a/>
 * @see Filter
 * @see FilterAdapter
 * @since 1.0.0
 */
public abstract class Resilience4jDruidFilter<E, C, R extends Registry<E, C>> extends FilterAdapter {

    private static final Logger logger = getLogger(Resilience4jDruidFilter.class);

    private DataSourceProxy dataSource;

    private String validationSQL;

    protected final Resilience4jTemplate<E, C, R> template;

    public Resilience4jDruidFilter(R registry) {
        this.template = createTemplate(registry);
    }

    @Override
    public void init(DataSourceProxy dataSource) {
        this.dataSource = dataSource;
        if (dataSource instanceof DruidDataSource) {
            DruidDataSource druidDataSource = (DruidDataSource) dataSource;
            this.validationSQL = druidDataSource.getValidationQuery();
        }
    }

    @Override
    public boolean preparedStatement_execute(FilterChain chain, PreparedStatementProxy statement) throws SQLException {
        return doInResilience4j(statement, () -> super.preparedStatement_execute(chain, statement));
    }

    @Override
    public ResultSetProxy preparedStatement_executeQuery(FilterChain chain, PreparedStatementProxy statement) throws SQLException {
        return doInResilience4j(statement, () -> super.preparedStatement_executeQuery(chain, statement));
    }

    @Override
    public int preparedStatement_executeUpdate(FilterChain chain, PreparedStatementProxy statement) throws SQLException {
        return doInResilience4j(statement, () -> super.preparedStatement_executeUpdate(chain, statement));
    }

    @Override
    public ResultSetProxy statement_executeQuery(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_executeQuery(chain, statement, sql));
    }

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_execute(chain, statement, sql));
    }

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql, int autoGeneratedKeys) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_execute(chain, statement, sql, autoGeneratedKeys));
    }

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql, int[] columnIndexes) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_execute(chain, statement, sql, columnIndexes));
    }

    @Override
    public boolean statement_execute(FilterChain chain, StatementProxy statement, String sql, String[] columnNames) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_execute(chain, statement, sql, columnNames));
    }

    @Override
    public int[] statement_executeBatch(FilterChain chain, StatementProxy statement) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_executeBatch(chain, statement));
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_executeUpdate(chain, statement, sql));
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, int autoGeneratedKeys) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_executeUpdate(chain, statement, sql, autoGeneratedKeys));
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, int[] columnIndexes) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_executeUpdate(chain, statement, sql, columnIndexes));
    }

    @Override
    public int statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, String[] columnNames) throws SQLException {
        return doInResilience4j(statement, () -> super.statement_executeUpdate(chain, statement, sql, columnNames));
    }

    /**
     * Get the {@link Resilience4jTemplate}
     *
     * @return non-null
     */
    public final <T extends Resilience4jTemplate<E, C, R>> T getTemplate() {
        return (T) this.template;
    }

    protected final <T> T doInResilience4j(StatementProxy statement, Callable<T> callable) throws SQLException {
        try {
            return this.template.call(getEntryName(statement), callable::call);
        } catch (Throwable e) {
            throw new SQLException(e);
        }
    }

    public final String getEntryName(StatementProxy statement) {
        String sql = statement.getLastExecuteSql();
        if (Objects.equals(sql, validationSQL)) {
            return sql;
        }
        String dbType = dataSource.getDbType();
        List<SQLStatement> statementList = parseStatements(sql, dbType);
        String resourceName = null;
        if (statementList.size() > 0) {
            SQLStatement sqlStatement = statementList.get(0);
            resourceName = getEntryName(sqlStatement);
        }
        if (resourceName == null) {
            logger.debug("The JDBC statement can't be recognized, sql : '{}' , dbType : '{}'", sql, dbType);
            resourceName = "UNRECOGNIZED";
        }
        return resourceName;
    }

    private String getEntryName(SQLStatement sqlStatement) {
        try {
            if (sqlStatement instanceof SQLSelectStatement) {
                return getEntryName((SQLSelectStatement) sqlStatement);
            } else if (sqlStatement instanceof SQLUpdateStatement) {
                return getEntryName((SQLUpdateStatement) sqlStatement);
            } else if (sqlStatement instanceof SQLInsertStatement) {
                return getEntryName((SQLInsertStatement) sqlStatement);
            } else if (sqlStatement instanceof SQLDeleteStatement) {
                return getEntryName((SQLDeleteStatement) sqlStatement);
            }
        } catch (Throwable e) {
            logger.debug("The JDBC statement can't be parsed, sql : '{}'", sqlStatement, e);
        }
        return null;
    }

    private String getEntryName(SQLSelectStatement selectStatement) {
        SQLSelect sqlSelect = selectStatement.getSelect();
        SQLSelectQueryBlock sqlSelectQueryBlock = sqlSelect.getFirstQueryBlock();
        if (sqlSelectQueryBlock == null) {
            return null;
        }
        SQLTableSource sqlTableSource = sqlSelectQueryBlock.getFrom();
        return "SELECT " + sqlTableSource.computeAlias();
    }

    private String getEntryName(SQLUpdateStatement updateStatement) {
        SQLTableSource sqlTableSource = updateStatement.getFrom();
        return "UPDATE " + sqlTableSource.computeAlias();
    }

    private String getEntryName(SQLInsertStatement insertStatement) {
        SQLExprTableSource sqlTableSource = insertStatement.getTableSource();
        return "INSERT " + sqlTableSource.computeAlias();
    }

    private String getEntryName(SQLDeleteStatement deleteStatement) {
        SQLTableSource sqlTableSource = deleteStatement.getTableSource();
        return "DELETE " + sqlTableSource.computeAlias();
    }
}