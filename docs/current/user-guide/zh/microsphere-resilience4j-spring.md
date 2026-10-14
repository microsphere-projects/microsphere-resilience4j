# microsphere-resilience4j-spring — 用户指南

> 源分支：`release` · 版本线：`0.2.x`（最新发布 `0.2.4`）· 目标：**Resilience4j 2.0.0 – 2.4.0**（默认 `2.4.0`）
> 返回 [指南索引](README.md) · 英文原文：[microsphere-resilience4j-spring — User Guide](../microsphere-resilience4j-spring.md)

`microsphere-resilience4j-spring` 是 microsphere-resilience4j 的 Spring 层。它把
`microsphere-resilience4j-commons` 中与框架无关的模板变成一套 Spring 使用体验：

- 一族 `@Enable*` 注解（`@EnableCircuitBreaker`、`@EnableRateLimiter`、`@EnableRetry`、
  `@EnableBulkhead`、`@EnableThreadPoolBulkhead`、`@EnableTimeLimiter`），它们注册 Resilience4j 自己的
  Spring 配置类，从而注册该模块的 `Registry` bean；
- 自动把 `microsphere.resilience4j.<module>.*` 属性绑定到 Resilience4j 的
  `*ConfigurationProperties` 类型上；
- 一个**事件桥**，它把 Resilience4j 事件重新发布为 Spring 应用事件
  （`publishEvents = true`），并把你的 `EventConsumer` bean 注入到 Resilience4j 的事件发布者
  （`consumeEvents = true`）；
- 一个 `Resilience4jPlugin` SPI，使框架插件（MyBatis、Druid、Spring Web……）可以把自身挂接到某个
  `@Enable*` 注解上；
- `LazyResilience4jFacade`，一个 `Resilience4jFacade` bean，在容器刷新之后由上下文中存在的
  `Registry` bean 组装而成。

---

## 1. 前置条件

| 要求 | 说明 |
|-------------|--------|
| `microsphere-resilience4j-commons` | 本模块的非可选编译依赖——模板、`Resilience4jFacade`、`Resilience4jModule`、`Resilience4jConstants` 全部来自那里。 |
| `microsphere-spring-context` | 非可选编译依赖——提供 `BeanCapableImportCandidate`、`BeanRegistrar`、`ResolvablePlaceholderAnnotationAttributes`、`@EnableConfigurationBeanBinding`、`SpringFactoriesLoaderUtils`。 |
| Spring Framework / Spring Context | 由你的应用提供（`microsphere-spring-cloud-parent` 版本线）。 |
| Resilience4j 构件 | **本模块 POM 中每一个 `io.github.resilience4j:*` 依赖都是 `<optional>true</optional>`**（`resilience4j-spring`；`spring-webmvc` 同样是可选的），因此请在自己的 POM 中声明你实际使用的 Resilience4j 构件——见 §2.2。 |
| `@Configuration` 类 | `@Enable*` 注解是 `@Target(TYPE)`：把它们放在配置类上（或者在测试中直接放在测试类上，测试类同时充当配置）。 |
| JDK | `release` 分支根 POM 设置了 `<java.version>17</java.version>`，即 `0.2.x` 版本线的构建基线是 **JDK 17**（在该父 POM 下对应 Spring Boot 3/4 与 Spring Cloud 2022+）。JDK 8 版本线是 `0.1.x`——见 [1.x 用户指南](../../../1.x/user-guide/zh/README.md)。 |

---

## 2. Maven 坐标与版本

### 2.1 坐标

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
        <artifactId>microsphere-resilience4j-spring</artifactId>
        <!-- version managed by the BOM -->
    </dependency>
</dependencies>
```

BOM（`microsphere-resilience4j-dependencies`）与 `…-commons` 以及各插件构件一起管理着
`microsphere-resilience4j-spring`，全部使用项目版本号。

### 2.2 你必须自行添加的 Resilience4j 构件

这些注解直接引用了 Resilience4j 的类型
（`io.github.resilience4j.<module>.configure.<Module>Configuration` 和
`io.github.resilience4j.<module>.configure.<Module>ConfigurationProperties`）。这些类来自
Resilience4j 的 Spring 支持构件，而本模块把它们声明为**可选**
（其自身 POM 中的 `resilience4j-spring`，commons POM 中的 `resilience4j-framework-common` / `resilience4j-core` /
`resilience4j-<module>`），因此需要你自行声明：

```xml
<dependencies>
    <!-- Resilience4j Spring support: hosts the <Module>Configuration / <Module>ConfigurationProperties
         classes that the registrars register and that @EnableConfigurationBeanBinding binds -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring</artifactId>
        <version>2.4.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-framework-common</artifactId>
        <version>2.4.0</version>
    </dependency>
    <!-- the runtime module(s) you enable, e.g. -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-circuitbreaker</artifactId>
        <version>2.4.0</version>
    </dependency>
</dependencies>
```

### 2.3 版本矩阵（本版本线）

| 项目 | 值 | 核实来源 |
|------|-------|---------------|
| 项目版本线 | `0.2.x`，最新发布 `0.2.4`，下一版 `0.2.5-SNAPSHOT` | 根 `pom.xml`（`${revision}`）、`README.md` |
| Resilience4j | 通过 Maven profiles `resilience4j-2.0` … `resilience4j-2.4` 支持 `2.0.0`、`2.1.0`、`2.2.0`、`2.3.0`、`2.4.0`；**默认 profile `resilience4j-2.4` → `2.4.0`** | `microsphere-resilience4j-parent/pom.xml` |
| Spring Cloud | `2022.0.x` – `2025.0.x`（父 POM `microsphere-spring-cloud-parent` `0.2.26`，默认 profile `spring-cloud-2025` → Spring Boot `3.5.15` / Spring Cloud `2025.0.3`） | 父 POM profiles |
| JDK | `17`（`<java.version>17</java.version>`） | 根 `pom.xml` |
| Spring Framework 版本 | 由导入的 `microsphere-spring-cloud-dependencies` BOM 管理；本模块 POM 中没有显式版本 | `microsphere-resilience4j-spring/pom.xml` |

> **值得了解的 POM 怪癖**：`microsphere-resilience4j-spring/pom.xml` 重新声明了 profile
> `resilience4j-2.4`（仍然是 `activeByDefault`），但把 `<resilience4j.version>` 写成 `2.3.0`。
> 子级 profile 会替换继承来的那个，因此在**只构建本模块**且不带 `-P` 时，解析出的 Resilience4j
> 版本是 `2.3.0`。当确切的 Resilience4j 构建版本很重要时，请从 reactor 根目录传入
> `-P resilience4j-2.4`，或者自行锁定版本。

---

## 3. `@Enable*` 注解

### 3.1 公共元注解 `@EnableResilience4j`

`io.microsphere.resilience4j.spring.common.annotation.EnableResilience4j` **不**供应用直接使用：
它被声明为 `@Target(ANNOTATION_TYPE)`，因此只作为六个按模块划分的注解上的元注解存在，
其属性在这些注解上通过 `@AliasFor` 重新暴露。

| 属性 | 类型 | 默认值 | 含义 |
|-----------|------|---------|---------|
| `publishEvents` | `boolean` | `false` | 注册本模块的 `Resilience4jEventApplicationEventPublisher` 实现 → Resilience4j 事件变为 Spring 应用事件。 |
| `consumeEvents` | `boolean` | `false` | 注册本模块的 `Resilience4jEventConsumerBeanRegistrar` 实现 → 你的 `EventConsumer` bean 会被订阅到 Resilience4j 的事件发布者上。 |
| `plugins` | `String[]` | `{}` | 要激活的插件的 **`Resilience4jPlugin.getName()` 名称**（与在 `META-INF/spring.factories` 中找到的插件进行匹配）。 |

> `publishEvents`/`consumeEvents` 的 Javadoc 写着“默认 `true`”，但本模块中每个注解编译出来的默认值
> 都是 **`false`**（`boolean publishEvents() default false;`）。请以代码为准：如果不设置这些开关，
> 就不会注册任何事件桥。
> 同样，按模块划分的注解中 `plugins()` 的 Javadoc 讲的是“Spring Bean 名称”，而注册器拿这些值去
> 比对的是 `Resilience4jPlugin#getName()`，也就是说**是插件名称，不是 bean 名称**。

### 3.2 六个公共注解

每个按模块划分的注解都恰好声明相同的三个属性（`publishEvents`、
`consumeEvents`、`plugins`），全部带 `@AliasFor(annotation = EnableResilience4j.class, …)`；每个注解都是
`@Target(TYPE) @Retention(RUNTIME) @Documented @Inherited`，都标注了 `@EnableResilience4j`，并且
`@Import` 自己的（包级私有的）注册器。不同的地方在于它们所引入的模块装配：

| 注解 | 注册器（被 `@Import`） | 入口类型 `E` | 注册的入口配置 `EC` | 由 `@EnableConfigurationBeanBinding(s)` 绑定的属性 |
|------------|-------------------------|----------------|-------------------------------------|----------------------------------------------------------|
| `…spring.circuitbreaker.annotation.EnableCircuitBreaker` | `EnableCircuitBreakerRegistrar` | `CircuitBreaker` | `CircuitBreakerConfiguration` | `microsphere.resilience4j.circuit-breaker.` → `CircuitBreakerConfigurationProperties` |
| `…spring.ratelimiter.annotation.EnableRateLimiter` | `EnableRateLimiterRegistrar` | `RateLimiter` | `RateLimiterConfiguration` | `microsphere.resilience4j.rate-limiter.` → `RateLimiterConfigurationProperties` |
| `…spring.retry.annotation.EnableRetry` | `EnableRetryRegistrar` | `Retry` | `RetryConfiguration` | `microsphere.resilience4j.retry.` → `RetryConfigurationProperties` |
| `…spring.timelimiter.annotation.EnableTimeLimiter` | `EnableTimeLimiterRegistrar` | `TimeLimiter` | `TimeLimiterConfiguration` | `microsphere.resilience4j.time-limiter.` → `TimeLimiterConfigurationProperties` |
| `…spring.bulkhead.annotation.EnableBulkhead` | `EnableBulkheadRegistrar` | `Bulkhead` | `BulkheadConfiguration` | `microsphere.resilience4j.bulkhead.` → `BulkheadConfigurationProperties` **以及** `microsphere.resilience4j.thread-pool-bulkhead.` → `CommonThreadPoolBulkheadConfigurationProperties` |
| `…spring.bulkhead.annotation.EnableThreadPoolBulkhead` | `EnableThreadPoolBulkheadRegistrar` | `ThreadPoolBulkhead` | `BulkheadConfiguration`（与 `@EnableBulkhead` 同一个类） | 与 `@EnableBulkhead` 相同的两个绑定 |

这六个注解上的三个属性完全相同：

| 属性 | 类型 | 默认值 | 含义 |
|-----------|------|---------|---------|
| `publishEvents` | `boolean` | `false` | 注册本模块的 `Resilience4jEventApplicationEventPublisher`（见 §6.1） |
| `consumeEvents` | `boolean` | `false` | 注册本模块的 `Resilience4jEventConsumerBeanRegistrar`（见 §6.2） |
| `plugins` | `String[]` | `{}` | 要激活的插件名称，与 `Resilience4jPlugin#getName()` 比对（见 §7） |

在这些值被使用之前，注册器会用
`ResolvablePlaceholderAnnotationAttributes.of(annotationAttributes, annotationType, getEnvironment())`
（`io.microsphere.spring.core.annotation`，来自 microsphere-spring-context）把原始属性 Map
包装起来，其作用是针对 Spring `Environment` 解析 `${…}`
占位符，然后用
`attributes.getBoolean("publishEvents")` / `getBoolean("consumeEvents")` 和
`attributes.getStringArray("plugins")` 读取它们。

说明：

- 前缀来自 `io.microsphere.resilience4j.common.Resilience4jConstants`（commons）：
  `PREFIX = "microsphere.resilience4j."`，再加上 `retry.`、`circuit-breaker.`、`rate-limiter.`、
  `time-limiter.`、`bulkhead.`、`thread-pool-bulkhead.`。
- `@EnableThreadPoolBulkhead` 把 `BulkheadConfiguration` 作为其入口配置类型（源码中第三个泛型参数写的
  就是它），并且——由于 `META-INF/spring.factories` 中没有提供 `ThreadPoolBulkhead`
  类型的事件组件——它的 `publishEvents`/`consumeEvents` 开关目前什么都不会注册。如果你需要
  `BulkheadEvent`（`ThreadPoolBulkhead` 同样会发布它），请同时启用
  `@EnableBulkhead(publishEvents = true, consumeEvents = true)`；本模块的
  `EnableThreadPoolBulkheadTest` 做的正是这件事。
- 注册器是包级私有类，因此用户代码无法直接 `@Import` 它们——请使用注解。

### 3.3 最小用法

```java
import io.microsphere.resilience4j.spring.circuitbreaker.annotation.EnableCircuitBreaker;
import io.microsphere.resilience4j.spring.retry.annotation.EnableRetry;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCircuitBreaker(publishEvents = true, consumeEvents = true)
@EnableRetry(publishEvents = true)
public class ResilienceConfig {
}
```

---

## 4. 配置属性

每个 `@Enable*` 注解都会把 `microsphere.resilience4j.<module>.*` 绑定到 Resilience4j 自己的
`*ConfigurationProperties` bean 上。本模块中的测试就是参考范例：

```properties
# from EnableCircuitBreakerTest
microsphere.resilience4j.circuit-breaker.instances[test].waitDurationInOpenState=PT30S
microsphere.resilience4j.circuit-breaker.instances[test].slidingWindowSize=100
microsphere.resilience4j.circuit-breaker.instances[test].slowCallRateThreshold=0.7

# from EnableRetryTest
microsphere.resilience4j.retry.instances[test].waitDuration=PT1S
microsphere.resilience4j.retry.instances[test].maxAttempts=1
microsphere.resilience4j.retry.instances[test].eventConsumerBufferSize=99

# from EnableRateLimiterTest
microsphere.resilience4j.rate-limiter.instances[test].timeoutDuration=PT10S
microsphere.resilience4j.rate-limiter.instances[test].limitRefreshPeriod=PT0.000001S
microsphere.resilience4j.rate-limiter.instances[test].limitForPeriod=20

# from EnableTimeLimiterTest
microsphere.resilience4j.time-limiter.instances[test].timeoutDuration=PT10S
microsphere.resilience4j.time-limiter.instances[test].cancelRunningFuture=true
microsphere.resilience4j.time-limiter.instances[test].eventConsumerBufferSize=200

# from EnableBulkheadTest
microsphere.resilience4j.bulkhead.instances[test].maxConcurrentCalls=10
microsphere.resilience4j.bulkhead.instances[test].eventConsumerBufferSize=100
microsphere.resilience4j.bulkhead.instances[test].maxWaitDuration=PT30S

# from EnableThreadPoolBulkheadTest
microsphere.resilience4j.thread-pool-bulkhead.instances[test].maxThreadPoolSize=1
microsphere.resilience4j.thread-pool-bulkhead.instances[test].coreThreadPoolSize=1
microsphere.resilience4j.thread-pool-bulkhead.instances[test].queueCapacity=1
```

本模块的每个测试还都加上了 `@EnableSpringConverterAdapter`
（`io.microsphere.spring.core.convert.annotation`，来自 `microsphere-spring-context`）。当你像测试那样
把 `PT30S`、`PT0.000001S` 或 `0.7` 这类值绑定到 Resilience4j 的配置类型时，请保留它。

---

## 5. 可运行示例（取自本模块的测试）

### 5.1 CircuitBreaker：注册表 + 属性 + Spring 事件

来自 `io.microsphere.resilience4j.spring.circuitbreaker.annotation.EnableCircuitBreakerTest`：

```java
@SpringJUnitConfig(classes = EnableCircuitBreakerTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.circuit-breaker.instances[test].waitDurationInOpenState=PT30S",
        "microsphere.resilience4j.circuit-breaker.instances[test].slidingWindowSize=100",
        "microsphere.resilience4j.circuit-breaker.instances[test].slowCallRateThreshold=0.7"})
@EnableCircuitBreaker(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
class EnableCircuitBreakerTest {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Autowired
    private CircuitBreakerConfigurationProperties properties;

    @Test
    void test() {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("test");
        circuitBreaker.acquirePermission();
        circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);

        CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceProperties =
                properties.getInstances().get("test");
        assertEquals(Float.valueOf(0.7f), instanceProperties.getSlowCallRateThreshold());
        assertEquals(Integer.valueOf(100), instanceProperties.getSlidingWindowSize());
        assertEquals(Duration.ofSeconds(30), instanceProperties.getWaitDurationInOpenState());
    }

    @EventListener(CircuitBreakerOnSuccessEvent.class)
    void onCircuitBreakerOnSuccessEvent(CircuitBreakerOnSuccessEvent event) {
        assertEquals("test", event.getCircuitBreakerName());
        assertEquals(Duration.ofMillis(100), event.getElapsedDuration());
    }
}
```

这个测试所说明的归属关系是：`CircuitBreakerRegistry` 来自注册器所注册的
`CircuitBreakerConfiguration` 类（这正是 `resilience4j-spring` 必须出现在你的 classpath 上的原因——§2.2），
`CircuitBreakerConfigurationProperties` 由注解的
`@EnableConfigurationBeanBinding` 绑定，而 `@EventListener` 桥只是因为
`publishEvents = true` 才存在。

### 5.2 Retry

来自 `EnableRetryTest`：

```java
@SpringJUnitConfig(classes = EnableRetryTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.retry.instances[test].waitDuration=PT1S",
        "microsphere.resilience4j.retry.instances[test].maxAttempts=1",
        "microsphere.resilience4j.retry.instances[test].eventConsumerBufferSize=99"})
@EnableRetry(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
class EnableRetryTest {

    @Autowired
    private RetryRegistry registry;

    @Autowired
    private RetryConfigurationProperties properties;

    @Test
    void test() {
        Retry retry = registry.retry("test");
        RetryConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(ofSeconds(1), instanceProperties.getWaitDuration());
        assertEquals(valueOf(1), instanceProperties.getMaxAttempts());
        assertEquals(valueOf(99), instanceProperties.getEventConsumerBufferSize());

        retry.executeSupplier(() -> "Hello,World");
    }
}
```

### 5.3 RateLimiter / TimeLimiter

来自 `EnableRateLimiterTest` 与 `EnableTimeLimiterTest`：

```java
@EnableRateLimiter(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
@SpringJUnitConfig(classes = EnableRateLimiterTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.rate-limiter.instances[test].timeoutDuration=PT10S",
        "microsphere.resilience4j.rate-limiter.instances[test].limitRefreshPeriod=PT0.000001S",
        "microsphere.resilience4j.rate-limiter.instances[test].limitForPeriod=20"})
class EnableRateLimiterTest {

    @Autowired private RateLimiterRegistry registry;
    @Autowired private RateLimiterConfigurationProperties properties;

    @Test
    void test() {
        RateLimiter rateLimiter = registry.rateLimiter("test");
        rateLimiter.acquirePermission();
        RateLimiterConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(ofSeconds(10), instanceProperties.getTimeoutDuration());
        assertEquals(valueOf(20), instanceProperties.getLimitForPeriod());
        assertEquals(ofNanos(1000), instanceProperties.getLimitRefreshPeriod());
        rateLimiter.onSuccess();
    }

    @EventListener(RateLimiterOnSuccessEvent.class)
    void onRateLimiterOnSuccessEvent(RateLimiterOnSuccessEvent event) {
        assertEquals("test", event.getRateLimiterName());
        assertEquals(SUCCESSFUL_ACQUIRE, event.getEventType());
    }
}
```

```java
@EnableTimeLimiter(publishEvents = true, consumeEvents = true)
@SpringJUnitConfig(classes = EnableTimeLimiterTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.time-limiter.instances[test].timeoutDuration=PT10S",
        "microsphere.resilience4j.time-limiter.instances[test].cancelRunningFuture=true",
        "microsphere.resilience4j.time-limiter.instances[test].eventConsumerBufferSize=200"})
class EnableTimeLimiterTest {

    @Autowired private TimeLimiterRegistry registry;
    @Autowired private TimeLimiterConfigurationProperties properties;

    @Test
    void test() {
        TimeLimiter timeLimiter = registry.timeLimiter("test");
        TimeLimiterConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(ofSeconds(10), instanceProperties.getTimeoutDuration());
        assertEquals(TRUE, instanceProperties.getCancelRunningFuture());
        assertEquals(valueOf(200), instanceProperties.getEventConsumerBufferSize());
        timeLimiter.onSuccess();
    }

    @EventListener(TimeLimiterOnSuccessEvent.class)
    void onTimeLimiterOnSuccessEvent(TimeLimiterOnSuccessEvent event) {
        assertEquals("test", event.getTimeLimiterName());
    }
}
```

### 5.4 Bulkhead（含 replace/remove 生命周期）

来自 `EnableBulkheadTest`——注意 `plugins = NAME` 属性（见 §7）以及注册表生命周期调用：

```java
@SpringJUnitConfig(classes = EnableBulkheadTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.bulkhead.instances[test].maxConcurrentCalls=10",
        "microsphere.resilience4j.bulkhead.instances[test].eventConsumerBufferSize=100",
        "microsphere.resilience4j.bulkhead.instances[test].maxWaitDuration=PT30S"})
@EnableBulkhead(publishEvents = true, consumeEvents = true, plugins = NAME) // NAME == "logging"
@EnableSpringConverterAdapter
class EnableBulkheadTest {

    @Autowired private BulkheadRegistry registry;
    @Autowired private BulkheadConfigurationProperties properties;

    @Test
    void test() {
        String entryName = "test";
        Bulkhead circuitBreaker = registry.bulkhead(entryName);
        circuitBreaker.acquirePermission();

        CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(Integer.valueOf(10), instanceProperties.getMaxConcurrentCalls());
        assertEquals(Integer.valueOf(100), instanceProperties.getEventConsumerBufferSize());
        assertEquals(Duration.ofSeconds(30), instanceProperties.getMaxWaitDuration());

        registry.replace(entryName, circuitBreaker); // -> onEntryReplacedEvent
        registry.remove(entryName);                  // -> onEntryRemovedEvent
    }

    @EventListener(BulkheadOnCallPermittedEvent.class)
    void onBulkheadOnCallPermittedEvent(BulkheadOnCallPermittedEvent event) {
        assertEquals("test", event.getBulkheadName());
        assertEquals(CALL_PERMITTED, event.getEventType());
    }
}
```

### 5.5 ThreadPoolBulkhead

来自 `EnableThreadPoolBulkheadTest`——两个 bulkhead 注解都被加上，因为
单独使用 `@EnableThreadPoolBulkhead` 不提供任何事件桥，且其入口配置是
`BulkheadConfiguration`：

```java
@SpringJUnitConfig(classes = EnableThreadPoolBulkheadTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.thread-pool-bulkhead.instances[test].maxThreadPoolSize=1",
        "microsphere.resilience4j.thread-pool-bulkhead.instances[test].coreThreadPoolSize=1",
        "microsphere.resilience4j.thread-pool-bulkhead.instances[test].queueCapacity=1",
        "microsphere.resilience4j.bulkhead.instances[test].maxConcurrentCalls=10",
        "microsphere.resilience4j.bulkhead.instances[test].eventConsumerBufferSize=100",
        "microsphere.resilience4j.bulkhead.instances[test].maxWaitDuration=PT30S"})
@EnableBulkhead
@EnableThreadPoolBulkhead(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
class EnableThreadPoolBulkheadTest {

    @Autowired private ThreadPoolBulkheadRegistry registry;
    @Autowired private CommonThreadPoolBulkheadConfigurationProperties properties;

    @Test
    void test() throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead threadPoolBulkhead = registry.bulkhead("test");
        CompletionStage<String> completionStage = threadPoolBulkhead.executeCallable(() -> "Hello World");
        assertEquals("Hello World", completionStage.toCompletableFuture().get());

        InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(1, instanceProperties.getMaxThreadPoolSize());
        assertEquals(1, instanceProperties.getCoreThreadPoolSize());
        assertEquals(1, instanceProperties.getQueueCapacity());
    }

    @EventListener(BulkheadOnCallPermittedEvent.class)
    void onBulkheadOnCallPermittedEvent(BulkheadOnCallPermittedEvent event) {
        assertEquals("test", event.getBulkheadName());
        assertEquals(CALL_PERMITTED, event.getEventType());
    }
}
```

上面的 `@EventListener` 方法逐字复制自测试，但请注意在这些注解之下它永远不会被调用：
`@EnableThreadPoolBulkhead` 上的 `publishEvents = true` 不会注册任何发布者（§3.2），而这里的
`@EnableBulkhead` 保持了 `publishEvents` 的默认值 `false`。即便使用
`Bulkhead` 发布者也无济于事——它订阅的是 `Bulkhead` 条目，而不是像这里被执行的
`ThreadPoolBulkhead` 实例。

---

## 6. 消费 Resilience4j 事件

本模块提供了两个相互独立、彼此互补的方向。

```mermaid
flowchart LR
    ENTRY["Resilience4j 条目<br/>例如 CircuitBreaker 'test'"] -- "事件（CircuitBreakerOnSuccessEvent）" --> PUB["…ApplicationEventPublisher<br/>由 publishEvents = true 注册"]
    PUB -- "publishEvent(事件对象)" --> SPRING["Spring ApplicationEventPublisher"]
    SPRING --> LISTENER["@EventListener 方法"]
    CONSUMER["你的上下文中的 EventConsumer bean"] -. "consumeEvents = true：<br/>…EventConsumerBeanRegistrar 把它们<br/>订阅到条目的 EventPublisher 上" .-> ENTRY
```

### 6.1 把 Resilience4j 事件作为 Spring 应用事件（`publishEvents = true`）

当 `publishEvents = true` 时，注册器会注册与注解入口类型相匹配的
`Resilience4jEventApplicationEventPublisher` 实现——以下是其一种：

| 注解 | 发布者 bean 类 | 重新发布的事件类型 |
|------------|----------------------|-------------------------|
| `@EnableCircuitBreaker` | `…spring.circuitbreaker.event.CircuitBreakerApplicationEventPublisher` | `CircuitBreakerEvent` |
| `@EnableRateLimiter` | `…spring.ratelimiter.event.RateLimiterApplicationEventPublisher` | `RateLimiterEvent` |
| `@EnableRetry` | `…spring.retry.event.RetryApplicationEventPublisher` | `RetryEvent` |
| `@EnableTimeLimiter` | `…spring.timelimiter.event.TimeLimiterApplicationEventPublisher` | `TimeLimiterEvent` |
| `@EnableBulkhead` | `…spring.bulkhead.event.BulkheadApplicationEventPublisher` | `BulkheadEvent` |
| `@EnableThreadPoolBulkhead` | *（不注册任何 bean——见 §3.2 中的说明）* | — |

语义（来自 `Resilience4jEventApplicationEventPublisher`）：

- 它是一个 `RegistryEventConsumer<E>`，因此只要本模块的 `Registry` 新增或替换
  一个条目，它就会收到通知；对每个这样的条目，它通过反射调用 `getEventPublisher()`，并通过
  `EventPublisher#onEvent(...)` 订阅 `this::consumeEvent`，也就是该条目的**全部**事件；
- `consumeEvent(ET event)` 调用 `applicationEventPublisher.publishEvent(event)`——发布的就是 Resilience4j 事件
  对象本身（Spring 会把它包装进一个 `PayloadApplicationEvent`），因此你可以像 §5.1 那样完全按事件类来监听：

```java
@EventListener(CircuitBreakerOnSuccessEvent.class)
void onCircuitBreakerOnSuccessEvent(CircuitBreakerOnSuccessEvent event) { … }
```

`onEntryRemovedEvent` 实现为空操作，因此被移除的条目仍保持订阅（无害）；
每重新发布一个事件都会输出 trace 日志
`"The Resilience4j Event : {} was published on the Spring ApplicationListener(s)"`。

### 6.2 消费者方法（`consumeEvents = true`）

当 `consumeEvents = true` 时，注册器会注册相匹配的
`Resilience4jEventConsumerBeanRegistrar` 实现
（`CircuitBreakerEventConsumerBeanRegistrar`、`RateLimiterEventConsumerBeanRegistrar`、
`RetryEventConsumerBeanRegistrar`、`TimeLimiterEventConsumerBeanRegistrar`、
`BulkheadEventConsumerBeanRegistrar`）。它**不**发布 Spring 事件；相反，对于注册表中
新增/替换的每个条目，它会：

1. 反射获取 `entry.getEventPublisher()`（例如 `CircuitBreaker.EventPublisher`）；
2. 只保留 `EventConsumerMethodFilter` 认可的发布者方法：**恰好一个参数，
   且声明类型为 `io.github.resilience4j.core.EventConsumer`**——这正是使
   `onSuccess(EventConsumer<CircuitBreakerOnSuccessEvent>)`、`onError(...)`、`onEvent(...)` 等得以匹配、
   并使其他所有方法被忽略的原因；
3. 对每个这样的方法，用 `ResolvableType.forMethodParameter(method, 0)` 解析其*泛型*参数类型（例如
   `EventConsumer<CircuitBreakerOnSuccessEvent>`），并用 `beanFactory.getBeanProvider(type)` 找出
   **所有匹配的 Spring bean**；
4. 用每个找到的 bean 调用该发布者方法，也就是把它订阅上。

因此，消费者就是任何声明为 `EventConsumer<SomeResilience4jEvent>` 的 bean。来自
`CircuitBreakerEventConsumerBeanRegistrarTest`：

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CircuitBreakerConfigurationProperties.class,
        CircuitBreakerConfiguration.class,
        CircuitBreakerEventConsumerBeanRegistrar.class,   // the registrar bean, as @EnableCircuitBreaker(consumeEvents = true) registers it
        CircuitBreakerEventConsumerBeanRegistrarTest.class
})
class CircuitBreakerEventConsumerBeanRegistrarTest {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Test
    void test() {
        CircuitBreaker circuitBreaker = registry.circuitBreaker("test");
        circuitBreaker.acquirePermission();
        circuitBreaker.onSuccess(100, TimeUnit.MILLISECONDS);
    }

    @Bean
    public EventConsumer<CircuitBreakerOnSuccessEvent> circuitBreakerOnSuccessEventEventConsumer() {
        return event -> {
            assertEquals("test", event.getCircuitBreakerName());
            assertEquals(Duration.ofMillis(100), event.getElapsedDuration());
        };
    }
}
```

在应用代码中，用注解即可达到同样效果：

```java
@Configuration
@EnableCircuitBreaker(consumeEvents = true)
class ConsumerConfig {

    @Bean
    EventConsumer<CircuitBreakerOnSuccessEvent> onSuccessConsumer() {
        return event -> Metrics.record(event.getCircuitBreakerName(), event.getElapsedDuration());
    }
}
```

你也可以把两个方向结合起来（就像 `@Enable*` 测试中用
`publishEvents = true, consumeEvents = true` 那样）：用 `@EventListener` 获得松耦合，用 `EventConsumer`
bean 获得按事件类划分的强类型订阅。

---

## 7. 实现并注册 `Resilience4jPlugin`

`io.microsphere.resilience4j.spring.common.Resilience4jPlugin` 是框架插件
（`…-mybatis`、`…-alibaba-druid`、`…-spring-web`）所使用的扩展点：

```java
public interface Resilience4jPlugin {

    void plugin(ConfigurableListableBeanFactory beanFactory,
                BeanDefinitionRegistry registry,
                ConfigurableEnvironment environment);

    String getName();
}
```

实现必须遵守的三条规则（全部来自注册器代码）：

1. 它是在容器**之外**从 `META-INF/spring.factories` 实例化的（无参构造器），
   所以请保持该类为 public 并带有 public 无参构造器；
2. `getName()` 必须返回你将列在 `plugins` 属性中的那个字符串——匹配方式是
   `ArrayUtils.contains(plugins, plugin.getName())`，即**精确的插件名称**，而不是 bean 名称；
3. `plugin(...)` 在 `@Enable*` 注册器仍在注册 bean 定义时运行，所以它的职责是
   *注册 bean 定义*（或以其他方式操作注册表），而不是假定 bean 已经存在。

测试范围内的参考实现（`io.microsphere.resilience4j.spring.LoggingResilience4jPlugin`）：

```java
public class LoggingResilience4jPlugin implements Resilience4jPlugin {

    public static final String NAME = "logging";

    private static final Logger logger = getLogger(LoggingResilience4jPlugin.class);

    @Override
    public void plugin(ConfigurableListableBeanFactory beanFactory, BeanDefinitionRegistry registry,
                       ConfigurableEnvironment environment) {
        logger.trace("BeanFactory : {} , Environment : {}", beanFactory, environment);
    }

    @Override
    public String getName() {
        return NAME;
    }
}
```

在 `src/test/resources/META-INF/spring.factories` 中注册：

```properties
io.microsphere.resilience4j.spring.common.Resilience4jPlugin=\
io.microsphere.resilience4j.spring.LoggingResilience4jPlugin,\
io.microsphere.resilience4j.spring.NoOpResilience4jPlugin
```

并在 `EnableBulkheadTest` 中按名称激活：

```java
import static io.microsphere.resilience4j.spring.LoggingResilience4jPlugin.NAME;

@EnableBulkhead(publishEvents = true, consumeEvents = true, plugins = NAME)
class EnableBulkheadTest { … }
```

配套测试插件 `NoOpResilience4jPlugin`（名称 `"no-operation"`）展示了退化情形：一个
空的 `plugin(...)` 方法体。它会被加载，但因为没有任何注解列出它，所以从不被激活。

在本分支上项目自带插件所提供的插件名称：

| 插件类 | `getName()` | 模块 |
|--------------|-------------|--------|
| `io.microsphere.resilience4j.mybatis.MyBatisResilience4jPlugin` | `mybatis` | `microsphere-resilience4j-mybatis` |
| `io.microsphere.resilience4j.spring.web.SpringWebResilience4jPlugin` | `spring-web` | `microsphere-resilience4j-spring-web` |
| `io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin` | `alibaba-druid` | `microsphere-resilience4j-alibaba-druid` |

```java
@Configuration
@EnableBulkhead(publishEvents = true, plugins = {"mybatis", "spring-web"})
class ProductionConfig {
}
```

---

## 8. `LazyResilience4jFacade`

`io.microsphere.resilience4j.spring.LazyResilience4jFacade` 是统一编程模型在 Spring 侧的入口：
它实现 `Resilience4jFacade`（commons），并把职责委托给由你的上下文中恰好存在的
`Registry` bean 构建出的 `ChainableResilience4jFacade`。

- 由**每一个** `@Enable*` 注解自动注册，bean 名称固定为
  `LazyResilience4jFacade.BEAN_NAME` = `"lazyResilience4jFacade"`，并且是一个 **primary** bean 定义
  （`beanDefinition.setPrimary(true)`），其可变的 `modules` 属性会为每个启用的注解累积一个
  `Resilience4jModule`。
- 它是一个 `ApplicationListener<ContextRefreshedEvent>`：在容器刷新时，对每个启用的模块解析
  `context.getBean(module.getRegistryClass())` 并构造委托对象。**请在容器刷新之后再调用它**——
  在此之前委托对象是 `null`，任何操作都会抛出 `NullPointerException`。
- 它实现了 `io.microsphere.lang.DelegatingWrapper`，因此 `getDelegate()` 以及继承来的
  `unwrap(Class)` 会暴露底层的 `ChainableResilience4jFacade`。

| 方法 | 签名（来自源码） | 说明 |
|--------|-------------------------|-------|
| `execute` | `void execute(String name, Runnable callback)` / `<T> T execute(String name, Supplier<T> callback)` | 带包装的、对受检异常友好的入口 |
| `call` | `void call(String name, ThrowableAction)` · `<TR extends Throwable> void call(String name, ThrowableAction, Class<TR>)` · `<T> T call(String name, ThrowableSupplier<T>)` · `<T, TR extends Throwable> T call(String name, ThrowableSupplier<T>, Class<TR>)` | 抛出 `Throwable` / 所声明的 `TR` |
| `begin` / `end` | `Resilience4jContext<Resilience4jContext[]> begin(String name)` · `void end(Resilience4jContext<Resilience4jContext[]>)` | 手动装饰；`getEntry()` 是按模块划分的子上下文数组 |
| `isBeginSupported` / `isEndSupported` | `boolean` | 委托调用 |
| `setModules` / `getModules` | `void setModules(Set<Resilience4jModule>)` / `Set<Resilience4jModule> getModules()` | 由注册器通过 bean 定义属性设置 |
| `onApplicationEvent` | `void onApplicationEvent(ContextRefreshedEvent)` | 构造委托对象 |
| `getDelegate` | `Object getDelegate()` | 覆写 `DelegatingWrapper` 并返回当前委托对象（首次 `ContextRefreshedEvent` 之前为 `null`）；`unwrap(Class)` 从同一接口继承 |

示例（来自 `LazyResilience4jFacadeTest`，它只启用了 Bulkhead 模块）：

```java
@SpringJUnitConfig(LazyResilience4jFacadeTest.class)
@EnableBulkhead
class LazyResilience4jFacadeTest {

    private static final String TEST_ENTRY_NAME = "test";

    @Autowired
    private LazyResilience4jFacade lazyResilience4jFacade;

    @Test
    void testExecute() {
        LazyResilience4jFacade facade = this.lazyResilience4jFacade;
        facade.execute(TEST_ENTRY_NAME, () -> assertNotNull(facade));
        assertSame(facade, facade.execute(TEST_ENTRY_NAME, () -> facade));
    }

    @Test
    void testCall() throws Throwable {
        LazyResilience4jFacade facade = this.lazyResilience4jFacade;
        facade.call(TEST_ENTRY_NAME, () -> assertNotNull(facade));
        facade.call(TEST_ENTRY_NAME, () -> assertNotNull(facade), Exception.class);
        assertSame(facade, facade.call(TEST_ENTRY_NAME, () -> facade));
        assertSame(facade, facade.call(TEST_ENTRY_NAME, () -> facade, Exception.class));
    }

    @Test
    void testBegin() {
        Resilience4jContext<Resilience4jContext[]> context = this.lazyResilience4jFacade.begin(TEST_ENTRY_NAME);
        assertEquals(1, context.getEntry().length); // one enabled module -> one sub-context
    }

    @Test
    void testEnd() {
        Resilience4jContext<Resilience4jContext[]> context = this.lazyResilience4jFacade.begin(TEST_ENTRY_NAME);
        this.lazyResilience4jFacade.end(context);
        assertEquals(1, context.getEntry().length);
    }

    @Test
    void testGetDelegate() {
        assertEquals(this.lazyResilience4jFacade.unwrap(Resilience4jFacade.class),
                this.lazyResilience4jFacade.getDelegate());
    }
}
```

在应用代码中，按类型注入它（它是 primary 的 `Resilience4jFacade`）或按名称注入：

```java
@Service
class OrderService {

    @Autowired
    private Resilience4jFacade facade;   // -> the primary "lazyResilience4jFacade" bean

    BigDecimal price(String sku) {
        return facade.execute("pricing-service", () -> remoteClient.price(sku));
    }
}
```

如果你启用多个模块（`@EnableRetry` + `@EnableCircuitBreaker` + `@EnableRateLimiter`），同一个
`execute("name", …)` 调用会被它们全部包装，顺序由 commons 中模板优先级所定义——见
[microsphere-resilience4j-commons.md](microsphere-resilience4j-commons.md)。

---

## 9. 快速排障

| 症状 | 原因 / 解决办法 |
|---------|-------------|
| `ClassNotFoundException: io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration` | `resilience4j-spring` 是本模块的一个*可选*依赖——请自行添加它（以及 `resilience4j-framework-common`，再加上各运行时模块）。见 §2.2。 |
| 没有任何 `@EventListener` 被触发 | 你把 `publishEvents` 留在了默认值 `false`；或者条目是在发布者 bean 存在之前创建的（该事件桥在注册表**条目新增/替换**时才订阅）；又或者你用的是 `@EnableThreadPoolBulkhead`，它的事件开关什么都不注册（§3.2）。 |
| `EventConsumer<X>` bean 从不被调用 | `consumeEvents` 为 `false`；注解的入口类型与注册器不匹配（只有五个已发布的类型受支持）；或者 bean 的泛型类型并非恰好是 `EventConsumer<TheEventClass>`。 |
| `Duration` / 百分比属性未生效 | 缺少 `@EnableSpringConverterAdapter`（见 §4），或者前缀写错——前缀以结尾点号结束，例如 `microsphere.resilience4j.circuit-breaker.instances[test].…`。 |
| facade 抛出 `NullPointerException` | `LazyResilience4jFacade` 在 `ContextRefreshedEvent` 之前被使用了；请注入它，并在启动之后才运行的 bean 中使用。 |
| 插件未被激活 | `plugins` 的值是拿 `Resilience4jPlugin#getName()` 来比对的；请确认该插件在注册器所使用的 class loader 可见的 `META-INF/spring.factories` 中列在 `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` 键之下。 |

---

## 10. 参见

- [用户指南索引](README.md) · [commons 模块页](microsphere-resilience4j-commons.md)（模板、`Resilience4jFacade`、`Resilience4jModule`）
- [开发者指南 — microsphere-resilience4j-spring](../../developer-guide/zh/microsphere-resilience4j-spring.md)（注册器源码分析、事件桥设计、流程图）
- 插件页：[alibaba-druid](microsphere-resilience4j-alibaba-druid.md)、[mybatis](microsphere-resilience4j-mybatis.md)、[spring-web](microsphere-resilience4j-spring-web.md)、[openfeign](microsphere-resilience4j-openfeign.md)
