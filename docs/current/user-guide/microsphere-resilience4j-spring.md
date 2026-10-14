# microsphere-resilience4j-spring — User Guide

> Source branch: `release` · Version line: `0.2.x` (latest release `0.2.4`) · Target: **Resilience4j 2.0.0 – 2.4.0** (default `2.4.0`)
> Back to [guide index](README.md)

`microsphere-resilience4j-spring` is the Spring layer of microsphere-resilience4j. It turns the
framework-neutral templates of `microsphere-resilience4j-commons` into a Spring experience:

- a family of `@Enable*` annotations (`@EnableCircuitBreaker`, `@EnableRateLimiter`, `@EnableRetry`,
  `@EnableBulkhead`, `@EnableThreadPoolBulkhead`, `@EnableTimeLimiter`) that register Resilience4j's
  own Spring configuration classes and therefore the `Registry` beans of the module;
- automatic binding of `microsphere.resilience4j.<module>.*` properties onto Resilience4j's
  `*ConfigurationProperties` types;
- an **event bridge** that re-publishes Resilience4j events as Spring application events
  (`publishEvents = true`) and that injects your `EventConsumer` beans into Resilience4j's event
  publishers (`consumeEvents = true`);
- a `Resilience4jPlugin` SPI so framework plugins (MyBatis, Druid, Spring Web, …) can attach
  themselves to an `@Enable*` annotation;
- `LazyResilience4jFacade`, a `Resilience4jFacade` bean that is assembled from the `Registry` beans
  present in the context once the context is refreshed.

---

## 1. Prerequisites

| Requirement | Detail |
|-------------|--------|
| `microsphere-resilience4j-commons` | Non-optional compile dependency of this module — the templates, `Resilience4jFacade`, `Resilience4jModule`, `Resilience4jConstants` all come from there. |
| `microsphere-spring-context` | Non-optional compile dependency — supplies `BeanCapableImportCandidate`, `BeanRegistrar`, `ResolvablePlaceholderAnnotationAttributes`, `@EnableConfigurationBeanBinding`, `SpringFactoriesLoaderUtils`. |
| Spring Framework / Spring Context | Provided by your application (`microsphere-spring-cloud-parent` line). |
| Resilience4j artifacts | **Every `io.github.resilience4j:*` dependency in this module's POM is `<optional>true</optional>`** (`resilience4j-spring`; `spring-webmvc` is optional as well), so declare the Resilience4j artifacts you actually use in your own POM — see §2.2. |
| `@Configuration` class | The `@Enable*` annotations are `@Target(TYPE)`: put them on a configuration class (or, in the tests, directly on the test class which doubles as the configuration). |
| JDK | The `release` branch root POM sets `<java.version>17</java.version>`, i.e. the build baseline of the `0.2.x` line is **JDK 17** (Spring Boot 3/4 and Spring Cloud 2022+ under the parent). The JDK 8 line is `0.1.x` — see the [1.x user guide](../../1.x/user-guide/README.md). |

---

## 2. Maven coordinates and versions

### 2.1 Coordinates

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

The BOM (`microsphere-resilience4j-dependencies`) manages `microsphere-resilience4j-spring`
alongside `…-commons` and the plugin artifacts, all with the project version.

### 2.2 Resilience4j artifacts you must add yourself

The annotations reference Resilience4j types directly
(`io.github.resilience4j.<module>.configure.<Module>Configuration` and
`io.github.resilience4j.<module>.configure.<Module>ConfigurationProperties`). Those classes come from the
Resilience4j Spring-support artifacts, and this module declares them **optional**
(`resilience4j-spring` in its own POM, `resilience4j-framework-common` / `resilience4j-core` /
`resilience4j-<module>` in the commons POM), so declare them yourself:

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

### 2.3 Version matrix (this line)

| Item | Value | Verified from |
|------|-------|---------------|
| Project version line | `0.2.x`, latest release `0.2.4`, next `0.2.5-SNAPSHOT` | root `pom.xml` (`${revision}`), `README.md` |
| Resilience4j | `2.0.0`, `2.1.0`, `2.2.0`, `2.3.0`, `2.4.0` via Maven profiles `resilience4j-2.0` … `resilience4j-2.4`; **default profile `resilience4j-2.4` → `2.4.0`** | `microsphere-resilience4j-parent/pom.xml` |
| Spring Cloud | `2022.0.x` – `2025.0.x` (parent `microsphere-spring-cloud-parent` `0.2.26`, default profile `spring-cloud-2025` → Spring Boot `3.5.15` / Spring Cloud `2025.0.3`) | parent POM profiles |
| JDK | `17` (`<java.version>17</java.version>`) | root `pom.xml` |
| Spring Framework version | managed by the imported `microsphere-spring-cloud-dependencies` BOM; no explicit version in this module's POM | `microsphere-resilience4j-spring/pom.xml` |

> **POM quirk worth knowing**: `microsphere-resilience4j-spring/pom.xml` re-declares the profile
> `resilience4j-2.4` (still `activeByDefault`) with `<resilience4j.version>2.3.0</resilience4j.version>`.
> The child profile replaces the inherited one, so building **this module alone** without `-P` resolves
> Resilience4j `2.3.0`. Pass `-P resilience4j-2.4` from the reactor root, or pin the version yourself,
> when the exact Resilience4j build matters.

---

## 3. The `@Enable*` annotations

### 3.1 The common meta-annotation `@EnableResilience4j`

`io.microsphere.resilience4j.spring.common.annotation.EnableResilience4j` is **not** applied by
applications: it is declared `@Target(ANNOTATION_TYPE)`, so it only exists as a meta-annotation on the
six per-module annotations, where its attributes are re-exposed with `@AliasFor`.

| Attribute | Type | Default | Meaning |
|-----------|------|---------|---------|
| `publishEvents` | `boolean` | `false` | Registers the module's `Resilience4jEventApplicationEventPublisher` implementation → Resilience4j events become Spring application events. |
| `consumeEvents` | `boolean` | `false` | Registers the module's `Resilience4jEventConsumerBeanRegistrar` implementation → your `EventConsumer` beans are subscribed to Resilience4j's event publishers. |
| `plugins` | `String[]` | `{}` | The **`Resilience4jPlugin.getName()` names** of the plugins to activate (matched against the plugins found in `META-INF/spring.factories`). |

> The Javadoc of `publishEvents`/`consumeEvents` says "`true` as default", but the compiled default of
> every annotation in this module is **`false`** (`boolean publishEvents() default false;`). Rely on the
> code: if you do not set the flags, no event bridge is registered.
> Likewise the `plugins()` Javadoc of the per-module annotations talks about "Spring Bean names"; the
> registrar compares the values against `Resilience4jPlugin#getName()`, i.e. **plugin names, not bean names**.

### 3.2 The six public annotations

Every per-module annotation declares exactly the same three attributes (`publishEvents`,
`consumeEvents`, `plugins`), all with `@AliasFor(annotation = EnableResilience4j.class, …)`; each is
`@Target(TYPE) @Retention(RUNTIME) @Documented @Inherited`, is annotated with `@EnableResilience4j`, and
`@Import`s its own (package-private) registrar. What differs is the module wiring they pull in:

| Annotation | Registrar (`@Import`ed) | Entry type `E` | Entry configuration registered `EC` | Properties bound by `@EnableConfigurationBeanBinding(s)` |
|------------|-------------------------|----------------|-------------------------------------|----------------------------------------------------------|
| `…spring.circuitbreaker.annotation.EnableCircuitBreaker` | `EnableCircuitBreakerRegistrar` | `CircuitBreaker` | `CircuitBreakerConfiguration` | `microsphere.resilience4j.circuit-breaker.` → `CircuitBreakerConfigurationProperties` |
| `…spring.ratelimiter.annotation.EnableRateLimiter` | `EnableRateLimiterRegistrar` | `RateLimiter` | `RateLimiterConfiguration` | `microsphere.resilience4j.rate-limiter.` → `RateLimiterConfigurationProperties` |
| `…spring.retry.annotation.EnableRetry` | `EnableRetryRegistrar` | `Retry` | `RetryConfiguration` | `microsphere.resilience4j.retry.` → `RetryConfigurationProperties` |
| `…spring.timelimiter.annotation.EnableTimeLimiter` | `EnableTimeLimiterRegistrar` | `TimeLimiter` | `TimeLimiterConfiguration` | `microsphere.resilience4j.time-limiter.` → `TimeLimiterConfigurationProperties` |
| `…spring.bulkhead.annotation.EnableBulkhead` | `EnableBulkheadRegistrar` | `Bulkhead` | `BulkheadConfiguration` | `microsphere.resilience4j.bulkhead.` → `BulkheadConfigurationProperties` **and** `microsphere.resilience4j.thread-pool-bulkhead.` → `CommonThreadPoolBulkheadConfigurationProperties` |
| `…spring.bulkhead.annotation.EnableThreadPoolBulkhead` | `EnableThreadPoolBulkheadRegistrar` | `ThreadPoolBulkhead` | `BulkheadConfiguration` (same class as `@EnableBulkhead`) | the same two bindings as `@EnableBulkhead` |

The three attributes are identical on all six annotations:

| Attribute | Type | Default | Meaning |
|-----------|------|---------|---------|
| `publishEvents` | `boolean` | `false` | registers the module's `Resilience4jEventApplicationEventPublisher` (see §6.1) |
| `consumeEvents` | `boolean` | `false` | registers the module's `Resilience4jEventConsumerBeanRegistrar` (see §6.2) |
| `plugins` | `String[]` | `{}` | plugin names to activate, matched against `Resilience4jPlugin#getName()` (see §7) |

Before these values are used, the registrar wraps the raw attribute map in
`ResolvablePlaceholderAnnotationAttributes.of(annotationAttributes, annotationType, getEnvironment())`
(`io.microsphere.spring.core.annotation`, microsphere-spring-context), whose role is to resolve `${…}`
placeholders against the Spring `Environment`, and then reads them with
`attributes.getBoolean("publishEvents")` / `getBoolean("consumeEvents")` and
`attributes.getStringArray("plugins")`.

Notes:

- Prefixes come from `io.microsphere.resilience4j.common.Resilience4jConstants` (commons):
  `PREFIX = "microsphere.resilience4j."` plus `retry.`, `circuit-breaker.`, `rate-limiter.`,
  `time-limiter.`, `bulkhead.`, `thread-pool-bulkhead.`.
- `@EnableThreadPoolBulkhead` uses `BulkheadConfiguration` as its entry-configuration type (that is what
  the third generic parameter says in the source), and — because `META-INF/spring.factories` ships no
  `ThreadPoolBulkhead`-typed event components — its `publishEvents`/`consumeEvents` flags currently
  register nothing. If you need `BulkheadEvent`s (which `ThreadPoolBulkhead` also publishes), enable
  `@EnableBulkhead(publishEvents = true, consumeEvents = true)` as well; this is exactly what the
  module's `EnableThreadPoolBulkheadTest` does.
- Registrars are package-private classes, so they cannot be `@Import`ed directly by user code — use the
  annotations.

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
`microsphere.resilience4j.<module>.*`. The tests in this module are the reference examples:

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

Every test of this module also adds `@EnableSpringConverterAdapter`
(`io.microsphere.spring.core.convert.annotation`, from `microsphere-spring-context`). Keep it when you
bind values such as `PT30S`, `PT0.000001S` or `0.7` into Resilience4j's configuration types, as the
tests do.

---

## 5. Working examples (taken from the module's tests)

### 5.1 CircuitBreaker: registry + properties + Spring event

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

What this test shows about attribution: `CircuitBreakerRegistry` comes from the
`CircuitBreakerConfiguration` class that the registrar registers (which is why `resilience4j-spring` must be
on your classpath — §2.2), `CircuitBreakerConfigurationProperties` is bound by the annotation's
`@EnableConfigurationBeanBinding`, and the `@EventListener` bridge only exists because of
`publishEvents = true`.

### 5.2 Retry

From `EnableRetryTest`:

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

From `EnableRateLimiterTest` and `EnableTimeLimiterTest`:

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

### 5.4 Bulkhead (including replace/remove lifecycle)

From `EnableBulkheadTest` — note the `plugins = NAME` attribute (see §7) and the registry lifecycle calls:

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

From `EnableThreadPoolBulkheadTest` — both bulkhead annotations are applied, because
`@EnableThreadPoolBulkhead` alone contributes no event bridge and its entry configuration is
`BulkheadConfiguration`:

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

The `@EventListener` method above is copied verbatim from the test, but note that with these
annotations it is never called: `publishEvents = true` on `@EnableThreadPoolBulkhead` registers no
publisher (§3.2), and `@EnableBulkhead` here keeps the `publishEvents` default `false`. Even the
`Bulkhead` publisher would not help — it subscribes to `Bulkhead` entries, not to
`ThreadPoolBulkhead` instances such as the one executed here.

---

## 6. Consuming Resilience4j events

The module gives you two independent, complementary directions.

```mermaid
flowchart LR
    ENTRY["Resilience4j entry<br/>e.g. CircuitBreaker 'test'"] -- "event (CircuitBreakerOnSuccessEvent)" --> PUB["…ApplicationEventPublisher<br/>registered by publishEvents = true"]
    PUB -- "publishEvent(the event object)" --> SPRING["Spring ApplicationEventPublisher"]
    SPRING --> LISTENER["@EventListener method"]
    CONSUMER["EventConsumer beans in your context"] -. "consumeEvents = true:<br/>…EventConsumerBeanRegistrar subscribes them<br/>onto the entry's EventPublisher" .-> ENTRY
```

### 6.1 Resilience4j events as Spring application events (`publishEvents = true`)

When `publishEvents = true`, the registrar registers the `Resilience4jEventApplicationEventPublisher`
implementation whose entry type matches the annotation — one of:

| Annotation | Publisher bean class | Event type re-published |
|------------|----------------------|-------------------------|
| `@EnableCircuitBreaker` | `…spring.circuitbreaker.event.CircuitBreakerApplicationEventPublisher` | `CircuitBreakerEvent` |
| `@EnableRateLimiter` | `…spring.ratelimiter.event.RateLimiterApplicationEventPublisher` | `RateLimiterEvent` |
| `@EnableRetry` | `…spring.retry.event.RetryApplicationEventPublisher` | `RetryEvent` |
| `@EnableTimeLimiter` | `…spring.timelimiter.event.TimeLimiterApplicationEventPublisher` | `TimeLimiterEvent` |
| `@EnableBulkhead` | `…spring.bulkhead.event.BulkheadApplicationEventPublisher` | `BulkheadEvent` |
| `@EnableThreadPoolBulkhead` | *(none registered — see the note in §3.2)* | — |

Semantics (from `Resilience4jEventApplicationEventPublisher`):

- it is a `RegistryEventConsumer<E>`, so it is notified whenever the module's `Registry` adds or replaces
  an entry; for each such entry it reflectively calls `getEventPublisher()` and subscribes
  `this::consumeEvent` through `EventPublisher#onEvent(...)`, i.e. **all** events of that entry;
- `consumeEvent(ET event)` calls `applicationEventPublisher.publishEvent(event)` — the Resilience4j event
  object itself is published (Spring wraps it in a `PayloadApplicationEvent`), so you listen by event
  class exactly as in §5.1:

```java
@EventListener(CircuitBreakerOnSuccessEvent.class)
void onCircuitBreakerOnSuccessEvent(CircuitBreakerOnSuccessEvent event) { … }
```

`onEntryRemovedEvent` is implemented as a no-op, so removed entries stay subscribed (harmless); the
trace log `"The Resilience4j Event : {} was published on the Spring ApplicationListener(s)"` is emitted
for every re-published event.

### 6.2 Consumer methods (`consumeEvents = true`)

When `consumeEvents = true`, the registrar registers the matching
`Resilience4jEventConsumerBeanRegistrar` implementation
(`CircuitBreakerEventConsumerBeanRegistrar`, `RateLimiterEventConsumerBeanRegistrar`,
`RetryEventConsumerBeanRegistrar`, `TimeLimiterEventConsumerBeanRegistrar`,
`BulkheadEventConsumerBeanRegistrar`). It does **not** publish Spring events; instead, for every entry
added/replaced in the registry it:

1. reflects `entry.getEventPublisher()` (e.g. `CircuitBreaker.EventPublisher`);
2. keeps only the publisher methods accepted by `EventConsumerMethodFilter`: **exactly one parameter
   whose declared type is `io.github.resilience4j.core.EventConsumer`** — this is what makes
   `onSuccess(EventConsumer<CircuitBreakerOnSuccessEvent>)`, `onError(...)`, `onEvent(...)`, … match and
   what makes every other method ignored;
3. for each such method, resolves the *generic* parameter type (e.g.
   `EventConsumer<CircuitBreakerOnSuccessEvent>`) with `ResolvableType.forMethodParameter(method, 0)` and
   looks up **all matching Spring beans** with `beanFactory.getBeanProvider(type)`;
4. calls the publisher method with each found bean, i.e. subscribes it.

So a consumer is any bean declared as `EventConsumer<SomeResilience4jEvent>`. From
`CircuitBreakerEventConsumerBeanRegistrarTest`:

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

In application code you get the same effect with the annotation:

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

You may also combine both directions (as the `@Enable*` tests do with
`publishEvents = true, consumeEvents = true`): `@EventListener` for loose coupling, `EventConsumer`
beans for typed, per-event-class subscriptions.

---

## 7. Implementing and registering a `Resilience4jPlugin`

`io.microsphere.resilience4j.spring.common.Resilience4jPlugin` is the extension point used by the
framework plugins (`…-mybatis`, `…-alibaba-druid`, `…-spring-web`):

```java
public interface Resilience4jPlugin {

    void plugin(ConfigurableListableBeanFactory beanFactory,
                BeanDefinitionRegistry registry,
                ConfigurableEnvironment environment);

    String getName();
}
```

Three rules the implementation must follow (all derived from the registrar code):

1. it is **instantiated outside the container** from `META-INF/spring.factories` (no-arg constructor), so
   keep the class public with a public no-arg constructor;
2. `getName()` must return the string you will list in the `plugins` attribute — matching is
   `ArrayUtils.contains(plugins, plugin.getName())`, i.e. **exact plugin name**, not a bean name;
3. `plugin(...)` runs while the `@Enable*` registrar is still registering bean definitions, so its job is
   to *register bean definitions* (or otherwise touch the registry), not to assume beans already exist.

The test-scope reference implementation (`io.microsphere.resilience4j.spring.LoggingResilience4jPlugin`):

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

registered in `src/test/resources/META-INF/spring.factories`:

```properties
io.microsphere.resilience4j.spring.common.Resilience4jPlugin=\
io.microsphere.resilience4j.spring.LoggingResilience4jPlugin,\
io.microsphere.resilience4j.spring.NoOpResilience4jPlugin
```

and activated by name in `EnableBulkheadTest`:

```java
import static io.microsphere.resilience4j.spring.LoggingResilience4jPlugin.NAME;

@EnableBulkhead(publishEvents = true, consumeEvents = true, plugins = NAME)
class EnableBulkheadTest { … }
```

The companion test plugin `NoOpResilience4jPlugin` (name `"no-operation"`) shows the degenerate case: an
empty `plugin(...)` body. It is loaded but never activated because no annotation lists it.

Plugin names shipped by the project's own plugins on this branch:

| Plugin class | `getName()` | Module |
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

`io.microsphere.resilience4j.spring.LazyResilience4jFacade` is the Spring-side entry point of the unified
programming model: it implements `Resilience4jFacade` (commons) and delegates to a
`ChainableResilience4jFacade` built from the `Registry` beans that happen to be in your context.

- Registered automatically by **every** `@Enable*` annotation, under the fixed bean name
  `LazyResilience4jFacade.BEAN_NAME` = `"lazyResilience4jFacade"`, as a **primary** bean definition
  (`beanDefinition.setPrimary(true)`), with the mutable `modules` property accumulating one
  `Resilience4jModule` per enabled annotation.
- It is an `ApplicationListener<ContextRefreshedEvent>`: on context refresh it resolves
  `context.getBean(module.getRegistryClass())` for every enabled module and constructs the delegate.
  **Call it only after the context is refreshed** — before that, the delegate is `null` and every
  operation throws `NullPointerException`.
- It implements `io.microsphere.lang.DelegatingWrapper`, so `getDelegate()` and the inherited
  `unwrap(Class)` expose the underlying `ChainableResilience4jFacade`.

| Method | Signature (from source) | Notes |
|--------|-------------------------|-------|
| `execute` | `void execute(String name, Runnable callback)` / `<T> T execute(String name, Supplier<T> callback)` | wrapped, unchecked-friendly entry point |
| `call` | `void call(String name, ThrowableAction)` · `<TR extends Throwable> void call(String name, ThrowableAction, Class<TR>)` · `<T> T call(String name, ThrowableSupplier<T>)` · `<T, TR extends Throwable> T call(String name, ThrowableSupplier<T>, Class<TR>)` | throws `Throwable` / the declared `TR` |
| `begin` / `end` | `Resilience4jContext<Resilience4jContext[]> begin(String name)` · `void end(Resilience4jContext<Resilience4jContext[]>)` | manual decoration; `getEntry()` is the array of per-module sub-contexts |
| `isBeginSupported` / `isEndSupported` | `boolean` | delegated |
| `setModules` / `getModules` | `void setModules(Set<Resilience4jModule>)` / `Set<Resilience4jModule> getModules()` | set by the registrar through the bean-definition property |
| `onApplicationEvent` | `void onApplicationEvent(ContextRefreshedEvent)` | builds the delegate |
| `getDelegate` | `Object getDelegate()` | overrides `DelegatingWrapper` and returns the current delegate (`null` before the first `ContextRefreshedEvent`); `unwrap(Class)` is inherited from the same interface |

Example (from `LazyResilience4jFacadeTest`, which enables only the Bulkhead module):

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

If you enable several modules (`@EnableRetry` + `@EnableCircuitBreaker` + `@EnableRateLimiter`), the same
`execute("name", …)` call is wrapped by all of them, in the order defined by commons' template
priorities — see [microsphere-resilience4j-commons.md](microsphere-resilience4j-commons.md).

---

## 9. Quick troubleshooting

| Symptom | Cause / fix |
|---------|-------------|
| `ClassNotFoundException: io.github.resilience4j.circuitbreaker.configure.CircuitBreakerConfiguration` | `resilience4j-spring` is an *optional* dependency of this module — add it (and `resilience4j-framework-common`, plus the runtime modules) yourself. See §2.2. |
| No `@EventListener` fires | You left `publishEvents` at its default `false`, or the entry was created before the publisher bean existed (the bridge subscribes on registry **entry added/replaced**), or you are on `@EnableThreadPoolBulkhead` whose event flags register nothing (§3.2). |
| `EventConsumer<X>` bean is never invoked | `consumeEvents` is `false`; the entry type of your annotation does not match the registrar (only the five published types are supported); or the bean's generic type is not exactly `EventConsumer<TheEventClass>`. |
| `Duration` / percentage properties not applied | Missing `@EnableSpringConverterAdapter` (see §4), or the wrong prefix — prefixes end with a trailing dot, e.g. `microsphere.resilience4j.circuit-breaker.instances[test].…`. |
| `NullPointerException` from the facade | `LazyResilience4jFacade` was used before `ContextRefreshedEvent`; inject and use it from a bean that runs after startup. |
| Plugin not activated | `plugins` values are matched against `Resilience4jPlugin#getName()`; make sure the plugin is listed under `io.microsphere.resilience4j.spring.common.Resilience4jPlugin` in `META-INF/spring.factories` on the class loader used by the registrar. |

---

## 10. See also

- [User guide index](README.md) · [commons module page](microsphere-resilience4j-commons.md) (templates, `Resilience4jFacade`, `Resilience4jModule`)
- [Developer guide — microsphere-resilience4j-spring](../developer-guide/microsphere-resilience4j-spring.md) (registrar source analysis, event bridge design, flow diagrams)
- Plugin pages: [alibaba-druid](microsphere-resilience4j-alibaba-druid.md), [mybatis](microsphere-resilience4j-mybatis.md), [spring-web](microsphere-resilience4j-spring-web.md), [openfeign](microsphere-resilience4j-openfeign.md)
