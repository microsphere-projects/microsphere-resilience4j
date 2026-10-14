# microsphere-resilience4j-mybatis — 用户指南

> 简体中文 | [English](../microsphere-resilience4j-mybatis.md)

> 版本线 `0.2.x`（`release` 分支）· 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0**（默认 `2.4.0`）· Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← 用户指南索引](README.md)

## 本插件的作用

`microsphere-resilience4j-mybatis` 装饰 **MyBatis `Executor`**——即执行每一个映射语句的组件——使每次 `update`、`query` 和 `queryCursor` 调用都运行在 `Resilience4jFacade` 提供的 Resilience4j 装饰之内。

| 类 | 角色 |
|-------|------|
| `io.microsphere.resilience4j.mybatis.executor.Resilience4jExecutor` | 一个 `org.apache.ibatis.executor.Executor` 实现，委托给另一个 `Executor`，并把执行语句的方法包装在 `facade.call(...)` 中 |
| `io.microsphere.resilience4j.mybatis.plugin.Resilience4jExecutorInterceptor` | 一个 `org.apache.ibatis.plugin.Interceptor`，其 `plugin(target)` 把任意 `Executor` 变成 `Resilience4jExecutor` |
| `io.microsphere.resilience4j.mybatis.MyBatisResilience4jPlugin` | 一个 `Resilience4jPlugin`（名称 `mybatis`），把拦截器注册为 Spring bean，从而让 MyBatis 收集到它 |

钩挂位置：MyBatis 为每个 `SqlSession` 新建一个 `Executor`，然后执行 `interceptorChain.pluginAll(executor)`。由于本插件的拦截器在 `plugin(...)` 中返回一个装饰用的 `Executor`，装饰就落在整条语句执行路径的外围——除去 SQL 解析/参数绑定之外，它覆盖了 JDBC 调用本身。一次注册即可保护该 `Configuration` 中的每一个 mapper 和每一条 XML 语句。

与 [Alibaba Druid 插件](microsphere-resilience4j-alibaba-druid.md)（按 SQL 动词 + 表来命名资源）相比，本插件按**映射语句 id** 命名资源，从而提供按 mapper 方法粒度的控制。

## Maven 坐标与依赖装配

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-mybatis</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

从 `microsphere-resilience4j-mybatis/pom.xml` 看，唯一非 optional 的依赖是本库的 Spring 层；插件所集成的每个框架都是 `optional`，期望由宿主应用提供：

| 依赖 | 在插件 POM 中的作用域 | 说明 |
|------------|-------------------------|-------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile | 拉取 `microsphere-resilience4j-commons`（门面/模板） |
| `org.mybatis:mybatis` | **optional** | 本仓库不锁定版本；reactor 通过导入的 `microsphere-mybatis-dependencies` BOM 解析它（`0.2.16`，见 `microsphere-resilience4j-parent/pom.xml`） |
| `org.mybatis:mybatis-spring` | **optional** | 仅 Spring 注册路径需要 |
| `org.springframework:spring-context`、`spring-jdbc` | **optional** | 插件的 `plugin(...)` 签名使用了 Spring 的 bean-factory 类型 |
| `io.github.resilience4j:resilience4j-spring`、`-annotations`、`-consumer`、`-framework-common`、`-core`、`-circuitbreaker`、`-ratelimiter`、`-retry`、`-bulkhead`、`-timelimiter` | **optional** | 添加你实际使用的模块 |
| `io.github.microsphere-projects:microsphere-spring-context` | **optional** | 插件使用的 `BeanRegistrar` 辅助工具 |
| `junit-jupiter`、`spring-test`、`microsphere-mybatis-spring`、`microsphere-spring-test`、`microsphere-mybatis-spring-test`、`h2`、`logback-classic`、`aspectjweaver` | **test** | 仅两个测试类使用 |

## 版本与兼容性

| 条目 | 要求 | 说明 |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x`（`release`） | 构件 `microsphere-resilience4j-mybatis` |
| Resilience4j | 2.0.0 – 2.4.0 | 默认 profile `resilience4j-2.4` → `2.4.0` |
| MyBatis | `org.mybatis:mybatis`（由应用提供） | 针对 `Executor`、`MappedStatement`、`Interceptor`、`Invocation`、`Cursor`、`BatchResult` 编译；`Executor` 接口必须存在 `queryCursor` 和 `deferLoad`（构建所使用的 MyBatis 3.5+ 分支） |
| mybatis-spring | 由应用提供 | 仅用于 `@EnableMyBatis` 风格的注册 |
| JDK | 17+ | 根 POM `java.version = 17` |
| Spring Framework / Spring Cloud | optional | 通过父级版本线支持 Spring Cloud 2022.0.x – 2025.0.x |

## 配置

### 方案 A —— 纯 MyBatis（`Configuration.addInterceptor`）

这正是 `Resilience4jExecutorInterceptorTest` 在其 `customize(Configuration)` 钩子中做的事：

```java
Resilience4jFacade facade = new ChainableResilience4jFacade(
        BulkheadRegistry.ofDefaults(),
        RateLimiterRegistry.ofDefaults(),
        RetryRegistry.ofDefaults(),
        TimeLimiterRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults());

Resilience4jExecutorInterceptor interceptor = new Resilience4jExecutorInterceptor(facade);

Configuration configuration = sqlSessionFactory.getConfiguration(); // or your own Configuration
configuration.addInterceptor(interceptor);                           // before opening sessions
```

此后从该 `Configuration` 打开的每个 `SqlSession`，其 `Executor` 都会被 `Resilience4jExecutor` 包装。

### 方案 B —— Spring 容器（`MyBatisResilience4jPlugin`）

把插件名加入 `microsphere-resilience4j-spring` 中的任意 `@Enable*` 注解；`EnableResilience4jRegistrar` 会从 `META-INF/spring.factories` 加载 `Resilience4jPlugin` 实现，并对你列出的名字调用 `plugin(...)`。随后 `MyBatisResilience4jPlugin.plugin(...)` 会注册一个 `Resilience4jExecutorInterceptor` 的 bean definition，其构造参数接到门面 bean `lazyResilience4jFacade`：

```java
@Configuration
@EnableBulkhead(plugins = MyBatisResilience4jPlugin.PLUGIN_NAME) // "mybatis"
@EnableCircuitBreaker
@EnableMyBatis                                                    // microsphere-mybatis-spring
@Import({MyBatisDataSourceTestConfiguration.class, MyBatisDataBaseTestConfiguration.class})
class DefaultConfig {
}
```

（上面 `@Import` 的配置是 `MyBatisResilience4jPluginTest` 使用的 test 作用域 DataSource/DDL 辅助类；在真实应用中由你自己提供 `DataSource` 和 mapper 注册。）

由于拦截器是以类型为 `org.apache.ibatis.plugin.Interceptor` 的普通 Spring bean 注册的，你的 MyBatis 启动流程必须把 `Interceptor` bean 收集进 `Configuration`（测试中 `microsphere-mybatis-spring` 的 `@EnableMyBatis` 就是这么做的）。如果你是手工构建 `SqlSessionFactory`，请把这个 bean 传给 `Configuration.addInterceptor(...)` 或 `SqlSessionFactoryBean.setPlugins(...)`。

> 插件名是与 `Resilience4jPlugin#getName()` 比对的（见 `EnableResilience4jRegistrar#registerPlugins`）。注解的 Javadoc 把这个属性称为“Spring Bean names”，但实现比对的是插件的**名称**。

## 公共 API

### `Resilience4jExecutorInterceptor`

| 成员 | 行为（源自代码） |
|--------|--------------------------|
| `public static final String DEFAULT_ENTRY_NAME_PREFIX = "mybatis@"` | 入口名的默认前缀 |
| `Resilience4jExecutorInterceptor(Resilience4jFacade facade)` | 使用默认前缀委托给双参构造器 |
| `Resilience4jExecutorInterceptor(Resilience4jFacade facade, String entryNamePrefix)` | 保存两者；无 null 校验 |
| `Object plugin(Object target)` | `if (target instanceof Executor) return new Resilience4jExecutor((Executor) target, facade, entryNamePrefix); return target;` |
| `Object intercept(Invocation invocation)` | `return invocation.proceed();` — 带有注释 *“Current method will be not executed”*（当前方法不会被执行）；在这种织入方式下 MyBatis 从来不会为它构造 `Invocation`，因为 `plugin()` 没有走 `Plugin.wrap(...)` |
| `getFacade()`、`getEntryNamePrefix()` | `@Nonnull` 访问器 |

本类刻意**没有 `@Intercepts` / `@Signature`** 声明——拦截目标是在 `plugin(...)` 中选定的，而不是按方法签名声明的。

### `Resilience4jExecutor`

| 成员 | 行为 |
|--------|-----------|
| `Resilience4jExecutor(Executor delegate, Resilience4jFacade facade, String entryNamePrefix)` | 唯一的构造器；纯字段赋值 |
| `update(MappedStatement, Object)` | 已装饰 |
| `query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)` | 已装饰 |
| `query(MappedStatement, Object, RowBounds, ResultHandler)` | 已装饰 |
| `queryCursor(MappedStatement, Object, RowBounds)` | 已装饰 |
| `flushStatements()`、`commit(boolean)`、`rollback(boolean)`、`createCacheKey(...)`、`isCached(...)`、`clearLocalCache()`、`deferLoad(...)`、`getTransaction()`、`close(boolean)`、`isClosed()` | 纯透传给被委托对象——**未**装饰 |
| `setExecutorWrapper(Executor executor)` | 会委托，但先把参数包装一层：`delegate.setExecutorWrapper(new Resilience4jExecutor(executor, facade, entryNamePrefix))` |
| `doInResilience4j(MappedStatement, ThrowableSupplier<T>)` | `protected <T> T ... throws SQLException` → `facade.call(buildEntryName(ms), callback, SQLException.class)` |
| `getFacade()`、`getEntryNamePrefix()` | `@Nonnull` 访问器 |

### Resilience4j 入口名

```java
private String buildEntryName(MappedStatement ms) {
    return this.entryNamePrefix + ms.getId();
}
```

因此在使用默认前缀时，资源名就是全限定的映射语句 id，例如：

| 映射语句 | 入口名 |
|------------------|------------|
| `io.microsphere.mybatis.test.mapper.UserMapper.getUserById` | `mybatis@io.microsphere.mybatis.test.mapper.UserMapper.getUserById` |
| `com.acme.repo.OrderMapper.insertOrder` | `mybatis@com.acme.repo.OrderMapper.insertOrder` |

语句 id 中含 `.` 字符，所以当你通过属性绑定为单个入口配置 Resilience4j 设置时，要给 key 加引号（YAML：`"mybatis@com.acme.repo.OrderMapper.insertOrder": …`）。`@Enable*` 所使用的绑定前缀来自 `Resilience4jConstants`，例如 `microsphere.resilience4j.circuit-breaker.` 和 `microsphere.resilience4j.bulkhead.`——见 [spring 模块页面](microsphere-resilience4j-spring.md)。

## 完整可运行示例

### 1. 经由拦截器调用 mapper（改写自 `Resilience4jExecutorInterceptorTest`）

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.mybatis.plugin.Resilience4jExecutorInterceptor;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

public class MyBatisResilience4jDemo {

    public static void main(String[] args) throws Throwable {
        Resilience4jFacade facade = new ChainableResilience4jFacade(
                BulkheadRegistry.ofDefaults(), RateLimiterRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());

        Configuration configuration = yourConfiguration();               // built by you / mybatis-spring
        configuration.addInterceptor(new Resilience4jExecutorInterceptor(facade));

        try (SqlSession sqlSession = yourSqlSessionFactory(configuration).openSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            userMapper.saveUser(new User(1, "Mercy"));                   // entry: mybatis@…UserMapper.saveUser
            User found = userMapper.getUserById(1);                      // entry: mybatis@…UserMapper.getUserById
            System.out.println(found);

            // Cursor-based query is decorated too (the statement id is resolved by MyBatis)
            org.apache.ibatis.cursor.Cursor<User> cursor = sqlSession
                    .selectCursor(UserMapper.class.getName() + ".getUserById", 1);
            for (User u : cursor) {
                System.out.println(u);
            }

            sqlSession.flushStatements();                                // pass-through, not decorated
            sqlSession.commit();
        }
    }
}
```

上游测试断言了 mapper 的往返调用（`assertEquals(foundUser, user)`）、cursor/`flushStatements`/`commit(boolean)`/`rollback(boolean)`/`clearCache()` 路径、常量（`DEFAULT_ENTRY_NAME_PREFIX == "mybatis@"`）以及 getter（`assertSame(facade, interceptor.getFacade())`）。

### 2. `intercept(Invocation)` —— 直接调用（与测试的做法一致）

`intercept()` 只有在你自己调用它时才会到达；`Resilience4jExecutorInterceptorTest.testInvoke` 构造了一个 `SimpleExecutor` 和一个针对 `Executor#isClosed` 的 `Invocation`，并断言其透传结果：

```java
Method method = io.microsphere.reflect.MethodUtils.findMethod(Executor.class, "isClosed");
Invocation invocation = new Invocation(new SimpleExecutor(configuration, transaction), method, new Object[0]);
Object result = interceptor.intercept(invocation); // -> Boolean.FALSE, no decoration applied
```

## 行为说明与注意事项

- **SQL 执行期间熔断器打开。** `CircuitBreakerTemplate` 抛出 `CallNotPermittedException`；插件调用的是 `facade.call(name, callback, SQLException.class)`，因此你的 mapper 方法抛出的是 **`SQLException`**（由 `io.microsphere.util.ExceptionUtils.wrap` 包装），而不是 Resilience4j 异常。Spring 的 `DataAccessException` 转换会把它看作一次 JDBC 失败。
- **命中缓存的读取仍会被装饰。** `Resilience4jExecutor.query(...)` 在 MyBatis 查询一级/二级缓存之前就会进入，因此缓存命中同样会消耗限流许可并计入熔断器调用次数。
- **事务控制不在范围内。** `flushStatements`、`commit`、`rollback`、`close` 都是透传。这里熔断器无法保护连接的获取——需要连接池级别的保护请使用 [Druid 插件](microsphere-resilience4j-alibaba-druid.md)。
- **可能出现重复装饰。** `plugin()` 包装 `Executor`，而 `setExecutorWrapper(...)` 在委托之前又会再包装它的参数一次。嵌套的 `Resilience4jExecutor` 实例会把同一条逻辑语句计数多次，这对 `RateLimiter`/`Bulkhead` 的许可数有影响。
- **每个 `SqlSession` 一个包装器。** MyBatis 每个会话创建一个新 `Executor`，因此 `plugin(...)` 会按会话执行；而 Resilience4j 入口本身是共享的（由门面持有的注册表按入口名懒创建）。
- **哪些模块真正生效。** 本插件使用一次性的 `call(...)` 路径，因此门面链中的每个模板都会装饰该调用——包括 `RetryTemplate`、`TimeLimiterTemplate` 和 `ThreadPoolBulkheadTemplate`，它们**不**支持 Spring Web 与 Hibernate 插件所使用的两阶段 `begin()/end()` 路径。注意 `TimeLimiterTemplate` 会在 `ExecutorService` 线程上执行回调，这会把 JDBC/线程绑定的事务工作移出调用线程。
- **门面身份。** 插件注册其拦截器 bean 时，构造参数是对 `LazyResilience4jFacade.BEAN_NAME`（`"lazyResilience4jFacade"`）的**引用**，因此门面是懒解析的，其被委托对象在容器刷新时（`ContextRefreshedEvent`）装配完成。不要在容器刷新之前调用由 `plugin()` 注册的拦截器。

## 相关页面

- [开发者指南 — microsphere-resilience4j-mybatis](../../developer-guide/zh/microsphere-resilience4j-mybatis.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
