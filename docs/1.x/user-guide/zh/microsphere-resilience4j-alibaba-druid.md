# microsphere-resilience4j-alibaba-druid — 用户指南（1.x）

> 简体中文 | [English](../microsphere-resilience4j-alibaba-druid.md)

> 版本线 `0.1.x`（`release-1.x` 分支）· 最新发布版本 `0.1.4` · 当前 `${revision}` = `0.1.5-SNAPSHOT` · Resilience4j **1.3.0 – 1.7.0**（默认 `1.7.0`）· Spring Cloud Hoxton – 2021.0.x · [← 用户指南索引](README.md)

源码树中的模块路径：`microsphere-resilience4j-plugins/microsphere-resilience4j-alibaba-druid`。

## 问题陈述

当数据库退化或宕机时，应用通常以最糟糕的方式失败：线程堆积在 JDBC 调用内部，连接池被耗尽，故障随后扩散到每一个无关的功能。Resilience4j 通过断路器、舱壁隔离（bulkhead）与限流器来解决这一问题——但这些装饰器必须施加在*每一个* SQL 调用都会经过的位置。

在每个 DAO 或每个服务处分别处理既重复又容易被绕过（裸 JDBC、JPA、MyBatis、报表查询）。本插件把边界下移一层，放进 **Alibaba Druid 连接池**内部，于是一次注册即可保护在该 `DataSource` 上执行的全部 SQL。

## Druid 中的钩子位置

本插件挂钩 Druid 自身的**过滤器链**（`com.alibaba.druid.filter.Filter`）。来自源码：

```java
public class Resilience4jDruidFilter extends FilterAdapter {
```

`FilterAdapter` 是 Druid 实现了 `Filter` SPI 并提供透传（pass-through）默认行为的基础适配器类——**不是** `FilterEventAdapter`（那个带有 `statementExecute` / `statementUpdate` 回调的更高层事件适配器）。你应当知道的后果：

- 弹性边界是围绕 *Druid 链的延续*（`super.<method>(chain, …)`）打开的，因此每一个下游过滤器以及真正的驱动调用都落在边界之内。
- 只覆盖了语句执行。连接获取（`connection_connect`）、结果集遍历与事务回调在本分支上并**未**被覆盖。
- 该类的 Javadoc 明确写出了受支持的表面：`Resilience4jDruidFilter only supports Statement and PreparedStatement`。`CallableStatement` 不受支持。

典型用途：

| 目标 | 在门面上配置的模块 | 被保护的条目名称 |
|------|-----------------------------------|----------------------|
| 某张表宕机时快速失败 | CircuitBreaker | `SELECT users`、`UPDATE users`，…… |
| 限制热点表的并发 | Bulkhead | 同上 |
| 对遗留表的写入做限流 | RateLimiter | 同上 |

## Maven 坐标

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-alibaba-druid</artifactId>
    <version>0.1.4</version>
    <!-- managed if you import microsphere-resilience4j-dependencies -->
</dependency>
```

该插件自身的 POM 只声明了一个强制依赖；其余全部为 `optional`：

| 依赖 | 在 `microsphere-resilience4j-alibaba-druid/pom.xml` 中声明的作用域 | 说明 |
|------------|---------------------------------------------------------------------|------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | compile（非 optional） | 传递性引入 `microsphere-resilience4j-commons` |
| `com.alibaba:druid` | **optional** | 必须由你提供；在本 reactor 中版本通过导入的 `microsphere-alibaba-druid-dependencies` BOM `0.1.20` 解析（属性位于 `microsphere-resilience4j-parent/pom.xml`） |
| `org.springframework:spring-context` | **optional** | 仅 `DruidResilience4jPlugin` / `@Enable*` 注解需要 |
| `io.github.resilience4j:resilience4j-{core,circuitbreaker,ratelimiter,retry,bulkhead,timelimiter,annotations,consumer,framework-common}` | **optional** | `framework-common` 携带显式的 `${resilience4j.version}` |
| `microsphere-spring-context`、`microsphere-annotation-processor` | **optional** | 内部辅助工具 |
| `junit-jupiter`、`spring-test`、`microsphere-spring-test`、`com.h2database:h2`、`ch.qos.logback:logback-classic` | **test** | 供 `Resilience4jDruidFilterTest` 使用 |

## 兼容性

以下内容依据 `.doc-tmp/release-1x/pom.xml`、`microsphere-resilience4j-parent/pom.xml` 与 `.github/workflows/maven-build.yml` 核实：

| 条目 | `release-1.x` 上的取值 | 证据 |
|------|------------------------|----------|
| microsphere-resilience4j | `0.1.x`；`${revision}` = `0.1.5-SNAPSHOT` | 根 `pom.xml` 的 `<properties><revision>0.1.5-SNAPSHOT</revision></properties>` |
| Resilience4j | 1.3.0 / 1.4.0 / 1.5.0 / 1.6.0 / **1.7.0（默认）** | 父工程 profile `resilience4j-1.3` … `resilience4j-1.7`，其中最后一个为 `activeByDefault` |
| 构建父工程 | `microsphere-spring-cloud-parent` **0.1.26** | 根 `pom.xml` 的 `<parent>` |
| Spring Cloud | Hoxton – 2021.0.x | CI 矩阵 `maven-profile-spring-cloud: ['spring-cloud-hoxton','spring-cloud-2020','spring-cloud-2021']` |
| Alibaba Druid | 未在本仓库 POM 中锁定 | 声明为 `optional` 且不带 `<version>`；本 reactor 通过上面导入的 BOM 解析它 |
| JDK 基线 | **无法从本快照核实** | 该分支任何 POM 中都没有出现 `maven.compiler.*`/`<source>`/`<target>`——它从外部父工程 0.1.26 继承。CI 构建矩阵 `java: ['8','11','17','21','25']`；`maven-publish.yml` 配置 Java `11`。指南索引声明“JDK 8”；可把 8 视为预期下限，但请对照你实际构建所用的父工程 POM 予以确认。 |

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

所有 `throws SQLException` 子句与源码一致（即被覆盖的 Druid 签名）。

使用 API 时需要留意的几点：

- `init(DataSourceProxy)` 会保存该数据源，并且仅当它是 `DruidDataSource` 时保存其 `getValidationQuery()`。这个值随后会使连接池校验流量短路掉 SQL 解析，因此 `validationQuery` 不会污染你的条目统计。
- `getEntryName(StatementProxy)` 读取 `statement.getLastExecuteSql()`，用 `com.alibaba.druid.sql.SQLUtils.parseStatements(sql, dataSource.getDbType())` 解析它，并根据第一个 AST 节点进行分派。
- 生成的条目名称形如 `SELECT <table>`、`UPDATE <table>`、`INSERT <table>`、`DELETE <table>`；任何无法识别或无法解析的内容都会变成字面量 `UNRECOGNIZED`。
- **在本分支上，`entryNamePrefix` 虽被存储并由 getter 暴露，却从未真正应用到条目名称上。** `getEntryName(...)` 直接返回解析出的资源名。请在诸如 `SELECT users` 这样的名称下配置你的 Resilience4j 实例，而不是 `alibaba-druid@SELECT users`。（对照：MyBatis 与 Hibernate 插件*确实*会给它们的条目名称加上前缀。）

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

在本分支上 `plugin(...)` 的方法是**空**的：选择该插件只是让名字被 SPI 知晓；它不会替你注册过滤器 bean。过滤器注册仍需手工完成（见下一节）。

## 装配 / 注册

`release-1.x` 上该插件没有 Spring Boot 自动配置（在本快照中，`microsphere-resilience4j-spring-boot` 与 `-spring-cloud` 只包含一个 `pom.xml` 且没有源码）。两条已核实的路径：

### 1. 编程式（这正是 `Resilience4jDruidFilterTest` 的做法）

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

`setProxyFilters(...)` + `init()` 是触发过滤器自身 `init(DataSourceProxy)` 回调的已验证方式。

### 2. 作为 Spring bean

把过滤器声明为一个 bean 并交给你的 `DruidDataSource`。该类**没有无参构造器**——只有两个 `(Resilience4jFacade[, String])` 构造器——因此：

- Spring 风格的 `<property>`/setter 注入，或 Druid 基于类名的 `filters=...` 配置，都无法实例化它。请使用传入 `Resilience4jFacade` 的 `@Bean` 方法（或带 constructor-arg 的 bean 定义）。
- 在基于 Spring 上下文的应用中，你应当引用的门面是 Spring 模块以 `LazyResilience4jFacade.BEAN_NAME` = `"lazyResilience4jFacade"` 注册的那个（由 `@Enable*` 注册器构建的 primary bean）。

### 3. `Resilience4jPlugin` SPI

`META-INF/spring.factories` 声明了该描述符：

```properties
io.microsphere.resilience4j.spring.common.Resilience4jPlugin=\
io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin
```

`EnableResilience4jRegistrar.registerPlugins(...)` 会加载所有 `Resilience4jPlugin` 工厂，并且仅当注解的 `plugins` 属性包含 `getName()` 时才调用 `plugin(...)`。因此，在例如 `@EnableBulkhead` 上添加 `plugins = "alibaba-druid"` 在**今天无害但无用**——方法是空的。

## 完整示例（来自真实测试）

`Resilience4jDruidFilterTest` 覆盖了每一种被覆盖的执行形态。下面是 statement 路径测试的节选，逐字取自源码：

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

预编译语句测试（`testExecutePreparedStatement`）针对由 `prepareStatement(sql)`、`prepareStatement(sql, TYPE_FORWARD_ONLY)` 与 `prepareStatement(sql, TYPE_FORWARD_ONLY, CONCUR_UPDATABLE)` 创建的 `PreparedStatement` 分别演练了 `executeUpdate()`、`executeQuery()` 与 `execute()`。

这些语句解析出的条目名称为 `INSERT users`、`UPDATE users`、`SELECT users`、`DELETE users`（而 `CREATE TABLE …` / `DROP TABLE …` → `UNRECOGNIZED`，因为只有 select/update/insert/delete 这几类 AST 节点被映射）。

## 行为说明

1. **失败以 `SQLException` 的形式暴露。** `doInResilience4j` 调用 `facade.call(entryName, callable::call, SQLException.class)`；`Resilience4jOperations` 通过 `io.microsphere.util.ExceptionUtils.wrap` 把任意 `Throwable` 包装进所声明的异常类型。因此一个打开的断路器会以 `SQLException` 的形式到达你的 JDBC 调用方，正是这一点使该插件对 JPA/MyBatis 调用方保持透明。
2. **究竟哪些模块真正装饰了这次调用**，取决于你门面背后的那些注册表。使用 `ChainableResilience4jFacade` 时，`call(...)` 会在按优先级排序的模板（Retry → CircuitBreaker → RateLimiter → TimeLimiter → Bulkhead，`Resilience4jModule.defaultAspectOrder`）之上构建一条 `CallbackChain`。
3. **条目名称每次调用都重新计算**，方式是用 Druid 解析器解析 SQL。这意味着每条语句上的 CPU 开销；本分支不缓存解析结果。设置一个 `validationQuery`，可让连接池校验流量不进入你的统计。
4. **命名基于表，而非基于语句。** 针对同一张表的两条不同 `SELECT` 语句共享同一个断路器实例。除了子类化各个 `statement_*` 覆盖方法之外，没有可自定义命名的钩子——`getEntryName(StatementProxy)` 与 `doInResilience4j(...)` 都是 `final`。
5. **解析时需要 `dataSource.getDbType()`**；过滤器保留了在 `init(...)` 中捕获的 `DataSourceProxy`。如果连接池从未调用 `init`，那么 `getEntryName` 会在 `dataSource.getDbType()` 上抛 NPE（除非该 SQL 恰好等于缓存的 `validationSQL`）。Druid 在构建代理过滤器时总会调用 `Filter.init`，因此实践中这一点只对直接实例化该过滤器的单元测试有影响。
6. **该插件描述符只是一个空壳（stub）。** 不要指望 `@Enable*(plugins = "alibaba-druid")` 会注册任何东西。

## 相关页面

- [开发者指南 — 本模块](../../developer-guide/zh/microsphere-resilience4j-alibaba-druid.md)
- [用户指南 — commons](microsphere-resilience4j-commons.md)（门面与模板）
- [用户指南 — spring](microsphere-resilience4j-spring.md)（`@Enable*`、`lazyResilience4jFacade`、插件 SPI）
