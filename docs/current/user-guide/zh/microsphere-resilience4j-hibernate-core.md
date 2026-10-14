# microsphere-resilience4j-hibernate-core — 用户指南

> 版本线 `0.2.x`（`release` 分支） · 最新发布版本 `0.2.4` · 当前 `${revision}` = `0.2.5-SNAPSHOT` · Resilience4j **2.0.0 – 2.4.0**（默认 `2.4.0`） · Spring Cloud 2022.0.x – 2025.0.x · JDK **17+** · [← 用户指南索引](README.md)
> [English](../microsphere-resilience4j-hibernate-core.md)

## 本插件的作用

`microsphere-resilience4j-hibernate-core` 通过实现由 `io.github.microsphere-projects:microsphere-hibernate-core` 提供的 **Hibernate 实体回调扩展点**，将实体持久化操作纳入 Resilience4j 的管控之下。单个类 —— `Resilience4jHibernateEntityCallback` —— 观察 Hibernate 触发的事件四对实体生命周期事件（`LOAD`、`INSERT`、`UPDATE`、`DELETE`），并把每一对转化为一次**两阶段**的 Resilience4j 调用：

| 前置事件 | 后置事件 | Resilience4j 操作 | 默认入口名 |
|-----------|------------|------------------------|--------------------|
| `onPreLoad` | `onPostLoad` | `facade.begin(name)` … `facade.end(context)` | `hibernate@<实体类名>@LOAD` |
| `onPreInsert` | `onPostInsert` | `begin` … `end` | `hibernate@<实体类名>@INSERT` |
| `onPreUpdate` | `onPostUpdate` | `begin` … `end` | `hibernate@<实体类名>@UPDATE` |
| `onPreDelete` | `onPostDelete` | `begin` … `end` | `hibernate@<实体类名>@DELETE` |

### 它解决的问题

本库的其他插件包装的是一个**你看得见的调用**：一个 Feign 请求、一条 MyBatis 语句、一个 HTTP 处理方法、一个 Druid 连接。而实体状态变化根本不像调用 —— `session.save(order)` 会立即返回，SQL 稍后才被 flush，通常发生在某个并没有应用代码显式发起的事务提交之中。没有什么可以交给 `facade.call(...)` 去装饰。

该插件让你在无需改动 DAO 代码的前提下，获得一个**按实体类、按生命周期动作**的资源保护边界：

- 针对每个实体动作的 `CircuitBreaker`，可在数据库劣化时阻止 persist/merge 风暴，
- 针对某个实体的 `Bulkhead`，可限制其并发写入的数量上限，
- 针对 `RateLimiter` 的写入可限流某个热点表。

由于前置事件和后置事件是两个不同的回调，插件使用 `Resilience4jFacade` 的 `begin()/end()` 配对，并在二者之间通过 `ThreadLocal` 携带 `Resilience4jContext` —— 与 [Spring Web 插件](microsphere-resilience4j-spring-web.md) 使用的机制相同（只不过那里改用请求属性来携带）。

> **范围说明。** 保护是针对*实体事件本身的准入控制*，而非针对 JDBC 语句。flush 可能发生在后置事件之后，而在 flush/commit 期间抛出的失败会上报给 Hibernate，而非上报到本回调。当你需要按 SQL 装饰或需要重试/超时行为时，请使用 [Druid](microsphere-resilience4j-alibaba-druid.md) 或 [MyBatis](microsphere-resilience4j-mybatis.md) 插件。

## Maven 坐标

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
    <version>0.2.4</version>   <!-- REQUIRED: not managed by the BOM, see below -->
</dependency>
```

> **重要 —— 此构件不受依赖管理清单（BOM）管理（已核实）。** `microsphere-resilience4j-dependencies` 在其 `<dependencyManagement>` 中仅列出六个构件（`commons`、`spring`、`alibaba-druid`、`mybatis`、`openfeign`、`spring-web`）；`microsphere-resilience4j-hibernate-core` **缺席**，尽管它在 `microsphere-resilience4j-plugins` 下的聚合反应堆中被构建。因此，导入该 BOM **不会**为它填入版本 —— 你必须自行固定版本，或添加你自己的 `<dependencyManagement>` 条目：
>
> ```xml
> <dependencyManagement>
>     <dependencies>
>         <dependency>
>             <groupId>io.github.microsphere-projects</groupId>
>             <artifactId>microsphere-resilience4j-hibernate-core</artifactId>
>             <version>0.2.4</version>
>         </dependency>
>     </dependencies>
> </dependencyManagement>
> ```
>
> 让该版本与你导入的 BOM 保持同步是手工进行的。参见 [dependencies 页面](microsphere-resilience4j-dependencies.md#gotcha-the-hibernate-plugin-is-not-in-the-bom)。

### 本模块 POM 引入了什么

来自 `microsphere-resilience4j-plugins/microsphere-resilience4j-hibernate-core/pom.xml`：

| 依赖 | 在本 POM 中的作用域 | 对你的影响 |
|------------|-------------------|---------------------|
| `io.github.microsphere-projects:microsphere-resilience4j-spring` | **compile**（版本 `${revision}`） | 带入 `microsphere-resilience4j-commons`（门面、模板）以及 Spring 的 `@Enable*` 层 |
| `io.github.microsphere-projects:microsphere-hibernate-core` | **compile**，无 `<version>` | 提供 `EntityCallback` 接口；版本来自 `microsphere-hibernate-dependencies:0.2.10`，由 `microsphere-resilience4j-parent` 导入 |
| `io.github.microsphere-projects:microsphere-spring-context` | **optional** | 仅 `spring` 层的 Spring 侧辅助类需要 |
| `io.github.microsphere-projects:microsphere-annotation-processor` | **optional** | 仅用于构建期 |
| `org.hibernate.orm:hibernate-core` | **optional** | **你必须提供它** —— Hibernate 6.x（`org.hibernate.orm` groupId），因为 `org.hibernate.type.Type` 出现在 API 中 |
| `io.github.resilience4j:resilience4j-annotations`、`-consumer`、`-framework-common`、`-core`、`-circuitbreaker`、`-ratelimiter`、`-retry`、`-bulkhead`、`-timelimiter` | **optional** | `-framework-common` 是唯一带有显式 `${resilience4j.version}` 的；请添加你实际使用的模块 |
| `org.springframework:spring-context` | **optional** | 仅下面展示的 Spring 装配需要 |
| `junit-jupiter`、`spring-test`、`microsphere-spring-test`、`microsphere-hibernate-test`、`h2`、`logback-classic` | **test** | 仅由模块自身的测试类使用 |

## 版本与兼容性

| 项目 | 要求 | 核实位置 |
|------|-------------|----------------|
| microsphere-resilience4j | `0.2.x`（`release` 分支），构件 `microsphere-resilience4j-hibernate-core` | `microsphere-resilience4j-plugins/pom.xml` `<modules>` |
| 引入版本 | `0.2.2` | `release-notes.md` —— “Hibernate Support: Added Hibernate core plugin and test utilities” |
| Resilience4j | 2.0.0 – 2.4.0 | 父 POM `<profiles>` `resilience4j-2.0` … `resilience4j-2.4`（默认 `2.4.0`） |
| Hibernate | `org.hibernate.orm:hibernate-core`（由应用提供，optional） | 模块 POM |
| microsphere-hibernate | `0.2.10`（BOM `microsphere-hibernate-dependencies`） | `microsphere-resilience4j-parent/pom.xml` |
| Spring Cloud | 2022.0.x – 2025.0.x | CI 矩阵 profile `spring-cloud-2022` … `spring-cloud-2025` |
| JDK | **17+** | 根 `pom.xml` `<java.version>17</java.version>`；CI 在 17 / 21 / 25 上构建 |

`1.x`（`release-1.x`，v0.1.x）线也带有一个 `microsphere-resilience4j-hibernate-core` 模块；针对该版本线请使用 [1.x 指南](../../../1.x/user-guide/zh/README.md)。

## 公共 API

`io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback` —— 模块中唯一的生产类。`public class Resilience4jHibernateEntityCallback implements EntityCallback`（`io.microsphere.hibernate.entity.EntityCallback`）。

### 构造与配置

该类**根本没有声明任何构造函数**，因此隐式的公有无参构造函数是唯一的实例化方式。两个协作对象都通过 JavaBean setter 设置（这正是它可以作为带属性注入的 Spring bean 使用的原因）：

| 成员 | 行为（源自代码） |
|--------|--------------------------|
| `public static final String DEFAULT_ENTRY_NAME_PREFIX = "hibernate@"` | `entryNamePrefix` 字段的初始值 |
| `Resilience4jHibernateEntityCallback()` | 隐式默认构造函数；`facade` 字段**未初始化**（`null`） |
| `void setFacade(@Nonnull Resilience4jFacade facade)` | 先 `assertNotNull(facade, …)` 再赋值；消息：`'Resilience4jFacade' argument must not be null!` |
| `void setEntryNamePrefix(@Nonnull String entryNamePrefix)` | 先 `assertNotNull(entryNamePrefix, …)` 再赋值；消息：`'entryNamePrefix' argument must not be null!` |
| `protected String buildEntryName(Object entity, String action)` | `entryNamePrefix + entity.getClass().getName() + '@' + action` |

该类**没有 getter**，也没有 `getFacade()` / `getEntryNamePrefix()` 访问器（与 MyBatis、Spring Web 插件不同）。

### 被拦截的生命周期事件

八个回调全部是对 `EntityCallback` 契约的 `@Override` 实现。签名按源码中声明的原样复现：

| 事件对 | 声明中的方法签名 | 插件所做的事情 |
|------------|-------------------------------|----------------------|
| LOAD | `onPreLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)` | `begin("…@LOAD")` + `withinContext()` |
| LOAD | `onPostLoad(Object entity, Object id, String[] propertyNames, Type[] types)` | `doInContext(facade::end, true)` |
| INSERT | `onPreInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `begin("…@INSERT")` + `withinContext()` |
| INSERT | `onPostInsert(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `doInContext(facade::end, true)` |
| UPDATE | `onPreUpdate(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `begin("…@UPDATE")` + `withinContext()` |
| UPDATE | `onPostUpdate(Object entity, Object id, Object[] state, String[] propertyNames, Type[] propertyTypes)` | `doInContext(facade::end, true)` |
| DELETE | `onPreDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)` | `begin("…@DELETE")` + `withinContext()` |
| DELETE | `onPostDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types)` | `doInContext(facade::end, true)` |

针对它编写正确代码时你需要知道的要点：

- **仅存在这四个动作** —— 这里没有集合（`@OneToMany` 脏化）或 flush/commit 回调。
- `Type` 是 `org.hibernate.type.Type`（直接来自 Hibernate 6.x 的导入）。
- **`onPostLoad` 没有 `state` 参数**，而每个其他方法都有；前置/后置签名并不对称。
- 参数在 LOAD/DELETE 方法中命名为 `types`，在 INSERT/UPDATE 方法中命名为 `propertyTypes` —— 同一个值，不同的名字。
- 字段上的 `@Nonnull` 属于文档级别：字段被声明为 `@Nonnull`，但 `facade` 初始为 `null`，因此一个未接线的回调会在第一个事件上抛出 `NullPointerException`。

### 入口名

```java
protected String buildEntryName(Object entity, String action) {
    Class<?> entityClass = entity.getClass();
    return entryNamePrefix + entityClass.getName() + '@' + action;
}
```

| 实体 / 动作 | 入口名（默认前缀） |
|-----------------|-----------------------------|
| `com.acme.order.Order`，insert | `hibernate@com.acme.order.Order@INSERT` |
| `com.acme.order.Order`，update | `hibernate@com.acme.order.Order@UPDATE` |
| `com.acme.Customer`，load | `hibernate@com.acme.Customer@LOAD` |
| `com.acme.Customer`，delete | `hibernate@com.acme.Customer@DELETE` |

`entity.getClass().getName()` 意味着**仅对具体实体类才有代理安全的命名**：如果 Hibernate 交给回调的是一个 ByteBuddy/Javassist 代理实例，那么生成的代理类名会进入入口名并创建一个不同的资源。对于懒加载实体要记住这一点。

## 配置

本模块**自身不注册任何东西**：它不含 `META-INF/spring.factories`，不含 `Resilience4jPlugin` 实现，也不含自动配置类。你创建回调、给它一个门面，并把它交给你的 Hibernate 引导流程（`EntityCallback` 扩展点由 `microsphere-hibernate-core` 定义，因此请查阅该项目以了解你的技术栈所暴露的注册入口）。

### 方式 A —— 编程式

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback;

Resilience4jFacade facade = new ChainableResilience4jFacade(
        BulkheadRegistry.ofDefaults(), RateLimiterRegistry.ofDefaults(),
        RetryRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults());

Resilience4jHibernateEntityCallback callback = new Resilience4jHibernateEntityCallback();
callback.setFacade(facade);                       // mandatory
callback.setEntryNamePrefix("entity@");           // optional, defaults to "hibernate@"
```

### 方式 B —— Spring bean

`microsphere-resilience4j-spring` 将懒加载门面注册为名为 `lazyResilience4jFacade` 的 bean（`LazyResilience4jFacade.BEAN_NAME`，由 `EnableResilience4jRegistrar` 声明为*primary*）。由于回调采用 setter 配置，一个普通的 `@Bean` 方法就足够了：

```java
import io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.spring.LazyResilience4jFacade;
import io.microsphere.resilience4j.spring.common.annotation.EnableBulkhead;
import io.microsphere.resilience4j.spring.common.annotation.EnableCircuitBreaker;
import io.microsphere.resilience4j.spring.common.annotation.EnableRateLimiter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCircuitBreaker
@EnableRateLimiter
@EnableBulkhead
public class Resilience4jHibernateConfig {

    @Bean
    public Resilience4jHibernateEntityCallback resilience4jHibernateEntityCallback(
            @Qualifier(LazyResilience4jFacade.BEAN_NAME) Resilience4jFacade facade) {
        Resilience4jHibernateEntityCallback callback = new Resilience4jHibernateEntityCallback();
        callback.setFacade(facade);
        return callback;   // register it with your microsphere-hibernate bootstrap
    }
}
```

`@Enable*` 注解决定了懒加载门面实际组装哪些模板 —— 在两阶段路径上精确启用你所需的模块（参见下方注意事项）。注解包与绑定前缀在 [spring 模块页面](microsphere-resilience4j-spring.md) 上有文档说明。

## 示例

### 随模块一起发布的测试

`microsphere-resilience4j-hibernate-core/src/test/java/io/microsphere/resilience4j/hibernate/Resilience4jHibernateEntityCallbackTest.java` 逐字来看就是整个测试类：

```java
/**
 * {@link Resilience4jHibernateEntityCallback} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see Resilience4jHibernateEntityCallback
 * @since 1.0.0
 */
class Resilience4jHibernateEntityCallbackTest extends AbstractHibernateH2Test {


}
```

它**不声明任何测试方法**。它对你的价值在于它所继承的装配：继承 `io.microsphere.hibernate.test.AbstractHibernateH2Test`（来自 `microsphere-hibernate-test` 测试依赖）会得到一个针对内存 H2 引导起来的 Hibernate 会话工厂，那里正是要添加基于实体的断言之处。仓库里没有任何可复制的断言，因此下面的驱动是依据生产源码而非测试编写的。

### 一个完整的驱动（前置 → 后置，按 Hibernate 触发它们的顺序）

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jContext;
import io.microsphere.resilience4j.common.Resilience4jFacade;
import io.microsphere.resilience4j.hibernate.Resilience4jHibernateEntityCallback;
import org.hibernate.type.Type;
import org.hibernate.type.StandardBasicTypes;

public class HibernateCallbackDriver {

    static class Order {
        Long id;
        String customer;
    }

    public static void main(String[] args) {
        Resilience4jFacade facade = new ChainableResilience4jFacade(
                BulkheadRegistry.ofDefaults(), RateLimiterRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults());

        Resilience4jHibernateEntityCallback callback = new Resilience4jHibernateEntityCallback();
        callback.setFacade(facade);                       // without this: NPE on first event

        Order order = new Order();
        order.id = 1L;
        order.customer = "Mercy";

        Object id = order.id;
        Object[] state = {order.id, order.customer};
        String[] propertyNames = {"id", "customer"};
        Type[] types = {StandardBasicTypes.LONG, StandardBasicTypes.STRING};

        // --- INSERT cycle ---------------------------------------------------
        callback.onPreInsert(order, id, state, propertyNames, types);
        // entry "hibernate@…$Order@INSERT" is open; the bound context is visible here:
        System.out.println(((Resilience4jContext<?>) Resilience4jContext.getContext())
                .getEntryName());
        callback.onPostInsert(order, id, state, propertyNames, types);          // end + unbind

        // --- LOAD cycle (note: no state argument) ---------------------------
        callback.onPreLoad(order, id, state, propertyNames, types);
        callback.onPostLoad(order, id, propertyNames, types);

        // --- UPDATE and DELETE cycles ---------------------------------------
        callback.onPreUpdate(order, id, state, propertyNames, types);
        callback.onPostUpdate(order, id, state, propertyNames, types);
        callback.onPreDelete(order, id, state, propertyNames, types);
        callback.onPostDelete(order, id, state, propertyNames, types);

        System.out.println("Resilience4jContext after the last post-event: "
                + Resilience4jContext.getContext());     // null -> every cycle was closed
    }
}
```

这段代码演练了什么，以及从源码看它证明了什么：

- 每个 `onPreXxx` 都会把一个 `Resilience4jContext` **绑定**到当前线程（`withinContext()`），因此在成对事件之间 `Resilience4jContext.getContext()` 非 `null`；
- 每个 `onPostXxx` 都会**移除并结束**它（`doInContext(consumer, true)` 先调用 `removeContext()`），这正是最后一次打印显示 `null` 的原因；
- 四个动作产生四个不同的入口名，从而在门面背后的注册表中产生四个互相独立的 `CircuitBreaker` / `Bulkhead` / `RateLimiter` 实例。

在真实应用中，你永远不会亲自调用这些方法 —— Hibernate 会在你的 DAO 执行 `save` / `get` / `delete` 期间，通过 `EntityCallback` 链触发它们。

## 行为说明与注意事项

- **只有三个 Resilience4j 模块参与。** `begin()`/`end()` 会查询每个模板上的 `isBeginSupported()` / `isEndSupported()`；`RetryTemplate`、`TimeLimiterTemplate` 和 `ThreadPoolBulkheadTemplate` 返回 `false` 并跳过。`CircuitBreakerTemplate`、`RateLimiterTemplate` 和 `BulkheadTemplate` 实现了这一配对。如果你需要在 SQL 周围进行重试或超时，请使用采用一次性 `call(...)` 路径的 [MyBatis](microsphere-resilience4j-mybatis.md)、[Druid](microsphere-resilience4j-alibaba-druid.md) 或 [OpenFeign](microsphere-resilience4j-openfeign.md) 插件。
- **拒绝发生在前置事件。** `CircuitBreaker#acquirePermission()` 抛出 `CallNotPermittedException`，`Bulkhead#acquirePermission()` 抛出 `RequestNotPermitted`，而 `RateLimiterTemplate.doBegin` 会先通过 `RateLimiter.waitForPermission(...)` 等待再抛出 `RequestNotPermitted`。异常会从 `onPreXxx` 回调传播到 Hibernate 的事件分发之中，也就是传播到你的 `save()`/`get()` 调用 —— 一旦 Hibernate 将其包装，预期它会以持久层异常（而非 Resilience4j 类型）的形式浮现。
- **每一对完成的事件都被记录为一次成功。** 插件在退出路径上从不调用 `context.setResult(...)` / `setFailure(...)`（对比 `Resilience4jHandlerMethodInterceptor.afterExecute`，它确实会调用）。`CircuitBreakerTemplate.doEnd` 和 `RateLimiterTemplate.doEnd` 会依据 `getFailure()`/`getResult()` 分支，因此当二者都为 `null` 时，它们走 `onSuccess(...)` / `onSuccess()` 式的成功路径。因此熔断器的错误率只有在前置事件拒绝本身被入口观察到时才会变动 —— 请围绕 `CallNotPermittedEvent` 以及隔层/限流器的拒绝事件来规划你的告警。
- **中止的操作会泄漏上下文和隔层许可。** `end()` 只在后置事件中运行。如果 insert/update/delete 在 Hibernate 触发后置事件之前抛出异常，那么 `ThreadLocal` 仍持有该上下文，而 `BulkheadTemplate.doEnd`（`bulkhead.onComplete()`）永远不会被调用，于是并发许可在该线程的逻辑调用上一直被占用。在为 `maxConcurrentCalls` 定容时要考虑这一点。
- **一个共享的 `ThreadLocal`，嵌套事件会覆盖它。** `Resilience4jContext.setContext(...)` 每线程只存一个值。级联保存（实体 A 的 insert 在同一线程上、在 A 的后置事件之前触发了实体 B 的 insert）意味着内层配对覆盖了外层上下文，而外层的 `onPostXxx` 随后会结束**内层**入口。请让级联避开受保护路径，或者接受嵌套不被追踪。
- **读取同样被装饰。** `onPreLoad`/`onPostLoad` 会保护每一次实体物化，包括由关联加载拉取的行。`…@LOAD` 上的 `RateLimiter` 限制的是取数而非写入 —— 通常你会在那里设置高得多的阈值。
- **`entryNamePrefix` 在接线后仍可修改**（无 `final`，无守卫），并且该类在设计上并非线程安全：实例本应被共享，但字段 setter 只应在初始化期间调用。
- **没有插件 SPI，没有自动注册。** 与 Druid、MyBatis、Spring Web 插件不同，该类并未实现 `io.microsphere.resilience4j.spring.common.Resilience4jPlugin`，因此 `@Enable*` 注解上的 `plugins = "…"` 属性对它毫无作用。注册完全由你负责。
- **版本漂移风险。** 由于 BOM 不管理此构件，把 BOM 升级到 `0.2.(n+1)` 而 Hibernate 插件仍固定为 `0.2.n`，会在悄无声息中混用两个库版本。升级时请在你的 POM 中 grep 该构件。

## 相关页面

- [开发者指南 — microsphere-resilience4j-hibernate-core](../../developer-guide/zh/microsphere-resilience4j-hibernate-core.md)
- [microsphere-resilience4j-dependencies（BOM 范围陷阱）](microsphere-resilience4j-dependencies.md)
- [microsphere-resilience4j-commons](microsphere-resilience4j-commons.md)
- [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- [microsphere-resilience4j-spring-web（另一个两阶段插件）](microsphere-resilience4j-spring-web.md)
