# microsphere-resilience4j-commons — 用户指南（1.x）

> 版本线：`0.1.x`（源分支 `release-1.x`） · 最新发布版本 `0.1.4` · 分支 revision `0.1.5-SNAPSHOT`
> 兼容 **Resilience4j 1.3.0 – 1.7.0**（Maven profile `resilience4j-1.3` … `resilience4j-1.7`，默认 **1.7.0**）
> 本模块**不依赖 Spring** —— 其源码没有导入任何 `org.springframework` 下的内容；它是构建在 Resilience4j 注册表（Registry）与 `microsphere-java-core` 之上的纯 Java。
> 英文原文：[microsphere-resilience4j-commons.md](../microsphere-resilience4j-commons.md)
> ← [指南索引](README.md) · [开发者指南页面](../../developer-guide/zh/microsphere-resilience4j-commons.md)

---

## 1. 模块概览

`microsphere-resilience4j-commons` 是 1.x 版本线中**框架中立**的核心。它把全部六个 Resilience4j 模块 ——
**断路器（CircuitBreaker）、限流器（RateLimiter）、重试（Retry）、舱壁隔离（Bulkhead）、线程池舱壁隔离（ThreadPoolBulkhead）、超时限制（TimeLimiter）** ——
统一到同一套编程模型之下，使应用代码永远无需去学习六套不同的装饰器（decorator）API。在这条
`release-1.x` 分支上，该模型会针对 Resilience4j **1.3.0 到 1.7.0** 进行编译和测试，并由一个可选的
兼容性垫片（compatibility shim，见第 8 节）补齐那些只有较新的 1.x 版本才提供的事件类型。

这套模型由四个活动部件组成：

| 概念 | 类型 | 含义 |
|---------|------|---------------|
| **模块（Module）** | `Resilience4jModule` | 一个枚举，描述某一种 Resilience4j 能力：它的入口类、配置类、事件类、注册表类、configuration-properties 类以及默认的切面顺序（aspect order）。 |
| **入口（Entry）** | 例如 `CircuitBreaker` | 存活在某个 Resilience4j `Registry` 内部的一个*具名*实例。 |
| **模板（Template）** | `Resilience4jTemplate` 的子类 | 恰好包装一个 `Registry`，并以该模块的装饰能力（`execute` / `call`，以及可选的 `begin` + `end`）来执行回调（callback）。 |
| **门面（Facade）** | `ChainableResilience4jFacade` | 包装*多个*模板，并按优先级顺序把它们组合在同一个回调周围。 |

你要打交道的包：

```
io.microsphere.resilience4j.common    <- 抽象层：Resilience4jOperations、AdvancedResilience4jOperations、
                                          Resilience4jTemplate、Resilience4jContext、Resilience4jModule、
                                          Resilience4jFacade、ChainableResilience4jFacade、CallbackChain、
                                          Resilience4jConstants
io.microsphere.resilience4j.bulkhead  <- BulkheadTemplate、ThreadPoolBulkheadTemplate
io.microsphere.resilience4j.circuitbreaker / .ratelimiter / .retry / .timelimiter
io.microsphere.resilience4j.util      <- Resilience4jUtils
```

相比于直接使用原生 Resilience4j 1.x，选择本模块的理由：

- 每一种能力都使用同一个统一 API（`execute` / `call`），其中包括对受检异常更友好的 `call`。
- 具名实例的衔接工作（在注册表中查找，找不到就用具名配置创建）已为你完成。
- 多模块组合（`Retry(CircuitBreaker(RateLimiter(TimeLimiter(Bulkhead(function))))))`），无需手写装饰器嵌套。
- 每次执行都有一个 `Resilience4jContext`，你可以携带它、装饰它，并把它绑定到当前线程。
- 支持注册表级与入口级的事件订阅，并提供流式（fluent）、按模块强类型的 API。

---

## 2. Maven 坐标与 BOM 用法

直接声明依赖（这是 `0.1.x` 版本线 —— 不要把它和 `0.2.x` 版本线混在同一个应用里）：

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-resilience4j-commons</artifactId>
    <version>0.1.4</version>
</dependency>
```

或者导入项目的 BOM（`microsphere-resilience4j-dependencies`），然后省略版本号：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.github.microsphere-projects</groupId>
            <artifactId>microsphere-resilience4j-dependencies</artifactId>
            <version>0.1.4</version>
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

> **本模块的每一个 compile 依赖都被声明为 `<optional>true</optional>`。** 该模块只提供抽象层；
> 你需要自己带入 Resilience4j 的相关构件（`resilience4j-core`、`resilience4j-circuitbreaker`、
> `resilience4j-ratelimiter`、`resilience4j-retry`、`resilience4j-bulkhead`、`resilience4j-timelimiter`、
> `resilience4j-framework-common`）以及 `microsphere-java-core`。这保证了该库对你实际部署哪个
> Resilience4j 1.x 版本保持中立。

Resilience4j 的版本本身由父 POM 的 Maven profile 决定，通过 `resilience4j.version` 属性解析：

| Profile | `resilience4j.version` | 是否挂载兼容垫片？ |
|---------|------------------------|---------------------------|
| `resilience4j-1.3` | `1.3.0` | 是 |
| `resilience4j-1.4` | `1.4.0` | 是 |
| `resilience4j-1.5` | `1.5.0` | 是 |
| `resilience4j-1.6` | `1.6.0` | 是 |
| `resilience4j-1.7` *（activeByDefault）* | `1.7.0` | 否（1.7.0 已自带这些事件类型） |

`compatible` 垫片会由本模块自己 `pom.xml` 中定义的 `resilience4j-1.3` … `resilience4j-1.6`
profile 自动添加到 `commons` 上；你自己不需要声明它。见第 8 节与
[compatible 用户指南](microsphere-resilience4j-compatible.md)。

---

## 3. 兼容性表

| microsphere-resilience4j | Resilience4j | Spring Cloud 技术栈（由父工程设定，不由本模块设定） | JDK |
|--------------------------|--------------|-------------------------------------------------------------|-----|
| `0.1.x`（`release-1.x`，最新 `0.1.4`） | 1.3.0 – 1.7.0（默认 1.7.0） | Hoxton – 2021.0.x | 8+ |

commons 模块本身并不引用 Spring 或 Spring Cloud；上表的 Spring Cloud 范围是与之配套的 1.x reactor
所构建于的技术栈，由 `microsphere-spring-cloud-parent` `0.1.26` 提供。

---

## 4. API 参考（公开类型）

下面每个签名都取自 `release-1.x` 源码。与 `0.2.x` 版本线上同名的类型，其行为可能不同，
因为它们委托给的是 Resilience4j **1.x** 的 API（见第 8 节）。

### 4.1 `Resilience4jOperations<E>`（接口）

在单个模块的装饰下运行一个回调的最小契约。`E` 是入口类型（例如 `CircuitBreaker`）。
除 `begin` / `end` 之外，所有方法都是 `default` 实现。

| 方法 | 行为 |
|--------|-----------|
| `void execute(String name, Runnable callback)` | 运行一个返回 void 的回调；把它包装成 supplier 形态。 |
| `<T> T execute(String name, Supplier<T> callback)` | 运行一个返回值的回调；委托给 `call(name, callback::get)`，并把包装成 `RuntimeException` 的任何 `Throwable` 重新抛出。 |
| `void call(String name, ThrowableAction callback) throws Throwable` | 可能抛出的 void 回调；把它包装成返回 `null` 的 supplier 形态。 |
| `<TR extends Throwable> void call(String name, ThrowableAction callback, Class<TR> throwableClass) throws TR` | 同上，但把失败强制转换为 `throwableClass` 后重新抛出。 |
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | 核心方法：`begin(name)` → 运行回调 → 在 `finally` 中 `end(context)`。具体模板可以覆盖此方法。 |
| `<T, TR extends Throwable> T call(String name, ThrowableSupplier<T> callback, Class<TR> throwableClass) throws TR` | 同上，把抛出的错误强制转换为 `throwableClass`。 |
| `boolean isBeginSupported()` | 默认为 `true`。 |
| `Resilience4jContext<E> begin(String name)` | 抽象方法 —— 两阶段（two-phase）生命周期的第一阶段。 |
| `boolean isEndSupported()` | 默认为 `true`。 |
| `void end(Resilience4jContext<E> context)` | 抽象方法 —— 两阶段生命周期的第二阶段。 |

两种执行风格：

- **一次性（one-time）**：`execute` / `call` —— 模板装饰整个回调并返回其结果。
- **两阶段（two-phase）**：`begin`（获取许可 / 记录开始）→ 执行业务 → `end`（记录成功/失败 / 释放）。

### 4.2 `AdvancedResilience4jOperations<E, C, R extends Registry<E, C>>`（接口）

继承 `Resilience4jOperations<E>` 和 `io.microsphere.lang.Prioritized`。新增了 getter、配置/入口管理，
以及作用于原始入口的 `Consumer`/`Function` 操作。`E` = 入口，`C` = 配置，`R` = 注册表。

| 分组 | 方法 |
|-------|---------|
| Getter | `R getRegistry()`、`Resilience4jModule getModule()`、`default Class<E> getEntryClass()`、`default Class<C> getConfigClass()`、`Logger getLogger()` |
| 配置 | `default C getConfiguration(String configName)`（会回退到默认配置）、`default C getDefaultConfig()`、`default AdvancedResilience4jOperations<E,C,R> addConfiguration(String configName, C configuration)` |
| 入口 | `default E getEntry(String name)`（在注册表中查找，否则 `createEntry`）、`E createEntry(String name)`、`default E removeEntry(String name)`、`default E replaceEntry(String name, E newEntry)` |
| 原始入口，一次性 | `default AdvancedResilience4jOperations<E,C,R> execute(String name, Consumer<E> entryConsumer)`、`default <T> T execute(String name, Function<E,T> entryFunction)` |
| 原始入口，可抛异常 | `default AdvancedResilience4jOperations<E,C,R> call(String name, ThrowableConsumer<E> entryConsumer) throws Throwable`、`default <TR extends Throwable> AdvancedResilience4jOperations<E,C,R> call(String name, ThrowableConsumer<E> entryConsumer, Class<TR> throwableClass) throws TR`、`default <T> T call(String name, ThrowableFunction<E,T> entryFunction) throws Throwable`、`default <T, TR extends Throwable> T call(String name, ThrowableFunction<E,T> entryFunction, Class<TR> throwableClass) throws TR` |

`Consumer`/`Function` 这些重载直接作用于解析出来的入口本身（而不是作用于回调）—— 例如
`template.execute(entryName, CircuitBreaker::reset)` 或 `template.execute(entryName, e -> e)` ——
它们很适合在不运行被装饰的业务调用的前提下去读取或修改某个入口。

### 4.3 `Resilience4jTemplate<E, C, R extends Registry<E, C>>`（抽象类）

`AdvancedResilience4jOperations` 的基础实现。它持有一个 `Registry`，从注册表类反推出所属模块，
并且（默认情况下）为维护一个本地的、非线程安全的 `HashMap` 入口缓存以保证性能。

构造器：

| 构造器 | 说明 |
|-------------|-------|
| `Resilience4jTemplate(R registry)` | 等价于 `(registry, true)` —— 缓存本地入口。 |
| `Resilience4jTemplate(R registry, boolean localEntriesCached)` | `assertNotNull(registry)`；通过 `Resilience4jModule.valueOf(registry.getClass())` 解析 `module`，取得注册表的 `EventProcessor`，并用 `module.getDefaultAspectOrder()` 来初始化 `priority`。 |

关键方法：

| 方法 | 可见性 | 说明 |
|--------|-----------|-------|
| `final R getRegistry()` / `final Resilience4jModule getModule()` / `final C getDefaultConfig()` / `final Class<E> getEntryClass()` / `final Class<C> getConfigClass()` / `final Logger getLogger()` | public final | 委托给接口的 `default` 实现，因此子类无法改变它们。 |
| `final E getEntry(String name)` | public final | 先读本地缓存，未命中再落到注册表的查找/创建。 |
| `final Resilience4jTemplate<E,C,R> initLocalEntriesCache(String name)` / `...(Iterable<String> names)` | public final | 预填充本地缓存（这是让缓存变得实际线程安全的唯一途径）。 |
| `protected Map<String,E> createLocalEntriesCache()` | protected | 返回一个新的 `HashMap`；返回 `null` 表示"不缓存"。 |
| `protected final boolean isLocalEntriesCached()` / `clearLocalEntriesCache()` / `getEntryFromCache(String)` | protected final | 缓存辅助方法。 |
| `final Resilience4jContext<E> begin(String name)` | public final | `getEntry(name)` → `new Resilience4jContext(name, entry)` → `doBegin(context)`。 |
| `protected void doBegin(Resilience4jContext<E> context)` | protected | 钩子方法；默认抛出 `UnsupportedOperationException`。 |
| `final void end(Resilience4jContext<E> context)` | public final | 调用 `doEnd(context)`。 |
| `protected void doEnd(Resilience4jContext<E> context)` | protected | 钩子方法；默认抛出 `UnsupportedOperationException`。 |
| `final Resilience4jTemplate<E,C,R> onEntryAddedEvent(EventConsumer<EntryAddedEvent<E>>)` / `onEntryRemovedEvent(...)` / `onEntryReplacedEvent(...)` | public final | 注册表级的事件订阅。 |
| `protected final <T extends RegistryEvent> Resilience4jTemplate<E,C,R> registerRegistryEventConsumer(Class<? super T> eventType, EventConsumer<T> consumer)` | protected final | |
| `protected final <T> Resilience4jTemplate<E,C,R> registerEntryEventConsumer(String entryName, Class<? super T> eventType, EventConsumer<T> consumer)` | protected final | 入口级的事件订阅（每个具体模板都会使用它）。 |
| `final int getPriority()` / `final Resilience4jTemplate<E,C,R> setPriority(int priority)` | public final | `Prioritized` 支持；决定门面的排序。 |
| `void destroy()` | public | 清空本地缓存。 |

请注意 `begin`/`end`/`getEntry`/`getRegistry` 这些方法是 `final` 的：子类只能通过 `doBegin`/`doEnd`
钩子、覆盖 `call(String, ThrowableSupplier)`、`isBeginSupported()` / `isEndSupported()`、
`createEntry(String)` 或 `createLocalEntriesCache()` 来自定义行为。

### 4.4 `Resilience4jContext<E>`（类）

可变的"每次执行"状态。构造器是 `protected` 的（由模板以及同包内的协作者创建）。

| 成员 | 描述 |
|--------|-------------|
| `Resilience4jContext(String entryName, E entry)` | `protected`；两个参数都会断言非 null。 |
| `String getEntryName()` / `E getEntry()` | 解析出的入口及其名称。 |
| `setStartTime(Long)` / `getStartTime()` | 执行开始的时间戳（未设置前为 `null`）。 |
| `setResult(Object)` / `getResult()` | 执行结果。 |
| `setFailure(Throwable)` / `getFailure()` | 执行失败信息。 |
| `setAttribute(String, Object)` / `hasAttribute(String)` / `getAttribute(String)` / `getAttribute(String, T)` / `removeAttribute(String)` / `removeAttributes()` / `getAttributes()` | 属性包；`getAttributes()` 返回一个不可修改的视图（没有属性时返回空 map）。 |
| `withinContext()` | 把该上下文绑定到当前线程并返回它自己。 |
| `static Resilience4jContext getContext()` | 当前线程绑定的上下文，或 `null`。 |
| `static void setContext(Resilience4jContext)` | 在线程上绑定（传 `null` 则清除）。 |
| `static Resilience4jContext removeContext()` | 移除并返回线程绑定的上下文。 |
| `static void doInContext(Consumer<Resilience4jContext>)` / `doInContext(Consumer<Resilience4jContext>, boolean forRemoval)` | 若存在绑定的上下文则运行该 consumer；`forRemoval=true` 会先移除它。 |

这些 setter（`setStartTime`、`setResult`、`setFailure`、`setAttribute`、`removeAttributes`、`withinContext`）
都是流式的，返回 `this`。线程绑定有两种写法，承担不同角色：**实例**方法
`withinContext()` 把当前上下文绑定到线程；**静态**方法 `setContext(Resilience4jContext)`
绑定任意一个上下文（传 `null` 则清除）。发布说明记录了一次早先把实例辅助方法重命名为
`withinContext()` 的改动；静态的 `setContext` 作为一个独立的 API 保留了下来。

### 4.5 `Resilience4jFacade`（接口）

一个标记（marker）接口：`Resilience4jFacade extends Resilience4jOperations<Resilience4jContext[]>`。
它的"入口"类型是**子上下文数组**，每个被包装的模板对应一个元素。

### 4.6 `ChainableResilience4jFacade`（类）

默认的 `Resilience4jFacade`。它为每个传入的 `Registry` 构建一个 `Resilience4jTemplate`
（通过 `Resilience4jUtils.createTemplates`），对模板去重，并按优先级排序（`Prioritized.COMPARATOR`）。

构造器：

| 构造器 | 说明 |
|-------------|-------|
| `ChainableResilience4jFacade(Registry... registries)` | 可变参数便捷形式；包装成 `Collection` 形式。 |
| `ChainableResilience4jFacade(Collection<Registry> registries)` | 创建并排序模板。 |
| `ChainableResilience4jFacade(List<Resilience4jTemplate> templates)` | 对给定模板去重并排序。 |

方法：

| 方法 | 说明 |
|--------|-------|
| `<T> T call(String name, ThrowableSupplier<T> callback) throws Throwable` | 把回调包进一个 `CallbackChain`，然后运行整条模板链（见下文）。 |
| `Resilience4jContext<Resilience4jContext[]> begin(String name)` | 分配一个由子上下文组成的 `Resilience4jContext[]`，并调用每个支持 `begin` 的模板的 `begin`。 |
| `void end(Resilience4jContext<Resilience4jContext[]> context)` | 用各自的子上下文调用每个支持 `end` 的模板的 `end`。 |
| `List<Resilience4jTemplate> getTemplates()` / `int getSize()` / `void destroy()` | 访问器与生命周期方法。 |

### 4.7 `CallbackChain<T>`（package-private 类）

不属于公开 API（声明时没有修饰符，即在 `io.microsphere.resilience4j.common` 包中为 package-private）。
它是一个 `ThrowableSupplier<T>`，遍历模板列表，把每个模板的 `call(name, …)` 嵌套在剩余链条的外层，
最后运行用户的回调：

```java
public T get() throws Throwable {
    if (pos < size) {
        Resilience4jTemplate template = templates.get(pos++);
        return (T) template.call(this.entryName, CallbackChain.this::get);
    }
    return delegate.get();
}
```

### 4.8 `Resilience4jModule`（枚举）

六种能力的元数据表。枚举常量（名称 / 入口 / 配置 / config-properties / 事件 / 注册表 / 默认切面顺序）：

| 常量 | 模块名 | 入口 | 事件类 | 注册表 | 顺序 |
|----------|-------------|-------|-------------|----------|-------|
| `RETRY` | `retry` | `Retry` | `RetryEvent` | `RetryRegistry` | 0 |
| `CIRCUIT_BREAKER` | `circuit-breaker` | `CircuitBreaker` | `CircuitBreakerEvent` | `CircuitBreakerRegistry` | 1 |
| `RATE_LIMITER` | `rate-limiter` | `RateLimiter` | `RateLimiterEvent` | `RateLimiterRegistry` | 2 |
| `TIME_LIMITER` | `time-limiter` | `TimeLimiter` | `TimeLimiterEvent` | `TimeLimiterRegistry` | 3 |
| `BULKHEAD` | `bulkhead` | `Bulkhead` | `BulkheadEvent` | `BulkheadRegistry` | 4 |
| `THREAD_POOL_BULKHEAD` | `thread-pool-bulkhead` | `ThreadPoolBulkhead` | `BulkheadEvent` | `ThreadPoolBulkheadRegistry` | 4 |

该顺序复刻了 Resilience4j 官方文档中的切面顺序（`Retry > CircuitBreaker > RateLimiter > TimeLimiter >
Bulkhead`）；`ThreadPoolBulkhead` 与 `Bulkhead` 共享顺序 `4`。

方法：`getName()`、`getEntryClass()`、`getConfigClass()`、`getConfigurationPropertiesClass()`、
`getEventClass()`、`getRegistryClass()`、`getDefaultAspectOrder()`、`toString()`，以及静态查找方法
`valueOf(Class<?> type)`：它会把传入的类与每个模块的五个已知类逐一匹配，若都不匹配则抛出
`IllegalArgumentException`。

### 4.9 `Resilience4jConstants`（接口）

用于配置前缀的字符串常量。`PREFIX = "microsphere.resilience4j."`，以及每个模块的
`<MODULE>_MODULE_NAME` / `<MODULE>_PREFIX`，例如 `RETRY_MODULE_NAME = "retry"`、`RETRY_PREFIX =
"microsphere.resilience4j.retry."`。覆盖 `retry`、`circuit-breaker`、`rate-limiter`、`time-limiter`、
`bulkhead`、`thread-pool-bulkhead`。

### 4.10 `Resilience4jUtils`（抽象类，实现 `Utils`）

反射/注册表辅助工具。公开的静态方法：

| 方法 | 说明 |
|--------|-------|
| `static <E, C> E getEntry(Registry<E, C> registry, String name)` | 使用 `registry.getDefaultConfig()`。 |
| `static <E, C> E getEntry(Registry<E, C> registry, String name, C configuration)` | 先解析模块，然后通过反射调用注册表的 `entryName(name, config)` 方法。 |
| `static <E> EventProcessor<E> getEventProcessor(E entry)` | 依据入口类型分派（`CircuitBreaker`/`Bulkhead`/`ThreadPoolBulkhead`/`RateLimiter`/`TimeLimiter`/`Retry`）；其他类型抛 `UnsupportedOperationException`。 |
| `static <E> EventProcessor<E> getEventProcessor(Retry/TimeLimiter/RateLimiter/Bulkhead/ThreadPoolBulkhead/CircuitBreaker entry)` | 按类型分别提供的形式，把 `entry.getEventPublisher()` 作为 `EventProcessor` 返回。 |
| `static <T> EventProcessor<T> getEventProcessor(Registry registry)` | 注册表级的事件发布者。 |
| `static <E, C, R extends Registry<E, C>, T extends Resilience4jTemplate<E, C, R>> T createTemplate(R registry)` | 实例化该注册表所属模块的默认模板类（取自 `META-INF/default/templates.properties`）。 |
| `static List<Resilience4jTemplate> createTemplates(Collection<Registry> registries)` | 构建、去重并按优先级排序模板；返回不可修改的列表（输入为空时返回空列表）。 |

模块到模板类的映射来自随包资源
`META-INF/default/templates.properties`：

```properties
bulkhead = io.microsphere.resilience4j.bulkhead.BulkheadTemplate
thread-pool-bulkhead = io.microsphere.resilience4j.bulkhead.ThreadPoolBulkheadTemplate
circuit-breaker = io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate
rate-limiter = io.microsphere.resilience4j.ratelimiter.RateLimiterTemplate
retry = io.microsphere.resilience4j.retry.RetryTemplate
time-limiter = io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate
```

### 4.11 六个具体模板

这六个模板各自位于自己的子包中，并且都接受 `(Registry)` 或 `(Registry, boolean localEntriesCached)`。
`TimeLimiterTemplate` 额外接受一个 `ExecutorService`。

| 模板 | `call` 的实现（1.x Resilience4j API） | 两阶段（`begin`/`end`） | 入口事件注册方法 |
|----------|---------------------------------------------|---------------------------|----------------------------------|
| `CircuitBreakerTemplate` | `circuitBreaker.decorateCheckedSupplier(callback::get).apply()` | 支持 —— `doBegin` 调用 `acquirePermission()` 并记录 `nanoTime()`；`doEnd` 调用 `onSuccess(duration, NANOSECONDS)` 或 `onError(duration, NANOSECONDS, failure)` | `onCallNotPermittedEvent`、`onErrorEvent`、`onFailureRateExceededEvent`、`onIgnoredErrorEvent`、`onResetEvent`、`onSlowCallRateExceededEvent`、`onStateTransitionEvent`、`onSuccessEvent` |
| `RateLimiterTemplate` | `RateLimiter.decorateCheckedSupplier(rateLimiter, callback::get).apply()` | 支持 —— `doBegin` **和** `doEnd` 都调用 `RateLimiter.waitForPermission(rateLimiter)` | `onSuccessEvent`、`onFailureEvent`、`onDrainedEvent` |
| `RetryTemplate` | `Retry.decorateCheckedSupplier(retry, callback::get).apply()` | **不支持**（`isBeginSupported()`/`isEndSupported()` → `false`） | `onErrorEvent`、`onIgnoredErrorEvent`、`onRetryEvent`、`onSuccessEvent` |
| `BulkheadTemplate` | *不覆盖 `call`* → 走 `begin`/`end` | 支持 —— `doBegin` = `acquirePermission()`，`doEnd` = `onComplete()` | `onCallPermittedEvent`、`onCallRejectedEvent`、`onCallFinishedEvent` |
| `ThreadPoolBulkheadTemplate` | `threadPoolBulkhead.executeCallable(...).toCompletableFuture().get()`，失败时剥离根因（root cause） | **不支持** | `onCallPermittedEvent`、`onCallRejectedEvent`、`onCallFinishedEvent` |
| `TimeLimiterTemplate` | `timeLimiter.decorateFutureSupplier(() -> executorService.submit(...)).call()` | **不支持** | `onSuccessEvent`、`onErrorEvent`、`onTimeoutEvent` |

构造器（在本分支上已核验）：

- `CircuitBreakerTemplate`、`RateLimiterTemplate`、`RetryTemplate`、`BulkheadTemplate`、`ThreadPoolBulkheadTemplate`：`(Registry)` 与 `(Registry, boolean localEntriesCached)`。
- `TimeLimiterTemplate` 有**四个**构造器：`(TimeLimiterRegistry)`、`(TimeLimiterRegistry, boolean)`、
  `(TimeLimiterRegistry, ExecutorService)` 和 `(TimeLimiterRegistry, ExecutorService, boolean)`。未提供
  executor 时，默认使用 `ForkJoinPool.commonPool()`。

这些事件注册方法都遵循同一种流式模式：`(String entryName, EventConsumer<SomeEvent> consumer)`，
返回 `this`。其中 `…OnFailureRateExceededEvent`、`…OnSlowCallRateExceededEvent` 和 `…OnDrainedEvent`
这几个类型，是你针对 Resilience4j 1.3–1.6 构建时由 `compatible` 垫片提供的。

---

## 5. 代码示例

以下示例改编自 `release-1.x` 的测试套件
（`src/test/java/io/microsphere/resilience4j/...`）。

### 5.1 单个模板 —— 断路器（`CircuitBreakerTemplateTest`）

```java
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type;
import io.microsphere.resilience4j.circuitbreaker.CircuitBreakerTemplate;

import java.time.Duration;

import static java.time.Duration.ofMillis;

CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(10)
        .ignoreExceptions(RuntimeException.class)
        .recordExceptions(Exception.class)
        .slowCallDurationThreshold(ofMillis(50))
        .permittedNumberOfCallsInHalfOpenState(2)
        .slowCallRateThreshold(10)
        .automaticTransitionFromOpenToHalfOpenEnabled(true)
        .build();

CircuitBreakerTemplate template = new CircuitBreakerTemplate(CircuitBreakerRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

template.onSuccessEvent("test-entry", event -> {
    // event.getCircuitBreakerName()、event.getEventType() == Type.SUCCESS
});

String result = template.execute("test-entry", () -> "test-entry");

// 直接读取/修改解析出来的入口（AdvancedResilience4jOperations）：
template.execute("test-entry", io.github.resilience4j.circuitbreaker.CircuitBreaker::reset);

template.destroy();
```

`call` 这条路径同样可以抛出受检异常，并被强制转换成声明的类型：

```java
template.call("test-entry", () -> {
    throw new java.util.concurrent.TimeoutException("boom");
}, java.util.concurrent.TimeoutException.class);   // 以 TimeoutException 的形式重新抛出
```

### 5.2 舱壁隔离 —— 一次性执行会经过 begin/end（`BulkheadTemplateTest`）

因为 `BulkheadTemplate` 没有覆盖 `call`，所以 `execute`/`call` 会在回调前后获取并释放舱壁的许可：

```java
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;

import static java.time.Duration.ofMillis;

BulkheadConfig config = BulkheadConfig.custom()
        .maxConcurrentCalls(2)
        .maxWaitDuration(ofMillis(100))
        .build();

BulkheadTemplate template = new BulkheadTemplate(BulkheadRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

template.onCallPermittedEvent("test-entry", event -> { /* Type.CALL_PERMITTED */ });
template.onCallFinishedEvent("test-entry", event -> { /* Type.CALL_FINISHED */ });

Object result = template.execute("test-entry", () -> null); // 结果为 null
```

### 5.3 限流器（`RateLimiterTemplateTest`）

```java
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.microsphere.resilience4j.ratelimiter.RateLimiterTemplate;

import static java.time.Duration.ofMillis;

RateLimiterConfig config = RateLimiterConfig.custom()
        .limitForPeriod(1)
        .limitRefreshPeriod(ofMillis(50))
        .timeoutDuration(ofMillis(50))
        .build();

RateLimiterTemplate template = new RateLimiterTemplate(RateLimiterRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

template.onSuccessEvent("test-entry", event -> { /* RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE */ });
template.onFailureEvent("test-entry", event -> { /* ... */ });
template.onDrainedEvent("test-entry", event -> { /* RateLimiterEvent.Type.DRAINED */ });

for (int i = 0; i < 5; i++) {
    template.execute("test-entry", () -> "For testing");
}
```

### 5.4 重试（`RetryTemplateTest`）

`RetryTemplate` 不支持两阶段，因此请使用 `execute`/`call`；重试状态通过事件来观测：

```java
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.retry.RetryTemplate;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

RetryConfig config = RetryConfig.custom()
        .maxAttempts(3)
        .ignoreExceptions(IllegalStateException.class)
        .retryExceptions(TimeoutException.class)
        .build();

RetryTemplate template = new RetryTemplate(RetryRegistry.ofDefaults());
template.addConfiguration("test-entry", config);

AtomicInteger attempts = new AtomicInteger(3);
template.onRetryEvent("test-entry", event -> { /* Type.RETRY、event.getNumberOfRetryAttempts() */ });
template.onSuccessEvent("test-entry", event -> { /* Type.SUCCESS */ });

String result = template.call("test-entry", () -> {
    if (attempts.decrementAndGet() == 0) {
        return "OK";
    }
    throw new TimeoutException("For testing");
});
// 经过若干次重试后 result == "OK"
```

### 5.5 使用显式 executor 的超时限制（`TimeLimiterTemplateTest`）

```java
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.timelimiter.TimeLimiterTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import static java.time.Duration.ofMillis;
import static java.util.concurrent.Executors.newFixedThreadPool;

ExecutorService executorService = newFixedThreadPool(1);

TimeLimiterConfig config = TimeLimiterConfig.custom()
        .cancelRunningFuture(true)
        .timeoutDuration(ofMillis(50))
        .build();

TimeLimiterTemplate template = new TimeLimiterTemplate(TimeLimiterRegistry.ofDefaults(), executorService);
template.addConfiguration("test-entry", config);

template.onSuccessEvent("test-entry", event -> { /* TimeLimiterEvent.Type.SUCCESS */ });
template.onTimeoutEvent("test-entry", event -> { /* TimeLimiterEvent.Type.TIMEOUT */ });

template.execute("test-entry", () -> { /* 快速调用会成功 */ });

executorService.shutdown();
```

如果某个回调的运行时长超过 `timeoutDuration`，`call` 会抛出 `TimeoutException`。

### 5.6 线程池舱壁隔离（`ThreadPoolBulkheadTemplateTest`）

```java
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.microsphere.resilience4j.bulkhead.ThreadPoolBulkheadTemplate;

ThreadPoolBulkheadTemplate template = new ThreadPoolBulkheadTemplate(ThreadPoolBulkheadRegistry.ofDefaults());

template.onCallPermittedEvent("test-entry", event -> { /* event.getBulkheadName() */ });
template.onCallFinishedEvent("test-entry", event -> { /* ... */ });

String result = template.execute("test-entry", () -> "test-entry");
// callable 在线程池上运行；失败时会重新抛出其根因
```

### 5.7 可链式调用的门面（`ChainableResilience4jFacadeTest2`）

传入多个注册表会为每个模块构建一个模板，并去重、按优先级排序：

```java
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.microsphere.resilience4j.common.ChainableResilience4jFacade;

ChainableResilience4jFacade facade = new ChainableResilience4jFacade(
        BulkheadRegistry.ofDefaults(),
        BulkheadRegistry.ofDefaults(),     // 重复 -> 被合并
        RateLimiterRegistry.ofDefaults(),
        RateLimiterRegistry.ofDefaults(),   // 重复 -> 被合并
        TimeLimiterRegistry.ofDefaults(),
        TimeLimiterRegistry.ofDefaults(),   // 重复 -> 被合并
        RetryRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults(),
        CircuitBreakerRegistry.ofDefaults() // 重复 -> 被合并
);

facade.getSize();        // 5 个生效的模板（bulkhead、rate-limiter、time-limiter、retry、circuit-breaker）
facade.execute("test-entry", () -> { /* 整条链 */ });
facade.call("test-entry", () -> { /* 整条链 */ });

facade.destroy();
```

两阶段形式会为每个支持的模板构建一个子上下文：

```java
import io.microsphere.resilience4j.common.Resilience4jContext;

Resilience4jContext<Resilience4jContext[]> context = facade.begin("test-entry");
Resilience4jContext[] subContexts = context.getEntry();   // 长度 == facade.getSize()
facade.end(context);
```

### 5.8 上下文辅助方法（`Resilience4jContextTest`）

```java
import io.microsphere.resilience4j.common.Resilience4jContext;

Resilience4jContext<String> context = newContext();   // 通过模板创建，例如 template.begin(name)
context.withinContext();                              // 绑定到当前线程
Resilience4jContext.getContext();                     // -> 已绑定的上下文

Resilience4jContext.doInContext(ctx -> {
    ctx.setAttribute("key", "value");
    ctx.getAttribute("key");                          // "value"
    ctx.getAttribute("missing", "default");           // "default"
});

Resilience4jContext.doInContext(ctx -> { /* ... */ }, true); // forRemoval == true，执行后清除
Resilience4jContext.getContext();                     // -> null
```

> `Resilience4jContext` 的构造器是 `protected` 的；应用代码应当通过
> `template.begin(name)` 或 `facade.begin(name)` 获取上下文，而不是直接构造它们。

### 5.9 工具类（`Resilience4jUtilsTest`）

```java
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.microsphere.resilience4j.bulkhead.BulkheadTemplate;
import io.microsphere.resilience4j.retry.RetryTemplate;
import io.microsphere.resilience4j.util.Resilience4jUtils;

import static java.util.Arrays.asList;

Bulkhead bulkhead = Resilience4jUtils.getEntry(BulkheadRegistry.ofDefaults(), "test");
BulkheadTemplate bt = Resilience4jUtils.createTemplate(BulkheadRegistry.ofDefaults()); // -> BulkheadTemplate
RetryTemplate rt  = (RetryTemplate) Resilience4jUtils.createTemplate(RetryRegistry.ofDefaults());

Resilience4jUtils.createTemplates(asList(RetryRegistry.ofDefaults(), BulkheadRegistry.ofDefaults())).size(); // 2
```

`createTemplate` 会为每个模块挑选正确的子类，`createTemplates` 返回一个去重、按优先级排序的
不可修改列表 —— 这正是 `ChainableResilience4jFacade` 所依赖的机制。

---

## 6. 两阶段 vs 一次性：每个模板各自做什么

| 模板 | `execute`/`call` 路径 | 显式 `begin`/`end` | `isBeginSupported()` / `isEndSupported()` |
|----------|-----------------------|------------------------|--------------------------------------------|
| CircuitBreaker | `decorateCheckedSupplier(...).apply()` | 支持 | `true` / `true` |
| RateLimiter | `decorateCheckedSupplier(...).apply()` | 支持（两个阶段都调用 `waitForPermission`） | `true` / `true` |
| Bulkhead | 基类 `call` → `begin` + `end` | 支持 | `true` / `true` |
| Retry | `decorateCheckedSupplier(...).apply()` | 不支持 | `false` / `false` |
| ThreadPoolBulkhead | `executeCallable(...).toCompletableFuture().get()` | 不支持 | `false` / `false` |
| TimeLimiter | `decorateFutureSupplier(...).call()` | 不支持 | `false` / `false` |

---

## 7. 日志

模板使用 `io.microsphere.logging.Logger`（对日志后端的一层薄抽象），并为创建、缓存命中/未命中、
事件注册以及 `begin`/`end` 状态转换输出 `trace` 级别消息。编译期不会强制你使用任何日志框架 ——
`logback-classic` 只是本模块的一个 test 作用域依赖。

---

## 8. 与 0.2.x 版本线的差异

本页记录的是 `release-1.x`（`0.1.x`）版本线。API 形态（`execute` / `call` / `begin` / `end`、
六个模板、`ChainableResilience4jFacade`）是刻意与 `0.2.x` 版本线保持一致的，但内部实现面向的是
**Resilience4j 1.x**：

- **兼容垫片。** 额外的 Maven 模块 `microsphere-resilience4j-compatible` 回移（back-port）了 Resilience4j
  的事件类（`CircuitBreakerOnFailureRateExceededEvent`、`CircuitBreakerOnSlowCallRateExceededEvent`、
  `CircuitBreakerEvent`、`RateLimiterEvent`、`RateLimiterOnDrainedEvent`），并把它们放在原有的
  `io.github.resilience4j.*` 包名下，从而使 commons 代码能够针对 1.3–1.6 编译。它只在
  `resilience4j-1.3` … `resilience4j-1.6` profile 下被自动挂到 commons 上；默认的 1.7 构建以及
  0.2.x 版本线都不需要它。见 [compatible 用户指南](microsphere-resilience4j-compatible.md)。
- **Resilience4j 1.x 的装饰器 API。** 这些模板调用的是 1.x 的 `decorateCheckedSupplier(...).apply()`
  （`CheckedFunction0` 风格、会抛异常的 `apply()`）、`TimeLimiter.decorateFutureSupplier(...).call()`
  以及 `ThreadPoolBulkhead.executeCallable(...)`。在 Resilience4j 2.x（0.2.x 版本线）上，这些装饰器工厂
  与函数式接口返回的是不含 vavr 依赖的 2.x 类型，方法名也不同，因此即便入口点看起来一样，方法体是不同的。
- **事件类型名称。** 1.x 的事件枚举（例如 `CircuitBreakerEvent.Type.SUCCESS`、
  `RateLimiterEvent.Type.SUCCESSFUL_ACQUIRE`、`BulkheadEvent.Type.CALL_PERMITTED`）被直接使用；
  其中一些在 2.x 的事件模型中被重命名或重新设计了。
- **版本范围与父工程。** 本版本线面向 Resilience4j `1.3.0 – 1.7.0` 和
  `microsphere-spring-cloud-parent` `0.1.26`（Spring Cloud Hoxton – 2021.0.x），基于 JDK 8 构建。
  0.2.x 版本线面向 Resilience4j 2.x，并在 JDK 17 上使用更新的 Spring 技术栈。

当前版本线请见 [../../../current/user-guide/zh/microsphere-resilience4j-commons.md](../../../current/user-guide/zh/microsphere-resilience4j-commons.md)。

---

所有页面的中文译文位于 `zh/` 目录下，文件名与英文版本完全一致。
