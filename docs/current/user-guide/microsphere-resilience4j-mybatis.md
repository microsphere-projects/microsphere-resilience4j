# microsphere-resilience4j-mybatis — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0** (default `2.4.0`) · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← user guide index](README.md)

## What this plugin does

`microsphere-resilience4j-mybatis` decorates the **MyBatis `Executor`** — the component that executes every mapped statement — so that each `update`, `query` and `queryCursor` call runs inside the Resilience4j decoration provided by `Resilience4jFacade`.

| Class | Role |
|-------|------|
| `io.microsphere.resilience4j.mybatis.executor.Resilience4jExecutor` | `org.apache.ibatis.executor.Executor` implementation that delegates to another `Executor` and wraps the statement-executing methods in `facade.call(...)` |
| `io.microsphere.resilience4j.mybatis.plugin.Resilience4jExecutorInterceptor` | `org.apache.ibatis.plugin.Interceptor` whose `plugin(target)` turns any `Executor` into a `Resilience4jExecutor` |
| `io.microsphere.resilience4j.mybatis.MyBatisResilience4jPlugin` | `Resilience4jPlugin` (name `mybatis`) that registers the interceptor as a Spring bean so MyBatis picks it up |

Where it hooks in: MyBatis builds a new `Executor` for every `SqlSession` and then runs `interceptorChain.pluginAll(executor)`. Because this plugin's interceptor returns a decorating `Executor` from `plugin(...)`, the decoration sits around the whole statement execution path — SQL parsing/parameter binding aside, it covers the JDBC call itself. One registration protects every mapper and every XML statement in that `Configuration`.

Compared to the [Alibaba Druid plugin](microsphere-resilience4j-alibaba-druid.md) (which names resources by SQL verb + table), this plugin names resources by **mapped statement id**, giving you per-mapper-method granularity.

## Maven coordinates and dependency wiring

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-mybatis</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

From `microsphere-resilience4j-mybatis/pom.xml`, the only non-optional dependency is the Spring layer of this library; every framework the plugin integrates with is `optional` and expected from the host application:

| Dependency | Scope in the plugin POM | Notes |
|------------|-------------------------|-------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile | pulls `microsphere-resilience4j-commons` (facades/templates) |
| `org.mybatis:mybatis` | **optional** | version is not pinned in this repo; the reactor resolves it through the imported `microsphere-mybatis-dependencies` BOM (`0.2.16`, see `microsphere-resilience4j-parent/pom.xml`) |
| `org.mybatis:mybatis-spring` | **optional** | only needed for the Spring registration path |
| `org.springframework:spring-context`, `spring-jdbc` | **optional** | the plugin's `plugin(...)` signature uses Spring bean-factory types |
| `io.github.resilience4j:resilience4j-spring`, `-annotations`, `-consumer`, `-framework-common`, `-core`, `-circuitbreaker`, `-ratelimiter`, `-retry`, `-bulkhead`, `-timelimiter` | **optional** | add the modules you actually use |
| `io.github.microsphere-projects:microsphere-spring-context` | **optional** | `BeanRegistrar` helper used by the plugin |
| `junit-jupiter`, `spring-test`, `microsphere-mybatis-spring`, `microsphere-spring-test`, `microsphere-mybatis-spring-test`, `h2`, `logback-classic`, `aspectjweaver` | **test** | used by the two test classes only |

## Version and compatibility

| Item | Requirement | Notes |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x` (`release`) | artifact `microsphere-resilience4j-mybatis` |
| Resilience4j | 2.0.0 – 2.4.0 | default profile `resilience4j-2.4` → `2.4.0` |
| MyBatis | `org.mybatis:mybatis` (application-supplied) | compiles against `Executor`, `MappedStatement`, `Interceptor`, `Invocation`, `Cursor`, `BatchResult`; `queryCursor` and `deferLoad` must exist in the `Executor` interface (MyBatis 3.5+ line as used by the build) |
| mybatis-spring | application-supplied | only for `@EnableMyBatis`-style registration |
| JDK | 17+ | root POM `java.version = 17` |
| Spring Framework / Spring Cloud | optional | Spring Cloud 2022.0.x – 2025.0.x via the parent line |

## Setup

### Option A — plain MyBatis (`Configuration.addInterceptor`)

This is exactly what `Resilience4jExecutorInterceptorTest` does in its `customize(Configuration)` hook:

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

From then on every `SqlSession` opened from that `Configuration` gets an `Executor` wrapped by `Resilience4jExecutor`.

### Option B — Spring container (`MyBatisResilience4jPlugin`)

Add the plugin name to any `@Enable*` annotation from `microsphere-resilience4j-spring`; `EnableResilience4jRegistrar` loads `Resilience4jPlugin` implementations from `META-INF/spring.factories` and calls `plugin(...)` for the names you list. `MyBatisResilience4jPlugin.plugin(...)` then registers a `Resilience4jExecutorInterceptor` bean definition wired to the facade bean `lazyResilience4jFacade`:

```java
@Configuration
@EnableBulkhead(plugins = MyBatisResilience4jPlugin.PLUGIN_NAME) // "mybatis"
@EnableCircuitBreaker
@EnableMyBatis                                                    // microsphere-mybatis-spring
@Import({MyBatisDataSourceTestConfiguration.class, MyBatisDataBaseTestConfiguration.class})
class DefaultConfig {
}
```

(The `@Import`ed configurations above are the test-scope DataSource/DDL helpers used by `MyBatisResilience4jPluginTest`; in an application you provide your own `DataSource` and mapper registration.)

Because the interceptor is registered as an ordinary Spring bean of type `org.apache.ibatis.plugin.Interceptor`, your MyBatis bootstrap must collect `Interceptor` beans into the `Configuration` (that is what `@EnableMyBatis` from `microsphere-mybatis-spring` does in the test). If you build the `SqlSessionFactory` by hand, pass the bean to `Configuration.addInterceptor(...)` or to `SqlSessionFactoryBean.setPlugins(...)`.

> Plugin names are matched against `Resilience4jPlugin#getName()` (see `EnableResilience4jRegistrar#registerPlugins`). The annotation Javadoc calls the attribute “Spring Bean names”, but the implementation compares the plugin **name**.

## Public API

### `Resilience4jExecutorInterceptor`

| Member | Behaviour (from source) |
|--------|--------------------------|
| `public static final String DEFAULT_ENTRY_NAME_PREFIX = "mybatis@"` | default prefix for entry names |
| `Resilience4jExecutorInterceptor(Resilience4jFacade facade)` | delegates to the 2-arg constructor with the default prefix |
| `Resilience4jExecutorInterceptor(Resilience4jFacade facade, String entryNamePrefix)` | stores both; no null checks |
| `Object plugin(Object target)` | `if (target instanceof Executor) return new Resilience4jExecutor((Executor) target, facade, entryNamePrefix); return target;` |
| `Object intercept(Invocation invocation)` | `return invocation.proceed();` — carries the comment *“Current method will be not executed”*; in this wiring MyBatis never builds an `Invocation` for it, because `plugin()` does not go through `Plugin.wrap(...)` |
| `getFacade()`, `getEntryNamePrefix()` | `@Nonnull` accessors |

There is deliberately **no `@Intercepts` / `@Signature`** declaration on this class — the interception target is selected in `plugin(...)`, not by method signature.

### `Resilience4jExecutor`

| Member | Behaviour |
|--------|-----------|
| `Resilience4jExecutor(Executor delegate, Resilience4jFacade facade, String entryNamePrefix)` | the only constructor; plain field assignment |
| `update(MappedStatement, Object)` | decorated |
| `query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)` | decorated |
| `query(MappedStatement, Object, RowBounds, ResultHandler)` | decorated |
| `queryCursor(MappedStatement, Object, RowBounds)` | decorated |
| `flushStatements()`, `commit(boolean)`, `rollback(boolean)`, `createCacheKey(...)`, `isCached(...)`, `clearLocalCache()`, `deferLoad(...)`, `getTransaction()`, `close(boolean)`, `isClosed()` | pure pass-through to the delegate — **not** decorated |
| `setExecutorWrapper(Executor executor)` | delegates, but wraps the argument first: `delegate.setExecutorWrapper(new Resilience4jExecutor(executor, facade, entryNamePrefix))` |
| `doInResilience4j(MappedStatement, ThrowableSupplier<T>)` | `protected <T> T ... throws SQLException` → `facade.call(buildEntryName(ms), callback, SQLException.class)` |
| `getFacade()`, `getEntryNamePrefix()` | `@Nonnull` accessors |

### Resilience4j entry name

```java
private String buildEntryName(MappedStatement ms) {
    return this.entryNamePrefix + ms.getId();
}
```

So with the default prefix the resource name is the fully-qualified mapped statement id, e.g.:

| Mapped statement | Entry name |
|------------------|------------|
| `io.microsphere.mybatis.test.mapper.UserMapper.getUserById` | `mybatis@io.microsphere.mybatis.test.mapper.UserMapper.getUserById` |
| `com.acme.repo.OrderMapper.insertOrder` | `mybatis@com.acme.repo.OrderMapper.insertOrder` |

Statement ids contain `.` characters, so when you configure per-entry Resilience4j settings through property binding, quote the key (YAML: `"mybatis@com.acme.repo.OrderMapper.insertOrder": …`). The binding prefixes used by `@Enable*` come from `Resilience4jConstants`, e.g. `microsphere.resilience4j.circuit-breaker.` and `microsphere.resilience4j.bulkhead.` — see the [spring module page](microsphere-resilience4j-spring.md).

## Complete runnable examples

### 1. Mapper call through the interceptor (adapted from `Resilience4jExecutorInterceptorTest`)

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

The upstream test asserts the mapper round-trip (`assertEquals(foundUser, user)`), the cursor/`flushStatements`/`commit(boolean)`/`rollback(boolean)`/`clearCache()` path, the constants (`DEFAULT_ENTRY_NAME_PREFIX == "mybatis@"`), and the getters (`assertSame(facade, interceptor.getFacade())`).

### 2. `intercept(Invocation)` — direct invocation (as the test does)

`intercept()` is reachable only if you call it yourself; `Resilience4jExecutorInterceptorTest.testInvoke` builds a `SimpleExecutor` and an `Invocation` for `Executor#isClosed` and asserts the proceed-through result:

```java
Method method = io.microsphere.reflect.MethodUtils.findMethod(Executor.class, "isClosed");
Invocation invocation = new Invocation(new SimpleExecutor(configuration, transaction), method, new Object[0]);
Object result = interceptor.intercept(invocation); // -> Boolean.FALSE, no decoration applied
```

## Behavioural notes and caveats

- **Open circuit during SQL execution.** `CircuitBreakerTemplate` throws `CallNotPermittedException`; the plugin calls `facade.call(name, callback, SQLException.class)`, so your mapper method throws a **`SQLException`** (wrapped by `io.microsphere.util.ExceptionUtils.wrap`), not a Resilience4j exception. Spring's `DataAccessException` translation sees a JDBC failure.
- **Cached reads are still decorated.** `Resilience4jExecutor.query(...)` is entered before MyBatis consults the first/second-level cache, so cache hits consume rate-limiter permits and count as circuit-breaker calls.
- **Transaction control is out of scope.** `flushStatements`, `commit`, `rollback`, `close` are pass-through. A circuit breaker cannot protect connection acquisition here — use the [Druid plugin](microsphere-resilience4j-alibaba-druid.md) for pool-level protection.
- **Double decoration is possible.** `plugin()` wraps the `Executor`, and `setExecutorWrapper(...)` wraps its argument again before delegating. Nested `Resilience4jExecutor` instances count the same logical statement more than once, which matters for `RateLimiter`/`Bulkhead` permits.
- **One wrapper per `SqlSession`.** MyBatis creates a new `Executor` per session, so `plugin(...)` runs per session; the Resilience4j entries themselves are shared (created lazily per entry name in the registries held by the facade).
- **Which modules actually apply.** This plugin uses the one-time `call(...)` path, so every template in the facade chain decorates the call — including `RetryTemplate`, `TimeLimiterTemplate` and `ThreadPoolBulkheadTemplate`, which do **not** support the two-phase `begin()/end()` path used by the Spring Web and Hibernate plugins. Note that `TimeLimiterTemplate` executes the callback on an `ExecutorService` thread, which moves JDBC/transaction-bound work off the calling thread.
- **Facade identity.** The plugin registers its interceptor bean with a constructor-arg **reference** to `LazyResilience4jFacade.BEAN_NAME` (`"lazyResilience4jFacade"`), so the facade is resolved lazily and its delegate is assembled when the context is refreshed (`ContextRefreshedEvent`). Do not call `plugin()`-registered interceptors before the context is refreshed.

## Related pages

- [Developer guide — microsphere-resilience4j-mybatis](../developer-guide/microsphere-resilience4j-mybatis.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
