# 源码差异对比：0.2.4 vs 0.1.4（`src/test`）

按 **artifactId / 目录名同名匹配** 的规则，逐个 Maven 模块对比两个最新 tag 的 `src/test` 内容。`0.2.4` = Resilience4j 2.0.0–2.4.0 发布线，`0.1.4` = Resilience4j 1.3.0–1.7.0 发布线。

按需求排除：Java 8 ↔ Java 17 的语法与 API 差异、Java EE ↔ Jakarta EE 命名空间差异（唯一发现的语言级改动单列并标注）；只在一侧存在的模块忽略。

## 模块总览

| 模块（`src/test`） | 0.1.4 文件数 | 0.2.4 文件数 | 可匹配 | 完全一致 | 有差异 | 仅 0.1.4 | 仅 0.2.4 |
|---|---|---|---|---|---|---|---|
| `microsphere-resilience4j-commons` | 13 | 14 | 12 | 7 | 5 | 1 | 2 |
| `microsphere-resilience4j-spring` | 14 | 13 | 13 | 6 | 7 | 1 | 0 |
| `microsphere-resilience4j-plugins/alibaba-druid` | 1 | 1 | 1 | 1 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/hibernate-core` | 0 | 1 | 0 | 0 | 0 | 0 | 1 |
| `microsphere-resilience4j-plugins/mybatis` | 2 | 2 | 2 | 2 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/openfeign` | 11 | 11 | 11 | 11 | 0 | 0 | 0 |
| `microsphere-resilience4j-plugins/spring-web` | 1 | 1 | 1 | 1 | 0 | 0 | 0 |
| **可匹配合计** | **42** | **42** | **40** | **28** | **12** | **2** | **3** |

- `microsphere-resilience4j-compatible`（仅 0.1.4）与纯 POM 模块（`-parent`、`-dependencies`、`-spring-boot`、`-spring-cloud`）无可对比的测试源码。

## `microsphere-resilience4j-commons`（src/test）

| 文件 | 测试差异 | 原因解释 |
|---|---|---|
| `bulkhead/BulkheadTemplateTest.java` | `maxConcurrentCalls` 由 `2` 改为 `1` | **让拒绝路径可复现。** 并发上限为 2 时“请求不被允许”分支依赖线程交错时序；上限为 1 时断言变为确定性结果。 |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | 配置构建器新增 `.maxWaitDurationInHalfOpenState(duration)` | **覆盖 0.1.x 下限不具备的选项。** 该 builder 方法在 Resilience4j 1.3/1.4 中不存在（1.6 才引入），0.1.x 测试只能不配置；2.0 下限下可以配置。 |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | 配置构建器新增 `.writableStackTraceEnabled(false)` | **降低测试噪声** —— 断言不再需要记录事件里的堆栈。该选项两条发布线都有，属于测试卫生选择，而非被迫改动。 |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | `onFailureRateExceededEvent` / `onSlowCallRateExceededEvent` 消费者新增 `assertSame(FAILURE_RATE_EXCEEDED / SLOW_CALL_RATE_EXCEEDED, event.getEventType())` | **断言加强，并解释了 `-compatible` 模块的存在意义。** 0.1.x 中这两个事件类由 `microsphere-resilience4j-compatible` 提供，事件类型常量只在 1.x 原生保证处断言；0.2.x 中它们是 Resilience4j 原生类型，可直接断言。 |
| `circuitbreaker/CircuitBreakerTemplateTest.java` | `testExecute()` 不再声明 `throws Throwable` | **清理，无行为变化** —— 方法体只调用 `template.execute(...)`，不会传播受检异常，多余的声明被去掉。 |
| `common/AbstractResilience4jTemplateTest.java` | `testBegin()` / `testEnd()` 重写：由 `ValueHolder<Object>` + `switch (module)` 的“先捕获后断言”改为直接 `if (template.isBeginSupported()) { …断言… } else { assertThrows(UnsupportedOperationException.class, …) }` | **功能需要。** 旧写法存在的原因是：不支持 begin/end 的模块会抛异常，必须先把异常暂存再进入模块分支。守卫式写法直接断言契约；并且 `testEnd` 现在覆盖**全部三条**上报路径——纯成功、`context.setResult("For testing")`（新的 `onResult(...)` 分支）、失败——这正是 `src/main` 中 `RateLimiterTemplate`/`CircuitBreakerTemplate` 改动所要求的。 |
| `common/AbstractResilience4jTemplateTest.java` | `testIsEndSupported()` 去掉 `assertEquals(isBeginSupported(), isEndSupported())` | **去冗余** —— 紧随其后的按模块 `switch` 已分别断言两个标志，跨标志的等值断言是重复的。（实现仍是对称的：两个 tag 中 `RetryTemplate`/`TimeLimiterTemplate` 的两个方法都返回 `false`。） |
| `common/AbstractResilience4jTemplateTest.java` | 删除 `io.microsphere.util.ValueHolder` 导入；`case A: case B: case C:` → `case A, B, C:` | **仅语言级差异**（Java 14+ 多标签 `case`，由 Java 17 发布线启用）—— 列出仅为便于对照原始 diff，**不算**实现差异。 |
| `common/Resilience4jModuleTest.java` | 六处 `assertModule(...)` 全部改为传入 `Common*ConfigurationProperties.class` | **API 兼容** —— 与 `Resilience4jModule` 枚举的重新绑定一致；2.x 对 framework-common 属性持有类改名。 |
| `ratelimiter/RateLimiterTemplateTest.java` | `SUCCESSFUL_ACQUIRE` 消费者新增 `assertEquals(1, event.getNumberOfPermits())` | **新流程让该覆盖变得有意义。** `getNumberOfPermits()` 在 1.x 已存在，但 0.1.x 模板从不向 `RateLimiter` 回馈结果；现在 `doEnd` 会调用 `onSuccess()`，发出的事件带上实际授予的许可数，才具备可断言性。 |

新增 / 删除文件：

| 路径 | 状态 | 原因解释 |
|---|---|---|
| `common/ChainableResilience4jFacadeTest2.java` | 删除（仅 0.1.4） | 重命名——见下一行。 |
| `common/ChainableResilience4jFacadeTest.java` | 新增（仅 0.2.4） | **测试重构，覆盖不变。** 去掉临时的 `2` 后缀；把内联构造五个 `*Registry.ofDefaults()` 注册表的部分替换为 `createChainableResilience4jFacade()`。 |
| `test/Resilience4jTestUtils.java` | 新增（仅 0.2.4） | **提取共享夹具** —— 一个返回默认 `ChainableResilience4jFacade` 的 `@Nonnull` 工厂方法，供 commons 的门面测试与新增的 hibernate-core 插件测试共用，避免重复装配注册表。 |

未变更（7 个文件）：`ThreadPoolBulkheadTemplateTest`、`CallbackChainTest`、`Resilience4jConstantsTest`、`Resilience4jContextTest`、`RetryTemplateTest`、`TimeLimiterTemplateTest`、`util/Resilience4jUtilsTest`。

## `microsphere-resilience4j-spring`（src/test）

| 文件 | 测试差异 | 原因解释 |
|---|---|---|
| `bulkhead/annotation/EnableBulkheadTest.java` | 局部变量类型改为 `CommonBulkheadConfigurationProperties.InstanceProperties` | **API 兼容** —— 2.x 将 `InstanceProperties` 移到 `Common*` 持有类之下。断言值（`maxConcurrentCalls` 10、缓冲区 100、`maxWaitDuration` 30 秒）未变。 |
| `bulkhead/annotation/EnableThreadPoolBulkheadTest.java` | 字段与嵌套类型导入改为 `CommonThreadPoolBulkheadConfigurationProperties` | **API 兼容** —— 同一改名；断言不变。 |
| `circuitbreaker/annotation/EnableCircuitBreakerTest.java` | 局部变量类型改为 `CommonCircuitBreakerConfigurationProperties.InstanceProperties` | **API 兼容** —— 同一改名；断言不变。 |
| `circuitbreaker/event/CircuitBreakerApplicationEventPublisherTest.java` | `@ContextConfiguration` 去掉 `initializers = {CommonConfigurationsApplicationContextInitializer.class}`；`CircuitBreakerConfigurationProperties` 与 `CircuitBreakerConfiguration` 交换声明顺序 | **功能需要。** 该 initializer 唯一作用是注入 `FallbackConfiguration`/`SpelResolverConfiguration` Bean——因为 1.x 的 `CircuitBreakerConfiguration` 没有 import 它们。2.x 中该类自带 `@Import({FallbackConfiguration.class, SpelResolverConfiguration.class})`，上下文无需 initializer 即可加载。 |
| `circuitbreaker/event/CircuitBreakerEventConsumerBeanRegistrarTest.java` | 同样去掉 initializer；`静态导入 ofMillis(100)` → 显式 `Duration.ofMillis(100)` | **功能需要**（同上）+ 纯 **风格** 的导入改动；耗时断言的含义完全一致。 |
| `ratelimiter/annotation/EnableRateLimiterTest.java` | 测试结束前新增 `rateLimiter.onSuccess();` | **功能需要（2.x 流程）。** 限流器只有在结果被显式上报后才会发出成功事件与指标；该调用使测试与新的 `RateLimiterTemplate.doEnd` 契约保持一致。 |
| `retry/annotation/EnableRetryTest.java` | 新增 `assertEquals(valueOf(1), instanceProperties.getMaxAttempts())` | **覆盖增强** —— 验证配置的 `max-attempts` 确实经由 `EnableRetry` 绑定；周边断言未变。 |

删除文件：

| 路径 | 状态 | 原因解释 |
|---|---|---|
| `context/CommonConfigurationsApplicationContextInitializer.java` | 删除（仅 0.1.4） | **兼容脚手架退役。** 它为两个熔断事件测试注册 1.x 的 fallback / SpEL-resolver 公共配置；对应的外部辅助（`Resilience4jUtils.registerCommonConfigurations`）及其在 `EnableResilience4jRegistrar` 中的调用在 0.2.4 中因同一原因被移除。 |

未变更（6 个文件）：`LazyResilience4jFacadeTest`、`LoggingResilience4jPlugin`、`NoOpResilience4jPlugin`、`common/event/EventConsumerMethodFilterTest`、`timelimiter/annotation/EnableTimeLimiterTest`、`resources/META-INF/spring.factories`。

## `microsphere-resilience4j-plugins/*`（src/test）

| 模块 | 差异 | 原因解释 |
|---|---|---|
| `hibernate-core` | **新增** `Resilience4jHibernateEntityCallbackTest`，继承 `AbstractHibernateH2Test` | **补齐覆盖空白。** 0.1.4 中该插件完全没有测试。0.2.4 的模块 POM 增加了 `microsphere-hibernate-test`、`microsphere-spring-test`、`spring-test` 以及 H2/logback 测试依赖，正是这些依赖使基于 H2 的实体回调测试成为可能。属于覆盖补齐，不是 API 移植的副产物。 |
| `alibaba-druid` | 完全一致 | — |
| `mybatis` | 完全一致 | — |
| `openfeign` | 完全一致（11 个文件） | — |
| `spring-web` | 完全一致 | — |

## 小结

`src/test` 的差异与 `src/main` 几乎一一对应：

1. **改名连带（3 个文件）** —— 测试中所有 `*ConfigurationProperties` 引用都跟随 `Resilience4jModule` 与 `Enable*` 注解的 `Common*` 改名。
2. **行为连带（3 个文件）** —— `AbstractResilience4jTemplateTest` 重构以驱动成功/结果/失败三条上报路径；`EnableRateLimiterTest` 增加显式 `onSuccess()`；`RateLimiterTemplateTest` 增加许可数断言。
3. **覆盖增长（4 个文件）** —— `maxWaitDurationInHalfOpenState`、失败率/慢调用率事件的类型断言、`max-attempts` 绑定，以及全新的 hibernate-core 回调测试。
4. **脚手架清理（2 个文件 + 1 处重命名）** —— `CommonConfigurationsApplicationContextInitializer` 及其 `initializers = {…}` 装配消失；`ChainableResilience4jFacadeTest2` 更名为 `ChainableResilience4jFacadeTest`，其夹具提取为新的共享 `Resilience4jTestUtils`。

所对比的测试源码中，唯一观察到的语言级差异是 `AbstractResilience4jTemplateTest` 里的 Java 17 多标签 `case` 写法；未出现 `javax`/`jakarta` 相关差异。
