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
package io.microsphere.resilience4j.spring.circuitbreaker.jdbc.druid;

import com.alibaba.druid.DbType;
import com.alibaba.druid.filter.FilterChainImpl;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.proxy.jdbc.ConnectionProxy;
import com.alibaba.druid.proxy.jdbc.JdbcParameter;
import com.alibaba.druid.proxy.jdbc.StatementExecuteType;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.stat.JdbcSqlStat;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry.ofDefaults;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link CircuitBreakerDruidFilter} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class CircuitBreakerDruidFilterTest {

    private DruidDataSource druidDataSource;

    private CircuitBreakerDruidFilter filter;

    private CircuitBreakerTemplate template;

    @BeforeEach
    public void init() {
        this.druidDataSource = new DruidDataSource();
        druidDataSource.setValidationQuery("SELECT 1");
        druidDataSource.setDbType(DbType.mysql);

        this.filter = new CircuitBreakerDruidFilter(ofDefaults());
        this.filter.init(druidDataSource);
        this.template = this.filter.getTemplate();
    }

    @Test
    public void testDoInResilience4j() throws SQLException {

        StatementProxy statement = new StatementProxyImpl();

        String entryName = this.filter.getEntryName(statement);

        template.onSuccessEvent(entryName, event -> {
            assertSame(SUCCESS, event.getEventType());
        });

        filter.statement_execute(new FilterChainImpl(this.druidDataSource), statement, null);
    }

}

class StatementProxyImpl implements StatementProxy {

    @Override
    public ConnectionProxy getConnectionProxy() {
        return null;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Statement getRawObject() {
        return this;
    }

    @Override
    public int getAttributesSize() {
        return 0;
    }

    @Override
    public void clearAttributes() {

    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public Object getAttribute(String key) {
        return null;
    }

    @Override
    public void putAttribute(String key, Object value) {

    }

    @Override
    public List<String> getBatchSqlList() {
        return Collections.emptyList();
    }

    @Override
    public String getBatchSql() {
        return "";
    }

    @Override
    public JdbcSqlStat getSqlStat() {
        return null;
    }

    @Override
    public StatementExecuteType getLastExecuteType() {
        return null;
    }

    @Override
    public void setSqlStat(JdbcSqlStat sqlStat) {

    }

    @Override
    public String getLastExecuteSql() {
        return "SELECT 1";
    }

    @Override
    public long getLastExecuteStartNano() {
        return 0;
    }

    @Override
    public void setLastExecuteStartNano(long lastExecuteStartNano) {

    }

    @Override
    public void setLastExecuteStartNano() {

    }

    @Override
    public long getLastExecuteTimeNano() {
        return 0;
    }

    @Override
    public void setLastExecuteTimeNano(long nano) {

    }

    @Override
    public void setLastExecuteTimeNano() {

    }

    @Override
    public Map<Integer, JdbcParameter> getParameters() {
        return Collections.emptyMap();
    }

    @Override
    public int getParametersSize() {
        return 0;
    }

    @Override
    public JdbcParameter getParameter(int i) {
        return null;
    }

    @Override
    public boolean isFirstResultSet() {
        return false;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return null;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return 0;
    }

    @Override
    public void close() throws SQLException {

    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {

    }

    @Override
    public int getMaxRows() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {

    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {

    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return 0;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {

    }

    @Override
    public void cancel() throws SQLException {

    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public void setCursorName(String name) throws SQLException {

    }

    @Override
    public boolean execute(String sql) throws SQLException {
        return false;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return null;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return 0;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {

    }

    @Override
    public int getFetchDirection() throws SQLException {
        return 0;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {

    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return 0;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return 0;
    }

    @Override
    public void addBatch(String sql) throws SQLException {

    }

    @Override
    public void clearBatch() throws SQLException {

    }

    @Override
    public int[] executeBatch() throws SQLException {
        return new int[0];
    }

    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return null;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return 0;
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return 0;
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return false;
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return false;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return false;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {

    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {

    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
