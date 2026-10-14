# microsphere-resilience4j-alibaba-druid — User Guide

> Version line `0.2.x` (`release` branch) · latest release `0.2.4` · current `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0** (default `2.4.0`) · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← user guide index](README.md)

## What this plugin does

`microsphere-resilience4j-alibaba-druid` puts Resilience4j decoration **inside the Alibaba Druid JDBC connection pool**, at the point where a SQL statement is actually executed. It ships one production class:

| Class | Role |
|-------|------|
| `io.microsphere.resilience4j.alibaba.druid.filter.Resilience4jDruidFilter` | A Druid `Filter` that wraps every `Statement` / `PreparedStatement` execution in `Resilience4jFacade.call(...)` |
| `io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin` | A `Resilience4jPlugin` descriptor (name `alibaba-druid`) — see [its current limitation](#druidresilience4jplugin-is-a-no-op) |

The hook point is Druid's own filter chain (`com.alibaba.druid.filter.Filter`), not a JDBC driver proxy and not Spring AOP. `Resilience4jDruidFilter` extends `com.alibaba.druid.filter.FilterAdapter` and overrides the `statement_*` / `preparedStatement_*` execution methods, delegating to `super.*` **inside** the resilience boundary. Everything above the pool — JDBC, JPA, MyBatis, plain `DriverManager` on a Druid `DataSource` — is therefore protected by a single registration.

Typical problems it solves:

- Fail fast when the database behind one table is dead, instead of letting every request pile up on SQL calls.
- Cap concurrency per SQL operation (`Bulkhead`) so a slow table cannot exhaust the pool.
- Rate-limit traffic to a specific table/operation (`RateLimiter`).

## Maven coordinates and dependency wiring

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-alibaba-druid</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

The module's own `pom.xml` declares exactly one **mandatory** (non-optional, non-test) dependency; everything else is `optional` and must be provided by your application:

| Dependency | Scope in the plugin POM | What you must do |
|------------|-------------------------|------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile (transitively pulls `microsphere-resilience4j-commons`) | nothing — it comes with the plugin |
| `com.alibaba:druid` | **optional** | you already have it; version comes from your app (the reactor resolves it through the imported `microsphere-alibaba-druid-dependencies` BOM, `0.2.20`, declared in `microsphere-resilience4j-parent/pom.xml`) |
| `org.springframework:spring-context` | **optional** | needed only if you use `DruidResilience4jPlugin` / `@Enable*` annotations |
| `io.github.resilience4j:resilience4j-core`, `-circuitbreaker`, `-ratelimiter`, `-retry`, `-bulkhead`, `-timelimiter`, `-annotations`, `-consumer`, `-framework-common` | **optional** (`-framework-common` is the only one with an explicit `${resilience4j.version}`) | add the modules you actually use; usually pulled in by `resilience4j-spring-boot` / your own declarations |
| `io.github.microsphere-projects:microsphere-spring-context`, `microsphere-annotation-processor` | **optional** | internal (Spring bean-registration helpers used by other plugins) |
| `org.junit.jupiter:junit-jupiter`, `org.springframework:spring-test`, `microsphere-spring-test`, `com.h2database:h2`, `ch.qos.logback:logback-classic` | **test** | only used by `Resilience4jDruidFilterTest` |

Because `resilience4j-*` is `optional`, a plain (non-Spring) application only needs the plugin + `druid` + the Resilience4j modules whose registries you construct.

## Version and compatibility

| Item | Requirement | Notes |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x` (`release`) | artifact `microsphere-resilience4j-alibaba-druid`, `${revision}`-flattened version |
| Resilience4j | 2.0.0 – 2.4.0 | default profile `resilience4j-2.4` → `2.4.0` |
| Alibaba Druid | `com.alibaba:druid` (not pinned by this repo) | the plugin compiles against `FilterAdapter`, `DruidDataSource`, `*Proxy`, and the Druid SQL parser (`com.alibaba.druid.sql.SQLUtils`) |
| JDK | 17+ | root POM property `java.version = 17` |
| Spring Framework | optional | only for the `Resilience4jPlugin` descriptor |
| Database driver | any | tested in-repo against H2 (`jdbc:h2:mem:test_mem`, `org.h2.Driver`) |

## Setup

### 1. Build a `Resilience4jFacade`

The filter does not create registries; it takes a facade. In plain Java the test builds one over the five registries:

```java
BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

Resilience4jFacade facade = new ChainableResilience4jFacade(
        bulkheadRegistry, rateLimiterRegistry, retryRegistry, timeLimiterRegistry, circuitBreakerRegistry);
```

In a Spring application, `@EnableCircuitBreaker` / `@EnableBulkhead` / … register a primary `lazyResilience4jFacade` bean of type `Resilience4jFacade` that you can inject.

### 2. Create the filter and register it with the pool

`Resilience4jDruidFilter` is added to the Druid `DataSource` as a proxy filter **before** `init()`:

```java
Resilience4jDruidFilter filter = new Resilience4jDruidFilter(facade);

DruidDataSource dataSource = new DruidDataSource();
dataSource.setUrl("jdbc:h2:mem:test_mem");
dataSource.setDriverClassName("org.h2.Driver");
dataSource.setUsername("sa");
dataSource.setProxyFilters(Collections.singletonList(filter)); // replaces the filter list
dataSource.init();                                              // triggers Filter.init(DataSourceProxy)
```

> If your `DruidDataSource` already has filters, add this one to the existing list instead of overwriting it — `setProxyFilters` replaces the whole collection.

### <a id="druidresilience4jplugin-is-a-no-op"></a>3. `DruidResilience4jPlugin` is a no-op — do not expect auto-registration

`DruidResilience4jPlugin` is loaded through `META-INF/spring.factories` under the `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` key and reports `getName() == "alibaba-druid"`, but its `plugin(...)` method body is **empty** in this version:

```java
@Override
public void plugin(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
                   ConfigurableEnvironment environment) {
}
```

So `@EnableBulkhead(plugins = "alibaba-druid")` (or any other `@Enable*` with that plugin name) matches the plugin and does nothing. **Register the filter yourself** (step 2), and construct it from the container so it receives the Spring-managed facade:

```java
@Bean
public Resilience4jDruidFilter resilience4jDruidFilter(Resilience4jFacade facade, DruidDataSource dataSource) {
    Resilience4jDruidFilter filter = new Resilience4jDruidFilter(facade);
    dataSource.setProxyFilters(Collections.singletonList(filter));
    return filter;
}
```

## Public API

### `Resilience4jDruidFilter` (`extends com.alibaba.druid.filter.FilterAdapter`)

| Member | Signature / behaviour |
|--------|------------------------|
| Constant | `public static final String DEFAULT_ENTRY_NAME_PREFIX = "alibaba-druid@"` |
| Constructor | `Resilience4jDruidFilter(Resilience4jFacade facade)` — delegates to the two-arg constructor with the default prefix |
| Constructor | `Resilience4jDruidFilter(Resilience4jFacade facade, String entryNamePrefix)` — stores both (no null checks) |
| `init(DataSourceProxy dataSource)` | Overrides `Filter.init`; keeps the `DataSourceProxy` and, when it is a `DruidDataSource`, caches `getValidationQuery()` as `validationSQL` |
| `getFacade()` | `@Nonnull public final Resilience4jFacade getFacade()` |
| `getEntryNamePrefix()` | `@Nonnull public final String getEntryNamePrefix()` |
| `getEntryName(StatementProxy)` | `public final String` — the resource-name resolution algorithm below |
| `doInResilience4j(StatementProxy, Callable<T>)` | `protected final <T> T ... throws SQLException` — the single decoration point: `facade.call(entryName, callable::call, SQLException.class)` |

Overridden Druid methods (each one calls `super.<method>(...)` through `doInResilience4j`):

| `PreparedStatement` | `Statement` |
|---------------------|-------------|
| `preparedStatement_execute` | `statement_execute` (4 overloads: `sql`, `sql+autoGeneratedKeys`, `sql+int[]`, `sql+String[]`) |
| `preparedStatement_executeQuery` | `statement_executeUpdate` (4 overloads, same variants) |
| `preparedStatement_executeUpdate` | `statement_executeQuery`, `statement_executeBatch` |

Not decorated: `CallableStatement` execution, `ResultSet*` methods, connection acquisition/commit/rollback, and Druid's other filter callbacks. The class Javadoc states this explicitly: *“Resilience4jDruidFilter only supports `Statement` and `PreparedStatement`”*.

### How the Resilience4j entry (resource) name is chosen

`getEntryName(StatementProxy)` reads `statement.getLastExecuteSql()` and then:

| Situation | Entry name |
|-----------|------------|
| SQL equals the cached `validationSQL` of the `DruidDataSource` | the raw validation SQL itself (no parsing) |
| `SELECT …` parsed by `SQLUtils.parseStatements(sql, dbType)` | `"SELECT " + <first query block's FROM table source>.computeAlias()` |
| `UPDATE …` | `"UPDATE " + <table source>.computeAlias()` |
| `INSERT …` | `"INSERT " + <table source>.computeAlias()` |
| `DELETE …` | `"DELETE " + <table source>.computeAlias()` |
| No statement parsed, an unsupported statement type, or a parse error inside the four branches | `"UNRECOGNIZED"` (a `debug` log line is emitted) |

The parse uses `dbType` from the injected `DataSourceProxy`, and only the **first** parsed `SQLStatement` is used. Consequence: all statements touching the same table with the same verb share one CircuitBreaker/Bulkhead/RateLimiter instance, and multi-statement SQL is attributed to its first statement.

> Note: `entryNamePrefix` (`"alibaba-druid@"`) is stored, exposed by `getEntryNamePrefix()` and asserted in the test, but `getEntryName(...)` **does not prepend it** — the Resilience4j entry names are the plain `"SELECT users"`-style strings. Do not rely on the prefix in this version.

## Complete runnable example (adapted from `Resilience4jDruidFilterTest`)

```java
import com.alibaba.druid.filter.Filter;
import com.alibaba.druid.pool.DruidDataSource;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.alibaba.druid.filter.Resilience4jDruidFilter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collections;

public class DruidResilience4jDemo {

    public static void main(String[] args) throws Throwable {
        Resilience4jFacade facade = new ChainableResilience4jFacade(
                BulkheadRegistry.ofDefaults(),
                RateLimiterRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                TimeLimiterRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());

        Filter filter = new Resilience4jDruidFilter(facade);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:mem:test_mem");
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUsername("sa");
        dataSource.setProxyFilters(Collections.singletonList(filter));
        dataSource.init();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute("CREATE TABLE users (id INT, name VARCHAR(50))", Statement.NO_GENERATED_KEYS);

            // every one of these goes through facade.call("INSERT users", …, SQLException.class)
            System.out.println(statement.executeUpdate("INSERT INTO users (id, name) VALUES (1, 'Mercy')"));
            System.out.println(statement.executeUpdate("INSERT INTO users (id, name) VALUES (2, 'Blitz')", new String[0]));
            System.out.println(statement.executeUpdate("INSERT INTO users (id, name) VALUES (3, 'Ma')", new int[0]));

            statement.addBatch("INSERT INTO users (id, name) VALUES (5, 'Z')");
            statement.addBatch("UPDATE users set name = 'z' WHERE id = 5");
            System.out.println(java.util.Arrays.toString(statement.executeBatch()));

            ResultSet resultSet = statement.executeQuery("SELECT id,name FROM users"); // entry: "SELECT users"
            while (resultSet.next()) {
                System.out.println(resultSet.getInt(1) + " -> " + resultSet.getString(2));
            }
            statement.execute("DELETE FROM users WHERE id = 1");                        // entry: "DELETE users"

            try (PreparedStatement ps = connection.prepareStatement("SELECT id,name FROM users WHERE id=?")) {
                ps.setInt(1, 2);
                System.out.println(ps.executeQuery() != null);                          // entry: "SELECT users"
            }
        } finally {
            dataSource.close();
        }
    }
}
```

The upstream test (`src/test/java/io/microsphere/resilience4j/alibaba/druid/filter/Resilience4jDruidFilterTest.java`) additionally asserts the constants (`DEFAULT_ENTRY_NAME_PREFIX == "alibaba-druid@"`) and the getters (`assertSame(facade, filter.getFacade())`, `assertEquals(DEFAULT_ENTRY_NAME_PREFIX, filter.getEntryNamePrefix())`).

## Behavioural notes and caveats

- **Open circuit during SQL execution.** With the default `CircuitBreakerRegistry.ofDefaults()` configuration, an OPEN breaker makes `CircuitBreakerTemplate` throw `CallNotPermittedException`. Because the filter decorates with `facade.call(entryName, callback, SQLException.class)`, that failure is re-thrown to the JDBC caller as a **`SQLException`**, not as a Resilience4j exception. Code that branches on `CallNotPermittedException` (or on `instanceof` Resilience4j types) will not fire — inspect the wrapped failure instead.
- **`init()` must have run.** `dataSource` and `validationSQL` are only assigned in `init(DataSourceProxy)`. Druid calls it when the filter is registered during `DruidDataSource.init()`; a filter instance used before that would call `getEntryName` with a `null` `dataSource`/`dbType`.
- **Every execution parses SQL.** `SQLUtils.parseStatements` runs on each intercepted call; for latency-sensitive pools prefer coarse entry names or a different hook. Parse failures are swallowed at `debug` level and collapse into the shared `"UNRECOGNIZED"` entry — which then shares one breaker for all unparsable traffic.
- **Pool validation traffic is isolated** by design: the validation query keeps its own entry name, so health checks cannot trip the table-level breakers.
- **Thread-hopping if `TimeLimiter` is in the chain.** `TimeLimiterTemplate.call(...)` submits the callback to an `ExecutorService`. Since a JDBC `Connection`/transaction is thread-bound, adding a `TimeLimiterRegistry` to a facade used by this filter executes the SQL on a different thread. The test above builds a facade with a `TimeLimiterRegistry`; drop it if you rely on thread-bound transactions.
- **Two-phase templates are the ones that matter for `begin`/`end`** — see the developer guide page for the full chain semantics.
- **Batch semantics.** `statement_executeBatch` is decorated once for the whole batch (entry name comes from `getLastExecuteSql()` of the statement), not per batch element.

## Related pages

- [Developer guide — microsphere-resilience4j-alibaba-druid](../developer-guide/microsphere-resilience4j-alibaba-druid.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md) — `Resilience4jFacade`, `ChainableResilience4jFacade`
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md) — `@Enable*` annotations, `lazyResilience4jFacade`, `Resilience4jPlugin` SPI
