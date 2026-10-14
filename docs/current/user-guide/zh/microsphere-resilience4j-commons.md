# microsphere-resilience4j-commons — 用户指南

> 版本线：`0.2.x`（源分支 `release`）· 最新发行版 `0.2.4` · 分支修订号 `0.2.5-SNAPSHOT`
> 兼容 **Resilience4j 2.0.0 – 2.4.0**（Maven profile `resilience4j-2.0` … `resilience4j-2.4`，默认 **2.4.0**）
> 本模块**不依赖 Spring** —— 它是构建在 Resilience4j 注册表之上的纯 Java 代码。
> ← [用户指南索引](README.md) · [开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-commons.md)

---

## 1. 模块概览

`microsphere-resilience4j-commons` 是本项目的框架中立核心。它把全部六个 Resilience4j 模块 —— **CircuitBreaker（断路器）、RateLimiter（限流器）、Retry（重试）、Bulkhead（舱壁隔离）、ThreadPoolBulkhead（线程池舱壁隔离）、TimeLimiter（超时限制）** —— 统一到一套编程模型之下，使应用代码无需再去学习六套不同的装饰器 API。

该模型由四个关键部分组成：

| 概念 | 类型 | 含义 |
|---------|------|---------------|
| **模块（Module）** | `Resilience4jModule` | 一个枚举，描述某一项 Resilience4j 能力：它的入口类、配置类、事件类、注册表类、配置属性类以及默认切面顺序。 |
| **入口（Entry）** | 例如 `CircuitBreaker` | 存活在某个 Resilience4j `Registry` 内部的一个*具名*实例。 |
| **模板（Template）** | `Resilience4jTemplate` 的子类 | 恰好包装一个 `Registry`，并以该模块的装饰逻辑执行回调（`execute` / `call` / `begin`+`end`）。 |
| **门面（Facade）** | `ChainableResilience4jFacade` | 包装*多个*模板，并按优先级顺序将它们组合在同一个回调之外。 |

你需要的一切都集中在三个包里：

```
io.microsphere.resilience4j.common    <- abstractions: Resilience4jOperations, AdvancedResilience4jOperations,
                                          Resilience4jTemplate, Resilience4jContext, Resilience4jModule,
                                          Resilience4jFacade, ChainableResilience4jFacade, CallbackChain,
                                          Resilience4jConstants
io.microsphere.resilience4j.bulkhead  <- BulkheadTemplate, ThreadPoolBulkheadTemplate
io.microsphere.resilience4j.circuitbreaker / .ratelimiter / .retry / .timelimiter
io.microsphere.resilience4j.util      <- Resilience4jUtils
```

相比直接使用原生 Resilience4j，选用本模块的典型理由有：

- 每项能力都用同一套统一 API（`execute` / `call`），其中包括对受检异常友好的 `call`。
- 具名实例的接线工作（先在注册表中查找，找不到就用具名配置创建）已为你代劳。
- 多模块组合（`Retry(CircuitBreaker(RateLimiter(TimeLimiter(Bulkhead(function))))))`）无需手写装饰器嵌套。
- 一个每次执行专属的 `Resilience4jContext`，你可以携带它、装饰它并把它绑定到当前线程。
- 以链式、模块强类型的 API 进行注册表级与入口级的事件订阅。

---

## 2. Maven 坐标

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-commons</artifactId>
    <version>0.2.4</version>
</dependency>
```

或者导入项目的 BOM 并省略版本号：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.2.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>io.github.microsphere-projects</groupId>
        <artifactId>microsphere-resilience4j-commons</artifactId>
    </dependency>
</dependencies>
```

> **重要 —— 本模块的每一个编译期依赖都被声明为 `<optional>true</optional>`。**
> 这包括 `resilience4j-core`、`resilience4j-circuitbreaker`、`resilience4j-ratelimiter`、
> `resilience4j-retry`、`resilience4j-bulkhead`、`resilience4j-timelimiter`、`resilience4j-annotations`、
> `resilience4j-consumer`、`resilience4j-framework-common` 以及 `microsphere-java-core`。
> 可选依赖**不会**被你的应用传递性继承，因此你必须自己声明所使用的那些 Resilience4j 构件（下面这段
> `io.github.resilience4j:resilience4j-bom` 的导入能让它们保持在同一个版本上）：
>
> ```xml
> <dependencyManagement>
>     <dependencies>
>         <dependency>
>             <groupId>io.github.resilience4j</groupId>
>             <artifactId>resilience4j-bom</artifactId>
>             <version>2.4.0</version>
>             <type>pom</type>
>             <scope>import</scope>
>         </dependency>
>     </dependencies>
> </dependencyManagement>
> ```
>
> 这是有意为之：本模块是一层薄薄的抽象，不希望替你锁定 Resilience4j 的版本。
> 代价是：如果你希望 `Resilience4jUtils` 能正常工作，就必须把六个模块的构件都放到 classpath 上（见 §8.7）。

---

## 3. 版本与兼容性

| 项目 | 值 | 依据（从何核实） |
|------|-------|---------------|
| 分支 / 版本线 | `release`、`0.2.x` | 聚合 `pom.xml`（`<revision>0.2.5-SNAPSHOT</revision>`） |
| 最新已发布版本 | `0.2.4` | 根 `README.md` 的分支表（"**main** … Latest Version `0.2.4`"）；本分支上的 `release-notes.md` 只记述了 `v0.2.1` 和 `v0.2.2` |
| Resilience4j | `2.0.0`、`2.1.0`、`2.2.0`、`2.3.0`、`2.4.0` | `microsphere-resilience4j-parent/pom.xml` 中的 profile `resilience4j-2.0` … `resilience4j-2.4`；`resilience4j-2.4` 为 `activeByDefault` |
| Spring Cloud（项目整体版本线，**非**本模块） | `2022.0.x` – `2025.0.x` | CI 中的 profile `spring-cloud-2022` … `spring-cloud-2025` + 父 `microsphere-spring-cloud-parent` `0.2.26` |
| JDK | 基线 `17`（`<java.version>17</java.version>`），CI 运行 `17`、`21`、`25` | 聚合 `pom.xml`、`.github/workflows/maven-build.yml` |
| 本模块对 Spring 的依赖 | **无** | `microsphere-resilience4j-commons/pom.xml` |

请勿把本版本线与 `0.1.x` / `1.x` 线（Resilience4j 1.3.0 – 1.7.0）混用；参见
[1.x 用户指南](../../../1.x/user-guide/zh/README.md)。

---

## 4. 必须先理解的核心概念

### 4.1 注册表语义与具名实例语义

`Resilience4jTemplate` **从不拥有**入口；它拥有一个 `Registry`，并按名称惰性解析入口：

```
getEntry(name)  ->  1) local entries cache (if enabled)
                    2) registry.find(name)
                    3) createEntry(name)  ==  registry.<module>(name, getConfiguration(name))
```

- 第 2/3 步解释了为什么同一个名字始终映射到*同一个*入口对象：状态（失败率窗口、
  舱壁许可、重试计数）保存在注册表里，而不在模板里。
- `createEntry(name)` 是**按入口名**（`getConfiguration(name)`）来查找配置的。
  若该名称下没有注册任何配置，就使用注册表的**默认配置**。
- 两个舱壁隔离模板还会传播注册表的标签：
  `registry.bulkhead(name, getConfiguration(name), registry.getTags())`。断路器、限流器、重试
  和超时限制的入口在创建时不带标签。

> **一个后果（这里很容易踩坑）：** 要给某个入口配一份专属配置，你必须在入口首次被解析之前，
> 就把该配置以*入口自己的*名字注册进去：
>
> ```java
> template.addConfiguration("checkout-service", circuitBreakerConfig); // name == entry name
> CircuitBreaker entry = template.getEntry("checkout-service");        // created WITH that config
> ```
>
> 本项目自己的测试在 `setUp()` 里做的正是这件事。以别的名字注册配置，对入口创建没有任何影响。

### 4.2 一次性操作与两阶段操作

`Resilience4jOperations` 提供两种形态（该接口的 Javadoc 称之为
*One-Time Operations*（一次性操作）和 *Two-Phase Operations*（两阶段操作））：

| 形态 | API | 何时使用 |
|-------|-----|-------------|
| 一次性 | `execute(name, …)` / `call(name, …)` | 整段受保护的调用都能放进一个 lambda 里。模板会替你处理进入/退出。 |
| 两阶段 | `begin(name)` … `end(context)` | 你需要把装饰逻辑拆分到无法移入 lambda 的代码周围（例如分散在某个拦截器的 `before`/`after` 方法里的埋点，或一条响应式管道）。 |

所有一次性重载都汇入 **`call(String, ThrowableSupplier)`**，它是唯一的装饰钩子。见 §7.1。

### 4.3 优先级 = 切面顺序

每个模板都实现了 `io.microsphere.lang.Prioritized`。它的默认优先级就是该模块的
`defaultAspectOrder`，而 `ChainableResilience4jFacade` 会按优先级**升序**排序模板
（值越小 = 越外层）：

| 模块 | `defaultAspectOrder` |
|--------|----------------------|
| `RETRY` | `0` |
| `CIRCUIT_BREAKER` | `1` |
| `RATE_LIMITER` | `2` |
| `TIME_LIMITER` | `3` |
| `BULKHEAD` | `4` |
| `THREAD_POOL_BULKHEAD` | `4`（与 `BULKHEAD` 同值） |

这复现了 [Resilience4j 推荐的切面顺序](https://resilience4j.readme.io/docs/getting-started-3#aspect-order)：
`Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( Function ) ) ) ) )`。
你可以用 `setPriority(int)` 逐个模板地修改它。当心 `Prioritized` 的常量：
`Prioritized.MAX_PRIORITY` 的值是 `Integer.MIN_VALUE`，而 `MIN_PRIORITY` 是 `Integer.MAX_VALUE`，所以
`setPriority(Prioritized.MAX_PRIORITY)` 会把模板放到**最外层**，而不是最内层。

---

## 5. API 参考 — 抽象

### 5.1 `io.microsphere.resilience4j.common.Resilience4jOperations<E>`

单个模块的最小契约。`E` 是 Resilience4j 的入口类型（例如 `CircuitBreaker`）。
除 `begin` 和 `end` 之外的所有方法都是 `default` 方法。

| 方法 | 签名 | 参数 | 返回 | 抛出 |
|--------|-----------|--------|---------|--------|
| `execute` | `default void execute(String name, Runnable callback)` | `name` 入口名；`callback` 无返回结果的代码 | `void` | 仅非受检异常（见下方说明） |
| `execute` | `default <T> T execute(String name, Supplier<T> callback)` | `callback` 有返回结果的代码 | 回调的结果 | 任何 `Throwable`：**若本身已是 `RuntimeException` 则原样重抛，否则通过 `ExceptionUtils.wrap(t, RuntimeException.class)` 包装成 `RuntimeException`** |
| `call` | `default void call(String name, ThrowableAction callback) throws Throwable` | `ThrowableAction` = `void execute() throws Throwable` | `void` | `Throwable`（任何错误，包括受检异常） |
| `call` | `default <TR extends Throwable> void call(String name, ThrowableAction callback, Class<TR> throwableClass) throws TR` | 外加 `throwableClass`：调用方声明的异常类型 | `void` | `TR` —— 该失败会被重新类型化/包装为 `throwableClass` |
| `call` | `default <T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `ThrowableSupplier<T>` = `T get() throws Throwable` | 结果 | `Throwable` |
| `call` | `default <T, TR extends Throwable> T call(String name, ThrowableSupplier<T> callback, Class<TR> throwableClass) throws TR` | 外加 `throwableClass` | 结果 | `TR` |
| `isBeginSupported` | `default boolean isBeginSupported()` | — | 除非模块覆写，否则为 `true` | — |
| `begin` | `Resilience4jContext<E> begin(String name)` | **抽象方法** | 非空上下文（第一阶段） | 不支持时通常抛 `UnsupportedOperationException` |
| `isEndSupported` | `default boolean isEndSupported()` | — | 除非覆写，否则为 `true` | — |
| `end` | `void end(Resilience4jContext<E> context)` | **抽象方法** | `void`（第二阶段） | 不支持时通常抛 `UnsupportedOperationException` |

实际使用中需要注意的行为：

- `execute(name, Runnable)`、`execute(name, Supplier)` 和 `call(name, ThrowableAction)` 都是建立在
  `call(name, ThrowableSupplier)` 之上的薄适配器。因此在子类里覆写 `call(String, ThrowableSupplier)`
  会改变**每一个**一次性重载的行为。
- `execute` 是*不抛受检异常*的门面；`call` 是*声明 throws Throwable* 的门面。当你的业务代码会抛出受检异常
  （JDBC、IO、`Feign` 解码……）而又不想包装它们时，就选 `call`。
- `execute(name, supplier)` 从不吞掉任何东西：当 `t instanceof RuntimeException` 时，
  `wrap(t, RuntimeException.class)` 返回的就是同一个实例，所以 `CallNotPermittedException` 或 `BulkFullException`
  依然原封不动地到达你手里。
- 这对两阶段方法本身并*不*是异常安全的 —— `begin()` 返回一个上下文，你必须把它交给 `end()`。

### 5.2 `io.microsphere.resilience4j.common.AdvancedResilience4jOperations<E, C, R extends Registry<E, C>>`

`extends Resilience4jOperations<E>, io.microsphere.lang.Prioritized`。新增注册表自省、
配置/入口管理、入口级（`Consumer`/`Function`）执行以及日志访问。

**Getter**

| 方法 | 返回 | 说明 |
|--------|---------|-------|
| `R getRegistry()` | 被包装的 `Registry` | 在本接口中为抽象方法 |
| `Resilience4jModule getModule()` | 该模块 | 在本接口中为抽象方法 |
| `Class<E> getEntryClass()` | 例如 `CircuitBreaker.class` | 默认实现：`getModule().getEntryClass()`（未经检查的强制转换） |
| `Class<C> getConfigClass()` | 例如 `CircuitBreakerConfig.class` | 默认实现：来自该模块 |
| `Logger getLogger()` | `io.microsphere.logging.Logger` | 抽象方法；模板返回它自己的 logger |

**配置操作**

| 方法 | 参数 | 返回 | 行为 |
|--------|--------|---------|-----------|
| `getDefaultConfig()` | — | 非空 `C` | `registry.getDefaultConfig()` |
| `getConfiguration(String configName)` | `configName` | 非空 `C` | `registry.getConfiguration(configName)`；缺失时**回退到 `getDefaultConfig()`**（绝不返回 null） |
| `addConfiguration(String configName, C configuration)` | 名称 + 配置 | `AdvancedResilience4jOperations<E, C, R>`（`this`） | `registry.addConfiguration(...)`；链式 |

**入口操作**

| 方法 | 参数 | 返回 | 行为 |
|--------|--------|---------|-----------|
| `getEntry(String name)` | 入口名 | 非空 `E` | `registry.find(name)`，找不到则 `createEntry(name)` |
| `createEntry(String name)` | 入口名 | 非空 `E` | **抽象方法** —— 由各模板分别实现 |
| `removeEntry(String name)` | 入口名 | `E` 或 `null` | `registry.remove(name)`；没有注册过任何内容时返回 `null` |
| `replaceEntry(String name, E newEntry)` | 名称、新入口 | 旧的 `E` 或 `null` | `registry.replace(name, newEntry)`；当该名称未知时，会添加新入口并返回 `null` |

**入口级一次性操作**（注意：这些方法**不**做任何装饰 —— 它们把原始入口直接交给你）

| 方法 | 参数 | 返回 | 抛出 |
|--------|--------|---------|--------|
| `execute(String name, Consumer<E> entryConsumer)` | 原始入口消费者 | `this`（链式） | 仅非受检异常 |
| `execute(String name, Function<E, T> entryFunction)` | 原始入口函数 | `T` | 仅非受检异常 |
| `call(String name, ThrowableConsumer<E> entryConsumer) throws Throwable` | 原始入口消费者 | `this` | `Throwable` |
| `call(String name, ThrowableConsumer<E> entryConsumer, Class<TR>) throws TR` | + 目标异常类型 | `this` | `TR` |
| `call(String name, ThrowableFunction<E, T> entryFunction) throws Throwable` | 原始入口函数 | `T` | `Throwable` |
| `call(String name, ThrowableFunction<E, T> entryFunction, Class<TR>) throws TR` | + 目标异常类型 | `T` | `TR` |

> **不要混淆这两族方法。** `execute(name, () -> …)` / `call(name, () -> …)`（零参
> lambda）是**经过装饰的**执行。`execute(name, entry -> …)` / `call(name, entry -> …)`
> （单参 lambda）是**原始入口访问** —— 没有 `acquirePermission`、没有装饰、没有事件。
> 它们是为管理用途而存在的，例如 `template.execute(name, CircuitBreaker::reset)`（在
> `CircuitBreakerTemplateTest` 中已验证）。

### 5.3 `io.microsphere.resilience4j.common.Resilience4jTemplate<E, C, R extends Registry<E, C>>`

`abstract class`，实现 `AdvancedResilience4jOperations<E, C, R>`。这就是你要继承的
基类（或者，是你要直接实例化的六个具体子类）。

**构造器**

| 构造器 | 行为 |
|-------------|-----------|
| `public Resilience4jTemplate(R registry)` | `this(registry, true)` —— 本地入口缓存**启用** |
| `public Resilience4jTemplate(R registry, boolean localEntriesCached)` | `registry` 不得为 `null`（microsphere 的 `Assert.assertNotNull`，消息为 `"The registry must not be null"`）；通过 `Resilience4jModule.valueOf(...)` 从 `registry.getClass()` 解析出 `module`；构建注册表的 `EventProcessor`；当 `localEntriesCached` 为 `true` 时创建本地缓存；设置 `priority = module.getDefaultAspectOrder()` |

**公开 / 受保护的 API**

| 方法 | 返回 | 说明 |
|--------|---------|-------|
| `final R getRegistry()` | 非空 | |
| `final Resilience4jModule getModule()` | 非空 | |
| `final C getDefaultConfig()` | 非空 | 委托给接口的默认实现 |
| `final Class<E> getEntryClass()` / `final Class<C> getConfigClass()` | 非空 | |
| `final Logger getLogger()` | 非空 | |
| `final Resilience4jTemplate<E, C, R> initLocalEntriesCache(String name)` | `this` | 预热缓存：解析（因而**创建**）入口并存入 |
| `final Resilience4jTemplate<E, C, R> initLocalEntriesCache(Iterable<String> names)` | `this` | 对多个名字执行同样操作 |
| `final E getEntry(String name)` | 非空 | 缓存 → 注册表 → `createEntry(name)` |
| `protected Map<String, E> createLocalEntriesCache()` | 默认为 `HashMap`；`null` 表示"无缓存" | 覆写以定制或禁用缓存 |
| `protected final boolean isLocalEntriesCached()` | `boolean` | `localEntriesCache != null` |
| `protected final void clearLocalEntriesCache()` | `void` | |
| `protected final E getEntryFromCache(String name)` | `E` 或 `null` | |
| `final AdvancedResilience4jOperations<E, C, R> addConfiguration(String, C)` | `this` | |
| `final C getConfiguration(String configName)` | 非空 | 回退到默认配置 |
| `final Resilience4jContext<E> begin(String name)` | 非空上下文 | 解析入口，构建 `new Resilience4jContext<>(name, entry)`，然后调用 `doBegin(context)`。**`final`** —— 请改为覆写那个钩子 |
| `protected void doBegin(Resilience4jContext<E> context)` | `void` | 默认实现**抛出 `UnsupportedOperationException`**，消息为 `"<class> does not support begin operation"` |
| `final void end(Resilience4jContext<E> context)` | `void` | 调用 `doEnd(context)`。**`final`** |
| `protected void doEnd(Resilience4jContext<E> context)` | `void` | 默认实现**抛出 `UnsupportedOperationException`**（`"… does not support end operation"`） |
| `final Resilience4jTemplate<E, C, R> onEntryAddedEvent(EventConsumer<EntryAddedEvent<E>>)` | `this` | 注册表级的生命周期事件 |
| `final Resilience4jTemplate<E, C, R> onEntryRemovedEvent(EventConsumer<EntryRemovedEvent<E>>)` | `this` | |
| `final Resilience4jTemplate<E, C, R> onEntryReplacedEvent(EventConsumer<EntryReplacedEvent<E>>)` | `this` | |
| `protected final <T extends RegistryEvent> Resilience4jTemplate<E, C, R> registerRegistryEventConsumer(Class<? super T>, EventConsumer<T>)` | `this` | 用于自定义注册表事件 |
| `protected final <T> Resilience4jTemplate<E, C, R> registerEntryEventConsumer(String entryName, Class<? super T>, EventConsumer<T>)` | `this` | 被所有具体的 `onXxxEvent(...)` 方法使用；它会先解析 `getEntry(entryName)`，所以挂载一个监听器时，如果入口缺失就会**创建该入口** |
| `final int getPriority()` | 优先级 | `Prioritized` 的实现 |
| `final Resilience4jTemplate<E, C, R> setPriority(int priority)` | `this` | 影响门面的排序 |
| `public void destroy()` | `void` | 清空本地缓存；在你弃用一个模板时调用它 |
| `String toString()` | — | `"<className>{registry = …, module = …, registryEventProcessor = …, localEntriesCache = …, priority = …}"` |

由于 `begin`、`end`、`getEntry`、`getRegistry`、`getModule` 以及优先级访问器都是 `final`，
子类只能通过 `doBegin` / `doEnd` / `createEntry` / `call(String, ThrowableSupplier)`
来自定义行为。

**本地入口缓存的线程安全性** —— Javadoc 说得很直白：该缓存是一个 `HashMap`，
即*并非*线程安全；**只有当**它在并发使用之前、于初始化阶段被 `initLocalEntriesCache(...)` 完全填充后，
才是安全的。此后的读写是安全的，因为入口已经就位。如果你从请求线程里惰性调用 `initLocalEntriesCache`，
请禁用缓存（`new XxxTemplate(registry, false)`）。

### 5.4 `Resilience4jFacade` 与 `ChainableResilience4jFacade`

`Resilience4jFacade` 是一个标记接口：

```java
public interface Resilience4jFacade extends Resilience4jOperations<Resilience4jContext[]> {
}
```

它唯一的入口类型是 `Resilience4jContext[]` —— 即每个模板的子上下文组成的数组。

**`ChainableResilience4jFacade`**（`public class`，实现 `Resilience4jFacade`）

| 构造器 | 参数 | 行为 |
|-------------|--------|-----------|
| `ChainableResilience4jFacade(Registry... registries)` | 可变参数注册表 | `this(Arrays.asList(registries))` |
| `ChainableResilience4jFacade(Collection<Registry> registries)` | 注册表 | `Resilience4jUtils.createTemplates(registries)` —— 每个**互异**的注册表实例一个模板，已完成去重和优先级排序；空集合 → 空列表 |
| `ChainableResilience4jFacade(List<Resilience4jTemplate> templates)` | 现成的模板 | 用 `List#contains` 去重（按引用，因为 `Resilience4jTemplate` 没有覆写 `equals`），然后用 `Prioritized.COMPARATOR` 排序（优先级升序） |

| 方法 | 返回 | 行为 |
|--------|---------|-----------|
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | 回调的结果 | 把所有模板上的回调包装进一个 `CallbackChain`（见 §5.5 和 §7.9） |
| `Resilience4jContext<Resilience4jContext[]> begin(String name)` | 外层上下文 | 分配 `new Resilience4jContext[size]`，对每个 `isBeginSupported()` 为 `true` 的模板调用 `template.begin(name)`，跳过其余的（留下 `null` 槽位） |
| `void end(Resilience4jContext<Resilience4jContext[]> context)` | `void` | 对每个 `isEndSupported()` 为 `true` 的模板调用 `template.end(subContexts[i])`，**顺序与 `begin` 相同的前向顺序**（并非 LIFO） |
| `List<Resilience4jTemplate> getTemplates()` | 非空、有序 | 生效的模板列表 |
| `int getSize()` | `>= 0` | 生效（去重后）的模板数量 |
| `void destroy()` | `void` | 对每个模板调用 `destroy()` |

从 `Resilience4jOperations` 继承而来：`execute(name, Runnable)`、`execute(name, Supplier)`、
`call(name, ThrowableAction)`、`call(name, ThrowableAction, Class)` 以及 `isBeginSupported` /
`isEndSupported` 的默认实现（两者都为 `true`）。门面**并不**实现
`AdvancedResilience4jOperations`，所以 `getRegistry()`、`getEntry(name)` 以及原始入口级的
`Consumer`/`Function` 重载在它上面都**不可用**。

### 5.5 `CallbackChain<T>`（包私有）

`class CallbackChain<T> implements ThrowableSupplier<T>` —— **非 public**，它位于
`io.microsphere.resilience4j.common`，由 `ChainableResilience4jFacade.call` 创建。它的构造器是
`CallbackChain(String entryName, ThrowableSupplier<T> delegate, List<Resilience4jTemplate> templates)`，
而 `get()` 用一个游标（`pos`）遍历模板列表，反复调用
`templates.get(pos++).call(entryName, CallbackChain.this::get)`，直到列表耗尽，然后再调用
真正的业务回调。你自己从不构造它；你只能通过门面的顺序来观察它。

### 5.6 `Resilience4jContext<E>`

`public class`，一个由 `begin(...)` 创建的可变、每次执行专属的持有者。它的构造器是 **`protected`**，所以
从普通应用代码里你无法调用 `new Resilience4jContext<>(...)` —— 你从 `template.begin(name)`
（或 `facade.begin(name)`）获取实例。只有 `io.microsphere.resilience4j.common` 中的类、它的子类，
以及位于该包内的测试辅助类（例如 `AbstractResilience4jTemplateTest#testEnd`）才能直接构造它。

| 成员 | 签名 | 说明 |
|--------|-----------|-------|
| 构造器 | `protected Resilience4jContext(String entryName, E entry)` | 拒绝 `null` 的名称/入口（microsphere 的 `Assert.assertNotNull`） |
| `String getEntryName()` | 非空 | |
| `E getEntry()` | 非空（对于门面，它就是 `Resilience4jContext[]` 数组） | |
| `setStartTime(Long)` / `getStartTime()` | 链式 / 可空 | `Long`（装箱）—— `CircuitBreakerTemplate.doBegin` 会填充它，`BulkheadTemplate` 和 `RateLimiterTemplate` 不会 |
| `setResult(Object)` / `getResult()` | 链式 / 可空 | 在两阶段用法中你必须自行设置它 |
| `setFailure(Throwable)` / `getFailure()` | 链式 / 可空 | 在两阶段用法中你必须自行设置它 |
| `setAttribute(String, Object)` / `hasAttribute(String)` / `getAttribute(String)` / `getAttribute(String, T)` / `removeAttribute(String)` / `removeAttributes()` / `getAttributes()` | 链式 / 强类型 | 属性 map 惰性创建；在此之前 `getAttributes()` 返回 `Collections.emptyMap()`，之后则是一个只读的*实时*视图 |
| `Resilience4jContext<E> withinContext()` | `this` | 把该上下文绑定到当前线程（`ThreadLocal`） |
| `static Resilience4jContext getContext()` | 可空 | 当前线程的上下文 |
| `static void setContext(Resilience4jContext context)` | — | **替换**当前上下文（允许 `null`） |
| `static Resilience4jContext removeContext()` | 可空 | 返回并解绑 |
| `static void doInContext(Consumer<Resilience4jContext> consumer)` | — | 便捷方法：在当前上下文上运行该消费者 |
| `static void doInContext(Consumer<Resilience4jContext> consumer, boolean forRemoval)` | — | 当 `forRemoval` 为 `true` 时，先移除上下文 |
| `String toString()` | — | `Resilience4jContext{entryName='…', entry=…, startTime=…, result=…, failure=…, attributes=…}` |

有两个值得钉死的行为（都能由 `Resilience4jContextTest` 证明）：

1. **当没有任何绑定内容时，`doInContext` 保持静默。** 测试的最后一条语句运行了一个会
   抛 `new RuntimeException("Impossible")` 的消费者，并期望什么都不发生 —— 因为
   `getContext()` 返回了 `null`，消费者根本不会被调用。
2. **线程绑定是单个槽位，不是栈。** `setContext` 会覆盖；嵌套的
   `begin(...)`/`withinContext()` 调用会丢失外层上下文，除非你自己保存并恢复它。

### 5.7 `Resilience4jModule`

`public enum`，含六个常量及其元数据。构造器是枚举私有的；你只消费它。

| 方法 | 返回 |
|--------|---------|
| `String getName()` | `"retry"`、`"circuit-breaker"`、`"rate-limiter"`、`"time-limiter"`、`"bulkhead"`、`"thread-pool-bulkhead"` |
| `Class<?> getEntryClass()` | `Retry` / `CircuitBreaker` / `RateLimiter` / `TimeLimiter` / `Bulkhead` / `ThreadPoolBulkhead` |
| `Class<?> getConfigClass()` | `RetryConfig` / `CircuitBreakerConfig` / `RateLimiterConfig` / `TimeLimiterConfig` / `BulkheadConfig` / `ThreadPoolBulkheadConfig` |
| `Class<? extends CommonProperties> getConfigurationPropertiesClass()` | `CommonRetryConfigurationProperties` / `CommonCircuitBreakerConfigurationProperties` / `CommonRateLimiterConfigurationProperties` / `CommonTimeLimiterConfigurationProperties` / `CommonBulkheadConfigurationProperties` / `CommonThreadPoolBulkheadConfigurationProperties` |
| `Class<?> getEventClass()` | `RetryEvent` / `CircuitBreakerEvent` / `RateLimiterEvent` / `TimeLimiterEvent` / `BulkheadEvent` / `BulkheadEvent`（与 `BULKHEAD` 共用） |
| `Class<? extends Registry> getRegistryClass()` | `RetryRegistry` / `CircuitBreakerRegistry` / `RateLimiterRegistry` / `TimeLimiterRegistry` / `BulkheadRegistry` / `ThreadPoolBulkheadRegistry` |
| `int getDefaultAspectOrder()` | `0`、`1`、`2`、`3`、`4`、`4` |
| `static Resilience4jModule valueOf(Class<?> type)` | 其 entry/config/properties/event/registry 类可由 `type` 赋值（isAssignableFrom）的那个模块 |
| `String toString()` | `"Resilience4jModule{name = …, entryClass = …, configClass = …, configurationPropertiesClass = …, eventClass = …, registryClass = …, defaultAspectOrder = …}"` |

`valueOf(Class)` 的细节（由 `Resilience4jModuleTest` 证明）：

- 它是对隐式的 `Enum.valueOf(String)` 的一个**重载**；调用 `valueOf(Retry.class)`、
  `valueOf(RetryConfig.class)`、`valueOf(RetryEvent.class)` 或 `valueOf(RetryRegistry.class)` —— 这四者
  都返回 `RETRY`。
- 枚举成员按声明顺序（`RETRY`、`CIRCUIT_BREAKER`、`RATE_LIMITER`、`TIME_LIMITER`、
  `BULKHEAD`、`THREAD_POOL_BULKHEAD`）被扫描，**第一个**匹配胜出。因为 `BULKHEAD` 和
  `THREAD_POOL_BULKHEAD` 共用 `BulkheadEvent`，所以 `valueOf(BulkheadEvent.class)` 得到 `BULKHEAD`。
- 无匹配 → `IllegalArgumentException("The 'type' can't be found in Resilience4jModule : " + type.getName())`，
  例如 `valueOf(Resilience4jModule.class)`。

### 5.8 `Resilience4jConstants`

`public interface`，持有若干 `String` 常量（配置命名空间的构件，被 Spring
各模块和插件作者使用）：

| 常量 | 值 |
|----------|-------|
| `PREFIX` | `"microsphere.resilience4j."` |
| `RETRY_MODULE_NAME` / `RETRY_PREFIX` | `"retry"` / `"microsphere.resilience4j.retry."` |
| `CIRCUIT_BREAKER_MODULE_NAME` / `CIRCUIT_BREAKER_PREFIX` | `"circuit-breaker"` / `"microsphere.resilience4j.circuit-breaker."` |
| `RATE_LIMITER_MODULE_NAME` / `RATE_LIMITER_PREFIX` | `"rate-limiter"` / `"microsphere.resilience4j.rate-limiter."` |
| `TIME_LIMITER_MODULE_NAME` / `TIME_LIMITER_PREFIX` | `"time-limiter"` / `"microsphere.resilience4j.time-limiter."` |
| `BULKHEAD_MODULE_NAME` / `BULKHEAD_PREFIX` | `"bulkhead"` / `"microsphere.resilience4j.bulkhead."` |
| `THREAD_POOL_BULKHEAD_MODULE_NAME` / `THREAD_POOL_BULKHEAD_PREFIX` | `"thread-pool-bulkhead"` / `"microsphere.resilience4j.thread-pool-bulkhead."` |

### 5.9 `io.microsphere.resilience4j.util.Resilience4jUtils`

`public abstract class ... implements io.microsphere.util.Utils`，带一个**私有构造器** —— 只提供静态方法。

| 静态方法 | 参数 | 返回 | 抛出 |
|---------------|--------|---------|--------|
| `createTemplate(R registry)` | 任意 `Registry` | 该模块默认的 `Resilience4jTemplate`，以注册表作为唯一构造参数经反射实例化 | — |
| `createTemplates(Collection<Registry> registries)` | 注册表 | 非空**只读** `List<Resilience4jTemplate>`，每个互异注册表一个，按优先级升序排序；空输入 → `Collections.emptyList()` | — |
| `getEntry(Registry<E, C> registry, String name)` | 注册表 + 名称 | 以注册表的**默认**配置创建/获取的入口 | — |
| `getEntry(Registry<E, C> registry, String name, C configuration)` | + 显式配置 | 通过注册表的 `"<entry>"(String, Config)` 方法、以该配置创建的入口 | 当配置类型不属于该注册表时，把反射失败包装成 `IllegalArgumentException` |
| `getEventProcessor(E entry)` | 任意受支持的入口 | 该入口事件发布器对应的 `EventProcessor` | 对未知类型的入口抛 `UnsupportedOperationException` |
| `getEventProcessor(Retry / TimeLimiter / RateLimiter / Bulkhead / ThreadPoolBulkhead / CircuitBreaker)` | 强类型入口 | `EventProcessor` | — |
| `getEventProcessor(Registry registry)` | 注册表 | 把注册表的 `EventPublisher` 作为 `EventProcessor` 返回 | 当发布器不是 `EventProcessor`（或为 `null`）时抛 `IllegalArgumentException` |

由 `Resilience4jUtilsTest` 验证的说明：

- `Resilience4jUtils.getEntry(registry, name)` 会同时绕过本地缓存**和**具名配置；
  请优先用 `template.getEntry(name)`。
- `createTemplate(ThreadPoolBulkheadRegistry.ofDefaults())` 返回一个 `ThreadPoolBulkheadTemplate`，
  也就是说全部六个模块都是通过 `META-INF/default/templates.properties` 接线的。

---

## 6. API 参考 — 六个模板

每个模板都暴露两个构造器，`(registry)` 和 `(registry, boolean localEntriesCached)`，外加模块强类型的
`onXxxEvent(String entryName, EventConsumer<...>)` 链式注册方法，返回模板自身。`TimeLimiterTemplate`
还多出两个构造器（见下）。

### 6.1 `CircuitBreakerTemplate`（`io.microsphere.resilience4j.circuitbreaker`）

`extends Resilience4jTemplate<CircuitBreaker, CircuitBreakerConfig, CircuitBreakerRegistry>`

| 成员 | 细节 |
|--------|---------|
| `CircuitBreakerTemplate(CircuitBreakerRegistry registry)` | 缓存启用 |
| `CircuitBreakerTemplate(CircuitBreakerRegistry registry, boolean localEntriesCached)` | |
| `CircuitBreaker createEntry(String name)` | `registry.circuitBreaker(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `circuitBreaker.decorateCheckedSupplier(callback::get).get()` —— 完整的装饰器，**绕过 `begin`/`end`** |
| `doBegin(Resilience4jContext<CircuitBreaker>)` | `acquirePermission()` + 从 `circuitBreaker.getCurrentTimestamp()` 记录 `startTime` |
| `doEnd(Resilience4jContext<CircuitBreaker>)` | 计算 `duration = getCurrentTimestamp() - startTime`，随后在无失败且无结果时调用 `onSuccess(duration, unit)`，在有结果时调用 `onResult(duration, unit, result)`，否则调用 `onError(duration, unit, failure)` |
| `isBeginSupported()` / `isEndSupported()` | 继承来的 `true` |

事件注册方法：`onCallNotPermittedEvent`、`onErrorEvent`、`onFailureRateExceededEvent`、
`onIgnoredErrorEvent`、`onResetEvent`、`onSlowCallRateExceededEvent`、`onStateTransitionEvent`、
`onSuccessEvent` —— 每个都是 `(String entryName, EventConsumer<CircuitBreakerOn…Event>)`。

### 6.2 `RateLimiterTemplate`（`io.microsphere.resilience4j.ratelimiter`）

`extends Resilience4jTemplate<RateLimiter, RateLimiterConfig, RateLimiterRegistry>`

| 成员 | 细节 |
|--------|---------|
| 构造器 | `(RateLimiterRegistry)` / `(RateLimiterRegistry, boolean)` |
| `RateLimiter createEntry(String name)` | `registry.rateLimiter(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `RateLimiter.decorateCheckedSupplier(rateLimiter, callback::get).get()` |
| `public void doBegin(Resilience4jContext<RateLimiter>)` | `RateLimiter.waitForPermission(rateLimiter)`（声明为 `public`，放宽了基类的 `protected` 钩子） |
| `doEnd(Resilience4jContext<RateLimiter>)` | `onSuccess()` / `onResult(result)` / `onError(failure)` —— 不涉及 duration |
| 事件 | `onSuccessEvent`、`onFailureEvent`、`onDrainedEvent` |

### 6.3 `RetryTemplate`（`io.microsphere.resilience4j.retry`）

`extends Resilience4jTemplate<Retry, RetryConfig, RetryRegistry>`

| 成员 | 细节 |
|--------|---------|
| 构造器 | `(RetryRegistry)` / `(RetryRegistry, boolean)` |
| `Retry createEntry(String name)` | `registry.retry(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `Retry.decorateCheckedSupplier(retry, callback::get).get()` |
| `isBeginSupported()` | **`false`** |
| `isEndSupported()` | **`false`** |
| 事件 | `onErrorEvent`、`onIgnoredErrorEvent`、`onRetryEvent`、`onSuccessEvent` |

`begin(name)` / `end(context)` 会抛 `UnsupportedOperationException`：重试需要一次性拿到整个调用体，
无法表达为进入/退出成对操作。

### 6.4 `BulkheadTemplate`（`io.microsphere.resilience4j.bulkhead`）

`extends Resilience4jTemplate<Bulkhead, BulkheadConfig, BulkheadRegistry>`

| 成员 | 细节 |
|--------|---------|
| 构造器 | `(BulkheadRegistry)` / `(BulkheadRegistry, boolean)` |
| `Bulkhead createEntry(String name)` | `registry.bulkhead(name, getConfiguration(name), registry.getTags())` —— 注册表的标签被传播到实例上 |
| `call(String name, ThrowableSupplier<T>)` | **未覆写** → 接口的默认实现 `begin → callback → end` 会运行，因此舱壁上的 `execute`/`call` 都走两阶段路径 |
| `doBegin(Resilience4jContext<Bulkhead>)` | `bulkhead.acquirePermission()`（当超过 `maxWaitDuration` 仍满载时抛 `BulkFullException`；它**不**记录 `startTime`） |
| `doEnd(Resilience4jContext<Bulkhead>)` | `bulkhead.onComplete()` |
| 事件 | `onCallPermittedEvent`、`onCallRejectedEvent`、`onCallFinishedEvent` |

### 6.5 `ThreadPoolBulkheadTemplate`（`io.microsphere.resilience4j.bulkhead`）

`extends Resilience4jTemplate<ThreadPoolBulkhead, ThreadPoolBulkheadConfig, ThreadPoolBulkheadRegistry>`

| 成员 | 细节 |
|--------|---------|
| 构造器 | `(ThreadPoolBulkheadRegistry)` / `(ThreadPoolBulkheadRegistry, boolean)` |
| `ThreadPoolBulkhead createEntry(String name)` | `registry.bulkhead(name, getConfiguration(name), registry.getTags())` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | 通过 `executeCallable` 把 `() -> ThrowableSupplier.execute(callback)` 提交给舱壁的线程池，转换为 `CompletableFuture` 并**阻塞在 `future.get()` 上**；任何 `ExecutionException` 都会被解包到其**根因**并重抛 |
| `isBeginSupported()` / `isEndSupported()` | **`false`**（`begin`/`end` 抛 `UnsupportedOperationException`） |
| 事件 | `onCallPermittedEvent`、`onCallRejectedEvent`、`onCallFinishedEvent` |
| 辅助方法 | `static Throwable getRootCause(Throwable)` —— 包私有，沿 `getCause()` 一直走到最深的根因 |

由此带来的后果是：回调运行在池线程上（不继承 `ThreadLocal`），失败以最*深层*的根因形式到达（一个本身带
cause 的用户异常会丢失它的外层包装），并且 `call` 会阻塞，所以"异步"舱壁经由这个模板表现得是同步的。

### 6.6 `TimeLimiterTemplate`（`io.microsphere.resilience4j.timelimiter`）

`extends Resilience4jTemplate<TimeLimiter, TimeLimiterConfig, TimeLimiterRegistry>`

| 成员 | 细节 |
|--------|---------|
| `TimeLimiterTemplate(TimeLimiterRegistry registry)` | executor = `ForkJoinPool.commonPool()`，缓存启用 |
| `TimeLimiterTemplate(TimeLimiterRegistry registry, boolean localEntriesCached)` | executor = 公共池 |
| `TimeLimiterTemplate(TimeLimiterRegistry registry, ExecutorService executorService)` | 缓存启用 |
| `TimeLimiterTemplate(TimeLimiterRegistry registry, ExecutorService executorService, boolean localEntriesCached)` | 完全可控 |
| `TimeLimiter createEntry(String name)` | `registry.timeLimiter(name, getConfiguration(name))` |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | `timeLimiter.decorateFutureSupplier(() -> executorService.submit(...)).call()`；任务内部用 `ExceptionUtils.wrap(t, Exception.class)` 转换回调的 `Throwable`，因为 `Callable` 只能抛 `Exception` |
| `isBeginSupported()` / `isEndSupported()` | **`false`** |
| 事件 | `onSuccessEvent`、`onErrorEvent`、`onTimeoutEvent` |

带有 `cancelRunningFuture(true)` 的 `TimeLimiterConfig` 会在超时时取消已提交的任务 —— 但回调线程本身只是
被中断；并不保证它会停止。

### 6.7 模板能力矩阵

| 模板 | 是否覆写 `call(String, ThrowableSupplier)` | `doBegin` / `doEnd` | `isBeginSupported` / `isEndSupported` | 额外状态 |
|----------|-------------------------------------------|---------------------|----------------------------------------|-------------|
| `CircuitBreakerTemplate` | 是（装饰器） | 已实现 | `true` / `true` | 记录 `startTime`，基于 duration 的上报 |
| `RateLimiterTemplate` | 是（装饰器） | 已实现 | `true` / `true` | `doBegin` 为 `public` |
| `RetryTemplate` | 是（装饰器） | 未实现 | **`false` / `false`** | — |
| `BulkheadTemplate` | **否** → 使用 `begin`/`end` | 已实现 | `true` / `true` | 不记录 `startTime` |
| `ThreadPoolBulkheadTemplate` | 是（线程池 + 阻塞式 `get()`） | 未实现 | **`false` / `false`** | 根因解包 |
| `TimeLimiterTemplate` | 是（executor + `Callable`） | 未实现 | **`false` / `false`** | 拥有一个 `ExecutorService` |

---

## 7. 代码示例

所有示例都取自或改编自本模块自己的测试源码（`.../src/test/java/io/microsphere/resilience4j/...`）；它们能
针对上面的签名编译通过。与测试中一致，`entryName` 始终是 `"test-entry"` 或 `"test"`。

### 7.1 断路器：一次性执行 + 成功事件

改编自 `CircuitBreakerTemplateTest#testExecute`。

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
CircuitBreakerTemplate template = new CircuitBreakerTemplate(registry);

String entryName = "test-entry";

template.onSuccessEvent(entryName, event -> {
    // event is a CircuitBreakerOnSuccessEvent
    System.out.printf("circuit breaker '%s' succeeded%n", event.getCircuitBreakerName());
});

// execute(String, Supplier<String>) -> the decorator path (CircuitBreaker.decorateCheckedSupplier)
String result = template.execute(entryName, () -> entryName);
assert result.equals("test-entry");

template.destroy();
```

### 7.2 断路器：按入口名配置专属配置

改编自 `AbstractResilience4jTemplateTest#setUp` + `CircuitBreakerTemplateTest#createEntryConfig`。

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

import java.time.Duration;

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());

CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(10)
        .maxWaitDurationInHalfOpenState(Duration.ofMillis(50))
        .ignoreExceptions(RuntimeException.class)
        .recordExceptions(Exception.class)
        .slowCallDurationThreshold(Duration.ofMillis(50))
        .permittedNumberOfCallsInHalfOpenState(2)
        .slowCallRateThreshold(10)
        .writableStackTraceEnabled(false)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .build();

// The configuration must be registered under the ENTRY name before the entry is first resolved.
template.addConfiguration("test-entry", config);

CircuitBreaker entry = template.getEntry("test-entry"); // created with `config`
assert entry.getState() == CircuitBreaker.State.CLOSED;
assert template.getEntry("test-entry") == entry;        // registry keeps one instance per name
```

### 7.3 断路器：错误、被忽略的错误、重置，以及原始入口重载

改编自 `CircuitBreakerTemplateTest#testExecuteOnCallFailure`。它展示了单参数的
`execute(String, Consumer<CircuitBreaker>)` 重载与零参数的经过装饰的那个重载有何不同，以及受检异常如何从
`call` 中冒出来。

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

String entryName = "test-entry";

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());
template.addConfiguration(entryName, CircuitBreakerConfig.custom()
        .failureRateThreshold(10)
        .ignoreExceptions(RuntimeException.class)
        .recordExceptions(Exception.class)
        .build());

template.onErrorEvent(entryName, event -> System.out.println("ERROR: " + event.getThrowable()));
template.onIgnoredErrorEvent(entryName, event -> System.out.println("IGNORED: " + event.getThrowable()));
template.onFailureRateExceededEvent(entryName, event -> System.out.println("failure rate exceeded"));
template.onStateTransitionEvent(entryName, event -> System.out.println("state: " + event.getStateTransition()));
template.onCallNotPermittedEvent(entryName, event -> System.out.println("not permitted"));
template.onResetEvent(entryName, event -> System.out.println("reset"));

// call(String, ThrowableAction) throws Throwable - checked exceptions are recorded, not wrapped
try {
    template.call(entryName, () -> {
        throw new Exception("For testing");
    });
} catch (Throwable ignored) {
    // the recorded failure reaches the circuit breaker first
}

// RuntimeException is in `ignoreExceptions(...)`, so it is reported as an ignored error
try {
    template.call(entryName, () -> {
        throw new RuntimeException();
    });
} catch (RuntimeException ignored) {
}

// One-argument lambda == AdvancedResilience4jOperations.execute(String, Consumer<CircuitBreaker>):
// raw entry access, NO decoration.
template.execute(entryName, CircuitBreaker::reset);
```

### 7.4 舱壁隔离：把并发限制为一次调用

改编自 `BulkheadTemplateTest#testExecute` / `#testExecuteOnCallRejected`。

```java
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

BulkheadRegistry registry = BulkheadRegistry.ofDefaults();
BulkheadTemplate template = new BulkheadTemplate(registry);

String entryName = "test-entry";
template.addConfiguration(entryName, BulkheadConfig.custom()
        .maxConcurrentCalls(1)
        .maxWaitDuration(Duration.ofMillis(100))
        .build());

template.onCallPermittedEvent(entryName, event -> System.out.println("permitted " + event.getBulkheadName()));
template.onCallFinishedEvent(entryName, event -> System.out.println("finished " + event.getBulkheadName()));
template.onCallRejectedEvent(entryName, event -> System.out.println("rejected " + event.getBulkheadName()));

// BulkheadTemplate does not override call(...), so this runs begin -> callback -> end
Object result = template.execute(entryName, () -> null);
assert result == null;

// Contention: a saturated bulkhead throws BulkFullException from execute(...)
ExecutorService executorService = newFixedThreadPool(3);
for (int i = 0; i < 3; i++) {
    executorService.execute(() -> template.execute(entryName, () -> {
    }));
}
executorService.shutdown();

template.destroy();
```

### 7.5 限流器：每周期许可数

改编自 `RateLimiterTemplateTest#execute`。

```java
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.microsphere.resilience4j.ratelimiter.RateLimiterTemplate;

import java.time.Duration;

RateLimiterTemplate template = new RateLimiterTemplate(RateLimiterRegistry.ofDefaults());

String entryName = "test-entry";
template.addConfiguration(entryName, RateLimiterConfig.custom()
        .limitForPeriod(1)
        .limitRefreshPeriod(Duration.ofMillis(50))
        .timeoutDuration(Duration.ofMillis(50))
        .build());

template.onSuccessEvent(entryName, event ->
        System.out.println(event.getRateLimiterName() + " permits=" + event.getNumberOfPermits()));
template.onFailureEvent(entryName, event -> System.out.println("acquire failed"));
template.onDrainedEvent(entryName, event -> System.out.println("drained"));

for (int i = 0; i < 5; i++) {
    template.execute(entryName, () -> {          // execute(String, Runnable)
    });
    template.execute(entryName, () -> "For testing"); // execute(String, Supplier<String>)
}
```

### 7.6 带受检异常的重试

改编自 `RetryTemplateTest#testExecute` 和 `#testExecuteOnIgnoredException`。
注意：由于 `call(String, ThrowableSupplier)` 声明了 `Throwable`，外层方法必须声明 `throws Throwable`
（或捕获它）。

```java
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.retry.RetryTemplate;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public void retryExample() throws Throwable {
    RetryTemplate template = new RetryTemplate(RetryRegistry.ofDefaults());

    String entryName = "test-entry";
    int maxAttempts = 3;
    template.addConfiguration(entryName, RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .ignoreExceptions(IllegalStateException.class)
            .retryExceptions(TimeoutException.class)
            .build());

    AtomicInteger attempts = new AtomicInteger(maxAttempts);

    template.onRetryEvent(entryName, event ->
            System.out.println("retry #" + event.getNumberOfRetryAttempts()));
    template.onSuccessEvent(entryName, event ->
            System.out.println("success after " + event.getNumberOfRetryAttempts() + " retries"));
    template.onErrorEvent(entryName, event ->
            System.out.println("final error: " + event.getThrowable()));
    template.onIgnoredErrorEvent(entryName, event ->
            System.out.println("ignored error: " + event.getThrowable()));

    String result = template.call(entryName, () -> {
        if (attempts.decrementAndGet() == 0) {
            return "OK";
        }
        throw new TimeoutException("For testing"); // retried
    });
    assert "OK".equals(result);

    // IllegalStateException is in `ignoreExceptions` -> propagated immediately, no retry
    try {
        template.call(entryName, () -> {
            throw new IllegalStateException("For testing");
        });
    } catch (IllegalStateException expected) {
    }
}
```

### 7.7 使用你自己的 executor 的超时限制

改编自 `TimeLimiterTemplateTest`。

```java
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.Executors.newFixedThreadPool;

ExecutorService executorService = newFixedThreadPool(1);
TimeLimiterTemplate template = new TimeLimiterTemplate(TimeLimiterRegistry.ofDefaults(), executorService);

String entryName = "test-entry";
long timeoutMillis = 50;
template.addConfiguration(entryName, TimeLimiterConfig.custom()
        .cancelRunningFuture(true)
        .timeoutDuration(Duration.ofMillis(timeoutMillis))
        .build());

template.onSuccessEvent(entryName, event -> System.out.println("ok " + event.getTimeLimiterName()));
template.onErrorEvent(entryName, event -> System.out.println("error " + event.getThrowable()));
template.onTimeoutEvent(entryName, event -> System.out.println("timeout " + event.getTimeLimiterName()));

template.execute(entryName, () -> {   // completes within the timeout
});

// Blocking two-phase calls are not supported:
// assertThrows(UnsupportedOperationException.class, () -> template.begin(entryName));

try {
    template.call(entryName, () -> {
        Thread.sleep(timeoutMillis * 3);
    });
} catch (TimeoutException expected) {
    // the callback's own exception (if any) arrives wrapped as an Exception subtype
}

executorService.shutdown();
```

### 7.8 线程池舱壁隔离

改编自 `ThreadPoolBulkheadTemplateTest#testOnEvents`。

```java
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.ThreadPoolBulkheadTemplate;

ThreadPoolBulkheadTemplate template = new ThreadPoolBulkheadTemplate(ThreadPoolBulkheadRegistry.ofDefaults());

String entryName = "test-entry";
template.addConfiguration(entryName, ThreadPoolBulkheadConfig.custom()
        .coreThreadPoolSize(1)
        .maxThreadPoolSize(2)
        .queueCapacity(10)
        .build());

template.onCallPermittedEvent(entryName, event -> System.out.println("permitted " + event.getBulkheadName()));
template.onCallFinishedEvent(entryName, event -> System.out.println("finished " + event.getBulkheadName()));
template.onCallRejectedEvent(entryName, event -> System.out.println("rejected " + event.getBulkheadName()));

// Runs on a pool thread but blocks until the result is available.
String result = template.execute(entryName, () -> entryName);
assert "test-entry".equals(result);
```

### 7.9 可链接门面：把一个调用包裹在六个模块之中

改编自 `ChainableResilience4jFacadeTest`，并把 `Resilience4jTestUtils.createChainableResilience4jFacade()`
内联进来（那个辅助方法位于 `src/test/java`，因此**不在**你的 classpath 上 —— 请复制其逻辑）：

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;

BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
RateLimiterRegistry rateLimiterRegistry = RateLimiterRegistry.ofDefaults();
RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        bulkheadRegistry,
        bulkheadRegistry,          // duplicated registries collapse into one template
        rateLimiterRegistry,
        rateLimiterRegistry,
        timeLimiterRegistry,
        timeLimiterRegistry,
        retryRegistry,
        circuitBreakerRegistry,
        circuitBreakerRegistry);

assert facade.getSize() == 5;                 // 5 distinct registries -> 5 templates
assert facade.getTemplates().size() == 5;
// priority-sorted, lowest first:
// RetryTemplate(0) > CircuitBreakerTemplate(1) > RateLimiterTemplate(2)
//   > TimeLimiterTemplate(3) > BulkheadTemplate(4)

String result = facade.execute("test-entry", () -> "Hello,World"); // wrapped by all five
assert "Hello,World".equals(result);

facade.destroy();
```

用显式模板构造同一个门面（同样会去重，注意那个 `List<Resilience4jTemplate>` 构造器）：

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;

import static java.util.Arrays.asList;

BulkheadTemplate bulkheadTemplate = new BulkheadTemplate(BulkheadRegistry.ofDefaults());

// The inline list lets Java infer the constructor's List<Resilience4jTemplate> parameter
// (the same way ChainableResilience4jFacadeTest#testConstructor does).
ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        asList(bulkheadTemplate, bulkheadTemplate, bulkheadTemplate));

assert facade.getSize() == 1;   // duplicates removed by identity
```

### 7.10 门面的两阶段执行

改编自 `ChainableResilience4jFacadeTest#testBeginAndEnd`：

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;
import io.microsphere.resilience4j.common.Resilience4jContext;

ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        RetryRegistry.ofDefaults(), CircuitBreakerRegistry.ofDefaults(),
        RateLimiterRegistry.ofDefaults(), TimeLimiterRegistry.ofDefaults(),
        BulkheadRegistry.ofDefaults());

Resilience4jContext<Resilience4jContext[]> context = facade.begin("test-entry");
Resilience4jContext[] subContexts = context.getEntry();
assert subContexts.length == facade.getSize();   // 5 slots

// Only CircuitBreaker / RateLimiter / Bulkhead are begin-supported, so the Retry
// and TimeLimiter slots stay null.
for (Resilience4jContext sub : subContexts) {
    System.out.println(sub == null ? "skipped" : sub.getEntryName());
}

facade.end(context);   // symmetric forward-order end of the non-null slots
```

### 7.11 手动两阶段断路器（正确的写法）

派生自 `AbstractResilience4jTemplateTest#testEnd`，这是模块本身唯一直接驱动 `begin`/`end` 的地方。
关键在于：**你必须自己填充 `result` 或 `failure`** —— `Resilience4jContext` 不会被自动填充，而
`CircuitBreakerTemplate.doEnd` 会读取这两个字段。

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import io.microsphere.resilience4j.common.Resilience4jContext;

String entryName = "test-entry";
CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());

// doBegin() already records startTime for the circuit breaker
Resilience4jContext<CircuitBreaker> context = template.begin(entryName);
try {
    String result = invokeRemoteService();
    context.setResult(result);      // -> circuitBreaker.onResult(duration, unit, result)
} catch (Throwable failure) {
    context.setFailure(failure);    // -> circuitBreaker.onError(duration, unit, failure)
} finally {
    template.end(context);          // -> circuitBreaker.onSuccess(duration, unit) if both are null
}
```

如果你把 `begin`/`end` 用在 `BulkheadTemplate` 上，是不会记录 `startTime` 的；因此绝不要在没有先前
`begin()` 的情况下，对你自己为断路器创建的上下文调用 `end()` —— `doEnd` 会把 `context.getStartTime()`
拆箱成 `long`，从而抛出 `NullPointerException`。

### 7.12 `Resilience4jContext` 属性与线程绑定

改编自 `Resilience4jContextTest`：

```java
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import io.microsphere.resilience4j.common.Resilience4jContext;

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());
Resilience4jContext<CircuitBreaker> context = template.begin("test-entry");

context.setAttribute("tenant", "acme");
assert context.hasAttribute("tenant");
String tenant = context.getAttribute("tenant");            // <T> T getAttribute(String)
String fallback = context.getAttribute("operator", "none"); // default value variant
context.removeAttribute("tenant");
assert !context.hasAttribute("tenant");
assert context.getAttributes().isEmpty();                    // read-only view

// Thread binding (single slot - not a stack)
context.withinContext();
assert Resilience4jContext.getContext() == context;

Resilience4jContext.doInContext(ctx -> ctx.setResult("For testing"));        // keeps the binding
Resilience4jContext.doInContext(ctx -> System.out.println(ctx.getEntryName()), true); // removes it

assert Resilience4jContext.getContext() == null;
// doInContext silently does nothing when no context is bound
Resilience4jContext.doInContext(ctx -> {
    throw new IllegalStateException("never reached");
});
```

### 7.13 `Resilience4jUtils` 与 `Resilience4jModule`

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;
import io.microsphere.resilience4j.common.Resilience4jModule;
import io.microsphere.resilience4j.common.Resilience4jTemplate;
import io.microsphere.resilience4j.util.Resilience4jUtils;

import java.util.Arrays;
import java.util.List;

CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

// Factory: registry -> the module's default template, resolved from
// META-INF/default/templates.properties
CircuitBreakerTemplate template = Resilience4jUtils.createTemplate(circuitBreakerRegistry);

// Bulk factory: one template per distinct registry, priority-sorted, read-only list
List<Resilience4jTemplate> templates = Resilience4jUtils.createTemplates(Arrays.asList(
        RetryRegistry.ofDefaults(), BulkheadRegistry.ofDefaults()));
assert templates.size() == 2;

// Module metadata
Resilience4jModule module = Resilience4jModule.valueOf(CircuitBreakerRegistry.class);
assert module == Resilience4jModule.CIRCUIT_BREAKER;
assert module.getName().equals("circuit-breaker");
assert module.getDefaultAspectOrder() == 1;

// Event plumbing: registry- and entry-level EventProcessor access
// (getEventProcessor(Object) would throw UnsupportedOperationException)
Resilience4jUtils.getEventProcessor(circuitBreakerRegistry);
Resilience4jUtils.getEventProcessor(template.getEntry("test-entry"));
```

---

## 8. 行为说明

### 8.1 我到底在调用哪个重载？

| 你的代码 | 解析到的方法 | 是否被装饰？ |
|-----------|-----------------|------------|
| `execute(name, () -> {})` | `execute(String, Runnable)` | 是 |
| `execute(name, () -> value)` | `execute(String, Supplier<T>)` | 是 |
| `execute(name, entry -> {})` | `AdvancedResilience4jOperations.execute(String, Consumer<E>)` | **否**（原始入口） |
| `execute(name, entry -> entry)` | `AdvancedResilience4jOperations.execute(String, Function<E, T>)` | **否**（原始入口） |
| `call(name, () -> value)` | `call(String, ThrowableSupplier<T>)` | 是 |
| `call(name, () -> {})` | `call(String, ThrowableAction)` | 是 |
| `call(name, entry -> {})` | `call(String, ThrowableConsumer<E>)` | **否**（原始入口） |
| `call(name, entry -> value)` | `call(String, ThrowableFunction<E, T>)` | **否**（原始入口） |
| `call(name, action, IllegalStateException.class)` | `call(String, ThrowableAction, Class<TR>)` | 是，失败被重新类型化为 `TR` |

对于 `{}` 形式的 lambda 体，Java 会选择 void 兼容的重载；对于有返回值的体，则选择 value 兼容的重载，
所以两族方法能无歧义地共存。若不想遇到意外，就把 lambda 赋给一个显式声明类型的局部变量。

### 8.2 `execute` 与 `call`

- `execute` 从不声明受检异常。本身已是 `RuntimeException` 实例的失败会原样重抛；其余情况（受检异常、
  `Error`）会用 `ExceptionUtils.wrap(t, RuntimeException.class)` 包装，当目标类型可赋值时返回同一个实例，
  否则创建目标类型的新实例。
- `call` 声明 `throws Throwable`。当你想保留原始异常身份时就用它（来自 `TimeLimiterTemplate` 的
  `TimeoutException`、`BulkFullException`、`CallNotPermittedException`、你自己的受检异常）。
- `call(name, callback, SomeException.class)` 是为声明 `throws SomeException` 的方法提供的人性化变体。

### 8.3 `execute` / `call` 与 `begin` / `end`

- 一次性路径最终都会落到 `call(String, ThrowableSupplier)`。六个模板里有五个用 Resilience4j 装饰器覆写了它；
  `BulkheadTemplate` 保留了接口默认实现，因而会在 `finally` 块里执行 `begin(name)` → 回调 → `end(context)`。
- 两阶段路径只涉及*入口级*钩子。由于 `begin`/`end` 在 `Resilience4jTemplate` 里是 `final` 的，各模块的语义
  都落在 `doBegin`/`doEnd` 中：`CircuitBreaker` = 获取许可 + 计时上报；`RateLimiter` = 获取许可 + 结果上报；
  `Bulkhead` = 获取许可 + 完成；`Retry` / `TimeLimiter` / `ThreadPoolBulkhead` = 不支持。
- `begin()` **不**会捕获来自更早阶段的失败，`end()` 也不会被自动调用 —— 如果你使用两阶段调用，请把
  `end(context)` 放进 `finally`。
- 从 `begin()` 拿到的上下文除非你自行设置，否则不携带 result/failure（见 §7.11）。

### 8.4 门面的顺序与嵌套

模板按优先级**升序**排序，而 `CallbackChain` 最先调用下标 `0`，所以更小的优先级值意味着该模块是**外层**包装。
带上全部六个模块时，你得到的就是 Resilience4j 推荐顺序。`THREAD_POOL_BULKHEAD` 和 `BULKHEAD` 共享优先级 `4`；
平局由收集模板的顺序打破（排序是稳定的），所以如果你需要 `ThreadPoolBulkhead` 严格位于 `Bulkhead` 之内，
请用 `setPriority` 显式设置优先级。

另外注意，`facade.begin()/facade.end()` 会以与开启时**相同**的前向顺序展开（并非 LIFO），
而上报 `isBeginSupported() == false` 的模板会在子上下文数组里留下 `null` 槽位。

### 8.5 门面的去重规则

- `(Registry...)` / `(Collection<Registry>)` 构造器会把**相等的注册表实例**折叠起来
  （`Resilience4jUtils.createTemplates` 以 `Registry` 对象作为 map 的键），所以同一个注册表传两次只会
  得到一个模板。
- `(List<Resilience4jTemplate>)` 构造器会把**同一个模板实例**折叠起来
  （`List#contains`，即引用相等，因为 `Resilience4jTemplate` 没有覆写 `equals`）。因此，包装两个
  *不同* `BulkheadRegistry` 实例的两个 `BulkheadTemplate` 都会被保留。

### 8.6 本地入口缓存

默认启用。它对 `getEntry(name)` 做了短路，且本身是一个普通 `HashMap`，因此：启动时通过
`initLocalEntriesCache(...)` 预创建的条目可以安全地被并发读取；运行时被并发解析出来的条目则不行。
`clearLocalEntriesCache()` 由 `destroy()` 调用。从注册表移除一个入口（`removeEntry`）**不会**把它从缓存中
剔除 —— 如果你依赖最新的查找结果，就重建模板或清空缓存。

### 8.7 `Resilience4jUtils` 初始化

`Resilience4jUtils` 在类初始化期间加载 `META-INF/default/templates.properties`，并把每个
`Resilience4jModule` 映射到它的模板类
（`retry → RetryTemplate`、`circuit-breaker → CircuitBreakerTemplate`、`rate-limiter → RateLimiterTemplate`、
`time-limiter → TimeLimiterTemplate`、`bulkhead → BulkheadTemplate`、
`thread-pool-bulkhead → ThreadPoolBulkheadTemplate`）。因为这个 map 会为**全部六个**模块预先构建，
所以使用 `Resilience4jUtils`（或 `ChainableResilience4jFacade(Registry...)`）要求 classpath 上具备每一个
Resilience4j 模块构件 —— 包括 `resilience4j-timelimiter` 和 `resilience4j-framework-common` ——
即便你的应用只用到断路器。如果你想发布更小的依赖集，就直接实例化各个模板。

### 8.8 生命周期

模板只是围绕注册表的轻量包装，但它们确实会保留状态（本地缓存、事件消费者，以及 `TimeLimiterTemplate`
对一个 `ExecutorService` 的引用）。在你弃用它们时调用 `destroy()`（或 `facade.destroy()`）；
测试正是用 `@AfterEach` 来做这件事。

---

## 9. 已知限制

| 限制 | 依据 | 变通办法 |
|------------|----------|------------|
| `Retry`、`TimeLimiter` 和 `ThreadPoolBulkhead` 无法通过 `begin`/`end` 使用。 | `isBeginSupported()`/`isEndSupported()` 返回 `false`；`doBegin`/`doEnd` 抛 `UnsupportedOperationException`。 | 改用一次性 `execute`/`call` 形式，或继承模板并自己实现钩子。 |
| 默认的 `call(String, ThrowableSupplier)` 从不把 `result`/`failure` 写入上下文。 | `Resilience4jOperations.call` 的方法体。 | 目前只有 `BulkheadTemplate` 走该路径；对于两阶段断路器用法，请自行设置这些字段（§7.11）。 |
| `Resilience4jContext` 的线程绑定是单个槽位、没有嵌套栈，且未绑定时 `doInContext` 会静默地什么都不做。 | `ThreadLocal` + `Resilience4jContextTest`。 | 在嵌套作用域周围保存/恢复先前的上下文。 |
| 所有模块依赖都是 `optional`，因此没有任何传递性依赖。 | `microsphere-resilience4j-commons/pom.xml`。 | 自行声明你的 Resilience4j + `microsphere-java-core` 依赖（或使用会带上它们的 Spring 模块）。 |
| `ThreadPoolBulkheadTemplate.call` 会阻塞，且只重抛*根*因。 | `executeCallable(...).toCompletableFuture().get()` + `getRootCause`。 | 不要指望中间包装（`ExecutionException`、`CompletionException`）能存活下来。 |
| `Resilience4jModule.valueOf(Class)` 返回**第一个**匹配的常量；`BulkheadEvent` 解析到 `BULKHEAD`，而非 `THREAD_POOL_BULKHEAD`。 | `Resilience4jModule` 中的声明顺序。 | 用你实际持有的 registry/entry 类去匹配。 |

---

## 10. 后续步骤

- 内部实现、流程图与扩展点 →
  [开发者指南 — microsphere-resilience4j-commons](../../developer-guide/zh/microsphere-resilience4j-commons.md)
- Spring 注解驱动的用法 → [microsphere-resilience4j-spring](microsphere-resilience4j-spring.md)
- 返回指南索引 → [README.md](README.md) · English → [../README.md](../README.md)
