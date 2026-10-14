# microsphere-resilience4j-alibaba-druid — 用户指南

> 简体中文 | [English](../microsphere-resilience4j-alibaba-druid.md)

> 版本线 `0.2.x`（`release` 分支）· 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0**（默认 `2.4.0`）· Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← 用户指南索引](README.md)

## 本插件的作用

`microsphere-resilience4j-alibaba-druid` 把 Resilience4j 的装饰能力放进 **Alibaba Druid JDBC 连接池内部**，也就是 SQL 语句真正被执行的那个点。它只提供一个生产类：

| 类 | 角色 |
|-------|------|
| `io.microsphere.resilience4j.alibaba.druid.filter.Resilience4jDruidFilter` | 一个 Druid `Filter`，把每一次 `Statement` / `PreparedStatement` 执行都包装在 `Resilience4jFacade.call(...)` 之中 |
| `io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin` | 一个 `Resilience4jPlugin` 描述符（名称 `alibaba-druid`）——参见[它当前的局限](#druidresilience4jplugin-is-a-no-op) |

钩挂点是 Druid 自己的过滤器链（`com.alibaba.druid.filter.Filter`），而不是 JDBC 驱动代理，也不是 Spring AOP。`Resilience4jDruidFilter` 继承 `com.alibaba.druid.filter.FilterAdapter`，重写 `statement_*` / `preparedStatement_*` 执行方法，并**在**弹性边界**之内**委托给 `super.*`。因此连接池之上的一切——JDBC、JPA、MyBatis、在 Druid `DataSource` 上直接使用 `DriverManager`——都只需一次注册即可得到保护。

它解决的典型问题：

- 当某张表背后的数据库挂掉时快速失败，而不是让所有请求都堆积在 SQL 调用上。
- 限制每个 SQL 操作的并发度（`Bulkhead`），使一张慢表无法耗尽连接池。
- 对访问特定表/操作的流量做限流（`RateLimiter`）。

## Maven 坐标与依赖装配

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-alibaba-druid</artifactId>
    <!-- version managed by microsphere-resilience4j-dependencies, otherwise: 0.2.4 -->
</dependency>
```

该模块自身的 `pom.xml` 只声明了一个**强制**（非 optional、非 test）依赖；其余全部是 `optional`，必须由你的应用提供：

| 依赖 | 在插件 POM 中的作用域 | 你需要做什么 |
|------------|-------------------------|------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile（传递引入 `microsphere-resilience4j-commons`） | 无需操作——它随插件一起提供 |
| `com.alibaba:druid` | **optional** | 你本来就有它；版本由你的应用决定（reactor 通过导入的 `microsphere-alibaba-druid-dependencies` BOM 解析它，`0.2.20`，声明在 `microsphere-resilience4j-parent/pom.xml`） |
| `org.springframework:spring-context` | **optional** | 仅当你使用 `DruidResilience4jPlugin` / `@Enable*` 注解时才需要 |
| `io.github.resilience4j:resilience4j-core`、`-circuitbreaker`、`-ratelimiter`、`-retry`、`-bulkhead`、`-timelimiter`、`-annotations`、`-consumer`、`-framework-common` | **optional**（`-framework-common` 是唯一显式写了 `${resilience4j.version}` 的） | 添加你实际使用的模块；通常由 `resilience4j-spring-boot` 或你自己的声明引入 |
| `io.github.microsphere-projects:microsphere-spring-context`、`microsphere-annotation-processor` | **optional** | 内部使用（其他插件用到的 Spring bean 注册辅助工具） |
| `org.junit.jupiter:junit-jupiter`、`org.springframework:spring-test`、`microsphere-spring-test`、`com.h2database:h2`、`ch.qos.logback:logback-classic` | **test** | 仅 `Resilience4jDruidFilterTest` 使用 |

因为 `resilience4j-*` 是 `optional`，一个纯 Java（非 Spring）应用只需要插件本身 + `druid` + 你所构造的 Resilience4j 模块。

## 版本与兼容性

| 条目 | 要求 | 说明 |
|------|-------------|-------|
| microsphere-resilience4j | `0.2.x`（`release`） | 构件 `microsphere-resilience4j-alibaba-druid`，`${revision}` 扁平化后的版本 |
| Resilience4j | 2.0.0 – 2.4.0 | 默认 profile `resilience4j-2.4` → `2.4.0` |
| Alibaba Druid | `com.alibaba:druid`（本仓库不锁定版本） | 插件针对 `FilterAdapter`、`DruidDataSource`、`*Proxy` 以及 Druid SQL 解析器（`com.alibaba.druid.sql.SQLUtils`）编译 |
| JDK | 17+ | 根 POM 属性 `java.version = 17` |
| Spring Framework | optional | 仅 `Resilience4jPlugin` 描述符需要 |
| 数据库驱动 | 任意 | 仓库内使用 H2 测试（`jdbc:h2:mem:test_mem`、`org.h2.Driver`） |

## 配置

### 1. 构建一个 `Resilience4jFacade`

过滤器并不创建注册表，它接收一个门面。在纯 Java 场景下，测试类基于五个注册表构造了一个：

```java
BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

Resilience4jFacade facade = new ChainableResilience4jFacade(
        bulkheadRegistry, rateLimiterRegistry, retryRegistry, timeLimiterRegistry, circuitBreakerRegistry);
```

在 Spring 应用中，`@EnableCircuitBreaker` / `@EnableBulkhead` / … 会注册一个类型为 `Resilience4jFacade`、名为 `lazyResilience4jFacade` 的 primary bean，你可以直接注入。

### 2. 创建过滤器并注册到连接池

`Resilience4jDruidFilter` 作为代理过滤器在 `init()` **之前**被加入 Druid `DataSource`：

```java
Resilience4jDruidFilter filter = new Resilience4jDruidFilter(facade);

DruidDataSource dataSource = new DruidDataSource();
dataSource.setUrl("jdbc:h2:mem:test_mem");
dataSource.setDriverClassName("org.h2.Driver");
dataSource.setUsername("sa");
dataSource.setProxyFilters(Collections.singletonList(filter)); // replaces the filter list
dataSource.init();                                              // triggers Filter.init(DataSourceProxy)
```

> 如果你的 `DruidDataSource` 已经有过滤器，请把本过滤器追加到现有列表，而不是覆盖它——`setProxyFilters` 会替换整个集合。

### <a id="druidresilience4jplugin-is-a-no-op"></a>3. `DruidResilience4jPlugin` 是一个空实现——不要指望自动注册

`DruidResilience4jPlugin` 通过 `META-INF/spring.factories` 中 `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` 这个 key 被加载，并且 `getName() == "alibaba-druid"`，但在当前版本里它的 `plugin(...)` 方法体是**空的**：

```java
@Override
public void plugin(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
                   ConfigurableEnvironment environment) {
}
```

因此 `@EnableBulkhead(plugins = "alibaba-druid")`（或任何带该插件名的其他 `@Enable*`）会匹配到这个插件，但什么都不做。**请自行注册过滤器**（第 2 步），并从容器中构造它，以便它拿到由 Spring 管理的门面：

```java
@Bean
public Resilience4jDruidFilter resilience4jDruidFilter(Resilience4jFacade facade, DruidDataSource dataSource) {
    Resilience4jDruidFilter filter = new Resilience4jDruidFilter(facade);
    dataSource.setProxyFilters(Collections.singletonList(filter));
    return filter;
}
```

## 公共 API

### `Resilience4jDruidFilter`（`extends com.alibaba.druid.filter.FilterAdapter`）

| 成员 | 签名 / 行为 |
|--------|------------------------|
| 常量 | `public static final String DEFAULT_ENTRY_NAME_PREFIX = "alibaba-druid@"` |
| 构造器 | `Resilience4jDruidFilter(Resilience4jFacade facade)` — 使用默认前缀委托给双参构造器 |
| 构造器 | `Resilience4jDruidFilter(Resilience4jFacade facade, String entryNamePrefix)` — 保存两者（无 null 校验） |
| `init(DataSourceProxy dataSource)` | 重写 `Filter.init`；保留 `DataSourceProxy`，并在它是 `DruidDataSource` 时把 `getValidationQuery()` 缓存为 `validationSQL` |
| `getFacade()` | `@Nonnull public final Resilience4jFacade getFacade()` |
| `getEntryNamePrefix()` | `@Nonnull public final String getEntryNamePrefix()` |
| `getEntryName(StatementProxy)` | `public final String` — 见下文的资源名解析算法 |
| `doInResilience4j(StatementProxy, Callable<T>)` | `protected final <T> T ... throws SQLException` — 唯一的装饰点：`facade.call(entryName, callable::call, SQLException.class)` |

被重写的 Druid 方法（每一个都通过 `doInResilience4j` 调用 `super.<method>(...)`）：

| `PreparedStatement` | `Statement` |
|---------------------|-------------|
| `preparedStatement_execute` | `statement_execute`（4 个重载：`sql`、`sql+autoGeneratedKeys`、`sql+int[]`、`sql+String[]`） |
| `preparedStatement_executeQuery` | `statement_executeUpdate`（4 个重载，同样的变体） |
| `preparedStatement_executeUpdate` | `statement_executeQuery`、`statement_executeBatch` |

未被装饰的：`CallableStatement` 执行、`ResultSet*` 方法、连接的获取/提交/回滚，以及 Druid 的其他过滤器回调。类的 Javadoc 明确写了这一点：*“Resilience4jDruidFilter only supports `Statement` and `PreparedStatement`”*（Resilience4jDruidFilter 只支持 `Statement` 与 `PreparedStatement`）。

### Resilience4j 入口（资源）名是如何选定的

`getEntryName(StatementProxy)` 读取 `statement.getLastExecuteSql()`，然后：

| 场景 | 入口名 |
|-----------|------------|
| SQL 等于该 `DruidDataSource` 缓存的 `validationSQL` | 原始校验 SQL 本身（不做解析） |
| 由 `SQLUtils.parseStatements(sql, dbType)` 解析出的 `SELECT …` | `"SELECT " + <第一个查询块的 FROM table source>.computeAlias()` |
| `UPDATE …` | `"UPDATE " + <table source>.computeAlias()` |
| `INSERT …` | `"INSERT " + <table source>.computeAlias()` |
| `DELETE …` | `"DELETE " + <table source>.computeAlias()` |
| 没有解析出语句、出现不支持的语句类型、或四个分支内部发生解析错误 | `"UNRECOGNIZED"`（同时输出一条 `debug` 日志） |

解析使用从注入的 `DataSourceProxy` 得到的 `dbType`，并且只使用**第一个**解析出的 `SQLStatement`。带来的结果是：所有以同一动词操作同一张表的语句共享同一个 CircuitBreaker/Bulkhead/RateLimiter 实例，而多语句 SQL 会被归属到它的第一条语句上。

> 注意：`entryNamePrefix`（`"alibaba-druid@"`）会被保存、由 `getEntryNamePrefix()` 暴露，并在测试中被断言，但 `getEntryName(...)` **并不会把它拼在前面**——Resilience4j 的入口名就是形如 `"SELECT users"` 的纯字符串。在当前版本不要依赖这个前缀。

## 完整可运行示例（改写自 `Resilience4jDruidFilterTest`）

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

上游测试（`src/test/java/io/microsphere/resilience4j/alibaba/druid/filter/Resilience4jDruidFilterTest.java`）还额外断言了常量（`DEFAULT_ENTRY_NAME_PREFIX == "alibaba-druid@"`）和 getter（`assertSame(facade, filter.getFacade())`、`assertEquals(DEFAULT_ENTRY_NAME_PREFIX, filter.getEntryNamePrefix())`）。

## 行为说明与注意事项

- **SQL 执行期间熔断器打开。** 使用默认的 `CircuitBreakerRegistry.ofDefaults()` 配置时，处于 OPEN 状态的熔断器会让 `CircuitBreakerTemplate` 抛出 `CallNotPermittedException`。由于过滤器是用 `facade.call(entryName, callback, SQLException.class)` 做装饰的，这个失败会以 **`SQLException`** 的形式重新抛给 JDBC 调用方，而不是以 Resilience4j 异常的形式。那些针对 `CallNotPermittedException`（或 `instanceof` Resilience4j 类型）做分支的代码不会触发——请检查被包装的失败原因。
- **必须先执行过 `init()`。** `dataSource` 和 `validationSQL` 只在 `init(DataSourceProxy)` 中赋值。当过滤器在 `DruidDataSource.init()` 期间注册时 Druid 会调用它；在此之前使用该过滤器实例，`getEntryName` 会在 `dataSource`/`dbType` 为 `null` 的情况下被调用。
- **每次执行都会解析 SQL。** `SQLUtils.parseStatements` 在每一次被拦截的调用上都会运行；对于延迟敏感的连接池，更应使用粗粒度的入口名或换一种钩挂点。解析失败会被吞掉并只记 `debug` 日志，并统一塌缩到共享的 `"UNRECOGNIZED"` 入口——于是所有无法解析的流量共用同一个熔断器。
- **连接池校验流量是被隔离的**，这是有意设计：校验 SQL 保留自己的入口名，因此健康检查无法触发表级别的熔断器。
- **链中包含 `TimeLimiter` 时会切换线程。** `TimeLimiterTemplate.call(...)` 会把回调提交给一个 `ExecutorService`。由于 JDBC 的 `Connection`/事务是绑定线程的，给本过滤器使用的门面加入 `TimeLimiterRegistry` 会让 SQL 在另一个线程上执行。上面的示例构造了一个包含 `TimeLimiterRegistry` 的门面；如果你依赖线程绑定的事务，请去掉它。
- **真正使用 `begin`/`end` 的是两阶段模板**——完整链语义参见开发者指南页面。
- **批量语义。** `statement_executeBatch` 对整个批次只装饰一次（入口名来自该语句的 `getLastExecuteSql()`），而不是逐个批次元素装饰。

## 相关页面

- [开发者指南 — microsphere-resilience4j-alibaba-druid](../../developer-guide/zh/microsphere-resilience4j-alibaba-druid.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md) — `Resilience4jFacade`、`ChainableResilience4jFacade`
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md) — `@Enable*` 注解、`lazyResilience4jFacade`、`Resilience4jPlugin` SPI
