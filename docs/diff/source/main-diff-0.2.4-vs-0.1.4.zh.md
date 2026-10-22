# 源码差异对比：0.2.4 vs 0.1.4（`src/main`）

按 **artifactId / 目录名同名匹配** 的规则，逐个 Maven 模块对比两个最新 tag 的 `src/main` 内容：

| Tag | Maven 版本（`revision`） | 支持的 Resilience4j 范围 | Java | 命名空间 |
|---|---|---|---|---|
| `0.2.4` | `0.2.4-SNAPSHOT` | 2.0.0 / 2.1.0 / 2.2.0 / 2.3.0 / 2.4.0（Maven profile） | 17 | `jakarta.*` |
| `0.1.4` | `0.1.4-SNAPSHOT` | 1.3.0 / 1.4.0 / 1.5.0 / 1.6.0 / 1.7.0（Maven profile） | 8 | `javax.*` |

按需求排除：Java 8 ↔ Java 17 的语法与 API 差异、Java EE ↔ Jakarta EE 命名空间差异、以及只在一侧存在的模块（忽略，不计入新增/删除）。

## 模块总览

| 模块（`src/main`） | 0.1.4 文件数 | 0.2.4 文件数 | 可匹配 | 完全一致 | 有差异 | 仅 0.1.4 | 仅 0.2.4 |
|---|---|---|---|---|---|---|---|
| `microsphere-resilience4j-commons` | 17 | 17 | 17 | 12 | 5 | 0 | 0 |
| `microsphere-resilience4j-spring` | 31 | 30 | 30 | 27 | 3 | 1 | 0 |
| `microsphere-resilience4j-plugins/alibaba-druid` | 3 | 3 | 3 | 3 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/hibernate-core` | 1 | 1 | 1 | 1 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/mybatis` | 4 | 4 | 4 | 4 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/openfeign` | 4 | 4 | 4 | 4 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/spring-web` | 3 | 3 | 3 | 3 | 0 | 0 | 0 |
| `-parent` / `-dependencies` / `-spring-boot` / `-spring-cloud` | 0 | 0 | 0 | — | — | — | — |
| **可匹配合计** | **63** | **62** | **62** | **54** | **8** | **1** | **0** |
| `microsphere-resilience4j-compatible` | 6 | — | *忽略* | — | — | — | — |

- `-parent`、`-dependencies`、`-spring-boot`、`-spring-cloud` 是纯 POM 模块（parent / BOM / 聚合），没有 `src/main` 内容。
- `microsphere-resilience4j-compatible` 在 0.2.4 中无同名模块，按规则**忽略**。背景说明：该模块只是为了把 Resilience4j 2.x 的事件类型（`CircuitBreakerEvent`、`CircuitBreakerOnFailureRateExceededEvent`、`CircuitBreakerOnSlowCallRateExceededEvent`、`RateLimiterEvent`、`RateLimiterOnDrainedEvent`）回填到 1.x 运行时；2.x 中这些类型原生存在，兼容层因此不再需要。

## 两条发布线差异的根因

`microsphere-resilience4j-parent` 为每个受支持的 Resilience4j 版本声明了一个 Maven profile，因此 `commons` / `spring` 的源码必须能以本发布线**最低**版本编译（`0.1.x` 为 1.3.0，`0.2.x` 为 2.0.0）。已核对官方 jar 的关键 API：

| API | 1.4.0 | 1.6.1 | 1.7.0 | 2.0.2 | 影响 |
|---|---|---|---|---|---|
| `CircuitBreaker.getCurrentTimestamp()` / `getTimestampUnit()` | ✘ | ✔ | ✔ | ✔ | 0.1.x 无法使用条目自身时钟（受 1.3 下限约束） |
| `CircuitBreaker.onResult(long, TimeUnit, Object)` | ✘ | ✘ | ✔ | ✔ | 同上 |
| `RateLimiter.onSuccess()` / `onError()` / `onResult()` | ✘ | ✘ | ✔ | ✔ | 同上 |
| `decorateCheckedSupplier(...)` 返回类型 | `io.vavr.CheckedFunction0` | `io.vavr.CheckedFunction0` | `io.vavr.CheckedFunction0` | `io.github.resilience4j.core.functions.CheckedSupplier` | `.apply()` → `.get()` |
| `Registry.getTags()` 值类型 | `io.vavr.collection.Map` | `io.vavr.collection.Map` | `io.vavr.collection.Map` | `java.util.Map` | 2.x 移除 vavr |
| `io.github.resilience4j.common.*.configuration.*ConfigurationProperties` | 无前缀 | 无前缀 | 无前缀 | `Common` 前缀 | 2.x `resilience4j-framework-common` 类改名 |
| `CircuitBreakerConfiguration` 上的 `@Import({FallbackConfiguration, SpelResolverConfiguration})` | ✘ | ✘ | ✘ | ✔ | 0.1.x 必须手工注册这些 Bean |

## `microsphere-resilience4j-commons`（src/main）

| 文件 | 实现差异 | 原因解释 |
|---|---|---|
| `bulkhead/BulkheadTemplate.java` | `registry.bulkhead(name, configuration)` → `registry.bulkhead(name, configuration, registry.getTags())` | **功能对齐 / 功能需要。** 三参重载在 1.x 也存在，但其 `tags` 参数是 `io.vavr.collection.Map`；2.x 把 tags 改为 `java.util.Map`，模板因此可以在不引入 vavr 的前提下透传注册表级标签。效果：通过模板创建的 `Bulkhead` 会继承注册表标签。 |
| `circuitbreaker/CircuitBreakerTemplate.java` | `call(...)`：`decorateCheckedSupplier(callback::get).apply()` → `.get()` | **API 兼容。** 2.x 弃用 vavr，返回自研 `CheckedSupplier`，其抽象方法名为 `get()`。 |
| `circuitbreaker/CircuitBreakerTemplate.java` | `doBegin`：`System.nanoTime()` → `circuitBreaker.getCurrentTimestamp()`；`doEnd`：耗时 = `getCurrentTimestamp() - startTime`，时间单位取自 `circuitBreaker.getTimestampUnit()`，不再硬编码 `NANOSECONDS` | **API 兼容 + 正确性。** 时钟/单位 API 从 1.6 起才可用（高于 0.1.x 下限）；同时它才是正确的耗时来源——指标按 `CircuitBreaker` 自身配置的时间单位上报，而不是固定按纳秒。 |
| `circuitbreaker/CircuitBreakerTemplate.java` | `doEnd`：当 `context.getResult() != null` 时调用 `onResult(duration, unit, result)`，否则 `onSuccess(duration, unit)` | **新能力（基于返回值的记录）。** `onResult` 仅在 1.7/2.x 可用；`Resilience4jContext.result` 字段一直存在，但在 1.3 下限下无法回馈给熔断器。 |
| `common/Resilience4jModule.java` | 六项配置属性类全部重新绑定：`Retry/CircuitBreaker/RateLimiter/TimeLimiter/Bulkhead/ThreadPoolBulkhead ConfigurationProperties` → `Common…ConfigurationProperties` | **API 兼容。** 2.x 将 framework-neutral 的属性持有类改名并加 `Common` 前缀，无前缀名字留给 Spring Boot 专用子类。该枚举承载“模块 → 配置属性类”的映射，必须跟随改名。 |
| `ratelimiter/RateLimiterTemplate.java` | `call(...)`：`.apply()` → `.get()` | **API 兼容。** 同为 vavr → `CheckedSupplier` 的改动。 |
| `ratelimiter/RateLimiterTemplate.java` | `doEnd(...)`：删除第二次 `waitForPermission(rateLimiter)`；改为 `onSuccess()` / `onResult(result)` / `onError(failure)` 上报结果 | **功能需要 + 版本下限。** `doBegin` 已经申请许可，`doEnd` 再等一次许可是 1.x 时代的结果传递权宜做法。2.x（确切说 ≥1.7）的 `RateLimiter` 提供显式 `onSuccess/onError/onResult`，成功/失败/耗尽事件与指标都依赖它。 |
| `retry/RetryTemplate.java` | `call(...)`：`.apply()` → `.get()` | **API 兼容。** 同为 vavr → `CheckedSupplier` 的改动。 |

未变更（12 个文件）：`ThreadPoolBulkheadTemplate`、`TimeLimiterTemplate`、`Resilience4jTemplate`、`Resilience4jFacade`、`AdvancedResilience4jOperations`、`Resilience4jOperations`、`ChainableResilience4jFacade`、`CallbackChain`、`Resilience4jContext`、`Resilience4jConstants`、`util/Resilience4jUtils`、`resources/META-INF/default/templates.properties`。
`TimeLimiter` / `ThreadPoolBulkhead` 模板无需修改，因为它们使用的是 `decorateFutureSupplier(...).call()`、`future.get()` 等在 1.x/2.x 中签名未变的装饰器，没有触碰返回 vavr 类型的装饰器。

## `microsphere-resilience4j-spring`（src/main）

| 文件 | 实现差异 | 原因解释 |
|---|---|---|
| `bulkhead/annotation/EnableBulkhead.java` | 线程池隔离绑定类型 `ThreadPoolBulkheadConfigurationProperties` → `CommonThreadPoolBulkheadConfigurationProperties`（import 同步） | **API 兼容** — 2.x 类名变更。`resilience4j.bulkhead.*` 前缀仍绑定 Boot 侧的 `BulkheadConfigurationProperties`（1.7 与 2.0 均存在），只有 framework-common 的持有类改名。 |
| `bulkhead/annotation/EnableThreadPoolBulkhead.java` | 同样的改名（import + 绑定类型） | **API 兼容** — 同上。 |
| `common/annotation/EnableResilience4jRegistrar.java` | 从注册流程中移除 `registerCommonConfigurations(registry)` 及其静态导入 | **功能需要。** 1.x 的 `CircuitBreakerConfiguration` 并未 import `FallbackConfiguration`/`SpelResolverConfiguration`，所以 Registrar 必须自己注册（`FallbackConfiguration` 直接注册，`SpelResolverConfiguration` 在可选模块存在时反射注册）。2.x 中该类自带 `@Import({FallbackConfiguration.class, SpelResolverConfiguration.class})`，手工注册成为多余。 |
| `common/Resilience4jUtils.java` | **删除**（仅存在于 0.1.4） | 直接承接上一行：该类只包含 `registerCommonConfigurations` / `registerFallbackConfiguration` / `registerSpelResolverConfiguration` 及 `SPEL_RESOLVER_CONFIGURATION_CLASS_NAME` 常量，在 2.x 下完全过时。 |

未变更（27 个文件）：`LazyResilience4jFacade`、其余全部 `Enable*` 注解与 `*Registrar`、全部 `*ApplicationEventPublisher` / `*EventConsumerBeanRegistrar`、`common/event/EventConsumerMethodFilter` 与 `Resilience4jEvent*` 基类、`common/Resilience4jPlugin`、`resources/META-INF/spring.factories`。

## `microsphere-resilience4j-plugins/*`（src/main）

五个插件——`alibaba-druid`、`hibernate-core`、`mybatis`、`openfeign`、`spring-web`——在两个 tag 之间**逐字节相同**，包括各自的 `META-INF/spring.factories`。

原因：插件只依赖本项目的抽象（`Resilience4jTemplate`、`ChainableResilience4jFacade`）以及第三方 SPI（Druid `Filter`、MyBatis `Interceptor`、Feign `Capability`/`Client`、Spring Web `HandlerMethodInterceptor`、JPA `EntityCallback`）。Resilience4j 1.x→2.x 的断裂被 `commons` 与 `spring` 完全吸收；`javax`→`jakarta` 的切换由 parent POM 及 microsphere 的 druid/mybatis/hibernate BOM 承担，因此插件源码无需改动。

## 小结

全部 `src/main` 差异可归为三类：

1. **API 兼容（8 个变更文件中的 6 个）** — `Common*ConfigurationProperties` 改名；vavr 移除导致的 `.apply()` → `.get()`；条目时钟/单位与 `onResult` 因版本下限从 1.3 抬到 2.0 而可用。
2. **新增或纠正的行为** — `Bulkhead` 透传注册表标签；限流器 `doEnd` 改为上报 `onSuccess`/`onResult`/`onError`，不再重复等待许可。
3. **清理 1.x 兼容脚手架** — `-compatible` 模块（本次对比中忽略）、手工注册 `FallbackConfiguration`/`SpelResolverConfiguration` 及其 `Resilience4jUtils` 辅助类。

在所对比文件中未发现任何 Java 8/17 或 `javax`/`jakarta` 层面的源码差异：语言级别与命名空间的分裂完全体现在 parent POM 与导入的 BOM 中。
