# microsphere-resilience4j-mybatis — 用户指南（1.x）

> 简体中文 | [English](../microsphere-resilience4j-mybatis.md)

> 版本线 `0.1.x`（`release-1.x` 分支）· 最新发布版本 `0.1.4` · 当前 `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0**（默认 `1.7.0`）· Spring Cloud Hoxton – 2021.0.x · [← 用户指南索引](README.md)

源码树中的模块路径：`microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis`。

## 问题陈述

MyBatis 应用通常逐个保护仓储（repository）方法，这意味着要么给每一个 mapper 调用加注解，要么给每一个 DAO 写一层包装。两者都难以扩展，而且都覆盖不到你遗忘的那些 mapper 方法。

本插件把弹性能力放在 MyBatis 唯一的收口点上：`org.apache.ibatis.executor.Executor`，**每一条**映射语句（select / insert / update / delete / cursor）都必须经过它。于是一个拦截器就能覆盖某个 `Configuration` 下所有的 mapper 与所有的 `SqlSession` 调用。

## MyBatis 中的钩子位置

两个相互协作的组件，都是本模块的真实类：

| 类 | MyBatis 契约 | 作用 |
|-------|------------------|--------------|
| `io.microsphere.resilience4j.mybatis.plugin.Resilience4jExecutorInterceptor` | `implements org.apache.ibatis.plugin.Interceptor` | 在 `plugin(Object target)` 中决定*是否*对一个目标进行装饰 |
| `io.microsphere.resilience4j.mybatis.executor.Resilience4jExecutor` | `implements org.apache.ibatis.executor.Executor` | 真正打开/关闭弹性边界的装饰器 |

这与典型的 MyBatis 插件有一处重要差别，并且可以在源码中核实：**`Resilience4jExecutorInterceptor` 没有携带 `@Intercepts`/`@Signature` 注解，也没有使用 `org.apache.ibatis.plugin.Plugin.wrap(...)`。** 它的 `plugin()` 返回一个手工构造的装饰器：

```java
@Override
public Object plugin(Object target) {
    if (target instanceof Executor) {
        return new Resilience4jExecutor((Executor) target, facade, entryNamePrefix);
    }
    return target;
}
```

`@Intercepts`/`@Signature` 只是 MyBatis 的 `Plugin.wrap()` 那套 JDK 代理机制的要求，而本拦截器刻意绕过了它。带来的结果是：在正常的 MyBatis 执行过程中 `intercept(Invocation)` **从不**被调用；它的方法体是一处带显式注释的透传实现：

```java
@Override
public Object intercept(Invocation invocation) throws Throwable {
    // Current method will be not executed
    return invocation.proceed();
}
```

## Maven 坐标

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-mybatis</artifactId>
    <version>0.1.4</version>
    <!-- managed if you import microsphere-resilience4j-dependencies -->
</dependency>
```

模块 POM 声明了一个强制依赖（`microsphere-resilience4j-spring`）；所有框架依赖都是 `optional`：

| 依赖 | 声明的作用域 |
|------------|----------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile（非 optional） |
| `org.mybatis:mybatis`、`org.mybatis:mybatis-spring` | **optional** —— 由你提供 |
| `io.github.resilience4j:resilience4j-spring`、`-annotations`、`-consumer`、`-framework-common`、`-core`、`-circuitbreaker`、`-ratelimiter`、`-retry`、`-bulkhead`、`-timelimiter` | **optional** |
| `org.springframework:spring-context`、`spring-jdbc` | **optional** |
| `io.github.microsphere-projects:microsphere-spring-context`、`microsphere-annotation-processor` | **optional** |
| `junit-jupiter`、`spring-test`、`microsphere-mybatis-spring`、`microsphere-spring-test`、`microsphere-mybatis-spring-test`、`h2`、`logback-classic`、`aspectjweaver` | **test** |

## 兼容性

| 条目 | `release-1.x` 上的取值 | 证据 |
|------|------------------------|----------|
| microsphere-resilience4j | `0.1.x`，`${revision}` = `0.1.5-SNAPSHOT`，最新发布版本 `0.1.4` | 根 `pom.xml` |
| Resilience4j | 1.3.0 – 1.7.0，默认 `1.7.0` | `microsphere-resilience4j-parent/pom.xml` 的 profile `resilience4j-1.3` … `resilience4j-1.7`（`1.7` 为 `activeByDefault`） |
| Spring Cloud | Hoxton – 2021.0.x | `.github/workflows/maven-build.yml` 中的 CI 矩阵 `maven-profile-spring-cloud` |
| MyBatis / mybatis-spring | **未在本仓库中锁定** | `org.mybatis:mybatis` 与 `org.mybatis:mybatis-spring` 以 `optional` 声明且不带版本；本 reactor 通过导入的 `microsphere-mybatis-dependencies` BOM 解析它们，属性 `microsphere-mybatis.version = 0.1.15` |
| JDK | 未在本快照中锁定 | 该分支任何 POM 中都没有编译器属性；从 `microsphere-spring-cloud-parent` **0.1.26** 继承。CI 矩阵运行 `java: ['8','11','17','21','25']`，发布流程使用 Java `11`。索引中所说的“JDK 8”是一句意图声明，而不是 POM 事实。 |
| 测试数据库 | H2（`com.h2database:h2`，test 作用域） | 模块 `pom.xml` |

## API

### `Resilience4jExecutorInterceptor`

```java
public class Resilience4jExecutorInterceptor implements Interceptor {

    public static final String DEFAULT_ENTRY_NAME_PREFIX = "mybatis@";

    public Resilience4jExecutorInterceptor(Resilience4jFacade facade);
    public Resilience4jExecutorInterceptor(Resilience4jFacade facade, String entryNamePrefix);

    @Override public Object intercept(Invocation invocation) throws Throwable;
    @Override public Object plugin(Object target);

    @Nonnull public Resilience4jFacade getFacade();
    @Nonnull public String            getEntryNamePrefix();
}
```

### `Resilience4jExecutor`

```java
public class Resilience4jExecutor implements Executor {

    public Resilience4jExecutor(Executor delegate, Resilience4jFacade facade, String entryNamePrefix);

    // Protected by Resilience4j
    @Override public int update(MappedStatement ms, Object parameter) throws SQLException;
    @Override public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds,
                                       ResultHandler resultHandler,
                                       CacheKey cacheKey, BoundSql boundSql) throws SQLException;
    @Override public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds,
                                       ResultHandler resultHandler) throws SQLException;
    @Override public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException;

    // Plain pass-through to the delegate
    @Override public List<BatchResult> flushStatements() throws SQLException;
    @Override public void commit(boolean required) throws SQLException;
    @Override public void rollback(boolean required) throws SQLException;
    @Override public CacheKey createCacheKey(MappedStatement ms, Object parameterObject,
                                             RowBounds rowBounds, BoundSql boundSql);
    @Override public boolean isCached(MappedStatement ms, CacheKey key);
    @Override public void clearLocalCache();
    @Override public void deferLoad(MappedStatement ms, MetaObject resultObject, String property,
                                    CacheKey key, Class<?> targetType);
    @Override public Transaction getTransaction();
    @Override public void close(boolean forceRollback);
    @Override public boolean isClosed();
    @Override public void setExecutorWrapper(Executor executor);

    @Nonnull public Resilience4jFacade getFacade();
    @Nonnull public String            getEntryNamePrefix();
}
```

**只有四个 `Executor` 操作被装饰**：`update`、两个 `query` 重载以及 `queryCursor`。`flushStatements`、`commit`、`rollback`、缓存与事务相关方法都是直接委托——在本分支上，批量语句刷新与事务控制*不会*被熔断或舱壁隔离所保护。

条目名称：`buildEntryName(MappedStatement ms)` 返回 `entryNamePrefix + ms.getId()`，也就是默认情况下：

```
mybatis@io.microsphere.mybatis.test.mapper.UserMapper.getUserById
```

这里前缀**确实**被应用了（与 Druid 过滤器不同）。你在 Resilience4j 中配置的实例名必须与完整的 `mybatis@<statementId>` 字符串一致。

### `MyBatisResilience4jPlugin`

```java
public class MyBatisResilience4jPlugin implements Resilience4jPlugin {

    public static final String PLUGIN_NAME = "mybatis";

    @Override public void plugin(ConfigurableListableBeanFactory beanFactory,
                                 BeanDefinitionRegistry registry,
                                 ConfigurableEnvironment environment) {
        registerResilience4jExecutorInterceptor(registry);
    }
    @Override public String getName() { return PLUGIN_NAME; }
}
```

与它的 Druid 对应物不同，这个 `plugin(...)` 做的是真正的工作：它注册一个 `Resilience4jExecutorInterceptor` 的 bean 定义，其**构造器参数是对名为 `lazyResilience4jFacade` 的 bean 的引用**（`LazyResilience4jFacade.BEAN_NAME`）。

## 装配 / 注册

从下列方式中任选其一。三种方式都以该模块的源码或测试为依据。

### 1. Spring：`@Enable*` 指定插件名（测试的做法）

```java
@EnableBulkhead(plugins = "mybatis")
@EnableCircuitBreaker
@EnableMyBatis
@Import(value = {
        MyBatisDataSourceTestConfiguration.class,
        MyBatisDataBaseTestConfiguration.class,
})
static class DefaultConfig {
}
```

`microsphere-resilience4j-spring` 的任意一个 `@Enable*` 注解都暴露 `plugins()`（由 `@EnableResilience4j` 别名而来）。注册器会从 `META-INF/spring.factories` 加载 `Resilience4jPlugin` 工厂，并在 `plugins` 包含 `getName()` 时调用 `plugin(...)`。因此正是 `plugins = "mybatis"` 触发了拦截器 bean 的注册：

```properties
# microsphere-resilience4j-mybatis/src/main/resources/META-INF/spring.factories
io.microsphere.resilience4j.spring.common.Resilience4jPlugin=\
io.microsphere.resilience4j.mybatis.MyBatisResilience4jPlugin
```

你仍然需要让相应的 `@Enable*` 模块注解生效（上面的 `@EnableBulkhead`，再加上例如 `@EnableCircuitBreaker`），这样 `lazyResilience4jFacade` 才会存在，并持有你期望的那些注册表。

### 2. 编程式 MyBatis `Configuration`（`Resilience4jExecutorInterceptorTest` 的做法）

```java
@Override
protected void customize(Configuration configuration) {
    this.facade = createResilience4jFacade();
    this.interceptor = createResilience4jExecutorInterceptor(this.facade);
    configuration.addInterceptor(this.interceptor);
}
```

其中门面建立在五个注册表之上：

```java
BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

Resilience4jFacade facade = new ChainableResilience4jFacade(
        bulkheadRegistry, rateLimiterRegistry, retryRegistry, timeLimiterRegistry, circuitBreakerRegistry);

Resilience4jExecutorInterceptor interceptor = new Resilience4jExecutorInterceptor(facade);
```

### 3. 普通 `@Bean` 注册

把 `Resilience4jExecutorInterceptor` 声明为一个 Spring bean，并让你的 `SqlSessionFactory` 收集它，这是 mybatis-spring 的常规做法——**但请注意两处源码层面的约束**：该拦截器没有无参构造器（只有 `(Resilience4jFacade)` 与 `(Resilience4jFacade, String)`），因此必须用 `@Bean` 方法或 constructor-arg 方式装配；而某个具体的 `SqlSessionFactory` 实现是否会自动发现 `Interceptor` bean，取决于 MyBatis-Spring / `@EnableMyBatis`（外部构件 `microsphere-mybatis-spring` 0.1.15）的行为，与本仓库无关。请在你自己的技术栈中验证。

出于同样的原因，经典的 `mybatis-config.xml` 写法 `<plugins><plugin interceptor="…"/></plugins>` 也无法照原样工作：MyBatis 会通过默认构造器反射实例化该元素并注入 `Properties`，而这两个条件本类都不满足。

## 完整示例（来自真实测试）

`Resilience4jExecutorInterceptorTest` 继承 `AbstractMyBatisTest`（以 `io.microsphere.mybatis.test.AbstractMyBatisTest` 的形式 import，即来自本 POM 中以 test 作用域声明的某个 Microsphere MyBatis 测试构件——究竟是哪个构件提供它，无法从本仓库核实），并通过 `Configuration.addInterceptor` 注册拦截器。随后它的 mapper 测试演练了两条被装饰的路径：

```java
@Test
void testMapper() throws Throwable {
    doInMapper(UserMapper.class, userMapper -> {
        int id = 1;
        String name = "Mercy";
        User user = new User(id, name);
        userMapper.saveUser(user);                       // → Executor.update → "mybatis@…UserMapper.saveUser"
        User foundUser = userMapper.getUserById(1);      // → Executor.query  → "mybatis@…UserMapper.getUserById"
        assertEquals(foundUser, user);
    });
}

@Test
void testSqlSession() throws Throwable {
    doInSqlSession(sqlSession -> {
        Cursor<User> cursor = sqlSession.selectCursor(
                "io.microsphere.mybatis.test.mapper.UserMapper.getUserById", 1);   // → queryCursor
        assertNotNull(cursor);
        assertNotNull(sqlSession.flushStatements());
        sqlSession.commit();
        sqlSession.commit(true);
        sqlSession.rollback();
        sqlSession.rollback(true);
        sqlSession.clearCache();
    });
}
```

`MyBatisResilience4jPluginTest` 覆盖 Spring 路径：它以前面所示的配置启动一个上下文，通过 `SQL_SESSION_FACTORY_BEAN_NAME` 解析出 `SqlSessionFactory`，开启一个会话，并调用 mapper（`assertUserMapper(userMapper)`）。

## 行为说明

1. **失败保持其 SQL 类型。** `Resilience4jExecutor.doInResilience4j` 使用 `facade.call(entryName, callback, SQLException.class)`；`Resilience4jOperations` 会把任意 `Throwable` 重新包装为 `SQLException`（`io.microsphere.util.ExceptionUtils.wrap`）。因此一个打开的断路器在 MyBatis 看来就是一次普通的 `SQLException`。如果你的技术栈配置了 MyBatis-Spring 的异常转换，那么该 `SQLException` 会在调用方一侧被进一步转换为 `DataAccessException`——这是 MyBatis-Spring 的行为，而不是本插件的行为。
2. **条目名称按语句 id 划分。** 一个 `MappedStatement` id 对应一个 Resilience4j 实例，因此同一个 mapper 方法的 `SELECT` 与 `UPDATE` 语句天然就是分开的。命名空间变更（移动某个 mapper 方法）会改变被保护资源的名称。
3. **拦截器是通过 MyBatis 的 `InterceptorChain.pluginAll` 生效的**——因为 `plugin()` 只对 `Executor` 目标返回装饰器，其他目标类型（`StatementHandler`、`ParameterHandler`、`ResultSetHandler`）会原样穿过。
4. **`setExecutorWrapper` 会再次织入。** `delegate.setExecutorWrapper(new Resilience4jExecutor(executor, facade, entryNamePrefix))`——传入的执行器在交给 delegate 之前会被再次装饰。如果 MyBatis 用一个已被装饰的执行器调用它，就会出现嵌套装饰（进而导致嵌套计数）。请把它标记为一处行为怪癖；本分支没有任何测试覆盖它。
5. **Bulkhead/CircuitBreaker 的状态只在被装饰调用的持续期间持有**——`call(name, supplier)` 在调用 delegate 之前执行 `begin`、之后执行 `end`，因此重试/超时模板也在其中：`RetryTemplate` 与 `TimeLimiterTemplate` 只参与一次性的 `call` 路径（它们的 `isBeginSupported()`/`isEndSupported()` 返回 `false`），而这恰恰是本插件所使用的路径。
6. **`release-1.x` 上本插件不存在 Spring Boot 自动配置**（`microsphere-resilience4j-spring-boot` 只有一个 `pom.xml`，没有源码）。

## 相关页面

- [开发者指南 — 本模块](../../developer-guide/zh/microsphere-resilience4j-mybatis.md)
- [用户指南 — commons](microsphere-resilience4j-commons.md)
- [用户指南 — spring](microsphere-resilience4j-spring.md)
- [用户指南 — alibaba druid](microsphere-resilience4j-alibaba-druid.md)（更底层 JDBC 的替代方案）
