# microsphere-resilience4j-alibaba-druid — User Guide (1.x)

> Version line `0.1.x` (`release-1.x` branch) · latest release `0.1.4` · current `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0** (default `1.7.0`) · Spring Cloud Hoxton – 2021.0.x · [← user guide index](README.md)

Module path in the source tree: `microsphere-resilience4j-plugins/microsphere-resilience4j-alibaba-druid`.

## Problem statement

When a database degrades or dies, applications usually fail in the worst possible way: threads pile up inside JDBC calls, the connection pool drains, and the outage propagates to every unrelated feature. Resilience4j solves this with circuit breakers, bulkheads and rate limiters — but those decorators have to be applied at a point where *every* SQL call passes through.

Doing it per DAO or per service is repetitive and easy to bypass (raw JDBC, JPA, MyBatis, reporting queries). This plugin moves the boundary one layer down, into the **Alibaba Druid connection pool** itself, so one registration protects all SQL executed on that `DataSource`.

## Hook point in Druid

The plugin hooks Druid's own **filter chain** (`com.alibaba.druid.filter.Filter`). From the source:

```java
public class Resilience4jDruidFilter extends FilterAdapter {
```

`FilterAdapter` is Druid's base adapter implementing the `Filter` SPI with pass-through defaults — **not** `FilterEventAdapter` (the higher-level event adapter with `statementExecute` / `statementUpdate` callbacks). Consequences you should know:

- The resilience boundary is opened around the *continuation of the Druid chain* (`super.<method>(chain, …)`), so every downstream filter and the real driver call sit inside it.
- Only statement execution is covered. Connection acquisition (`connection_connect`), result-set traversal and transaction callbacks are **not** overridden on this branch.
- The class Javadoc states the supported surface explicitly: `Resilience4jDruidFilter only supports Statement and PreparedStatement`. `CallableStatement` is not covered.

Typical uses:

| Goal | Module you configure on the facade | Entry name you protect |
|------|-----------------------------------|----------------------|
| Fail fast on a dead table | CircuitBreaker | `SELECT users`, `UPDATE users`, … |
| Cap concurrency for a hot table | Bulkhead | same |
| Throttle writes to a legacy table | RateLimiter | same |

## Maven coordinates

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-alibaba-druid</artifactId>
    <version>0.1.4</version>
    <!-- managed if you import microsphere-resilience4j-dependencies -->
</dependency>
```

The plugin's own POM declares exactly one mandatory dependency; everything else is `optional`:

| Dependency | Scope declared in `microsphere-resilience4j-alibaba-druid/pom.xml` | Note |
|------------|---------------------------------------------------------------------|------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile (non-optional) | transitively brings `microsphere-resilience4j-commons` |
| `com.alibaba:druid` | **optional** | you must provide it; version is resolved in this reactor through the imported `microsphere-alibaba-druid-dependencies` BOM `0.1.20` (property in `microsphere-resilience4j-parent/pom.xml`) |
| `org.springframework:spring-context` | **optional** | only needed for `DruidResilience4jPlugin` / the `@Enable*` annotations |
| `io.github.resilience4j:resilience4j-{core,circuitbreaker,ratelimiter,retry,bulkhead,timelimiter,annotations,consumer,framework-common}` | **optional** | `framework-common` carries an explicit `${resilience4j.version}` |
| `microsphere-spring-context`, `microsphere-annotation-processor` | **optional** | internal helpers |
| `junit-jupiter`, `spring-test`, `microsphere-spring-test`, `com.h2database:h2`, `ch.qos.logback:logback-classic` | **test** | used by `Resilience4jDruidFilterTest` |

## Compatibility

Verified from `.doc-tmp/release-1x/pom.xml`, `microsphere-resilience4j-parent/pom.xml` and `.github/workflows/maven-build.yml`:

| Item | Value on `release-1.x` | Evidence |
|------|------------------------|----------|
| microsphere-resilience4j | `0.1.x`; `${revision}` = `0.1.5-SNAPSHOT` | root `pom.xml` `<properties><revision>0.1.5-SNAPSHOT</revision></properties>` |
| Resilience4j | 1.3.0 / 1.4.0 / 1.5.0 / 1.6.0 / **1.7.0 (default)** | parent profiles `resilience4j-1.3` … `resilience4j-1.7`, the last `activeByDefault` |
| Build parent | `microsphere-spring-cloud-parent` **0.1.26** | root `pom.xml` `<parent>` |
| Spring Cloud | Hoxton – 2021.0.x | CI matrix `maven-profile-spring-cloud: ['spring-cloud-hoxton','spring-cloud-2020','spring-cloud-2021']` |
| Alibaba Druid | not pinned in this repository's POMs | declared `optional` without a `<version>`; the reactor resolves it via the imported BOM above |
| JDK baseline | **not verifiable from this snapshot** | no `maven.compiler.*`/`<source>`/`<target>` appears in any POM of the branch — it is inherited from the external parent 0.1.26. CI builds the matrix `java: ['8','11','17','21','25']`; `maven-publish.yml` sets up Java `11`. The guide index states "JDK 8"; treat 8 as the intended floor but confirm against the parent POM you actually build with. |

## API

### `io.microsphere.resilience4j.alibaba.druid.filter.Resilience4jDruidFilter`

```java
public class Resilience4jDruidFilter extends FilterAdapter {

    public static final String DEFAULT_ENTRY_NAME_PREFIX = "alibaba-druid@";

    public Resilience4jDruidFilter(Resilience4jFacade facade);
    public Resilience4jDruidFilter(Resilience4jFacade facade, String entryNamePrefix);

    @Override
    public void init(DataSourceProxy dataSource);

    @Override public boolean     preparedStatement_execute(FilterChain chain, PreparedStatementProxy statement);
    @Override public ResultSetProxy preparedStatement_executeQuery(FilterChain chain, PreparedStatementProxy statement);
    @Override public int         preparedStatement_executeUpdate(FilterChain chain, PreparedStatementProxy statement);

    @Override public ResultSetProxy statement_executeQuery(FilterChain chain, StatementProxy statement, String sql);
    @Override public boolean        statement_execute(FilterChain chain, StatementProxy statement, String sql);
    @Override public boolean        statement_execute(FilterChain chain, StatementProxy statement, String sql, int autoGeneratedKeys);
    @Override public boolean        statement_execute(FilterChain chain, StatementProxy statement, String sql, int[] columnIndexes);
    @Override public boolean        statement_execute(FilterChain chain, StatementProxy statement, String sql, String[] columnNames);
    @Override public int[]          statement_executeBatch(FilterChain chain, StatementProxy statement);
    @Override public int            statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql);
    @Override public int            statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, int autoGeneratedKeys);
    @Override public int            statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, int[] columnIndexes);
    @Override public int            statement_executeUpdate(FilterChain chain, StatementProxy statement, String sql, String[] columnNames);

    @Nonnull public final Resilience4jFacade getFacade();
    @Nonnull public final String            getEntryNamePrefix();

    protected final <T> T doInResilience4j(StatementProxy statement, Callable<T> callable) throws SQLException;
    public final String getEntryName(StatementProxy statement);
}
```

All `throws SQLException` clauses are as in the source (the overridden Druid signatures).

Notes that matter when you use the API:

- `init(DataSourceProxy)` stores the data source and, only when it is a `DruidDataSource`, its `getValidationQuery()`. That value later short-circuits SQL parsing for pool validation traffic, so `validationQuery` does not pollute your entry statistics.
- `getEntryName(StatementProxy)` reads `statement.getLastExecuteSql()`, parses it with `com.alibaba.druid.sql.SQLUtils.parseStatements(sql, dataSource.getDbType())` and dispatches on the first AST node.
- Entry names produced are of the form `SELECT <table>`, `UPDATE <table>`, `INSERT <table>`, `DELETE <table>`; anything unrecognised or unparsable becomes the literal `UNRECOGNIZED`.
- **`entryNamePrefix` is stored and exposed by the getter but never applied to the entry name on this branch.** `getEntryName(...)` returns the parsed resource name directly. Configure your Resilience4j instances under names like `SELECT users`, not `alibaba-druid@SELECT users`. (Contrast: the MyBatis and Hibernate plugins *do* prefix their entry names.)

### `io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin`

```java
public class DruidResilience4jPlugin implements Resilience4jPlugin {
    @Override public void plugin(ConfigurableListableBeanFactory beanFactory,
                                 BeanDefinitionRegistry registry,
                                 ConfigurableEnvironment environment) {
    }
    @Override public String getName() { return "alibaba-druid"; }
}
```

The `plugin(...)` body is **empty** on this branch: selecting the plugin only makes the name known to the SPI; it registers no filter bean for you. Filter registration stays manual (next section).

## Wiring / registration

There is no Spring Boot auto-configuration for this plugin on `release-1.x` (`microsphere-resilience4j-spring-boot` and `-spring-cloud` contain only a `pom.xml` and no sources in this snapshot). Two verified paths:

### 1. Programmatic (this is what `Resilience4jDruidFilterTest` does)

```java
BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

Resilience4jFacade facade = new ChainableResilience4jFacade(
        bulkheadRegistry, rateLimiterRegistry, retryRegistry, timeLimiterRegistry, circuitBreakerRegistry);

Resilience4jDruidFilter filter = new Resilience4jDruidFilter(facade);

DruidDataSource dataSource = new DruidDataSource();
dataSource.setUrl("jdbc:h2:mem:test_mem");
dataSource.setDriverClassName("org.h2.Driver");
dataSource.setUsername("sa");
dataSource.setProxyFilters(asList(filter));
dataSource.init();
```

`setProxyFilters(...)` + `init()` is the verified trigger for the filter's own `init(DataSourceProxy)` callback.

### 2. As a Spring bean

Declare the filter as a bean and hand it to your `DruidDataSource`. The class has **no no-arg constructor** — only the two `(Resilience4jFacade[, String])` constructors — so:

- Spring-style `<property>`/setter injection or Druid's class-name-based `filters=...` configuration cannot instantiate it. Use `@Bean` methods (or constructor-arg bean definitions) that pass a `Resilience4jFacade`.
- In a Spring-context application, the facade you should reference is the one the Spring module registers as `LazyResilience4jFacade.BEAN_NAME` = `"lazyResilience4jFacade"` (a primary bean built by the `@Enable*` registrars).

### 3. The `Resilience4jPlugin` SPI

`META-INF/spring.factories` declares the descriptor:

```properties
io.microsphere.resilience4j.spring.common.Resilience4jPlugin=\
io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin
```

`EnableResilience4jRegistrar.registerPlugins(...)` loads all `Resilience4jPlugin` factories and calls `plugin(...)` only when the annotation's `plugins` attribute contains `getName()`. Adding `plugins = "alibaba-druid"` to e.g. `@EnableBulkhead` is therefore **harmless but useless today** — the body is empty.

## Full example (from the real test)

`Resilience4jDruidFilterTest` covers every overridden execution shape. Excerpt of the statement-path test, verbatim from the source:

```java
@Test
void testExecuteStatement() throws Throwable {
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
```

The prepared-statement test (`testExecutePreparedStatement`) exercises `executeUpdate()`, `executeQuery()` and `execute()` on `PreparedStatement`s created with `prepareStatement(sql)`, `prepareStatement(sql, TYPE_FORWARD_ONLY)` and `prepareStatement(sql, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)`.

The entry names those statements resolve to are `INSERT users`, `UPDATE users`, `SELECT users`, `DELETE users` (and `CREATE TABLE …` / `DROP TABLE …` → `UNRECOGNIZED`, since only select/update/insert/delete AST nodes are mapped).

## Behavioral notes

1. **Failures surface as `SQLException`.** `doInResilience4j` calls `facade.call(entryName, callable::call, SQLException.class)`; `Resilience4jOperations` wraps any `Throwable` into the declared throwable type via `io.microsphere.util.ExceptionUtils.wrap`. An open circuit breaker therefore reaches your JDBC caller as a `SQLException`, which is what makes the plugin transparent to JPA/MyBatis callers.
2. **Which modules actually decorate the call** depends on the registries behind your facade. With `ChainableResilience4jFacade`, `call(...)` builds a `CallbackChain` over the priority-sorted templates (Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead, `Resilience4jModule.defaultAspectOrder`).
3. **Entry names are computed per call** by parsing SQL with the Druid parser. This is CPU cost on every statement; the parser result is not cached on this branch. Set a `validationQuery` to keep pool-validation traffic out of your statistics.
4. **Naming is table-based, not statement-based.** Two different `SELECT` statements against the same table share one circuit breaker instance. There is no hook to customise naming other than subclassing the individual `statement_*` overrides — `getEntryName(StatementProxy)` and `doInResilience4j(...)` are both `final`.
5. **`dataSource.getDbType()` is required** for parsing; the filter keeps the `DataSourceProxy` captured in `init(...)`. If `init` were never called by the pool, `getEntryName` would NPE on `dataSource.getDbType()` (except when the SQL equals the cached `validationSQL`). Druid always calls `Filter.init` while building proxy filters, so in practice this only matters for unit tests that instantiate the filter directly.
6. **The plugin descriptor is a stub.** Do not expect `@Enable*(plugins = "alibaba-druid")` to register anything.

## Related pages

- [Developer guide — this module](../developer-guide/microsphere-resilience4j-alibaba-druid.md)
- [User guide — commons](microsphere-resilience4j-commons.md) (facade and templates)
- [User guide — spring](microsphere-resilience4j-spring.md) (`@Enable*`, `lazyResilience4jFacade`, plugin SPI)
