# microsphere-resilience4j-spring — User Guide (1.x)

> Source branch: `release-1.x` · Version line: `0.1.x` (latest release `0.1.4`, branch revision `0.1.5-SNAPSHOT`) · Target: **Resilience4j 1.3.0 – 1.7.0** (default `1.7.0`) · Spring Cloud **Hoxton – 2021.0.x**
> Back to [guide index](README.md) · Comparable page for Resilience4j 2.x: [current (0.2.x) user guide](../../current/user-guide/microsphere-resilience4j-spring.md)

`microsphere-resilience4j-spring` is the Spring layer of the 1.x maintenance line. It turns the
framework-neutral templates of `microsphere-resilience4j-commons` into a Spring experience, without
requiring Spring Boot:

- a family of `@Enable*` annotations — `@EnableCircuitBreaker`, `@EnableRateLimiter`, `@EnableRetry`,
  `@EnableBulkhead`, `@EnableThreadPoolBulkhead`, `@EnableTimeLimiter` — that register Resilience4j's own
  Spring configuration classes (and therefore the module's `Registry` beans);
- automatic binding of `microsphere.resilience4j.<module>.*` properties onto Resilience4j's own
  `*ConfigurationProperties` types;
- an **event bridge** that re-publishes Resilience4j 1.x events as Spring application events
  (`publishEvents = true`) and subscribes your `EventConsumer` beans onto Resilience4j's 1.x
  `EventPublisher`s (`consumeEvents = true`);
- registration of the Resilience4j 1.x *common configurations* (`FallbackConfiguration`,
  `SpelResolverConfiguration`) that the 1.x entry configurations do not import themselves;
- a `Resilience4jPlugin` SPI so the framework plugins (MyBatis, Druid, Spring Web, …) can attach
  themselves to an `@Enable*` annotation;
- `LazyResilience4jFacade`, a primary `Resilience4jFacade` bean assembled from the `Registry` beans
  present in the context once the context is refreshed.

Everything on this page is read from the `release-1.x` sources under
`microsphere-resilience4j-spring/src/{main,test}/java`. Where the code and its Javadoc disagree, this
page documents the code and flags the difference.

---

## 1. Prerequisites

| Requirement | Detail |
|-------------|--------|
| `microsphere-resilience4j-commons` | **Non-optional** compile dependency of this module (same `${revision}`). Supplies `Resilience4jFacade`, `ChainableResilience4jFacade`, `Resilience4jModule`, `Resilience4jConstants`, the templates. |
| `microsphere-spring-context` | **Non-optional** compile dependency (version managed by the parent chain). Supplies `BeanCapableImportCandidate`, `BeanRegistrar`, `BeanDefinitionUtils`, `ResolvablePlaceholderAnnotationAttributes`, `SpringFactoriesLoaderUtils`, `@EnableConfigurationBeanBinding(s)`, `@EnableSpringConverterAdapter`. |
| Resilience4j artifacts | In this module's POM `resilience4j-spring` is `<optional>true</optional>`, and in the commons POM every `io.github.resilience4j:*` artifact is optional too. **You must declare the Resilience4j artifacts you use** — see §2.2. |
| `spring-webmvc` | Declared `<optional>true</optional>`; only needed by the `…-spring-web` plugin, not by the annotations themselves. |
| A `@Configuration` (or any `@Target(TYPE)`) class | All six annotations are `@Target(TYPE) @Retention(RUNTIME) @Inherited @Documented`. The module's tests apply them directly on the test class, which doubles as the configuration (`@SpringJUnitConfig(classes = X.class)`). |
| Registrars are package-private | `EnableCircuitBreakerRegistrar`, `EnableRateLimiterRegistrar`, `EnableRetryRegistrar`, `EnableBulkheadRegistrar`, `EnableThreadPoolBulkheadRegistrar`, `EnableTimeLimiterRegistrar` have no `public` modifier — drive them through the annotations. |

The event bridge needs the *1.x* event API on the classpath
(`io.github.resilience4j.core.EventConsumer`, `io.github.resilience4j.core.EventPublisher`,
`io.github.resilience4j.core.registry.RegistryEventConsumer`) plus the module event types under
`io.github.resilience4j.<module>.event`. On Resilience4j `1.3.0` – `1.6.0` some of those event types are
supplied by the branch's compatibility shim `microsphere-resilience4j-compatible`, which is attached
automatically to `…-commons` by the `resilience4j-1.3` … `resilience4j-1.6` Maven profiles — see §10 and
[microsphere-resilience4j-compatible.md](microsphere-resilience4j-compatible.md).

---

## 2. Maven coordinates and compatibility

### 2.1 Coordinates

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

The 1.x BOM (`microsphere-resilience4j-dependencies`) manages exactly these artifacts, all at
`${revision}`: `…-commons`, `…-spring`, `…-alibaba-druid`, `…-openfeign`, `…-mybatis`,
`…-spring-web`. `microsphere-resilience4j-compatible` is **not** in the BOM (it is attached by the
build profiles, and it is not deployed — its `maven-deploy-plugin` is `skip=true`).

### 2.2 Resilience4j artifacts you must add yourself

The annotations reference Resilience4j types directly
(`io.github.resilience4j.<module>.configure.<Module>Configuration` and
`io.github.resilience4j.<module>.configure.<Module>ConfigurationProperties`), which live in
`resilience4j-spring`; the registry/entry types live in the per-module artifacts:

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

### 2.3 Version and compatibility table (this line)

| Item | Value on `release-1.x` | Verified from |
|------|------------------------|---------------|
| Project version | `0.1.x` line: `<revision>0.1.5-SNAPSHOT</revision>`, latest release `0.1.4` | root `pom.xml`, `release-notes.md` |
| Resilience4j | `1.3.0`, `1.4.0`, `1.5.0`, `1.6.0`, `1.7.0` via Maven profiles `resilience4j-1.3` … `resilience4j-1.7`; **`resilience4j-1.7` is `activeByDefault` → `1.7.0`** | `microsphere-resilience4j-parent/pom.xml` |
| Spring Cloud / Spring Boot | parent `microsphere-spring-cloud-parent` **`0.1.26`**, profiles `spring-cloud-hoxton` (Boot `2.3.12.RELEASE` / Cloud `Hoxton.SR12`), `spring-cloud-2020` (`2.4.13` / `2020.0.5`), `spring-cloud-2021` (`2.6.15` / `2021.0.9`, `activeByDefault`) | parent POM, `microsphere-resilience4j-parent/pom.xml` |
| JDK | CI matrix `8, 11, 17, 21, 25` × the three Spring Cloud profiles × the five Resilience4j profiles | `.github/workflows/maven-build.yml` |
| This module's own POM profiles | **none** — `microsphere-resilience4j-spring/pom.xml` declares no `<profiles>`, so `resilience4j.version` always comes from the parent | `microsphere-resilience4j-spring/pom.xml` |
| Test-scope dependencies | `junit-jupiter`, `spring-test`, `microsphere-spring-test`, `aspectjweaver`, `logback-classic` | `microsphere-resilience4j-spring/pom.xml` |

---

## 3. The `@Enable*` annotations

### 3.1 The meta-annotation `@EnableResilience4j`

`io.microsphere.resilience4j.spring.common.annotation.EnableResilience4j` is
`@Target(ANNOTATION_TYPE)`, so applications never use it directly: it only exists as a meta-annotation on
the six per-module annotations, which re-expose its attributes with `@AliasFor`.

| Attribute | Type | Default (compiled) | Meaning |
|-----------|------|--------------------|---------|
| `publishEvents` | `boolean` | **`false`** | registers the module's `Resilience4jEventApplicationEventPublisher` implementation → Resilience4j events become Spring application events |
| `consumeEvents` | `boolean` | **`false`** | registers the module's `Resilience4jEventConsumerBeanRegistrar` implementation → your `EventConsumer` beans are subscribed onto the entries' 1.x `EventPublisher`s |
| `plugins` | `String[]` | `{}` (`@Nonnull`) | the **`Resilience4jPlugin#getName()`** names of the plugins to activate |

> **Two Javadoc traps in the 1.x sources** (documented here because they are the source you read):
> 1. `publishEvents` / `consumeEvents` Javadoc says "`true` as default" in `EnableResilience4j` **and** in all
>    six per-module annotations, but the declarations are `default false`. If you do not set a flag, the
>    corresponding event component is **not** registered.
> 2. `plugins()` in `EnableResilience4j` correctly says "the `Resilience4jPlugin#getName()` names", while the
>    per-module annotations' Javadoc says "Spring Bean names". The registrar matches plugin **names**
>    (`ArrayUtils.contains(plugins, resilience4jPlugin.getName())`) — not bean names.

### 3.2 The six public annotations and what each one wires

Every per-module annotation declares the same three attributes, each with
`@AliasFor(annotation = EnableResilience4j.class, attribute = …)`, is annotated `@EnableResilience4j`,
`@Import`s its own registrar, and adds `@EnableConfigurationBeanBinding(s)` for its properties prefix.
The prefix constants come from commons'
`io.microsphere.resilience4j.common.Resilience4jConstants` (`PREFIX = "microsphere.resilience4j."`).

| Annotation (package `io.microsphere.resilience4j.spring.*`) | Registrar (`@Import`ed) | Entry type `E` | Entry configuration registered `EC` | Properties bound by `@EnableConfigurationBeanBinding(s)` (1.x types) |
|---|---|---|---|---|
| `circuitbreaker.annotation.EnableCircuitBreaker` | `EnableCircuitBreakerRegistrar` | `CircuitBreaker` | `io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration` | `microsphere.resilience4j.circuit-breaker.` → `…circuitbreaker.configure.CircuitBreakerConfigurationProperties` |
| `ratelimiter.annotation.EnableRateLimiter` | `EnableRateLimiterRegistrar` | `RateLimiter` | `…ratelimiter.configure.RateLimiterConfiguration` | `microsphere.resilience4j.rate-limiter.` → `…ratelimiter.configure.RateLimiterConfigurationProperties` |
| `retry.annotation.EnableRetry` | `EnableRetryRegistrar` | `Retry` | `…retry.configure.RetryConfiguration` | `microsphere.resilience4j.retry.` → `…retry.configure.RetryConfigurationProperties` |
| `timelimiter.annotation.EnableTimeLimiter` | `EnableTimeLimiterRegistrar` | `TimeLimiter` | `…timelimiter.configure.TimeLimiterConfiguration` | `microsphere.resilience4j.time-limiter.` → `…timelimiter.configure.TimeLimiterConfigurationProperties` |
| `bulkhead.annotation.EnableBulkhead` | `EnableBulkheadRegistrar` | `Bulkhead` | `…bulkhead.configure.BulkheadConfiguration` | `microsphere.resilience4j.bulkhead.` → `…bulkhead.configure.BulkheadConfigurationProperties` **and** `microsphere.resilience4j.thread-pool-bulkhead.` → `io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties` |
| `bulkhead.annotation.EnableThreadPoolBulkhead` | `EnableThreadPoolBulkheadRegistrar` | `ThreadPoolBulkhead` | `…bulkhead.configure.BulkheadConfiguration` (the same class as `@EnableBulkhead`; its Javadoc mentions `ThreadPoolBulkheadConfiguration`, the code passes `BulkheadConfiguration`) | the same two bindings as `@EnableBulkhead` |

`publishEvents`, `consumeEvents` and `plugins` behave identically on all six (defaults `false`, `false`,
`{}`).

Notes that are specific to the 1.x wiring:

- **The properties types are Resilience4j 1.x's `resilience4j-spring` types** (`…<module>.configure.<Module>ConfigurationProperties`),
  not the framework-common `Common<Module>ConfigurationProperties` types used on the 0.2.x line.
- `@EnableThreadPoolBulkhead` registers `BulkheadConfiguration`, and in Resilience4j `1.7.0` that class is
  annotated `@Configuration @Import({ThreadPoolBulkheadConfiguration.class, FallbackConfiguration.class,
  SpelResolverConfiguration.class})` — which is why the module's `EnableThreadPoolBulkheadTest` can still
  autowire `ThreadPoolBulkheadRegistry`. If you pin an older Resilience4j, verify the registry bean you
  depend on.
- `@EnableThreadPoolBulkhead` contributes **no event bridge**: `META-INF/spring.factories` ships event
  components only for `Bulkhead`, `CircuitBreaker`, `RateLimiter`, `Retry`, `TimeLimiter`, and the registrar
  only registers a component whose entry type equals the annotation's `E`. So
  `publishEvents`/`consumeEvents` on `@EnableThreadPoolBulkhead` currently register nothing. `BulkheadEvent`s
  are still reachable through `@EnableBulkhead(publishEvents = true, consumeEvents = true)` — the shape of
  `EnableThreadPoolBulkheadTest`, although that test keeps `@EnableBulkhead` at its defaults, so its
  `@EventListener` method is not invoked.
- The registrar resolves the annotation attributes through
  `ResolvablePlaceholderAnnotationAttributes.of(annotationAttributes, annotationType, getEnvironment())`
  (from `microsphere-spring-context`) so `${…}` placeholders inside the attribute values are resolved
  against the Spring `Environment` before they are read with `getBoolean("publishEvents")`,
  `getBoolean("consumeEvents")` and `getStringArray("plugins")`.

### 3.3 Minimal usage

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

## 4. Configuration properties

Each `@Enable*` annotation binds Resilience4j's own `*ConfigurationProperties` bean from
`microsphere.resilience4j.<module>.*`. The values below are exactly those used by the module's 1.x tests:

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

Every `@Enable*` test also carries `@EnableSpringConverterAdapter`
(`io.microsphere.spring.core.convert.annotation`, from `microsphere-spring-context`). Keep it when you bind
values such as `PT30S`, `PT0.000001S` or `0.7` into the 1.x configuration types, as the tests do.

---

## 5. Working examples (taken from the 1.x tests)

### 5.1 CircuitBreaker: registry + bound properties + Spring event

From `io.microsphere.resilience4j.spring.circuitbreaker.annotation.EnableCircuitBreakerTest`:

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

Attribution: `CircuitBreakerRegistry` comes from the `CircuitBreakerConfiguration` bean definition the
registrar registers (hence `resilience4j-spring` must be on your classpath, §2.2);
`CircuitBreakerConfigurationProperties` is bound by the annotation's `@EnableConfigurationBeanBinding`;
the `@EventListener` only fires because `publishEvents = true`.

### 5.2 Retry

From `EnableRetryTest` — note that the 1.x test asserts `waitDuration` and `eventConsumerBufferSize`
(it does not assert `maxAttempts`, unlike the 0.2.x test):

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

### 5.3 RateLimiter and TimeLimiter

From `EnableRateLimiterTest` (the 1.x version calls `acquirePermission()` only — no explicit
`rateLimiter.onSuccess()`) and `EnableTimeLimiterTest`:

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

### 5.4 Bulkhead, including the registry replace/remove lifecycle and a plugin

From `EnableBulkheadTest` — `plugins = NAME` where `NAME` is
`io.microsphere.resilience4j.spring.LoggingResilience4jPlugin.NAME` (= `"logging"`):

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

From `EnableThreadPoolBulkheadTest` — both bulkhead annotations are applied, and the thread-pool
properties type is the 1.x `io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties`:

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

The `@EventListener` above is copied verbatim from the test, but with these annotations it is never
called: `@EnableThreadPoolBulkhead`'s event flags register nothing (§3.2) and `@EnableBulkhead` keeps its
`publishEvents` default `false`.

---

## 6. Consuming Resilience4j events

The bridge offers two independent directions, both driven by the annotation flags.

```mermaid
flowchart LR
    ENTRY["Resilience4j 1.x entry<br/>e.g. CircuitBreaker 'test'"] -- "CircuitBreakerOnSuccessEvent" --> PUB["CircuitBreakerApplicationEventPublisher<br/>(publishEvents = true)"]
    PUB -- "ApplicationEventPublisher.publishEvent(event)" --> SPRING["Spring context"]
    SPRING --> LISTENER["@EventListener method"]
    CONSUMER["EventConsumer beans<br/>in your context"] -. "consumeEvents = true:<br/>CircuitBreakerEventConsumerBeanRegistrar<br/>subscribes them on the entry's EventPublisher" .-> ENTRY
```

### 6.1 Resilience4j events as Spring application events (`publishEvents = true`)

| Annotation | Publisher bean class (`io.microsphere.resilience4j.spring.*`) | Event type re-published |
|------------|---------------------------------------------------------------|--------------------------|
| `@EnableCircuitBreaker` | `circuitbreaker.event.CircuitBreakerApplicationEventPublisher` | `CircuitBreakerEvent` |
| `@EnableRateLimiter` | `ratelimiter.event.RateLimiterApplicationEventPublisher` | `RateLimiterEvent` |
| `@EnableRetry` | `retry.event.RetryApplicationEventPublisher` | `RetryEvent` |
| `@EnableTimeLimiter` | `timelimiter.event.TimeLimiterApplicationEventPublisher` | `TimeLimiterEvent` |
| `@EnableBulkhead` | `bulkhead.event.BulkheadApplicationEventPublisher` | `BulkheadEvent` |
| `@EnableThreadPoolBulkhead` | *none* (see §3.2) | — |

Semantics of `common.event.Resilience4jEventApplicationEventPublisher<E, ET>` (source):

- it implements `EventConsumer<ET>`, `RegistryEventConsumer<E>` and `ApplicationEventPublisherAware`;
- as a `RegistryEventConsumer`, it is notified when the module's `Registry` **adds** or **replaces** an
  entry; for that entry it reflectively calls `getEventPublisher()` and subscribes `this::consumeEvent`
  via `EventPublisher#onEvent(...)`, i.e. *all* events of that entry;
- `consumeEvent(ET event)` calls `applicationEventPublisher.publishEvent(event)` — the Resilience4j event
  object itself is published (Spring wraps it in a `PayloadApplicationEvent`), so you listen by event
  class exactly as in §5.1;
- `onEntryRemovedEvent(EntryRemovedEvent<E>)` is an empty method, so entries removed from the registry
  stay subscribed (harmless);
- every re-published event logs at trace: `"The Resilience4j Event : {} was published on the Spring
  ApplicationListener(s)"`.

Because the bridge subscribes on **entry added/replaced**, entries created before the publisher bean
existed are not propagated; create entries after startup (or re-create/replace them) if events seem to
be missing.

### 6.2 Typed `EventConsumer` beans (`consumeEvents = true`)

`common.event.Resilience4jEventConsumerBeanRegistrar<E>` (a `RegistryEventConsumer<E>` +
`BeanFactoryAware`) does not publish Spring events. For every added/replaced entry it:

1. reflectively obtains `entry.getEventPublisher()` (e.g. `CircuitBreaker.EventPublisher`);
2. walks that publisher class' methods, keeping only those accepted by
   `common.event.EventConsumerMethodFilter`: **exactly one parameter whose type is
   `io.github.resilience4j.core.EventConsumer`**;
3. resolves the generic parameter type (e.g. `EventConsumer<CircuitBreakerOnSuccessEvent>`) with
   `ResolvableType.forMethodParameter(method, 0)` and finds *all* matching beans through
   `beanFactory.getBeanProvider(type)`;
4. invokes the publisher method with each found bean, i.e. subscribes it.

So a consumer is any bean declared as `EventConsumer<SomeResilience4jEvent>`. From
`CircuitBreakerEventConsumerBeanRegistrarTest`:

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

In application code you get the same effect through the annotation:

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

(`java.util.logging.Logger` is shown for JDK 8 compatibility; the library itself logs through
`io.microsphere.logging.Logger`.)

Both directions combine freely (`publishEvents = true, consumeEvents = true`), as all `@Enable*` tests do.

The `EventConsumerMethodFilter` is public API in practice: its `INSTANCE` constant and
`matches(Method)` rule are exercised by `EventConsumerMethodFilterTest`, which asserts that *every*
locally declared method of `io.github.resilience4j.core.EventPublisher` and of the 1.x nested publishers
`Retry.EventPublisher`, `CircuitBreaker.EventPublisher`, `RateLimiter.EventPublisher`,
`TimeLimiter.EventPublisher`, `Bulkhead.EventPublisher` passes the filter on the Resilience4j 1.x
classpath.

---

## 7. Implementing and activating a `Resilience4jPlugin`

`io.microsphere.resilience4j.spring.common.Resilience4jPlugin` is the extension point used by the
project's plugins:

```java
public interface Resilience4jPlugin {

    void plugin(ConfigurableListableBeanFactory beanFactory,
                BeanDefinitionRegistry registry,
                ConfigurableEnvironment environment);

    String getName();
}
```

Three rules follow directly from `EnableResilience4jRegistrar`:

1. plugins are loaded from `META-INF/spring.factories` under the key
   `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` and instantiated **outside** the
   container (`SpringFactoriesLoaderUtils.loadFactories(beanFactory, Resilience4jPlugin.class)`) — keep a
   public no-arg constructor;
2. activation is by name: `plugins = {"…"}` is matched with
   `io.microsphere.util.ArrayUtils.contains(plugins, resilience4jPlugin.getName())`;
3. `plugin(...)` runs while the `@Enable*` registrar is still registering bean definitions, so its job is
   to register bean definitions (or otherwise touch the `BeanDefinitionRegistry`), not to assume beans
   already exist.

### 7.1 Reference implementation: `LoggingResilience4jPlugin` (test scope)

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

Registered in `src/test/resources/META-INF/spring.factories`:

```properties
io.microsphere.resilience4j.spring.common.Resilience4jPlugin=\
io.microsphere.resilience4j.spring.LoggingResilience4jPlugin,\
io.microsphere.resilience4j.spring.NoOpResilience4jPlugin
```

`NoOpResilience4jPlugin` (name `"no-operation"`, package-private class) shows the degenerate case — an
empty `plugin(...)` body — and is loaded but never activated because no annotation lists it.
`LoggingResilience4jPlugin` is activated by `EnableBulkheadTest` through `plugins = NAME`.

### 7.2 Plugin names shipped on this branch

| Plugin class | `getName()` | Module |
|--------------|-------------|--------|
| `io.microsphere.resilience4j.alibaba.druid.DruidResilience4jPlugin` | `"alibaba-druid"` (returned literally; the class also implements `plugin(...)` as a no-op) | `microsphere-resilience4j-alibaba-druid` |
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

`io.microsphere.resilience4j.spring.LazyResilience4jFacade` is the Spring-side entry point of the unified
programming model. It implements `Resilience4jFacade` (commons),
`ApplicationListener<ContextRefreshedEvent>` and `io.microsphere.lang.DelegatingWrapper`, and delegates to
a `ChainableResilience4jFacade` built from the `Registry` beans in your context.

- Registered by **every** `@Enable*` annotation under the fixed bean name
  `LazyResilience4jFacade.BEAN_NAME` = `"lazyResilience4jFacade"`, as a **primary** generic bean
  definition (`beanDefinition.setPrimary(true)`); its `modules` property is a mutable
  `Set<Resilience4jModule>` that accumulates one module per enabled annotation, regardless of how many
  annotations you apply.
- On `ContextRefreshedEvent` it calls `context.getBean(module.getRegistryClass())` for each accumulated
  module and constructs the delegate. **Use it only after the context is refreshed** — before that the
  delegate is `null` and every operation throws `NullPointerException`.
- `setModules(Set<Resilience4jModule>)` / `getModules()` are the JavaBean accessors the registrar writes
  through the bean definition.
- As a `DelegatingWrapper`, `getDelegate()` returns the current delegate and the inherited `unwrap(Class)`
  exposes it typed.

| Method (as declared in the 1.x source) | Notes |
|---|---|
| `void execute(String name, Runnable callback)` · `<T> T execute(String name, Supplier<T> callback)` | unchecked-friendly entry point |
| `void call(String name, ThrowableAction)` · `<TR extends Throwable> void call(String name, ThrowableAction, Class<TR>)` · `<T> T call(String name, ThrowableSupplier<T>)` · `<T, TR extends Throwable> T call(String name, ThrowableSupplier<T>, Class<TR>)` | may throw `Throwable` / the declared `TR` |
| `Resilience4jContext<Resilience4jContext[]> begin(String name)` · `void end(Resilience4jContext<Resilience4jContext[]>)` | manual decoration; `getEntry()` is the array of per-module sub-contexts |
| `boolean isBeginSupported()` · `boolean isEndSupported()` | delegated |
| `void onApplicationEvent(ContextRefreshedEvent event)` | builds the delegate |
| `Object getDelegate()` | `null` before the first `ContextRefreshedEvent` |

Example from `LazyResilience4jFacadeTest` (which enables the Bulkhead module only):

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

In application code, inject it by type (it is the primary `Resilience4jFacade`) or by name:

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

When you enable several modules (`@EnableRetry` + `@EnableCircuitBreaker` + `@EnableRateLimiter`), the same
`execute("name", …)` call is wrapped by all of them, ordered by commons' template priorities — see
[microsphere-resilience4j-commons.md](microsphere-resilience4j-commons.md).

---

## 9. Troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| `NoClassDefFoundError: io/github/resilience4j/<module>/configure/<Module>Configuration` | `resilience4j-spring` is *optional* here — add it plus the runtime modules and `resilience4j-framework-common` yourself (§2.2). |
| No `@EventListener` fires | `publishEvents` left at its default `false` (the Javadoc claiming `true` is wrong); the entry was created before the publisher existed (the bridge subscribes on registry **entry added/replaced**); or you are on `@EnableThreadPoolBulkhead`, whose event flags register nothing (§3.2). |
| `EventConsumer<X>` bean never invoked | `consumeEvents = false`; the annotation's entry type does not match the registrar's; or the bean's declared type is not exactly `EventConsumer<TheEventClass>` (raw `EventConsumer` or a `Consumer<…>` does not match `ResolvableType.forMethodParameter`). |
| `ClassNotFoundException` for an event type such as `CircuitBreakerOnFailureRateExceededEvent` / `RateLimiterOnDrainedEvent` on Resilience4j `1.3` – `1.6` | Those types are provided by `microsphere-resilience4j-compatible`. It is attached automatically when you build *this* line with the matching `resilience4j-1.x` profile; in your own application add the Resilience4j artifacts of a version that contains them (e.g. `1.7.0`) or ship the missing event classes yourself. |
| `Duration` / percentage properties ignored | Missing `@EnableSpringConverterAdapter`, or the wrong prefix (each prefix ends with a trailing dot, e.g. `microsphere.resilience4j.circuit-breaker.instances[test].…`). |
| `NullPointerException` from the facade | `LazyResilience4jFacade` used before `ContextRefreshedEvent`. |
| Plugin not activated | `plugins` values are matched against `Resilience4jPlugin#getName()` (not bean names), and the plugin must be listed under `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` in a `META-INF/spring.factories` visible to the registrar's class loader. |
| `ThreadPoolBulkheadRegistry` missing with `@EnableThreadPoolBulkhead` | The registrar registers `BulkheadConfiguration`; on Resilience4j `1.7.0` that class transitively imports `ThreadPoolBulkheadConfiguration`. On older 1.x releases, verify the registry bean or declare the thread-pool configuration yourself. |

---

## 10. Difference from the 0.2.x line

Both lines carry the *same programming model*, and most of this module's sources are byte-identical
between `release-1.x` and `release`. Use this table when reading the sibling page
[../../current/user-guide/microsphere-resilience4j-spring.md](../../current/user-guide/microsphere-resilience4j-spring.md)
together with this one.

| Aspect | 1.x (`release-1.x`, `0.1.x`) | 0.2.x (`release`, current) |
|--------|------------------------------|----------------------------|
| Resilience4j | `1.3.0` – `1.7.0`, default `1.7.0` (profiles `resilience4j-1.3` … `resilience4j-1.7`) | `2.0.0` – `2.4.0`, default `2.4.0` |
| Spring stack | parent `microsphere-spring-cloud-parent` `0.1.26`; Spring Cloud `Hoxton` – `2021.0.x` (Boot `2.3.12.RELEASE` / `2.4.13` / `2.6.15`) | parent `0.2.26`; Spring Cloud `2022.0.x` – `2025.0.x` |
| Compatibility shim | Extra module **`microsphere-resilience4j-compatible`**, attached to `…-commons` by the `resilience4j-1.3` … `resilience4j-1.6` profiles. It declares `io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent`, `CircuitBreakerOnFailureRateExceededEvent`, `CircuitBreakerOnSlowCallRateExceededEvent`, `io.github.resilience4j.ratelimiter.event.RateLimiterEvent`, `RateLimiterOnDrainedEvent`. Consequently `CircuitBreakerApplicationEventPublisher`'s import of `…circuitbreaker.event.CircuitBreakerEvent` may resolve to the **shim** instead of `resilience4j-circuitbreaker`. | absent — Resilience4j 2.x ships those types |
| Common configurations | `EnableResilience4jRegistrar` calls `registerCommonConfigurations(registry)` (a **1.x-only** helper class `io.microsphere.resilience4j.spring.common.Resilience4jUtils`) which registers `io.github.resilience4j.fallback.configure.FallbackConfiguration` and, reflectively, `io.github.resilience4j.spelresolver.configure.SpelResolverConfiguration` ("since Resilience4j 1.5", registered only if present on the classpath). The 1.x Javadoc explains why: only *Resilience4j Spring 2.0+* entry configurations import them by default. | that call and that class do not exist — 2.x entry configurations already import both |
| Thread-pool bulkhead properties type | `@EnableBulkhead` / `@EnableThreadPoolBulkhead` bind `io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties` | bind `…common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties` |
| Test support | the 1.x tests use a `CommonConfigurationsApplicationContextInitializer` (`io.microsphere.resilience4j.spring.context`, test scope) so that the manually wired event tests still get `Fallback`/`SpelResolver` configurations; the tests read instance properties via the module's own `…<module>.configure.<Module>ConfigurationProperties.InstanceProperties` | that initializer class does not exist; tests read properties via the `Common<Module>ConfigurationProperties` types |
| This module's POM | no `<profiles>` section → `resilience4j.version` comes from the parent (default `1.7.0`) | re-declares profile `resilience4j-2.4` as `activeByDefault` but sets `2.3.0`, so building this module alone resolves Resilience4j `2.3.0` |
| Event bridge sources | `Resilience4jEventApplicationEventPublisher`, `Resilience4jEventConsumerBeanRegistrar`, `EventConsumerMethodFilter`, all six publishers/registrars, `LazyResilience4jFacade`, `Resilience4jPlugin`, `@EnableResilience4j` and the per-module annotations/registrars are **byte-identical** to the 0.2.x files; only the types those imports resolve to differ (1.x `EventPublisher`/`EventConsumer` and `.event` packages) | same sources, Resilience4j 2.x types |
| `@Enable*` attribute defaults | identical: `publishEvents = false`, `consumeEvents = false`, `plugins = {}`, with the same "`true` as default" Javadoc mismatch | identical |

Porting an application from `0.2.x` down to `0.1.x` therefore means: change the BOM version, keep the
annotations and event code unchanged, add the Resilience4j 1.x artifacts, and be aware that properties
now bind to the 1.x `…configure.*ConfigurationProperties` /
`io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties` types.

---

## 11. See also

- [1.x user guide index](README.md) · [commons module page](microsphere-resilience4j-commons.md) (templates, `Resilience4jFacade`, `Resilience4jModule`)
- [compatible shim module page](microsphere-resilience4j-compatible.md)
- [1.x developer guide — microsphere-resilience4j-spring](../developer-guide/microsphere-resilience4j-spring.md) (registrar source analysis, event-bridge design, bootstrap/plugin diagrams)
- Current line equivalent: [0.2.x user guide](../../current/user-guide/microsphere-resilience4j-spring.md)
- Plugin pages: [alibaba-druid](microsphere-resilience4j-alibaba-druid.md), [mybatis](microsphere-resilience4j-mybatis.md), [spring-web](microsphere-resilience4j-spring-web.md), [openfeign](microsphere-resilience4j-openfeign.md)
