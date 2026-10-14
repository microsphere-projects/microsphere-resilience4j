# microsphere-resilience4j-mybatis — User Guide (1.x)

> Version line `0.1.x` (`release-1.x` branch) · latest release `0.1.4` · current `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0** (default `1.7.0`) · Spring Cloud Hoxton – 2021.0.x · [← user guide index](README.md)

Module path in the source tree: `microsphere-resilience4j-plugins/microsphere-resilience4j-mybatis`.

## Problem statement

MyBatis applications normally protect individual repository methods, which means either annotating every mapper call or writing a wrapper around every DAO. Neither scales, and neither covers the mapper methods you forgot.

This plugin puts resilience at MyBatis' single choke point: the `org.apache.ibatis.executor.Executor`, through which **every** mapped statement (select / insert / update / delete / cursor) must pass. One interceptor then covers all mappers and all `SqlSession` calls of a `Configuration`.

## Hook point in MyBatis

Two collaborating pieces, both real classes of this module:

| Class | MyBatis contract | What it does |
|-------|------------------|--------------|
| `io.microsphere.resilience4j.mybatis.plugin.Resilience4jExecutorInterceptor` | `implements org.apache.ibatis.plugin.Interceptor` | Decides *whether* a target is decorated, in `plugin(Object target)` |
| `io.microsphere.resilience4j.mybatis.executor.Resilience4jExecutor` | `implements org.apache.ibatis.executor.Executor` | The decorator that actually opens/closes the resilience boundary |

Important difference from typical MyBatis plugins, and it is verifiable in the source: **`Resilience4jExecutorInterceptor` carries no `@Intercepts`/`@Signature` annotation and does not use `org.apache.ibatis.plugin.Plugin.wrap(...)`.** Its `plugin()` returns a hand-built decorator:

```java
@Override
public Object plugin(Object target) {
    if (target instanceof Executor) {
        return new Resilience4jExecutor((Executor) target, facade, entryNamePrefix);
    }
    return target;
}
```

`@Intercepts`/`@Signature` are only required by MyBatis' `Plugin.wrap()` JDK-proxy machinery, which this interceptor deliberately bypasses. The consequence is that `intercept(Invocation)` is **never** called during normal MyBatis execution; its body is a pass-through with an explicit comment:

```java
@Override
public Object intercept(Invocation invocation) throws Throwable {
    // Current method will be not executed
    return invocation.proceed();
}
```

## Maven coordinates

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-mybatis</artifactId>
    <version>0.1.4</version>
    <!-- managed if you import microsphere-resilience4j-dependencies -->
</dependency>
```

The module POM declares one mandatory dependency (`microsphere-resilience4j-spring`); all framework dependencies are `optional`:

| Dependency | Declared scope |
|------------|----------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile (non-optional) |
| `org.mybatis:mybatis`, `org.mybatis:mybatis-spring` | **optional** — you provide them |
| `io.github.resilience4j:resilience4j-spring`, `-annotations`, `-consumer`, `-framework-common`, `-core`, `-circuitbreaker`, `-ratelimiter`, `-retry`, `-bulkhead`, `-timelimiter` | **optional** |
| `org.springframework:spring-context`, `spring-jdbc` | **optional** |
| `io.github.microsphere-projects:microsphere-spring-context`, `microsphere-annotation-processor` | **optional** |
| `junit-jupiter`, `spring-test`, `microsphere-mybatis-spring`, `microsphere-spring-test`, `microsphere-mybatis-spring-test`, `h2`, `logback-classic`, `aspectjweaver` | **test** |

## Compatibility

| Item | Value on `release-1.x` | Evidence |
|------|------------------------|----------|
| microsphere-resilience4j | `0.1.x`, `${revision}` = `0.1.5-SNAPSHOT`, latest release `0.1.4` | root `pom.xml` |
| Resilience4j | 1.3.0 – 1.7.0, default `1.7.0` | `microsphere-resilience4j-parent/pom.xml` profiles `resilience4j-1.3` … `resilience4j-1.7` (`1.7` is `activeByDefault`) |
| Spring Cloud | Hoxton – 2021.0.x | CI matrix `maven-profile-spring-cloud` in `.github/workflows/maven-build.yml` |
| MyBatis / mybatis-spring | **not pinned in this repository** | `org.mybatis:mybatis` and `org.mybatis:mybatis-spring` are declared `optional` without a version; the reactor resolves them through the imported `microsphere-mybatis-dependencies` BOM, property `microsphere-mybatis.version = 0.1.15` |
| JDK | not pinned in this snapshot | no compiler properties in any branch POM; inherited from `microsphere-spring-cloud-parent` **0.1.26**. CI matrix runs `java: ['8','11','17','21','25']`, publishing uses Java `11`. The index's "JDK 8" is an intent statement, not a POM fact. |
| Test database | H2 (`com.h2database:h2`, test scope) | module `pom.xml` |

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

**Only four `Executor` operations are decorated**: `update`, both `query` overloads and `queryCursor`. `flushStatements`, `commit`, `rollback`, cache and transaction methods are straight delegations — batch flushing and transaction control are *not* subject to circuit breaking or bulkheading on this branch.

Entry names: `buildEntryName(MappedStatement ms)` returns `entryNamePrefix + ms.getId()`, i.e. by default

```
mybatis@io.microsphere.mybatis.test.mapper.UserMapper.getUserById
```

The prefix **is** applied here (unlike the Druid filter). Instance names you configure in Resilience4j must match the full `mybatis@<statementId>` string.

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

Unlike its Druid counterpart, this `plugin(...)` does real work: it registers a `Resilience4jExecutorInterceptor` bean definition whose **constructor argument is a reference to the bean named `lazyResilience4jFacade`** (`LazyResilience4jFacade.BEAN_NAME`).

## Wiring / registration

Pick one of the following. All three are grounded in the module's source or tests.

### 1. Spring: `@Enable*` with the plugin name (what the test does)

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

Any `@Enable*` annotation of `microsphere-resilience4j-spring` exposes `plugins()` (aliased from `@EnableResilience4j`). The registrar loads `Resilience4jPlugin` factories from `META-INF/spring.factories` and calls `plugin(...)` when `plugins` contains `getName()`. So `plugins = "mybatis"` is what causes the interceptor bean to be registered:

```properties
# microsphere-resilience4j-mybatis/src/main/resources/META-INF/spring.factories
io.microsphere.resilience4j.spring.common.Resilience4jPlugin=\
io.microsphere.resilience4j.mybatis.MyBatisResilience4jPlugin
```

You must still have the `@Enable*` module annotations active (`@EnableBulkhead` above, plus e.g. `@EnableCircuitBreaker`) so that `lazyResilience4jFacade` exists and holds the registries you want.

### 2. Programmatic MyBatis `Configuration` (what `Resilience4jExecutorInterceptorTest` does)

```java
@Override
protected void customize(Configuration configuration) {
    this.facade = createResilience4jFacade();
    this.interceptor = createResilience4jExecutorInterceptor(this.facade);
    configuration.addInterceptor(this.interceptor);
}
```

with the facade built over the five registries:

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

### 3. Plain `@Bean` registration

Declaring `Resilience4jExecutorInterceptor` as a Spring bean and letting your `SqlSessionFactory` collect it is the conventional mybatis-spring approach — **but note two source-level constraints**: the interceptor has no no-arg constructor (only `(Resilience4jFacade)` and `(Resilience4jFacade, String)`), so it must be built with a `@Bean` method or constructor-arg wiring; and whether a given `SqlSessionFactory` implementation auto-detects `Interceptor` beans is a property of MyBatis-Spring / `@EnableMyBatis` (external `microsphere-mybatis-spring` 0.1.15), not of this repository. Verify it in your own stack.

For the same reason, classic `mybatis-config.xml` `<plugins><plugin interceptor="…"/></plugins>` will **not** work as-is: MyBatis instantiates that element reflectively via a default constructor and feeds `Properties`, and this class offers neither.

## Full example (from the real tests)

`Resilience4jExecutorInterceptorTest` extends `AbstractMyBatisTest` (imported as `io.microsphere.mybatis.test.AbstractMyBatisTest`, i.e. from a Microsphere MyBatis test artifact declared at test scope in this POM — which exact artifact provides it is not verifiable from this repository) and registers the interceptor through `Configuration.addInterceptor`. Its mapper test then exercises both decorated paths:

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

`MyBatisResilience4jPluginTest` covers the Spring path: it starts a context with the configuration shown above, resolves the `SqlSessionFactory` by `SQL_SESSION_FACTORY_BEAN_NAME`, opens a session, and runs the mapper (`assertUserMapper(userMapper)`).

## Behavioral notes

1. **Failures keep their SQL type.** `Resilience4jExecutor.doInResilience4j` uses `facade.call(entryName, callback, SQLException.class)`; `Resilience4jOperations` re-wraps any `Throwable` into `SQLException` (`io.microsphere.util.ExceptionUtils.wrap`). An open circuit breaker therefore appears to MyBatis as a normal `SQLException`. If your stack configures MyBatis-Spring's exception translation, that `SQLException` is then converted on the caller's side into a `DataAccessException` — behaviour of MyBatis-Spring, not of this plugin.
2. **Entry names are per statement id.** One `MappedStatement` id = one Resilience4j instance, so `SELECT` and `UPDATE` statements of the same mapper method are naturally separate. Namespace changes (moving a mapper method) change the protected resource name.
3. **Interceptor is applied through MyBatis' `InterceptorChain.pluginAll`** — because `plugin()` returns the decorator only for `Executor` targets, other target types (`StatementHandler`, `ParameterHandler`, `ResultSetHandler`) pass through untouched.
4. **`setExecutorWrapper` re-wraps.** `delegate.setExecutorWrapper(new Resilience4jExecutor(executor, facade, entryNamePrefix))` — the incoming executor is decorated again before being handed to the delegate. If MyBatis calls this with an already-decorated executor you get nested decoration (and therefore nested counting). Flag this as a behavioural quirk; no test on this branch covers it.
5. **Bulkhead/CircuitBreaker state is only held for the duration of the decorated call** — `call(name, supplier)` runs `begin` before and `end` after the delegate invocation, so retry/time-limiter templates are also in play: `RetryTemplate` and `TimeLimiterTemplate` participate only in the one-time `call` path (`isBeginSupported()`/`isEndSupported()` return `false`), which is exactly the path this plugin uses.
6. **No Spring Boot auto-configuration exists for this plugin** on `release-1.x` (`microsphere-resilience4j-spring-boot` has only a `pom.xml`, no sources).

## Related pages

- [Developer guide — this module](../developer-guide/microsphere-resilience4j-mybatis.md)
- [User guide — commons](microsphere-resilience4j-commons.md)
- [User guide — spring](microsphere-resilience4j-spring.md)
- [User guide — alibaba druid](microsphere-resilience4j-alibaba-druid.md) (the lower-level JDBC alternative)
