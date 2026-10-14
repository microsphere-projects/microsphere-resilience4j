# microsphere-resilience4j-spring — 用户指南（1.x）

> 源分支：`release-1.x` · 版本线：`0.1.x`（最新发布 `0.1.4`，分支 revision `0.1.5-SNAPSHOT`） · 目标：**Resilience4j 1.3.0 – 1.7.0**（默认 `1.7.0`） · Spring Cloud **Hoxton – 2021.0.x**
> 返回 [指南索引](README.md) · Resilience4j 2.x 的对应页面：[current（0.2.x）用户指南](../../../current/user-guide/zh/microsphere-resilience4j-spring.md) · 英文原文：[microsphere-resilience4j-spring — User Guide](../microsphere-resilience4j-spring.md)

`microsphere-resilience4j-spring` 是 1.x 维护版本线的 Spring 层。它把
`microsphere-resilience4j-commons` 中与框架无关的模板变成一套 Spring 使用体验，并且不要求使用 Spring Boot：

- 一族 `@Enable*` 注解 —— `@EnableCircuitBreaker`、`@EnableRateLimiter`、`@EnableRetry`、
  `@EnableBulkhead`、`@EnableThreadPoolBulkhead`、`@EnableTimeLimiter` —— 它们注册 Resilience4j 自己的
  Spring 配置类（进而是该模块的 `Registry` bean）；
- 自动把 `microsphere.resilience4j.<module>.*` 属性绑定到 Resilience4j 自己的
  `*ConfigurationProperties` 类型上；
- 一个**事件桥**，它把 Resilience4j 1.x 事件重新发布为 Spring 应用事件
  （`publishEvents = true`），并把你的 `EventConsumer` bean 订阅到 Resilience4j 1.x 的
  `EventPublisher` 上（`consumeEvents = true`）；
- 注册 Resilience4j 1.x 的*通用配置*（`FallbackConfiguration`、
  `SpelResolverConfiguration`），而 1.x 的入口配置自身并不会导入它们；
- 一个 `Resilience4jPlugin` 服务提供者接口（SPI），使框架插件（MyBatis、Druid、Spring Web……）可以把自身
  挂接到某个 `@Enable*` 注解上；
- `LazyResilience4jFacade`，一个 primary 的 `Resilience4jFacade` bean，在容器刷新之后由上下文中存在的
  `Registry` bean 组装而成。

本页的每一项内容都取自 `release-1.x` 分支下
`microsphere-resilience4j-spring/src/{main,test}/java` 的源码。当代码与其 Javadoc 说法不一致时，本页以代码为准，
并会标出这一差异。

---

## 1. 前置条件

| 要求 | 说明 |
|-------------|--------|
| `microsphere-resilience4j-commons` | 本模块的**非可选**编译依赖（相同的 `${revision}`）。提供 `Resilience4jFacade`、`ChainableResilience4jFacade`、`Resilience4jModule`、`Resilience4jConstants` 以及各模板。 |
| `microsphere-spring-context` | **非可选**编译依赖（版本由父工程链管理）。提供 `BeanCapableImportCandidate`、`BeanRegistrar`、`BeanDefinitionUtils`、`ResolvablePlaceholderAnnotationAttributes`、`SpringFactoriesLoaderUtils`、`@EnableConfigurationBeanBinding(s)`、`@EnableSpringConverterAdapter`。 |
| Resilience4j 构件 | 在本模块的 POM 中 `resilience4j-spring` 是 `<optional>true</optional>`，而在 commons 的 POM 中每一个 `io.github.resilience4j:*` 构件同样是可选的。**你必须自行声明所使用的那些 Resilience4j 构件** —— 见 §2.2。 |
| `spring-webmvc` | 声明为 `<optional>true</optional>`；只有 `…-spring-web` 插件需要它，注解本身并不需要。 |
| 一个 `@Configuration`（或任意 `@Target(TYPE)`）类 | 六个注解都是 `@Target(TYPE) @Retention(RUNTIME) @Inherited @Documented`。本模块的测试直接把注解标在测试类上，测试类同时充当配置（`@SpringJUnitConfig(classes = X.class)`）。 |
| 注册器是包级私有的 | `EnableCircuitBreakerRegistrar`、`EnableRateLimiterRegistrar`、`EnableRetryRegistrar`、`EnableBulkheadRegistrar`、`EnableThreadPoolBulkheadRegistrar`、`EnableTimeLimiterRegistrar` 都没有 `public` 修饰符 —— 请通过注解来驱动它们。 |

事件桥需要在 classpath 上具备 *1.x* 的事件 API
（`io.github.resilience4j.core.EventConsumer`、`io.github.resilience4j.core.EventPublisher`、
`io.github.resilience4j.core.registry.RegistryEventConsumer`），外加 `io.github.resilience4j.<module>.event`
下的各模块事件类型。在 Resilience4j `1.3.0` – `1.6.0` 上，其中一部分事件类型由本分支的兼容性垫片
`microsphere-resilience4j-compatible` 提供，它由 `resilience4j-1.3` … `resilience4j-1.6` Maven profile
自动挂载到 `…-commons` 上 —— 见 §10 与
[microsphere-resilience4j-compatible.md](microsphere-resilience4j-compatible.md)。

---

## 2. Maven 坐标与兼容性

### 2.1 坐标

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
        <artifactId>microsphere-resilience4j-spring</artifactId>
        <!-- version managed by the BOM -->
    </dependency>
</dependencies>
```

1.x 的 BOM（`microsphere-resilience4j-dependencies`）管理的就是下列这些构件，全部使用
`${revision}`：`…-commons`、`…-spring`、`…-alibaba-druid`、`…-openfeign`、`…-mybatis`、
`…-spring-web`。`microsphere-resilience4j-compatible` **不在** BOM 中（它由构建 profile 挂载，
而且并不发布部署 —— 它的 `maven-deploy-plugin` 是 `skip=true`）。

### 2.2 你必须自行添加的 Resilience4j 构件

这些注解直接引用了 Resilience4j 的类型
（`io.github.resilience4j.<module>.configure.<Module>Configuration` 和
`io.github.resilience4j.<module>.configure.<Module>ConfigurationProperties`），它们位于
`resilience4j-spring` 中；而注册表/条目类型位于各模块自己的构件中：

```xml
<dependencies>
    <!-- hosts CircuitBreakerConfiguration / CircuitBreakerConfigurationProperties, ... -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring</artifactId>
        <version>1.7.0</version>
    </dependency>
    <!-- the runtime module(s) you enable, e.g. -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-circuitbreaker</artifactId>
        <version>1.7.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-bulkhead</artifactId>
        <version>1.7.0</version>
    </dependency>
</dependencies>
```

### 2.3 版本与兼容性表（本版本线）

| 项目 | `release-1.x` 上的值 | 核实来源 |
|------|------------------------|---------------|
| 项目版本 | `0.1.x` 版本线：`<revision>0.1.5-SNAPSHOT</revision>`，最新发布 `0.1.4` | 根 `pom.xml`、`release-notes.md` |
| Resilience4j | 通过 Maven profile `resilience4j-1.3` … `resilience4j-1.7` 支持 `1.3.0`、`1.4.0`、`1.5.0`、`1.6.0`、`1.7.0`；**`resilience4j-1.7` 是 `activeByDefault` → `1.7.0`** | `microsphere-resilience4j-parent/pom.xml` |
| Spring Cloud / Spring Boot | 父工程 `microsphere-spring-cloud-parent` **`0.1.26`**，profile `spring-cloud-hoxton`（Boot `2.3.12.RELEASE` / Cloud `Hoxton.SR12`）、`spring-cloud-2020`（`2.4.13` / `2020.0.5`）、`spring-cloud-2021`（`2.6.15` / `2021.0.9`，`activeByDefault`） | 父 POM、`microsphere-resilience4j-parent/pom.xml` |
| JDK | CI 矩阵 `8, 11, 17, 21, 25` × 三个 Spring Cloud profile × 五个 Resilience4j profile | `.github/workflows/maven-build.yml` |
| 本模块自身 POM 的 profile | **没有** —— `microsphere-resilience4j-spring/pom.xml` 不声明任何 `<profiles>`，因此 `resilience4j.version` 始终来自父工程 | `microsphere-resilience4j-spring/pom.xml` |
| test 作用域依赖 | `junit-jupiter`、`spring-test`、`microsphere-spring-test`、`aspectjweaver`、`logback-classic` | `microsphere-resilience4j-spring/pom.xml` |

---

## 3. `@Enable*` 注解

### 3.1 元注解 `@EnableResilience4j`

`io.microsphere.resilience4j.spring.common.annotation.EnableResilience4j`
是 `@Target(ANNOTATION_TYPE)`，因此应用永远不会直接使用它：它只作为六个按模块划分的注解上的元注解存在，
其属性在这些注解上通过 `@AliasFor` 重新暴露。

| 属性 | 类型 | 默认值（编译产物） | 含义 |
|-----------|------|--------------------|---------|
| `publishEvents` | `boolean` | **`false`** | 注册本模块的 `Resilience4jEventApplicationEventPublisher` 实现 → Resilience4j 事件变为 Spring 应用事件 |
| `consumeEvents` | `boolean` | **`false`** | 注册本模块的 `Resilience4jEventConsumerBeanRegistrar` 实现 → 你的 `EventConsumer` bean 会被订阅到各条目的 1.x `EventPublisher` 上 |
| `plugins` | `String[]` | `{}`（`@Nonnull`） | 要激活的插件的 **`Resilience4jPlugin#getName()`** 名称 |

> **1.x 源码中的两处 Javadoc 陷阱**（之所以记录在这里，因为你阅读的正是这份源码）：
> 1. `publishEvents` / `consumeEvents` 的 Javadoc 在 `EnableResilience4j` **以及**全部六个按模块划分的注解中
>    都写着“默认 `true`”，但声明却是 `default false`。如果你不设置某个开关，对应的事件组件**不会**被注册。
> 2. `EnableResilience4j` 中的 `plugins()` 正确地说的是“`Resilience4jPlugin#getName()` 名称”，而
>    按模块划分的注解的 Javadoc 说的是“Spring Bean 名称”。注册器匹配的是插件**名称**
>    （`ArrayUtils.contains(plugins, resilience4jPlugin.getName())`）—— 不是 bean 名称。

### 3.2 六个公共注解各自装配了什么

每个按模块划分的注解都声明同样的三个属性，每个属性都带
`@AliasFor(annotation = EnableResilience4j.class, attribute = …)`，都被标注 `@EnableResilience4j`，
都 `@Import` 自己的注册器，并为自己的属性前缀加上 `@EnableConfigurationBeanBinding(s)`。
这些前缀常量来自 commons 的
`io.microsphere.resilience4j.common.Resilience4jConstants`（`PREFIX = "microsphere.resilience4j."`）。

| 注解（包 `io.microsphere.resilience4j.spring.*`） | 注册器（被 `@Import`） | 入口类型 `E` | 所注册的入口配置 `EC` | 由 `@EnableConfigurationBeanBinding(s)` 绑定的属性（1.x 类型） |
|---|---|---|---|---|
| `circuitbreaker.annotation.EnableCircuitBreaker` | `EnableCircuitBreakerRegistrar` | `CircuitBreaker` | `io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration` | `microsphere.resilience4j.circuit-breaker.` → `…circuitbreaker.configure.CircuitBreakerConfigurationProperties` |
| `ratelimiter.annotation.EnableRateLimiter` | `EnableRateLimiterRegistrar` | `RateLimiter` | `…ratelimiter.configure.RateLimiterConfiguration` | `microsphere.resilience4j.rate-limiter.` → `…ratelimiter.configure.RateLimiterConfigurationProperties` |
| `retry.annotation.EnableRetry` | `EnableRetryRegistrar` | `Retry` | `…retry.configure.RetryConfiguration` | `microsphere.resilience4j.retry.` → `…retry.configure.RetryConfigurationProperties` |
| `timelimiter.annotation.EnableTimeLimiter` | `EnableTimeLimiterRegistrar` | `TimeLimiter` | `…timelimiter.configure.TimeLimiterConfiguration` | `microsphere.resilience4j.time-limiter.` → `…timelimiter.configure.TimeLimiterConfigurationProperties` |
| `bulkhead.annotation.EnableBulkhead` | `EnableBulkheadRegistrar` | `Bulkhead` | `…bulkhead.configure.BulkheadConfiguration` | `microsphere.resilience4j.bulkhead.` → `…bulkhead.configure.BulkheadConfigurationProperties` **以及** `microsphere.resilience4j.thread-pool-bulkhead.` → `io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties` |
| `bulkhead.annotation.EnableThreadPoolBulkhead` | `EnableThreadPoolBulkheadRegistrar` | `ThreadPoolBulkhead` | `…bulkhead.configure.BulkheadConfiguration`（与 `@EnableBulkhead` 是同一个类；它的 Javadoc 提到的是 `ThreadPoolBulkheadConfiguration`，而代码传入的是 `BulkheadConfiguration`） | 与 `@EnableBulkhead` 相同的两个绑定 |

`publishEvents`、`consumeEvents` 和 `plugins` 在六个注解上的行为完全一致（默认值分别是 `false`、`false`、
`{}`）。

1.x 接线方式所特有的说明：

- **这些属性类型是 Resilience4j 1.x 的 `resilience4j-spring` 类型**
  （`…<module>.configure.<Module>ConfigurationProperties`），
  而不是 0.2.x 版本线上所使用的那些 framework-common 的 `Common<Module>ConfigurationProperties` 类型。
- `@EnableThreadPoolBulkhead` 注册的是 `BulkheadConfiguration`，而在 Resilience4j `1.7.0` 上这个类被标注为
  `@Configuration @Import({ThreadPoolBulkheadConfiguration.class, FallbackConfiguration.class,
  SpelResolverConfiguration.class})` —— 这正是本模块的 `EnableThreadPoolBulkheadTest` 仍然能够
  autowire `ThreadPoolBulkheadRegistry` 的原因。如果你锁定的是更旧的 Resilience4j 版本，请自行核实你所依赖的
  那个注册表 bean。
- `@EnableThreadPoolBulkhead` **不提供任何事件桥**：`META-INF/spring.factories` 只为
  `Bulkhead`、`CircuitBreaker`、`RateLimiter`、`Retry`、`TimeLimiter` 提供事件组件，而注册器只注册入口类型
  与注解的 `E` 相等的那个组件。因此
  `@EnableThreadPoolBulkhead` 上的 `publishEvents`/`consumeEvents` 目前什么都不会注册。`BulkheadEvent`
  仍然可以通过 `@EnableBulkhead(publishEvents = true, consumeEvents = true)` 获得 —— 这正是
  `EnableThreadPoolBulkheadTest` 的形态，只不过该测试把 `@EnableBulkhead` 保持在默认值，所以它的
  `@EventListener` 方法并不会被调用。
- 注册器通过
  `ResolvablePlaceholderAnnotationAttributes.of(annotationAttributes, annotationType, getEnvironment())`
  （来自 `microsphere-spring-context`）来解析注解属性，因此属性值中的 `${…}` 占位符会先针对 Spring
  `Environment` 解析，然后才用 `getBoolean("publishEvents")`、
  `getBoolean("consumeEvents")` 和 `getStringArray("plugins")` 读取。

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

每个 `@Enable*` 注解都会从 `microsphere.resilience4j.<module>.*` 绑定 Resilience4j 自己的
`*ConfigurationProperties` bean。下面这些取值正是本模块 1.x 测试所使用的：

```properties
# from EnableCircuitBreakerTest
microsphere.resilience4j.circuit-breaker.instances[test].waitDurationInOpenState=PT30S
microsphere.resilience4j.circuit-breaker.instances[test].slidingWindowSize=100
microsphere.resilience4j.circuit-breaker.instances[test].slowCallRateThreshold=0.7

# from EnableRateLimiterTest
microsphere.resilience4j.rate-limiter.instances[test].timeoutDuration=PT10S
microsphere.resilience4j.rate-limiter.instances[test].limitRefreshPeriod=PT0.000001S
microsphere.resilience4j.rate-limiter.instances[test].limitForPeriod=20

# from EnableRetryTest
microsphere.resilience4j.retry.instances[test].waitDuration=PT1S
microsphere.resilience4j.retry.instances[test].maxAttempts=1
microsphere.resilience4j.retry.instances[test].eventConsumerBufferSize=99

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

每个 `@Enable*` 测试也都带有 `@EnableSpringConverterAdapter`
（`io.microsphere.spring.core.convert.annotation`，来自 `microsphere-spring-context`）。当你像测试那样把
`PT30S`、`PT0.000001S` 或 `0.7` 这类值绑定到 1.x 的配置类型上时，请保留它。

---

## 5. 可运行示例（取自 1.x 测试）

### 5.1 CircuitBreaker：注册表 + 属性绑定 + Spring 事件

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

        // 1.x type: io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfigurationProperties
        CircuitBreakerConfigurationProperties.InstanceProperties instanceProperties =
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

归属说明：`CircuitBreakerRegistry` 来自注册器所注册的 `CircuitBreakerConfiguration` bean 定义
（因此 `resilience4j-spring` 必须出现在你的 classpath 上，见 §2.2）；
`CircuitBreakerConfigurationProperties` 由注解的 `@EnableConfigurationBeanBinding` 绑定；
而 `@EventListener` 之所以会触发，只是因为 `publishEvents = true`。

### 5.2 Retry

来自 `EnableRetryTest` —— 注意 1.x 的测试断言的是 `waitDuration` 和 `eventConsumerBufferSize`
（与 0.2.x 的测试不同，它并不断言 `maxAttempts`）：

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
        assertEquals(valueOf(99), instanceProperties.getEventConsumerBufferSize());

        retry.executeSupplier(() -> "Hello,World");
    }
}
```

### 5.3 RateLimiter 与 TimeLimiter

来自 `EnableRateLimiterTest`（1.x 版本只调用 `acquirePermission()` —— 没有显式的
`rateLimiter.onSuccess()`）与 `EnableTimeLimiterTest`：

```java
@SpringJUnitConfig(classes = EnableRateLimiterTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.rate-limiter.instances[test].timeoutDuration=PT10S",
        "microsphere.resilience4j.rate-limiter.instances[test].limitRefreshPeriod=PT0.000001S",
        "microsphere.resilience4j.rate-limiter.instances[test].limitForPeriod=20"})
@EnableRateLimiter(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
class EnableRateLimiterTest {

    @Autowired
    private RateLimiterRegistry registry;

    @Autowired
    private RateLimiterConfigurationProperties properties;

    @Test
    void test() {
        RateLimiter rateLimiter = registry.rateLimiter("test");
        rateLimiter.acquirePermission();

        RateLimiterConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(ofSeconds(10), instanceProperties.getTimeoutDuration());
        assertEquals(valueOf(20), instanceProperties.getLimitForPeriod());
        assertEquals(ofNanos(1000), instanceProperties.getLimitRefreshPeriod());
    }

    @EventListener(RateLimiterOnSuccessEvent.class)
    void onRateLimiterOnSuccessEvent(RateLimiterOnSuccessEvent event) {
        assertEquals("test", event.getRateLimiterName());
        assertEquals(SUCCESSFUL_ACQUIRE, event.getEventType());
    }
}
```

```java
@SpringJUnitConfig(classes = EnableTimeLimiterTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.time-limiter.instances[test].timeoutDuration=PT10S",
        "microsphere.resilience4j.time-limiter.instances[test].cancelRunningFuture=true",
        "microsphere.resilience4j.time-limiter.instances[test].eventConsumerBufferSize=200"})
@EnableTimeLimiter(publishEvents = true, consumeEvents = true)
@EnableSpringConverterAdapter
class EnableTimeLimiterTest {

    @Autowired
    private TimeLimiterRegistry registry;

    @Autowired
    private TimeLimiterConfigurationProperties properties;

    @Test
    void test() {
        TimeLimiter timeLimiter = registry.timeLimiter("test");

        TimeLimiterConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
        assertEquals(ofSeconds(10), instanceProperties.getTimeoutDuration());
        assertEquals(TRUE, instanceProperties.getCancelRunningFuture());
        assertEquals(valueOf(200), instanceProperties.getEventConsumerBufferSize());

        timeLimiter.onSuccess();   // -> TimeLimiterOnSuccessEvent -> @EventListener below
    }

    @EventListener(TimeLimiterOnSuccessEvent.class)
    void onTimeLimiterOnSuccessEvent(TimeLimiterOnSuccessEvent event) {
        assertEquals("test", event.getTimeLimiterName());
    }
}
```

### 5.4 Bulkhead，包含注册表的 replace/remove 生命周期以及一个插件

来自 `EnableBulkheadTest` —— `plugins = NAME`，其中 `NAME` 是
`io.microsphere.resilience4j.spring.LoggingResilience4jPlugin.NAME`（即 `"logging"`）：

```java
@SpringJUnitConfig(classes = EnableBulkheadTest.class)
@TestPropertySource(properties = {
        "microsphere.resilience4j.bulkhead.instances[test].maxConcurrentCalls=10",
        "microsphere.resilience4j.bulkhead.instances[test].eventConsumerBufferSize=100",
        "microsphere.resilience4j.bulkhead.instances[test].maxWaitDuration=PT30S"})
@EnableBulkhead(publishEvents = true, consumeEvents = true, plugins = NAME)
@EnableSpringConverterAdapter
class EnableBulkheadTest {

    @Autowired
    private BulkheadRegistry registry;

    @Autowired
    private BulkheadConfigurationProperties properties;

    @Test
    void test() {
        String entryName = "test";
        Bulkhead circuitBreaker = registry.bulkhead(entryName);
        circuitBreaker.acquirePermission();

        BulkheadConfigurationProperties.InstanceProperties instanceProperties = properties.getInstances().get("test");
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

来自 `EnableThreadPoolBulkheadTest` —— 两个 bulkhead 注解都被加上了，并且线程池的属性类型是 1.x 的
`io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties`：

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

    @Autowired
    private ThreadPoolBulkheadRegistry registry;

    @Autowired
    private ThreadPoolBulkheadConfigurationProperties properties;

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

上面的 `@EventListener` 逐字复制自该测试，但在这些注解之下它永远不会被调用：
`@EnableThreadPoolBulkhead` 的事件开关什么都不注册（§3.2），而 `@EnableBulkhead` 把它的
`publishEvents` 保持在默认值 `false`。

---

## 6. 消费 Resilience4j 事件

这座事件桥提供两个相互独立的方向，二者都由注解上的开关驱动。

```mermaid
flowchart LR
    ENTRY["Resilience4j 1.x 条目<br/>例如 CircuitBreaker 'test'"] -- "CircuitBreakerOnSuccessEvent" --> PUB["CircuitBreakerApplicationEventPublisher<br/>（publishEvents = true）"]
    PUB -- "ApplicationEventPublisher.publishEvent(event)" --> SPRING["Spring 上下文"]
    SPRING --> LISTENER["@EventListener 方法"]
    CONSUMER["你的上下文中的<br/>EventConsumer bean"] -. "consumeEvents = true：<br/>CircuitBreakerEventConsumerBeanRegistrar<br/>把它们订阅到条目的 EventPublisher 上" .-> ENTRY
```

### 6.1 把 Resilience4j 事件作为 Spring 应用事件（`publishEvents = true`）

| 注解 | 发布者 bean 类（`io.microsphere.resilience4j.spring.*`） | 重新发布的事件类型 |
|------------|---------------------------------------------------------------|--------------------------|
| `@EnableCircuitBreaker` | `circuitbreaker.event.CircuitBreakerApplicationEventPublisher` | `CircuitBreakerEvent` |
| `@EnableRateLimiter` | `ratelimiter.event.RateLimiterApplicationEventPublisher` | `RateLimiterEvent` |
| `@EnableRetry` | `retry.event.RetryApplicationEventPublisher` | `RetryEvent` |
| `@EnableTimeLimiter` | `timelimiter.event.TimeLimiterApplicationEventPublisher` | `TimeLimiterEvent` |
| `@EnableBulkhead` | `bulkhead.event.BulkheadApplicationEventPublisher` | `BulkheadEvent` |
| `@EnableThreadPoolBulkhead` | *无*（见 §3.2） | — |

`common.event.Resilience4jEventApplicationEventPublisher<E, ET>` 的语义（源码）：

- 它实现 `EventConsumer<ET>`、`RegistryEventConsumer<E>` 和 `ApplicationEventPublisherAware`；
- 作为一个 `RegistryEventConsumer`，当本模块的 `Registry` **新增**或**替换**一个条目时它会收到通知；针对那个
  条目它通过反射调用 `getEventPublisher()`，并通过 `EventPublisher#onEvent(...)` 订阅
  `this::consumeEvent`，也就是该条目的*全部*事件；
- `consumeEvent(ET event)` 调用 `applicationEventPublisher.publishEvent(event)` —— 发布的就是 Resilience4j 事件
  对象本身（Spring 会把它包装进一个 `PayloadApplicationEvent`），因此你可以像 §5.1 那样完全按事件类来监听；
- `onEntryRemovedEvent(EntryRemovedEvent<E>)` 是一个空方法，因此从注册表中被移除的条目仍保持订阅（无害）；
- 每个被重新发布的事件都会输出 trace 日志：`"The Resilience4j Event : {} was published on the Spring
  ApplicationListener(s)"`。

由于这座事件桥是在**条目新增/替换**时才订阅，因此在发布者 bean 存在之前就创建好的条目不会被传导；如果看起来
丢失了事件，请在启动之后再创建条目（或者重新创建/替换它们）。

### 6.2 带类型的 `EventConsumer` bean（`consumeEvents = true`）

`common.event.Resilience4jEventConsumerBeanRegistrar<E>`（一个 `RegistryEventConsumer<E>` +
`BeanFactoryAware`）并不发布 Spring 事件。对于每个新增/替换的条目，它会：

1. 通过反射取得 `entry.getEventPublisher()`（例如 `CircuitBreaker.EventPublisher`）；
2. 遍历该发布者类的方法，只保留 `common.event.EventConsumerMethodFilter` 认可的那些：
   **恰好一个参数，且其类型为 `io.github.resilience4j.core.EventConsumer`**；
3. 用 `ResolvableType.forMethodParameter(method, 0)` 解析其泛型参数类型（例如
   `EventConsumer<CircuitBreakerOnSuccessEvent>`），并通过
   `beanFactory.getBeanProvider(type)` 找出*所有*匹配的 bean；
4. 用每个找到的 bean 调用该发布者方法，也就是把它订阅上。

因此，消费者就是任何声明为 `EventConsumer<SomeResilience4jEvent>` 的 bean。来自
`CircuitBreakerEventConsumerBeanRegistrarTest`：

```java
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        CircuitBreakerConfigurationProperties.class,
        CircuitBreakerConfiguration.class,
        CircuitBreakerEventConsumerBeanRegistrar.class,   // the bean @EnableCircuitBreaker(consumeEvents = true) would register
        CircuitBreakerEventConsumerBeanRegistrarTest.class
}, initializers = {
        CommonConfigurationsApplicationContextInitializer.class   // 1.x only: registers Fallback/SpelResolver configurations
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
            assertEquals(ofMillis(100), event.getElapsedDuration());
        };
    }
}
```

在应用代码中，通过注解即可获得同样的效果：

```java
@Configuration
@EnableCircuitBreaker(consumeEvents = true)
class ConsumerConfig {

    private static final Logger logger = Logger.getLogger(ConsumerConfig.class.getName());

    @Bean
    EventConsumer<CircuitBreakerOnSuccessEvent> onSuccessConsumer() {
        // CircuitBreakerOnSuccessEvent#getCircuitBreakerName() / #getElapsedDuration() are 1.x APIs
        return event -> logger.info(() -> "success on '" + event.getCircuitBreakerName()
                + "' in " + event.getElapsedDuration());
    }
}
```

（这里展示 `java.util.logging.Logger` 是为了兼容 JDK 8；库自身是通过
`io.microsphere.logging.Logger` 记录日志的。）

两个方向可以自由组合（`publishEvents = true, consumeEvents = true`），正如所有 `@Enable*` 测试所做的那样。

`EventConsumerMethodFilter` 在实践中就是公共 API：它的 `INSTANCE` 常量与
`matches(Method)` 规则由 `EventConsumerMethodFilterTest` 加以验证，该测试断言在 Resilience4j 1.x 的
classpath 上，`io.github.resilience4j.core.EventPublisher` 以及 1.x 的那些嵌套发布者
`Retry.EventPublisher`、`CircuitBreaker.EventPublisher`、`RateLimiter.EventPublisher`、
`TimeLimiter.EventPublisher`、`Bulkhead.EventPublisher` 中*每一个*本地声明的方法都能通过过滤器。

---

## 7. 实现并激活一个 `Resilience4jPlugin`

`io.microsphere.resilience4j.spring.common.Resilience4jPlugin` 是本项目的各个插件所使用的扩展点：

```java
public interface Resilience4jPlugin {

    void plugin(ConfigurableListableBeanFactory beanFactory,
                BeanDefinitionRegistry registry,
                ConfigurableEnvironment environment);

    String getName();
}
```

由 `EnableResilience4jRegistrar` 可直接得出三条规则：

1. 插件从 `META-INF/spring.factories` 中、在
   `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` 这个键下加载，并在容器**之外**实例化
   （`SpringFactoriesLoaderUtils.loadFactories(beanFactory, Resilience4jPlugin.class)`）—— 请保留一个
   public 无参构造器；
2. 按名称激活：`plugins = {"…"}` 会用
   `io.microsphere.util.ArrayUtils.contains(plugins, resilience4jPlugin.getName())` 来匹配；
3. `plugin(...)` 是在 `@Enable*` 注册器仍在注册 bean 定义时运行的，所以它的职责是注册 bean 定义
   （或以其他方式操作 `BeanDefinitionRegistry`），而不是假定 bean 已经存在。

### 7.1 参考实现：`LoggingResilience4jPlugin`（test 作用域）

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

`NoOpResilience4jPlugin`（名称 `"no-operation"`，包级私有类）展示了退化情形 —— 一个
空的 `plugin(...)` 方法体 —— 它会被加载，但从不被激活，因为没有任何注解列出它。
`LoggingResilience4jPlugin` 由 `EnableBulkheadTest` 通过 `plugins = NAME` 激活。

### 7.2 本分支自带的插件名称

| 插件类 | `getName()` | 模块 |
|--------------|-------------|--------|
| `io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin` | `"alibaba-druid"`（字面返回；该类同样把 `plugin(...)` 实现为空操作） | `microsphere-resilience4j-alibaba-druid` |
| `io.microsphere.resilience4j.mybatis.MyBatisResilience4jPlugin` | `PLUGIN_NAME = "mybatis"` | `microsphere-resilience4j-mybatis` |
| `io.microsphere.resilience4j.spring.web.SpringWebResilience4jPlugin` | `PLUGIN_NAME = "spring-web"` | `microsphere-resilience4j-spring-web` |

```java
@Configuration
@EnableBulkhead(publishEvents = true, plugins = {"mybatis", "spring-web"})
class ProductionConfig {
}
```

---

## 8. `LazyResilience4jFacade`

`io.microsphere.resilience4j.spring.LazyResilience4jFacade` 是统一编程模型在 Spring 侧的入口。它实现
`Resilience4jFacade`（commons）、`ApplicationListener<ContextRefreshedEvent>` 和
`io.microsphere.lang.DelegatingWrapper`，并把职责委托给由你的上下文中那些 `Registry` bean 构建出的
`ChainableResilience4jFacade`。

- 由**每一个** `@Enable*` 注解注册，bean 名称固定为
  `LazyResilience4jFacade.BEAN_NAME` = `"lazyResilience4jFacade"`，并且是一个 **primary** 的泛型 bean
  定义（`beanDefinition.setPrimary(true)`）；其 `modules` 属性是一个可变的
  `Set<Resilience4jModule>`，无论你应用了多少个注解，它都为每个被启用的注解累积一个模块。
- 在 `ContextRefreshedEvent` 上，它对每个累积到的模块调用 `context.getBean(module.getRegistryClass())`
  并构造委托对象。**请在容器刷新之后再使用它** —— 在那之前委托对象是 `null`，任何操作都会抛出
  `NullPointerException`。
- `setModules(Set<Resilience4jModule>)` / `getModules()` 是注册器通过 bean 定义写入的 JavaBean 存取方法。
- 作为一个 `DelegatingWrapper`，`getDelegate()` 返回当前的委托对象，继承来的 `unwrap(Class)`
  则以带类型的方式暴露它。

| 方法（如 1.x 源码中所声明） | 说明 |
|---|---|
| `void execute(String name, Runnable callback)` · `<T> T execute(String name, Supplier<T> callback)` | 对受检异常友好的入口 |
| `void call(String name, ThrowableAction)` · `<TR extends Throwable> void call(String name, ThrowableAction, Class<TR>)` · `<T> T call(String name, ThrowableSupplier<T>)` · `<T, TR extends Throwable> T call(String name, ThrowableSupplier<T>, Class<TR>)` | 可能抛出 `Throwable` / 所声明的 `TR` |
| `Resilience4jContext<Resilience4jContext[]> begin(String name)` · `void end(Resilience4jContext<Resilience4jContext[]>)` | 手动装饰；`getEntry()` 是按模块划分的子上下文数组 |
| `boolean isBeginSupported()` · `boolean isEndSupported()` | 委托调用 |
| `void onApplicationEvent(ContextRefreshedEvent event)` | 构造委托对象 |
| `Object getDelegate()` | 在首次 `ContextRefreshedEvent` 之前为 `null` |

来自 `LazyResilience4jFacadeTest` 的示例（它只启用了 Bulkhead 模块）：

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
    void testIsBeginSupported() {
        assertTrue(this.lazyResilience4jFacade.isBeginSupported());
    }

    @Test
    void testBegin() {
        Resilience4jContext<Resilience4jContext[]> context = this.lazyResilience4jFacade.begin(TEST_ENTRY_NAME);
        assertEquals(1, context.getEntry().length);   // one enabled module -> one sub-context
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

当你启用多个模块时（`@EnableRetry` + `@EnableCircuitBreaker` + `@EnableRateLimiter`），同一个
`execute("name", …)` 调用会被它们全部包装，顺序由 commons 中模板的优先级决定 —— 见
[microsphere-resilience4j-commons.md](microsphere-resilience4j-commons.md)。

---

## 9. 快速排障

| 症状 | 原因 / 解决办法 |
|---------|-------------|
| `NoClassDefFoundError: io/github/resilience4j/<module>/configure/<Module>Configuration` | `resilience4j-spring` 在这里是*可选*的 —— 请自行添加它，外加各运行时模块以及 `resilience4j-framework-common`（§2.2）。 |
| 没有任何 `@EventListener` 被触发 | 你把 `publishEvents` 留在了默认值 `false`（声称是 `true` 的那份 Javadoc 是错的）；或者条目是在发布者存在之前创建的（事件桥在注册表**条目新增/替换**时才订阅）；又或者你用的是 `@EnableThreadPoolBulkhead`，它的事件开关什么都不注册（§3.2）。 |
| `EventConsumer<X>` bean 从不被调用 | `consumeEvents = false`；注解的入口类型与注册器的不匹配；或者 bean 的声明类型并非恰好是 `EventConsumer<TheEventClass>`（裸类型 `EventConsumer` 或 `Consumer<…>` 都不匹配 `ResolvableType.forMethodParameter`）。 |
| 在 Resilience4j `1.3` – `1.6` 上，诸如 `CircuitBreakerOnFailureRateExceededEvent` / `RateLimiterOnDrainedEvent` 这类事件类型抛出 `ClassNotFoundException` | 这些类型由 `microsphere-resilience4j-compatible` 提供。当你用与之匹配的 `resilience4j-1.x` profile 构建*本*版本线时，它会被自动挂载；在你自己的应用中，请添加包含这些类型的某个 Resilience4j 版本构件（例如 `1.7.0`），或者自行携带缺失的那些事件类。 |
| `Duration` / 百分比属性未生效 | 缺少 `@EnableSpringConverterAdapter`，或者前缀写错了（每个前缀都以结尾点号结束，例如 `microsphere.resilience4j.circuit-breaker.instances[test].…`）。 |
| facade 抛出 `NullPointerException` | `LazyResilience4jFacade` 在 `ContextRefreshedEvent` 之前被使用了。 |
| 插件未被激活 | `plugins` 的值是拿 `Resilience4jPlugin#getName()` 来比对的（不是 bean 名称），并且该插件必须列在注册器所用 class loader 可见的某个 `META-INF/spring.factories` 中的 `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` 键之下。 |
| 使用 `@EnableThreadPoolBulkhead` 时 `ThreadPoolBulkheadRegistry` 缺失 | 注册器注册的是 `BulkheadConfiguration`；在 Resilience4j `1.7.0` 上该类传递性地导入了 `ThreadPoolBulkheadConfiguration`。在更旧的 1.x 版本上，请核实那个注册表 bean，或者自行声明线程池配置。 |

---

## 10. 与 0.2.x 版本线的差异

两条版本线承载的是*同一个*编程模型，并且本模块的大部分源码在 `release-1.x` 与 `release` 之间是逐字节相同的。
把本页面与姊妹页面
[../../../current/user-guide/zh/microsphere-resilience4j-spring.md](../../../current/user-guide/zh/microsphere-resilience4j-spring.md)
放在一起阅读时，可使用下表。

| 方面 | 1.x（`release-1.x`，`0.1.x`） | 0.2.x（`release`，current） |
|--------|------------------------------|----------------------------|
| Resilience4j | `1.3.0` – `1.7.0`，默认 `1.7.0`（profile `resilience4j-1.3` … `resilience4j-1.7`） | `2.0.0` – `2.4.0`，默认 `2.4.0` |
| Spring 技术栈 | 父工程 `microsphere-spring-cloud-parent` `0.1.26`；Spring Cloud `Hoxton` – `2021.0.x`（Boot `2.3.12.RELEASE` / `2.4.13` / `2.6.15`） | 父工程 `0.2.26`；Spring Cloud `2022.0.x` – `2025.0.x` |
| 兼容性垫片 | 额外的模块 **`microsphere-resilience4j-compatible`**，由 `resilience4j-1.3` … `resilience4j-1.6` profile 挂载到 `…-commons` 上。它声明了 `io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent`、`CircuitBreakerOnFailureRateExceededEvent`、`CircuitBreakerOnSlowCallRateExceededEvent`、`io.github.resilience4j.ratelimiter.event.RateLimiterEvent`、`RateLimiterOnDrainedEvent`。因此 `CircuitBreakerApplicationEventPublisher` 对 `…circuitbreaker.event.CircuitBreakerEvent` 的导入有可能解析到**垫片**而不是 `resilience4j-circuitbreaker`。 | 不存在 —— Resilience4j 2.x 自带这些类型 |
| 通用配置 | `EnableResilience4jRegistrar` 调用 `registerCommonConfigurations(registry)`（一个 **1.x 独有**的辅助类 `io.microsphere.resilience4j.spring.common.Resilience4jUtils`），它注册 `io.github.resilience4j.fallback.configure.FallbackConfiguration`，并以反射方式注册 `io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration`（“自 Resilience4j 1.5 起”，仅当它在 classpath 上存在时才注册）。1.x 的 Javadoc 解释了原因：只有 *Resilience4j Spring 2.0+* 的入口配置才会默认导入它们。 | 那个调用和那个类都不存在 —— 2.x 的入口配置已经同时导入了两者 |
| 线程池 bulkhead 的属性类型 | `@EnableBulkhead` / `@EnableThreadPoolBulkhead` 绑定 `io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties` | 绑定 `…common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties` |
| 测试支持 | 1.x 的测试使用一个 `CommonConfigurationsApplicationContextInitializer`（`io.microsphere.resilience4j.spring.context`，test 作用域），使得那些手工接线的事件测试同样能获得 `Fallback`/`SpelResolver` 配置；测试通过模块自身的 `…<module>.configure.<Module>ConfigurationProperties.InstanceProperties` 读取实例属性 | 那个 initializer 类不存在；测试通过 `Common<Module>ConfigurationProperties` 类型读取属性 |
| 本模块的 POM | 没有 `<profiles>` 段 → `resilience4j.version` 来自父工程（默认 `1.7.0`） | 重新声明了 profile `resilience4j-2.4` 并设为 `activeByDefault`，但把值写成 `2.3.0`，因此单独构建本模块时解析出的 Resilience4j 是 `2.3.0` |
| 事件桥源码 | `Resilience4jEventApplicationEventPublisher`、`Resilience4jEventConsumerBeanRegistrar`、`EventConsumerMethodFilter`、全部六个发布者/注册器、`LazyResilience4jFacade`、`Resilience4jPlugin`、`@EnableResilience4j` 以及各按模块划分的注解/注册器，与 0.2.x 的文件**逐字节相同**；只是这些 import 所解析到的类型不同（1.x 的 `EventPublisher`/`EventConsumer` 与 `.event` 包） | 同样的源码，Resilience4j 2.x 的类型 |
| `@Enable*` 属性默认值 | 完全相同：`publishEvents = false`、`consumeEvents = false`、`plugins = {}`，并带有同样的“默认 `true`”Javadoc 不符 | 完全相同 |

因此，把一个应用从 `0.2.x` 向下移植到 `0.1.x` 意味着：更换 BOM 版本，保持注解和事件代码不变，添加 Resilience4j 1.x
的构件，并且要注意属性现在绑定到 1.x 的 `…configure.*ConfigurationProperties` /
`io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties` 类型上。

---

## 11. 参见

- [1.x 用户指南索引](README.md) · [commons 模块页](microsphere-resilience4j-commons.md)（模板、`Resilience4jFacade`、`Resilience4jModule`）
- [兼容性垫片模块页](microsphere-resilience4j-compatible.md)
- [1.x 开发者指南 — microsphere-resilience4j-spring](../../developer-guide/zh/microsphere-resilience4j-spring.md)（注册器源码分析、事件桥设计、引导/插件流程图）
- 当前版本线的对应页面：[0.2.x 用户指南](../../../current/user-guide/zh/microsphere-resilience4j-spring.md)
- 插件页：[alibaba-druid](microsphere-resilience4j-alibaba-druid.md)、[mybatis](microsphere-resilience4j-mybatis.md)、[spring-web](microsphere-resilience4j-spring-web.md)、[openfeign](microsphere-resilience4j-openfeign.md)
