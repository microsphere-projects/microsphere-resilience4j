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

package io.microsphere.resilience4j.mybatis;


import io.microsphere.mybatis.spring.annotation.EnableMyBatis;
import io.microsphere.mybatis.spring.test.config.MyBatisDataBaseTestConfiguration;
import io.microsphere.mybatis.spring.test.config.MyBatisDataSourceTestConfiguration;
import io.microsphere.mybatis.test.mapper.UserMapper;
import io.microsphere.resilience4j.spring.bulkhead.annotation.EnableBulkhead;
import io.microsphere.resilience4j.spring.circuitbreaker.annotation.EnableCircuitBreaker;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;

import static io.microsphere.mybatis.spring.annotation.MyBatisBeanDefinitionRegistrar.SQL_SESSION_FACTORY_BEAN_NAME;
import static io.microsphere.mybatis.test.AbstractMapperTest.assertUserMapper;
import static io.microsphere.mybatis.test.AbstractMyBatisTest.assertConfiguration;
import static io.microsphere.spring.test.util.SpringTestUtils.testInSpringContainer;

/**
 * {@link MyBatisResilience4jPlugin} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see MyBatisResilience4jPlugin
 * @since 1.0.0
 */
class MyBatisResilience4jPluginTest {

    @Test
    void test() {
        testInSpringContainer(context -> {
            SqlSessionFactory sqlSessionFactory = getSqlSessionFactory(context);
            Configuration configuration = sqlSessionFactory.getConfiguration();
            assertConfiguration(configuration);
            try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
                UserMapper userMapper = configuration.getMapper(UserMapper.class, sqlSession);
                assertUserMapper(userMapper);
            }
        }, DefaultConfig.class);
    }

    private SqlSessionFactory getSqlSessionFactory(ConfigurableApplicationContext context) {
        return context.getBean(SQL_SESSION_FACTORY_BEAN_NAME, SqlSessionFactory.class);
    }

    @EnableBulkhead(plugins = "mybatis")
    @EnableCircuitBreaker
    @EnableMyBatis
    @Import(value = {
            MyBatisDataSourceTestConfiguration.class,
            MyBatisDataBaseTestConfiguration.class,
    })
    static class DefaultConfig {
    }
}